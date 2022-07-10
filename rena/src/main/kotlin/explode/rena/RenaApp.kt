package explode.rena

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import cn.hutool.core.util.ZipUtil
import explode.dataprovider.provider.DifficultyUtils.toDifficultyClassNum
import explode.dataprovider.provider.mongo.MongoProvider
import explode.rena.model.loadAsRenaStandardPack
import org.slf4j.LoggerFactory
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
	
	[--ignore] or [-ig] refers to ignore the corrupted rena entries like missing resources or invalid resources.
""".trimIndent()

private val ArgParsedMessage
	get() = """
	========================
	Rena: ${if(::renaFile.isInitialized) renaFile.path else "~ERROR~"}
	Export Data to: ${if(shouldImportToDatabase) databaseConnStr else "NONE"}
	Export File to: ${if(shouldExportToZipFile) outputZipPath else "NONE"}
	Ignore Mode: $ignoreMode
	========================
""".trimIndent()

private val DeprecatedExportZipMessage = """
	You are using deprecated operation to export to zip file.
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
		mainLogger.warn(DeprecatedExportZipMessage)
		// exportToZip(renaResult)
	}
}

lateinit var renaFile: File

lateinit var databaseConnStr: String

lateinit var outputZipPath: String

var ignoreMode = false

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
					"-ig", "--ignore" -> {
						ignoreMode = true
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
		val setName = rr.setName.replace(Regex("[\\\\/:*?\"<>|.]"), "_")
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

	// shut up mf mongodb
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("org.mongodb.driver").level = Level.WARN

	val p = MongoProvider(databaseConnStr)

	renas.forEach { r ->
		val s = p.buildChartSet(
			setTitle = r.setName,
			composerName = r.composerName,
			noterUser = p.getUserByName(r.noterName) ?: p.officialUser, // noter user can be null, so we use the safe way.
			isRanked = false, // rsp contain no ranked data, you should modify it later.
			coinPrice = 0, // rsp contains no coin data, you should modify it later.
			introduction = r.introduction,
			needReview = false // since it is all from rsp or official, no need to review.
		) {
			r.charts.forEach {
				addChart(
					difficultyClass = it.hardClass,
					difficultyValue = it.hardLevel
				)
			}
		}

		p.addMusicResource(s._id, r.soundFile.readBytes())
		p.addSetCoverResource(s._id, r.coverFile.readBytes())
		p.addPreviewResource(s._id, r.previewFile.readBytes())
		s.chart.forEach {c1 ->
			r.charts.forEach { c2 ->
				if(c1.difficultyClass == c2.hardClass.toDifficultyClassNum()) {
					p.addChartResource(c1._id, c2.chartFile.readBytes())
				}
			}
		}

		mainLogger.info("Created ${s.musicTitle}(${s._id})")
	}

	mainLogger.info("Uploaded ${renas.size} charts.")
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