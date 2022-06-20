package explode.blow.graphql.model

import java.time.OffsetDateTime

data class AssessmentRecordWithRankModel(
	val player: PlayerModel,
	val rank: Int,
	val achievementRate: Double,
	val result: Int,
	val createTime: OffsetDateTime
)

data class PlayerModel(
	val _id: String,
	val username: String,
	val highestGoldenMedalLevel: Int,
	val RThisMonth: Int
)