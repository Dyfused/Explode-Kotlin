package explode.backend.bomb

import explode.dataprovider.provider.*
import explode.explodeConfig
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
	respondText(Json.encodeToString(message), contentType = ContentType.Application.Json, status = status)

@Serializable
data class PostSetAndChartMeta(
	val title: String,
	val composerName: String,
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

private val json = Json {
	ignoreUnknownKeys = true
}

fun Application.bombModule(
	data: IBlowDataProvider,
	res: IBlowResourceProvider,
	acc: IBlowAccessor
) {
	install(CORS) {
		anyHost()
	}

	routing {

		if(explodeConfig.enableBombFrontend && explodeConfig.enableBombBackend) {
			static(explodeConfig.bombFrontendPath) {
				staticBasePackage = "static"
				resources(".")
				defaultResource("index.html")
			}
		}

		if(explodeConfig.enableBombBackend) {
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
									if(partName == "chart-data") {
										meta = json.decodeFromString(part.value)
									}
								}
								else -> {}
							}
						}

						// 检查音乐和谱面
						if(meta.musicFileName !in uploadedData.keys) error("MUSIC_MISSING")
						meta.chartMeta.forEach { if(it.chartFileName !in uploadedData.keys) error("CHART_MISSING: ${it.chartFileName}") }

						// 存入数据库
						val s = acc.buildChartSet(
							setTitle = meta.title,
							composerName = meta.composerName,
							noterUser = data.getUserByName(meta.noterName) ?: data.emptyUser,
							isRanked = false,
							coinPrice = 0,
							introduction = "",
							needReview = false
						) {
							meta.chartMeta.forEach { (file, diffClass, diffValue) ->
								addChart(
									diffClass,
									diffValue
								).apply {
									res.addChartResource(_id, uploadedData[file]!!)
								}
							}
						}.apply {
							res.addMusicResource(_id, uploadedData[meta.musicFileName]!!)
							meta.previewFileName?.let { uploadedData[it] }?.let { res.addPreviewResource(_id, it) }
							meta.coverFileName?.let { uploadedData[it] }?.let { res.addSetCoverResource(_id, it) }
							meta.storePreviewFileName?.let { uploadedData[it] }?.let { res.addStorePreviewResource(_id, it) }
						}

						call.respondJson(mapOf("data" to s))
					} catch(ex: IllegalStateException) {
						call.respondJson(mapOf("error" to ex.message))
					}
				}
			}
		}
	}
}