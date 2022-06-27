package explode.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import explode.compose.data.StoreDataProvider
import explode.dataprovider.model.SetModel
import kotlinx.coroutines.launch

@Composable
fun StoreViewList() {
	val coroutineScope = rememberCoroutineScope()

	val listState = rememberLazyListState()

	val charts = remember { mutableStateListOf<SetModel>() }

	StoreSearchBar(requestStoreItemUpdate = { searchedName, ranked, unranked, official, review, hidden ->
		coroutineScope.launch {
			charts.clear()
			charts += StoreDataProvider.getChartSets(
				10, 0,
				searchedName.ifEmpty { null },
				hidden.takeIf { it },
				official.takeIf { it },
				if(ranked) true else if(unranked) false else null,
				review.takeIf { it }
			)

			listState.animateScrollToItem(0)
		}
	})

	LazyColumn(
		state = listState
	) {
		items(charts) {
			ChartSetCard(it)
		}
	}
}

@Composable
fun StoreSearchBar(requestStoreItemUpdate: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit) {
	var searchedName by remember { mutableStateOf("") }
	var isRanked by remember { mutableStateOf(false) }
	var isUnranked by remember { mutableStateOf(false) }
	var isOfficial by remember { mutableStateOf(false) }
	var isNeedReview by remember { mutableStateOf(false) }
	var isHidden by remember { mutableStateOf(false) }

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(12.dp)
	) {
		OutlinedTextField(
			value = searchedName,
			onValueChange = { searchedName = it },
			modifier = Modifier.fillMaxWidth(),
			singleLine = true,
			label = {
				Text("Search")
			},
			leadingIcon = {
				Icon(Icons.Default.Info, "Search")
			}
		)

		// 谱面属性选择
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Checkbox(isRanked, { isRanked = it; if(it) isUnranked = false })
			Text("Ranked")
			Spacer(Modifier.size(4.dp))
			Checkbox(isUnranked, { isUnranked = it; if(it) isRanked = false })
			Text("Unranked")
			Spacer(Modifier.size(4.dp))
			Checkbox(isOfficial, { isOfficial = it; isRanked = true })
			Text("Official")
			Spacer(Modifier.size(4.dp))
			Checkbox(isNeedReview, { isNeedReview = it })
			Text("Need Review")
			Spacer(Modifier.size(4.dp))
			Checkbox(isHidden, { isHidden = it })
			Text("Hidden")
		}

		Button(
			onClick = { requestStoreItemUpdate(searchedName, isRanked, isUnranked, isOfficial, isNeedReview, isHidden) },
			modifier = Modifier.fillMaxWidth()
		) {
			Icon(Icons.Default.Search, "Search")
		}
	}
}

@Composable
fun ChartSetCard(chartSet: SetModel) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(15.dp)
			.clickable {
				// TODO: Switch to the Chart Detail View
			},
		elevation = 10.dp
	) {
		val iter = chartSet.chart.iterator()

		Column(
			Modifier.padding(15.dp)
		) {

			// 谱面名称和难度
			Row(
				verticalAlignment = Alignment.CenterVertically
			) {

				Text(
					text = chartSet.musicTitle,
					fontWeight = FontWeight.Bold,
					fontSize = 24.sp
				)

				while(iter.hasNext()) {
					val chart = iter.next()
					val difficultyColor = when(chart.difficultyClass) {
						1 -> Color.Green
						2 -> Color.Cyan
						3 -> Color.Red
						4 -> Color.Magenta
						5 -> Color.LightGray
						else -> Color.Black
					}

					Box(
						modifier = Modifier.size(48.dp).padding(8.dp).clip(CircleShape)
							.background(color = difficultyColor),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = buildAnnotatedString {
								withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
									append("${chart.difficultyValue}")
								}
							}
						)
					}
				}
			}

			Divider(
				Modifier.padding(bottom = 8.dp)
			)

			// 谱面基础信息
			Row {
				Text(
					text = buildAnnotatedString {
						if(chartSet.needReview) {
							withStyle(SpanStyle(color = Color.Blue)) {
								append("Need Review")
							}
							append("/")
						}
						if(chartSet.isRanked) {
							withStyle(SpanStyle(color = Color.Red)) {
								if(chartSet.isOfficial) {
									append("Official Ranked")
								} else {
									append("Community Ranked")
								}
							}
						} else {
							withStyle(SpanStyle(color = Color.Green)) {
								append("Unranked")
							}
						}
						append("/")
						withStyle(SpanStyle(color = Color.Magenta)) {
							append("${chartSet.coinPrice}")
						}
					}
				)
			}

			if(chartSet.introduction.isNotEmpty()) {
				Text(text = chartSet.introduction.replace("\\n", "\n"), color = Color.Gray)
			}
		}
	}
}