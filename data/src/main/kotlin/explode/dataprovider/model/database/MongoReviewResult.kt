package explode.dataprovider.model.database

import explode.dataprovider.model.newUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MongoReview(
	val reviewedSet: String,

	@Deprecated("Deprecated because isReviewing has taken the place of NEED_REVIEW.")
	val expectStatus: SetStatus? = null,

	val reviews: MutableList<MongoReviewResult> = mutableListOf(),

	@SerialName("_id")
	val id: String = newUUID()
)

@Serializable
data class MongoReviewResult(
	val reviewerId: String,
	val status: Boolean, // true-通过；false-退回
	val evaluation: String,
	val priorty: Int = 0, // 评价优先级，数字越小权限越小，最小和默认为 0

	@SerialName("_id")
	val id: String = newUUID()
)