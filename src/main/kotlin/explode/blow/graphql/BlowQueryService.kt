package explode.blow.graphql

import explode.backend.ktor.NNInt
import explode.blow.graphql.model.*
import graphql.schema.DataFetchingEnvironment

interface BlowQueryService {

	suspend fun hello(env: DataFetchingEnvironment): String

	suspend fun gameSetting(): GameSettingModel

	suspend fun reviewer(env: DataFetchingEnvironment): ReviewerModel

	suspend fun set(
		playCountOrder: Int? = null,
		publishTimeOrder: Int? = null,
		limit: NNInt? = null,
		skip: NNInt? = null,
		isHidden: Int? = null,
		musicTitle: String? = null,
		isOfficial: Int? = null,
		isRanked: Int? = null
	): List<SetModel>

	suspend fun self(env: DataFetchingEnvironment): SelfModel

	suspend fun ownOrGotChart(env: DataFetchingEnvironment): List<DetailedChartModel>

	suspend fun charts(env: DataFetchingEnvironment, limit: Int?, skip: Int?, ranked: Int?): List<DetailedChartModel>

	suspend fun assessmentGroup(limit: Int?, skip: Int?): List<AssessmentGroupModel>

	suspend fun assessmentRank(
		assessmentGroupId: String?,
		medalLevel: Int?,
		skip: NNInt?,
		limit: NNInt?
	): List<AssessmentRecordWithRankModel>

	suspend fun setById(_id: String?): SetModel

	suspend fun userByUsername(username: String?): UserModel

	suspend fun playRank(chartId: String?, skip: NNInt?, limit: NNInt?): List<PlayRecordWithRank>

	suspend fun refreshSet(setVersion: List<ChartSetAndVersion>): List<ClassifiedModels.Set>
}