package explode

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

/**
 * set to 'true' for following behaviors:
 *   - print every GraphQL request body
 */
const val DebugMode = true

const val GameVersion = 81