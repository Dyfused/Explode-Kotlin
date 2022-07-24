@file:Suppress("UNUSED_PARAMETER")

package explode.backend.console

import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowDataProvider
import kotlin.concurrent.thread
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.system.exitProcess

@Retention(AnnotationRetention.RUNTIME)
annotation class SubCommand(val value: String = "", val desc: String = "")

class ExplodeConsole(private val data: IBlowDataProvider, private val acc: IBlowAccessor) {

	private val validFunctions = mutableMapOf<String, KFunction<Any?>>()
	private val commandDescs = mutableMapOf<String, String>()

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

		return if(data.getUserByName(username) != null) {
			"Duplicating username."
		} else {
			data.registerUser(username, password)
		}
	}

	@SubCommand(desc = "(/findUser <username>) Find a existing User with given <username>")
	fun findUser(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"

		return data.getUserByName(username) ?: "Cannot find the user named $username"
	}

	@SubCommand(desc = "(/resetPassword <username> [password]) Reset the password of a existing User with given <username>")
	fun resetPassword(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"
		val u = data.getUserByName(username) ?: return "Cannot find the user named $username"
		val p = sp.getOrNull(2) ?: List(8) { (('A'..'z') + ('0'..'9')).random() }.joinToString()
		with(data) {
			u.password = p
			return "New password: $p"
		}
	}

	@SubCommand(desc = "(/setChartRankedState <setId> [rankedState=true]) Change the 'ranked' state of given Chart Set.")
	fun setChartRankedState(sp: List<String>): Any {
		val chartId = sp.getOrNull(1) ?: return "Missing parameter: setId"
		val rankedState = (sp.getOrNull(2) ?: "true").toBooleanStrictOrNull() ?: return "Invalid parameter: rankedState"

		val s = acc.getSet(chartId) ?: return "Cannot find the chart with ID $chartId"

		s.isRanked = rankedState

		return "Set(${s._id})[${s.musicTitle}] has changed ranked state to ${s.isRanked}"
	}

	@SubCommand(desc = "(/listRanking <ChartId> [limit=15] [skip=0]) List the play records of the given chart.")
	fun listRanking(sp: List<String>): Any {
		val chartId = sp.getOrNull(1) ?: return "Missing parameter: chartId"
		val limit = sp.getOrNull(2)?.let { it.toIntOrNull() ?: return "Invalid parameter: limit" } ?: 15
		val skip = sp.getOrNull(3)?.let { it.toIntOrNull() ?: return "Invalid parameter: skip" } ?: 0

		val l = data.getPlayRank(chartId, limit, skip);
		println("Fetched ${l.size} records:")
		l.forEach {
			println("[${it.rank}] ${it.player.username} - ${it.score}(${it.perfect}/${it.good}/${it.miss})")
		}
		return ""
	}

}