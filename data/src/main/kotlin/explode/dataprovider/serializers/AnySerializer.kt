package explode.dataprovider.serializers

import explode.dataprovider.serializers.AnySerializer.toString
import explode.dataprovider.util.explodeLogger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Encoder

/**
 * This serializer is used to prevent exceptions from serializing Any type.
 * We use [toString] to serialize with notice text `[SERIALIZED BY ANY]` ahead of the content.
 * Whenever you see this notice, you should at once check if there is something wrong!
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializer(Any::class)
object AnySerializer {

	override fun serialize(encoder: Encoder, value: Any) {
		explodeLogger.warn("AnySerializer is called at somewhere. This is not expected to happen!")
		Thread.dumpStack()

		encoder.encodeString("[SERIALIZED BY ANY] $value")
	}
}