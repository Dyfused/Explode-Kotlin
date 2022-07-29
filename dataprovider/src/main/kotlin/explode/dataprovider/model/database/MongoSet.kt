package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
data class MongoSet(
	val _id: String,
	val musicName: String,
	val composerName: String,
	val noterId: String,
	var introduction: String?,
	var price: Int,
	val status: SetStatus,
	val charts: MutableList<String>
)
