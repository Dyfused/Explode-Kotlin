package explode.backend.bomb.v1

import explode.dataprovider.model.database.*
import explode.dataprovider.provider.IBlowReadOnly
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
@BusinessObject(MongoSet::class)
data class BombSet(
	val id: String,
	val musicName: String,
	val composerName: String,
	var introduction: String?,
	var price: Int,
	val charts: MutableList<String>,
	var noterName: String? = null,
	@Contextual
	val uploadedTime: OffsetDateTime? = null,
	var isHidden: Boolean = false,
	var isReviewing: Boolean = false,
)

@Serializable
@BusinessObject(MongoChart::class)
data class BombChart(
	val id: String,
	val difficultyClass: Int,
	val difficultyValue: Int,
	val D: Double?,
	val parentSetId: String?
)

@Serializable
@BusinessObject(MongoUser::class)
data class BombUser(
	val id: String,
	val username: String,
	val coin: Int,
	val diamond: Int,
	val R: Int,
	val permission: UserPermission,
	val highestGoldenMedal: Int?
)

@Serializable
@BusinessObject(MongoReview::class)
data class BombReview(
	val id: String,
	val reviewSetId: String,
	val reviews: List<MongoReviewResult>
)

@Serializable
@BusinessObject(MongoRecord::class)
data class BombRecord(
	val id: String,
	val playerId: String,
	val chartId: String,
	val score: Int,
	val perfect: Int,
	val good: Int,
	val miss: Int,
	@Contextual
	val uploadedTime: OffsetDateTime,
	val r: Double?
)

@Serializable
@BusinessObject(MongoRecordRanked::class)
data class BombRecordRanked(
	val id: String,
	val playerId: String,
	val chartId: String,
	val score: Int,
	val perfect: Int,
	val good: Int,
	val miss: Int,
	@Contextual
	val uploadedTime: OffsetDateTime,
	val r: Double?,
	val ranking: Int
)

context(IBlowReadOnly)
fun MongoSet.asBomb(): BombSet =
	BombSet(
		id,
		musicName,
		composerName,
		introduction,
		price,
		charts,
		displayNoterName,
		uploadedTime,
		isHidden,
		isReviewing
	)

context(IBlowReadOnly)
fun MongoChart.asBomb(): BombChart =
	BombChart(
		id, difficultyClass, difficultyValue, D, getParentSet().id
	)

fun MongoUser.asBomb(): BombUser =
	BombUser(
		id, username, coin, diamond, R, permission, highestGoldenMedal
	)

fun MongoReview.asBomb(): BombReview =
	BombReview(id, reviewedSet, reviews)

fun MongoRecord.asBomb(): BombRecord {
	val (perfect, good, miss) = scoreDetail
	return BombRecord(
		id, playerId, chartId, score, perfect, good, miss, uploadedTime, RScore
	)
}

fun MongoRecordRanked.asBomb(): BombRecordRanked {
	val (perfect, good, miss) = scoreDetail
	return BombRecordRanked(
		id, playerId, chartId, score, perfect, good, miss, uploadedTime, RScore, ranking
	)
}