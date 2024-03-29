package explode.compose.data

import explode.dataprovider.provider.mongo.MongoProvider

object StoreDataProvider {

	internal val p = MongoProvider()

	fun getChartSets() = p.getSetList(10, 0, isRanked = true)

	fun getChartSets(
		limit: Int,
		skip: Int,
		searchedName: String? = null,
		isHidden: Boolean? = null,
		isOfficial: Boolean? = null,
		isRanked: Boolean? = null,
		isNeedReview: Boolean? = null
	) = with(p) { getSetList(limit, skip, searchedName, isHidden, isOfficial, isRanked, isNeedReview).map { it.tunerize } }

}