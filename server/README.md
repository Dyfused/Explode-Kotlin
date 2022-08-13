
### explode.blow

游戏后端的 GraphQL 抽象层

#### explode.blow.impl

使用 `dataprovider` 提供数据的后端 GraphQL 实现。

### explode.backend.graphql

使用 Ktor 实现的 GraphQL 服务器，数据来源于 `App.kt` 定义的 `blow`, `blowAccessor` 等字段。

### explode.backend.console

控制台交互，将被其他模块替代。

### explode.backend.bomb

为第三方应用预留的 API 接口，与游戏后端无关，仅用于数据库操作，如谱面上传等。