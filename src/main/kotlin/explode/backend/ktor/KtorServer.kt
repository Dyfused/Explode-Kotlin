package explode.backend.ktor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

class KtorServer {

	private val mapper = jacksonObjectMapper()
	private val ktorGQLServer = getGraphQLServer(mapper)

	suspend fun handle(appCall: ApplicationCall) {
		val result = ktorGQLServer.execute(appCall.request)

		if(result != null) {
			val json = mapper.writeValueAsString(result)
			appCall.response.call.respond(json)
		} else {
			appCall.response.call.respond(HttpStatusCode.BadRequest, "Invalid request")
		}
	}
}