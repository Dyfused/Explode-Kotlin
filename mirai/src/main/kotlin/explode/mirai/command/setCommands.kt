package explode.mirai.command

import explode.dataprovider.model.database.MongoSet
import explode.dataprovider.model.database.SetStatus
import explode.mirai.Explode
import explode.mirai.ExplodeMiraiPlugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import java.lang.ref.WeakReference

object FindSetCommand : CompositeCommand(
	ExplodeMiraiPlugin, "set",
	description = "管理曲目：/set <find|set> <曲目信息> [其他参数...]"
) {

	private val lastFind = mutableMapOf<Long?, WeakReference<MongoSet>>()

	@SubCommand("find")
	suspend fun CommandSender.find(searchInfo: String) {
		val sets = getSetListBySearchInfo(searchInfo)

		when(sets.size) {
			0 -> {
				sendMessage("没有查询到匹配的曲目。")
			}

			1 -> {
				val set = sets.single()
				val noterName = Explode.getUser(set.noterId)?.username ?: "无法找到作者"
				val introText = set.introduction?.takeIf { it.isNotBlank() }?.let {
					"谱面介绍：\n" + it.replace("\\n", "\n")
				} ?: "无谱面介绍"
				val message = """
						《${set.musicName}》
						（${set._id}）
						状态：${set.status.humanizedName}
						价格：${getPriceText(set.price)}
						作者名称：$noterName
						拥有难度：${set.getChartsSummary()}
					""".trimIndent() + "\n$introText"
				sendMessage(message)
				lastFind[user?.id] = WeakReference(set)
			}

			else -> {
				val message = "匹配到多条曲目\n" + sets.joinToString(separator = "\n") {
					"<${it._id}>：${it.getChartsSummary()}"
				}
				sendMessage(message)
			}
		}
	}

	@SubCommand("set")
	suspend fun CommandSender.set(searchInfo: String, propertyName: String, propertyValue: String) {
		val sets = getSetListBySearchInfo(searchInfo)

		when(sets.size) {
			0 -> {
				sendMessage("没有查询到匹配的曲目。")
			}

			1 -> {
				val set = sets.single()

				when(propertyName.lowercase()) {
					"coin", "price" -> {
						val newCoin = propertyValue.toIntOrNull()
						if(newCoin == null) {
							sendMessage("无效的值：${propertyValue}")
							return
						}
						set.price = newCoin
						Explode.updateSet(set)
						sendMessage("操作成功")
					}
					"status" -> {
						val newStatus = SetStatus.values().firstOrNull { it.name.lowercase() == propertyValue.lowercase() }
						if(newStatus == null) {
							sendMessage("无效的值：${propertyValue}")
							return
						}
						set.status = newStatus
						Explode.updateSet(set)
						sendMessage("操作成功")
					}
					else -> {
						sendMessage("无效的字段：${propertyName}")
					}
				}
			}

			else -> {
				val message = "匹配到多条曲目\n" + sets.joinToString(separator = "\n") {
					"<${it._id}>：${it.getChartsSummary()}"
				}
				sendMessage(message)
			}
		}
	}

	private fun CommandSender.getSetListBySearchInfo(searchInfo: String): List<MongoSet> =
		if(searchInfo.startsWith("#")) {
			val id = searchInfo.substring(1)
			listOfNotNull(Explode.getSet(id))
		} else if(searchInfo == "$") {
			listOfNotNull(lastFind[user?.id]?.get())
		} else {
			Explode.getSetByName(searchInfo).toList()
		}

	private fun getPriceText(price: Int): String = when(price) {
		0 -> "免费"
		else -> "$price 金币"
	}

	private fun getHardLevelText(level: Int): String = when(level) {
		1 -> "CASUAL"
		2 -> "NORMAL"
		3 -> "HARD"
		4 -> "MEGA"
		5 -> "GIGA"
		else -> "UNKNOWN"
	}

	private fun MongoSet.getChartsSummary(): String = with(Explode) {
		val charts = getCharts()
		return charts.joinToString(separator = "，") {
			"${getHardLevelText(it.difficultyClass)} ${it.difficultyValue}"
		}
	}

}