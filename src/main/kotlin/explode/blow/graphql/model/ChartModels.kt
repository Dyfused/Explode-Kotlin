@file:Suppress("UNUSED_PARAMETER")

package explode.blow.graphql.model

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
	val introduction: String,
	val coinPrice: Int,
	val noter: NoterModel,
	val musicTitle: String,
	val composerName: String,
	val playCount: Int,
	val chart: List<ChartModel>,
	val isGot: Boolean,
	val isRanked: Boolean,
	val isOfficial: Boolean,
	val OverridePriceStr: String, // DON'T FIX: Capital 'O' is defined in the game not Explode's fault.
	val D: Double? = null
)

@Serializable
data class DetailedChartModel(
	val _id: String,
	val charter: UserModel,
	val chartName: String,
	val gcPrice: Int,
	val music: MusicModel,
	val difficultyBase: Int,
	val difficultyValue: Int
)

@Serializable
data class MusicModel(
	val musicianName: String
)