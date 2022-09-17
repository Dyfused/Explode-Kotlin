// function genChartInputForm(c) {
//     return `
// <div>
//     <fieldset class="inline">
//         <!-- Chart ${c + 1} -->
//         <label>
//             <span>Chart Difficulty Class</span>
//             <input type="number" min="1" max="6" value="${c + 1}" name="chart_diff_class_${c}" id="chart_diff_class_${c}">
//         </label>
//         <label>
//             <span>Chart Difficulty Value</span>
//             <input type="number" value="0" name="chart_diff_value_${c}" id="chart_diff_value_${c}">
//         </label>
//         <label>
//             <span>Chart File</span>
//             <input type="file" name="chart_file_${c}" id="chart_file_${c}" />
//         </label>
//     </fieldset>
// </div>
// `}

// const BOMBDATA = {
//     "chart_count": 0,
//     "add_chart": () => {
//         if (BOMBDATA.chart_count >= 5) return
//         $("#charts").append(genChartInputForm(BOMBDATA.chart_count++))
//     },
//     "gen_set_model": (
//         musicTitle,
//         composerName,
//         charts
//     ) => {
//         return {
//             "title": musicTitle,
//             "composerName": composerName,
//             "chartMeta": charts,
//             "musicFileName": "music-file",
//             "coverFileName": "cover-file",
//             "previewFileName": "preview-file",
//             "storePreviewFileName": "store-preview-file"
//         }
//     },
//     "gen_chart_model": (f, c, v) => {
//         return {
//             "chartFileName": f,
//             "chartDifficultyClass": c,
//             "chartDifficultyValue": v
//         }
//     },
//     "get_input_file": (id) => {
//         return $(`#${id}`)[0].files[0]
//     }
// }

// $("#upload_btn").on('click', () => {

//     let data = new FormData()

//     let findCharts = []
//     for (let i = 0; i < BOMBDATA.chart_count; i++) {
//         let chartClass = $(`#chart_diff_class_${i}`).val()
//         let chartValue = $(`#chart_diff_value_${i}`).val()
//         let chartFile = `chart-file-${i}`
//         findCharts.push(BOMBDATA.gen_chart_model(chartFile, chartClass, chartValue))

//         data.append(chartFile, BOMBDATA.get_input_file(`chart_file_${i}`))
//     }

//     let setModel = BOMBDATA.gen_set_model(
//         $("#music_name").val(),
//         $("#composer_name").val(),
//         findCharts
//     )

//     console.log(data)

//     data.append("chart-data", JSON.stringify(setModel))
//     data.append("music-file", BOMBDATA.get_input_file("music_file"))
//     data.append("preview-file", BOMBDATA.get_input_file("preview_music_file"))
//     data.append("cover-file", BOMBDATA.get_input_file("cover_file"))
//     data.append("store-preview-file", BOMBDATA.get_input_file("store_preview_file"))

//     $.ajax({
//         url: `${BOMB.endpoint}/bomb/upload`,
//         method: "post",
//         data: data,
//         contentType: false,
//         processData: false,
//         enctype: "multipart/form-data",
//         success: (data) => {
//             console.log(data)
//             alert(`Success: ${data.data}`)
//         },
//         error: ({ responseJSON }) => {
//             console.log(responseJSON)
//             alert(`Failed: ${responseJSON.error}`)
//         }
//     })
// })

document.getElementById("upload_btn").addEventListener("click", (ev) => {
	console.log(
		"Start to submit and upload the set, please wait until success or failure"
	);

	let charts = [];
	while (true) {
		let index = charts.length;

		console.log(`Searching for the chart in ${index}`);
		let clazz = document.getElementById(`chart_diff_class_${index}`);
		let value = document.getElementById(`chart_diff_value_${index}`);

		if (clazz == null || value == null) {
			console.log("Done", charts.length);
			break;
		}

		let c = {
			file: document.getElementById(`chart_file_${index}`).files[0],
			clazz: parseInt(clazz.value),
			value: parseInt(value.value),
			d: null,
			defaultId: null,
		};

		console.log(c);
		charts.push(c);
	}

	console.log(
		document.getElementById("music_name").value,
		document.getElementById("composer_name").value,
		0,
		null,
		"Introduction here",
		true,
		"UNRANKED",
		null,
		charts,
		document.getElementById("music_file").files[0],
		document.getElementById("cover_file").files[0],
		document.getElementById("preview_music_file").files[0],
		document.getElementById("store_preview_file").files[0]
	);

	return Bomb("http://localhost:10443/v1")
		.Management("Taskeren", "123456")
		.upload(
			document.getElementById("music_name").value,
			document.getElementById("composer_name").value,
			0,
			null,
			"Introduction here",
			true,
			"UNRANKED",
			null,
			charts,
			document.getElementById("music_file").files[0],
			document.getElementById("cover_file").files[0],
			document.getElementById("preview_music_file").files[0],
			document.getElementById("store_preview_file").files[0]
		);
});

// $("#add_chart_btn").on("click", BOMBDATA.add_chart)

var chart_count = 0;

document.getElementById("add_chart_btn").addEventListener("click", (ev) => {
	if (chart_count > 5) return console.log("Meet maximum chart count!");
	document.getElementById("charts").innerHTML += `
    <div>
        <fieldset class="inline">
            <!-- Chart ${chart_count} -->
            <label>
                <span>Chart Difficulty Class</span>
                <input type="number" min="1" max="6" value="${chart_count}" name="chart_diff_class_${chart_count}" id="chart_diff_class_${chart_count}">
            </label>
            <label>
                <span>Chart Difficulty Value</span>
                <input type="number" value="0" name="chart_diff_value_${chart_count}" id="chart_diff_value_${chart_count}">
            </label>
            <label>
                <span>Chart File</span>
                <input type="file" name="chart_file_${chart_count}" id="chart_file_${chart_count}" />
            </label>
        </fieldset>
    </div>
    `;
	chart_count++;
});
