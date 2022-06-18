package explode.schema.model

data class GameSettingModel(
	val appVer: Int?
)

data class JudgementData(
	val perfect: Int,
	val good: Int,
	val miss: Int,
	val maxHp: Int,
	val startHp: Int
)