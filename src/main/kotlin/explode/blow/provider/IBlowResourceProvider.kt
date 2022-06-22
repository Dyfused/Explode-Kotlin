package explode.blow.provider

import java.io.File

interface IBlowResourceProvider {

	/**
	 * 加密谱面文件
	 */
	fun getChartFile(chartId: String?): File?

	/**
	 * 加密音乐文件
	 */
	fun getMusicFile(setId: String?): File?

	/**
	 * 加密预览音乐文件
	 */
	fun getPreviewFile(setId: String?): File?

	/**
	 * 加密封面图文件
	 */
	fun getSetCoverFile(setId: String?): File?

	/**
	 * 商店预览图文件
	 */
	fun getStorePreviewFile(setId: String?): File?

	/**
	 * 用户头像文件
	 */
	fun getUserAvatarFile(userId: String?): File?

}