package explode.mirai.command

import explode.dataprovider.model.database.MongoUser
import explode.mirai.*
import net.mamoe.mirai.console.command.*
import kotlin.math.roundToInt

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

	private suspend fun CommandSender.sendBest20(u: MongoUser) = with(Explode) {
		val b20 = u.getBestPlayRecordsR(20, 0) // Explode.getUserBestR20(u.id)
		val b20Message = b20.joinToString(separator = "\n") {
			val songInfo = with(Explode) {
				val ch = getChart(it.chartId)
				if(ch == null) {
					"未知曲目，这是一个错误的结果，请汇报给管理员。(chartId=${it.chartId})"
				} else {
					val musicName = ch.getParentSet().musicName
					val hardnessLevel = when(ch.difficultyClass) {
						1 -> "C"
						2 -> "N"
						3 -> "H"
						4 -> "M"
						5 -> "G"
						6 -> "T"
						else -> "U"
					}
					"<$musicName>@$hardnessLevel${ch.difficultyValue}"
				}
			}
			val (perfect, good, miss) = it.scoreDetail
			"$songInfo：${it.RScore?.roundToInt()}【${perfect}/${good}/${miss}】"
		}
		sendMessage("<${u.username}> 的生涯最佳\n$b20Message")
	}

}