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
import explode.dataprovider.util.explodeLogger
import kotlinx.serialization.*
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.*
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

	private val logger = explodeLogger
	private val mongo = (KMongo.createClient(config.connectionString))
	private val db = mongo.getDatabase(config.databaseName)

	override val unencrypted: Boolean
		get() = config.applyUnencryptedFixes

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

		// Pair<is initialized, MongoProvider instance>
		private var providerSingleton: Pair<Boolean, MongoProvider?> = false to null

		private fun MongoProvider.initSingleton() {
			providerSingleton = if(!providerSingleton.first) { // put value if not initialized yet
				true to this
			} else {
				logger.error("Unable to initialize Singleton Mode with multiple instance, disabled.")
				logger.error("Notice that some features depends on Singleton Mode is disabled as well.")
				true to null
			}
		}

		fun letSingleton(block: (MongoProvider) -> Unit) = providerSingleton.second?.let(block)
	}

	init {
		initSingleton()
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

	override fun getSetsByName(name: String): FindIterable<MongoSet> =
		chartSetC.find(MongoSet::musicName eq name)

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
	override fun MongoChart.getParentSet(): MongoSet = getSetByChartId(id) ?: run {
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
		id = "f6fe9c4d-98e6-450a-937c-d64848eacc40",
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
			id = UUID.randomUUID().toString(),
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
		defaultId: String?,
		introduction: String?,
		status: SetStatus,
		price: Int,
		displayNoterName: String?,

		musicContent: ByteArray?,
		previewMusicContent: ByteArray?,
		setCoverContent: ByteArray?,
		storePreviewContent: ByteArray?,
	): MongoSet = MongoSet(
		id = defaultId ?: randomId(),
		musicName,
		composerName,
		noterId = noterId ?: serverUser.id,
		introduction,
		price,
		status,
		charts = charts.map { it.id }.toMutableList(),
		noterDisplayOverride = displayNoterName,
		uploadedTime = OffsetDateTime.now()
	).apply(::updateSet).apply {
		musicContent?.let { addMusicResource(id, it) }
		previewMusicContent?.let { addPreviewResource(id, it) }
		setCoverContent?.let { addSetCoverResource(id, it) }
		storePreviewContent?.let { addStorePreviewResource(id, it) }
	}

	override fun createChart(
		difficultyClass: Int,
		difficultyValue: Int,
		defaultId: String?,
		D: Double?,
		content: ByteArray?
	): MongoChart =
		MongoChart(
			id = defaultId ?: genNewChartId(),
			difficultyClass = difficultyClass,
			difficultyValue = difficultyValue,
			D = D
		).apply(::updateChart).apply {
			content?.let { addChartResource(id, it) }
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
			id = randomId(),
			playerId = id,
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

	override fun MongoUser.getBestPlayRecordsR(limit: Int, skip: Int): Iterable<MongoRecord> {
		return playRecordC.aggregate(
			match(MongoRecord::playerId eq id),
			sort(descending(MongoRecord::RScore)),
			group(MongoRecord::chartId, Accumulators.first("data", ThisDocument)),
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			sort(descending(MongoRecord::RScore)),
			limit(limit),
			skip(skip)
		)
	}

	fun MongoUser.updatePlayerRValue() = apply {
		R = getBestPlayRecordsR(20, 0).sumOf { it.RScore ?: 0.0 }.roundToInt()

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
				group.assessments.mapNotNull { (medalLevel, _) -> getTunerizedAssessment(group.id, medalLevel, id) }
			)
		}.toList()
	}

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
			AssessmentChartModel(chart.id, set.tunerize)
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
			sort(descending(MongoAssessmentRecordRanked::accuracy, MongoAssessmentRecordRanked::playerId)),
			aggregateGroup,
			replaceWith(PlayRecordGroupingAggregationMiddleObject::data),
			aggregateAssessmentRanking,
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
				MongoAssessmentRecord::playerId eq id
			)
		)
	}

	private fun MongoUser.getSelfBestAssessmentRecord(assessmentId: String): MongoAssessmentRecord? {
		return getSelfAssessmentRecord(assessmentId).sort(descending(MongoAssessmentRecord::accuracy)).firstOrNull()
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
		filterName: String,
		filterCategory: StoreCategory,
		filterSort: StoreSort
	): List<MongoSet> {
		val filters = buildList {

			if(filterName.isNotEmpty()) {
				if(filterName.startsWith('#')) {
					val diffLevel = filterName.substring(1).toIntOrNull() ?: 99
					this += MongoSet::charts elemMatch (MongoChart::difficultyValue eq diffLevel)
				} else if(filterName.startsWith('@')) {
					val noterNameOrId = filterName.substring(1)
					this += or(MongoSet::noterDisplayOverride eq noterNameOrId, MongoSet::noterId eq noterNameOrId)
				} else if(filterName.startsWith('&')) {
					val composerName = filterName.substring(1)
					this += MongoSet::composerName eq composerName
				} else {
					this += (MongoSet::musicName.regex(filterName, "i"))
				}
			}

			@Suppress("DEPRECATION")
			when(filterCategory) {
				StoreCategory.OFFICIAL -> this += (MongoSet::status eq SetStatus.OFFICIAL)
				StoreCategory.RANKED -> this += (or(
					MongoSet::status eq SetStatus.RANKED,
					MongoSet::status eq SetStatus.OFFICIAL
				))

				StoreCategory.UNRANKED -> this += (MongoSet::status eq SetStatus.UNRANKED)
				// usage of deprecated enum values below is for compatiblity with older model
				// will be removed in 1.5.x maybe
				StoreCategory.NEED_REVIEW -> this += or(
					MongoSet::isReviewing eq true,
					MongoSet::status eq SetStatus.NEED_REVIEW
				)

				StoreCategory.HIDDEN -> this += or(MongoSet::isHidden eq true, MongoSet::status eq SetStatus.HIDDEN)
				else -> {}
			}
		}

		return chartSetC
			.find(*filters.toTypedArray())
			.run { // sort
				when(filterSort) {
					StoreSort.PUBLISH_TIME -> sort(descending(MongoSet::uploadedTime, MongoSet::id))
					StoreSort.PLAY_COUNT -> this // won't support for now
				}
			}
			.limit(limit)
			.skip(skip).toList()
	}

	fun getAllSets(): FindIterable<MongoSet> = chartSetC.find()

	override fun MongoUser.buySet(id: String): ExchangeSetModel {
		val s = getSet(id) ?: fail("Invalid set: $id")

		// already bought
		if(id in ownedSets) return ExchangeSetModel(this.coin)

		val coinRemain = this.coin - s.price
		if(coinRemain >= 0) {
			coin = coinRemain
			ownedSets += s.id
			ownedCharts += s.charts
			updateUser(this)
			logger.info("User[${this.username}] bought ChartSet[${s.musicName}](${s.id}) cost ${s.price} remaining ${coin}.")
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
			match(MongoAssessmentRecordRanked::playerId eq id)
		).map { (_, _, result, _, _, _, accuracy, time, _, ranking) ->
			AssessmentRecordWithRankModel(this.shrink, ranking, accuracy, result, time)
		}.firstOrNull()
	}

	private val aggregateRanking =
		Aggregates.setWindowFields(null, MongoRecordRanked::score eq -1, WindowedComputations.rank("ranking"))

	private val aggregateAssessmentRanking =
		Aggregates.setWindowFields(
			null,
			descending(MongoAssessmentRecordRanked::accuracy),
			WindowedComputations.rank("ranking")
		)

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
			match(MongoRecordRanked::playerId eq id)
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
			match(MongoRecordRanked::playerId eq id),
			sort(descending(MongoRecordRanked::uploadedTime)),
			limit(limit),
			skip(skip)
		)
	}

	// TODO: Make Compatible with v1.2 Record Change
	override fun MongoUser.getBestPlayRecords(limit: Int, skip: Int): Iterable<MongoRecordRanked> {
		return playRecordC.aggregate(
			aggregateRanking,
			match(MongoRecordRanked::playerId eq id),
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

		fun calcAccuracy(perf: Int, good: Int, total: Int): Double {
			val acc = ((perf * 100000L + good * 50000L) / total) / 1000.0
			return decimalFormat.format(acc).toDouble()
		}

		val recs = records.map {

			val perf = it.perfect!!
			val good = it.good!!
			val miss = it.miss!!

			MongoAssessmentRecordEntry(
				it.score!!,
				ScoreDetail(it.perfect, it.good, it.miss),
				calcAccuracy(perf, good, perf + good + miss)
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
			id,
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

	override fun MongoReview.addReview(userId: String, status: Boolean, evaluation: String) {
		reviews += MongoReviewResult(userId, status, evaluation)
		upsert(reviewC)
	}

	override fun MongoSet.getReview(): MongoReview? {
		return reviewC.findOne(MongoReview::reviewedSet eq id)
	}

	override fun MongoSet.startReview() {
		isReviewing = true
		updateSet(this)

		val r = MongoReview(id)
		r.upsert(reviewC)

		logger.info("Started a Review on Set($id)")
	}

	@Deprecated("ExpectStatus is no longer required. Status will not be NEED_REVIEW any more.")
	override fun MongoSet.startReview(expectStatus: SetStatus) {
		// set status to the expectedStatus like newer model
		// and set reviewing mode on
		status = expectStatus
		startReview()
	}

	override fun MongoSet.endReview(pass: Boolean) {
		// get the review data
		val r = getReview() ?: fail("Set Review Data Lost: $id")

		// update status
		// if the status is not NEED_REVIEW, then it is the exactly what we wanted,
		// or try to put it with ExpectStatus like older model.
		// if both of them is missing, just throw the exception.
		@Suppress("DEPRECATION")
		if(status == SetStatus.NEED_REVIEW) {
			val expected = r.expectStatus
			if(expected == null) {
				error("You need either define status in Set or expectedStatus in ReviewC")
			} else {
				status = expected
			}
		}
		// hide if not pass
		isHidden = pass
		updateSet(this)

		// remove review data
		reviewC.deleteOneById(r.id)

		logger.info("Ended a Review on Set($musicName, #$id), new status is $status.")
	}

	fun MongoReview.peekAutoReviewResult(): Boolean {
		return if(reviews.size >= config.autoEndReviewCountReviewer) {
			val rejectReviews = reviews.filter { !it.status }
			val acceptPercentage = (reviews.size - rejectReviews.size).toDouble() / reviews.size
			acceptPercentage >= config.autoEndReviewAcceptPercentage
		} else {
			false
		}
	}

	override fun getReviewList(): FindIterable<MongoReview> = reviewC.find()

	override fun MongoUser.payCoin(coin: Int): Boolean {
		return if(canAffordCoin(coin)) {
			this.coin -= coin
			updateUser(this)
			true
		} else {
			false
		}
	}

	override fun MongoUser.payDiamond(diamond: Int): Boolean {
		return if(canAffordDiamond(diamond)) {
			this.diamond -= diamond
			updateUser(this)
			true
		} else {
			false
		}
	}

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
				ID:               $id
				D Value:          $D
				DifficultyClass:  ${difficultyClass.toDifficultyClassStr()}(${difficultyClass})
				DifficultyNumber: $difficultyValue
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
				logger.error("[Headless Chart Alert] Chart(${chart.id}) has no existent parent set. This log only warns the administrator but do nothing.")
			}

			ErrorHandlingStrategy.Destructive -> {
				dz.deleteChartById(chart.id)
				println(chart.dump())
				logger.error("[Headless Chart Alert] Chart(${chart.id}) has no existent parent set. The broken chart has been deleted, and details are dumped above.")
			}
		}
	}

	// UTILITIES

	fun MongoUser.isOwned(chart: MongoChart): Boolean {
		val set = chart.getParentSet()
		return set.id in ownedSets && chart.id in ownedCharts
	}

	override val MongoUser.tunerize: UserModel
		get() = UserModel(
			_id = id,
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
				_id = id,
				charter = UserWithUserNameModel(s.displayNoterName),
				chartName = "${s.musicName}_${difficultyClass}",
				gcPrice = 0,
				music = MusicModel(s.musicName, MusicianModel(s.composerName)),
				difficultyBase = difficultyClass,
				difficultyValue = difficultyValue,
				D = D
			)
		}

	override val MongoSet.tunerize: SetModel
		get() = SetModel(
			_id = id,
			introduction = introduction ?: "",
			coinPrice = price,
			noter = NoterModel(displayNoterName),
			musicTitle = musicName,
			composerName = composerName,
			playCount = 0,
			chart = charts.mapNotNull { getChart(it)?.tunerize?.minify },
			isGot = false,
			isRanked = status == SetStatus.RANKED || status == SetStatus.OFFICIAL,
			isOfficial = status == SetStatus.OFFICIAL,
			needReview = isReviewing
		)

	override fun MongoSet.tunerize(owner: MongoUser?): SetModel =
		if(owner == null) {
			tunerize
		} else {
			SetModel(
				_id = id,
				introduction = introduction ?: "",
				coinPrice = price,
				noter = NoterModel(displayNoterName),
				musicTitle = musicName,
				composerName = composerName,
				playCount = 0,
				chart = charts.mapNotNull { getChart(it)?.tunerize?.minify },
				isGot = id in owner.ownedSets,
				isRanked = status == SetStatus.RANKED || status == SetStatus.OFFICIAL,
				isOfficial = status == SetStatus.OFFICIAL,
				needReview = isReviewing
			)
		}

	val MongoUser.shrink: PlayerModel
		get() = PlayerModel(id, username, highestGoldenMedal ?: 0, R)

	val dz by lazy { DangerZone() }

	@Suppress("unused")
	inner class DangerZone internal constructor() {

		/**
		 * To delete an existent set and its charts, the related records and own status.
		 */
		fun deleteSetById(id: String) {
			val set = getSet(id) ?: return logger.warn("Unable to delete a non-existent set.")
			// delete charts
			set.charts.forEach(::deleteChartById)
			// remove the set from user
			userC.updateMany(Document(), pull(MongoUser::ownedSets, set.id))
			// remove the set
			chartSetC.deleteOneById(set.id)
			logger.info("Deleted Set(${set.id}): $set")
		}

		/**
		 * To delete an existent chart and the related records.
		 */
		fun deleteChartById(id: String) {
			val chart = getChart(id) ?: return logger.warn("Unable to delete a non-existent chart.")
			// remove playing records
			playRecordC.deleteMany(MongoRecord::chartId eq chart.id)
			// remove the chart from user
			userC.updateMany(Document(), pull(MongoUser::ownedCharts, chart.id))
			// remove the chart
			chartC.deleteOneById(chart.id)
			logger.info("Deleted Chart(${id}): $chart")
		}

		/**
		 * To delete an existent user and re-bind its sets to [default user][serverUser].
		 */
		fun deleteUserById(id: String) {
			val user = getUser(id) ?: return logger.warn("Unable to delete a non-existent user.")
			// replace the noterId to the default user.
			chartSetC.updateMany(MongoSet::noterId eq user.id, MongoSet::noterId setTo serverUser.id)
			// remove user
			userC.deleteOneById(user.id)
			logger.info("Deleted User(${user.id}): $user")
		}

	}
}
