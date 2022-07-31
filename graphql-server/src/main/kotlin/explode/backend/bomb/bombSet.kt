package explode.backend.bomb

import explode.dataprovider.provider.IBlowAccessor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.bombSetCrud(acc: IBlowAccessor) {

	route("set") {

		get {
			val lim = (call.parameters["limit"]?.toIntOrNull() ?: 10).takeIf { it != 0 }
			val skip = call.parameters["skip"]?.toIntOrNull()
			call.respondJson(OkResult(acc.getSets(lim, skip).toList()))
		}

		route("{id}") {
			get {
				val sid = call.parameters["id"] ?: return@get call.respondJson(
					BadResult("Bad Request: Missing ID."),
					HttpStatusCode.BadRequest
				)
				val set = acc.getSet(sid) ?: return@get call.respondJson(
					BadResult("Bad Request: Invalid ID."),
					HttpStatusCode.NotFound
				)
				call.respondJson(OkResult(set))
			}
		}
	}
}