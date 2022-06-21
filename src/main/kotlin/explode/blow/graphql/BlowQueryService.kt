package explode.blow.graphql

import explode.backend.ktor.NNInt
import explode.blow.graphql.model.*
import graphql.schema.DataFetchingEnvironment

interface BlowQueryService {

	suspend fun hello(env: DataFetchingEnvironment): String

	suspend fun gameSetting(env: DataFetchingEnvironment): GameSettingModel

	suspend fun reviewer(env: DataFetchingEnvironment): ReviewerModel

	suspend fun set(
		env: DataFetchingEnvironment,
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

	suspend fun assessmentGroup(env: DataFetchingEnvironment, limit: Int?, skip: Int?): List<AssessmentGroupModel>

	suspend fun assessmentRank(
		env: DataFetchingEnvironment,
		assessmentGroupId: String?,
		medalLevel: Int?,
		skip: NNInt?,
		limit: NNInt?
	): List<AssessmentRecordWithRankModel>

	suspend fun setById(env: DataFetchingEnvironment, _id: String?): SetModel

	suspend fun userByUsername(env: DataFetchingEnvironment, username: String?): UserModel?

	suspend fun playRank(env: DataFetchingEnvironment, chartId: String?, skip: NNInt?, limit: NNInt?): List<PlayRecordWithRank>

	suspend fun refreshSet(env: DataFetchingEnvironment, setVersion: List<ChartSetAndVersion>): List<ClassifiedModels.Set>
}