package explode.backend

import explode.globalJson
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.decodeFromString

suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.payload() =
	globalJson.decodeFromString<T>(call.receiveText())