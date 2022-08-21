package explode.backend.bomb.v0.model

import explode.dataprovider.model.database.SetStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class BombPlayRecordResult(
	val score: Int,
	val perfect: Int,
	val good: Int,
	val miss: Int,
	val rank: Int?
)

/**
 * Used in '/bomb/user/$userId/best20' and '/last20'
 */
@Serializable
data class BombPlayRecordOfUser(
	val chartId: String,
	val result: BombPlayRecordResult,
	@Contextual val uploaded: OffsetDateTime,
	val R: Double?
)

@Serializable
data class BombPayloadSetPatch(
	val musicName: String? = null,
	val composerName: String? = null,
	val noterId: String? = null,
	val introduction: String? = null,
	val price: Int? = null,
	val status: SetStatus? = null,
	val charts: List<String>? = null
)

data class BombPayloadReview(
	val review: String
)