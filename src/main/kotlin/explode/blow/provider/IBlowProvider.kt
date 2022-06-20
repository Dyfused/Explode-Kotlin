package explode.blow.provider

import explode.blow.graphql.model.*

interface IBlowProvider {

	fun getSelf(soudayo: String): SelfModel

	fun getReviewer(soudayo: String): ReviewerModel

	fun getReviewRequests(limit: Int, skip: Int, status: Int, searchStr: String): List<ReviewRequestModel>

	fun getGotSets(soudayo: String): List<SetModel>

	fun getOwnedOrGotCharts(soudayo: String): List<DetailedChartModel>

	fun getOwnedCharts(soudayo: String, limit: Int, skip: Int, isRanked: Boolean): List<DetailedChartModel>

	fun getGameSetting(): GameSettingModel

	fun getUser(username: String): UserModel

	fun loginUser(username: String, password: String): UserModel

	fun registerUser(username: String, password: String): UserModel

	fun getSet(id: String): SetModel

	fun getSets(
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

	fun getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel>

	fun getAssessmentRank(assessmentGroupId: String, medalLevel: Int, limit: Int, skip: Int): List<AssessmentRecordWithRankModel>

	fun getAssessmentRankSelf(soudayo: String, assessmentGroupId: String, medalLevel: Int): AssessmentRecordWithRankModel

	fun getPlayRankSelf(chartId: String): PlayRecordWithRank?

	fun getPlayRank(chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank>

	fun submitBeforeAssessment(soudayo: String, assessmentId: String, medal: Int): BeforePlaySubmitModel

	fun submitBeforePlay(soudayo: String, chartId: String, ppCost: Int, eventArgs: String): BeforePlaySubmitModel

	fun submitAfterAssessment(soudayo: String, records: List<PlayRecordInput>, randomId: String): AfterAssessmentModel

	fun submitAfterPlay(soudayo: String, record: PlayRecordInput, randomId: String): AfterPlaySubmitModel
}