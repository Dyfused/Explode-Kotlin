package explode.utils

object RenaReader {

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

	data class ChartWithHardnessAndPath(
		val hardClass: String,
		val hardLevel: Int,
		val chartPath: String
	)

	fun execute(lines: Iterable<String>): List<RenaObject> {
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