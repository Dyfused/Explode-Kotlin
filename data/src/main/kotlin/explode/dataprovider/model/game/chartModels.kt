package explode.dataprovider.model.game

import kotlinx.serialization.Serializable

@Serializable
data class ChartModel(
	val _id: String,
	val difficultyClass: Int,
	val difficultyValue: Int?
)

@Serializable
data class NoterModel(
	val username: String
)

@Serializable
data class ReviewRequestModel(
	val set: SetModel,
	val isUnranked: Boolean
)

@Serializable
data class SetModel(
	val _id: String,
	var introduction: String,
	var coinPrice: Int,
	val noter: NoterModel,
	var musicTitle: String,
	var composerName: String,
	var playCount: Int,
	val chart: List<ChartModel>,
	var isGot: Boolean,
	var isRanked: Boolean,
	var isOfficial: Boolean,
	var needReview: Boolean
)

@Serializable
data class DetailedChartModel(
	val _id: String,
	val charter: UserWithUserNameModel,
	val chartName: String,
	val gcPrice: Int,
	val music: MusicModel,
	val difficultyBase: Int,
	val difficultyValue: Int,
	val D: Double? = null
)

@Serializable
data class MusicModel(
	val musicName: String,
	val musician: MusicianModel
)

@Serializable
data class MusicianModel(
	val musicianName: String
)

@Serializable
data class UserWithUserNameModel(
	val username: String
)

val DetailedChartModel.minify: ChartModel get() = ChartModel(_id, difficultyBase, difficultyValue)