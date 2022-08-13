package explode.dataprovider.provider

import explode.dataprovider.model.database.MongoChart
import explode.dataprovider.model.database.MongoRecord
import kotlin.reflect.KProperty

private val DifficultyStringIntMapping = mapOf(
	"casual" to 1,
	"normal" to 2,
	"hard" to 3,
	"mega" to 4,
	"giga" to 5,
	"tera" to 6,
	"unknown" to 0
)

private val DifficultyIntStringMapping = DifficultyStringIntMapping.map { (k, v) -> v to k }.toMap()

object DifficultyUtils {
	fun String.toDifficultyClassNum() = DifficultyStringIntMapping.getOrElse(this.lowercase()) { 0 }
	fun Int.toDifficultyClassStr() = DifficultyIntStringMapping.getOrElse(this) { "unknown" }
}

enum class RecordSort(val prop: KProperty<*>) {
	TIME(MongoRecord::uploadedTime),
	SCORE(MongoRecord::score);
}

fun compareCharts(c1s: Collection<MongoChart>, c2s: Collection<MongoChart>): Boolean {
	val c2sCompared = c2s.toMutableList()
	c1s.forEach { c1c ->
		val c2c = c2s.firstOrNull { it.difficultyClass == c1c.difficultyClass }
		if(c2c == null || c1c.difficultyValue != c2c.difficultyValue) {
			return false
		} else {
			c2sCompared -= c2c
		}
	}
	c2sCompared.forEach { c2c ->
		val c1c = c1s.firstOrNull { it.difficultyClass == c2c.difficultyClass }
		if(c1c == null || c1c.difficultyValue != c2c.difficultyValue) {
			return false
		}
	}
	return true
}