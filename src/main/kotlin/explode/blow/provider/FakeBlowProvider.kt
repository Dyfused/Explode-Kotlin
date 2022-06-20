package explode.blow.provider

import explode.blow.graphql.model.*
import java.time.LocalDate
import java.time.OffsetDateTime

object FakeBlowProvider : IBlowProvider {

	private val FakeChart1Mega = ChartModel(
		"5f9a0d952496ed90dd75b054",
		4,
		14
	)

	private val FakeChart2Giga = ChartModel(
		"5c966fe9ec41aa750fc85870",
		5,
		15
	)

	private val FakeSet1 = SetModel(
		"5f9a0d952496ed90dd75b054",
		"Intro",
		10,
		NoterModel("FakeNoter"),
		"FakeMusicTitle",
		"FakeComposer",
		1,
		listOf(FakeChart1Mega, FakeChart1Mega),
		isGot = true,
		isRanked = true,
		OverridePriceStr = ""
	)

	private val FakeSet2 = SetModel(
		"5c966fe9ec41aa750fc85870",
		"Intro",
		100,
		NoterModel("(TangScend)"),
		"無人区-Vacuum Track#ADD8E6",
		"Unknown",
		1,
		listOf(FakeChart2Giga),
		isGot = false,
		isRanked = true,
		OverridePriceStr = ""
	)

	private val FakePlayerKaede = PlayerModel(
		"1",
		"楓，测试玩家",
		5,
		9999
	)

	private val FakePlayerNix = PlayerModel(
		"2",
		"Nix，测试玩家",
		2,
		2345
	)

	private val FakeAssessmentRecordWithRank1 = AssessmentRecordWithRankModel(
		FakePlayerKaede,
		1,
		100.0,
		1,
		OffsetDateTime.now()
	)

	private val FakeAssessmentRecordWithRank2 = AssessmentRecordWithRankModel(
		FakePlayerNix,
		2,
		85.3,
		1,
		OffsetDateTime.now()
	)

	private val FakeReviewRequestRanked = ReviewRequestModel(
		FakeSet1,
		false
	)

	private val FakeReviewRequestUnranked = ReviewRequestModel(
		FakeSet1,
		true
	)

	private val FakeUser = UserModel(
		"1",
		"楓，测试用户",
		listOf(),
		listOf(),
		listOf(),
		0,
		9999,
		9999,
		LocalDate.of(2000, 1, 1).toString(),
		"fake-tokenizer",
		9999,
		AccessData(true)
	)

	private val FakeAssessmentChart1 = AssessmentChartModel(
		"5f9a0d952496ed90dd75b054",
		FakeSet1
	)

	private val FakeAssessmentChart2 = AssessmentChartModel(
		"5c966fe9ec41aa750fc85870",
		FakeSet2
	)

	private val FakeAssessmentRecords1 = AssessmentRecordsModel(
		100.0,
		true,
		listOf(AssessmentPlayRecordModel(1024, 0, 0, 1000000), AssessmentPlayRecordModel(1023, 1, 0, 999999))
	)

	private val FakeAssessmentRecords2 = AssessmentRecordsModel(
		100.0,
		true,
		listOf(AssessmentPlayRecordModel(4096, 0, 0, 1000000), AssessmentPlayRecordModel(4095, 1, 0, 999999))
	)

	private val FakeAssessment1 = AssessmentModel(
		"1",
		1,
		10.0,
		10.0,
		15.0,
		20.0,
		listOf(FakeAssessmentChart1, FakeAssessmentChart2),
		listOf(FakeAssessmentRecords1, FakeAssessmentRecords2)
	)

	private val FakeAssessment2 = AssessmentModel(
		"2",
		2,
		10.0,
		10.0,
		15.0,
		20.0,
		listOf(FakeAssessmentChart1),
		listOf(FakeAssessmentRecords2)
	)

	private val FakeAssessmentGroup = AssessmentGroupModel(
		"1",
		"Assessment",
		listOf(FakeAssessment1, FakeAssessment2)
	)

	private val FakePlayRecordWithRank1 = PlayRecordWithRank(
		FakePlayerKaede,
		PlayMod(1, 1, isBleed = true, isMirror = false),
		1,
		1000000,
		1024,
		0,
		0,
		LocalDate.now()
	)

	private val FakePlayRecordWithRank2 = PlayRecordWithRank(
		FakePlayerNix,
		PlayMod(1, 1, isBleed = true, isMirror = false),
		2,
		1000000,
		1024,
		0,
		0,
		LocalDate.now()
	)

	class SelfSoudayoModel(private val soudayo: String): SelfModel() {
		override suspend fun gotSet(): List<SetModel> {
			return getGotSets(soudayo)
		}

		override suspend fun assessmentRankSelf(
			assessmentGroupId: String?,
			medalLevel: Int?
		): AssessmentRecordWithRankModel {
			return getAssessmentRankSelf(soudayo, assessmentGroupId!!, medalLevel!!)
		}

		override suspend fun playRankSelf(chartId: String?): PlayRecordWithRank? {
			return getPlayRankSelf(chartId!!)
		}
	}

	class ReviewerSoudayoModel(private val soudayo: String): ReviewerModel() {
		override suspend fun reviewRequest(
			limit: Int?,
			skip: Int?,
			status: Int?,
			searchStr: String?
		): List<ReviewRequestModel> {
			return getReviewRequests(limit!!, skip!!, status!!, searchStr!!)
		}
	}

	override fun getSelf(soudayo: String): SelfModel {
		return SelfSoudayoModel(soudayo)
	}

	override fun getReviewer(soudayo: String): ReviewerModel {
		return ReviewerSoudayoModel(soudayo)
	}

	override fun getReviewRequests(limit: Int, skip: Int, status: Int, searchStr: String): List<ReviewRequestModel> {
		return if(skip < 2) listOf(FakeReviewRequestRanked, FakeReviewRequestUnranked) else listOf()
	}

	override fun getGotSets(soudayo: String): List<SetModel> {
		return listOf(FakeSet1)
	}

	override fun getOwnedOrGotCharts(soudayo: String): List<DetailedChartModel> {
		return listOf()
	}

	override fun getOwnedCharts(soudayo: String, limit: Int, skip: Int, isRanked: Boolean): List<DetailedChartModel> {
		return listOf()
	}

	override fun getGameSetting(): GameSettingModel {
		return GameSettingModel(81)
	}

	override fun getUser(username: String): UserModel {
		return FakeUser
	}

	override fun loginUser(username: String, password: String): UserModel {
		return FakeUser
	}

	override fun registerUser(username: String, password: String): UserModel {
		return FakeUser
	}

	override fun getSet(id: String): SetModel {
		return FakeSet1
	}

	override fun getSets(
		limit: Int,
		skip: Int,
		searchedName: String,
		showHidden: Boolean,
		showOfficial: Boolean,
		showRanked: Boolean,
		showUnranked: Boolean,
		orderedByPlayCount: Boolean,
		orderedByPublishTime: Boolean
	): List<SetModel> {
		return if(skip < 1) listOf(FakeSet1) else listOf()
	}

	override fun buySet(soudayo: String, id: String): ExchangeSetModel {
		return ExchangeSetModel(-999)
	}

	override fun getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel> {
		return if(skip < 1) listOf(FakeAssessmentGroup) else listOf()
	}

	override fun getAssessmentRank(
		assessmentGroupId: String,
		medalLevel: Int,
		limit: Int,
		skip: Int
	): List<AssessmentRecordWithRankModel> {
		return if(medalLevel == 1 && skip < 2) listOf(FakeAssessmentRecordWithRank1, FakeAssessmentRecordWithRank2) else listOf()
	}

	override fun getAssessmentRankSelf(
		soudayo: String,
		assessmentGroupId: String,
		medalLevel: Int
	): AssessmentRecordWithRankModel {
		return FakeAssessmentRecordWithRank1
	}

	override fun getPlayRankSelf(chartId: String): PlayRecordWithRank? {
		return null
	}

	override fun getPlayRank(chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank> {
		return if(skip < 1) listOf(FakePlayRecordWithRank1) else listOf()
	}

	override fun submitBeforeAssessment(soudayo: String, assessmentId: String, medal: Int): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(LocalDate.now().toString(), PlayingRecordModel("rAndOmAsseSs"))
	}

	override fun submitBeforePlay(
		soudayo: String,
		chartId: String,
		ppCost: Int,
		eventArgs: String
	): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(LocalDate.now().toString(), PlayingRecordModel("rAndOm"))
	}

	override fun submitAfterAssessment(
		soudayo: String,
		records: List<PlayRecordInput>,
		randomId: String
	): AfterAssessmentModel {
		return AfterAssessmentModel(1, 8888, 999, 888)
	}

	override fun submitAfterPlay(soudayo: String, record: PlayRecordInput, randomId: String): AfterPlaySubmitModel {
		return AfterPlaySubmitModel(RankingModel(true, RankModel(1)), 6789, 888, 999)
	}
}