package explode.blow.graphql.model

import kotlinx.serialization.Serializable

@Serializable
data class ChartSetAndVersion(
	val setId: String,
	val version: Int
)

object ClassifiedModels {
	// ONLY FOR
	// refreshSet
	@Serializable
	data class Set(
		val _id: String,
		val isRanked: Boolean,
		val introduction: String,
		val noterName: String,
		val musicTitle: String
	)
}