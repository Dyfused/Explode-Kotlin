package explode.rena.model

import cn.hutool.core.util.ZipUtil
import java.io.File

private val RenaCachingFolder = File(".rena")

fun File.loadAsRenaStandardPack(): RenaStandardPack {
	return if(isFile && (extension == "zip" || extension == "rsp")) {
		val unzip = ZipUtil.unzip(this, RenaCachingFolder.resolve(".rena_unzip"))
		unzip.loadAsRenaStandardPack()
	}

	else if(isDirectory) {
		val sets = listFiles()!!.mapNotNull(::loadRSPSetFolder)
		RenaStandardPack(sets.toMutableList())
	}

	else {
		error("Unsupported file: $this")
	}
}

private fun loadRSPSetFolder(file: File): RenaSet? {
	if(!file.isDirectory) return null
	else {
		// if the folder containing ".skip" file,
		// then ignore this folder.
		if(file.resolve(".skip").exists()) {
			return null
		}

		// read meta from ".rena" if exists,
		// otherwise use the folder name as SetName, and empty Composer name, Noter name and Introduction.
		val metaFile = file.resolveExist(".rena")
		val meta = if(metaFile != null) RenaMeta(metaFile.readText()) else RenaMeta(file.name, "", "", "")

		// get resources
		val soundFile = file.resolveExistOrRnx("${meta.name}.mp3")
		val coverFile = file.resolveExistOrRnx("${meta.name}.jpg")
		val previewFile = file.resolveExistOrRnx("${meta.name}_preview.mp3")
		val chartFiles = ChartDifficulty.normalValues.mapNotNull {
			file.resolveExistOrRnx("${meta.name}_${it.name}.xml")
		}

		// validate resources
		return RenaSet(
			meta,
			soundFile?.toRenaContent() ?: error("Missing Sound"),
			coverFile?.toRenaContent() ?: error("Missing Cover"),
			previewFile?.toRenaContent() ?: error("Missing Preview"),
			chartFiles.map(File::toRenaChartContent)
		)
	}
}

fun RenaStandardPack.toZip(): File {
	val f = RenaCachingFolder.resolve(".rena_zip").also { it.createNewFile() }

	renaSets.forEach { set ->
		val name = set.meta.name

		// create the set folder
		val sf = f.resolve(name).also { it.createNewFile() }

		fun RenaContent.writeTo(filename: String) {
			sf.resolveRnx(filename, this.isRnx).writeBytes(this.getBytes())
		}

		set.music.writeTo("${name}.mp3")
		set.cover.writeTo("${name}.jpg")
		set.preview.writeTo("${name}_preview.mp3")
		set.charts.forEach {
			it.writeTo("${name}_${it.difficulty}.xml")
		}
	}

	return ZipUtil.zip(f)
}

// File extensions

private fun File.resolveExist(relative: String) = resolve(relative).takeIf { it.exists() }

/**
 * Return the file named [relative] or [relative].rnx. `null` if both not exist.
 */
private fun File.resolveExistOrRnx(relative: String) =
	resolveExist(relative) ?: resolveExist("$relative.rnx")

private fun File.resolveRnx(relative: String, isRnx: Boolean = false) = resolve(if(isRnx) "${relative}.rnx" else relative)

