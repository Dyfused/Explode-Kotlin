package explode.blow.impl

import explode.blow.BlowReviewerService
import explode.blow.BlowSelfService
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.*
import explode.dataprovider.provider.IBlowDataProvider
import graphql.schema.DataFetchingEnvironment

class ProviderSelfService(private val p: IBlowDataProvider) : BlowSelfService() {
	override suspend fun gotSet(env: DataFetchingEnvironment): List<SetModel> {
		return p.getGotSets(env.soudayo)
	}

	override suspend fun assessmentRankSelf(
		env: DataFetchingEnvironment,
		assessmentGroupId: String?,
		medalLevel: Int?
	): AssessmentRecordWithRankModel? {
		return p.getAssessmentRankSelf(env.soudayo, assessmentGroupId!!, medalLevel!!)
	}

	override suspend fun playRankSelf(env: DataFetchingEnvironment, chartId: String?): PlayRecordWithRank? {
		return p.getPlayRankSelf(env.soudayo, chartId!!)
	}
}

class ProviderReviewerService(private val p: IBlowDataProvider) : BlowReviewerService() {
	override suspend fun reviewRequest(
		env: DataFetchingEnvironment,
		limit: Int?,
		skip: Int?,
		status: Int?,
		searchStr: String?
	): List<ReviewRequestModel> {
		return p.getReviewRequests(env.soudayo, limit!!, skip!!, status!!, searchStr!!)
	}
}