package explode.backend

import explode.backend.bomb.v1.backend.bombLogger
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

internal suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.payload(): T {
	val text = call.receiveText()
	return runCatching {
		globalJson.decodeFromString<T>(text)
	}.onFailure {
		bombLogger.error("Unable to parse text into JSON: \n$text")
	}.getOrThrow()
}