window.requestAnimationFrame =
           window.requestAnimationFrame ||
           window.mozRequestAnimationFrame ||
           window.webkitRequestAnimationFrame ||
           window.msRequestAnimationFrame;
 
var hbdApi = new HbdApi();
var batchViewTable = null;
var fileModal = $('#analyzedModal');
 
$(document).ready(function () {
	let AFVJson = hbdApi.getFilesView();
	console.log(AFVJson);
	
	batchViewTable = generateTable(AFVJson, 'filesViewTable');
	let numRows = batchViewTable.rows().count();
	if(numRows == 0) {
		info.classList.remove("visually-hidden");
	}
	
	$('#closeModal').on('click', function() {
		fileModal.modal('hide');
	});
	$('#xModal').on('click', function() {
		fileModal.modal('hide');
	});
	
	$('#modalSubmit').html('Analyze Selected Files');
	
	$('#modalSubmit').on('click', function () {
		$('#archiveAlert').hide();
	    let files = document.querySelector("#archivesToSubmit").value;
	    if(!files) {
			$('#archiveAlert').show();
		}
		else {
			submit(files);
		}
    });
	
});


function generateTable(json, tablename) {
	var headers = [];
	json['headers'].forEach((header, i) => { // add headers to headers arr
		let h = {title: header,
              data: header};
		headers.push(h);
	});
    var table = $('#'+tablename).DataTable({ // generate table
    "searching": false,
	columns: headers,
        data: json['rows'],
        order: [[0, 'desc']], // sort by taskId
        responsive: true,
});
	return table;
};

function submit(files)
{
	let obfuscate = document.querySelector("#obfuscate").checked;
	let sanitize = document.querySelector("#sanitize").checked;
	let name = document.querySelector("#name").value;
	let submitJson = hbdApi.submitView(name, files, obfuscate, sanitize);
	let batchId = submitJson['batch'];
	console.log(name)
	console.log(batchId);	
	if(name=='') {
		name=batchId;
		}

	let successList = submitJson['submitted'],
 	successListString = '',
	failList = submitJson['failed'],
	failListString = '';
	
	if(successList.length == 1)
	{
		successListString = successList[0];
	} else {
	successList.forEach((file) => successListString+=' '+ file+',');
	successListString = successListString.substring(0, successListString.length-1);
	}
	if(failList.length == 1)
	{
		failListString = failList[0];
	} else {
	failList.forEach((file) => failListString+=' '+ file+',');
	failListString = failListString.substring(0, failListString.length-1);
	}
	
	let submitAlertModal = $("#modalHeader");
	submitAlertModal[0].classList.remove("bg-primary");
	submitAlertModal[0].classList.remove("bg-warning");
	submitAlertModal[0].classList.remove("bg-danger");
	
	let modalHeaderText = $("#modalTitle");
	let modalTextInfo = $("#modalSubmitInfo");
	let reportLink = $("#reportButton");
	let partialToUpload = $('#uploadButton');
	let uploadInfo = $('#uploadAlert');
	
	uploadInfo[0].classList.remove("alert-danger");
	uploadInfo[0].classList.remove("alert-warning");
	
	if(successList.length != 0 && failList.length ==0)
	{
		submitAlertModal[0].classList.add("bg-primary");
		modalHeaderText[0].textContent = 'Created Batch ' + "'" + name + "'";
		modalTextInfo[0].childNodes[1].textContent =  "Analyzing " + successListString;
		modalTextInfo[0].childNodes[3].textContent = '';
		uploadInfo.hide();
		partialToUpload.hide();
		reportLink.show();
		reportLink.html("View Batch "+ "'" + name + "'");
		reportLink[0].setAttribute("href", '/'+batchId+'/report?name='+name);
	} else if(successList.length != 0 && failList.length !=0) {
		submitAlertModal[0].classList.add("bg-warning");
		modalHeaderText[0].textContent =  "Created Batch " + "'" + name + "'"; 
		modalTextInfo[0].childNodes[1].textContent =  "Analyzing " + successListString;
		modalTextInfo[0].childNodes[3].textContent = "Failed to submit " + failListString  + " for analysis";
		uploadInfo.show();
		uploadInfo[0].classList.add("alert-warning");
		partialToUpload.show();
		partialToUpload.html("Upload Files");
		partialToUpload[0].setAttribute("href", '/upload');
		reportLink.show();
		reportLink.html("View Batch "+ "'" + name + "'");
		reportLink[0].setAttribute("href", '/'+batchId+'/report?name='+name);
	} else {
		submitAlertModal[0].classList.add("bg-danger");
		modalHeaderText[0].textContent =  "Failed to create Batch";
		modalTextInfo[0].childNodes[1].textContent = "Failed to submit " + failListString + " for analysis";
		modalTextInfo[0].childNodes[3].textContent = '';
		uploadInfo.show();
		uploadInfo[0].classList.add("alert-danger");
		partialToUpload.hide();
		reportLink.show();
		reportLink.html("Upload Files");
		reportLink[0].setAttribute("href", '/upload');
	};

	fileModal.modal('show');
}