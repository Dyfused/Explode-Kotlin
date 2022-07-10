package explode.rena

import java.io.File

data class RenaObject(
	val setId: String,
	val setName: String,
	val soundPath: String,
	val coverPath: String,
	val previewPath: String,
	val noterName: String,
	val composerName: String,
	val introduction: String,
	val charts: List<ChartWithHardnessAndPath>
)

data class ResolvedRenaObject(
	val setId: String,
	val setName: String,
	var soundPath: String,
	var coverPath: String,
	var previewPath: String,
	val noterName: String,
	val composerName: String,
	val introduction: String,
	val charts: List<ResolvedChartWithHardness>,

	val soundFile: File,
	val coverFile: File,
	val previewFile: File
)

data class ChartWithHardnessAndPath(
	val hardClass: String,
	val hardLevel: Int,
	val chartPath: String
)

data class ResolvedChartWithHardness(
	val hardClass: String,
	val hardLevel: Int,
	var chartPath: String,
	val chartFile: File
)

object RenaReader {

	fun read(renaFile: File): List<RenaObject> {
		return execute(renaFile.readLines())
	}

	fun readAndResolve(renaFile: File, dataFolderName: String = "Charts"): List<ResolvedRenaObject> {
		val resolveFolder = renaFile.parentFile.resolve(dataFolderName).apply {
			if(!exists() || !isDirectory) error("Invalid Data folder, not existing or not a directory.")
		}

		return read(renaFile).mapNotNull {
			val r = runCatching {
				resolveRenaObject(it, resolveFolder)
			}.onFailure {
				mainLogger.error("Exception occurred when loading Rena.", it)
			}

			if(ignoreMode) {
				r.getOrNull()
			} else {
				r.getOrThrow()
			}
		}
	}

	private fun execute(lines: Iterable<String>): List<RenaObject> {
		val r = mutableListOf<RenaObject>()

		var setId: String? = null
		var setName: String? = null
		var soundPath: String? = null
		var coverPath: String? = null
		var previewPath: String? = null
		var noterName: String? = null
		var composerName: String? = null
		var introduce: String? = null
		var hardLevels: List<Pair<String, Int>>? = null
		var chartPaths: List<String>? = null

		lines.forEach { s ->
			if(s.startsWith("B.")) {
				setId = s.substring(2)
			} else if(s.startsWith("N?")) {
				setName = s.substring(2)
			} else if(s.startsWith("S?")) {
				soundPath = s.substring(2)
			} else if(s.startsWith("C?")) {
				coverPath = s.substring(2)
			} else if(s.startsWith("P?")) {
				previewPath = s.substring(2)
			} else if(s.startsWith("U?")) {
				noterName = s.substring(2)
			} else if(s.startsWith("W?")) {
				composerName = s.substring(2)
			} else if(s.startsWith("I?")) {
				introduce = s.substring(2)
			} else if(s.startsWith("H?")) {
				hardLevels = s.substring(2).split(';').filter { it.isNotEmpty() }.map {
					val str = it.split(',')
					str[0] to str[1].toInt()
				}
			} else if(s.startsWith("M?")) {
				chartPaths = s.substring(2).split(';').filter { it.isNotEmpty() }
			} else if(s.startsWith("E.")) {
				if(listOfNotNull(
						setId,
						setName,
						soundPath,
						coverPath,
						previewPath,
						noterName,
						composerName,
						introduce,
						hardLevels,
						chartPaths
					).size == 10
				) {
					if(hardLevels!!.size != chartPaths!!.size) {
						error("Not matched size of HardLevels and ChartPaths.\n$hardLevels\n$chartPaths")
					}
					val charts = List(hardLevels!!.size) { i ->
						ChartWithHardnessAndPath(hardLevels!![i].first, hardLevels!![i].second, chartPaths!![i])
					}
					r += RenaObject(
						setId!!,
						setName!!,
						soundPath!!,
						coverPath!!,
						previewPath!!,
						noterName!!,
						composerName!!,
						introduce!!,
						charts
					)
				} else {
					error("Not all data collected. $setId, $setName, $soundPath, $coverPath, $previewPath, $noterName, $composerName, $introduce, $hardLevels, $chartPaths")
				}
			}
		}

		return r
	}

	private fun resolveRenaObject(renaObject: RenaObject, resolveFolder: File): ResolvedRenaObject {
		val (setId, setName, soundPath, coverPath, previewPath, notername, composerName, introduction, charts) = renaObject

		val soundFile = resolveFolder.resolve(soundPath)
		val coverFile = resolveFolder.resolve(coverPath)
		val previewFile = resolveFolder.resolve(previewPath)

		if(!soundFile.exists()) error("谱 $setName（${setId}）丢失音乐文件：$soundPath。")
		if(!coverFile.exists()) error("谱 $setName（${setId}）丢失封面文件：$coverPath。")
		if(!previewFile.exists()) error("谱 $setName（${setId}）丢失预览音乐文件：$previewPath。")

		val resolvedCharts = charts.map { (hardClass, hardLevel, chartPath) ->
			val chartFile = resolveFolder.resolve(chartPath)
			if(chartFile.exists()) {
				ResolvedChartWithHardness(
					hardClass,
					hardLevel,
					chartPath,
					chartFile
				)
			} else {
				error("找不到谱（${setName}）的谱面文件：$chartFile。")
			}
		}

		return ResolvedRenaObject(
			setId,
			setName,
			soundPath,
			coverPath,
			previewPath,
			notername,
			composerName,
			introduction,
			resolvedCharts,
			soundFile,
			coverFile,
			previewFile
		)
	}

	fun getHardClassNum(str: String) = when(str.lowercase()) {
		"casual" -> 1
		"normal" -> 2
		"hard"   -> 3
		"mega"   -> 4
		"giga"   -> 5
		"tera"   -> 6
		else -> error("Unknown hard class: $str")
	}

}