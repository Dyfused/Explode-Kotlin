package explode.dataprovider.model.database

import explode.dataprovider.provider.mongo.MongoProvider
import explode.dataprovider.util.explodeLogger
import kotlinx.serialization.*
import java.time.OffsetDateTime

@Serializable
data class MongoSet(
	@SerialName("_id")
	val id: String,
	var musicName: String,
	var composerName: String,
	var noterId: String,
	var introduction: String?,
	var price: Int,
	var status: SetStatus,
	val charts: MutableList<String>,

	var noterDisplayOverride: String? = null,
	@Contextual
	val uploadedTime: OffsetDateTime? = null,

	var isHidden: Boolean = false,
	var isReviewing: Boolean = false,
) {

	init {
		@Suppress("DEPRECATION")
		if(status == SetStatus.HIDDEN || status == SetStatus.NEED_REVIEW) {
			if(status == SetStatus.HIDDEN) {
				explodeLogger.info("Unexpected status $status on Set $id")
				status = SetStatus.UNRANKED
				isHidden = true
			} else if(status == SetStatus.NEED_REVIEW) {
				explodeLogger.info("Unexpected status $status on Set $id")
				status = SetStatus.UNRANKED
				isReviewing = true
			}

			// update value in db
			MongoProvider.letSingleton {
				it.updateSet(this)
			}
		}
	}
}
