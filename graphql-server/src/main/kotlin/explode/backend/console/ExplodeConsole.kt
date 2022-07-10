package explode.backend.console

import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowDataProvider
import kotlin.concurrent.thread
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

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

		return if(acc.getUser(username) != null) {
			"Duplicating username."
		} else {
			data.registerUser(username, password)
		}
	}

	@SubCommand(desc = "(/findUser <username>) Find a existing User with given <username>")
	fun findUser(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"

		return acc.getUser(username) ?: "Cannot find the user named $username"
	}

	@SubCommand(desc = "(/resetPassword <username>) Reset the password of a existing User with given <username>")
	fun resetPassword(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"

		val u = acc.getUser(username) ?: return "Cannot find the user named $username"
		val p = List(16) { (('A'..'z') + ('0'..'9')).random() }.joinToString()
		with(acc) {
			u.setPassword(p)
			return "New password: $p"
		}
	}

}