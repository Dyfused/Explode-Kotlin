@file:Suppress("UNUSED_PARAMETER")

package explode.schema

import com.expediagroup.graphql.server.operations.Query
import explode.schema.model.SetModel

/**
 * 关于游戏下载部分
 *
 * 游戏会在下载前调用 setById 最后确认信息，然后从以下地址下载对应资源：
 *   - 音乐：https://dynamite.tunergames.com/download/music/encoded/{setId}
 *   - 封面：https://dynamite.tunergames.com/download/cover/encoded/{setId}
 *   - 预览：https://dynamite.tunergames.com/download/preview/encoded/{setId}
 *   - 铺面：https://dynamite.tunergames.com/download/chart/encoded/{chartId}
 */
object DownloadQueryService : Query {

	data class ExchangeSetModel(
		val coin: Int? // remaining coins after purchase
	)

	// TODO: Implement
	suspend fun setById(_id: String?): SetModel {
		return FakeSet
	}

	// TODO: Implement
	suspend fun exchangeSet(setId: String?): ExchangeSetModel {
		return ExchangeSetModel(null)
	}

}