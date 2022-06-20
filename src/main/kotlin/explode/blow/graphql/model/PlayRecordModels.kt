package explode.blow.graphql.model

import java.time.LocalDate

data class PlayRecordWithRank(
	val player: PlayerModel,
	val mod: PlayMod,
	val rank: Int,
	val score: Int,
	val perfect: Int,
	val good: Int,
	val miss: Int,
	val createTime: LocalDate
)

data class PlayMod(
	val narrow: Int,
	val speed: Int,
	val isBleed: Boolean,
	val isMirror: Boolean
)

data class BeforePlaySubmitModel(
	val PPTime: String,
	val playingRecord: PlayingRecordModel
)

data class PlayingRecordModel(
	val randomId: String
)

data class AfterPlaySubmitModel(
	val ranking: RankingModel,
	val RThisMonth: Int,
	val coin: Int,
	val diamond: Int
)

data class RankingModel(
	val isPlayRankUpdated: Boolean,
	val playRank: RankModel
)

data class RankModel(
	val rank: Int
)