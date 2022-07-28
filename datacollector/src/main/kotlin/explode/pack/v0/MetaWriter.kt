package explode.pack.v0

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object MetaWriter {

	fun writePackMetaJson(packMeta: PackMeta, packFolder: File): File {
		return packFolder.resolve("explode-pack.json").apply { writeText(Json.encodeToString(packMeta)) }
	}

}