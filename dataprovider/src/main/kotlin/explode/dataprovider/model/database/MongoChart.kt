package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
data class MongoChart(
	val _id: String,
	val difficultyClass: Int,
	val difficultyValue: Int,
	val D: Double?
)