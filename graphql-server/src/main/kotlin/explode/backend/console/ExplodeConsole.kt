@file:JvmName("Console")
package explode.backend.console

import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.mongo.MongoProvider
import kotlin.concurrent.thread
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

@Retention(AnnotationRetention.RUNTIME)
annotation class SubCommand(val value: String = "", val desc: String = "")

fun main() {
	val mp = MongoProvider()
	ExplodeConsole(mp).loop()
}

class ExplodeConsole(private val acc: IBlowAccessor) {

	private val validFunctions = mutableMapOf<String, KFunction<Any?>>()
	private val commandDescs = mutableMapOf<String, String>()

	private val mp = acc as? MongoProvider
	private val mpd = mp?.DangerZone()

	init {
		ExplodeConsole::class.declaredFunctions.forEach { func ->
			val sc = func.findAnnotation<SubCommand>()
			if(sc != null) {
				if(func.parameters.size == 2) {
					val command = sc.value.ifBlank { func.name }

					validFunctions[command] = func
					if(sc.desc.isNotBlank()) {
						commandDescs[command] = sc.desc
					}

					println("Found Subcommand[$command]${if(sc.desc.isNotBlank()) ": ${sc.desc}" else ""}")
				}
			}
		}
	}

	fun loop() = thread(isDaemon = true, name = "ExplodeConsole") {
		while(true) {
			val input = readLine() ?: continue
			val sp = input.split(' ')
			validFunctions[sp.getOrNull(0)]?.call(this, sp)?.let(::println)
		}
	}

	@SubCommand(desc = "(/registerUser <username> <password>) Register a new User with given <username> and <password>")
	fun registerUser(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"
		val password = sp.getOrNull(2) ?: return "Missing parameter: password"

		return if(acc.getUserByName(username) != null) {
			"Duplicating username."
		} else {
			acc.registerUser(username, password)
		}
	}

	@SubCommand(desc = "(/findUser <username>) Find a existing User with given <username>")
	fun findUser(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"

		return acc.getUserByName(username) ?: "Cannot find the user named $username"
	}

	@SubCommand(desc = "(/resetPassword <username> [password]) Reset the password of a existing User with given <username>")
	fun resetPassword(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"
		val u = acc.getUserByName(username) ?: return "Cannot find the user named $username"
		val p = sp.getOrNull(2) ?: List(8) { (('A'..'z') + ('0'..'9')).random() }.joinToString()
		with(acc) {
			u.password = p
			return "New password: $p"
		}
	}

	@SubCommand(desc = "(/listRanking <ChartId> [limit=15] [skip=0]) List the play records of the given chart.")
	fun listRanking(sp: List<String>): Any {
		val chartId = sp.getOrNull(1) ?: return "Missing parameter: chartId"
		val limit = sp.getOrNull(2)?.let { it.toIntOrNull() ?: return "Invalid parameter: limit" } ?: 15
		val skip = sp.getOrNull(3)?.let { it.toIntOrNull() ?: return "Invalid parameter: skip" } ?: 0

		val l = acc.getPlayRank(chartId, limit, skip)
		println("Fetched ${l.size} records:")
		l.forEach {
			println("[${it.rank}] ${it.player.username} - ${it.score}(${it.perfect}/${it.good}/${it.miss})")
		}
		return ""
	}

	@SubCommand(desc = "(/findSet <setName>) List the set in specified name.")
	fun findSet(sp: List<String>): Any {
		val setName = sp.toMutableList().apply { removeAt(0) }.joinToString(" ")
		val sets = mp?.getSetByName(setName) ?: return "Invalid parameter: setName"

		sets.forEach { set ->
			println("<${set.musicName}> ${set._id}")
			set.charts.mapNotNull(mp::getChart).forEach { chart ->
				println("- <${chart._id}> [${chart.difficultyClass}] ${chart.difficultyValue}")
			}
		}
		return ""
	}

	@SubCommand(desc = "(/findChart <chartId>) List the chart in specified name.")
	fun findChart(sp: List<String>): Any {
		val chartId = sp.getOrNull(1) ?: return "Missing parameter: chartId"
		val chart = mp?.getChart(chartId) ?: return "Invalid parameter: chartId"

		println("- <${chart._id}> [${chart.difficultyClass}] ${chart.difficultyValue}")
		return ""
	}

}