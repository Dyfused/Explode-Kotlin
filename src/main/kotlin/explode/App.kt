package explode

import explode.blow.provider.IBlowProvider
import explode.blow.provider.mongo.MongoProvider
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val mainLogger = LoggerFactory.getLogger("Explode")

fun main(args: Array<String>) {
	mainLogger.info("Explode ($GameVersion)")

	Json {
		ignoreUnknownKeys = true
	}

	EngineMain.main(args)

	mainLogger.info("Exploded.")
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
val blow: IBlowProvider get() = MongoProvider()