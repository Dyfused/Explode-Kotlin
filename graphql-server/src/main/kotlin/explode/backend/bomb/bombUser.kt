package explode.backend.bomb

import explode.backend.payload
import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowResourceProvider
import explode.dataprovider.provider.mongo.MongoProvider
import explode.globalJson
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

fun Route.bombUserCrud(acc: IBlowAccessor, res: IBlowResourceProvider) {

	@Serializable
	data class BombPostLogin(val username: String, val password: String)

	route("user") {

		post("search/{username}") {
			val username = call.parameters["username"] ?: return@post call.respondJson(
				BadResult("Undefined username."),
				HttpStatusCode.BadRequest
			)
			val user = acc.getUserByName(username) ?: return@post call.respondJson(
				BadResult("Not Found: Request User."),
				HttpStatusCode.NotFound
			)
			call.respondJson(OkResult(user))
		}

		post("login") {
			val d = payload<BombPostLogin>()
			kotlin.runCatching {
				acc.loginUser(d.username, d.password)
			}.onSuccess {
				call.respondJson(OkResult(it))
			}.onFailure {
				call.respondJson(BadResult(it.message.orEmpty()), HttpStatusCode.BadRequest)
			}
		}

		post("register") {
			val d = payload<BombPostLogin>()
			kotlin.runCatching {
				acc.registerUser(d.username, d.password)
			}.onSuccess {
				call.respondJson(OkResult(it))
			}.onFailure {
				call.respondJson(BadResult(it.message.orEmpty()), HttpStatusCode.BadRequest)
			}
		}

		route("{userId}") {
			get {
				val uid = call.parameters["userId"] ?: return@get call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				val user = acc.getUser(uid) ?: return@get call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)
				call.respondJson(OkResult(user))
			}

			get("last20") {
				val uid = call.parameters["userId"] ?: return@get call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				val user = acc.getUser(uid) ?: return@get call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)
				with(acc) {
					val d = user.getLastPlayRecords(20, 0).toList()
					call.respondJson(OkResult(d))
				}
			}

			get("best20") {
				val uid = call.parameters["userId"] ?: return@get call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				val user = acc.getUser(uid) ?: return@get call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)
				with(acc) {
					val d = user.getBestPlayRecords(20, 0).toList()
					call.respondJson(OkResult(d))
				}
			}

			get("best20r") {
				val uid = call.parameters["userId"] ?: return@get call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				acc.getUser(uid) ?: return@get call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)
				with(acc) {
					val d = (this as? MongoProvider)?.getUserBestR20(uid)?.toList() ?: emptyList()
					call.respondJson(OkResult(d))
				}
			}

			post("update-r") {
				val uid = call.parameters["userId"] ?: return@post call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				val user = acc.getUser(uid) ?: return@post call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)
				if(acc is MongoProvider) {
					with(acc) {
						user.updatePlayerRValue()
						call.respondJson(OkResult(user))
					}
				} else {
					call.respondJson(BadResult("Unsupported operation."), HttpStatusCode.ServiceUnavailable)
				}
			}

			post("avatar") {
				val uid = call.parameters["userId"] ?: return@post call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				acc.getUser(uid) ?: return@post call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)
				var success = false
				call.receiveMultipart().forEachPart {
					if(it.name == "avatar" && it is PartData.FileItem) {
						res.addUserAvatarResource(uid, it.provider().readBytes())
						success = true
					}
				}
				if(success) {
					call.respondJson(OkResult("Done."))
				} else {
					call.respondJson(BadResult("Avatar not found."), HttpStatusCode.BadRequest)
				}
			}

			post {
				val uid = call.parameters["userId"] ?: return@post call.respondJson(
					BadResult("Undefined userId."),
					HttpStatusCode.BadRequest
				)
				val user = acc.getUser(uid) ?: return@post call.respondJson(
					BadResult("Requested user not found."),
					HttpStatusCode.NotFound
				)

				val d = globalJson.decodeFromString<JsonElement>(call.receiveText())
				d.jsonObject["coin"]?.jsonPrimitive?.intOrNull?.let { user.coin = it }
				d.jsonObject["diamond"]?.jsonPrimitive?.intOrNull?.let { user.diamond = it }
				d.jsonObject["password"]?.jsonPrimitive?.contentOrNull?.let { user.password = it }
				d.jsonObject["permission"]?.jsonObject?.get("review")?.jsonPrimitive?.booleanOrNull?.let { user.permission.review = it }

				acc.updateUser(user)

				call.respondJson(OkResult(user))
			}
		}
	}

//	route("user") {
//
//		get {
//			call.respond(HttpStatusCode.Forbidden, badResult("Access Denied"))
//		}
//
//		param("{uid}") {
//
//			suspend fun PipelineContext<Unit, ApplicationCall>.receiveUser(): MongoUser? {
//				val uid = call.receiveParameters()["uid"] ?: return run {
//					call.respond(
//						HttpStatusCode.BadRequest,
//						badResult("Invailid Request: Missing UID.")
//					)
//					null
//				}
//				return data.getUser(uid) ?: run {
//					call.respond(
//						HttpStatusCode.NotFound,
//						"Not Found: Invalid UID."
//					)
//					null
//				}
//			}
//
//			get {
//				receiveUser()?.let { u ->
//					call.respond(HttpStatusCode.OK, u)
//				}
//			}
//
//			post<MongoUser> {
//				receiveUser()?.let { u ->
//					if(it._id == u._id) {
//						println("Uploading user data: $it")
//						data.updateUser(it)
//					}
//				}
//			}
//
//		}
//	}
}