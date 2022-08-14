package explode.mirai.command

import explode.dataprovider.model.database.MongoUser
import explode.mirai.*
import net.mamoe.mirai.console.command.*

object Best20Command : SimpleCommand(
	ExplodeMiraiPlugin,
	"best20",
	"b20", "best20r",
	description = "查询生涯最佳：/best20 <用户名称|自己>"
) {

	@Handler
	suspend fun UserCommandSender.byDefault() {
		val u = ExplodeBind.getDyId(user.id)?.let(Explode::getUser)
		if(u == null) {
			sendMessage("请先绑定游戏账号，使用“/bind <账号名称>”指令")
		} else {
			sendBest20(u)
		}
	}

	@Handler
	suspend fun CommandSender.byUsername(username: String) {
		val u = Explode.getUserByName(username)
		if(u == null) {
			sendMessage("无法找到用户")
		} else {
			sendBest20(u)
		}
	}

	private suspend fun CommandSender.sendBest20(u: MongoUser) {
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