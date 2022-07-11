package explode.dataprovider.provider

import explode.dataprovider.model.*

interface IBlowDataProvider : IBlowUserAccessor {

	val gameSetting: GameSettingModel

	fun loginUser(username: String, password: String): UserModel

	fun registerUser(username: String, password: String): UserModel

	/**
	 * Get Chart Set
	 */
	fun getSet(setId: String): SetModel

	/**
	 * Get Detailed Chart
	 *
	 * @see DetailedChartModel.minify
	 */
	fun getChart(chartId: String): DetailedChartModel

	/**
	 * Get Chart Sets
	 *
	 * Priority: showHidden > showReview > showOfficial > showRanked,
	 * which means if showHidden is true, it will ignore all other filters.
	 */
	fun getSets(
		limit: Int,
		skip: Int,
		searchedName: String,
		onlyRanked: Boolean,
		onlyOfficial: Boolean,
		onlyReview: Boolean,
		onlyHidden: Boolean,
		playCountOrder: Boolean,
		publishTimeOrder: Boolean
	): List<SetModel>

	fun getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel>

	fun getAssessmentRank(
		assessmentGroupId: String,
		medalLevel: Int,
		limit: Int,
		skip: Int
	): List<AssessmentRecordWithRankModel>

	fun getPlayRank(chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank>

	fun UserModel.getAssessmentRankSelf(
		assessmentGroupId: String,
		medalLevel: Int
	): AssessmentRecordWithRankModel?

	fun UserModel.getPlayRankSelf(chartId: String): PlayRecordWithRank?

	fun UserModel.buySet(id: String): ExchangeSetModel

	fun UserModel.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel

	fun UserModel.submitBeforePlay(chartId: String, ppCost: Int, eventArgs: String): BeforePlaySubmitModel

	fun UserModel.submitAfterAssessment(records: List<PlayRecordInput>, randomId: String): AfterAssessmentModel

	fun UserModel.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel
}