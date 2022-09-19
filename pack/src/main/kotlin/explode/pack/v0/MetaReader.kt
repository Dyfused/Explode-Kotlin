package explode.pack.v0

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

object MetaReader {

	/**
	 * Read the file to [PackMeta] instance.
	 *
	 * If not existent, returns [PackMeta.EMPTY]; or if it is a folder, then read `explode-pack.json` in it.
	 * Otherwise, directly read the file.
	 */
	fun File.asExplodePack(): PackMeta {
		return if(!exists()) {
			PackMeta.EMPTY
		} else if(isFile) {
			readPackMetaJson(this)
		} else {
			readPackFolder(this)
		}
	}

	private fun readPackMetaJson(packMetaFile: File): PackMeta {
		return Json.decodeFromString(packMetaFile.readText())
	}

	private fun readPackFolder(packFolder: File): PackMeta = readPackMetaJson(packFolder.resolve("explode-pack.json"))

}