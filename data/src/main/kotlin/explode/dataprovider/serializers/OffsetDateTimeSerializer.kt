package explode.dataprovider.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = OffsetDateTime::class)
object OffsetDateTimeSerializer {

	override fun deserialize(decoder: Decoder): OffsetDateTime {
		return OffsetDateTime.parse(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: OffsetDateTime) {
		encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
	}
}