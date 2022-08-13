import explode.pack.v0.MetaUtil
import explode.pack.v0.RenaReader
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import javax.swing.JOptionPane

fun main() {

	val renaPath = JOptionPane.showInputDialog("Rena File Path")
	val renaFile = File(renaPath)

	var startTiming = LocalDateTime.now()
	val packMeta = RenaReader.parse(renaFile.readLines())
	val renaFolder = renaFile.parentFile.resolve(packMeta.relativeFolderPath)
	var endTiming = LocalDateTime.now()

	println(packMeta)
	println("Done (${Duration.between(startTiming, endTiming).toMillis()}ms)")

	startTiming = LocalDateTime.now()
	val validateResult = MetaUtil.validateFiles(packMeta, renaFolder)
	endTiming = LocalDateTime.now()
	if(validateResult.isEmpty()) {
		println("Nothing incorrect (${Duration.between(startTiming, endTiming).toMillis()}ms)")
	} else {
		validateResult.forEach {
			println("-> $it")
		}
		println("Done (${Duration.between(startTiming, endTiming).toMillis()}ms)")
	}

}
