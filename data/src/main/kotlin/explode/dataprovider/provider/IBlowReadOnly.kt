package explode.dataprovider.provider

import com.mongodb.client.FindIterable
import explode.dataprovider.model.database.*
import explode.dataprovider.model.game.*

interface IBlowReadOnly {

	val serverUser: MongoUser

	// getters
	fun getUser(userId: String): MongoUser?
	fun getSet(setId: String): MongoSet?
	fun getChart(chartId: String): MongoChart?
	fun getRecord(recordId: String): MongoRecord?

	// advanced getters
	fun getSetByChartId(chartId: String): MongoSet?
	fun getUserByName(username: String): MongoUser?
	fun getUserByToken(token: String): MongoUser?
	fun getUserRecord(userId: String, limit: Int, skip: Int, sort: RecordSort): Iterable<MongoRecord>
	fun getChartRecord(chartId: String, limit: Int, skip: Int, sort: RecordSort): Iterable<MongoRecord>
	fun getUserChartRecord(userId: String, chartId: String, limit: Int, skip: Int, sort: RecordSort, duplicate: Boolean = false): Iterable<MongoRecord>

	fun getAssessmentGroups(limit: Int, skip: Int): List<AssessmentGroupModel>
	fun getAssessmentRank(
		assessmentGroupId: String,
		medalLevel: Int,
		limit: Int,
		skip: Int
	): List<AssessmentRecordWithRankModel>
	fun getPlayRank(chartId: String, limit: Int, skip: Int): List<PlayRecordWithRank>
	fun MongoUser.getAssessmentRankSelf(
		assessmentGroupId: String,
		medalLevel: Int
	): AssessmentRecordWithRankModel?
	fun MongoUser.getPlayRankSelf(chartId: String): PlayRecordWithRank?
	fun MongoUser.getLastPlayRecords(limit: Int, skip: Int): Iterable<MongoRecordRanked>
	fun MongoUser.getBestPlayRecords(limit: Int, skip: Int): Iterable<MongoRecordRanked>

	fun getReviewList(): FindIterable<MongoReview>

	// transformers
	val MongoUser.tunerize: UserModel
	val MongoChart.tunerize: DetailedChartModel
	val MongoSet.tunerize: SetModel
}