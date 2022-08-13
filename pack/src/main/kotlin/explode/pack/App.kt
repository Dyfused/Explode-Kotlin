@file:JvmName("App")
package explode.pack

import explode.pack.v0.MetaWriter
import explode.pack.v0.RenaReader
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JOptionPane

val OpenMessage = """
Explode Pack Helper (v0)
${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}
""".trimIndent()

/**
 * Use Java command to use this like:
 * `java -cp datacollector.jar explode.pack.App`
 */
fun main(args: Array<String>) {
	println(OpenMessage)

	if("nogui" in args) {
		noGuiMode()
	} else {
		guiMode()
	}
}

private fun noGuiMode() {
	print("Rena File Path: ")
	val renaPath = readLine()
	if(renaPath == null) {
		println("We cannot help you if you don't give us the Rena.")
		return
	}
	val renaFile = File(renaPath)
	if(!renaFile.exists() || !renaFile.isFile) {
		println("Invalid Rena file as it can be non-regular file or just missing.")
		return
	}
	print("Rena Folder Path (relative to Rena file)[default: Charts]: ")
	val renaFolderPath = readLine() ?: "Charts"
	val renaFolder = renaFile.parentFile.resolve(renaFolderPath)
	if(!renaFolder.exists() || !renaFolder.isDirectory) {
		println("Invalid Rena folder as it can be non-regular directory or just missing.")
		return
	}
	val packMeta = RenaReader.parse(renaFile.readLines(), renaFolderPath)
	val metaFile = MetaWriter.writePackMetaJson(packMeta, renaFile.parentFile)
	println("Meta file exported at $metaFile.")
}

private fun guiMode() {
	val renaPath = JOptionPane.showInputDialog("Rena File Path: ")
	if(renaPath == null) {
		JOptionPane.showMessageDialog(null, "We cannot help you if you don't give us the Rena.")
		return
	}
	val renaFile = File(renaPath)
	if(!renaFile.exists() || !renaFile.isFile) {
		JOptionPane.showMessageDialog(null, "Invalid Rena file as it can be non-regular file or just missing.")
		return
	}
	val renaFolderPath = JOptionPane.showInputDialog("Rena Folder Path (relative to Rena file)[default: Charts]: ").takeIf { it.isNotBlank() } ?: "Charts"
	val renaFolder = renaFile.parentFile.resolve(renaFolderPath)
	if(!renaFolder.exists() || !renaFolder.isDirectory) {
		JOptionPane.showMessageDialog(null, "Invalid Rena folder as it can be non-regular directory or just missing.")
		return
	}
	val packMeta = RenaReader.parse(renaFile.readLines(), renaFolderPath)
	val metaFile = MetaWriter.writePackMetaJson(packMeta, renaFile.parentFile)
	JOptionPane.showMessageDialog(null, "Meta file exported at $metaFile.")
}