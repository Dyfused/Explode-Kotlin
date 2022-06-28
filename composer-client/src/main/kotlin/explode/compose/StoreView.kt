package explode.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import explode.compose.data.StoreDataProvider
import explode.compose.theme.ExplodeColor
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
		items(charts) { setModel ->
			ChartSetCard(setModel) {
				ExplodeViewModel.setEditingChartSet(it)
			}
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
			Checkbox(isHidden, { isHidden = it })
			Text("Hidden")
		}

		Row {
			Button(
				onClick = { ExplodeViewModel.onCreatingPage = true },
				modifier = Modifier.weight(2F)
			) {
				Icon(Icons.Default.Edit, "Add")
			}

			Spacer(Modifier.weight(0.5F))

			Button(
				onClick = { requestStoreItemUpdate(searchedName, isRanked, isUnranked, isOfficial, isNeedReview, isHidden) },
				modifier = Modifier.weight(5F)
			) {
				Icon(Icons.Default.Search, "Search")
			}
		}
	}
}

@Composable
fun ChartSetCard(chartSet: SetModel, onClick: (SetModel) -> Unit) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(15.dp)
			.clickable { onClick(chartSet) },
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
						1 -> ExplodeColor.Casual
						2 -> ExplodeColor.Normal
						3 -> ExplodeColor.Hard
						4 -> ExplodeColor.Mega
						5 -> ExplodeColor.Giga
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
							withStyle(SpanStyle(color = ExplodeColor.DarkGreen)) {
								append("Unranked")
							}
						}
						append("/")
						withStyle(SpanStyle(color = ExplodeColor.Gold)) {
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
