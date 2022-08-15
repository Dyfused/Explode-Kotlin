@file:Suppress("MemberVisibilityCanBePrivate")

package explode.mirai.command

import explode.dataprovider.model.database.MongoChart

object StringifyHelper {

	fun getDifficultyClassText(difficultyClass: Int) = when(difficultyClass) {
		1 -> "CASUAL"
		2 -> "NORMAL"
		3 -> "HARD"
		4 -> "MEGA"
		5 -> "GIGA"
		else -> "UNKNOWN"
	}

	fun getShortDifficultyClassText(difficultyClass: Int) = when(difficultyClass) {
		1 -> "C"
		2 -> "N"
		3 -> "H"
		4 -> "M"
		5 -> "G"
		else -> "U"
	}

	fun getSimpleHardness(chart: MongoChart) =
		getSimpleHardness(chart.difficultyClass, chart.difficultyValue)

	fun getSimpleHardness(difficultyClass: Int, difficultyValue: Int) =
		"${getShortDifficultyClassText(difficultyClass)}${difficultyValue}"

}