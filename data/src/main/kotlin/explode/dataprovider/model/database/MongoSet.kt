package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
data class MongoSet(
	val _id: String,
	var musicName: String,
	var composerName: String,
	var noterId: String,
	var introduction: String?,
	var price: Int,
	var status: SetStatus,
	val charts: MutableList<String>,

	// `null` unless status is NEED_REVIEW
	var reviews: MutableList<ReviewResult>? = null
)

@Serializable
data class ReviewResult(
	val reviewer: String,
	val accepted: Boolean,
	val rejectMessage: String? = null
)