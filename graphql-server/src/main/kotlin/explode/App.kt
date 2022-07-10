package explode

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import explode.backend.console.ExplodeConsole
import explode.backend.graphql.dynamiteResourceModule
import explode.backend.graphql.graphQLModule
import explode.dataprovider.provider.*
import explode.dataprovider.provider.mongo.MongoProvider
import explode.utils.Config
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

private val mainLogger = LoggerFactory.getLogger("Explode")

val explodeConfig = Config(File("explode.cfg")).also { it.save() }

fun main(args: Array<String>) {

	if(!explodeConfig.mongoLogging) {
		disableMongoLogging()
	}
	if(!explodeConfig.ktorLogging) {
		disableKtorLogging()
	}

	mainLogger.info("Explode ($GameVersion)")

	val m = MongoProvider()

	@Suppress("DEPRECATION") // no warning to set
	run {
		blow = m.provider
		blowAccess = m
		blowResource = m
	}

	val operation = args.getOrNull(0)

	Json {
		ignoreUnknownKeys = true
	}

	ExplodeConsole(m.provider).loop()

	when(operation) {
		"backend", null -> startKtorServer(m.provider, m)
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

private fun startKtorServer(dataProvider: IBlowDataProvider, resourceProvider: IBlowResourceProvider) {
	// EngineMain.main(args) // deprecated since it is not allowed to modify the port in the code.
	embeddedServer(
		Netty,
		environment = applicationEngineEnvironment {
			module {
				graphQLModule(dataProvider)
				dynamiteResourceModule(resourceProvider)
				// bombModule() // dont use due to working in progress state.
			}

			connector {
				port = explodeConfig.port
			}
		}
	).start(true)
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
@Deprecated("Legacy")
lateinit var blow: IBlowDataProvider

@Deprecated("Legacy")
lateinit var blowAccess: IBlowAccessor

@Deprecated("Legacy")
lateinit var blowResource: IBlowResourceProvider