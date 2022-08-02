package explode.backend.bomb

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
//				val sid = call.parameters["id"] ?: return@post call.respondJson(
//					BadResult("Bad Request: Missing ID."),
//					HttpStatusCode.BadRequest
//				)
//				val set = acc.getSet(sid) ?: return@post call.respondJson(
//					BadResult("Bad Request: Invalid ID."),
//					HttpStatusCode.NotFound
//				)
//				val patch = payload<BombPayloadSetPatch>()
//
//				val errorMessages = mutableListOf<String>()
//
//				patch.musicName?.let { set.musicName = it }
//				patch.composerName?.let { set.composerName = it }
//				patch.noterId?.let {
//					acc.getUser(it).onNull { errorMessages += "Invalid User ID: $it" }
//						.onNotNull {
//							set.noterId = it
//						}
//
//				}
//				patch.introduction?.let { set.introduction = it }
//				patch.price?.let { set.price = it }
//				patch.status?.let { set.status = it }
//				patch.charts?.forEach {
//					set.charts.clear()
//					acc.getChart(it).onNull { errorMessages += "Invalid Chart ID: $it" }
//						.onNotNull {
//							set.charts += it
//						}
//				}
//
//				acc.updateSet(set)
//				if(errorMessages.isEmpty()) {
//					call.respondJson(OkResult(set))
//				} else {
//					call.respondJson(BadResultWithData(errorMessages.joinToString("\n"), set))
//				}

				call.respondJson(OkResult("FQ"))
			}
		}
	}
}