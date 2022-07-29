package explode.dataprovider.model.extend

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