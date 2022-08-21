package explode.backend.bomb.v1

import explode.dataprovider.model.database.*
import explode.dataprovider.provider.IBlowReadOnly
import kotlin.reflect.KProperty1

/*

Functions in this file is used to shrink the data of the objects,
in order not to leak sensitive values like user token, user password, etc.

These functions should be named by `bombify`.

 */

private fun <T> doBombify(any: T, vararg fields: KProperty1<T, *>) =
	buildMap {
		fields.forEach {
			this[it.name.camelCaseToSnakeCase()] = it.get(any)
		}
	}

context(MutableMap<String, Any?>)
private fun <T> T.property(property: KProperty1<T, *>) {
	put(property.name.camelCaseToSnakeCase(), property.get(this))
}

context(MutableMap<String, Any?>)
private fun <T> T.properties(vararg properties: KProperty1<T, *>) = properties.forEach { property(it) }

// from: https://stackoverflow.com/questions/60010298/how-can-i-convert-a-camel-case-string-to-snake-case-and-back-in-idiomatic-kotlin
private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
// convert camel cases to snake case like 'chartId' to 'chart-id'
private fun String.camelCaseToSnakeCase() = camelRegex.replace(this) { "-${it.value}" }.lowercase()

fun MongoUser.bombify() =
	buildMap {
		properties(
			MongoUser::id,
			MongoUser::username,
			MongoUser::R,
			MongoUser::coin,
			MongoUser::diamond,
			MongoUser::highestGoldenMedal
		)
	}

context(IBlowReadOnly)
fun MongoSet.bombify() =
	buildMap {
		properties(
			MongoSet::id,
			MongoSet::musicName,
			MongoSet::composerName,
			MongoSet::introduction,
			MongoSet::price,
			MongoSet::status,
			MongoSet::charts,
		)
		// to replace the [noterId] and [noterDisplayOverride]
		this["display-noter-name"] = noterDisplayOverride ?: getUser(noterId)?.username ?: "unknown"
	}

context(IBlowReadOnly)
fun MongoChart.bombify() =
	buildMap {
		properties(
			MongoChart::id,
			MongoChart::difficultyClass,
			MongoChart::difficultyValue,
			MongoChart::D
		)
		this["included-in"] = getSetByChartId(id)?.id
	}

fun MongoRecord.bombify() =
	buildMap {
		properties(
			MongoRecord::chartId,
			MongoRecord::score,
			MongoRecord::RScore,
			MongoRecord::uploadedTime
		)
		// unwrap [scoreDetail]
		this["perfect"] = scoreDetail.perfect
		this["good"] = scoreDetail.good
		this["miss"] = scoreDetail.miss
	}

fun MongoRecordRanked.bombify() =
	doBombify(
		this,
		MongoRecordRanked::chartId,
		MongoRecordRanked::score,
		MongoRecordRanked::scoreDetail,
		MongoRecordRanked::RScore,
		MongoRecordRanked::ranking,
		MongoRecordRanked::uploadedTime
	)

fun MongoReview.bombify() =
	buildMap {
		properties(
			MongoReview::id,
			MongoReview::reviewedSet,
			MongoReview::reviews,
			MongoReview::expectStatus
		)
	}

/**
 * Replace [this] field values with [newValues] by Java reflection.
 */
internal inline fun <reified T : Any> T.reflectApplyChanges(
	newValues: Map<String, Any?>,
	vararg excludedFields: KProperty1<T, *>
) {
	val c = T::class.java
	val exclude = excludedFields.map { it.name }

	// replace the values with Java reflection
	newValues.forEach { (name, value) ->
		kotlin.runCatching {
			if(name !in exclude) {
				val field = c.getDeclaredField(name).apply { isAccessible = true }
				field.set(this, value)
			}
		}.onFailure {
			it.printStackTrace()
		}
	}
}