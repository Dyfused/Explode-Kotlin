@file:JvmName("Omni")

package explode.omni

import explode.dataprovider.provider.mongo.MongoProvider
import explode.mirai.OmniHelper
import kotlin.concurrent.thread

lateinit var ExplodeThread: Thread
lateinit var MiraiBotThread: Thread

fun main() {

	val p = MongoProvider()

	ExplodeThread = thread {
		explode.bootstrap(p)
	}

	MiraiBotThread = thread {
		explode.mirai.bootstrap(p)
	}

	// register status command for Mirai
	OmniHelper.initStatusCommand(ExplodeThread, MiraiBotThread)

}
