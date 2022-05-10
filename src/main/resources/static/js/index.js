var fileUploadBtn = document.getElementById("fileUploadBtn");
fileUploadBtn.disabled = true;
var csvFile = document.getElementById("csv-file");
csvFile.addEventListener("change", toggleUploadBtn);

function toggleUploadBtn() {
    if (csvFile.value === "") {
        fileUploadBtn.disabled = true;
    } else {
        fileUploadBtn.disabled = false;
    }
}

var quickSearchBtn = document.getElementById("quickSearchBtn");
quickSearchBtn.disabled = true;
var quickSearchVal = document.getElementById("quickSearchVal");
quickSearchVal.addEventListener("change", toggleSearchBtn);

function toggleSearchBtn() {
    if (quickSearchVal.value === "") {
        quickSearchBtn.disabled = true;
    } else {
        quickSearchBtn.disabled = false;
    }
}