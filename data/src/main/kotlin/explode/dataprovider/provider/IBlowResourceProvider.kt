package explode.dataprovider.provider

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

interface IBlowFileResourceProvider : IBlowResourceProvider {
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

	fun getChartFile(chartId: String?): File?

	fun getMusicFile(setId: String?): File?

	fun getPreviewFile(setId: String?): File?

	fun getSetCoverFile(setId: String?): File?

	fun getStorePreviewFile(setId: String?): File?

	fun getUserAvatarFile(userId: String?): File?

	fun addChartFile(chartId: String, data: ByteArray)

	fun addMusicFile(setId: String, data: ByteArray)

	fun addPreviewFile(setId: String, data: ByteArray)

	fun addSetCoverFile(setId: String, data: ByteArray)

	fun addStorePreviewFile(setId: String, data: ByteArray)

	fun addUserAvatarFile(userId: String, data: ByteArray)
}

class BlowFileResourceProvider(
	private val dataBin: File,

	private val defaultUserAvatar: String? = null,
	private val defaultStorePreview: String? = null
) : IBlowFileResourceProvider {

	private fun File.ensureExistanceAsFolder() = apply {
		if(!this.exists() || !this.isDirectory) {
			if(!this.isDirectory) this.delete()
			if(!this.exists()) this.mkdirs()
		}
	}

	init {
		dataBin.ensureExistanceAsFolder()
	}

	override fun toString(): String = "BlowFileResourceProvider[${dataBin.absolutePath}]"

	private val chartFolder = dataBin.resolve("chart").ensureExistanceAsFolder()
	private val musicFolder = dataBin.resolve("music").ensureExistanceAsFolder()
	private val coverFolder = dataBin.resolve("cover").ensureExistanceAsFolder()
	private val avatarFolder = dataBin.resolve("avatar").ensureExistanceAsFolder()
	private val storePreviewFolder = dataBin.resolve("store_preview").ensureExistanceAsFolder()

	private fun File.resolveExists(relative: String): File? {
		val f = this.resolve(relative)
		return f.takeIf { f.exists() }
	}

	override fun getChartFile(chartId: String?): File? =
		chartId?.let { chartFolder.resolveExists("$chartId.xml") }

	override fun getMusicFile(setId: String?): File? =
		setId?.let { musicFolder.resolveExists("$setId.mp3") }

	override fun getPreviewFile(setId: String?): File? =
		setId?.let { musicFolder.resolveExists("${setId}_preview.mp3") }

	override fun getSetCoverFile(setId: String?): File? =
		setId?.let { coverFolder.resolveExists("$setId.jpg") }

	override fun getStorePreviewFile(setId: String?): File? =
		setId?.let { storePreviewFolder.resolveExists("$setId.jpg") } ?: defaultStorePreview?.let { storePreviewFolder.resolveExists("$defaultStorePreview.jpg") }

	override fun getUserAvatarFile(userId: String?): File? =
		userId?.let { avatarFolder.resolveExists("$userId.jpg") } ?: defaultUserAvatar?.let { avatarFolder.resolveExists("$defaultUserAvatar.jpg") }

	override fun addChartFile(chartId: String, data: ByteArray) {
		chartFolder.resolve("$chartId.xml").writeBytes(data)
	}

	override fun addMusicFile(setId: String, data: ByteArray) {
		musicFolder.resolve("$setId.mp3").writeBytes(data)
	}

	override fun addPreviewFile(setId: String, data: ByteArray) {
		musicFolder.resolve("${setId}_preview.mp3").writeBytes(data)
	}

	override fun addSetCoverFile(setId: String, data: ByteArray) {
		coverFolder.resolve("$setId.jpg").writeBytes(data)
	}

	override fun addStorePreviewFile(setId: String, data: ByteArray) {
		storePreviewFolder.resolve("$setId.jpg").writeBytes(data)
	}

	override fun addUserAvatarFile(userId: String, data: ByteArray) {
		avatarFolder.resolve("$userId.jpg").writeBytes(data)
	}

}