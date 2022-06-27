
$("#upload_btn").on('click', () => {

    let data = new FormData()

    $.each($("#upload_file")[0].files, (i, file) => {
        data.append(`f-${i}`, file)
    })

    data.append(`chart-data`, ["test1", 'test2'])

    $.ajax({
        url: "http://127.0.0.1:10443/bomb/upload",
        method: "post",
        data: data,
        contentType: false,
        processData: false,
        enctype: "multipart/form-data",
        success: (data) => {
            console.log(data)
        },
        error: (err) => {
            console.error(err)
        }
    })
})