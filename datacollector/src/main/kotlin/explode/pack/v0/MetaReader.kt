package explode.pack.v0

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

object MetaReader {

	fun readPackMetaJson(packMetaFile: File): PackMeta {
		return Json.decodeFromString(packMetaFile.readText())
	}

}