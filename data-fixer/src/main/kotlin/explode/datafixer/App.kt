@file:JvmName("App")
package explode.datafixer

import java.time.Duration
import java.time.LocalDateTime

fun main() {

	println("Explode Data-Fixer")
	println("Current available: ${NamedDataFixer.values().map(NamedDataFixer::name)}")
	println()

	print("Use Data-Fixer[Default=MongoV0ToV1]: ")
	val fixerName = readLine()

	val fixer = runCatching {
		if(fixerName.isNullOrEmpty()) {
			NamedDataFixer.MongoV0ToV1
		} else {
			NamedDataFixer.valueOf(fixerName)
		}
	}.onFailure {
		println("Cannot find $fixerName.")
	}.getOrDefault(NamedDataFixer.MongoV0ToV1)

	val startTiming = LocalDateTime.now()
	fixer.fix()

	println("Done (${Duration.between(startTiming, LocalDateTime.now()).toMillis()}ms)")
}

interface FixExecutor {
	fun fix()
	fun prepare()
	val versionBeforeFix: Int
	val versionAfterFix: Int
}

enum class NamedDataFixer(private val fixer: FixExecutor) : FixExecutor by fixer {
	MongoV0ToV1(MongoV0ToV1FixExecutor)
}