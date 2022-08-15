package explode.mirai.command

import explode.dataprovider.model.database.MongoReviewResult
import explode.dataprovider.model.database.SetStatus
import explode.mirai.*
import net.mamoe.mirai.console.command.*

private const val HelpText = "审核曲目：\n" +
		"/review add <曲目ID> <审核状态：Accept/Reject> [审核信息（拒绝必须拥有拒绝原因）]\n" +
		"/review info <曲目ID>\n" +
		"/review start <曲目ID> <预期过审状态：Ranked/Unranked/Official>\n" +
		"/review end <曲目ID> <指定过审状态：Accept/Reject>\n" +
		"/review list"

object ReviewCommand : CompositeCommand(
	ExplodeMiraiPlugin,
	"review",
	"rw",
	description = HelpText
) {

	@SubCommand("help")
	suspend fun CommandSender.help() {
		sendMessage(HelpText)
	}

	@SubCommand("add")
	suspend fun CommandSender.add(setId: String, statusStr: String, messageStr: String? = null) {
		if(this is AbstractUserCommandSender) {
			val u = ExplodeBind.getMongoUserOrNull(user.id)
			if(u == null) {
				sendMessage("无法查询身份信息")
				return
			}

			if(!u.permission.review) {
				sendMessage("无权审核曲目")
				return
			}

			val set = Explode.getSet(setId)
			if(set == null) {
				sendMessage("曲目信息错误：ID")
				return
			}

			with(Explode) {
				val rv = set.getReview()
				if(rv == null) {
					sendMessage("曲目信息错误：无需审核或尚未开始审核")
					return
				}

				val status = when(statusStr.lowercase()) {
					"accept", "true" -> true
					"reject", "false" -> false
					else -> {
						sendMessage("参数错误：审核状态")
						return
					}
				}
				val evaluation = messageStr.orEmpty()

				runCatching {
					set.addReviewResult(MongoReviewResult(u._id, status, evaluation))
				}.onSuccess {
					sendMessage("已提交审核")
				}.onFailure {
					sendMessage("提交审核错误：${it.message}")
				}
			}
		}
	}

	@SubCommand("info")
	suspend fun CommandSender.info(setId: String) {
		if(this is AbstractUserCommandSender) {
			val u = ExplodeBind.getMongoUserOrNull(user.id)
			if(u == null) {
				sendMessage("无法查询身份信息")
				return
			}

			if(!u.permission.review) {
				sendMessage("无权审核曲目")
				return
			}

			val set = Explode.getSet(setId)
			if(set == null) {
				sendMessage("曲目信息错误：ID")
				return
			}

			with(Explode) {
				val rv = set.getReview()
				if(rv == null) {
					sendMessage("曲目信息错误：无需审核或尚未开始审核")
					return
				}

				val noterName = Explode.getUser(set.noterId)?.username ?: "错误的上传人员信息：${set.noterId}"
				val introText = set.introduction?.takeIf { it.isNotBlank() }?.let {
					"谱面介绍：\n" + it.replace("\\n", "\n")
				} ?: "无谱面介绍"
				val setInfoText = """
					<${set.musicName}>
					（${set._id}）
					谱面作者（上传者）：${noterName}
					$introText
					曲目价格：${set.price}
				""".trimIndent()

				val reviewText = "已审核数量：${rv.reviews.size}\n" + rv.reviews.joinToString(separator = "\n") {
					val username = Explode.getUser(it.reviewerId)?.username ?: "错误的审核人员信息：${it.reviewerId}"
					val resultText = if(it.status) "通过" else "拒绝"
					val evaluationSuffix = if(it.evaluation.isEmpty()) "" else "：${it.evaluation}"
					"$username（$resultText）$evaluationSuffix"
				}

				sendMessage(setInfoText + "\n" + reviewText)
			}
		}
	}

	@SubCommand("start")
	suspend fun CommandSender.start(setId: String, expectStatusStr: String) {
		if(this is AbstractUserCommandSender) {
			val u = ExplodeBind.getMongoUserOrNull(user.id)
			if(u == null) {
				sendMessage("无法查询身份信息")
				return
			}

			if(!u.permission.review) {
				sendMessage("无权审核曲目")
				return
			}

			val set = Explode.getSet(setId)
			if(set == null) {
				sendMessage("曲目信息错误：ID")
				return
			}

			with(Explode) {
				val rv = set.getReview()
				if(rv != null) {
					sendMessage("曲目信息错误：已经开始审核")
					return
				}

				val expectStatus = SetStatus.values().firstOrNull { it.name.lowercase() == expectStatusStr.lowercase() }
				if(expectStatus == null) {
					sendMessage("参数错误：预期过审状态")
					return
				}

				set.startReview(expectStatus)
				sendMessage("已开启审核：${set.musicName}（${set._id}）")
			}
		}
	}

	@SubCommand("end")
	suspend fun CommandSender.end(setId: String, forcedStatusStr: String) {
		if(this is AbstractUserCommandSender) {
			val u = ExplodeBind.getMongoUserOrNull(user.id)
			if(u == null) {
				sendMessage("无法查询身份信息")
				return
			}

			if(!u.permission.review) {
				sendMessage("无权审核曲目")
				return
			}

			val set = Explode.getSet(setId)
			if(set == null) {
				sendMessage("曲目信息错误：ID")
				return
			}

			with(Explode) {
				val rv = set.getReview()
				if(rv == null) {
					sendMessage("曲目信息错误：无需审核或尚未开始审核")
					return
				}

				val forcedReviewResult = when(forcedStatusStr.lowercase()) {
					"accept", "true" -> true
					"reject", "false" -> false
					else -> {
						sendMessage("参数错误：指定过审状态")
						return
					}
				}

				val resultText = if(forcedReviewResult) "通过" else "拒绝"

				set.endReview(forcedReviewResult)
				sendMessage("已结束审核：${set.musicName}（${set._id}），结果为：$resultText")
			}
		}
	}

	@SubCommand("list")
	suspend fun CommandSender.list() {
		if(this is AbstractUserCommandSender) {
			val u = ExplodeBind.getMongoUserOrNull(user.id)
			if(u == null) {
				sendMessage("无法查询身份信息")
				return
			}

			if(!u.permission.review) {
				sendMessage("无权审核曲目")
				return
			}

			val reviews = Explode.getReviewList()
			val count = reviews.count()
			val messageText = if(count == 0) {
				"目前没有正在审核的曲目"
			} else {
				"正在审核的曲目有${count}个：\n" + reviews.joinToString(separator = "\n") {
					val s = Explode.getSet(it.reviewedSet)
					if(s == null) {
						"错误的审核信息，无法找到曲目：${it.reviewedSet}"
					} else {
						val n = Explode.getUser(s.noterId)?.username ?: "曲目数据错误，无效用户ID：${s.noterId}"
						"<${s.musicName}>（${s._id}） by $n"
					}
				}
			}
			sendMessage(messageText)
		}
	}

}