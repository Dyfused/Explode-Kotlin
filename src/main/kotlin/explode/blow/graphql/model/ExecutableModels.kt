package explode.blow.graphql.model

import com.expediagroup.graphql.server.operations.Query
import explode.blow.graphql.BlowService.soudayo
import explode.blow.provider.IBlowProvider
import graphql.schema.DataFetchingEnvironment

open class ReviewerModel : Query {

	open suspend fun reviewRequest(env: DataFetchingEnvironment, limit: Int?, skip: Int?, status: Int?, searchStr: String?): List<ReviewRequestModel> = error("Need Implementation")
}

open class SelfModel : Query {

	open suspend fun gotSet(env: DataFetchingEnvironment): List<SetModel> = error("Need Implementation")

	open suspend fun assessmentRankSelf(env: DataFetchingEnvironment, assessmentGroupId: String?, medalLevel: Int?): AssessmentRecordWithRankModel? = error("Need Implementation")

	open suspend fun playRankSelf(env: DataFetchingEnvironment, chartId: String?): PlayRecordWithRank? = error("Need Implementation")
}

class ProviderSelfModel(private val p: IBlowProvider) : SelfModel() {
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

class ProviderReviewerModel(private val p: IBlowProvider) : ReviewerModel() {
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