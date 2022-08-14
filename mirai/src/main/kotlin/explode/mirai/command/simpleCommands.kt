package explode.mirai.command

import explode.mirai.ExplodeMiraiPlugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import thirdparty.crazy_bull.CrazyBullRCalculator
import kotlin.math.max
import kotlin.math.roundToInt

object RCalcCommand : SimpleCommand(
	ExplodeMiraiPlugin, "calc-r",
	"dy-r", "r",
	description = "/calc-r <D> <Acc> | /calc-r <D> <perfect> <good> <miss>"
) {

	init {
		CrazyBullRCalculator.init()
	}

	@Handler
	suspend fun CommandSender.handleAcc(d: Double, acc: Double) {
		if(acc > 1 || acc < 0) {
			sendMessage("不正确的 Acc 值：应该介于 [0, 1] 之间。")
			return
		}
		val r = max(CrazyBullRCalculator.eval(d, acc), 0.0).roundToInt()
		sendMessage("本次游玩R值为：$r")
	}

	@Handler
	suspend fun CommandSender.handleRec(d: Double, perfect: Int, good: Int, miss: Int) {
		val acc = (perfect + (good.toDouble() / 2)) / (perfect + good + miss)
		handleAcc(d, acc)
	}

}