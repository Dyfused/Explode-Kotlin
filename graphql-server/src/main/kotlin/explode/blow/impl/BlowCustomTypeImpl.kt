package explode.blow.impl

import explode.blow.BlowReviewerService
import explode.blow.BlowSelfService
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.*
import explode.dataprovider.provider.IBlowDataProvider
import graphql.schema.DataFetchingEnvironment

class ProviderSelfService(private val p: IBlowDataProvider) : BlowSelfService() {
	override suspend fun gotSet(env: DataFetchingEnvironment): List<SetModel> {
		return p.getUserByToken(env.soudayo)?.ownSet?.map(p::getSet) ?: listOf()
	}

	override suspend fun assessmentRankSelf(
		env: DataFetchingEnvironment,
		assessmentGroupId: String?,
		medalLevel: Int?
	): AssessmentRecordWithRankModel? {
		return with(p) {
			p.getUserByToken(env.soudayo)?.getAssessmentRankSelf(assessmentGroupId!!, medalLevel!!)
		}
	}

	override suspend fun playRankSelf(env: DataFetchingEnvironment, chartId: String?): PlayRecordWithRank? {
		return with(p) {
			p.getUserByToken(env.soudayo)?.getPlayRankSelf(chartId!!)
		}
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
		val u = p.getUserByToken(env.soudayo)

		if(u == null || !u.access.reviewer) return emptyList()

		return p.getSets(
			limit!!,
			skip!!,
			searchStr ?: "",
			onlyRanked = false,
			onlyOfficial = false,
			onlyReview = false,
			onlyHidden = false,
			playCountOrder = false,
			publishTimeOrder = false
		).map {
			ReviewRequestModel(it, !it.isRanked)
		}
	}
}