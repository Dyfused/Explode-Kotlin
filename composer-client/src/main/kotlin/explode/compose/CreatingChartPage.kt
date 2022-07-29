package explode.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import explode.compose.data.ChartDetailHolder
import explode.compose.data.StoreDataProvider
import explode.compose.theme.ExplodeColor
import explode.compose.theme.ExplodeColor.getDifficultyName
import java.awt.FileDialog
import java.io.File
import kotlin.math.roundToInt

@Composable
fun CreatingChartPage() {
    val offsetPercentage by animateFloatAsState(if (ExplodeViewModel.onCreatingPage) 0F else 1F)

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

        Column {
            // TopBar
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { ExplodeViewModel.onCreatingPage = false }
                ) {
                    Icon(Icons.Default.ArrowBack, "Back to Store")
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    text = "New Charts",
                    fontWeight = FontWeight.Bold
                )
            }

            ActualMetaCreating()
        }
    }
}

@Composable
fun ActualMetaCreating() {
    var title by remember { mutableStateOf("") }
    var composerName by remember { mutableStateOf("") }
    var introduction by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("0") }

    var isRanked by remember { mutableStateOf(false) }
    var isOfficial by remember { mutableStateOf(false) }
    var isNeedReview by remember { mutableStateOf(true) }
    var isHidden by remember { mutableStateOf(false) }

    var musicFile: File? by remember { mutableStateOf(null) }
    var coverFile: File? by remember { mutableStateOf(null) }
    var previewFile: File? by remember { mutableStateOf(null) }

    var operationResult by remember { mutableStateOf(true) }
    var operationResultMessage by remember { mutableStateOf("") }

    val holders = remember { mutableStateListOf(ChartDetailHolder.of(1), ChartDetailHolder.of(2), ChartDetailHolder.of(3), ChartDetailHolder.of(4), ChartDetailHolder.of(5)) }

    var uploaded by remember { mutableStateOf(false) }

    Column(
        Modifier.padding(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fillMaxWidthModifier = Modifier.weight(1F)

            OutlinedTextField(title, { title = it }, label = {
                Text("Title")
            }, singleLine = true, modifier = fillMaxWidthModifier)

            OutlinedTextField(composerName, { composerName = it }, label = {
                Text("Composer")
            }, singleLine = true, modifier = fillMaxWidthModifier)

            var priceError by remember { mutableStateOf(false) }
            OutlinedTextField(
                price,
                {
                    price = it; priceError = it.toIntOrNull() == null; isHidden =
                    -1 == it.toIntOrNull()
                },
                label = {
                    Text("Coin Price")
                },
                singleLine = true,
                modifier = fillMaxWidthModifier,
                isError = priceError
            )
        }

        val scrollState = rememberScrollState()
        OutlinedTextField(
            introduction,
            { introduction = it },
            label = {
                Text("Introduction")
            },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().scrollable(scrollState, Orientation.Vertical)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Checkbox(
                isRanked,
                { isRanked = it; if (it) isRanked = true else isOfficial = false })
            Text("Ranked")
            Checkbox(!isRanked, {
                isRanked = !it; if (it) {
                isRanked = false; isOfficial = false
            }
            })
            Text("Unranked")
            Checkbox(isOfficial, { isOfficial = it; isRanked = true })
            Text("Official")
            Checkbox(isNeedReview, { isNeedReview = it })
            Text("Need Review")
            Checkbox(isHidden, { isHidden = it; price = if (it) "-1" else "0" })
            Text("Hidden")
        }

        Divider(Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FileReceiver(
                "Music",
                onFileChoose = { files ->
                    ExplodeViewModel.validateFile(
                        files.getOrNull(0),
                        true,
                        onAccept = { musicFile = it })
                }
            )

            FileReceiver(
                "Cover",
                onFileChoose = { files ->
                    ExplodeViewModel.validateFile(
                        files.getOrNull(0),
                        true,
                        onAccept = { coverFile = it })
                }
            )

            FileReceiver(
                "Preview",
                onFileChoose = { files ->
                    ExplodeViewModel.validateFile(
                        files.getOrNull(0),
                        true,
                        onAccept = { previewFile = it })
                }
            )
//
//            Button(
//                onClick = {
//                    holders += ChartDetailHolder.of(holders.size + 1)
//                },
//                colors = ButtonDefaults.textButtonColors(backgroundColor = ExplodeColor.Gold),
//                enabled = holders.size < 6
//            ) {
//                Icon(Icons.Default.Add, "Add", tint = Color.White)
//                Spacer(Modifier.width(4.dp))
//                Text("Add Chart", fontWeight = FontWeight.Bold, color = Color.White)
//            }
        }

        holders.forEachIndexed { _, holder ->
            DifficultyDetails(holder, holders)
        }

        Divider(Modifier.padding(vertical = 4.dp))

        Button(
            onClick = {
                runCatching {

                    // validate: fast-fail, and catch exceptions by runCatching
                    val takenDiff = mutableListOf<Int>()
                    holders.forEach {
                        if(it.difficultyBase!! in takenDiff) {
                            error("Duplicate Difficulty Class: ${getDifficultyName(it.difficultyBase!!)}")
                        } else {
                            ExplodeViewModel.validateFile(it.chartFile, true, onReject = { _ -> error("Chart(${it.difficultyBase}) unset or not exist") })
                            takenDiff += it.difficultyBase!!
                        }
                    }
                    ExplodeViewModel.validateFile(musicFile, true, onReject = { error("Music unset or not exist") })
                    ExplodeViewModel.validateFile(coverFile, true, onReject = { error("Cover unset or not exist") })
                    ExplodeViewModel.validateFile(previewFile, true, onReject = { error("Preview unset or not exist") })

                    val set = StoreDataProvider.p.buildChartSet(
                        title,
                        composerName,
                        StoreDataProvider.p.serverUser,
                        isRanked,
                        price.toInt(),
                        introduction,
                        isNeedReview
                    ) {
                        holders
                            .filter {
                                it.chartFile != null
                            }
                            .forEach {
                            addChart(
                                checkNotNull(it.difficultyBase) { "Invalid Difficulty Class" },
                                checkNotNull(it.difficultyValue) { "Invalid Difficulty Value for ${getDifficultyName(it.difficultyBase!!)}" }
                            )
                        }
                    }

                    StoreDataProvider.p.addMusicResource(set._id, musicFile!!.readBytes())
                    StoreDataProvider.p.addSetCoverResource(set._id, coverFile!!.readBytes())
                    StoreDataProvider.p.addPreviewResource(set._id, previewFile!!.readBytes())

                    set.charts.mapNotNull { StoreDataProvider.p.getChart(it) }.forEach { (id, diffClass, _) ->
                        StoreDataProvider.p.addChartResource(id, holders.find { it.difficultyBase == diffClass }!!.chartFile!!.readBytes())
                    }
                }.onSuccess {
                    operationResult = true
                    operationResultMessage = "Successfully updated"

                    uploaded = true
                }.onFailure {
                    operationResult = false
                    operationResultMessage =
                        "Failed due to ${it.message} (${it.javaClass.simpleName})"
                    it.printStackTrace()
                }
            },
            enabled = !uploaded
        ) {
            Text("Create")
        }

        Text(
            text = operationResultMessage,
            color = if (operationResult) ExplodeColor.DarkGreen else Color.Red
        )
    }
}

@Composable
fun FileReceiver(
    choosingCategory: String,
    onFileChoose: (Array<File>) -> Boolean,
    multipleMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var fileChoosed by remember { mutableStateOf(false) }

    Button(
        onClick = {
            val f = FileDialog(ComposeWindow())
            f.isVisible = true
            f.isMultipleMode = multipleMode
            if (onFileChoose(f.files)) {
                fileChoosed = true
            }
        },
        modifier = modifier
    ) {
        if (!fileChoosed) {
            Icon(Icons.Default.ExitToApp, "Not Selected")
        } else {
            Icon(Icons.Default.Check, "Selected")
        }

        Spacer(Modifier.width(4.dp))

        Text(choosingCategory, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun DifficultyDetails(holder: ChartDetailHolder, holders: MutableList<ChartDetailHolder>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FileReceiver("Chart XML", onFileChoose = {
            ExplodeViewModel.validateFile(
                it.getOrNull(0),
                true,
                onAccept = { f -> holder.chartFile = f })
        }, modifier = Modifier.weight(1F))

        Spacer(Modifier.weight(0.1F))

        var diffClass by remember { mutableStateOf(holder.difficultyBase ?: 1) }
        Button(
            onClick = {
                diffClass++
                if (diffClass > 6) diffClass = 1
            },
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = ExplodeColor.getDifficultyBackgroundColor(diffClass)
            ),
            modifier = Modifier.weight(1F)
        ) {
            Text(
                text = getDifficultyName(diffClass),
                fontWeight = FontWeight.Bold,
                color = ExplodeColor.getDifficultyForegroundColor(diffClass)
            )
        }

        Spacer(Modifier.weight(0.1F))

        var diffNumStr by remember { mutableStateOf("") }
        var diffValueError by remember { mutableStateOf(true) }
        OutlinedTextField(
            value = diffNumStr,
            onValueChange = {
                diffNumStr = it
                val num = it.toIntOrNull()
                if (num != null) {
                    holder.difficultyValue = num
                    diffValueError = false
                } else {
                    diffValueError = true
                }
            },
            isError = diffValueError,
            label = { Text("Difficulty") },
            modifier = Modifier.weight(1F).scale(scaleY = 0.8F, scaleX = 1F)
        )

        Spacer(Modifier.weight(0.1F))

        Button(
            onClick = { holders.remove(holder) },
            colors = ButtonDefaults.textButtonColors(Color.Red, Color.White)
        ) {
            Text("Remove")
        }
    }
}