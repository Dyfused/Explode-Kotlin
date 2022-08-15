package explode.mirai.command

import explode.dataprovider.model.database.MongoChart
import explode.mirai.*
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand

object FindChartCommand : SimpleCommand(
	ExplodeMiraiPlugin,
	"chart", "chart-find",
	description = "查询谱面：/chart <谱面信息>"
) {

	@Handler
	suspend fun CommandSender.handle(searchInfo: String) {
		val charts = getChartListBySearchInfo(searchInfo)

		when(charts.size) {
			0 -> {
				sendMessage("没有查询到匹配的谱面。")
			}

			1 -> {
				val chart = charts.single()
				val set = with(Explode) { chart.getParentSet() }
				val message = """
					谱面
					（${chart._id}）
					难度：${StringifyHelper.getSimpleHardness(chart)}
					定值：${chart.D}
					包含于：《${set.musicName}》（${set._id}）
				""".trimIndent()
				sendMessage(message)
				putContext(chart)
			}

			else -> {
				val message = "匹配到多个谱面\n" + charts.joinToString(separator = "\n") {
					"<${it._id}> ${StringifyHelper.getSimpleHardness(it)}"
				}
				sendMessage(message)
				putContext(charts)
			}
		}
	}

}

object ManageChartCommand : SimpleCommand(
	ExplodeMiraiPlugin,
	"chart-manage", "chart-m",
	description = "管理谱面：/chart-manage <谱面信息> <修改字段：d> <新值>"
) {

	@Handler
	suspend fun CommandSender.handle(searchInfo: String, propertyName: String, propertyValue: String) {
		val charts = getChartListBySearchInfo(searchInfo)

		when(charts.size) {
			0 -> {
				sendMessage("没有查询到匹配的谱面。")
			}

			1 -> {
				val chart = charts.single()
				val usr = ExplodeBind.getMongoUserOrNull(this)

				if(usr == null) {
					sendMessage("请先绑定用户")
					return
				}

				when(propertyName.lowercase()) {
					"d" -> {
						val newD = propertyValue.toDoubleOrNull()
						if(newD == null) {
							sendMessage("无效的值：${propertyValue}")
							return
						}
						chart.D = newD
						Explode.updateChart(chart)
						sendMessage("操作成功")
					}

					else -> {
						sendMessage("无效的字段：${propertyName}")
					}
				}
			}

			else -> { // 不太可能发生的东西
				val message = "匹配到多个谱面\n" + charts.joinToString(separator = "\n") {
					"<${it._id}> ${StringifyHelper.getSimpleHardness(it)}"
				}
				sendMessage(message)
			}
		}
	}


}

private fun CommandSender.getChartListBySearchInfo(searchInfo: String): List<MongoChart> =
	parseContext(searchInfo).toActual(Explode::getChart)