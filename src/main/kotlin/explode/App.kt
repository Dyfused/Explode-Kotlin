package explode

import explode.blow.provider.FakeBlowProvider
import explode.blow.provider.IBlowProvider
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val mainLogger = LoggerFactory.getLogger("Explode")

fun main(args: Array<String>) {
	mainLogger.info("Explode ($GameVersion)")

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
val blow: IBlowProvider get() = FakeBlowProvider