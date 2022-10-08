@file:JvmName("Console")

package explode.backend.console

import explode.backend.importExplodePack
import explode.dataprovider.model.game.PlayModInput
import explode.dataprovider.model.game.PlayRecordInput
import explode.dataprovider.provider.BlowException
import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.mongo.MongoProvider
import java.io.File
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
			runCatching {
				validFunctions[sp.getOrNull(0)]?.call(this, sp)?.let(::println)
			}.onFailure {
				when(it) {
					is BlowException, is IllegalStateException -> println(it.message)
					else -> it.printStackTrace()
				}
			}
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
		u.password = p
		return "New password: $p"
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
		val sets = mp?.getSetsByName(setName) ?: return "Invalid parameter: setName"

		sets.forEach { set ->
			println("<${set.musicName}> ${set.id}")
			set.charts.mapNotNull(mp::getChart).forEach { chart ->
				println("- <${chart.id}> [${chart.difficultyClass}] ${chart.difficultyValue}")
			}
		}
		return ""
	}

	@SubCommand(desc = "(/findChart <chartId>) List the chart in specified name.")
	fun findChart(sp: List<String>): Any {
		val chartId = sp.getOrNull(1) ?: return "Missing parameter: chartId"
		val chart = mp?.getChart(chartId) ?: return "Invalid parameter: chartId"

		println("- <${chart.id}> [${chart.difficultyClass}] ${chart.difficultyValue}")
		return ""
	}

	@SubCommand(desc = "(/addReview <setId> <status: true/false> [message]) Review the specified set.")
	fun addReview(sp: List<String>): Any {
		val setId = sp.getOrNull(1) ?: return "Missing parameter: setId"
		val status = sp.getOrNull(2)?.toBooleanStrictOrNull() ?: return "Invalid parameter: status"
		val message = sp.getOrNull(3).orEmpty()
		val set = mp?.getSet(setId) ?: return "Invalid parameter: setId"

		with(mp) {
			set.getReview()?.addReview(serverUser.id, status, message)
		}

		return "Done"
	}

	@SubCommand(desc = "(/listReview <setId>) Get the review info of specified set.")
	fun listReview(sp: List<String>): Any {
		val setId = sp.getOrNull(1) ?: return "Missing parameter: setId"
		val set = mp?.getSet(setId) ?: return "Invalid parameter: setId"

		val r = with(mp) { set.getReview() }
		if(r == null) {
			return "No ongoing Review."
		} else {
			val size = r.reviews.size
			println("Ongoing review $size: ")
			r.reviews.forEach {
				val u = mp.getUser(it.reviewerId)
				println(" - ${u?.username ?: it.reviewerId}(${it.status}) ${it.evaluation}")
			}
		}
		return ""
	}

	@SubCommand(desc = "(/endReview <setId> [status=true]) End the review of the specified set.")
	fun endReview(sp: List<String>): Any {
		val setId = sp.getOrNull(1) ?: return "Missing parameter: setId"
		val set = mp?.getSet(setId) ?: return "Invalid parameter: setId"
		val status = sp.getOrNull(2)?.toBooleanStrictOrNull() ?: true

		with(mp) {
			set.getReview() ?: return "No ongoing review for <${set.musicName}>(${set.id})"
			set.endReview(status)
		}

		return ""
	}

	@SubCommand(desc = "(/startReview <setId>) Start a review on a NEED_REVIEW set.")
	fun startReview(sp: List<String>): Any {
		val setId = sp.getOrNull(1) ?: return "Missing parameter: setId"
		val set = mp?.getSet(setId) ?: return "Invalid parameter: setId"

		with(mp) {
			set.startReview()
		}

		return "Done"
	}

	@SubCommand(desc = "(/generateFakeRecords <chartId> [count]) Generate fake records for the chart.")
	fun generateFakeRecords(sp: List<String>): Any {
		val chartId = sp.getOrNull(1) ?: return "Missing parameter: chartId"
		val chart = mp?.getChart(chartId) ?: return "Invalid parameter: chartId"
		val count = sp.getOrNull(2)?.toIntOrNull() ?: 20

		repeat(count) {
			with(mp) {
				serverUser.buySet(chart.getParentSet().id)

				val rid = serverUser.submitBeforePlay(chartId, 0, "").playingRecord.randomId
				serverUser.submitAfterPlay(
					PlayRecordInput(
						PlayModInput(1.0, 1.0, isBleed = true, isMirror = false),
						true,
						(0..1000000).random(),
						(0..255).random(),
						(0..255).random(),
						(0..255).random()
					),
					rid
				)
			}
		}

		return "Done"
	}

	@SubCommand(desc = "(/importPack <path>) Import a set of charts by Explode Pack Meta file.")
	fun importPack(sp: List<String>): Any {
		val path = sp.getOrNull(1) ?: return "Missing parameter: path"
		try {
			mp?.importExplodePack(File(path))
		} catch(ex: Exception) {
			ex.printStackTrace()
			return ex.message.orEmpty()
		}
		return "Done"
	}

}