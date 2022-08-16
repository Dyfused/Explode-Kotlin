package explode.mirai

import explode.mirai.OmniHelper.thingsNeedToBeDoneOnBotInit
import explode.mirai.command.OmniStatusCommand

/**
 * [OmniHelper] is a wrapping helper used to avoid error of missing Mirai things in Omni module.
 *
 * [thingsNeedToBeDoneOnBotInit] will be invoked when [ExplodeMiraiPlugin] is ready to registration.
 */
object OmniHelper {

	private val thingsNeedToBeDoneOnBotInit = mutableListOf<() -> Unit>()

	internal fun onBotInit() {
		thingsNeedToBeDoneOnBotInit.forEach { it() }
	}

	fun initStatusCommand(explodeThread: Thread, miraiThread: Thread) {
		thingsNeedToBeDoneOnBotInit += {
			OmniStatusCommand.init(explodeThread, miraiThread)
		}
	}


}