package explode.dataprovider.model.database

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class MongoRecord(
	val _id: String,
	val playerId: String,
	val chartId: String,
	val score: Int,
	val scoreDetail: ScoreDetail,
	@Contextual
	val uploadedTime: OffsetDateTime,
	val RScore: Double?
)
