package explode.blow.graphql.model


import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class UserModel(
	val _id: String,
	val username: String,
	val ownChart: List<String>,
	val coworkChart: List<String>,
	val ownSet: List<String>,
	val follower: Int?,
	val coin: Int?,
	val diamond: Int?,
	@Contextual
	val PPTime: OffsetDateTime,
	val token: String,
	val RThisMonth: Int?,
	val highestGoldenMedal: Int?,
	val access: AccessData
)

@Serializable
data class AccessData(val reviewer: Boolean)
