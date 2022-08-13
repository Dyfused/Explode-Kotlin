package explode.pack.v0

/**
 * [RenaReader] is an ultility class for reading Rena files designed by TunerGames officially.
 *
 * As there is already a repository of collection of official charts a.k.a "dynamite-charts-repository",
 * I created this class for further usage.
 */
object RenaReader {

	/**
	 * Parse Rena files into [PackMeta].
	 *
	 * Notice: only process with the data with no file existance validations.
	 */
	fun parse(lines: List<String>, relativeFolderPath: String = "Charts"): PackMeta {
		val parsedSets = splitRenaIntoMap(lines).mapIndexed(::parseRenaPartIntoSetMeta)
		return PackMeta(
			id = null,
			author = null,
			sets = parsedSets,
			relativeFolderPath = relativeFolderPath
		)
	}

	private fun splitRenaIntoMap(lines: List<String>): List<Map<String, String>> {
		val parts = mutableListOf<Map<String, String>>()
		// the key-value map of the part.
		// setup with an empty map.
		// replace a new instance on each 'E.' token occurs.
		var d = mutableMapOf<String, String>()
		lines.forEachIndexed { lineNum, it ->
			if(it.isEmpty()) {
				// ignore empty line
			} else if(it.startsWith("B.")) { // begin a part
				if(d.isNotEmpty()) {
					error("Unexpected token: $it at line ${lineNum + 1}. An ending token (\"E.\") is required before a start token (\"B.\").")
				}
				d["B"] = it.substring(2)
			} else if(it.startsWith("E.")) { // end a part
				parts += d
				d = mutableMapOf()
			} else { // other values
				val s0 = it.split("?", limit = 2)
				d[s0[0]] = s0.getOrElse(1) { "" }
			}
		}
		return parts
	}

	private fun parseRenaEachPart(lines: List<String>, callback: (Map<String, String>) -> Unit) {
		// the key-value map of the part
		// setup with an empty map.
		// consumed on each '.E' occurs.
		var d = mutableMapOf<String, String>()
		lines.forEachIndexed { lineNum, it ->
			if(it.startsWith("B.")) { // begin a part
				if(d.isNotEmpty()) {
					error("Unexpected token: $it at line ${lineNum+1}. An ending token (\"E.\") is required before a start token (\"B.\").")
				}
				d["B"] = it.substring(2)
			} else if(it.startsWith("E.")) { // end a part
				callback(d)
				d = mutableMapOf()
			} else { // other values
				val s0 = it.split("?", limit = 2)
				d[s0[0]] = s0[1]
			}
		}
	}

	private fun parseRenaPartIntoSetMeta(index: Int, d: Map<String, String>): SetMeta {
		try {
			val parsedCharts = parseRenaPartChartsIntoChartMeta(d)
			return SetMeta(
				id = d["B"],
				musicName = d["N"].requireNotNull("MusicName D[N]"),
				composerName = d["W"].requireNotNull("ComposerName D[W]"),
				noterName = d["U"].requireNotNull("NoterName D[U]"),
				introduction = d["I"],
				musicPath = d["S"].requireNotNull("MusicPath D[S]"),
				previewMusicPath = d["P"].requireNotNull("PreviewMusicPath D[P]"),
				coverPicturePath = d["C"].requireNotNull("CoverPicturePath D[C]"),
				storePreviewPicturePath = d["K"],
				charts = parsedCharts
			)
		} catch(ex: Exception) {
			throw IllegalStateException("Unexpected exception occurred parsing part ${index+1}, id ${d["B"]}.", ex)
		}
	}

	private fun parseRenaPartChartsIntoChartMeta(d: Map<String, String>): List<ChartMeta> {
		val hardnesses = d["H"].requireNotNull("Hardnesses D[H]").split(";").filter { it.isNotBlank() }.map {
			val s = it.split(",", limit = 2)
			MetaUtil.parseHardnessClassString2Int(s[0]) to s[1].toInt()
		}
		val mapPaths = d["M"].requireNotNull("MapPathes D[M]").split(";").filter { it.isNotBlank() }
		if(hardnesses.size != mapPaths.size) {
			error("Unexpected unequal size of Hardnesses(${hardnesses.size}) and MapPaths(${mapPaths.size}).")
		}
		return List(hardnesses.size) {
			ChartMeta(
				id = null,
				difficultyClass = hardnesses[it].first,
				difficultyValue = hardnesses[it].second,
				DValue = null,
				chartPath = mapPaths[it]
			)
		}
	}

	// utilities and validators

	private fun <T> T?.requireNotNull(propName: String): T {
		return requireNotNull(this) { "Required value $propName was null." }
	}

}