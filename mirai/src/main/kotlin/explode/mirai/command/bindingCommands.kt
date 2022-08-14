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
			ExplodeBind.bind(qid, u._id)
			sendMessage("绑定成功")
		} else {
			sendMessage("不支持的环境")
		}
	}

}