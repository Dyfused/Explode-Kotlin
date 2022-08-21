package explode.dataprovider.model.database

import explode.dataprovider.model.newUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MongoAssessmentGroup(
	var name: String,
	val assessments: Map<Int, String>, // medalLevel to Assessment

	@SerialName("_id")
	val id: String = newUUID()
)
