package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
data class ScoreDetail(
	val perfect: Int,
	val good: Int,
	val miss: Int
)
