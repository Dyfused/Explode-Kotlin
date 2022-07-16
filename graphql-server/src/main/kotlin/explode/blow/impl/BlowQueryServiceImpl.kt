package explode.blow.impl

import explode.backend.graphql.NNInt
import explode.blow.*
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.*
import explode.dataprovider.provider.IBlowDataProvider
import graphql.schema.DataFetchingEnvironment

class BlowQueryServiceImpl(private val p: IBlowDataProvider) : BlowQueryService {

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
	): List<SetModel> {
		// 我都不知道请求 refreshSet 的时候要 set 干嘛，
		// 请求了数据还不用，请求体里面还没任何参数。无语。
		if(isOfficial == null) return listOf()

		return p.getSets(
			limit!!.value,
			skip!!.value,
			musicTitle!!,
			isHidden == 1,
			isOfficial == 1,
			isRanked == 1,
			isRanked == -1,
			playCountOrder == -1,
			publishTimeOrder == -1
		)
	}

	override suspend fun self(env: DataFetchingEnvironment): BlowSelfService {
		return self
	}

	override suspend fun ownOrGotChart(env: DataFetchingEnvironment): List<DetailedChartModel> {
		return p.getUserByToken(env.soudayo)?.ownChart?.map(p::getChart) ?: listOf()
	}

	override suspend fun charts(env: DataFetchingEnvironment, limit: Int?, skip: Int?, ranked: Int?): List<DetailedChartModel> {
		return p.getUserByToken(env.soudayo)?.ownSet?.map(p::getChart) ?: listOf()
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

	override suspend fun setById(env: DataFetchingEnvironment, _id: String?): SetModel {
		return p.getSet(_id!!)
	}

	override suspend fun userByUsername(env: DataFetchingEnvironment, username: String?): UserModel? {
		return p.getUser(username!!)
	}

	override suspend fun playRank(env: DataFetchingEnvironment, chartId: String?, skip: NNInt?, limit: NNInt?): List<PlayRecordWithRank> {
		return p.getPlayRank(chartId!!, limit!!.value, skip!!.value)
	}

	override suspend fun refreshSet(env: DataFetchingEnvironment, setVersion: List<ChartSetAndVersion>): List<ClassifiedModels.Set> {
		return setVersion.map { p.getSet(it.setId) }.map {
			ClassifiedModels.Set(it._id, it.isRanked, it.introduction, it.noter.username, it.musicTitle)
		}
	}
}