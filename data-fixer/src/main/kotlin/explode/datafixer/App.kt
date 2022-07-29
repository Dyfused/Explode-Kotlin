@file:JvmName("App")
package explode.datafixer

import java.time.Duration
import java.time.LocalDateTime

fun main() {

	println("Explode Data-Fixer")
	println("Current available: ${DataFixers.values().map(DataFixers::name)}")
	println()

	print("Use Data-Fixer[Default=MongoV0ToV1]: ")
	val fixerName = readLine()

	val fixer = runCatching {
		if(fixerName == null || fixerName.isEmpty()) {
			DataFixers.MongoV0ToV1
		} else {
			DataFixers.valueOf(fixerName)
		}
	}.onFailure {
		println("Cannot find $fixerName.")
	}.getOrDefault(DataFixers.MongoV0ToV1)

	val startTiming = LocalDateTime.now()
	fixer.fix()

	println("Done (${Duration.between(startTiming, LocalDateTime.now()).toMillis()}ms)")
}

interface DataFixer {
	fun fix()
}

enum class DataFixers : DataFixer {
	MongoV0ToV1 {
		override fun fix() {
			MongoV0ToV1DataFixer.fix()
		}
	}
}