package explode.blow.graphql.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class AssessmentRecordWithRankModel(
	val player: PlayerModel,
	val rank: Int,
	val achievementRate: Double,
	val result: Int,
	@Contextual
	val createTime: OffsetDateTime
)

@Serializable
data class PlayerModel(
	val _id: String,
	val username: String,
	val highestGoldenMedalLevel: Int,
	val RThisMonth: Int
)