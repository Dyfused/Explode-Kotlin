package explode.mirai

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object ExplodeMiraiData : AutoSavePluginData("Explode-General") {

	val idBinding: MutableMap<Long, String> by value()

}