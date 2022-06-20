package explode.blow.graphql.model

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

data class ExchangeSetModel(
	val coin: Int? // remaining coins after purchase
)