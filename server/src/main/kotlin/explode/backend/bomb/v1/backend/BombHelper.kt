package explode.backend.bomb.v1.backend

import explode.dataprovider.model.database.*
import explode.dataprovider.provider.IBlowReadOnly
import kotlinx.serialization.json.*
import kotlin.reflect.KProperty1

/*

Functions in this file is used to shrink the data of the objects,
in order not to leak sensitive values like user token, user password, etc.

These functions should be named by `bombify`.

 */

// from: https://stackoverflow.com/questions/60010298/how-can-i-convert-a-camel-case-string-to-snake-case-and-back-in-idiomatic-kotlin
private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
// convert camel cases to snake case like 'chartId' to 'chart-id'
@Suppress("unused")
private fun String.camelCaseToSnakeCase() = camelRegex.replace(this) { "-${it.value}" }.lowercase()

fun MongoUser.bombify() =
	buildJsonObject {
		put("id", id)
		put("username", username)
		put("r", R)
		put("coin", coin)
		put("diamond", diamond)
		put("highest-golden-medal", highestGoldenMedal)
	}

context(IBlowReadOnly)
fun MongoSet.bombify() =
	buildJsonObject {
		put("id", id)
		put("music-name", musicName)
		put("music-composer", composerName)
		put("introduction", introduction)
		put("price", price)
		put("status", status.name)
		put("noter-name", displayNoterName)
		putJsonArray("charts") {
			charts.forEach {
				add(it)
			}
		}
	}

context(IBlowReadOnly)
fun MongoChart.bombify() =
	buildJsonObject {
		put("id", id)
		put("difficulty-class", difficultyClass)
		put("difficulty-value", difficultyValue)
		put("d", D)
		put("included-in", getSetByChartId(id)?.id)
	}

fun MongoRecord.bombify() =
	buildJsonObject {
		put("chart-id", chartId)
		put("score", score)
		put("perfect", scoreDetail.perfect)
		put("good", scoreDetail.good)
		put("miss", scoreDetail.miss)
		put("r", RScore)
	}

fun MongoRecordRanked.bombify() =
	buildJsonObject {
		put("chart-id", chartId)
		put("score", score)
		put("perfect", scoreDetail.perfect)
		put("good", scoreDetail.good)
		put("miss", scoreDetail.miss)
		put("r", RScore)
		put("ranking", ranking)
	}

fun MongoReview.bombify() =
	buildJsonObject {
		put("id", id)
		put("set-id", reviewedSet)
		put("expect-status", expectStatus?.name)
		putJsonArray("reviews") {
			reviews.forEach {
				add(buildJsonObject {
					put("id", it.id)
					put("status", it.status)
					put("evaluation", it.evaluation)
					put("reviewer-id", it.reviewerId)
				})
			}
		}
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