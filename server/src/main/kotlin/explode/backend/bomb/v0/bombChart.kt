package explode.backend.bomb.v0

import explode.dataprovider.provider.IBlowAccessor
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.bombChartCrud(acc: IBlowAccessor) {

	route("chart") {

		get {
			call.respondJson(BadResult("Not Supported"), HttpStatusCode.MethodNotAllowed)
		}

		route("{id}") {
			get {
				val sid = call.parameters["id"] ?: return@get call.respondJson(
					BadResult("Bad Request: Missing ID."),
					HttpStatusCode.BadRequest
				)
				val chart = acc.getChart(sid) ?: return@get call.respondJson(
					BadResult("Bad Request: Invalid ID."),
					HttpStatusCode.NotFound
				)
				call.respondJson(OkResult(chart))
			}

			post {
				call.respondJson(OkResult("FQ"))
			}
		}
	}
}