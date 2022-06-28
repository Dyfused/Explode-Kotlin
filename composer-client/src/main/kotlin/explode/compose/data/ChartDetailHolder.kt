package explode.compose.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class ChartDetailHolder {
    var chartFile: File? by mutableStateOf(null)
    var difficultyBase: Int? by mutableStateOf(null)
    var difficultyValue: Int? by mutableStateOf(null)

    companion object {
        fun of(difficultyBase: Int) = ChartDetailHolder().apply { this.difficultyBase = difficultyBase }
    }
}