package explode.blow.impl

import explode.blow.BlowReviewerService
import explode.blow.BlowSelfService
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.database.SetStatus
import explode.dataprovider.model.database.StoreSort
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.IBlowAccessor
import graphql.schema.DataFetchingEnvironment

class ProviderSelfService(private val p: IBlowAccessor) : BlowSelfService() {
	override suspend fun gotSet(env: DataFetchingEnvironment): List<SetModel> {
		return with(p) { p.getUserByToken(env.soudayo)?.tunerize?.ownSet?.mapNotNull(p::getSet)?.map { it.tunerize } ?: listOf() }
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

class ProviderReviewerService(private val p: IBlowAccessor) : BlowReviewerService() {
	override suspend fun reviewRequest(
		env: DataFetchingEnvironment,
		limit: Int?,
		skip: Int?,
		status: Int?,
		searchStr: String?
	): List<ReviewRequestModel> {
		val u = p.getUserByToken(env.soudayo)

		if(u == null || !u.permission.review) return emptyList()

		return with(p) {
			p.getSets(
				limit!!,
				skip!!,
				searchStr!!,
				filterCategory = SetStatus.NEED_REVIEW,
				filterSort = StoreSort.PUBLISH_TIME
			).map {
				ReviewRequestModel(it.tunerize, !it.status.isRanked)
			}
		}
	}
}