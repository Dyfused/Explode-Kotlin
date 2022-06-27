package explode.backend.console

import explode.blow
import kotlin.concurrent.thread
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

@Retention(AnnotationRetention.RUNTIME)
annotation class SubCommand(val value: String = "", val desc: String = "")

object ExplodeConsole {

	private val validFunctions = mutableMapOf<String, KFunction<Any?>>()
	private val commandDescs = mutableMapOf<String, String>()

	init {
		ExplodeConsole::class.declaredFunctions.forEach { func ->
			val sc = func.findAnnotation<SubCommand>()
			if(sc != null) {
				if(func.parameters.size == 2) {
					val command = sc.value.ifBlank { func.name }

					validFunctions[command] = func
					if(sc.desc.isNotBlank()) { commandDescs[command] = sc.desc }

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

		return blow.registerUser(username, password)
	}

	@SubCommand
	fun findUser(sp: List<String>): Any {
		val username = sp.getOrNull(1) ?: return "Missing parameter: username"

		return blow.getUser("", username) ?: "Cannot find the user named $username"
	}

}