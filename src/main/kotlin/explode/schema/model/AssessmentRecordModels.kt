package explode.schema.model

import java.time.LocalDate

data class AssessmentRecordWithRankModel(
	val player: PlayerModel,
	val rank: Int,
	val achievementRate: Double,
	val result: Int,
	val createTime: LocalDate
)

data class PlayerModel(
	val _id: String,
	val username: String,
	val highestGoldenMedalLevel: Int,
	val RThisMonth: Int
)