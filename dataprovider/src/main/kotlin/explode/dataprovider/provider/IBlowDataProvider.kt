package explode.dataprovider.provider

import explode.dataprovider.model.*

interface IBlowDataProvider {

	fun getReviewRequests(soudayo: String, limit: Int, skip: Int, status: Int, searchStr: String): List<ReviewRequestModel>

	fun getGotSets(soudayo: String): List<SetModel>

	fun getOwnedOrGotCharts(soudayo: String): List<DetailedChartModel>

	fun getOwnedCharts(soudayo: String, limit: Int, skip: Int, isRanked: Boolean): List<DetailedChartModel>

	fun getGameSetting(soudayo: String, ): GameSettingModel

	fun getUser(soudayo: String, username: String): UserModel?

	fun loginUser(username: String, password: String): UserModel

	fun registerUser(username: String, password: String): UserModel

	fun getSet(soudayo: String, id: String): SetModel

	fun getSets(
		soudayo: String,
		limit: Int,
		skip: Int,
		searchedName: String,
		showHidden: Boolean,
		showOfficial: Boolean,
		showRanked: Boolean,
		showUnranked: Boolean,
		orderedByPlayCount: Boolean,
		orderedByPublishTime: Boolean
	): List<SetModel>

	fun buySet(soudayo: String, id: String): ExchangeSetModel

	fun getAssessmentGroups(soudayo: String, limit: Int, skip: Int): List<AssessmentGroupModel>

	fun getAssessmentRank(soudayo: String, assessmentGroupId: String, medalLevel: Int, limit: Int, skip: Int): List<AssessmentRecordWithRankModel>

	fun getAssessmentRankSelf(soudayo: String, assessmentGroupId: String, medalLevel: Int): AssessmentRecordWithRankModel?

	fun getPlayRankSelf(soudayo: String, chartId: String): PlayRecordWithRank?

	fun getPlayRank(soudayo: String, chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank>

	fun submitBeforeAssessment(soudayo: String, assessmentId: String, medal: Int): BeforePlaySubmitModel

	fun submitBeforePlay(soudayo: String, chartId: String, ppCost: Int, eventArgs: String): BeforePlaySubmitModel

	fun submitAfterAssessment(soudayo: String, records: List<PlayRecordInput>, randomId: String): AfterAssessmentModel

	fun submitAfterPlay(soudayo: String, record: PlayRecordInput, randomId: String): AfterPlaySubmitModel
}