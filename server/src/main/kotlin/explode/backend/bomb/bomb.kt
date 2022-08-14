package explode.backend.bomb

import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowResourceProvider
import explode.explodeConfig
import explode.globalJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

suspend inline fun <reified T> ApplicationCall.respondJson(message: T, status: HttpStatusCode? = null) =
	respondText(globalJson.encodeToString(message), contentType = ContentType.Application.Json, status = status)

@Serializable
data class OkResult<T>(val data: T)

@Serializable
data class BadResult(val error: String)

@Serializable
data class BadResultWithData<T>(val error: String, val data: T)

/**
 * Replace respond with this for not using ContentNegotiation,
 * which seems has conflicts with GraphQL query serialization.
 */
private suspend inline fun <reified T> ApplicationCall.respond(data: T) {
	respondText(globalJson.encodeToString(data))
}

/**
 * Replace respond with this for not using ContentNegotiation,
 * which seems has conflicts with GraphQL query serialization.
 */
private suspend inline fun <reified T> ApplicationCall.respond(status: HttpStatusCode, data: T) {
	respondText(globalJson.encodeToString(data), status = status)
}

fun Application.bombModule(
	acc: IBlowAccessor,
	res: IBlowResourceProvider
) {
	install(CORS) {
		anyHost()
		allowHeader("X-Token")
		allowHeader(HttpHeaders.AccessControlAllowOrigin)
	}

	install(Authentication) {
		basic {
			realm = "Access"
			validate {
				runCatching {
					val tok = acc.loginUser(it.name, it.password).token
					UserIdPrincipal(tok)
				}.getOrNull()
			}
		}
	}

	routing {

		if(explodeConfig.enableBombBackend) {
			route("bomb") {
				if(explodeConfig.enableBombFrontend) {
					static("/") {
						staticBasePackage = "static"
						resources(".")
						defaultResource("index.html")
					}
				}

				get {
					call.respondRedirect("bomb/", true)
				}

				bombUpload(acc, res)
				bombUserCrud(acc, res)
				bombSetCrud(acc)
				bombChartCrud(acc)

				authenticate {
					get("auth") {
						val principal = call.authentication.principal
						if(principal != null) {
							if(principal is UserIdPrincipal) {
								call.respondJson(OkResult(principal.name))
							} else {
								call.respondJson(OkResult("User principal found but not supported."))
							}
						} else {
							call.respondJson(BadResult("No authentication provided."), HttpStatusCode.Unauthorized)
						}
					}
				}
			}
		}
	}

}

@Serializable
data class PostReviewSet(
	val accepted: Boolean,
	val rejectMessage: String? = null
)

internal fun <T> T.onNull(block: () -> Unit): T? {
	if(this == null) { block() }
	return this
}

internal fun <T> T.onNotNull(block: () -> Unit): T? {
	if(this != null) { block() }
	return this
}
