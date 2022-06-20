package explode.blow.graphql.model

data class UserModel(
	val _id: String,
	val username: String,
	val ownChart: List<String>,
	val coworkChart: List<String>,
	val ownSet: List<String>,
	val follower: Int?,
	val coin: Int?,
	val diamond: Int?,
	val PPTime: String,
	val token: String,
	val RThisMonth: Int?,
	val access: AccessData
)

data class AccessData(val reviewer: Boolean)
