package explode.backend.bomb.v0

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*

@Suppress("FunctionName")
suspend fun PipelineContext<Unit, ApplicationCall>.TODO() =
	call.respondJson(OkResult("TODO method requested."))

@Suppress("FunctionName")
suspend fun PipelineContext<Unit, ApplicationCall>.TODO(reason: String) =
	call.respondJson(OkResult("TODO method requested: $reason"))

/**
 * Get the value of specified value in paramters.
 * Null value in parameters responds with Bad Request. Null value of block responds with Not Found.
 */
suspend fun <T> PipelineContext<Unit, ApplicationCall>.param(paramName: String, block: (String) -> T?): Result<T> {
	val valueParam = call.parameters[paramName]
	return if(valueParam != null) {
		val value = block(valueParam)
		if(value != null) {
			Result.success(value)
		} else {
			val err = "Invalid parameter: $paramName"
			call.respondJson(BadResult(err), HttpStatusCode.NotFound)
			Result.failure(NullPointerException(err))
		}
	} else {
		val err = "Missing parameter: $paramName"
		call.respondJson(BadResult(err), HttpStatusCode.BadRequest)
		Result.failure(NullPointerException(err))
	}
}