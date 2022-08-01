@file:Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package explode.dataprovider.provider.mongo

import TConfig.Configuration
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.*
import explode.dataprovider.detonate.ExplodeConfig.Companion.explode
import explode.dataprovider.model.database.*
import explode.dataprovider.model.extend.BombPlayRecordOfUser
import explode.dataprovider.model.extend.BombPlayRecordResult
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.*
import explode.dataprovider.provider.mongo.MongoExplodeConfig.Companion.toMongo
import explode.dataprovider.provider.mongo.RandomUtil.randomId
import explode.dataprovider.provider.mongo.RandomUtil.randomIdUncrypted
import kotlinx.serialization.*
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MongoProvider(private val config: MongoExplodeConfig, val detonate: Detonate = Detonate(config)) : IBlowAccessor,
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

	// ACTUAL DATA ACCESSING
	private val userC = db.getCollection<MongoUser>("User")
	private val chartC = db.getCollection<MongoChart>("Chart")
	private val chartSetC = db.getCollection<MongoSet>("ChartSet")
	private val playRecordC = db.getCollection<MongoRecord>("PlayRecord")

	// getters
	override fun getUser(userId: String) = userC.findOneById(userId)
	override fun getChart(chartId: String) = chartC.findOneById(chartId)
	override fun getSet(setId: String) = chartSetC.findOneById(setId)
	override fun getRecord(recordId: String) = playRecordC.findOneById(recordId)

	// advanced getters
	override fun getSetByChartId(chartId: String) =
		chartSetC.findOne(MongoSet::charts contains chartId)

	override fun getUserByName(username: String) = userC.findOne(MongoUser::username eq username)
	override fun getUserByToken(token: String) = userC.findOne(MongoUser::token eq token)
	override fun getUserRecord(userId: String, limit: Int, skip: Int, sort: RecordSort): FindIterable<MongoRecord> =
		playRecordC.find(MongoRecord::playerId eq userId).sort(descending(sort.prop)).limit(limit).skip(skip)

	override fun getChartRecord(chartId: String, limit: Int, skip: Int, sort: RecordSort): FindIterable<MongoRecord> =
		playRecordC.find(MongoRecord::chartId eq chartId).sort(descending(sort.prop)).limit(limit).skip(skip)

	override fun getUserChartRecord(
		userId: String,
		chartId: String,
		limit: Int,
		skip: Int,
		sort: RecordSort,
		duplicate: Boolean
	): Iterable<MongoRecord> = if(duplicate) {
		getUserChartRecordDup(userId, chartId, limit, skip, sort)
	} else {
		getUserChartRecordNoDup(userId, chartId, limit, skip, sort)
	}

	private fun getUserChartRecordDup(
		userId: String,
		chartId: String,
		limit: Int,
		skip: Int,
		sort: RecordSort
	): Iterable<MongoRecord> =
		playRecordC.find(and(MongoRecord::playerId eq userId, MongoRecord::chartId eq chartId))
			.sort(descending(sort.prop)).limit(limit).skip(skip)

	private fun getUserChartRecordNoDup(
		userId: String,
		chartId: String,
		limit: Int,
		skip: Int,
		sort: RecordSort
	): Iterable<MongoRecord> =
		playRecordC.aggregate(
			match(MongoRecord::chartId eq chartId, MongoRecord::playerId eq userId),
			sort(descending(MongoRecord::playerId, sort.prop)),
			aggregateGroup,
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			skip(skip),
			limit(limit)
		)

	// setters
	override fun updateUser(mongoUser: MongoUser): MongoUser = mongoUser.upsert(userC)
	override fun updateChart(mongoChart: MongoChart): MongoChart = mongoChart.upsert(chartC)
	override fun updateSet(mongoSet: MongoSet): MongoSet = mongoSet.upsert(chartSetC)


	/**
	 * Used for inserting or updating new data.
	 */
	private inline fun <reified T : Any> T.upsert(coll: MongoCollection<T>): T = apply {
		coll.updateOne(this, UpdateOptions().upsert(true))
	}

	override val serverUser: MongoUser = MongoUser(
		_id = "f6fe9c4d-98e6-450a-937c-d64848eacc40",
		"official",
		"",
		mutableListOf(),
		mutableListOf(),
		Int.MIN_VALUE,
		Int.MIN_VALUE,
		ppTime = OffsetDateTime.MIN,
		token = "f6fe9c4d-98e6-450a-937c-d64848eacc40",
		R = Int.MIN_VALUE,
		permission = UserPermission.Default
	)

	override fun createUser(username: String, password: String): MongoUser {
		logger.info("Creating user with parameters [username=$username, password=$password]")
		return MongoUser(
			_id = UUID.randomUUID().toString(),
			username = username,
			password = password,
			ownedSets = mutableListOf(),
			ownedCharts = mutableListOf(),
			coin = 1000,
			diamond = 0,
			ppTime = OffsetDateTime.now(),
			token = UUID.randomUUID().toString(),
			R = 0,
			permission = UserPermission.Default
		).upsert(userC)
	}

	private fun genNewChartId() = if(config.applyUnencryptedFixes) randomIdUncrypted() else randomId()

	override fun createSet(
		musicName: String,
		composerName: String,
		noterId: String?,
		charts: List<MongoChart>,
		id: String?,
		introduction: String?,
		status: SetStatus,
		price: Int
	): MongoSet = MongoSet(
		_id = id ?: randomId(),
		musicName = musicName,
		composerName = composerName,
		noterId = noterId ?: serverUser._id,
		introduction = introduction,
		price = price,
		status = status,
		charts = charts.map { it._id }.toMutableList()
	).apply(::updateSet)

	override fun createChart(difficultyClass: Int, difficultyValue: Int, id: String?, D: Double?): MongoChart =
		MongoChart(
			_id = id ?: genNewChartId(),
			difficultyClass = difficultyClass,
			difficultyValue = difficultyValue,
			D = D
		).apply(::updateChart)

	private fun getSetStoreList(
		limit: Int,
		skip: Int,
		searchedName: String,
		showHidden: Boolean,
		showOfficial: Boolean,
		showRanked: Boolean,
		showUnranked: Boolean,
		showReview: Boolean
	): List<MongoSet> {
		// logger.info("$limit, $skip, $searchedName, $showHidden, $showOfficial, $showRanked, $showUnranked")
		return if(searchedName.isNotEmpty()) {
			chartSetC.find((MongoSet::musicName).regex(searchedName, "i")).limit(limit).skip(skip).toList()
			// chartSetC.find("""{ "musicName": ${searchedName.toFuzzySearch()} }""").limit(limit).skip(skip).toList()
		} else if(showHidden) {
			chartSetC.find(MongoSet::status eq SetStatus.HIDDEN).limit(limit).skip(skip).toList()
		} else if(showReview) {
			chartSetC.find(MongoSet::status eq SetStatus.NEED_REVIEW).limit(limit).skip(skip).toList()
		} else if(showOfficial) {
			chartSetC.find(MongoSet::status eq SetStatus.OFFICIAL).limit(limit).skip(skip).toList()
		} else if(showRanked) {
			chartSetC.find(or(MongoSet::status eq SetStatus.RANKED, MongoSet::status eq SetStatus.OFFICIAL)).limit(limit).skip(skip).toList()
		} else if(showUnranked) {
			chartSetC.find(MongoSet::status eq SetStatus.UNRANKED).limit(limit).skip(skip).toList()
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
	): List<MongoSet> {
		val filter = buildList {
			if(searchedName != null) add(MongoSet::musicName eq searchedName)
			if(isHidden != null) if(isHidden) add(MongoSet::price eq -1) else add(MongoSet::price ne -1)
			if(isOfficial != null) add(MongoSet::status eq SetStatus.OFFICIAL)
			if(isRanked != null) add(or(MongoSet::status eq SetStatus.RANKED, MongoSet::status eq SetStatus.OFFICIAL))
			if(isNeedReview != null) add(MongoSet::status eq SetStatus.NEED_REVIEW)
		}.let(::and)
		return chartSetC.find(filter).limit(limit).skip(skip).toList()
	}

	override fun getSets(limit: Int?, skip: Int?): Iterable<MongoSet> {
		return chartSetC.find().apply {
			skip?.let { skip(skip) }
			limit?.let { limit(limit) }
		}
	}

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

	/*
	 * Update v1.2.1: since the score is not the key to judge R, so the best R and the best score have change not on the same PlayRecord.
	 *                So we store every play, and get the best R and best score by sorting and limiting the iterable.
	 *                This is compatitable with the old version, but the problem is that the performance may be affected.
	 */
	private fun MongoUser.updatePlayerScoreOnChart(chartId: String, record: PlayRecordInput): MongoRecord {
		// calculate R
		val r = getChart(chartId)?.D?.let {
			detonate.calcR(it, record)
		}

		val new = MongoRecord(
			_id = randomId(),
			playerId = _id,
			chartId = chartId,
			score = record.score!!,
			scoreDetail = ScoreDetail(record.perfect!!, record.good!!, record.miss!!),
			uploadedTime = OffsetDateTime.now(),
			RScore = r
		)
		playRecordC.insertOne(new)
		logger.info("Updated PlayRecord: $new")
		return new
	}

	fun MongoUser.updatePlayerRValue() = apply {
		R = playRecordC.aggregate<MongoRecord>(
			match(MongoRecord::playerId eq _id),
			sort(descending(MongoRecord::RScore)),
			group(MongoRecord::chartId, Accumulators.first("data", ThisDocument)),
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			limit(20)
		).sumByDouble { it.RScore ?: 0.0 }.roundToInt()

		updateUser(this)
	}

	override fun getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel> {
		return emptyList()
	}

	override fun getAssessmentRank(
		assessmentGroupId: String, medalLevel: Int, limit: Int, skip: Int
	): List<AssessmentRecordWithRankModel> {
		return emptyList()
	}

	override val gameSetting = GameSettingModel(81)

	override fun loginUser(username: String, password: String): MongoUser {
		val u = getUserByName(username) ?: error("Invalid username.")
		return if(u.password == password) {
			u
		} else {
			error("Invalid password.")
		}
	}

	override fun registerUser(username: String, password: String): MongoUser {
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
	): List<MongoSet> {
		//return getSetStoreList(limit, skip, searchedName, onlyRanked, onlyOfficial, onlyReview, onlyHidden)
		return getSetStoreList(
			limit, skip,
			searchedName,
			onlyHidden,
			onlyOfficial,
			onlyRanked,
			!onlyRanked,
			onlyReview
		)
	}

	fun getAllSets(): FindIterable<MongoSet> = chartSetC.find()

	override fun MongoUser.buySet(id: String): ExchangeSetModel {
		val s = getSet(id) ?: error("Invalid set: $id")

		// already bought
		if(id in ownedSets) return ExchangeSetModel(this.coin)

		val coinRemain = this.coin - s.price
		if(coinRemain >= 0) {
			coin = coinRemain
			ownedSets += s._id
			ownedCharts += s.charts
			updateUser(this)
			logger.info("User[${this.username}] bought ChartSet[${s.musicName}](${s._id}) cost ${s.price} remaining ${coin}.")
			return ExchangeSetModel(this.coin)
		} else {
			error("You lack money!")
		}
	}

	override fun MongoUser.getAssessmentRankSelf(
		assessmentGroupId: String, medalLevel: Int
	): AssessmentRecordWithRankModel? {
		return null
	}

	@Serializable
	private data class MongoRecordRanked(
		val _id: String,
		val playerId: String,
		val chartId: String,
		val score: Int,
		val scoreDetail: ScoreDetail,
		@Contextual
		val uploadedTime: OffsetDateTime,
		val RScore: Double?,
		val ranking: Int
	)

	private val aggregateRanking =
		Aggregates.setWindowFields(null, MongoRecordRanked::score eq -1, WindowedComputations.rank("ranking"))

	private val aggregateGroup = Aggregates.group(MongoRecord::playerId, Accumulators.first("data", ThisDocument))

	private data class PlayRecordGroupingAggregationMiddleObject(
		val data: MongoRecord
	)

	private fun <TExpression> replaceWith(value: TExpression): Bson = Aggregates.replaceWith(value)

	override fun MongoUser.getPlayRankSelf(chartId: String): PlayRecordWithRank? {
		return playRecordC.aggregate<MongoRecordRanked>(
			match(MongoRecordRanked::chartId eq chartId),
			sort(descending(MongoRecordRanked::playerId, MongoRecordRanked::score)),
			aggregateGroup,
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			aggregateRanking,
			match(MongoRecordRanked::playerId eq _id)
		).map { (_, _, _, score, detail, time, _, ranking) ->
			val (perfect, good, miss) = detail
			PlayRecordWithRank(this.shrink, PlayMod.Default, ranking, score, perfect, good, miss, time)
		}.firstOrNull()
	}

	override fun getPlayRank(chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank> {
		return playRecordC.aggregate<MongoRecordRanked>(
			match(MongoRecordRanked::chartId eq chartId),
			sort(descending(MongoRecordRanked::playerId, MongoRecordRanked::score)),
			aggregateGroup,
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			aggregateRanking,
			skip(skip),
			limit(limit)
		).map { (_, uid, _, score, detail, time, _, ranking) ->
			val (perfect, good, miss) = detail
			val user = getUser(uid) ?: error("Invalid user: $uid")
			PlayRecordWithRank(user.shrink, PlayMod.Default, ranking, score, perfect, good, miss, time)
		}.toList()
	}

	override fun MongoUser.getLastPlayRecords(limit: Int, skip: Int): Iterable<BombPlayRecordOfUser> {
		return playRecordC.aggregate<MongoRecordRanked>(
			aggregateRanking,
			match(MongoRecordRanked::playerId eq _id),
			sort(descending(MongoRecordRanked::uploadedTime)),
			limit(limit),
			skip(skip)
		).map { (_, _, cid, score, detail, time, R, ranking) ->
			val (perfect, good, miss) = detail
			BombPlayRecordOfUser(cid, BombPlayRecordResult(score, perfect, good, miss, ranking), time, R)
		}
	}

	// TODO: Make Compatible with v1.2 Record Change
	override fun MongoUser.getBestPlayRecords(limit: Int, skip: Int): Iterable<BombPlayRecordOfUser> {
		return playRecordC.aggregate<MongoRecordRanked>(
			aggregateRanking,
			match(MongoRecordRanked::playerId eq _id),
			sort(descending(MongoRecordRanked::score)),
			limit(limit),
			skip(skip)
		).map { (_, _, cid, score, detail, time, R, ranking) ->
			val (perfect, good, miss) = detail
			BombPlayRecordOfUser(cid, BombPlayRecordResult(score, perfect, good, miss, ranking), time, R)
		}
	}

	override fun MongoUser.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel {
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel("0"))
	}

	override fun MongoUser.submitBeforePlay(
		chartId: String, ppCost: Int, eventArgs: String
	): BeforePlaySubmitModel {
		// Uncrypted Fix: the Uncrypted charts SHOULD end with 4 zero, which will be lost in the argument sent to here.
		//                like actual Chart id is 6c4mcbcvwiw0ayniuze9azca,
		//                but we can only receive 6c4mcbcvwiw0ayniuze9.
		//                So I assert the last four character of the chart ID in Uncrypted Mode is zero,
		//                like this 6c4mcbcvwiw0ayniuze90000.
		//                And the actual chart ID need fixing ending up as 4 zeros as well.
		val fixedChartId = if(config.applyUnencryptedFixes) chartId + "0000" else chartId

		// check existance of the chart, in order to prevent the self-imported charts data infection.
		checkNotNull(getChart(fixedChartId)) { "Invalid ChartId: $fixedChartId" }

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

	override fun MongoUser.submitAfterAssessment(
		records: List<PlayRecordInput>, randomId: String
	): AfterAssessmentModel {
		return AfterAssessmentModel(1, R, this.coin, this.diamond)
	}

	override fun MongoUser.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel {
		val p = playingDataCache[randomId] ?: error("Invalid randomId($randomId)")

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
		val chartSet = getSetByChartId(chartId) ?: error("Invalid Chart: cannot find the set by chart($chartId)")
		val chart = getChart(chartId) ?: error("Invalid chart: cannot find chart($chartId)")
		val coinDiff = detonate.calcGainCoin(chartSet.status.isRanked, chart.difficultyValue, record)
		this.coin = this.coin + coinDiff
		updateUser(this)
		logger.info("User[${this.username}] submited a record of ChartSet[${chartSet.musicName}] score ${record.score}(${record.perfect}/${record.good}/${record.miss}). [$randomId]")
		return AfterPlaySubmitModel(
			RankingModel(needUpdate, RankModel(after!!.rank)), R, this.coin, this.diamond
		)
	}

	override fun MongoUser.reviewSet(set: MongoSet, accepted: Boolean, rejectMessage: String?) {
		if(set.status == SetStatus.NEED_REVIEW) {
			set.reviews = set.reviews ?: mutableListOf()
			set.reviews!! += ReviewResult(
				this._id,
				accepted,
				rejectMessage
			)
			updateSet(set)
		} else {
			error("Invalid status of set: ${set._id}, NEED_REVIEW required.")
		}
	}

	// UTILITIES
	override val MongoUser.tunerize: UserModel
		get() = UserModel(
			_id = _id,
			username = username,
			ownChart = ownedCharts,
			coworkChart = mutableListOf(),
			ownSet = ownedSets,
			follower = 0,
			coin = coin,
			diamond = diamond,
			PPTime = ppTime,
			token = token,
			RThisMonth = R,
			highestGoldenMedal = null,
			access = AccessData(permission.review)
		)

	override val MongoChart.tunerize: DetailedChartModel
		get() {
			val s = getSetByChartId(_id) ?: error("Invalid chart($_id): not included in a set.")
			return DetailedChartModel(
				_id = _id,
				charter = (getUser(s.noterId) ?: serverUser).tunerize,
				chartName = "${s.musicName}_${difficultyClass}",
				gcPrice = 0,
				music = MusicModel(s.composerName),
				difficultyBase = difficultyClass,
				difficultyValue = difficultyValue,
				D = D
			)
		}

	override val MongoSet.tunerize: SetModel
		get() = SetModel(
			_id = _id,
			introduction = introduction ?: "",
			coinPrice = price,
			noter = NoterModel(getUser(noterId)?.username ?: "unknown"),
			musicTitle = musicName,
			composerName = composerName,
			playCount = 0,
			chart = charts.mapNotNull { getChart(it)?.tunerize?.minify },
			isGot = false,
			isRanked = status == SetStatus.RANKED || status == SetStatus.OFFICIAL,
			isOfficial = status == SetStatus.OFFICIAL,
			OverridePriceStr = "",
			needReview = status == SetStatus.NEED_REVIEW
		)

	val UserModel.asPlayer: PlayerModel
		get() = PlayerModel(_id, username, highestGoldenMedal ?: 0, RThisMonth ?: 0)

	private fun DetailedChartModel.minimal(): ChartModel {
		return ChartModel(_id, difficultyBase, difficultyValue)
	}

	val MongoUser.shrink: PlayerModel
		get() = PlayerModel(_id, username, 0, R)
}
