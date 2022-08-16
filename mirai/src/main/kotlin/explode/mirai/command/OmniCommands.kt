package explode.mirai.command

import explode.mirai.ExplodeMiraiPlugin
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand

object OmniStatusCommand : SimpleCommand(
	ExplodeMiraiPlugin, "omni-status", "omni-s"
) {

	private var explodeThread: Thread? = null
	private var miraiThread: Thread? = null

	private var init: Boolean = false

	fun init(explodeThread: Thread, miraiThread: Thread) {
		if(init) error("Don't initialize twice!")

		this.explodeThread = explodeThread
		this.miraiThread = miraiThread

		register()

		init = true
	}

	@Handler
	suspend fun CommandSender.handle() {
		val message = StringBuilder()

		if(explodeThread == null && miraiThread == null) {
			message += "查询错误，似乎没有注册相关线程，请确定 Omni 状态。"
		}

		explodeThread?.let {
			message += "服务器线程运行状态：${it.isAlive} [${it.state}]"
		}
		miraiThread?.let {
			message += "机器人线程运行状态：${it.isAlive} [${it.state}]"
		}

		sendMessage(message.toString())
	}

	operator fun StringBuilder.plusAssign(str: String) {
		appendLine(str)
	}

}