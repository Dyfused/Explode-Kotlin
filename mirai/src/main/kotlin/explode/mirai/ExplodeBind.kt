package explode.mirai

import explode.dataprovider.model.database.MongoUser
import net.mamoe.mirai.console.command.CommandSender
import explode.mirai.ExplodeMiraiData.idBinding as binding

object ExplodeBind {

	fun bind(qqId: Long, dyId: String) {
		binding[qqId] = dyId
	}

	fun unbind(qqId: Long) {
		binding.remove(qqId)
	}

	fun unbind(dyId: String) {
		binding.filter { it.value == dyId }.map { it.key }.forEach(binding::remove)
	}

	fun getDyId(qqId: Long) = binding[qqId]
	fun getQQId(dyId: String) = binding.entries.firstOrNull { it.value == dyId }?.key

	fun getMongoUserOrNull(qqId: Long?): MongoUser? = qqId?.let(::getDyId)?.let(Explode::getUser)

	fun getMongoUserOrNull(sender: CommandSender): MongoUser? = getMongoUserOrNull(sender.user?.id)
}

var MongoUser.qqId: Long?
	get() = ExplodeBind.getQQId(id)
	set(value) { if(value != null) ExplodeBind.bind(value, id) else ExplodeBind.unbind(id) }