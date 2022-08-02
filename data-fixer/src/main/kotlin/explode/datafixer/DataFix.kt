package explode.datafixer

import java.io.File

class DataFix(
	private val versionNumberFile: File,
	var compatibleVersion: Int
) {

	init {
		if(!versionNumberFile.exists()) {
			versionNumberFile.writeText("$compatibleVersion")
		}
	}

	var currentVersion: Int
		get() = versionNumberFile.readText().toInt()
		set(value) { versionNumberFile.writeText(value.toString()) }

	companion object {
		val FixExecutors = mutableMapOf(
			0 to MongoV0ToV1DataFixer
		)
	}

	val shouldUpdate: Boolean get() = currentVersion < compatibleVersion

	private fun executeUpdateOnce() {
		val fixer = FixExecutors[currentVersion]
		if(fixer == null) {
			error("No Data Fixer provided for current version $currentVersion.")
		} else {
			fixer.fix()
			currentVersion = fixer.versionAfterFix
		}
	}

	fun executeUpdate() {
		while(shouldUpdate) {
			executeUpdateOnce()
		}
	}

}