package explode.datafixer

import com.mongodb.client.MongoDatabase
import explode.dataprovider.model.database.*
import explode.dataprovider.model.game.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import java.time.OffsetDateTime

object MongoV0ToV1DataFixer : DataFixer {

	private lateinit var db: MongoDatabase

	override fun fix() {

		prepareDatabase()

		fixUsers()
		fixChartsAndSets()
		fixPlayRecords()
	}

	private fun prepareDatabase() {
		print("Database URI (mongodb://localhost:27017): ")
		val dbUri = readLine()?.takeIf { it.isNotEmpty() } ?: "mongodb://localhost:27017"
		print("Database Name (Explode): ")
		val dbName = readLine()?.takeIf { it.isNotEmpty() } ?: "Explode"
		val cli = KMongo.createClient(dbUri)
		db = cli.getDatabase(dbName)
	}

	private fun fixUsers() {

		@Serializable
		class IdAndPassword(val _id: String, val password: String)

		val oldColl = db.getCollection<UserModel>("User")
		val newColl = db.getCollection<MongoUser>("User")

		val oldPasswordColl = db.getCollection<IdAndPassword>("UserLogin")

		oldColl.find().forEach {
			val id = it._id
			val password = oldPasswordColl.findOneById(id)?.password ?: ""

			println("Fix User $id")

			val new = MongoUser(
				_id = it._id,
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
	private val users by lazy { db.getCollection<MongoUser>("User") }

	private fun fixChartsAndSets() {
		val oldSetColl = db.getCollection<SetModel>("ChartSet")
		val newSetColl = db.getCollection<MongoSet>("ChartSet")

		val oldChartColl = db.getCollection<DetailedChartModel>("Chart")
		val newChartColl = db.getCollection<MongoChart>("Chart")

		oldSetColl.find().forEach { s ->
			val id = s._id

			println("Fix Set $id")

			val new = MongoSet(
				_id = s._id,
				musicName = s.musicTitle,
				composerName = s.composerName,
				noterId = users.findOne(MongoUser::username eq s.noter.username)?._id ?: "f6fe9c4d-98e6-450a-937c-d64848eacc40",
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
				_id = it._id,
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

		val oldColl = db.getCollection<PlayRecordData>("PlayRecord")
		val newColl = db.getCollection<MongoRecord>("PlayRecord")

		oldColl.find().forEach {
			val id = it._id

			println("Fix PlayRecord $id")

			val new = MongoRecord(
				_id = it._id,
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
