package explode.dataprovider.model.game

import kotlinx.serialization.Serializable

@Serializable
data class AssessmentGroupModel(
	val _id: String,
	val name: String,
	val assessment: List<AssessmentModel>
)

@Serializable
data class AssessmentModel(
	val _id: String,
	val medalLevel: Int,
	val lifeBarLength: Double,
	val normalPassAcc: Double,
	val goldenPassAcc: Double,
	val exMiss: Double,
	val chart: List<AssessmentChartModel>,
	// A stupid design by Dynamite: They only read the 'best' entry but need to send a list of these shit to the client.
	// so in Explode, we only return a one-element list.
	val assessmentRecord: List<AssessmentRecordsModel>
)

@Serializable
data class AssessmentChartModel(
	val _id: String,
	val set: SetModel
)

@Serializable
data class AssessmentRecordsModel(
	val achievementRate: Double, // Round in [0, 200]
	val isBest: Boolean,
	val playRecord: List<AssessmentPlayRecordModel>
)

@Serializable
data class AssessmentPlayRecordModel(
	val perfect: Int,
	val good: Int,
	val miss: Int,
	val score: Int
)

@Serializable
data class AfterAssessmentModel(
	val result: Int,
	val RThisMonth: Int,
	val coin: Int?,
	val diamond: Int?
)