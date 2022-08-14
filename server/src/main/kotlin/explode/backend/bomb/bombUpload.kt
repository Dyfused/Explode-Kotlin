package explode.backend.bomb

import explode.backend.checkAuthentication
import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowResourceProvider
import explode.globalJson
import explode.utils.getOrThrow
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Bomb-Uploader")

fun Route.bombUpload(data: IBlowAccessor, res: IBlowResourceProvider) = authenticate {

	post("upload") {
		val auth = checkAuthentication(data::getUserByToken)

		if(!auth.isSuccess) {
			// invalid token
			return@post
		} else {
			val u = auth.getOrThrow()
			logger.info("Received upload request from User(${u.username}, #${u._id}): ${u.token}")
		}

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
							meta = globalJson.decodeFromString(part.value)
						}
					}

					else -> {}
				}
			}

			// 检查音乐和谱面
			if(meta.musicFileName !in uploadedData.keys) error("MUSIC_MISSING")
			meta.chartMeta.forEach { if(it.chartFileName !in uploadedData.keys) error("CHART_MISSING: ${it.chartFileName}") }

			// 存入数据库
			val s = data.buildChartSet(
				setTitle = meta.title,
				composerName = meta.composerName,
				noterUser = auth.getOrThrow(),
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
				meta.storePreviewFileName?.let { uploadedData[it] }
					?.let { res.addStorePreviewResource(_id, it) }
			}

			call.respondJson(OkResult(s))
			logger.info("Successfully uploaded Set<${s.musicName}>(${s._id}) with ${s.charts.size} charts.")
		} catch(ex: IllegalStateException) {
			call.respondJson(BadResult(ex.message.orEmpty()), HttpStatusCode.BadRequest)
		}
	}

}

@Serializable
data class PostSetAndChartMeta(
	val title: String,
	val composerName: String,

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