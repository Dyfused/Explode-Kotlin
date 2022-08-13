package explode

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import explode.backend.bomb.bombModule
import explode.backend.console.ExplodeConsole
import explode.backend.graphql.dynamiteResourceModule
import explode.backend.graphql.graphQLModule
import explode.dataprovider.provider.IBlowAccessor
import explode.dataprovider.provider.IBlowResourceProvider
import explode.dataprovider.provider.mongo.MongoProvider
import explode.dataprovider.serializers.OffsetDateTimeSerializer
import explode.utils.Config
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.time.LocalDateTime

private val mainLogger = LoggerFactory.getLogger("Explode")

val explodeConfig = Config(File("explode.cfg")).also { it.save() }

private val theVeryBeginningTime = LocalDateTime.now()

fun main(args: Array<String>) {

	if(!explodeConfig.mongoLogging) {
		disableMongoLogging()
	}
	if(!explodeConfig.ktorLogging) {
		disableKtorLogging()
	}
	if(!explodeConfig.graphQLLogging) {
		disableGraphQLLogging()
	}

	mainLogger.info("Explode ${ExplodeInfo["version"]} ($GameVersion)")

	// prepare data
	val m = MongoProvider()

	val operation = args.getOrNull(0)

	ExplodeConsole(m).loop()

	when(operation) {
		"backend", null -> startKtorServer(m, m)
		"console" -> { /* Console Only */ }
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

private fun disableGraphQLLogging() {
	(LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("notprivacysafe.graphql.execution.SimpleDataFetcherExceptionHandler").level =
		Level.ERROR
}

private fun startKtorServer(dataProvider: IBlowAccessor, resourceProvider: IBlowResourceProvider) {
	mainLogger.info("Backend Port: ${explodeConfig.port}")
	mainLogger.info("GraphQl PlayGround: ${explodeConfig.enablePlayground}")
	mainLogger.info("Done! (${Duration.between(theVeryBeginningTime, LocalDateTime.now()).toMillis()}ms)")

	embeddedServer(
		Netty,
		environment = applicationEngineEnvironment {
			module {
				graphQLModule(dataProvider, explodeConfig.enablePlayground)
				dynamiteResourceModule(resourceProvider)
				bombModule(dataProvider, resourceProvider)
			}

			connector {
				port = explodeConfig.port
			}
		}
	).start(true)
}

val globalJson = Json {
	ignoreUnknownKeys = true
	serializersModule = SerializersModule {
		contextual(OffsetDateTimeSerializer)
	}
}

const val GameVersion = 81

/**
 * Read the prepared data in resource.
 */
private val ExplodeInfo by lazy {
	val cl = object {}.javaClass.classLoader
	val jsonContent = cl.getResource("explode.json")?.readText() ?: "{}"
	Json.decodeFromString<Map<String, String>>(jsonContent)
}