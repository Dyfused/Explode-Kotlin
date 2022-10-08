package explode.dataprovider.model.database

import kotlinx.serialization.*
import java.time.OffsetDateTime

@Serializable
data class MongoRecord(
	@SerialName("_id")
	val id: String,
	val playerId: String,
	val chartId: String,
	val score: Int,
	val scoreDetail: ScoreDetail,
	@Contextual
	val uploadedTime: OffsetDateTime,
	val RScore: Double?
)

@Serializable
data class MongoRecordRanked(
	@SerialName("_id")
	val id: String,
	val playerId: String,
	val chartId: String,
	val score: Int,
	val scoreDetail: ScoreDetail,
	@Contextual
	val uploadedTime: OffsetDateTime,
	val RScore: Double?,
	val ranking: Int
)