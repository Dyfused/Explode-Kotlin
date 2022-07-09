package explode.rena

import cn.hutool.core.util.ZipUtil
import explode.dataprovider.provider.mongo.MongoProvider
import explode.rena.model.loadAsRenaStandardPack
import java.io.*
import java.nio.channels.FileChannel
import java.nio.file.*
import kotlin.io.path.pathString
import kotlin.system.exitProcess

var useInteractMode = false

private val HelpMessage = """
	RenaApp d'Explode.
	
	You've started RenaApp in wrong arguments.
	
	java -jar ${File(RenaReader::class.java.protectionDomain.codeSource.location.toURI()).path} \
	rena_index_list.txt \
	--mongodb=mongodb://localhost:27017 \
	--local=rena_export.zip \
	--debug
	
	[--mongodb] or [-m] refers to export the data of Rena into the Database where the value linked to.
		(Default: mongodb://localhost:27017)
		
	[--local] or [-l] refers to export the data of Rena into a Zip file where the value locating to.
		(Default: rena_export.zip)
		
	[--debug] or [-d] refers to show all the logs including debug level, for debugging or stacktracing.
	
	*You must set either [--local] or [--mongodb].
""".trimIndent()

private val ArgParsedMessage
	get() = """
	========================
	Rena: ${if(::renaFile.isInitialized) renaFile.path else "~ERROR~"}
	Export Data to: ${if(shouldImportToDatabase) databaseConnStr else "NONE"}
	Export File to: ${if(shouldExportToZipFile) outputZipPath else "NONE"}
	========================
""".trimIndent()

fun main(args: Array<String>) {
	readArgs(args)

	println(ArgParsedMessage)

	val renaResult =
		runCatching { RenaReader.readAndResolve(renaFile) }.onFailure { mainLogger.error(it.message, it) }.getOrNull()
			?: return

	if(shouldImportToDatabase) {
		exportToDatabase(renaResult)
	}
	if(shouldExportToZipFile) {
		exportToZip(renaResult)
	}
}

lateinit var renaFile: File

lateinit var databaseConnStr: String

lateinit var outputZipPath: String

val shouldImportToDatabase: Boolean get() = ::databaseConnStr.isInitialized
val shouldExportToZipFile: Boolean get() = ::outputZipPath.isInitialized

private fun readArgs(args: Array<String>) {
	catchFailure(
		failBlock = {
			println(HelpMessage)
			println("------------------\n")
			println(it)
			exitProcess(1)
		}
	) {
		args.forEachIndexed { index, s ->
			if(index == 0 && !(s startsWith "-" || s startsWith "--")) { // Must be Rena
				val rnf = File(s)
				if(!rnf.exists()) fastfail("Rena doesn't exists. ($rnf)")
				renaFile = rnf
			} else {
				val sp = s.split("=", limit = 2)
				val k = sp.getOrNull(0)
				val v = sp.getOrNull(1)

				when(k) {
					"-m", "--mongodb", "--database" -> {
						databaseConnStr = v ?: "mongodb://localhost:27017"
					}
					"-l", "--local", "--renastandardpack" -> {
						outputZipPath = v ?: "./out.rsp.zip"
					}
					// hidden parameters
					"--use-system-proxy" -> {
						System.setProperty("java.net.useSystemProxies", "true")
						mainLogger.info("Proxy enabled")
					}
				}
			}
		}

		if(!::renaFile.isInitialized) fastfail("Rena has not been set!")
		if(!::databaseConnStr.isInitialized && !::outputZipPath.isInitialized)
			fastfail("Neither [import] nor [export] has not been set!")
	}
}

// FAST-FAIL

class FastFailException(override val message: String?) : Exception(message)

fun fastfail(message: String?): Nothing = throw FastFailException(message)

fun <T> catchFailure(failBlock: (String?) -> Unit = { println(it) }, runBlock: () -> T): T? {
	return try {
		runBlock()
	} catch(fastfail: FastFailException) {
		failBlock(fastfail.message)
		null
	}
}

// EXPORTS

fun exportToZip(renas: List<ResolvedRenaObject>) {
	mainLogger.info("Exporting to zip")

	val cacheFolder = Paths.get(".rena")
	val cacheLockFile = Paths.get(".rena.lock")

	// acquire the lock
	val fc = FileChannel.open(
		cacheLockFile,
		StandardOpenOption.CREATE,
		StandardOpenOption.WRITE,
		StandardOpenOption.DELETE_ON_CLOSE
	)
	val lock = fc.tryLock()
	if(lock == null) {
		mainLogger.warn("Cannot acquire the lock. Maybe there is another RenaApp running currently.")
		return
	}
	mainLogger.info("Lock acquired")

	// validate Cache
	Files.createDirectories(cacheFolder)

	// copies
	/*
	This step is to arrange the files by Rena Standard Package v1(RSP-1)

	Rena Standard Package v1:
	1. The Package file should be a zip.
	2. All charts with its files should be placed in a directory, which the name should be equal to the name of Set.
	3. Officially Encrypted file should be named with extension of ".rnx", others should not be named with extension of ".rnx".
	4. The Music files should be named with "<SET_NAME>.mp3" or "<SET_NAME>.mp3.rnx".
	5. The Cover Image files should be named with "<SET_NAME>.jpg" or "<SET_NAME>.jpg.rnx", as it should be JPEG encoding.
	6. The Preview Music files should be named with "<SET_NAME>_preview.mp3" or "<SET_NAME>_preview.mp3.rnx".
	7. The Chart XML files should be named with "<SET_NAME>_<CHART_DIFFICULTY>.xml" or "<SET_NAME>_<CHART_DIFFICULTY>.xml.rnx".
	 */
	renas.forEach { rr ->
		val setName = rr.setName
		val setFolder = Files.createDirectories(cacheFolder.resolve(setName))

		rr.soundPath = Files.copy(
			rr.soundFile.toPath(),
			setFolder.resolve("${setName}.mp3.rnx"),
			StandardCopyOption.REPLACE_EXISTING
		).relativize(setFolder).pathString
		rr.coverPath = Files.copy(
			rr.coverFile.toPath(),
			setFolder.resolve("${setName}.jpg.rnx"),
			StandardCopyOption.REPLACE_EXISTING
		).relativize(setFolder).pathString
		rr.previewPath = Files.copy(
			rr.previewFile.toPath(),
			setFolder.resolve("${setName}_preview.mp3.rnx"),
			StandardCopyOption.REPLACE_EXISTING
		).relativize(setFolder).pathString

		rr.charts.forEach { rc ->
			rc.chartPath = Files.copy(
				rc.chartFile.toPath(),
				setFolder.resolve("${setName}_${rc.hardClass}.xml.rnx"),
				StandardCopyOption.REPLACE_EXISTING
			).relativize(setFolder).pathString
		}
	}

	mainLogger.info("Data copied")

	// pack to zip
	val pack = ZipUtil.zip(cacheFolder.toFile().absolutePath, outputZipPath)
	mainLogger.info("Data packaged")

	// validate the pack
	runCatching {
		pack.loadAsRenaStandardPack()
	}.onSuccess {
		mainLogger.info("Pack validated")
	}.onFailure {
		mainLogger.error("Invalid pack detected", it)
	}

	// release and delete
	lock.release()
	Files.deleteIfExists(cacheLockFile)
	Files.walk(cacheFolder).sorted(Comparator.reverseOrder()).forEach(Files::delete)
	mainLogger.info("Cleansed")
}

fun exportToDatabase(renas: List<ResolvedRenaObject>) {

}

// DIRECT MODE

interface Logger {
	fun info(message: String?)
	fun warn(message: String?)
	fun error(message: String?, throwable: Throwable?)
}

val mainLogger = object : Logger {
	override fun info(message: String?) {
		println("$message")
	}

	override fun warn(message: String?) {
		println("w: $message")
	}

	override fun error(message: String?, throwable: Throwable?) {
		System.err.println("e: $message")
		throwable?.let {
			val sr = StringWriter()
			it.printStackTrace(PrintWriter(sr))
			sr.toString().split("\n").forEach { str ->
				System.err.println("   $str")
			}
		}
	}
}

private fun formatByRenaIndexList(renaPath: String) {

	val renaIndex = File(renaPath)
	if(!renaIndex.exists() || !renaIndex.isFile) error("Invalid RenaIndexList.")
	val dataFolder = renaIndex.parentFile.resolve("Charts")
	if(!dataFolder.exists() || !dataFolder.isDirectory) error("Invalid DataFolder.")

	val p = MongoProvider()

	println("All pre-check passed.")

	val renas = RenaReader.read(renaIndex)

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