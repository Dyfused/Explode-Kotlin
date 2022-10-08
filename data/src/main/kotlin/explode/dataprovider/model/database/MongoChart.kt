package explode.dataprovider.model.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MongoChart(
	@SerialName("_id")
	val id: String,
	val difficultyClass: Int,
	val difficultyValue: Int,
	var D: Double?
)