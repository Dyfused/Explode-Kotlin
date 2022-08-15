package explode.mirai.command

import explode.mirai.ExplodeMiraiPlugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand

object ContextPeekCommand : SimpleCommand(
	ExplodeMiraiPlugin,
	"context-peek", "c-peek", "context-p"
) {

	@Handler
	suspend fun CommandSender.handleEmpty() {
		sendMessage("Context内容为：\n${this.getContext()}")
	}

	@Handler
	suspend fun CommandSender.handle(str: String) {
		val serializedContent = when(val r = parseContext(str)) {
			is ContextEmpty -> "Empty"
			is ContextID -> "ID(${r.id})"
			is ContextName -> "Name(${r.name})"
			is ContextValue<*> -> "Value[${r.value.javaClass.simpleName}]\n${r.value}"
		}
		sendMessage("上下文内容为：\n${this.getContext()}")
		sendMessage("解析后的内容为：\n${serializedContent}")
	}

}

object ContextSetCommand : SimpleCommand(
	ExplodeMiraiPlugin,
	"context-set", "c-set", "context-s"
) {

	@Handler
	suspend fun CommandSender.handle(str: String) {
		sendMessage("上下文内容为：\n${this.getContext()}")
		val serializedContent =
			when(val r = parseContext(str)) {
				is ContextEmpty -> {
					clearContext()
					"Empty"
				}

				is ContextID -> {
					putContext(r.id)
					"ID(${r.id})"
				}

				is ContextName -> {
					putContext(r.name)
					"Name(${r.name})"
				}

				is ContextValue<*> -> {
					putContext(r.value)
					"Value[${r.value.javaClass.simpleName}]\n${r.value}"
				}
			}
		sendMessage("解析后的内容为：\n${serializedContent}")
		sendMessage("已将上下文更新为解析后内容")
	}

}

//

inline fun <reified T : Any> ContextResult.toActual(
	getById: (String) -> T?,
	getByName: (String) -> List<T> = { emptyList() }
): List<T> =
	when(this) {
		is ContextEmpty -> emptyList()
		is ContextID -> listOfNotNull(getById(id))
		is ContextName -> getByName(name)
		is ContextValue<*> ->
			when(value) {
				is List<*> -> value.filterIsInstance<T>()
				is T -> listOfNotNull(value)
				else -> emptyList()
			}
	}