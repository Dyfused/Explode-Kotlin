package explode.mirai.command

import explode.mirai.*
import net.mamoe.mirai.console.command.*

object BindingCommand : SimpleCommand(
	ExplodeMiraiPlugin,
	"bind",
	"绑定",
	description = "绑定游戏账号：/bind <账号名称>"
) {

	@Handler
	suspend fun CommandSender.bind(username: String) {
		val u = Explode.getUserByName(username)
		if(u == null) {
			sendMessage("找不到用户")
			return
		}

		if(this is AbstractUserCommandSender) {
			val qid = user.id
			ExplodeBind.unbind(qid)
			ExplodeBind.bind(qid, u.id)
			sendMessage("绑定成功")
		} else {
			sendMessage("不支持的环境")
		}
	}

}

object MeCommand : SimpleCommand(
	ExplodeMiraiPlugin,
	"me",
	"我",
	description = "查询绑定的游戏账号"
) {

	@Handler
	suspend fun CommandSender.handle() {
		val usr = ExplodeBind.getMongoUserOrNull(this)
		if(usr == null) {
			sendMessage("尚未绑定账号")
		} else {
			val message = """
				${usr.username}
				（${usr.id}）
				R：${usr.R}
				金币：${usr.coin}
			""".trimIndent()
			sendMessage(message)
			putContext(usr)
		}
	}

}