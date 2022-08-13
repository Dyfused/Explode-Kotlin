package explode.mirai.command

import explode.mirai.Explode
import explode.mirai.ExplodeMiraiPlugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand

object Best20Command : SimpleCommand(
	ExplodeMiraiPlugin,
	"Best20",
	"b20", "best20r",
	description = "/Best20 <用户名称>"
) {

	@Handler
	suspend fun CommandSender.byUsername(username: String) {
		val u = Explode.getUserByName(username)
		if(u == null) {
			sendMessage("无法找到用户")
			return
		}
		val b20 = Explode.getUserBestR20(u._id)
		val b20Message = b20.joinToString(separator = "\n") {
			val musicName = with(Explode) {
				getChart(it.chartId)?.getParentSet()?.musicName
					?: "未知曲目，这是一个错误的结果，请汇报给管理员。(chartId=${it.chartId})"
			}
			val (perfect, good, miss) = it.scoreDetail
			"$musicName：${it.RScore} (${perfect}/${good}/${miss})"
		}
		sendMessage("<${u.username}> 的生涯最佳\n$b20Message")
	}

}