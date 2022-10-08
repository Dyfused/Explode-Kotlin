# Explode 爆炸

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/Dyfused/Explode-Kotlin)](https://github.com/Dyfused/Explode-Kotlin/releases/latest)
[![Issues](https://img.shields.io/github/issues/Dyfused/Explode-Kotlin)](https://github.com/Dyfused/Explode-Kotlin/issues)
[![GitHub](https://img.shields.io/github/license/Dyfused/Explode-Kotlin)](https://github.com/Dyfused/Explode-Kotlin/blob/master/LICENSE)
[![Create Explosion](https://github.com/Dyfused/Explode-Kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/Dyfused/Explode-Kotlin/actions/workflows/build.yml)

![](/docs/icon.gif)

Explode（爆炸）是已经歇逼的音游 Dynamite（炸药） 的私服计划。因为本人开发技术力有限，进度正在缓慢向上攀爬。

## 使用

[安装与使用](https://github.com/Dyfused/Explode-Kotlin/wiki/Installation-CN)

如果遇到其他未在安装与使用中说明的问题，你可以在 [Discussions](https://github.com/Dyfused/Explode-Kotlin/discussions) 中新建讨论，或者加群 [746464090](https://jq.qq.com/?_wv=1027&k=KgGAtymy) 获得帮助。

## 技术上的东西

炸药的后端采用的是 GraphQL，所以爆炸使用 Ktor + KotlinGraphQL 作为核心，配合 MongoDB 作为数据库开发。

## 子模块 (Submodules)

- `data` 游戏数据管理库
- `server` 游戏服务器后端
- `mirai` Mirai 机器人

## 提供帮助 (Contributing)

请在 Discussions 里与我们联系。

## 鸣谢

爆炸使用了很多大佬的成果，在此列出鸣谢：

在复现 R 值相关的数据处理时引用了以下三位大佬的专栏。

其中 DraXon 的结论被用在谱面定值（D）到谱面最大R值（RMax）的计算中。
KesdiaelKen 和 Crazy_Bull 的结论分别使用在了两套R值计算体系中，可以在配置中选择。

- DraXon 的 [关于Dynamite中Rank谱面定数D与其最大R值的拟合关系](https://www.bilibili.com/read/cv17024921)
- Crazy_Bull 的 [Dynamite R值机制的相关研究（一）](https://www.bilibili.com/read/cv16847763)
- KesdiaelKen 的 [dynamite的R值计算的一点猜想](https://www.bilibili.com/read/cv4890428)

在游戏客户端的修改方面， 感谢 *扎啤避难所* 的群友对爆炸的支持，包括但不限于提供测试，提供修改 global-meta 的方法，和提供绕过加密与解密的方法。

尤其感谢某位没有姓名的群主（　）使用并提交了若干与爆炸相关的问题，并提供测试。

**没有上面的这些佬就没有现在的爆炸。**
