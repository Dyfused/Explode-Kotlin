package explode.backend

import explode.dataprovider.provider.fail
import explode.globalJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

suspend inline fun <reified T> ApplicationCall.respondJson(data: T, status: HttpStatusCode) {
	this.respondText(contentType = ContentType.Application.Json, status) { globalJson.encodeToString(data) }
}

suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.payload(): T {
	return runCatching {
		globalJson.decodeFromString<T>(call.receiveText())
	}.onFailure { // response before throw
		call.respondJson(
			buildMap {
				this["error"] = it.message.orEmpty()
			},
			HttpStatusCode.BadRequest
		)
	}.getOrElse { // throw a console-friendly exception
		fail(it.message)
	}
}