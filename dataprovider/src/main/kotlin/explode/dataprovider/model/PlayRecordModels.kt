package explode.dataprovider.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class PlayRecordWithRank(
	val player: PlayerModel,
	val mod: PlayMod,
	val rank: Int,
	val score: Int,
	val perfect: Int,
	val good: Int,
	val miss: Int,
	@Contextual
	val createTime: OffsetDateTime
)

@Serializable
data class PlayMod(
	val narrow: Int,
	val speed: Int,
	val isBleed: Boolean,
	val isMirror: Boolean
)

@Serializable
data class BeforePlaySubmitModel(
	@Contextual
	val PPTime: OffsetDateTime,
	val playingRecord: PlayingRecordModel
)

@Serializable
data class PlayingRecordModel(
	val randomId: String
)

@Serializable
data class AfterPlaySubmitModel(
	val ranking: RankingModel,
	val RThisMonth: Int,
	val coin: Int?,
	val diamond: Int?
)

@Serializable
data class RankingModel(
	val isPlayRankUpdated: Boolean,
	val playRank: RankModel
)

@Serializable
data class RankModel(
	val rank: Int
)