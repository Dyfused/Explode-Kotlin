@file:Suppress("UNUSED_PARAMETER")

package explode.schema.model

import explode.schema.FakeSet
import java.time.LocalDate

data class ChartModel(
	val _id: String,
	val difficultyClass: Int,
	val difficultyValue: Int?
)

data class NoterModel(
	val username: String
)

class ReviewerModel {
	suspend fun reviewRequest(limit: Int?, skip: Int?, status: Int?, searchStr: String?): List<ReviewRequestModel> {
		return listOf(ReviewRequestModel(FakeSet, false))
	}
}

data class ReviewRequestModel(
	val set: SetModel,
	val isUnranked: Boolean
)

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
	val OverridePriceStr: String // DON'T FIX: Capital 'O' is defined in the game not Explode's fault.
)

class OwnSetWrapperModel(
	val gotSet: List<SetModel>
) {
	// TODO: Implement
	suspend fun assessmentRankSelf(assessmentGroupId: String?, medalLevel: Int?): AssessmentRecordWithRankModel {
		return AssessmentRecordWithRankModel(
			PlayerModel("10001", "FakeUsername", 100000, 1000),
			1,
			1.0,
			10,
			LocalDate.now()
		)
	}
}