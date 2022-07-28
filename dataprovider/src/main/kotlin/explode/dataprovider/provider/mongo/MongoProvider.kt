@file:Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package explode.dataprovider.provider.mongo

import TConfig.Configuration
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.*
import explode.dataprovider.detonate.ExplodeConfig.Companion.explode
import explode.dataprovider.model.*
import explode.dataprovider.provider.*
import explode.dataprovider.provider.mongo.MongoExplodeConfig.Companion.toMongo
import explode.dataprovider.provider.mongo.RandomUtil.randomId
import explode.dataprovider.provider.mongo.RandomUtil.randomIdUncrypted
import kotlinx.serialization.*
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MongoProvider(private val config: MongoExplodeConfig, val detonate: Detonate = Detonate(config)) : IBlowAccessor,
	IBlowUserAccessor, IBlowDataProvider,
	IBlowResourceProvider by detonate.resourceProvider {

	constructor() : this(Configuration(File("./provider.cfg")).explode().toMongo())

	private val logger = LoggerFactory.getLogger("Mongo")
	private val mongo = (KMongo.createClient(config.connectionString))
	private val db = mongo.getDatabase(config.databaseName)

	init {
		logger.info("Database: ${config.databaseName} at ${config.connectionString}")
		logger.info("Resource: ${config.resourceDirectory}")
		logger.info("Unencrypted: ${config.applyUnencryptedFixes}")
	}

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

	override val emptyUser: UserModel
		get() = officialUser

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
		logger.info("Creating user with parameters [username=$username, password=$password]")
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
		D: Double?,
		defaultId: String?
	): DetailedChartModel {
		return DetailedChartModel(
			defaultId ?: if(config.applyUnencryptedFixes) randomIdUncrypted() else randomId(),
			charterUser,
			chartName,
			gcPrice,
			MusicModel(musicianName),
			difficultyClass,
			difficultyValue,
			D
		).upsert(chartC).apply { logger.info("Created DetailedChart: $this") }
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
		needReview: Boolean,
		defaultId: String?
	): SetModel {
		return SetModel(
			defaultId ?: randomId(),
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
		).upsert(chartSetC).apply { logger.info("Created ChartSet: $this") }
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
			chartSetC.find("""{ "musicTitle": ${searchedName.toFuzzySearch()} }""").limit(limit).skip(skip).toList()
			// chartSetC.find(SetModel::musicTitle eq searchedName).limit(limit).skip(skip).toList()
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
		@Contextual val time: OffsetDateTime,
		val currentR: Double?
	)

	private val playRecordC = db.getCollection<PlayRecordData>("PlayRecord")

	@Serializable
	data class PlayingData(
		@SerialName("_id") val randomId: String,
		val chartId: String,
		val ppCost: Int,
		@Contextual val createTime: OffsetDateTime = OffsetDateTime.now()
	)

	private val playingDataCache = mutableMapOf<String, PlayingData>()

	init {
		thread(name = "PlayingDataGC", isDaemon = true) {
			while(true) {
				// execute every 10 mins
				Thread.sleep(Duration.ofMinutes(10).toMillis())

				val needRemoving = mutableListOf<String>()
				// select all the playingData existed greater than 1 hr.
				playingDataCache.forEach { (_, p) ->
					if(p.createTime + Duration.ofHours(1) < OffsetDateTime.now()) {
						needRemoving += p.randomId
					}
				}
				// remove 'em all
				needRemoving.forEach(playingDataCache::remove)
			}
		}
	}

	private fun UserModel.updatePlayerScoreOnChart(chartId: String, record: PlayRecordInput): PlayRecordData {
		// calculate R
		val r = getChart(chartId).D?.let {
			detonate.calcR(it, record)
		}

		val old =
			playRecordC.findOne(and(PlayRecordData::playerId eq _id, PlayRecordData::playedChartId eq chartId))
		val new = PlayRecordData(
			randomId(),
			_id,
			chartId,
			record.score!!,
			record.perfect!!,
			record.good!!,
			record.miss!!,
			record.mod!!.normal,
			OffsetDateTime.now(),
			r
		)
		if(old == null) {
			playRecordC.insertOne(new)
		} else {
			if(old.score < new.score) { // should not update if score equals, in order to keep the time order.
				playRecordC.updateOneById(old._id, new)
			}
		}
		logger.info("Updated PlayRecord: $old => $new")
		return new
	}

	fun UserModel.updatePlayerRValue(): UserModel {
		RThisMonth =
			playRecordC.find(PlayRecordData::playerId eq _id).sort(descending(PlayRecordData::currentR)).limit(20)
				.sumByDouble { it.currentR ?: 0.0 }.roundToInt()
		return updateUser(this)
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

	fun getAllSets(): FindIterable<SetModel> = chartSetC.find()

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
			logger.info("User[${this.username}] bought ChartSet(${s.musicTitle}) cost ${s.coinPrice} remaining ${coin}.")
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

	override fun UserModel.getLastPlayRecords(limit: Int, skip: Int): Iterable<PlayRecordWithRank> {
		return playRecordC.aggregate<PlayRecordDataRanked>(
			aggregateRanking,
			match(PlayRecordDataRanked::playerId eq _id),
			sort(descending(PlayRecordDataRanked::time)),
			limit(limit),
			skip(skip)
		).map { (_, _, _, score, perfect, good, miss, mod, time, ranking) ->
			PlayRecordWithRank(this.asPlayer, mod, ranking, score, perfect, good, miss, time)
		}
	}

	override fun UserModel.getBestPlayRecords(limit: Int, skip: Int): Iterable<PlayRecordWithRank> {
		return playRecordC.aggregate<PlayRecordDataRanked>(
			aggregateRanking,
			match(PlayRecordDataRanked::playerId eq _id),
			sort(descending(PlayRecordDataRanked::score)),
			limit(limit),
			skip(skip)
		).map { (_, _, _, score, perfect, good, miss, mod, time, ranking) ->
			PlayRecordWithRank(this.asPlayer, mod, ranking, score, perfect, good, miss, time)
		}
	}

	override fun UserModel.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel("0"))
	}

	override fun UserModel.submitBeforePlay(
		chartId: String, ppCost: Int, eventArgs: String
	): BeforePlaySubmitModel {
		// Uncrypted Fix: the Uncrypted charts SHOULD end with 4 zero, which will be lost in the argument sent to here.
		//                like actual Chart id is 6c4mcbcvwiw0ayniuze9azca,
		//                but we can only receive 6c4mcbcvwiw0ayniuze9.
		//                So I assert the last four character of the chart ID in Uncrypted Mode is zero,
		//                like this 6c4mcbcvwiw0ayniuze90000.
		//                And the actual chart ID need fixing ending up as 4 zeros as well.
		val fixedChartId = if(config.applyUnencryptedFixes) chartId + "0000" else chartId
		val p = PlayingData(randomId(), fixedChartId, ppCost)
		playingDataCache[p.randomId] = p
		logger.info(
			"User[${this.username}] submited a play request of ChartSet[id=$chartId], expires at ${
				p.createTime + Duration.ofHours(
					1
				)
			}. [${p.randomId}]"
		)
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel(p.randomId))
	}

	override fun UserModel.submitAfterAssessment(
		records: List<PlayRecordInput>, randomId: String
	): AfterAssessmentModel {
		return AfterAssessmentModel(1, this.RThisMonth ?: 0, this.coin, this.diamond)
	}

	override fun UserModel.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel {
		val p = playingDataCache[randomId] ?: error("Invalid randomId.")

		val chartId = p.chartId

		// remove the data
		playingDataCache -= randomId

		// check record
		val before = getPlayRankSelf(chartId)
		updatePlayerScoreOnChart(chartId, record)
		updatePlayerRValue()
		val after = getPlayRankSelf(chartId)

		val needUpdate = before == null || before.rank != after!!.rank

		// get coins
		val chartSet = getSetByChart(chartId) ?: error("Invalid Chart: Cannot find Set by Chart($chartId).")
		val chart = getChart(chartId)
		val coinDiff = detonate.calcGainCoin(chartSet.isRanked, chart.difficultyValue, record)
		this.coin = (this.coin ?: 0) + coinDiff
		updateUser(this)
		logger.info("User[${this.username}] submited a record of ChartSet[${chartSet.musicTitle}] score ${record.score}(${record.perfect}/${record.good}/${record.miss}). [$randomId]")
		return AfterPlaySubmitModel(
			RankingModel(needUpdate, RankModel(after!!.rank)), this.RThisMonth ?: 0, this.coin, this.diamond
		)
	}
}