package explode.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import explode.compose.data.StoreDataProvider
import explode.compose.theme.ExplodeColor
import explode.dataprovider.model.SetModel
import kotlin.math.roundToInt

@Composable
fun EditChartPage() {
    val offsetPercentage by animateFloatAsState(if (ExplodeViewModel.onEditingPage) 0F else 1F)

    Box(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative((placeable.width * offsetPercentage).roundToInt(), 0)
                }
            }
            .fillMaxSize()
            .background(Color.White)
    ) {
        val chartSet = ExplodeViewModel.currentEditingSet

        Column {
            // TopBar
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { ExplodeViewModel.setEditingChartSet(null) }
                ) {
                    Icon(Icons.Default.ArrowBack, "Back to Store")
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    text = chartSet?.musicTitle ?: "Error: Chart Set unset.",
                    fontWeight = FontWeight.Bold
                )
            }

            if (chartSet == null) {
                Box(
                    Modifier.fillMaxSize().align(Alignment.CenterHorizontally)
                ) {
                    Text("Please choose the Chart in the Store first. This can be a bug if you see this text.")
                }
            } else {
                ActualMetaEditing(chartSet)
            }
        }
    }
}

@Composable
fun ActualMetaEditing(chartSet: SetModel) {

    var title by remember { mutableStateOf(chartSet.musicTitle) }
    var composerName by remember { mutableStateOf(chartSet.composerName) }
    var introduction by remember { mutableStateOf(chartSet.introduction.replace("\\n", "\n")) }
    var price by remember { mutableStateOf(chartSet.coinPrice.toString()) }

    var isRanked by remember { mutableStateOf(chartSet.isRanked) }
    var isUnranked by remember { mutableStateOf(!chartSet.isRanked) }
    var isOfficial by remember { mutableStateOf(chartSet.isOfficial) }
    var isNeedReview by remember { mutableStateOf(chartSet.needReview) }
    var isHidden by remember { mutableStateOf(chartSet.coinPrice == -1) }

    var operationResult by remember { mutableStateOf(true) }
    var operationResultMessage by remember { mutableStateOf("") }

    Column(
        Modifier.padding(12.dp)
    ) {

        val fillMaxWidthModifier = Modifier.fillMaxWidth()

        OutlinedTextField(title, { title = it }, label = {
            Text("Title")
        }, singleLine = true, modifier = fillMaxWidthModifier)

        OutlinedTextField(composerName, { composerName = it }, label = {
            Text("Composer")
        }, singleLine = true, modifier = fillMaxWidthModifier)

        OutlinedTextField(introduction, { introduction = it }, label = {
            Text("Introduction")
        }, maxLines = 5, modifier = fillMaxWidthModifier)

        var priceError by remember { mutableStateOf(false) }
        OutlinedTextField(price, { price = it; priceError = it.toIntOrNull() == null; isHidden = -1 == it.toIntOrNull() }, label = {
            Text("Coin Price")
        }, singleLine = true, modifier = fillMaxWidthModifier, isError = priceError)

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(isRanked, { isRanked = it; if(it) isUnranked = false else isOfficial = false })
            Text("Ranked")
            Spacer(Modifier.size(4.dp))
            Checkbox(isUnranked, { isUnranked = it; if(it) { isRanked = false; isOfficial = false } })
            Text("Unranked")
            Spacer(Modifier.size(4.dp))
            Checkbox(isOfficial, { isOfficial = it; isRanked = true; isUnranked = false })
            Text("Official")
            Spacer(Modifier.size(4.dp))
            Checkbox(isNeedReview, { isNeedReview = it })
            Text("Need Review")
            Spacer(Modifier.size(4.dp))
            Checkbox(isHidden, { isHidden = it; price = "-1" })
            Text("Hidden")
        }

        Divider()

        Button(
            onClick = {
                chartSet.musicTitle = title
                chartSet.composerName = composerName
                chartSet.introduction = introduction.replace("\n", "\\n")
                chartSet.isRanked = isRanked
                chartSet.isOfficial = isOfficial
                chartSet.needReview = isNeedReview
                chartSet.coinPrice = (price.toIntOrNull() ?: 0).takeUnless { isHidden } ?: -1
                runCatching {
                    StoreDataProvider.p.updateSet(chartSet)
                }.onSuccess {
                    operationResult = true
                    operationResultMessage = "Successfully updated"
                }.onFailure {
                    operationResult = false
                    operationResultMessage = "Failed due to ${it.message} (${it.javaClass.simpleName})"
                    it.printStackTrace()
                }
            }
        ) {
            Text("Update")
        }

        Text(
            text = operationResultMessage,
            color = if(operationResult) ExplodeColor.DarkGreen else Color.Red
        )
    }

}