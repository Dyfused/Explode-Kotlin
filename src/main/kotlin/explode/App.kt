package explode

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.mongodb.client.model.UpdateOptions
import explode.blow.provider.IBlowFullProvider
import explode.blow.provider.mongo.MongoProvider
import explode.blow.provider.mongo.MongoProvider.*
import explode.utils.RenaReader
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.litote.kmongo.updateOne
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

private val mainLogger = LoggerFactory.getLogger("Explode")

fun main(args: Array<String>) {

	disableMongoLogging()

	mainLogger.info("Explode ($GameVersion)")

	val operation = args.getOrNull(0)

	Json {
		ignoreUnknownKeys = true
	}

	when(operation) {
		"backend", null -> startKtorServer(args)
		"rena" -> formatByRenaIndexList(args.getOrNull(1) ?: return println("Invalid RenaIndexList path"))
	}

	mainLogger.info("Exploded.")
}

private fun disableMongoLogging() {
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("org.mongodb.driver").level = Level.WARN
}

private fun startKtorServer(args: Array<String>) {
	EngineMain.main(args)
}

private fun formatByRenaIndexList(renaPath: String) {

	val renaIndex = File(renaPath)
	if(!renaIndex.exists() || !renaIndex.isFile) error("Invalid RenaIndexList.")
	val dataFolder = renaIndex.parentFile.resolve("Charts")
	if(!dataFolder.exists() || !dataFolder.isDirectory) error("Invalid DataFolder.")

	val p = MongoProvider()

	println("All pre-check passed.")

	val renas = RenaReader.execute(renaIndex.readLines())

	println("Rena data loaded.")

	renas.forEach { (setId, setName, soundPath, coverPath, previewPath, noterName, composerName, introduction, charts) ->
		val soundFile = dataFolder.resolve(soundPath)
		val coverFile = dataFolder.resolve(coverPath)
		val previewFile = dataFolder.resolve(previewPath)

		if(!soundFile.exists()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失音乐文件：$soundPath，跳过。")
		if(!coverFile.exists()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失封面文件：$coverPath，跳过。")
		if(!previewFile.exists()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失预览音乐文件：$previewPath，跳过。")

		val dcs = charts.mapNotNull { (hardClass, hardLevel, chartPath) ->
			val chartFile = dataFolder.resolve(chartPath)
			if(chartFile.exists()) {
				val noter = p.getUser("", noterName) ?: p.registerUser(noterName, "${Random.nextInt()}")
				val chart = p.createNewChart(
					noter,
					"${setName}_${hardClass}",
					0,
					composerName,
					RenaReader.getHardClassNum(hardClass),
					hardLevel
				)
				p.chartFiles.updateOne(IdToFile(chart._id, chartFile), UpdateOptions().upsert(true))
				mainLogger.info("创建谱面（${setName}_${hardClass}）：${chart._id}")
				chart
			} else {
				mainLogger.warn("找不到谱（${setName}）的谱面文件：$chartFile，跳过。")
				null
			}
		}

		if(dcs.isEmpty()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失所有谱面文件，跳过。")

		p.musicFiles.updateOne(IdToFile(setId, soundFile), UpdateOptions().upsert(true))
		p.coverFiles.updateOne(IdToFile(setId, coverFile), UpdateOptions().upsert(true))
		p.previewFiles.updateOne(IdToFile(setId, previewFile), UpdateOptions().upsert(true))

		val set = p.createNewSet(introduction, 0, noterName, setName, composerName, dcs, specifiedId = setId)

		mainLogger.info("成功添加谱 $setName（$setId）及其谱面 ${dcs.map { "${it.chartName}(${it.difficultyBase}/${it.difficultyValue})" }}（共${dcs.size}张）。")
		mainLogger.info(set.toString())
	}
}

/**
 * set to 'true' for following behaviors:
 *   - print every GraphQL request body
 */
const val DebugMode = true

const val GameVersion = 81

/**
 * This field is used to construct the Schema.
 */
val blow: IBlowFullProvider get() = MongoProvider()