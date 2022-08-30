package explode.backend.bomb.v1.backend.model

import explode.dataprovider.model.database.SetStatus
import kotlinx.serialization.Serializable

@Serializable
data class UploadSetRequest(
	val title: String,
	val composerName: String,

	val chartMeta: List<UploadChartRequest>,

	val musicFileName: String,
	val coverFileName: String,
	val previewFileName: String,

	val storePreviewFileName: String? = null,

	val coinPrice: Int? = null,
	val defaultId: String? = null,
	val introduction: String? = null,
	val startReview: Boolean? = null,
	val expectedStatus: SetStatus? = null,
	val noterDisplayOverride: String? = null,
)

@Serializable
data class UploadChartRequest(
	val chartFileName: String,

	val chartDifficultyClass: Int,
	val chartDifficultyValue: Int,

	val D: Double? = null,
	val defaultId: String? = null,
)