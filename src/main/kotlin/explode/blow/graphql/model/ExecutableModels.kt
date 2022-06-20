package explode.blow.graphql.model

import com.expediagroup.graphql.server.operations.Query

open class ReviewerModel : Query {

	open suspend fun reviewRequest(limit: Int?, skip: Int?, status: Int?, searchStr: String?): List<ReviewRequestModel> = error("Need Implementation")
}

open class SelfModel : Query {

	open suspend fun gotSet(): List<SetModel> = error("Need Implementation")

	open suspend fun assessmentRankSelf(assessmentGroupId: String?, medalLevel: Int?): AssessmentRecordWithRankModel = error("Need Implementation")

	open suspend fun playRankSelf(chartId: String?): PlayRecordWithRank? = error("Need Implementation")
}