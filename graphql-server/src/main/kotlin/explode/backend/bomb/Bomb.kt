package explode.backend.bomb

import explode.blowAccess
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

suspend inline fun <reified T> ApplicationCall.respondJson(message: T, status: HttpStatusCode? = null) =
	if(status != null) {
		respond(status, Json.encodeToString(message))
	} else {
		respond(Json.encodeToString(message))
	}

@Serializable
data class PostSetAndChartMeta(
	val title: String,
	val noterName: String,

	val chartMeta: List<PostChartData>,
	val musicFileName: String,

	val coverFileName: String?,
	val previewFileName: String?,
	val storePreviewFileName: String?
)

@Serializable
data class PostChartData(
	val chartFileName: String,
	val chartDifficultyClass: Int,
	val chartDifficultyValue: Int
)

fun Application.bombModule() {
	install(CORS) {
		anyHost()
	}

	routing {

		static("/") {
			staticBasePackage = "static"
			resources(".")
			defaultResource("index.html")
		}

		route("bomb") {

			post("upload") {
				try { // fast-fail

					lateinit var meta: PostSetAndChartMeta

					val uploadedData = mutableMapOf<String, ByteArray>()

					call.receiveMultipart().forEachPart { part ->
						val partName = part.name ?: error("MULTIPART_NAME_MISSING")
						when(part) {
							is PartData.FileItem -> {
								uploadedData[partName] = part.provider().readBytes()
							}
							is PartData.FormItem -> {
								meta = Json.decodeFromString(part.value)
							}
							else -> {}
						}
					}

					// 检查音乐和谱面
					if(meta.musicFileName !in uploadedData.keys) error("MUSIC_MISSING")
					meta.chartMeta.forEach { if(it.chartFileName !in uploadedData.keys) error("CHART_MISSING: ${it.chartFileName}") }

					// 存入数据库
					blowAccess

				} catch(ex: IllegalStateException) {
					call.respondJson(mapOf("error" to ex.message))
				}
			}
		}
	}
}