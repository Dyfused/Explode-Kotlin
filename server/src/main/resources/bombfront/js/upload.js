
function genChartInputForm(c) { return `
<div>
    <fieldset class="inline">
        <!-- Chart ${c+1} -->
        <label>
            <span>Chart Difficulty Class</span>
            <input type="number" min="1" max="6" value="${c+1}" name="chart_diff_class_${c}" id="chart_diff_class_${c}">
        </label>
        <label>
            <span>Chart Difficulty Value</span>
            <input type="number" value="0" name="chart_diff_value_${c}" id="chart_diff_value_${c}">
        </label>
        <label>
            <span>Chart File</span>
            <input type="file" name="chart_file_${c}" id="chart_file_${c}" />
        </label>
    </fieldset>
</div>
`}

const BOMBDATA = {
    "chart_count": 0,
    "add_chart": () => {
        if(BOMBDATA.chart_count >= 5) return
        $("#charts").append(genChartInputForm(BOMBDATA.chart_count++))
    },
    "gen_set_model": (
        musicTitle,
        composerName,
        charts
    ) => {
        return {
            "title": musicTitle,
            "composerName": composerName,
            "chartMeta": charts,
            "musicFileName": "music-file",
            "coverFileName": "cover-file",
            "previewFileName": "preview-file",
            "storePreviewFileName": "store-preview-file"
        }
    },
    "gen_chart_model": (f, c, v) => {
        return {
            "chartFileName": f,
            "chartDifficultyClass": c,
            "chartDifficultyValue": v
        }
    },
    "get_input_file": (id) => {
        return $(`#${id}`)[0].files[0]
    }
}

$("#upload_btn").on('click', () => {

    let data = new FormData()

    let findCharts = []
    for(let i = 0; i < BOMBDATA.chart_count; i++) {
        let chartClass = $(`#chart_diff_class_${i}`).val()
        let chartValue = $(`#chart_diff_value_${i}`).val()
        let chartFile = `chart-file-${i}`
        findCharts.push(BOMBDATA.gen_chart_model(chartFile, chartClass, chartValue))

        data.append(chartFile, BOMBDATA.get_input_file(`chart_file_${i}`))
    }

    let setModel = BOMBDATA.gen_set_model(
        $("#music_name").val(),
        $("#composer_name").val(),
        findCharts
    )

    console.log(data)

    data.append("chart-data", JSON.stringify(setModel))
    data.append("music-file", BOMBDATA.get_input_file("music_file"))
    data.append("preview-file", BOMBDATA.get_input_file("preview_music_file"))
    data.append("cover-file", BOMBDATA.get_input_file("cover_file"))
    data.append("store-preview-file", BOMBDATA.get_input_file("store_preview_file"))


    $.ajax({
        url: `${BOMB.endpoint}/bomb/upload`,
        method: "post",
        data: data,
        contentType: false,
        processData: false,
        enctype: "multipart/form-data",
        success: (data) => {
            console.log(data)
            alert(`Success: ${data.data}`)
        },
        error: ({ responseJSON }) => {
            console.log(responseJSON)
            alert(`Failed: ${responseJSON.error}`)
        }
    })
})

$("#add_chart_btn").on("click", BOMBDATA.add_chart)