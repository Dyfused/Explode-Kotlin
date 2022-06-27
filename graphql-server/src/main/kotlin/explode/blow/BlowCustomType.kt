package explode.blow

import com.expediagroup.graphql.server.operations.Query
import explode.dataprovider.model.*
import graphql.schema.DataFetchingEnvironment

open class BlowReviewerService : Query {

	open suspend fun reviewRequest(env: DataFetchingEnvironment, limit: Int?, skip: Int?, status: Int?, searchStr: String?): List<ReviewRequestModel> = error("Need Implementation")
}

open class BlowSelfService : Query {

	open suspend fun gotSet(env: DataFetchingEnvironment): List<SetModel> = error("Need Implementation")

	open suspend fun assessmentRankSelf(env: DataFetchingEnvironment, assessmentGroupId: String?, medalLevel: Int?): AssessmentRecordWithRankModel? = error("Need Implementation")

	open suspend fun playRankSelf(env: DataFetchingEnvironment, chartId: String?): PlayRecordWithRank? = error("Need Implementation")
}