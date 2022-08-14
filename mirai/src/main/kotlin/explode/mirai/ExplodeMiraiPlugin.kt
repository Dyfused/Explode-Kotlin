package explode.mirai

import explode.mirai.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object ExplodeMiraiPlugin : KotlinPlugin(
	JvmPluginDescription(
		id = "explode.mirai",
		version = "1.0",
		name = "Explode Mirai"
	) {
		author("Taskeren")
	}
) {

	override fun onEnable() {
		logger.info("Explode Mirai Enabled.")

		ExplodeMiraiData.reload()

		RCalcCommand.register()
		FindSetCommand.register()
		Best20Command.register()
		BindingCommand.register()
		ReviewCommand.register()
	}

}