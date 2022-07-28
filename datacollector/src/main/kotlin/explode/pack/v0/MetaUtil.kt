package explode.pack.v0

import java.io.File

object MetaUtil {

	/**
	 * Check the dependent files.
	 *
	 * @return the path list of non-existing files.
	 */
	fun validateFiles(packMeta: PackMeta, packFolder: File): List<String> {
		val errorPaths = mutableListOf<String>()

		fun checkExistance(path: String) {
			if(!packFolder.resolve(path).exists()) {
				errorPaths += path
			}
		}

		packMeta.sets.forEach { set ->
			checkExistance(set.musicPath)
			checkExistance(set.previewMusicPath)
			checkExistance(set.coverPicturePath)
			set.storePreviewPicturePath?.let(::checkExistance)
			set.charts.forEach { chart ->
				checkExistance(chart.chartPath)
			}
		}

		return errorPaths
	}

	fun parseHardnessClassString2Int(hardnessString: String): Int {
		return when(hardnessString.uppercase()) {
			"CASUAL" -> 1
			"NORMAL" -> 2
			"HARD" -> 3
			"MEGA" -> 4
			"GIGA" -> 5
			"TERA" -> 6
			else -> 0
		}
	}

	fun parseHardnessClassInt2String(hardnessInt: Int): String {
		return when(hardnessInt) {
			1 -> "CASUAL"
			2 -> "NORMAL"
			3 -> "HARD"
			4 -> "MEGA"
			5 -> "GIGA"
			6 -> "TERA"
			else -> "UNKNOWN"
		}
	}

}