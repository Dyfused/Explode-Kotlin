# Explode 爆炸

Explode（爆炸）是已经歇逼的音游 Dynamite（炸药） 的私服计划。因为本人开发技术力有限，进度正在缓慢向上攀爬。

## 使用

[安装与使用](https://github.com/Dyfused/Explode-Kotlin/wiki/Installation-CN)

如果遇到其他未在安装与使用中说明的问题，你可以在 [Discussions](https://github.com/Dyfused/Explode-Kotlin/discussions) 中新建讨论，或者加群 [746464090](https://jq.qq.com/?_wv=1027&k=KgGAtymy) 获得帮助。

## 技术上的东西

炸药的后端采用的是 GraphQL，所以爆炸使用 Ktor + KotlinGraphQL 作为核心，配合 MongoDB 作为数据库开发。

## 子模块 (Submodules)

- `dataprovider` 游戏数据管理库
- `graphql-server` 游戏服务器后端
- `composer-client` 游戏数据管理客户端
- `rena` Rena 数据处理工具

## 提供帮助 (Contributing)

### 提供前端开发

目前开发人员的技术栈尚未包含到前端开发，所有前端网站的功能暂时被搁置，
如果您有能力帮助团队，我们欢迎您的加入！

*请在 Disscusions 中提交您的信息，以帮助我们联系到您！*

### 提供英文翻译 (Provide English translation)

我们需要将项目文档，如 README，Wiki 等内容，翻译成英文。

*请在 Disscusions 中提交您的信息，以帮助我们联系到您！*

We need to translate the document like README and Wiki into English.

*If you can help us, please let us know in Disscusions.*

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