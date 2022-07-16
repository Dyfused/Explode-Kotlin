package explode.compose.data

import TConfig.Configuration
import explode.dataprovider.detonate.ExplodeConfig.Companion.explode
import explode.dataprovider.provider.mongo.MongoProvider
import java.io.File

object StoreDataProvider {

	internal val p = MongoProvider(Configuration(File("./provider.cfg")).explode())

	fun getChartSets() = p.getSetList(10, 0, isRanked = true)

	fun getChartSets(
		limit: Int,
		skip: Int,
		searchedName: String? = null,
		isHidden: Boolean? = null,
		isOfficial: Boolean? = null,
		isRanked: Boolean? = null,
		isNeedReview: Boolean? = null
	) = p.getSetList(limit, skip, searchedName, isHidden, isOfficial, isRanked, isNeedReview)

}