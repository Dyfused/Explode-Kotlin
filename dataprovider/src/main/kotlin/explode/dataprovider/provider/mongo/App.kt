@file:JvmName("App")

package explode.dataprovider.provider.mongo

import TConfig.Configuration
import explode.dataprovider.detonate.ExplodeConfig.Companion.explode
import explode.dataprovider.model.database.*
import explode.dataprovider.provider.mongo.MongoExplodeConfig.Companion.toMongo
import explode.pack.v0.*
import org.litote.kmongo.*
import java.io.File
import javax.swing.JOptionPane

/**
 * `java -cp graphql-server.jar explode.dataprovider.provider.mongo.App [import/export/inspect]`
 */
fun main(args: Array<String>) {
	if("import" in args) {
		import()
	} else if("export" in args) {
		export()
	} else if("inspect" in args) {
		inspect()
	} else if("renewId" in args) {
		renewId()
	} else if("updateStatusByD" in args) {
		updateStatusByD()
	} else if("loadDFromCsv" in args) {
		loadCsvD()
	} else {
		JOptionPane.showMessageDialog(null, "Invalid Operation")
	}
}

private fun import() {

	val mp = MongoProvider()

	val metaPath = JOptionPane.showInputDialog("Pack Meta Path: ")
	if(metaPath == null) {
		JOptionPane.showMessageDialog(null, "We cannot help you if you don't give us the PackMeta.")
		return
	}
	val metaFile = File(metaPath)
	if(!metaFile.exists() || !metaFile.isFile) {
		JOptionPane.showMessageDialog(null, "Invalid PackMeta file as it can be non-regular file or just missing.")
		return
	}

	val packMeta = runCatching { MetaReader.readPackMetaJson(metaFile) }.onFailure {
		JOptionPane.showMessageDialog(
			null, "Error occurred when parsing PackMeta file: ${it.message}"
		)
	}.getOrThrow()
	val packFolder = metaFile.parentFile.resolve(packMeta.relativeFolderPath)
	val validation = MetaUtil.validateFiles(packMeta, packFolder)
	if(validation.isNotEmpty()) {
		val message = "Multiple invalid dependent files are missing or corrupted: \n" + validation.joinToString(
			"\n", prefix = " - "
		)
		JOptionPane.showMessageDialog(null, message)
		return
	}

	var setCount = 0
	var chartCount = 0
	packMeta.sets.forEach { set ->
		runCatching {
			mp.buildChartSet(
				setTitle = set.musicName,
				composerName = set.composerName,
				noterUser = mp.getUserByName(set.noterName) ?: mp.serverUser,
				isRanked = false,
				coinPrice = 0,
				introduction = set.introduction ?: "",
				needReview = false,
				defaultId = set.id
			) {
				set.charts.forEach { chart ->
					addChart(chart.difficultyClass, chart.difficultyValue, chart.DValue, chart.id).apply {
						mp.addChartResource(_id, packFolder.resolve(chart.chartPath).readBytes())
					}
					chartCount++
				}
			}.apply {
				mp.addMusicResource(_id, packFolder.resolve(set.musicPath).readBytes())
				mp.addPreviewResource(_id, packFolder.resolve(set.previewMusicPath).readBytes())
				mp.addSetCoverResource(_id, packFolder.resolve(set.coverPicturePath).readBytes())
				set.storePreviewPicturePath?.let { mp.addStorePreviewResource(_id, packFolder.resolve(it).readBytes()) }

				println(_id)
			}
			setCount++
		}.onFailure {
			JOptionPane.showMessageDialog(
				null, "Error occurred when uploading ${set.musicName}(${set.noterName}): $it. Skip and continue."
			)
		}
	}

	JOptionPane.showMessageDialog(null, "Successfully uploaded $setCount sets containg $chartCount charts.")
}

private fun export() {

	val mp = MongoProvider()

	val outPath = JOptionPane.showInputDialog("Output Pack Path: ")
	if(outPath == null) {
		JOptionPane.showMessageDialog(null, "We cannot help you if you don't give us the folder.")
		return
	}
	val outFolder = File(outPath)
	if(!outFolder.exists()) {
		outFolder.mkdirs()
	}
	if(!outFolder.isDirectory) {
		JOptionPane.showMessageDialog(null, "Invalid path as it can be non-regular directory or just missing.")
		return
	}

	val setMeta = mp.getAllSets().map { set ->
		val sid = set._id

		val musicPath = "${sid}/${sid}.mp3"
		val previewMusicPath = "${sid}/${sid}_preview.mp3"
		val coverPicturePath = "${sid}/${sid}.jpg"

		outFolder.resolve(sid).mkdirs()
		outFolder.resolve(musicPath).writeBytes(mp.getMusicResource(sid)!!)
		outFolder.resolve(previewMusicPath).writeBytes(mp.getPreviewResource(sid)!!)
		outFolder.resolve(coverPicturePath).writeBytes(mp.getSetCoverResource(sid)!!)

		println(sid)
		val coverThumb = mp.getStorePreviewResource(sid)
		val storePreviewPicturePath = if(coverThumb != null) {
			outFolder.resolve("${sid}/${sid}_thumbnail.jpg").writeBytes(coverThumb)
			"${sid}/${sid}_thumbnail.jpg"
		} else {
			null
		}

		SetMeta(id = set._id,
			musicName = set.musicName,
			composerName = set.composerName,
			noterName = mp.getUser(set.noterId)?.username ?: "unknown",
			introduction = set.introduction,
			musicPath = musicPath,
			previewMusicPath = previewMusicPath,
			coverPicturePath = coverPicturePath,
			storePreviewPicturePath = storePreviewPicturePath,
			charts = set.charts.map {
				val c = mp.getChart(it) ?: error("Invalid chartId: $it")
				ChartMeta(
					id = it,
					difficultyClass = c.difficultyClass,
					difficultyValue = c.difficultyValue,
					DValue = c.D,
					chartPath = "${set._id}/${set._id}_${MetaUtil.parseHardnessClassInt2String(c.difficultyClass)}.xml",
				).apply {
					outFolder.resolve(chartPath).writeBytes(mp.getChartResource(c._id)!!)
				}
			})
	}

	val packMeta = PackMeta(
		id = null, author = null, sets = setMeta.toList(), relativeFolderPath = "."
	)

	val metaFile = MetaWriter.writePackMetaJson(packMeta, outFolder)
	JOptionPane.showMessageDialog(null, "Pack exported to $metaFile with ${packMeta.sets.size} sets included.")

}

private fun inspect() {

	val metaPath = JOptionPane.showInputDialog("Pack Meta Path: ")
	if(metaPath == null) {
		JOptionPane.showMessageDialog(null, "We cannot help you if you don't give us the PackMeta.")
		return
	}
	val metaFile = File(metaPath)
	if(!metaFile.exists() || !metaFile.isFile) {
		JOptionPane.showMessageDialog(null, "Invalid PackMeta file as it can be non-regular file or just missing.")
		return
	}

	val packMeta = MetaReader.readPackMetaJson(metaFile)
	val packFolder = metaFile.parentFile.resolve(packMeta.relativeFolderPath)
	val validation = MetaUtil.validateFiles(packMeta, packFolder)
	val validationMessage = if(validation.isEmpty()) "Pass" else {
		"Missing listed files: \n" + validation.joinToString("\n", prefix = " - ")
	}

	val message = """
		Explode Pack Inspector
		PackMeta: $metaFile
		PackData: $packFolder

		Contains ${packMeta.sets.size} sets and ${packMeta.sets.sumOf { it.charts.size }} charts included.

		File Validation: $validationMessage
	""".trimIndent()
	println(message)
	JOptionPane.showMessageDialog(null, message)
}

private fun renewId() {

	val config = Configuration(File("./provider.cfg")).explode().toMongo()
	val cli = KMongo.createClient(config.connectionString)
	val old = cli.getDatabase(config.databaseName)
	val new = cli.getDatabase(config.databaseName + "_ID_REPLACE")

	val oldSets = old.getCollection<MongoSet>("ChartSet")
	val newSets = new.getCollection<MongoSet>("ChartSet")

	val metaPath = JOptionPane.showInputDialog("Pack Meta Path: ")
	if(metaPath == null) {
		JOptionPane.showMessageDialog(null, "We cannot help you if you don't give us the PackMeta.")
		return
	}
	val metaFile = File(metaPath)
	if(!metaFile.exists() || !metaFile.isFile) {
		JOptionPane.showMessageDialog(null, "Invalid PackMeta file as it can be non-regular file or just missing.")
		return
	}

	val packMeta = runCatching { MetaReader.readPackMetaJson(metaFile) }.onFailure {
		JOptionPane.showMessageDialog(
			null, "Error occurred when parsing PackMeta file: ${it.message}"
		)
	}.getOrThrow()

	val warnings = mutableListOf<String>()
	val count = packMeta.sets.count { set ->
		val newId = set.id
		val musicName = set.musicName

		if(newId == null) {
			println("Error: No ID presents for $musicName in pack metadata.")
		} else {
			val iter = oldSets.find(MongoSet::musicName eq musicName).toList()
			warnings += if(iter.size > 1) {
				println("Warning: ${set.musicName} matches multiple sets ${iter.map(MongoSet::_id)}. Skipped.")
				"Warning: ${set.musicName} matches multiple sets ${iter.map(MongoSet::_id)}."
			} else if(iter.isEmpty()) {
				println("Warning: ${set.musicName} matches nothing. Skipped.")
				"Warning: ${set.musicName} matches nothing."
			} else {
				val dbSet = iter.first()
				val oldId = dbSet._id
				val newSet = dbSet.copy(_id = newId)
				newSets.insertOne(newSet)
				println("${set.musicName} matched and updated $oldId to $newId.")
				return@count true
			}
		}
		false
	}

	val message = "Updated $count sets with ${warnings.size} error: " + warnings.joinToString("\n", "- ")
	if(warnings.isNotEmpty()) {
		File("warnings.txt").writeText(message)
	}
	println(message)
	JOptionPane.showMessageDialog(null, message)

}

private fun updateStatusByD() {

	val mp = MongoProvider()

	val successCount = mp.getAllSets().count { set ->
		val dExistanceCount =
			set.charts.mapNotNull { chartId -> mp.getChart(chartId) }.count { chart -> chart.D != null }
		if(dExistanceCount == set.charts.size) { // all charts exist and have D value.
			set.status = SetStatus.RANKED
			mp.updateSet(set)
			true
		} else {
			println("Warning: ${set.musicName}(${set._id}) has multiple charts missing or with no D value.")
			false
		}
	}

	println("Successfully updated status of $successCount charts to Ranked.")
}

private fun loadCsvD() {

	val mp = MongoProvider()

	val csvPath = JOptionPane.showInputDialog("Csv: ")
	val csvFile = File(csvPath)

	if(!csvFile.exists()) {
		JOptionPane.showMessageDialog(null, "Error: Csv not exists.")
		return
	}

	var errorMessages = mutableListOf<String>()

	fun errorMsg(message: String) {
		errorMessages += message
	}

	csvFile.readLines().forEachIndexed { lineNum, line ->
		val parts = line.split(",")

		val musicName = parts.getOrNull(0) ?: return@forEachIndexed errorMsg("Music not found at line $lineNum")
		val difficultyAndD =
			parts.getOrNull(1) ?: return@forEachIndexed errorMsg("Difficulty and R value not found at line $lineNum")

		val left = difficultyAndD.indexOf('(')
		val right = difficultyAndD.indexOf(')')

		val diffStr = difficultyAndD.substring(0, left)
		val dStr = difficultyAndD.substring(left + 1, right)

		// println(diffStr)
		// println(rStr)

		val indSpace = diffStr.indexOf(' ')

		val hardLevel = diffStr.substring(0, indSpace)
		val hardValue = diffStr.substring(indSpace + 1)

		val diffClass = when(hardLevel) {
			"CASUAL" -> 1
			"NORMAL" -> 2
			"HARD" -> 3
			"MEGA" -> 4
			"GIGA" -> 5
			else -> 0
		}
		val diffValue = hardValue.toIntOrNull()
			?: return@forEachIndexed errorMsg("Invalid difficulty number $hardLevel at line $lineNum")
		val d = dStr.toDoubleOrNull() ?: return@forEachIndexed errorMsg("Invalid R value $dStr at line $lineNum")

		val sets = mp.getSetByName(musicName).toList()
		if(sets.isEmpty()) {
			errorMsg("No matched set found for name $musicName at line $lineNum")
		}

		val matchedChart = mutableListOf<MongoChart>()
		sets.flatMap { set -> set.charts.mapNotNull { chart -> mp.getChart(chart) } }.forEach {
			if(it.difficultyClass == diffClass && it.difficultyValue == diffValue) {
				matchedChart += it
			}
		}

		if(matchedChart.isEmpty()) {
			errorMsg("No matched chart found for $musicName class  $diffClass and value $diffValue at line $lineNum")
		} else if(matchedChart.size > 1) {
			errorMsg("Too many matched chart found for $musicName class $diffClass and value $diffValue at line $lineNum, they are:")
			matchedChart.forEach {
				errorMsg("- ${it._id}")
			}
		} else {
			val c = matchedChart[0]
			c.D = d
			println("Updated D of ${c._id} to $d")
		}

	}

	val errorMsg = errorMessages.joinToString("\n")
	File("warnings.txt").writeText(errorMsg)
	JOptionPane.showMessageDialog(null, errorMsg)
}