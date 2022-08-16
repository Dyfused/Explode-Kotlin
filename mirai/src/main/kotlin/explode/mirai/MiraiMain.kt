package explode.mirai

import explode.dataprovider.provider.mongo.MongoProvider
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

val logger: Logger = LoggerFactory.getLogger("Explode-Mirai")
val Explode get() = provider

private var provider by Delegates.notNull<MongoProvider>()

/**
 * Started as an Application, using self-provided MongoProvider.
 */
fun main() {
	logger.info("Explode Mirai")
	bootstrap(MongoProvider())
}

/**
 * Started as a part of others, using provided MongoProvider.
 *
 * More in Omni module.
 */
@OptIn(ConsoleExperimentalApi::class)
fun bootstrap(provider: MongoProvider) {
	explode.mirai.provider = provider
	MiraiConsoleTerminalLoader.startAsDaemon()
	ExplodeMiraiPlugin.load()
	ExplodeMiraiPlugin.enable()
}