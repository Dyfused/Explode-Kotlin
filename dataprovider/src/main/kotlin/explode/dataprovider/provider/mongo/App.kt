@file:JvmName("App")

package explode.dataprovider.provider.mongo

import TConfig.Configuration
import explode.dataprovider.detonate.ExplodeConfig.Companion.explode
import explode.dataprovider.model.database.MongoSet
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

	val message = "Updated $count sets with ${warnings.size} error: "+warnings.joinToString("\n", "- ")
	if(warnings.isNotEmpty()) {
		File("warnings.txt").writeText(message)
	}
	println(message)
	JOptionPane.showMessageDialog(null, message)

}