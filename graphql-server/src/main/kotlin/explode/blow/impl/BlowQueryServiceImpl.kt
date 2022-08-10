package explode.blow.impl

import explode.backend.graphql.NNInt
import explode.blow.*
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.fail
import graphql.schema.DataFetchingEnvironment

class BlowQueryServiceImpl(private val p: IBlowAccessor) : BlowQueryService {

	private val self = ProviderSelfService(p)
	private val reviewer = ProviderReviewerService(p)

	override suspend fun hello(env: DataFetchingEnvironment): String {
		return "You've been waiting, \"${env.soudayo}\"."
	}

	override suspend fun gameSetting(env: DataFetchingEnvironment, ): GameSettingModel {
		return p.gameSetting
	}

	override suspend fun reviewer(env: DataFetchingEnvironment): BlowReviewerService {
		return reviewer
	}

	override suspend fun set(
		env: DataFetchingEnvironment,
		playCountOrder: Int?,
		publishTimeOrder: Int?,
		limit: NNInt?,
		skip: NNInt?,
		isHidden: Int?,
		musicTitle: String?,
		isOfficial: Int?,
		isRanked: Int?
	): List<SetModel> = with(p) {
		// 我都不知道请求 refreshSet 的时候要 set 干嘛，
		// 请求了数据还不用，请求体里面还没任何参数。无语。
		if(isOfficial == null) return listOf()

		return p.getSets(
			limit!!.value,
			skip!!.value,
			musicTitle!!,
			isRanked == 1,
			isOfficial == 1,
			false,
			isHidden == 1,
			playCountOrder == -1,
			publishTimeOrder == -1
		).map { it.tunerize }
	}

	override suspend fun self(env: DataFetchingEnvironment): BlowSelfService {
		return self
	}

	override suspend fun ownOrGotChart(env: DataFetchingEnvironment): List<DetailedChartModel> = with(p) {
		return p.getUserByToken(env.soudayo)?.ownedCharts?.mapNotNull(p::getChart)?.map { it.tunerize } ?: listOf()
	}

	override suspend fun charts(env: DataFetchingEnvironment, limit: Int?, skip: Int?, ranked: Int?): List<DetailedChartModel> = with(p) {
		return p.getUserByToken(env.soudayo)?.ownedSets?.mapNotNull(p::getChart)?.map { it.tunerize } ?: listOf()
	}

	override suspend fun assessmentGroup(env: DataFetchingEnvironment, limit: Int?, skip: Int?): List<AssessmentGroupModel> {
		return listOf()
	}

	override suspend fun assessmentRank(
		env: DataFetchingEnvironment,
		assessmentGroupId: String?,
		medalLevel: Int?,
		skip: NNInt?,
		limit: NNInt?
	): List<AssessmentRecordWithRankModel> {
		return listOf()
	}

	override suspend fun setById(env: DataFetchingEnvironment, _id: String?): SetModel = with(p) {
		return p.getSet(_id!!)?.tunerize ?: fail("Invalid setId: $_id")
	}

	override suspend fun userByUsername(env: DataFetchingEnvironment, username: String?): UserModel? = with(p) {
		return p.getUser(username!!)?.tunerize
	}

	override suspend fun playRank(env: DataFetchingEnvironment, chartId: String?, skip: NNInt?, limit: NNInt?): List<PlayRecordWithRank> {
		return p.getPlayRank(chartId!!, limit!!.value, skip!!.value)
	}

	override suspend fun refreshSet(env: DataFetchingEnvironment, setVersion: List<ChartSetAndVersion>): List<ClassifiedModels.Set> = with(p) {
		return setVersion.mapNotNull { getSet(it.setId) }.map {
			ClassifiedModels.Set(it._id, it.status.isRanked, it.introduction ?: "", getUser(it.noterId)?.username ?: "unknown", it.musicName)
		}
	}
}