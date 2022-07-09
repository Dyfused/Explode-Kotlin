package explode.rena.model

import java.io.File

enum class ChartDifficulty {
	CASUAL, NORMAL, HARD, MEGA, GIGA, TERA, UNKNOWN;

	fun toInt() = when(this) {
		CASUAL -> 1
		NORMAL -> 2
		HARD -> 3
		MEGA -> 4
		GIGA -> 5
		TERA -> 6
		else -> 0
	}

	companion object {
		fun Int.toDifficulty() = when(this) {
			1 -> CASUAL
			2 -> NORMAL
			3 -> HARD
			4 -> MEGA
			5 -> GIGA
			6 -> TERA
			else -> UNKNOWN
		}

		val normalValues = ChartDifficulty.values().filter { it != UNKNOWN }
	}
}

class RenaStandardPack(
	val renaSets: MutableList<RenaSet>,
)

class RenaSet(
	val meta: RenaMeta,

	val music: RenaContent,
	val cover: RenaContent,
	val preview: RenaContent,
	val charts: List<RenaChartContent>,

	val id: String? = null,
)

class RenaChart(
	val difficulty: ChartDifficulty,
	val difficultyLevel: Int,
	val content: RenaContent
)

interface RenaContent {
	val isRnx: Boolean
	fun getBytes(): ByteArray
}

interface RenaChartContent : RenaContent {
	val difficulty: ChartDifficulty
	val difficultyLevel: Int
}

fun File.toRenaContent() = object : RenaContent {
	override val isRnx: Boolean get() = extension == "rnx"
	override fun getBytes(): ByteArray = this@toRenaContent.readBytes()
}

fun File.toRenaChartContent() = object : RenaChartContent {
	override val isRnx: Boolean get() = extension == "rnx"
	override fun getBytes(): ByteArray = this@toRenaChartContent.readBytes()

	override val difficulty: ChartDifficulty = ChartDifficulty.UNKNOWN
	override val difficultyLevel: Int = 0
}

data class RenaMeta(
	val name: String,
	val composer: String,
	val noter: String,
	val introduct: String,

	val id: String? = null
)

fun RenaMeta(content: String): RenaMeta {
	val m = content.toRenaStyledMap()
	return RenaMeta(m["N"]!! as String, m["W"]!! as String, m["U"]!! as String, m["I"]!! as String, m["B"] as String?)
}