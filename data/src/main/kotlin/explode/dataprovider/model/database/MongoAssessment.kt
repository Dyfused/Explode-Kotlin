package explode.dataprovider.model.database

import explode.dataprovider.model.newUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MongoAssessment(
	var lifeBarLength: Double,

	/**
	 * The Accuracy rate of Normal Pass, should be in [0, 200).
	 */
	var normalPassAcc: Double,

	/**
	 * The Accuracy rate of Golden Pass, should be in (normalPassRate, 200]
	 */
	var goldenPassAcc: Double,

	/**
	 * The rate of Missing.
	 * This value is used to prevent players to play the Ex chart.
	 * should be in [0, 100]. Value at 100 won't prevent anyone.
	 */
	var exMiss: Double,

	/**
	 * Chart IDs of the assessment charts
	 */
	val charts: List<String>,

	@SerialName("_id")
	val id: String = newUUID()
)