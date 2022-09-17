package explode.backend

import explode.dataprovider.model.database.SetStatus
import explode.dataprovider.provider.IBlowAccessor
import explode.pack.v0.MetaReader
import explode.pack.v0.MetaUtil
import java.io.File
import javax.swing.JOptionPane

internal fun IBlowAccessor.importExplodePack(packMetaFile: File): Result<Nothing?> {
	check(packMetaFile.isFile) { "$packMetaFile is not a file" }

	val packMeta = MetaReader.readPackMetaJson(packMetaFile)
	val packFolder = packMetaFile.parentFile.resolve(packMeta.relativeFolderPath)

	val validation = MetaUtil.validateFiles(packMeta, packFolder)
	if(validation.isNotEmpty()) {
		return Result.failure(IllegalStateException(validation.joinToString()))
	}

	packMeta.sets.forEach { set ->
		runCatching {
			this.buildChartSet(
				setTitle = set.musicName,
				composerName = set.composerName,
				noterUser = this.getUserByName(set.noterName) ?: this.serverUser,
				coinPrice = 0,
				introduction = set.introduction ?: "",
				needReview = false,
				defaultId = set.id,
				expectStatus = SetStatus.UNRANKED,
				musicContent = packFolder.resolve(set.musicPath).readBytes(),
				previewMusicContent = packFolder.resolve(set.previewMusicPath).readBytes(),
				setCoverContent = packFolder.resolve(set.coverPicturePath).readBytes(),
				storePreviewContent = set.storePreviewPicturePath?.let { packFolder.resolve(it).readBytes() }
			) {
				set.charts.forEach { chart ->
					addChart(chart.difficultyClass, chart.difficultyValue, chart.DValue, chart.id, packFolder.resolve(chart.chartPath).readBytes())
				}
			}
		}.onFailure {
			JOptionPane.showMessageDialog(
				null, "Error occurred when uploading ${set.musicName}(${set.noterName}): $it. Skip and continue."
			)
		}
	}

	return Result.success(null)
}