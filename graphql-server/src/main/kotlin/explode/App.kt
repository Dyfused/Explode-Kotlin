package explode

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import explode.backend.console.ExplodeConsole
import explode.dataprovider.provider.*
import explode.dataprovider.provider.mongo.MongoProvider
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val mainLogger = LoggerFactory.getLogger("Explode")

fun main(args: Array<String>) {

	disableMongoLogging()
	// disableKtorLogging()

	mainLogger.info("Explode ($GameVersion)")

	val m = MongoProvider()

	blow = m.provider
	blowAccess = m
	blowResource = m

	val operation = args.getOrNull(0)

	Json {
		ignoreUnknownKeys = true
	}

	ExplodeConsole.loop()

	when(operation) {
		"backend", null -> startKtorServer(args)
		else -> println("Unknown subcommand: $operation")
	}

	mainLogger.info("Exploded.")
}

private fun disableMongoLogging() {
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("org.mongodb.driver").level = Level.WARN
}

private fun disableKtorLogging() {
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("Application").level = Level.WARN
}

private fun startKtorServer(args: Array<String>) {
	EngineMain.main(args)
}

/**
 * set to 'true' for following behaviors:
 *   - print every GraphQL request body
 */
const val DebugMode = true

const val GameVersion = 81

/**
 * This field is used to construct the Schema.
 */
lateinit var blow: IBlowDataProvider

lateinit var blowAccess: IBlowAccessor

lateinit var blowResource: IBlowResourceProvider