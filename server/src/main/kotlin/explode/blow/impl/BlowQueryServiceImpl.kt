package explode.blow.impl

import explode.backend.graphql.NNInt
import explode.blow.*
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.database.SetStatus
import explode.dataprovider.model.database.StoreSort
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.fail
import graphql.schema.DataFetchingEnvironment
import kotlin.properties.Delegates

class BlowQueryServiceImpl(private val p: IBlowAccessor) : BlowQueryService {

	private val self = ProviderSelfService(p)
	private val reviewer = ProviderReviewerService(p)

	override suspend fun hello(env: DataFetchingEnvironment): String {
		return "You've been waiting, \"${env.soudayo}\"."
	}

	override suspend fun gameSetting(env: DataFetchingEnvironment): GameSettingModel {
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

		var category: SetStatus? by Delegates.observable(null) { _, old, new ->
			if(old != null) {
				error("Failed to serialize the request, the category has been decided $old, but received a new value $new.")
			}
		}

		if(isHidden == 1) category = SetStatus.HIDDEN
		if(isOfficial == 1) category = SetStatus.OFFICIAL
		if(isRanked == 1) category = SetStatus.RANKED
		if(isRanked == -1) category = SetStatus.UNRANKED

		if(category == null) error("Failed to serailze the request, the category is undefined.")

		var sort: StoreSort? by Delegates.observable(null) { _, old, new ->
			if(old != null) {
				error("Failed to serialize the request, the sort has been decided $old, but received a new value $new.")
			}
		}

		if(playCountOrder == 1) sort = StoreSort.PLAY_COUNT
		if(publishTimeOrder == 1) sort = StoreSort.PUBLISH_TIME

		if(sort == null) error("Failed to serialize the request, the sort is undefined.")

//		return p.getSets(
//			limit!!.value,
//			skip!!.value,
//			musicTitle!!,
//			isRanked == 1,
//			isOfficial == 1,
//			false,
//			isHidden == 1,
//			playCountOrder == -1,
//			publishTimeOrder == -1
//		).map { it.tunerize }

		return p.getSets(limit!!.value, skip!!.value, musicTitle!!, category!!, sort!!).map { it.tunerize }
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
		return with(p) { p.getUserByToken(env.soudayo)?.getAssessmentGroups(limit!!, skip!!) ?: listOf() }
	}

	override suspend fun assessmentRank(
		env: DataFetchingEnvironment,
		assessmentGroupId: String?,
		medalLevel: Int?,
		skip: NNInt?,
		limit: NNInt?
	): List<AssessmentRecordWithRankModel> {
		return p.getAssessmentRank(assessmentGroupId!!, medalLevel!!, limit!!.value, skip!!.value)
	}

	override suspend fun setById(env: DataFetchingEnvironment, _id: String?): SetModel = with(p) {
		return p.getSet(_id!!)?.tunerize ?: fail("Invalid setId: $_id")
	}

	override suspend fun userByUsername(env: DataFetchingEnvironment, username: String?): UserModel? = with(p) {
		return p.getUserByName(username!!)?.tunerize
	}

	override suspend fun playRank(env: DataFetchingEnvironment, chartId: String?, skip: NNInt?, limit: NNInt?): List<PlayRecordWithRank> {
		return p.getPlayRank(chartId!!, limit!!.value, skip!!.value)
	}

	override suspend fun refreshSet(env: DataFetchingEnvironment, setVersion: List<ChartSetAndVersion>): List<ClassifiedModels.Set> = with(p) {
		return setVersion.mapNotNull { getSet(it.setId) }.map {
			ClassifiedModels.Set(it.id, it.status.isRanked, it.introduction ?: "", getUser(it.noterId)?.username ?: "unknown", it.musicName)
		}
	}
}