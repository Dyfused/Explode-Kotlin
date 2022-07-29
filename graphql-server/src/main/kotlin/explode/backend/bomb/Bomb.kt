package explode.backend.bomb

import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowResourceProvider
import explode.dataprovider.provider.mongo.MongoProvider
import explode.dataprovider.serializers.OffsetDateTimeSerializer
import explode.explodeConfig
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

suspend inline fun <reified T> ApplicationCall.respondJson(message: T, status: HttpStatusCode? = null) =
	respondText(Json.encodeToString(message), contentType = ContentType.Application.Json, status = status)

@Serializable
data class PostSetAndChartMeta(
	val title: String,
	val composerName: String,
	val noterName: String,

	val chartMeta: List<PostChartData>,
	val musicFileName: String,

	val coverFileName: String?,
	val previewFileName: String?,
	val storePreviewFileName: String?
)

@Serializable
data class PostChartData(
	val chartFileName: String,
	val chartDifficultyClass: Int,
	val chartDifficultyValue: Int
)

private val j = Json {
	ignoreUnknownKeys = true
	serializersModule = SerializersModule {
		contextual(OffsetDateTimeSerializer)
	}
}

@Serializable
data class OkResult<T>(val data: T)

@Serializable
data class BadResult<T>(val error: String, val data: T?)

private fun badResult(error: String): BadResult<Nothing> = BadResult(error, null)



/**
 * Replace respond with this for not using ContentNegotiation,
 * which seems has conflicts with GraphQL query serialization.
 */
private suspend inline fun <reified T> ApplicationCall.respond(data: T) {
	respondText(j.encodeToString(data))
}

/**
 * Replace respond with this for not using ContentNegotiation,
 * which seems has conflicts with GraphQL query serialization.
 */
private suspend inline fun <reified T> ApplicationCall.respond(status: HttpStatusCode, data: T) {
	respondText(j.encodeToString(data), status = status)
}

fun Application.bombModule(
	data: IBlowAccessor,
	res: IBlowResourceProvider
) {
	install(CORS) {
		anyHost()
	}

	routing {

		if(explodeConfig.enableBombFrontend && explodeConfig.enableBombBackend) {
			static(explodeConfig.bombFrontendPath) {
				staticBasePackage = "static"
				resources(".")
				defaultResource("index.html")
			}
		}

		if(explodeConfig.enableBombBackend) {
			route("bomb") {

				post("upload") {
					try { // fast-fail

						lateinit var meta: PostSetAndChartMeta

						val uploadedData = mutableMapOf<String, ByteArray>()

						call.receiveMultipart().forEachPart { part ->
							val partName = part.name ?: error("MULTIPART_NAME_MISSING")
							when(part) {
								is PartData.FileItem -> {
									uploadedData[partName] = part.provider().readBytes()
								}
								is PartData.FormItem -> {
									if(partName == "chart-data") {
										meta = j.decodeFromString(part.value)
									}
								}
								else -> {}
							}
						}

						// 检查音乐和谱面
						if(meta.musicFileName !in uploadedData.keys) error("MUSIC_MISSING")
						meta.chartMeta.forEach { if(it.chartFileName !in uploadedData.keys) error("CHART_MISSING: ${it.chartFileName}") }

						// 存入数据库
						val s = data.buildChartSet(
							setTitle = meta.title,
							composerName = meta.composerName,
							noterUser = data.getUserByName(meta.noterName) ?: data.serverUser,
							isRanked = false,
							coinPrice = 0,
							introduction = "",
							needReview = false
						) {
							meta.chartMeta.forEach { (file, diffClass, diffValue) ->
								addChart(
									diffClass,
									diffValue
								).apply {
									res.addChartResource(_id, uploadedData[file]!!)
								}
							}
						}.apply {
							res.addMusicResource(_id, uploadedData[meta.musicFileName]!!)
							meta.previewFileName?.let { uploadedData[it] }?.let { res.addPreviewResource(_id, it) }
							meta.coverFileName?.let { uploadedData[it] }?.let { res.addSetCoverResource(_id, it) }
							meta.storePreviewFileName?.let { uploadedData[it] }
								?.let { res.addStorePreviewResource(_id, it) }
						}

						call.respondJson(mapOf("data" to s))
					} catch(ex: IllegalStateException) {
						call.respondJson(mapOf("error" to ex.message))
					}
				}

				route("user") {
					route("{userId}") {
						get {
							val uid = call.parameters["userId"] ?: return@get call.respond(
								HttpStatusCode.BadRequest,
								badResult("Undefined userId.")
							)
							val user = data.getUser(uid) ?: return@get call.respond(
								HttpStatusCode.NotFound,
								badResult("Requested user not found.")
							)
							call.respond(OkResult(user))
						}

						get("last20") {
							val uid = call.parameters["userId"] ?: return@get call.respond(
								HttpStatusCode.BadRequest,
								badResult("Undefined userId.")
							)
							val user = data.getUser(uid) ?: return@get call.respond(
								HttpStatusCode.NotFound,
								badResult("Requested user not found.")
							)
							with(data) {
								val d = user.getLastPlayRecords(20, 0).toList()
								call.respond(OkResult(d))
							}
						}

						get("best20") {
							val uid = call.parameters["userId"] ?: return@get call.respond(
								HttpStatusCode.BadRequest,
								badResult("Undefined userId.")
							)
							val user = data.getUser(uid) ?: return@get call.respond(
								HttpStatusCode.NotFound,
								badResult("Requested user not found.")
							)
							with(data) {
								val d = user.getBestPlayRecords(20, 0).toList()
								call.respond(OkResult(d))
							}
						}

						post("update-r") {
							val uid = call.parameters["userId"] ?: return@post call.respond(
								HttpStatusCode.BadRequest,
								badResult("Undefined userId.")
							)
							val user = data.getUser(uid) ?: return@post call.respond(
								HttpStatusCode.NotFound,
								badResult("Requested user not found.")
							)
							if(data is MongoProvider) {
								with(data) {
									user.updatePlayerRValue()
									call.respond(OkResult(user))
								}
							} else {
								call.respond(HttpStatusCode.ServiceUnavailable, badResult("Unsupported operation."))
							}
						}
					}
				}

				route("set") {
					route("{setId}") {
						get {
							val sid = call.parameters["setId"] ?: return@get call.respond(
								HttpStatusCode.BadRequest,
								badResult("Undefined setId.")
							)
							val set = runCatching { data.getSet(sid) }.onFailure {
								return@get call.respond(
									HttpStatusCode.NotFound,
									badResult("Requested set not found.")
								)
							}.getOrThrow()
							call.respond(OkResult(set))
						}
					}
				}
			}
		}
	}
}