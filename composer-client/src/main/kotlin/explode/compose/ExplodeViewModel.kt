package explode.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import explode.dataprovider.model.SetModel
import java.io.File

internal object ExplodeViewModel {

    var currentEditingSet: SetModel? by mutableStateOf(null)

    var onEditingPage by mutableStateOf(false)

    fun setEditingChartSet(chartSet: SetModel?) {
        println("Switching to $chartSet")

        currentEditingSet = chartSet
        onEditingPage = chartSet != null
    }

    var onCreatingPage by mutableStateOf(false)

    fun validateFile(f: File?, needFile: Boolean = true, extName: String? = null, onAccept: (File) -> Unit = {}, onReject: (File?) -> Unit = {}): Boolean {
        val validated = f != null && f.exists() && (!needFile || f.isFile) && (extName == null || f.endsWith(extName))
        if(validated) onAccept(f!!) else onReject(f)
        return validated
    }
}