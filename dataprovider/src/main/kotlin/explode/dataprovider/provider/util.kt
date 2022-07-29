package explode.dataprovider.provider

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

internal fun String.toFuzzySearch() =
	"{ \$regex: \"$this\", \$options: \"\$i\" }"

enum class RecordSort(val prop: KProperty<*>) {
	TIME(MongoRecord::uploadedTime),
	SCORE(MongoRecord::score);
}