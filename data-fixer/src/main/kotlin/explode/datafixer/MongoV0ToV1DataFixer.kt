package explode.datafixer

import com.mongodb.client.MongoDatabase
import explode.dataprovider.model.database.*
import explode.dataprovider.model.game.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import java.time.OffsetDateTime

object MongoV0ToV1DataFixer : VersionedDataFixer {

	private lateinit var dbOld: MongoDatabase
	private lateinit var dbNew: MongoDatabase

	override fun fix() {
		println("[MongoV0ToV1FixExecutor] Upgrading MongoDB.")

		prepare()

		fixUsers()
		fixChartsAndSets()
		fixPlayRecords()
	}

	override val versionBeforeFix: Int = 0
	override val versionAfterFix: Int = 1

	override fun prepare() {
		print("Database URI (mongodb://localhost:27017): ")
		val dbUri = readLine()?.takeIf { it.isNotEmpty() } ?: "mongodb://localhost:27017"
		print("Database Name (Explode): ")
		val dbName = readLine()?.takeIf { it.isNotEmpty() } ?: "Explode"
		val cli = KMongo.createClient(dbUri)
		dbOld = cli.getDatabase(dbName)
		dbNew = cli.getDatabase(dbName+"_NEW")
	}

	private fun fixUsers() {

		@Serializable
		class IdAndPassword(val _id: String, val password: String)

		val oldColl = dbOld.getCollection<UserModel>("User")
		val newColl = dbNew.getCollection<MongoUser>("User")

		val oldPasswordColl = dbOld.getCollection<IdAndPassword>("UserLogin")

		oldColl.find().forEach {
			val id = it._id
			val password = oldPasswordColl.findOneById(id)?.password ?: ""

			println("Fix User $id")

			val new = MongoUser(
				id = it._id,
				username = it.username,
				password = password,
				ownedSets = it.ownSet,
				ownedCharts = it.ownChart,
				coin = it.coin ?: 0,
				diamond = it.diamond ?: 0,
				ppTime = it.PPTime,
				token = it.token,
				R = it.RThisMonth ?: 0,
				permission = UserPermission(it.access.reviewer)
			)

			oldColl.deleteOneById(id)
			newColl.insertOne(new)
			oldPasswordColl.deleteOneById(id)
		}

		oldPasswordColl.drop()
	}

	// use afer [fixUsers] done.
	private val users by lazy { dbNew.getCollection<MongoUser>("User") }

	private fun fixChartsAndSets() {
		val oldSetColl = dbOld.getCollection<SetModel>("ChartSet")
		val newSetColl = dbNew.getCollection<MongoSet>("ChartSet")

		val oldChartColl = dbOld.getCollection<DetailedChartModel>("Chart")
		val newChartColl = dbNew.getCollection<MongoChart>("Chart")

		oldSetColl.find().forEach { s ->
			val id = s._id

			println("Fix Set $id")

			val new = MongoSet(
				id = s._id,
				musicName = s.musicTitle,
				composerName = s.composerName,
				noterId = users.findOne(MongoUser::username eq s.noter.username)?.id ?: "f6fe9c4d-98e6-450a-937c-d64848eacc40",
				introduction = s.introduction,
				price = s.coinPrice,
				status = if(s.isOfficial) SetStatus.OFFICIAL else if(s.isRanked) SetStatus.RANKED else if(s.needReview) SetStatus.NEED_REVIEW else SetStatus.UNRANKED,
				charts = s.chart.map { it._id }.toMutableList()
			)

			oldSetColl.deleteOneById(id)
			newSetColl.insertOne(new)
		}

		oldChartColl.find().forEach {
			val id = it._id

			println("Fix Chart $id")

			val new = MongoChart(
				id = it._id,
				difficultyClass = it.difficultyBase,
				difficultyValue = it.difficultyValue,
				D = it.D
			)
			oldChartColl.deleteOneById(id)
			newChartColl.insertOne(new)
		}
	}

	private fun fixPlayRecords() {
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

		val oldColl = dbOld.getCollection<PlayRecordData>("PlayRecord")
		val newColl = dbNew.getCollection<MongoRecord>("PlayRecord")

		oldColl.find().forEach {
			val id = it._id

			println("Fix PlayRecord $id")

			val new = MongoRecord(
				id = it._id,
				playerId = it.playerId,
				chartId = it.playedChartId,
				score = it.score,
				scoreDetail = ScoreDetail(it.perfect, it.good, it.miss),
				uploadedTime = it.time,
				RScore = it.currentR
			)

			oldColl.deleteOneById(id)
			newColl.insertOne(new)
		}
	}
}
