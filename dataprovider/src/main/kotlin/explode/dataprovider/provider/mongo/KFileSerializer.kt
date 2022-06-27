package explode.dataprovider.provider.mongo

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
@Serializer(File::class)
class KFileSerializer : KSerializer<File> {

	override fun deserialize(decoder: Decoder): File {
		return File(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: File) {
		encoder.encodeString(value.path)
	}
}