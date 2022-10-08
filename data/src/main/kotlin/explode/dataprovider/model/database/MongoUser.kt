package explode.dataprovider.model.database

import kotlinx.serialization.*
import java.time.OffsetDateTime

@Serializable
data class MongoUser(
	@SerialName("_id")
	val id: String,
	var username: String,
	var password: String,
	val ownedSets: MutableList<String>,
	val ownedCharts: MutableList<String>,
	var coin: Int,
	var diamond: Int,
	@Contextual
	val ppTime: OffsetDateTime,
	val token: String,
	var R: Int,
	val permission: UserPermission,

	var highestGoldenMedal: Int? = null
)
