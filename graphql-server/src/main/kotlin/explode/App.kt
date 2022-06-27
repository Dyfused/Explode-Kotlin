package explode

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import explode.backend.console.ExplodeConsole
import explode.dataprovider.provider.*
import explode.dataprovider.provider.mongo.MongoProvider
import explode.utils.RenaReader
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

private val mainLogger = LoggerFactory.getLogger("Explode")

fun main(args: Array<String>) {

	disableMongoLogging()
	// disableKtorLogging()

	mainLogger.info("Explode ($GameVersion)")

	val m = MongoProvider()

	blow = m.provider
	blowAccess = m
	blowResource = m

	val operation = args.getOrNull(0)

	Json {
		ignoreUnknownKeys = true
	}

	ExplodeConsole.loop()

	when(operation) {
		"backend", null -> startKtorServer(args)
		"rena" -> formatByRenaIndexList(args.getOrNull(1) ?: return println("Invalid RenaIndexList path"))
		else -> println("Unknown subcommand: $operation")
	}

	mainLogger.info("Exploded.")
}

private fun disableMongoLogging() {
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("org.mongodb.driver").level = Level.WARN
}

private fun disableKtorLogging() {
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("Application").level = Level.WARN
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

	renas.forEach { (setId, setName, soundPath, coverPath, previewPath, _, composerName, introduction, charts) ->
		val soundFile = dataFolder.resolve(soundPath)
		val coverFile = dataFolder.resolve(coverPath)
		val previewFile = dataFolder.resolve(previewPath)

		if(!soundFile.exists()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失音乐文件：$soundPath，跳过。")
		if(!coverFile.exists()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失封面文件：$coverPath，跳过。")
		if(!previewFile.exists()) return@forEach mainLogger.warn("谱 $setName（${setId}）丢失预览音乐文件：$previewPath，跳过。")

		val s = runCatching {
			p.buildChartSet(setName, composerName, p.officialUser, false, 0, introduction, true) {
				charts.forEach { (hardClass, hardLevel, chartPath) ->
					val chartFile = dataFolder.resolve(chartPath)
					if(chartFile.exists()) {
						val c = addChart(RenaReader.getHardClassNum(hardClass), hardLevel)
						p.addChartResource(c._id, chartFile.readBytes())
						mainLogger.info("找到谱面（${c.chartName}）：${c._id}")
					} else {
						mainLogger.warn("找不到谱（${setName}）的谱面文件：$chartFile，跳过。")
					}
				}
			}
		}.onSuccess {
			p.addMusicResource(it._id, soundFile.readBytes())
			p.addSetCoverResource(it._id, coverFile.readBytes())
			p.addPreviewResource(it._id, previewFile.readBytes())
		}.onFailure {
			mainLogger.error("", it)
		}.getOrThrow()

		mainLogger.info("成功添加谱 $setName（$setId）及其谱面 ${s.chart.map { "${it._id}(${it.difficultyClass}/${it.difficultyValue})" }}（共${s.chart.size}张）。")
		mainLogger.info(s.toString())
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
lateinit var blow: IBlowDataProvider

lateinit var blowAccess: IBlowAccessor

lateinit var blowResource: IBlowResourceProvider