package explode.compose.theme

import androidx.compose.ui.graphics.Color

object ExplodeColor {

    val Casual = Color(0xFF00C700)
    val Normal = Color(0xFF00C7B6)
    val Hard = Color(0xFFFF0000)
    val Mega = Color(0xFF800080)
    val Giga = Color(0xFFC0C0C0)

    fun getDifficultyBackgroundColor(difficulty: Int) = when(difficulty) {
        1 -> Casual
        2 -> Normal
        3 -> Hard
        4 -> Mega
        5 -> Giga
        else -> Color.Black
    }

    fun getDifficultyForegroundColor(difficulty: Int) = when(difficulty) {
        1, 2, 5 -> Color.Black
        3, 4 -> Color.White
        else -> Color.White
    }

    fun getDifficultyName(difficulty: Int) = when(difficulty) {
        1 -> "Casual"
        2 -> "Normal"
        3 -> "Hard"
        4 -> "Mega"
        5 -> "Giga"
        6 -> "Tera"
        else -> "Unknown"
    }

    // Minecraft Formatting Codes

    val Gold = Color(0xFFFFAA00)
    val Red = Color(0xFFFF5555)
    val DarkRed = Color(0xFFAA0000)
    val Purple = Color(0xFFAA00AA)
    val DarkPurple = Color(0xFFAA00AA)
    val Green = Color(0xFF55FF55)
    val DarkGreen = Color(0xFF00AA00)

}