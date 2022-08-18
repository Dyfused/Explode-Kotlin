@file:Suppress("MemberVisibilityCanBePrivate")

package explode.dataprovider.provider.mongo

import TConfig.Configuration
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.*
import explode.dataprovider.detonate.ExplodeConfig.Companion.explode
import explode.dataprovider.model.database.*
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.*
import explode.dataprovider.provider.DifficultyUtils.toDifficultyClassStr
import explode.dataprovider.provider.mongo.MongoExplodeConfig.Companion.toMongo
import kotlinx.serialization.*
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MongoProvider(private val config: MongoExplodeConfig, val detonate: Detonate = Detonate(config)) : IBlowOmni,
	IBlowAccessor,
	IBlowResourceProvider by detonate.resourceProvider {

	constructor() : this(Configuration(File("./provider.cfg")).explode().toMongo())

	private val logger = LoggerFactory.getLogger("Mongo")
	private val mongo = (KMongo.createClient(config.connectionString))
	private val db = mongo.getDatabase(config.databaseName)

	init {
		logger.info("Database: ${config.databaseName} at ${config.connectionString}")
		logger.info("Resource: ${config.resourceDirectory}")
		logger.info("Unencrypted: ${config.applyUnencryptedFixes}")
		logger.info("ErrorHandlingStrategy: ${config.errorHandlingStrategy}")
	}

	// get the error handling strategy, Coward by default
	private val errorHandlingStrategy =
		runCatching { ErrorHandlingStrategy.valueOf(config.errorHandlingStrategy) }.getOrDefault(ErrorHandlingStrategy.Coward)

	companion object {
		const val InvalidSubmissionRandomId = "Invalid"
	}

	// ACTUAL DATA ACCESSING
	private val userC = db.getCollection<MongoUser>("User")
	private val chartC = db.getCollection<MongoChart>("Chart")
	private val chartSetC = db.getCollection<MongoSet>("ChartSet")
	private val playRecordC = db.getCollection<MongoRecord>("PlayRecord")
	private val reviewC = db.getCollection<MongoReview>("Review")

	// getters
	override fun getUser(userId: String) = userC.findOneById(userId)
	override fun getChart(chartId: String) = chartC.findOneById(chartId)
	override fun getSet(setId: String) = chartSetC.findOneById(setId)
	override fun getRecord(recordId: String) = playRecordC.findOneById(recordId)

	// advanced getters
	override fun getSetByChartId(chartId: String) =
		chartSetC.findOne(MongoSet::charts contains chartId)

	fun getSetByName(name: String): FindIterable<MongoSet> =
		chartSetC.find(MongoSet::musicName eq name)

	fun getSetByNameList(name: String): List<MongoSet> = getSetByName(name).toList()

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

	// non-null getters
	override fun MongoChart.getParentSet(): MongoSet = getSetByChartId(_id) ?: run {
		fixHeadlessChart(this)
		fail("Unable to get the Set of a Headless Chart.")
	}

	/**
	 * Used for inserting or updating new data.
	 */
	private inline fun <reified T : Any> T.upsert(coll: MongoCollection<T>): T = apply {
		coll.updateOne(this, UpdateOptions().upsert(true))
	}

	/**
	 * The default user of the server.
	 */
	override val serverUser: MongoUser = MongoUser(
		_id = "f6fe9c4d-98e6-450a-937c-d64848eacc40",
		"official",
		"",
		mutableListOf(),
		mutableListOf(),
		Int.MIN_VALUE,
		Int.MIN_VALUE,
		ppTime = OffsetDateTime.now(),
		token = "f6fe9c4d-98e6-450a-937c-d64848eacc40",
		R = Int.MIN_VALUE,
		permission = UserPermission.Administrator
	)

	init {
		// ensure that the server user is in the database
		serverUser.upsert(userC)
	}

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
		var filters = arrayOf<Bson>()
		if(searchedName.isNotEmpty()) {
			filters += (MongoSet::musicName).regex(searchedName, "i")
		}
		if(showHidden) {
			filters += MongoSet::status eq SetStatus.HIDDEN
		} else if(showReview) {
			filters += MongoSet::status eq SetStatus.NEED_REVIEW
		} else if(showOfficial) {
			filters += MongoSet::status eq SetStatus.OFFICIAL
		} else if(showRanked) {
			filters += or(MongoSet::status eq SetStatus.RANKED, MongoSet::status eq SetStatus.OFFICIAL)
		} else if(showUnranked) {
			filters += MongoSet::status eq SetStatus.UNRANKED
		}
		chartSetC.find(MongoSet::status eq SetStatus.UNRANKED, MongoSet::status eq SetStatus.HIDDEN)

		return chartSetC.find(*filters).limit(limit).skip(skip).toList()
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

	@Serializable
	data class PlayingAssessmentData(
		@SerialName("_id") val randomId: String,
		val assessmentId: String,
		val medalLevel: Int,
		@Contextual val createTime: OffsetDateTime = OffsetDateTime.now()
	)

	private val playingDataCache = mutableMapOf<String, PlayingData>()
	private val playingAssessmentDataCache = mutableMapOf<String, PlayingAssessmentData>()

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

	fun getUserBestR20(userId: String) =
		playRecordC.aggregate<MongoRecord>(
			match(MongoRecord::playerId eq userId),
			sort(descending(MongoRecord::RScore)),
			group(MongoRecord::chartId, Accumulators.first("data", ThisDocument)),
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			sort(descending(MongoRecord::RScore)),
			limit(20)
		)

	fun MongoUser.updatePlayerRValue() = apply {
		R = getUserBestR20(_id).sumByDouble { it.RScore ?: 0.0 }.roundToInt()

		updateUser(this)
	}

	private val assessmentGroupC = db.getCollection<MongoAssessmentGroup>("AssessmentGroup")
	private val assessmentC = db.getCollection<MongoAssessment>("Assessment")
	private val assessmentRecordC = db.getCollection<MongoAssessmentRecord>("AssessmentRecord")

	override fun MongoUser.getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel> {
		return assessmentGroupC.find().limit(limit).skip(skip).map { group ->
			AssessmentGroupModel(
				group.id,
				group.name,
				group.assessments.mapNotNull { (medalLevel, _) -> getTunerizedAssessment(group.id, medalLevel, _id) }
			)
		}.toList()
	}

	private fun MongoAssessmentGroup.getAssessments() =
		assessments.mapNotNull { assessmentC.findOneById(it.value) }

	private fun getTunerizedAssessment(assessmentGroupId: String, medalLevel: Int, userId: String): AssessmentModel {
		val usr = getUser(userId) ?: fail("Invalid user: $userId")
		val ass = getAssessmentByGroupAndMedal(assessmentGroupId, medalLevel)
			?: fail("Invalid assessment: Medal $medalLevel of Group $assessmentGroupId")

		return AssessmentModel(
			ass.id,
			medalLevel,
			ass.lifeBarLength,
			ass.normalPassAcc,
			ass.goldenPassAcc,
			ass.exMiss,
			ass.getAssessmentCharts(),
			listOf(
				usr.getSelfBestAssessmentRecord(ass.id)?.let { assRec ->
					val records = assRec.records.map {
						val (perfect, good, miss) = it.scoreDetail
						AssessmentPlayRecordModel(perfect, good, miss, it.score)
					}
					AssessmentRecordsModel(assRec.accuracy, true, records)
				} ?: AssessmentRecordsModel(0.0, true, listOf())
			)
		)
	}

	private fun MongoAssessment.getAssessmentCharts() =
		charts.mapNotNull { getChart(it) }.associateWith { it.getParentSet() }.map { (chart, set) ->
			AssessmentChartModel(chart._id, set.tunerize)
		}

	fun getAssessmentByGroupAndMedal(assessmentGroupId: String, medalLevel: Int): MongoAssessment? {
		return assessmentC.findOneById(
			assessmentGroupC.findOneById(assessmentGroupId)?.assessments?.get(medalLevel) ?: return null
		)
	}

	override fun getAssessmentRank(
		assessmentGroupId: String, medalLevel: Int, limit: Int, skip: Int
	): List<AssessmentRecordWithRankModel> {
		val ass = getAssessmentByGroupAndMedal(assessmentGroupId, medalLevel) ?: return emptyList()

		return assessmentRecordC.aggregate<MongoAssessmentRecordRanked>(
			match(MongoAssessmentRecordRanked::assessmentId eq ass.id),
			sort(descending(MongoAssessmentRecordRanked::playerId, MongoAssessmentRecordRanked::totalScore)),
			aggregateGroup,
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			aggregateRanking,
			skip(skip),
			limit(limit)
		).map {
			val player = getUser(it.playerId) ?: fail("Invalid user: ${it.playerId}")
			AssessmentRecordWithRankModel(player.shrink, it.ranking, it.accuracy, it.result, it.time)
		}.toList()
	}

	private fun MongoUser.getSelfAssessmentRecord(assessmentId: String): FindIterable<MongoAssessmentRecord> {
		return assessmentRecordC.find(
			and(
				MongoAssessmentRecord::assessmentId eq assessmentId,
				MongoAssessmentRecord::playerId eq _id
			)
		)
	}

	private fun MongoUser.getSelfBestAssessmentRecord(assessmentId: String): MongoAssessmentRecord? {
		return getSelfAssessmentRecord(assessmentId).maxByOrNull {
			it.records.sumOf(MongoAssessmentRecordEntry::score) + (it.exRecord?.score ?: 0)
		}
	}

	override val gameSetting = GameSettingModel(config.latestClientVersion)

	override fun loginUser(username: String, password: String): MongoUser {
		val u = getUserByName(username)
		// register on invalid username
			?: if(config.invalidUsernameAsRegister) {
				return registerUser(username, password)
			} else {
				fail("Invalid Username.")
			}
		return if(u.password == password) {
			u
		} else {
			fail("Invalid Password.")
		}
	}

	override fun registerUser(username: String, password: String): MongoUser {
		if(getUserByName(username) != null) fail("Username exists.")
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
		val s = getSet(id) ?: fail("Invalid set: $id")

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
			fail("You lack of money!")
		}
	}

	override fun MongoUser.getAssessmentRankSelf(
		assessmentGroupId: String, medalLevel: Int
	): AssessmentRecordWithRankModel? {
		val ass = getAssessmentByGroupAndMedal(assessmentGroupId, medalLevel)
			?: fail("Invalid assessment: Medal $medalLevel of Group $assessmentGroupId")
		return assessmentRecordC.aggregate<MongoAssessmentRecordRanked>(
			match(MongoAssessmentRecordRanked::assessmentId eq ass.id),
			sort(descending(MongoAssessmentRecordRanked::playerId, MongoAssessmentRecordRanked::totalScore)),
			aggregateGroup,
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			aggregateRanking,
			match(MongoAssessmentRecordRanked::playerId eq _id)
		).map { (_, _, result, _, _, _, accuracy, time, _, ranking) ->
			AssessmentRecordWithRankModel(this.shrink, ranking, accuracy, result, time)
		}.firstOrNull()
	}

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
			val user = getUser(uid) ?: fail("Invalid user: $uid")
			PlayRecordWithRank(user.shrink, PlayMod.Default, ranking, score, perfect, good, miss, time)
		}.toList()
	}

	override fun MongoUser.getLastPlayRecords(limit: Int, skip: Int): Iterable<MongoRecordRanked> {
		return playRecordC.aggregate(
			aggregateRanking,
			match(MongoRecordRanked::playerId eq _id),
			sort(descending(MongoRecordRanked::uploadedTime)),
			limit(limit),
			skip(skip)
		)
	}

	// TODO: Make Compatible with v1.2 Record Change
	override fun MongoUser.getBestPlayRecords(limit: Int, skip: Int): Iterable<MongoRecordRanked> {
		return playRecordC.aggregate(
			aggregateRanking,
			match(MongoRecordRanked::playerId eq _id),
			sort(descending(MongoRecordRanked::score)),
			limit(limit),
			skip(skip)
		)
	}

	override fun MongoUser.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel {
		val data = PlayingAssessmentData(randomId(), assessmentId, medal, OffsetDateTime.now())
		playingAssessmentDataCache[data.randomId] = data
		return BeforePlaySubmitModel(data.createTime, PlayingRecordModel(data.randomId))
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
		val chart = getChart(fixedChartId)
		// allow invalid submission mode
			?: if(config.allowInvalidPlaySubmission) {
				return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel(InvalidSubmissionRandomId))
			} else {
				fail("Invalid ChartId: $fixedChartId")
			}

		if(!isOwned(chart)) {
			fail("Not Owned Chart: $chartId")
		}

		val p = PlayingData(randomId(), fixedChartId, ppCost)
		playingDataCache[p.randomId] = p
		val expireTime = p.createTime + Duration.ofHours(1)
		logger.info(
			"User[${this.username}] submited a play request of ChartSet[id=$chartId], expires at ${expireTime}. [${p.randomId}]"
		)
		return BeforePlaySubmitModel(OffsetDateTime.now(), PlayingRecordModel(p.randomId))
	}

	private val decimalFormat = DecimalFormat("#.##").also { it.roundingMode = RoundingMode.HALF_DOWN }

	override fun MongoUser.submitAfterAssessment(
		records: List<PlayRecordInput>, randomId: String
	): AfterAssessmentModel {
		val data = playingAssessmentDataCache[randomId] ?: fail("Invalid RandomId: $randomId")

		playingAssessmentDataCache -= randomId

		val medal = data.medalLevel
		val ass = getAssessmentByGroupAndMedal(data.assessmentId, medal)
			?: fail("Invalid assessment: Medal $medal of Group ${data.assessmentId}")

		data class SafePlayRecordInput(
			val score: Int,
			val perf: Int,
			val good: Int,
			val miss: Int
		)

		fun calcAccuracy(perf: Int, good: Int, total: Int): Double {
			val acc =  ((perf * 100000L + good * 50000L) / total) / 1000.0
			return decimalFormat.format(acc).toDouble()
		}

		val recs = records.map {

			val perf = it.perfect!!
			val good = it.good!!
			val miss = it.miss!!

			MongoAssessmentRecordEntry(
				it.score!!,
				ScoreDetail(it.perfect, it.good, it.miss),
				calcAccuracy(perf, good, perf+good+miss)
			)
		}.toList()

		val recsWithoutEx = recs.toMutableList()

		// only if the uploaded chart count equals to the expected chart count
		val exRec = if(records.size == ass.charts.size) {
			recsWithoutEx.removeLast()
		} else {
			null
		}

		val avgAcc = recsWithoutEx.map(MongoAssessmentRecordEntry::accuracy).average() + (exRec?.accuracy ?: 0.0)
		val totalScore = recsWithoutEx.sumOf { it.score } + (exRec?.score ?: 0)

		val result = when {
			avgAcc >= ass.goldenPassAcc -> 2
			avgAcc >= ass.normalPassAcc -> 1
			else -> 0
		}

		val r = MongoAssessmentRecord(
			_id,
			ass.id,
			result,
			recs,
			exRec,
			totalScore,
			avgAcc,
			data.createTime
		)

		println(r)

		r.upsert(assessmentRecordC)
		if(result == 2 && (this.highestGoldenMedal ?: 0) < medal) {
			highestGoldenMedal = medal
			this.upsert(userC)
		}

		return AfterAssessmentModel(result, R, this.coin, this.diamond)
	}

	override fun MongoUser.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel {
		// allow invalid submission mode
		if(randomId == InvalidSubmissionRandomId) {
			// respond with fake data
			return AfterPlaySubmitModel(RankingModel(false, RankModel(1)), R, coin, diamond)
		}

		val p = playingDataCache[randomId] ?: fail("Invalid RandomId: $randomId")

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
		val chart = getChart(chartId) ?: fail("Invalid chart: cannot find chart($chartId)")
		val chartSet = chart.getParentSet()
		val coinDiff = detonate.calcGainCoin(chartSet.status.isRanked, chart.difficultyValue, record)
		coin += coinDiff
		updateUser(this)
		logger.info("User[${this.username}] submited a record of ChartSet[${chartSet.musicName}] score ${record.score}(${record.perfect}/${record.good}/${record.miss}). [$randomId]")
		return AfterPlaySubmitModel(
			RankingModel(needUpdate, RankModel(after!!.rank)), R, coin, diamond
		)
	}

	override fun MongoSet.addReviewResult(review: MongoReviewResult) {
		// check before insertion

		// user must be existent and with reviewer permission
		val u = getUser(review.reviewerId) ?: fail("Invalid Reviewer ID: ${review.reviewerId}")
		if(!u.permission.review) fail("Not Reviewer: ${u.username}")

		// set must be existent and with NEED_REVIEW status
		if(status != SetStatus.NEED_REVIEW) fail("Invalid Reviewed Set: No Need Reviewing")

		// evaluation message must not be empty on Reject
		if(!review.status && review.evaluation.isEmpty()) fail("Must provide evaluation on Reject")

		// no duplicated review
		if(reviewC.findOne(MongoReview::reviews elemMatch (MongoReviewResult::reviewerId eq review.reviewerId)) != null)
			fail("Duplicated Review")

		val r = getReview() ?: fail("Set Review Data Lost: $_id")
		r.reviews += review
		r.upsert(reviewC)

		// reviewC.updateOneById(r.id, push(MongoReview::reviews, review))

		checkAutoEndReview(r) // auto check
	}

	override fun MongoSet.getReview(): MongoReview? {
		return reviewC.findOne(MongoReview::reviewedSet eq _id)
	}

	override fun MongoSet.startReview(expectStatus: SetStatus) {
		// update status to Need Review
		status = SetStatus.NEED_REVIEW
		updateSet(this)

		// create and insert the review data
		val r = MongoReview(_id, expectStatus)
		r.upsert(reviewC)

		logger.info("Started a Review on Set($_id).")
	}

	override fun MongoSet.endReview(pass: Boolean) {
		// get the review data
		val r = getReview() ?: fail("Set Review Data Lost: $_id")

		// update status
		status = if(pass) {
			r.expectStatus
		} else {
			SetStatus.HIDDEN
		}
		updateSet(this)

		// remove review data
		reviewC.deleteOneById(r.id)

		logger.info("Ended a Review on Set($musicName, #$_id), new status is $status.")
	}

	private fun MongoSet.checkAutoEndReview(review: MongoReview) {
		if(!config.autoEndReview) return

		// pass the reviewer count
		if(review.reviews.size >= config.autoEndReviewCountReviewer) {
			val rejectReviews = review.reviews.filter { !it.status }
			val acceptPercentage = (review.reviews.size - rejectReviews.size).toDouble() / review.reviews.size
			if(acceptPercentage >= config.autoEndReviewAcceptPercentage) { // pass
				endReview(true)

				logger.info("Auto ACCEPTED a review on Set<$musicName>($_id).")
			} else {
				endReview(false)

				// dump reject messages
				val rejectMessages = rejectReviews.joinToString(separator = "\n") { it.evaluation }
				introduction += "\n[Rejected Charts]\n$rejectMessages"
				updateSet(this)

				logger.info("Auto REJECTED a review on Set<$musicName>($_id).")
			}
		}
	}

	override fun getReviewList(): FindIterable<MongoReview> = reviewC.find()

	enum class ErrorHandlingStrategy {
		/**
		 * Cowardly report to the console, and wait for administrators to fix the problem.
		 * Before the fix take place, the error message can be dumped many times.
		 */
		Coward,

		/**
		 * Directly fix the problem and report to the console.
		 * This is destructive option.
		 */
		Destructive
	}

	// ERROR DUMPERS
	private fun MongoChart.dump() =
		"""
			[ChartDetails]
				ID:               $_id
				D Value:          $D
				DifficultyClass:  ${difficultyClass.toDifficultyClassStr()}(${difficultyClass})
				DifficultyNumber: $difficultyValue
		""".trimIndent()

	private fun MongoSet.dump() =
		"""
			[SetDetails]
				ID: $_id
				MusicName: $musicName
				MusicAuthor(ComposerName): $composerName
				NoterId: $noterId
				&Noter:
					${getUser(noterId)?.dump() ?: "~ERROR: Cannot find the author~"}
				Introduction: $introduction
				PriceCoin: $price
				Status: ${status.humanizedName}
				Charts: $charts
				&Charts:
					${charts.mapNotNull(::getChart).joinToString(separator = "\n") { it.dump() }}
		""".trimIndent()

	private fun MongoUser.dump() =
		"""
			[UserDetails]
				ID:   $_id
				Name: $username
				OwnedSets:   $ownedSets
				OwnedCharts: $ownedCharts
				Coin:    $coin
				Diamond: $diamond
				PPTime:  $ppTime
				Token:   $token
				R: $R
				Permission:
					Review: ${permission.review}
		""".trimIndent()

	// DATA FIXINGS

	/**
	 * Invoked when a [MongoChart] is not included in any [MongoSet].
	 * Destructive Solution is to directly delete the chart.
	 *
	 * @see getParentSet the caller
	 */
	private fun fixHeadlessChart(chart: MongoChart) {
		when(errorHandlingStrategy) {
			ErrorHandlingStrategy.Coward -> {
				logger.error("[Headless Chart Alert] Chart(${chart._id}) has no existent parent set. This log only warns the administrator but do nothing.")
			}

			ErrorHandlingStrategy.Destructive -> {
				dz.deleteChartById(chart._id)
				println(chart.dump())
				logger.error("[Headless Chart Alert] Chart(${chart._id}) has no existent parent set. The broken chart has been deleted, and details are dumped above.")
			}
		}
	}

	// UTILITIES

	fun MongoUser.isOwned(chart: MongoChart): Boolean {
		val set = chart.getParentSet()
		return set._id in ownedSets && chart._id in ownedCharts
	}

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
			highestGoldenMedal = highestGoldenMedal,
			access = AccessData(permission.review)
		)

	override val MongoChart.tunerize: DetailedChartModel
		get() {
			val s = getParentSet()
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
		get() = PlayerModel(_id, username, highestGoldenMedal ?: 0, R)

	fun MongoSet.getCharts(): List<MongoChart> {
		return this.charts.mapNotNull {
			getChart(it).apply {
				if(this == null) {
					logger.warn("Cannot find Chart($it), which is bound to Set(${this@getCharts._id}), removing from the set.")
				}
			}
		}
	}

	val dz by lazy { DangerZone() }

	inner class DangerZone internal constructor() {

		/**
		 * To delete an existent set and its charts, the related records and own status.
		 */
		fun deleteSetById(id: String) {
			val set = getSet(id) ?: return logger.warn("Unable to delete a non-existent set.")
			// delete charts
			set.charts.forEach(::deleteChartById)
			// remove the set from user
			userC.updateMany(Document(), pull(MongoUser::ownedSets, set._id))
			// remove the set
			chartSetC.deleteOneById(set._id)
			logger.info("Deleted Set(${set._id}): $set")
		}

		/**
		 * To delete an existent chart and the related records.
		 */
		fun deleteChartById(id: String) {
			val chart = getChart(id) ?: return logger.warn("Unable to delete a non-existent chart.")
			// remove playing records
			playRecordC.deleteMany(MongoRecord::chartId eq chart._id)
			// remove the chart from user
			userC.updateMany(Document(), pull(MongoUser::ownedCharts, chart._id))
			// remove the chart
			chartC.deleteOneById(chart._id)
			logger.info("Deleted Chart(${id}): $chart")
		}

		/**
		 * To delete an existent user and re-bind its sets to [default user][serverUser].
		 */
		fun deleteUserById(id: String) {
			val user = getUser(id) ?: return logger.warn("Unable to delete a non-existent user.")
			// replace the noterId to the default user.
			chartSetC.updateMany(MongoSet::noterId eq user._id, MongoSet::noterId setTo serverUser._id)
			// remove user
			userC.deleteOneById(user._id)
			logger.info("Deleted User(${user._id}): $user")
		}

	}
}
