package explode.pack.v0

import kotlinx.serialization.Serializable

@Serializable
data class PackMeta(
	val id: String?,
	val author: String?,
	val sets: List<SetMeta>,
	val relativeFolderPath: String
)

@Serializable
data class SetMeta(
	val id: String?,
	val musicName: String,
	val composerName: String,
	val noterName: String,
	val introduction: String?,
	val musicPath: String,
	val previewMusicPath: String,
	val coverPicturePath: String,
	val storePreviewPicturePath: String?,
	val charts: List<ChartMeta>
)

@Serializable
data class ChartMeta(
	val id: String?,
	val difficultyClass: Int,
	val difficultyValue: Int,
	val DValue: Double?,
	val chartPath: String
)