package explode.mirai

import explode.dataprovider.provider.mongo.MongoProvider
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("Explode-Mirai")
val Explode = MongoProvider()

@OptIn(ConsoleExperimentalApi::class)
fun main() {
	logger.info("Explode Mirai")
	MiraiConsoleTerminalLoader.startAsDaemon()
	ExplodeMiraiPlugin.load()
	ExplodeMiraiPlugin.enable()
}