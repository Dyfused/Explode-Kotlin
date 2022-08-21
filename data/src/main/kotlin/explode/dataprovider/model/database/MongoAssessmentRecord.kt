package explode.dataprovider.model.database

import explode.dataprovider.model.newUUID
import kotlinx.serialization.*
import java.time.OffsetDateTime

@Serializable
data class MongoAssessmentRecord(

	val playerId: String,
	val assessmentId: String,

	val result: Int, // 0-Failed; 1-Success; 2-Ex Success

	val records: List<MongoAssessmentRecordEntry>,
	val exRecord: MongoAssessmentRecordEntry?,

	val totalScore: Int,
	val accuracy: Double,

	@Contextual
	val time: OffsetDateTime,

	@SerialName("_id")
	val id: String = newUUID()
)

@Serializable
data class MongoAssessmentRecordEntry(
	val score: Int,
	val scoreDetail: ScoreDetail,
	val accuracy: Double
)

@Serializable
data class MongoAssessmentRecordRanked(

	val playerId: String,
	val assessmentId: String,

	val result: Int, // 0-Failed; 1-Success; 2-Ex Success

	val records: List<MongoAssessmentRecordEntry>,
	val exRecord: MongoAssessmentRecordEntry?,

	val totalScore: Int,
	val accuracy: Double,

	@Contextual
	val time: OffsetDateTime,

	@SerialName("_id")
	val id: String = newUUID(),

	val ranking: Int
)