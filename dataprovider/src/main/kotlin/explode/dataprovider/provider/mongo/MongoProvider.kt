@file:Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package explode.dataprovider.provider.mongo

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.*
import explode.dataprovider.model.*
import explode.dataprovider.provider.*
import explode.dataprovider.provider.mongo.RandomUtil.randomId
import kotlinx.serialization.*
import org.bson.types.Binary
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import kotlin.math.pow
import kotlin.math.round

class MongoProvider(connectionString: String? = null) : IBlowAccessor, IBlowUserAccessor, IBlowDataProvider,
	IBlowResourceProvider by BlowFileResourceProvider(File(".explode_data")) {

	private val logger = LoggerFactory.getLogger("MongoProvider")
	private val mongo = (if(connectionString == null) KMongo.createClient() else KMongo.createClient(connectionString))
	private val db = mongo.getDatabase("Explode")

	private val userC = db.getCollection<UserModel>("User")

	/**
	 * Used for inserting or updating new data.
	 */
	private inline fun <reified T : Any> T.upsert(coll: MongoCollection<T>): T = apply {
		coll.updateOne(this, UpdateOptions().upsert(true))
	}

	/**
	 * Official User is used to be the default value of charts whose noter is not currently available.
	 */
	val officialUser: UserModel get() = getUserByName("official") ?: createUser("official", "official_is_unbreakable")

	override fun getUser(userId: String): UserModel? {
		return userC.findOne(UserModel::_id eq userId)
	}

	override fun getUserByName(username: String): UserModel? {
		return userC.findOne(UserModel::username eq username)
	}

	override fun getUserByToken(soudayo: String): UserModel? {
		return userC.findOne(UserModel::token eq soudayo)
	}

	private val UserModel.asPlayer: PlayerModel
		get() = PlayerModel(_id, username, highestGoldenMedal ?: 0, RThisMonth ?: 0)

	private fun getPlayer(userId: String): PlayerModel? {
		return getUser(userId)?.asPlayer
	}

	private fun getPlayerByName(userId: String): PlayerModel? {
		return getUserByName(userId)?.asPlayer
	}

	private fun getPlayerByToken(token: String): PlayerModel? {
		return getUserByToken(token)?.asPlayer
	}

	override fun updateUser(userModel: UserModel): UserModel {
		return userModel.upsert(userC)
	}

	fun UserModel.update(block: UserModel.() -> Unit): UserModel {
		return this.apply(block).upsert(userC)
	}

	override fun createUser(username: String, password: String): UserModel {
		return UserModel(
			UUID.randomUUID().toString(),
			username,
			mutableListOf(),
			mutableListOf(),
			mutableListOf(),
			0,
			1000,
			0,
			OffsetDateTime.now(),
			UUID.randomUUID().toString(),
			0,
			0,
			AccessData(false)
		).upsert(userC).apply {
			// upload password data
			UserIdAndPassword(this._id, password).upsert(loginC)
		}
	}

	// LOGIN

	private val loginC = db.getCollection<UserIdAndPassword>("UserLogin")

	@Serializable
	data class UserIdAndPassword(@SerialName("_id") val userId: String, val password: String)

	override var UserModel.password
		get() = loginC.findOne(UserIdAndPassword::userId eq _id)!!.password
		set(value) {
			loginC.findOneAndUpdate(UserIdAndPassword::userId eq this._id, UserIdAndPassword::password eq value)
		}

	// Charts
	private val chartC = db.getCollection<DetailedChartModel>("Chart")
	private val chartSetC = db.getCollection<SetModel>("ChartSet")

	private fun getDetailedChart(id: String): DetailedChartModel? {
		return chartC.findOne(DetailedChartModel::_id eq id)
	}

	override fun getSet(setId: String): SetModel {
		return chartSetC.findOne(SetModel::_id eq setId) ?: error("Cannot find Set($setId)")
	}

	override fun getChart(chartId: String): DetailedChartModel {
		return chartC.findOne(DetailedChartModel::_id eq chartId) ?: error("Cannot find Chart($chartId)")
	}

	private fun getSetByChart(chartId: String): SetModel? {
		return chartSetC.findOne(SetModel::chart elemMatch (ChartModel::_id eq chartId))
	}

	private fun DetailedChartModel.minimal(): ChartModel {
		return ChartModel(_id, difficultyBase, difficultyValue)
	}

	override fun createChart(
		chartName: String,
		charterUser: UserModel,
		musicianName: String,
		difficultyClass: Int,
		difficultyValue: Int,
		gcPrice: Int,
		D: Double?
	): DetailedChartModel {
		return DetailedChartModel(
			randomId(), charterUser, chartName, gcPrice, MusicModel(musicianName), difficultyClass, difficultyValue, D
		).upsert(chartC)
	}

	override fun createSet(
		setTitle: String,
		composerName: String,
		noterName: String,
		chart: List<ChartModel>,
		isRanked: Boolean,
		introduction: String,
		coinPrice: Int,
		OverridePriceStr: String,
		needReview: Boolean
	): SetModel {
		return SetModel(
			randomId(),
			introduction,
			coinPrice,
			NoterModel(noterName),
			setTitle,
			composerName,
			0,
			chart,
			false,
			isRanked,
			false,
			OverridePriceStr,
			needReview
		).upsert(chartSetC)
	}

	override fun updateChart(detailedChartModel: DetailedChartModel): DetailedChartModel {
		return detailedChartModel.upsert(chartC)
	}

	fun DetailedChartModel.update(block: DetailedChartModel.() -> Unit): DetailedChartModel {
		return this.apply(block).upsert(chartC)
	}

	override fun updateSet(setModel: SetModel): SetModel {
		return setModel.upsert(chartSetC)
	}

	fun SetModel.update(block: SetModel.() -> Unit): SetModel {
		return this.apply(block).upsert(chartSetC)
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

	fun getSetList(
		limit: Int,
		skip: Int,
		searchedName: String? = null,
		isHidden: Boolean? = null,
		isOfficial: Boolean? = null,
		isRanked: Boolean? = null,
		isNeedReview: Boolean? = null
	): List<SetModel> {
		val filter = buildList {
			if(searchedName != null) add(SetModel::musicTitle eq searchedName)
			if(isHidden != null) if(isHidden) add(SetModel::coinPrice eq -1) else add(SetModel::coinPrice ne -1)
			if(isOfficial != null) add(SetModel::isOfficial eq isOfficial)
			if(isRanked != null) add(SetModel::isRanked eq isRanked)
			if(isNeedReview != null) add(SetModel::needReview eq isNeedReview)
		}.let(::and)
		return chartSetC.find(filter).limit(limit).skip(skip).toList()
	}

	// Play Record

	@Serializable
	data class PlayRecordData(
		val _id: String,
		val playerId: String,
		val playedChartId: String,
		val score: Int,
		val perfect: Int,
		val good: Int,
		val miss: Int,
		val playMod: PlayMod,
		@Contextual val time: OffsetDateTime
	)

	private val playRecordC = db.getCollection<PlayRecordData>("PlayRecord")

	@Serializable
	data class PlayingData(
		@SerialName("_id") val randomId: String,
		val chartId: String,
		val ppCost: Int,
		@Contextual val createTime: OffsetDateTime = OffsetDateTime.now()
	)

	private val playingC = db.getCollection<PlayingData>("PlayingData")

	init {
		playingC.drop()
	}

	private fun updatePlayerScoreOnChart(playerId: String, chartId: String, record: PlayRecordInput): PlayRecordData {
		val old =
			playRecordC.findOne(and(PlayRecordData::playerId eq playerId, PlayRecordData::playedChartId eq chartId))
		val new = PlayRecordData(
			randomId(),
			playerId,
			chartId,
			record.score!!,
			record.perfect!!,
			record.good!!,
			record.miss!!,
			record.mod!!.normal,
			OffsetDateTime.now()
		)
		if(old == null) {
			playRecordC.insertOne(new)
		} else {
			if(old.score <= new.score) {
				playRecordC.updateOneById(old._id, new)
			}
		}
		return new
	}

	@Suppress("UNUSED_PARAMETER")
	private fun updatePlayerRValue(playerId: String, chartId: String, record: PlayRecordInput) {
		// TODO: Find a way to calculate R
	}

	private fun calcR(d: Double): Double = if(d <= 5.5) {
		50.0
	} else {
		round((0.5813 * d.pow(3) - (3.28 * d.pow(2) + (14.43 * d) - 29.3)))
	}

	// File System

	val binaryC = db.getCollection<IdToBytes>("BinaryData")

	@Serializable
	data class IdToBytes(val _id: String, @Contextual val data: Binary)

	fun uploadData(id: String, data: ByteArray) {
		IdToBytes(id, Binary(data)).upsert(binaryC)
	}

	fun downloadData(id: String): ByteArray? {
		return binaryC.findOne(IdToBytes::_id eq id)?.data?.data
	}

	// IBlowProvider methods

	@Serializable
	private data class PlayRecordDataRanked(
		val _id: String,
		val playerId: String,
		val playedChartId: String,
		val score: Int,
		val perfect: Int,
		val good: Int,
		val miss: Int,
		val playMod: PlayMod,
		@Contextual val time: OffsetDateTime,
		val ranking: Int
	)

	override fun getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel> {
		return emptyList()
	}

	override fun getAssessmentRank(
		assessmentGroupId: String, medalLevel: Int, limit: Int, skip: Int
	): List<AssessmentRecordWithRankModel> {
		return emptyList()
	}

	override val gameSetting = GameSettingModel(81)

	override fun loginUser(username: String, password: String): UserModel {
		val u = getUserByName(username) ?: error("Invalid username.")
		return if(u.password == password) {
			u
		} else {
			error("Invalid password.")
		}
	}

	override fun registerUser(username: String, password: String): UserModel {
		return createUser(username, password).apply {
			logger.info("New User(name=$username, token=$token, password=$password) created.")
		}
	}

	override fun getSets(
		limit: Int,
		skip: Int,
		searchedName: String,
		onlyRanked: Boolean,
		onlyOfficial: Boolean,
		onlyReview: Boolean,
		onlyHidden: Boolean,
		playCountOrder: Boolean,
		publishTimeOrder: Boolean
	): List<SetModel> {
		return getSetStoreList(limit, skip, searchedName, onlyRanked, onlyOfficial, onlyReview, onlyHidden)
	}

	override fun UserModel.buySet(id: String): ExchangeSetModel {
		val s = getSet(id)

		// already bought
		if(id in this.ownSet) return ExchangeSetModel(this.coin)

		val coinRemain = (this.coin ?: 0) - s.coinPrice
		if(coinRemain >= 0) {
			this.update {
				coin = coinRemain
				ownSet += s._id
				ownChart += s.chart.map(ChartModel::_id)
			}
			logger.info("User(${this.username}) has just bought Set(${s.musicTitle}).")
			return ExchangeSetModel(this.coin)
		} else {
			error("Cannot afford.")
		}
	}

	override fun UserModel.getAssessmentRankSelf(
		assessmentGroupId: String, medalLevel: Int
	): AssessmentRecordWithRankModel? {
		return null
	}

	private val aggregateRanking =
		Aggregates.setWindowFields(null, PlayRecordDataRanked::score eq -1, WindowedComputations.rank("ranking"))

	override fun UserModel.getPlayRankSelf(chartId: String): PlayRecordWithRank? {
		val playerId = this._id
		return playRecordC.aggregate<PlayRecordDataRanked>(
			match(PlayRecordDataRanked::playedChartId eq chartId),
			aggregateRanking,
			match(PlayRecordDataRanked::playerId eq playerId)
		).map { (_, _, _, score, perfect, good, miss, mod, time, ranking) ->
			PlayRecordWithRank(this.asPlayer, mod, ranking, score, perfect, good, miss, time)
		}.firstOrNull()
	}

	override fun getPlayRank(chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank> {
		return playRecordC.aggregate<PlayRecordDataRanked>(
			match(PlayRecordDataRanked::playedChartId eq chartId), aggregateRanking, skip(skip), limit(limit)
		).map { (_, playerId, _, score, perfect, good, miss, mod, time, ranking) ->
			PlayRecordWithRank(getPlayer(playerId)!!, mod, ranking, score, perfect, good, miss, time)
		}.toList()
	}

	override fun UserModel.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel("0"))
	}

	override fun UserModel.submitBeforePlay(
		chartId: String, ppCost: Int, eventArgs: String
	): BeforePlaySubmitModel {
		val p = PlayingData(randomId(), chartId, ppCost)
		playingC.insertOne(p)
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel(p.randomId))
	}

	override fun UserModel.submitAfterAssessment(
		records: List<PlayRecordInput>, randomId: String
	): AfterAssessmentModel {
		return AfterAssessmentModel(1, this.RThisMonth ?: 0, this.coin, this.diamond)
	}

	override fun UserModel.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel {
		val p = playingC.findOne(PlayingData::randomId eq randomId)!!

		val playerId = this._id
		val chartId = p.chartId

		// remove the data
		playingC.deleteOneById(p.randomId)

		val before = getPlayRankSelf(p.chartId)
		updatePlayerScoreOnChart(playerId, chartId, record)
		updatePlayerRValue(playerId, chartId, record)
		val after = getPlayRankSelf(p.chartId)

		val needUpdate = before == null || before.rank != after!!.rank

		return AfterPlaySubmitModel(
			RankingModel(needUpdate, RankModel(after!!.rank)), this.RThisMonth ?: 0, this.coin, this.diamond
		)
	}
}