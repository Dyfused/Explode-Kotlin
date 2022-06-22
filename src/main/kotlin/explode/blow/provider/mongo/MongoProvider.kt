package explode.blow.provider.mongo

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.UpdateOptions
import explode.blow.graphql.model.*
import explode.blow.provider.IBlowFullProvider
import explode.blow.provider.mongo.RandomUtil.randomId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.OffsetDateTime
import java.util.*

class MongoProvider(connectionString: String? = null) : IBlowFullProvider {

	private val logger = LoggerFactory.getLogger("MongoProvider")
	private val mongo = (if(connectionString == null) KMongo.createClient() else KMongo.createClient(connectionString))
	private val db = mongo.getDatabase("Explode")

	private val userC = db.getCollection<UserModel>("User")

	private fun getUser(userId: String): UserModel? {
		return userC.findOne(UserModel::_id eq userId)
	}

	private fun getUserByName(username: String): UserModel? {
		return userC.findOne(UserModel::username eq username)
	}

	private fun getUserByToken(token: String): UserModel? {
		return userC.findOne(UserModel::token eq token)
	}

	private fun UserModel.updateDb() {
		userC.updateOne(this, UpdateOptions().upsert(true))
	}

	fun createNewUser(username: String, password: String): UserModel {
		return UserModel(
			UUID.randomUUID().toString(),
			username,
			listOf(),
			listOf(),
			listOf(),
			0,
			0,
			0,
			OffsetDateTime.now(),
			UUID.randomUUID().toString(),
			0,
			AccessData(false)
		).apply {
			updateDb()
			// upload Username-Password data
			UserIdAndPassword(this._id, password).apply { updateDb() }
		}
	}

	// LOGIN

	private val loginC = db.getCollection<UserIdAndPassword>("UserLogin")

	@Serializable
	data class UserIdAndPassword(@SerialName("_id") val userId: String, val password: String)

	private val UserModel.password get() = loginC.findOne(UserIdAndPassword::userId eq _id)!!.password

	private fun UserIdAndPassword.updateDb() {
		loginC.updateOne(this, UpdateOptions().upsert(true))
	}

	// Charts
	private val chartC = db.getCollection<DetailedChartModel>("Chart")
	private val chartSetC = db.getCollection<SetModel>("ChartSet")

	private fun getDetailedChart(id: String): DetailedChartModel? {
		return chartC.findOne(DetailedChartModel::_id eq id)
	}

	private fun getSet(id: String): SetModel? {
		return chartSetC.findOne(SetModel::_id eq id)
	}

	private fun DetailedChartModel.minimal(): ChartModel {
		return ChartModel(_id, difficultyBase, difficultyValue)
	}

	private fun DetailedChartModel.updateDb() {
		chartC.updateOne(this, UpdateOptions().upsert(true))
	}

	private fun SetModel.updateDb() {
		chartSetC.updateOne(this, UpdateOptions().upsert(true))
	}

	fun createNewChart(charter: UserModel, chartName: String, gcPrice: Int, musicianName: String, difficultyBase: Int, difficultyValue: Int, specifiedId: String = randomId()): DetailedChartModel {
		return DetailedChartModel(
			specifiedId,
			charter,
			chartName,
			gcPrice,
			MusicModel(musicianName),
			difficultyBase,
			difficultyValue
		).apply {
			updateDb()
		}
	}

	fun createNewSet(intro: String, price: Int, noterUsername: String, title: String, composer: String, charts: List<DetailedChartModel>, specifiedId: String = randomId()): SetModel {
		return SetModel(
			specifiedId,
			intro,
			price,
			NoterModel(noterUsername),
			title,
			composer,
			0,
			charts.map { it.minimal() },
			isGot = false,
			isRanked = false,
			isOfficial = false,
			""
		).apply {
			updateDb()
		}
	}

	private fun getSetStoreList(
		limit: Int,
		skip: Int,
		searchedName: String,
		showHidden: Boolean,
		showOfficial: Boolean,
		showRanked: Boolean,
		showUnranked: Boolean,
	): List<SetModel> {
		// logger.info("$limit, $skip, $searchedName, $showHidden, $showOfficial, $showRanked, $showUnranked")
		return if(searchedName.isNotEmpty()) {
			chartSetC.find(SetModel::musicTitle eq searchedName).limit(limit).skip(skip).toList()
		} else if(showHidden) {
			chartSetC.find(SetModel::coinPrice eq -1).limit(limit).skip(skip).toList()
		} else if(showOfficial) {
			chartSetC.find(SetModel::isOfficial eq true).limit(limit).skip(skip).toList()
		} else if(showRanked) {
			chartSetC.find(SetModel::isRanked eq true).limit(limit).skip(skip).toList()
		} else if(showUnranked) {
			chartSetC.find(SetModel::isRanked eq false).limit(limit).skip(skip).toList()
		} else {
			listOf()
		}
	}

	// File System

	@Serializable
	data class IdToFile(val _id: String, @Serializable(with = KFileSerializer::class) val file: File)

	private val fdb: MongoDatabase = mongo.getDatabase("Explode_File")

	val chartFiles = fdb.getCollection<IdToFile>("ChartFile")
	val coverFiles = fdb.getCollection<IdToFile>("CoverFile")
	val musicFiles = fdb.getCollection<IdToFile>("MusicFile")
	val previewFiles = fdb.getCollection<IdToFile>("PreviewFile")

	// IBlowProvider methods

	private fun getUserByTokenOrThrow(token: String) =
		getUserByToken(token) ?: error("Cannot find the User of the Token.")

	private val self = ProviderSelfModel(this)
	private val reviewer = ProviderReviewerModel(this)

	override fun getSelf(soudayo: String): SelfModel {
		return self
	}

	override fun getReviewer(soudayo: String): ReviewerModel {
		return reviewer
	}

	override fun getReviewRequests(
		soudayo: String,
		limit: Int,
		skip: Int,
		status: Int,
		searchStr: String
	): List<ReviewRequestModel> {
		return listOf()
	}

	override fun getGotSets(soudayo: String): List<SetModel> {
		val u = getUserByTokenOrThrow(soudayo)
		return u.ownSet.map { getSet(it)!! }
	}

	override fun getOwnedOrGotCharts(soudayo: String): List<DetailedChartModel> {
		val u = getUserByTokenOrThrow(soudayo)
		return u.ownChart.map { getDetailedChart(it)!! }
	}

	override fun getOwnedCharts(soudayo: String, limit: Int, skip: Int, isRanked: Boolean): List<DetailedChartModel> {
		val u = getUserByTokenOrThrow(soudayo)
		return u.ownChart.map { getDetailedChart(it)!! }
	}

	private val gameSetting = GameSettingModel(81)

	override fun getGameSetting(soudayo: String): GameSettingModel {
		return gameSetting
	}

	override fun getUser(soudayo: String, username: String): UserModel? {
		return getUserByName(username)
	}

	override fun loginUser(username: String, password: String): UserModel {
		val u = getUserByName(username) ?: error("Invalid username.")
		return if(u.password == password) {
			u
		} else {
			error("Invalid password.")
		}
	}

	override fun registerUser(username: String, password: String): UserModel {
		return createNewUser(username, password).apply {
			logger.info("New User(name=$username, token=$token, password=$password) created.")
		}
	}

	override fun getSet(soudayo: String, id: String): SetModel {
		return getSet(id) ?: error("Cannot find the set.")
	}

	override fun getSets(
		soudayo: String,
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
		return getSetStoreList(limit, skip, searchedName, showHidden, showOfficial, showRanked, showUnranked)
	}

	override fun buySet(soudayo: String, id: String): ExchangeSetModel {
		val u = getUserByTokenOrThrow(soudayo)
		val s = getSet(id) ?: error("Cannot find Chart Set($id).")

		if((u.coin ?: 0) >= s.coinPrice) {
			val newCoin = (u.coin ?: 0) - s.coinPrice
			val newOwnSet = u.ownSet + s._id
			val newOwnChart = u.ownChart + s.chart.map { it._id }

			u.copy(coin = newCoin, ownSet = newOwnSet, ownChart = newOwnChart).apply { updateDb() }

			logger.info("User(${u.username}) has just bought Set(${s.musicTitle}).")

			return ExchangeSetModel(newCoin)
		} else {
			error("Cannot afford.")
		}
	}

	override fun getAssessmentGroups(soudayo: String, limit: Int, skip: Int): List<AssessmentGroupModel> {
		return listOf()
	}

	override fun getAssessmentRank(
		soudayo: String,
		assessmentGroupId: String,
		medalLevel: Int,
		limit: Int,
		skip: Int
	): List<AssessmentRecordWithRankModel> {
		return listOf()
	}

	override fun getAssessmentRankSelf(
		soudayo: String,
		assessmentGroupId: String,
		medalLevel: Int
	): AssessmentRecordWithRankModel? {
		return null
	}

	override fun getPlayRankSelf(soudayo: String, chartId: String): PlayRecordWithRank? {
		return null
	}

	override fun getPlayRank(soudayo: String, chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank> {
		return listOf()
	}

	override fun submitBeforeAssessment(soudayo: String, assessmentId: String, medal: Int): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel("0"))
	}

	override fun submitBeforePlay(
		soudayo: String,
		chartId: String,
		ppCost: Int,
		eventArgs: String
	): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel("0"))
	}

	override fun submitAfterAssessment(
		soudayo: String,
		records: List<PlayRecordInput>,
		randomId: String
	): AfterAssessmentModel {
		if(randomId != "0") error("F")
		val u = getUserByTokenOrThrow(soudayo)
		return AfterAssessmentModel(1, u.RThisMonth ?: 0, u.coin, u.diamond)
	}

	override fun submitAfterPlay(soudayo: String, record: PlayRecordInput, randomId: String): AfterPlaySubmitModel {
		if(randomId != "0") error("F")
		val u = getUserByTokenOrThrow(soudayo)
		return AfterPlaySubmitModel(RankingModel(false, RankModel(1)), u.RThisMonth ?: 0, u.coin, u.diamond)
	}

	override fun getChartFile(chartId: String?): File? {
		return chartFiles.findOne(IdToFile::_id eq chartId)?.file
	}

	override fun getMusicFile(setId: String?): File? {
		return musicFiles.findOne(IdToFile::_id eq setId)?.file
	}

	override fun getPreviewFile(setId: String?): File? {
		return previewFiles.findOne(IdToFile::_id eq setId)?.file
	}

	override fun getSetCoverFile(setId: String?): File? {
		return coverFiles.findOne(IdToFile::_id eq setId)?.file
	}

	override fun getStorePreviewFile(setId: String?): File? {
		return null // TODO
	}

	override fun getUserAvatarFile(userId: String?): File? {
		return null // TODO
	}
}