package explode.backend.bomb.v1

import explode.backend.bomb.v0.respondJson
import explode.dataprovider.model.database.*
import explode.dataprovider.provider.BlowException
import explode.dataprovider.provider.IBlowOmni
import explode.dataprovider.provider.mongo.MongoProvider
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import java.io.File
import explode.backend.bomb.v1.BombConfiguration as Conf

typealias TheCall = PipelineContext<Unit, ApplicationCall>

internal val BombFolder = File(".explode_bomb").apply { mkdirs() }

private val callouts = listOf(
	"Ascendant Plane", "Black Garden", "Black Heart", "Commune",
	"Darkness", "Drink", "Earth", "Enter", "Fleet", "Give", "Grieve", "Guardian", "Hive", "Kill", "Knowledge",
	"Light", "Love", "Pyramid", "Savathûn", "Scorn", "Stop", "Tower", "Traveller", "Witness", "Worm", "Worship"
)

context(IBlowOmni)
class Bomb(private val omni: IBlowOmni) {

	fun Application.bombModule() = with(omni) {

		install(CORS) {
			anyHost()
			allowHeader(HttpHeaders.AccessControlAllowOrigin)
		}

		install(Authentication) {
			basic {
				realm = "Access"
				validate {
					runCatching {
						val tok = omni.loginUser(it.name, it.password).token
						UserIdPrincipal(tok)
					}.getOrNull()
				}
			}
		}

		install(StatusPages) {
			exception<BlowException> { call, cause ->
				call.respondJson(
					buildMap {
						this["error"] = cause.message ?: cause.javaClass.simpleName
						// no stack-trace for BlowException, because it's just a Fast-Fail but not actual error.
					},
					HttpStatusCode.InternalServerError
				)
			}

			exception<UnauthorizedException> { call, cause ->
				call.respondJson("error" to cause.message, HttpStatusCode.Unauthorized)
			}

//			exception<Throwable> { call, cause ->
//				call.respondJson(
//					buildMap {
//						this["error"] = cause.message ?: cause.javaClass.simpleName
//						this["trace"] = cause.stackTraceToString()
//					},
//					HttpStatusCode.InternalServerError
//				)
//			}
		}

		routing {
			route("v1") {

				// get [/] - return something to test
				get {
					respOk(callouts.random())
				}

				// private modules
				authenticate {
					// management
					route("management") {
						// get [/management] - test the authentication/return the auth user
						get {
							val user = getAuthUserOrNull()
							respOk(user)
						}

						// users
						route("user") {

							// post [/management/user/register] - register the user; return the full user data
							post("register") {
								checkAdmin()
								val body = receive<Map<String, String>>()
								val username = body.getOrBadRequest("username")
								val password = body.getOrBadRequest("password")
								// register and respond
								val user = omni.registerUser(username, password)
								respOk(user)
							}

							route("{id}") {
								// get [/management/user/{id}] - return the full user data
								get {
									checkAdmin()
									respOk(getParamUser())
								}

								// patch [/management/user/{id}] - change the fields of the user; return the full user data
								@InternalUse
								patch {
									checkAdmin()
									val user = getParamUser()
									val newValues = receive<Map<String, Any?>>()
									// change and update
									user.reflectApplyChanges(newValues, MongoUser::id)
									omni.updateUser(user)
									// respond
									respOk(user)
								}
							}
						}

						// set
						route("set") {
							// get [/management/set/by-name/{name}] - return the set with the provided name
							get("by-name/{name}") {
								checkAdmin()
								respOk(omni.getSetsByName(getParam("name")).toList().throwNotFoundIfEmpty())
							}

							// get [/management/set/by-chart/{id}] - return the set including the provided chart
							get("by-chart/{id}") {
								checkAdmin()
								respOk(omni.getSetByChartId(getParam("id")) ?: throw NotFoundException())
							}

							route("{id}") {
								// get [/management/set/{id}] - return the full set data
								get {
									checkAdmin()
									respOk(getParamSet())
								}

								// patch [/management/set/{id}] - change the fields of the set; return the full set data
								@InternalUse
								patch {
									checkAdmin()
									val set = getParamSet()
									val newValues = receive<Map<String, Any?>>()
									// change and update
									// - lock 'charts' because it can cause headless charts.
									set.reflectApplyChanges(newValues, MongoSet::id, MongoSet::charts)
									omni.updateSet(set)
									// respond
									respOk(set)
								}
							}
						}

						// chart
						route("chart") {
							route("{id}") {
								// get [/management/chart/{id}] - return the full chart data
								get {
									checkAdmin()
									respOk(getParamChart())
								}

								@InternalUse
								patch {
									checkAdmin()
									val chart = getParamChart()
									val newValues = receive<Map<String, Any?>>()
									// change and update
									chart.reflectApplyChanges(newValues, MongoChart::id)
									omni.updateChart(chart)
									// respond
									respOk(chart)
								}
							}
						}
					}
				}

				route("user") {

					// post [/user/register] - register a user; return summary user data
					post("register") {
						val body = receive<Map<String, String>>()
						val username = body.getOrBadRequest("username")
						val password = body.getOrBadRequest("password")

						// do invitation code check
						if(Conf.useInvitationCode) {
							val invitation = body.getOrBadRequest("invitation")
							val result = InvitationCode.useCode(invitation)
							if(!result) throw BadRequestException("Invitation code is invalid")
						}

						respOk(omni.registerUser(username, password).bombify())
					}

					route("{id}") {
						// get [/user/{id}] - return the summary user data
						get {
							respOk(getParamUser())
						}

						// post [/user/{id}/buy-invitation] - return an invitation code if the user can afford
						post("buy-invitation") {
							if(Conf.useInvitationCode || Conf.costInvitationCode < 0) {
								val user = getParamUser()
								if(user.payDiamond(Conf.costInvitationCode)) {
									respOk(InvitationCode.generateCode(user.id))
								} else {
									call.respondJson(
										mapOf("error" to "The user cannot afford the cost of diamond."),
										HttpStatusCode.PaymentRequired
									)
								}
							} else {
								respBadRequest("The server is not vending invitation code now")
							}
						}

						route("best20") {
							// get [/user/{id}/best20/s] - return the list of the best records in score descending
							get("s") {
								respOk(
									getParamUser().getBestPlayRecords(20, 0).toList().map(MongoRecordRanked::bombify)
								)
							}

							// get [/user/{id}/best20/r] - return the list of the best records in R value descending
							get("r") {
								respOk(getParamUser().getBestPlayRecordsR(20, 0).toList().map(MongoRecord::bombify))
							}
						}
					}
				}

				route("set") {
					route("{id}") {
						// get [/set/{id}] - return the summary set data
						get {
							respOk(getParamSet().bombify())
						}

						authenticate { // authentication for reviewer permission
							route("review") {
								// get [/set/{id}/review] - return the full review data
								get {
									checkReviewer()
									respOk(getParamReview().bombify())
								}

								// post [/set/{id}/review] - add a new review; return the full review data
								post {
									checkReviewer()
									val user = getAuthUser()
									val review = getParamReview()

									val data = receive<Map<String, String>>()
									val status = data.getOrBadRequest("status").toBooleanStrictOrNull()
										?: throw ParameterConversionException("status", "Boolean")
									val evaluation = data["evaluation"].orEmpty()
									review.addReview(user.id, status, evaluation)

									respOk(review)
								}

								// post [/set/{id}/review/start] - start a new review on the set; return the full review data
								post("start") {
									checkAdmin() // only administrator can start review
									val set = getParamSet()

									val data = receive<Map<String, String>>()
									val expectedStatus =
										data.getOrConversionException("status") { SetStatus.valueOf(it) }

									set.startReview(expectedStatus)

									respOk(set.getReview())
								}

								// post [/set/{id}/review/end] - end a review; return the result
								post("end") {
									checkAdmin()
									val set = getParamSet()
									val review = getParamReview()

									val data = receive<Map<String, String>>()
									val force = runCatching { // get the pass-in value
										data["status"]?.toBooleanStrict()
									}.onFailure { // throw if convert failed
										throw ParameterConversionException("status", "Boolean", it)
									}.getOrNull() ?: if(omni is MongoProvider) { // if the provider is MongoProvider then auto calculate the result
										with(omni) { review.peekAutoReviewResult() }
									} else { // otherwise use the default value
										true
									}

									set.endReview(force)

									respOk("OK")
								}
							}
						}
					}

					// get [/set/reviews] - return the list of reviews
					get("reviews") {
						checkReviewer()
						respOk(getReviewList().toList().map(MongoReview::bombify))
					}
				}

				route("chart") {
					route("{id}") {
						// get [/chart/{id}] - return the summary chart data
						get {
							respOk(getParamChart().bombify())
						}
					}
				}

			}
		}
	}

	// region: anthentication validation

	class UnauthorizedException : BadRequestException {
		constructor() : super("Request permission is not granted")
		constructor(message: String) : super(message)
	}

	private fun TheCall.getAuthUserOrNull(): MongoUser? {
		val token = call.authentication.principal<UserIdPrincipal>()?.name
		return token?.let { omni.getUserByToken(it) }
	}

	private fun TheCall.getAuthUser(): MongoUser {
		return getAuthUserOrNull() ?: throw UnauthorizedException("Authentication token is invalid.")
	}

	private fun TheCall.isAdmin(): Boolean {
		return getAuthUser().permission.operator
	}

	private fun TheCall.checkAdmin() {
		if(!isAdmin()) throw UnauthorizedException("You don't have enough authorization")
	}

	private fun TheCall.isReviewer(): Boolean {
		return getAuthUser().permission.review
	}

	private fun TheCall.checkReviewer() {
		if(!isReviewer()) throw UnauthorizedException("You don't have Reviewer role")
	}

	// region: path param accessor

	private fun TheCall.getParam(name: String): String {
		return call.parameters[name] ?: throw MissingRequestParameterException(name)
	}

	private fun TheCall.getParamUser(): MongoUser {
		return omni.getUser(getParam("id")) ?: throw NotFoundException()
	}

	private fun TheCall.getParamSet(): MongoSet {
		return omni.getSet(getParam("id")) ?: throw NotFoundException()
	}

	private fun TheCall.getParamChart(): MongoChart {
		return omni.getChart(getParam("id")) ?: throw NotFoundException()
	}

	private fun TheCall.getParamReview(): MongoReview {
		return getParamSet().getReview() ?: throw NotFoundException()
	}

	// region: body param accessor

	private suspend inline fun <reified T : Any> TheCall.receive(): T =
		call.receiveOrNull() ?: throw BadRequestException("Request body content is missing")

	// region: responding

	private suspend fun TheCall.respOk(data: Any?) {
		call.respondJson(data, HttpStatusCode.OK)
	}

	private suspend fun TheCall.respBadRequest(data: Any?) {
		call.respondJson(data, HttpStatusCode.BadRequest)
	}

	private suspend fun TheCall.respBadRequest(message: String) {
		call.respondJson(mapOf("error" to message), HttpStatusCode.BadRequest)
	}

	private suspend fun TheCall.respNotFound(data: Any?) {
		call.respondJson(data, HttpStatusCode.NotFound)
	}

	private suspend fun TheCall.respNotFound(message: String) {
		call.respondJson(mapOf("error" to message), HttpStatusCode.NotFound)
	}

	private suspend fun TheCall.respInternalError(message: String, data: Any? = null, exception: Throwable? = null) {
		val r = buildMap {
			this["message"] to message
			data?.let { this["data"] to it }
			exception?.let { this["stacktrace"] to it.stackTraceToString() }
		}
		call.respondJson(r, HttpStatusCode.InternalServerError)
	}

	private suspend fun TheCall.respAuthenticate() {
		call.respondJson<Any?>(null, HttpStatusCode.Unauthorized)
	}

	// region: util
	private suspend fun <T> T.doEither(onNotNull: suspend T.() -> Unit, onNull: suspend () -> Unit): T? = apply {
		if(this == null) {
			onNull()
		} else {
			onNotNull(this)
		}
	}

	/**
	 * Throw [MissingRequestParameterException] if the requested key is not provided.
	 */
	@Throws(MissingRequestParameterException::class)
	private fun <K, V> Map<K, V>.getOrBadRequest(key: K): V {
		return this[key] ?: throw MissingRequestParameterException("$key")
	}

	private inline fun <K, V, reified R> Map<K, V>.getOrConversionException(key: K, action: (V) -> R?): R {
		val value = getOrBadRequest(key)
		return runCatching {
			action(value) ?: throw NullPointerException()
		}.onFailure {
			throw ParameterConversionException("$key", R::class.java.simpleName, it)
		}.getOrThrow()
	}

	/**
	 * Throw [NotFoundException] if no element in the collection.
	 */
	@Throws(NotFoundException::class)
	private fun <T> Collection<T>.throwNotFoundIfEmpty() = apply {
		if(isEmpty()) throw NotFoundException()
	}

}
