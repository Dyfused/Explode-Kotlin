package explode.blow.graphql.internal

import explode.backend.ktor.NNInt
import explode.blow.graphql.BlowQueryService
import explode.blow.graphql.BlowService.soudayo
import explode.blow.graphql.model.*
import explode.blow.provider.IBlowProvider
import graphql.schema.DataFetchingEnvironment

class BlowQueryServiceImpl(private val p: IBlowProvider) : BlowQueryService {

	override suspend fun hello(env: DataFetchingEnvironment): String {
		return "You've been waiting, \"${env.soudayo}\"."
	}

	override suspend fun gameSetting(): GameSettingModel {
		return p.getGameSetting()
	}

	override suspend fun reviewer(env: DataFetchingEnvironment): ReviewerModel {
		return p.getReviewer(env.soudayo!!)
	}

	override suspend fun set(
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

	override suspend fun self(env: DataFetchingEnvironment): SelfModel {
		return p.getSelf(env.soudayo!!)
	}

	override suspend fun ownOrGotChart(env: DataFetchingEnvironment): List<DetailedChartModel> {
		return p.getOwnedOrGotCharts(env.soudayo!!)
	}

	override suspend fun charts(env: DataFetchingEnvironment, limit: Int?, skip: Int?, ranked: Int?): List<DetailedChartModel> {
		return p.getOwnedCharts(env.soudayo!!, limit!!, skip!!, ranked == 1)
	}

	override suspend fun assessmentGroup(limit: Int?, skip: Int?): List<AssessmentGroupModel> {
		return p.getAssessmentGroups(limit!!, skip!!)
	}

	override suspend fun assessmentRank(
		assessmentGroupId: String?,
		medalLevel: Int?,
		skip: NNInt?,
		limit: NNInt?
	): List<AssessmentRecordWithRankModel> {
		return p.getAssessmentRank(
			assessmentGroupId!!,
			medalLevel!!,
			limit!!.value,
			skip!!.value
		)
	}

	override suspend fun setById(_id: String?): SetModel {
		return p.getSet(_id!!)
	}

	override suspend fun userByUsername(username: String?): UserModel {
		return p.getUser(username!!)
	}

	override suspend fun playRank(chartId: String?, skip: NNInt?, limit: NNInt?): List<PlayRecordWithRank> {
		return p.getPlayRank(chartId!!, limit!!.value, skip!!.value)
	}

	override suspend fun refreshSet(setVersion: List<ChartSetAndVersion>): List<ClassifiedModels.Set> {
		return setVersion.map { p.getSet(it.setId) }.map {
			ClassifiedModels.Set(it._id, it.isRanked, it.introduction, it.noter.username, it.musicTitle)
		}
	}
}