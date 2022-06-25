package explode.blow.provider

import java.io.File

interface IBlowResourceProvider {

	fun getChartResource(chartId: String?): ByteArray?

	fun getMusicResource(setId: String?): ByteArray?

	fun getPreviewResource(setId: String?): ByteArray?

	fun getSetCoverResource(setId: String?): ByteArray?

	/**
	 * Preview of chart in the Store
	 */
	fun getStorePreviewResource(setId: String?): ByteArray?

	fun getUserAvatarResource(userId: String?): ByteArray?

	fun addChartResource(chartId: String, data: ByteArray)

	fun addMusicResource(setId: String, data: ByteArray)

	fun addPreviewResource(setId: String, data: ByteArray)

	fun addSetCoverResource(setId: String, data: ByteArray)

	/**
	 * Preview of chart in the Store
	 */
	fun addStorePreviewResource(setId: String, data: ByteArray)

	fun addUserAvatarResource(userId: String, data: ByteArray)

}

abstract class BlowFileResourceProvider : IBlowResourceProvider {
	override fun getChartResource(chartId: String?): ByteArray? = getChartFile(chartId)?.readBytes()

	override fun getMusicResource(setId: String?): ByteArray? = getMusicFile(setId)?.readBytes()

	override fun getPreviewResource(setId: String?): ByteArray? = getPreviewFile(setId)?.readBytes()

	override fun getSetCoverResource(setId: String?): ByteArray? = getSetCoverFile(setId)?.readBytes()

	override fun getStorePreviewResource(setId: String?): ByteArray? = getStorePreviewFile(setId)?.readBytes()

	override fun getUserAvatarResource(userId: String?): ByteArray? = getUserAvatarFile(userId)?.readBytes()

	override fun addChartResource(chartId: String, data: ByteArray) = addChartFile(chartId, data)

	override fun addMusicResource(setId: String, data: ByteArray) = addMusicFile(setId, data)

	override fun addPreviewResource(setId: String, data: ByteArray) = addPreviewFile(setId, data)

	override fun addSetCoverResource(setId: String, data: ByteArray) = addSetCoverFile(setId, data)

	override fun addStorePreviewResource(setId: String, data: ByteArray) = addStorePreviewFile(setId, data)

	override fun addUserAvatarResource(userId: String, data: ByteArray) = addUserAvatarFile(userId, data)

	abstract fun getChartFile(chartId: String?): File?

	abstract fun getMusicFile(setId: String?): File?

	abstract fun getPreviewFile(setId: String?): File?

	abstract fun getSetCoverFile(setId: String?): File?

	abstract fun getStorePreviewFile(setId: String?): File?

	abstract fun getUserAvatarFile(userId: String?): File?

	abstract fun addChartFile(chartId: String, data: ByteArray)

	abstract fun addMusicFile(setId: String, data: ByteArray)

	abstract fun addPreviewFile(setId: String, data: ByteArray)

	abstract fun addSetCoverFile(setId: String, data: ByteArray)

	abstract fun addStorePreviewFile(setId: String, data: ByteArray)

	abstract fun addUserAvatarFile(userId: String, data: ByteArray)
}