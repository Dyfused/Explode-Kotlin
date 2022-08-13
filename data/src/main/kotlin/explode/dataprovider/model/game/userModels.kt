package explode.dataprovider.model.game


import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class UserModel(
	val _id: String,
	var username: String,
	val ownChart: MutableList<String>,
	val coworkChart: MutableList<String>,
	val ownSet: MutableList<String>,
	var follower: Int?,
	var coin: Int?,
	var diamond: Int?,
	@Contextual
	var PPTime: OffsetDateTime,
	val token: String,
	var RThisMonth: Int?,
	var highestGoldenMedal: Int?,
	val access: AccessData
)

@Serializable
data class AccessData(val reviewer: Boolean)
