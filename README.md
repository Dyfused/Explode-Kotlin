# Explode 爆炸

Explode（爆炸）是已经歇逼的音游 Dynamite（炸药） 的私服计划。因为本人开发技术力有限，进度正在缓慢向上攀爬。

## 技术上的东西

炸药的后端采用的是 GraphQL，所以爆炸使用 Ktor + KotlinGraphQL 作为核心，配合 MongoDB 作为数据库开发。

## 子模块（Submodules）

- `dataprovider` 游戏数据管理
- `graphql-server` 游戏服务器后端