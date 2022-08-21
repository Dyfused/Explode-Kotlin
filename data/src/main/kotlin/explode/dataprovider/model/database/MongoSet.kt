package explode.dataprovider.model.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

	var noterDisplayOverride: String? = null
)

