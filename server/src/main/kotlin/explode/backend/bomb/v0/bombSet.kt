package explode.backend.bomb.v0

import explode.backend.bomb.v0.model.BombPayloadSetPatch
import explode.backend.payload
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

		post("by-chart/{chartId}") {
			val cid = call.parameters["chartId"] ?: return@post call.respondJson(
				BadResult("Bad Request: Missing ID."),
				HttpStatusCode.BadRequest
			)
			val set = acc.getSetByChartId(cid) ?: return@post call.respondJson(
				BadResult("Bad Request: Invalid ID."),
				HttpStatusCode.NotFound
			)
			call.respondJson(OkResult(set))
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

			post {
				val sid = call.parameters["id"] ?: return@post call.respondJson(
					BadResult("Bad Request: Missing ID."),
					HttpStatusCode.BadRequest
				)
				val set = acc.getSet(sid) ?: return@post call.respondJson(
					BadResult("Bad Request: Invalid ID."),
					HttpStatusCode.NotFound
				)
				val patch = payload<BombPayloadSetPatch>()

				val errorMessages = mutableListOf<String>()

				patch.musicName?.let { set.musicName = it }
				patch.composerName?.let { set.composerName = it }
				patch.noterId?.let {
					acc.getUser(it).onNull { errorMessages += "Invalid User ID: $it" }
						.onNotNull {
							set.noterId = it
						}

				}
				patch.introduction?.let { set.introduction = it }
				patch.price?.let { set.price = it }
				patch.status?.let { set.status = it }
				patch.charts?.forEach {
					set.charts.clear()
					acc.getChart(it).onNull { errorMessages += "Invalid Chart ID: $it" }
						.onNotNull {
							set.charts += it
						}
				}

				acc.updateSet(set)
				if(errorMessages.isEmpty()) {
					call.respondJson(OkResult(set))
				} else {
					call.respondJson(BadResultWithData(errorMessages.joinToString("\n"), set))
				}
			}

			post("review") {
				TODO("The logic of reviewing the set.")
			}
		}
	}
}