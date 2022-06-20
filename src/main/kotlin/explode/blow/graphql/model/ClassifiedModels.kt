package explode.blow.graphql.model

data class ChartSetAndVersion(
	val setId: String,
	val version: Int
)

object ClassifiedModels {
	// ONLY FOR
	// refreshSet
	data class Set(
		val _id: String,
		val isRanked: Boolean,
		val introduction: String,
		val noterName: String,
		val musicTitle: String
	)

	data class SetWithOnlyIdAndWeDontKnowWhatItIs(
		val _id: String
	)
}