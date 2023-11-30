window.requestAnimationFrame =
	window.requestAnimationFrame ||
	window.mozRequestAnimationFrame ||
	window.webkitRequestAnimationFrame ||
	window.msRequestAnimationFrame;

var hbdApi = new HbdApi();
var mailboxesTable = null;
var dateFormat = "yy-mm-dd";
var mailModal = $('#analyzedModal');

$(document).ready(function() {
	let mailboxesViewJson = hbdApi.getMailboxesView(0, -1);
	console.log(mailboxesViewJson);

	mailboxesTable = generateTable(mailboxesViewJson, 'mailboxesViewTable');

	$('#closeModal').on('click', function() {
		mailModal.modal('hide');
	});
	$('#xModal').on('click', function() {
		mailModal.modal('hide');
	});
	
	$('#modalSubmit').html('Analyze Selected Mailboxes');
	
	$('#modalSubmit').on('click', function () {
		$('#mailboxAlert').hide();
		$('#dateAlert').hide();
	    let mailboxRows = mailboxesTable.rows( {selected : true} ).data();
	    console.log(mailboxesTable.rows( {selected : true} ).data());
	    let mailboxes = '';
	    $.each(mailboxRows, function(index, row){
			mailboxes+=row['Mailbox']+',';
		});
		let dateRange0 = getDateRange();
		if (!dateRange0) {
			$('#dateAlert').show();
		}
	    if(!mailboxes) {
			$('#mailboxAlert').show();
		}
		if(mailboxes && dateRange0) {
			submit(mailboxes, dateRange0);
		}
    });
});

$(function() {
	var today = new Date();
	var maxScanInterval = '-150';
	today = paddedDateString(today);
	//console.log(today);
	
	from = $("#from").datepicker({
			dateFormat: dateFormat,
			changeMonth: true,
			changeYear: true,
			maxDate: today,
			minDate: maxScanInterval,
			numberOfMonths: 1
	})
		.on("change", function() {
			to.datepicker("option", "minDate", getDate(this));
			date1 = getDate(this);
		}),

		to = $("#to").datepicker({
			dateFormat: dateFormat,
			changeMonth: true,
			changeYear: true,
			maxDate: today,
			minDate: maxScanInterval,
			numberOfMonths: 1
		})
		.datepicker("setDate", new Date())
		
		.on("change", function() {
			from.datepicker("option", "maxDate", getDate(this));
			date2 = getDate(this);
		});
});

function paddedDateString(date) {
	let dd = date.getDate();
	let mm = date.getMonth() + 1; //January is 0
	let yy = date.getFullYear();
	return yy + '-' + padDateStringComponent(mm) + '-' + padDateStringComponent(dd);
};

function padDateStringComponent(component) {
	if(component < 10) return '0' + component;
	return component;
};
 
function getDate(element) {
	var date;
	try {
		date = $.datepicker.parseDate(dateFormat, element.value);
	} catch (error) {
		date = null;
	}
	return date;
};

function getDateRange() {
	//read in date 1, date 2 (already valid - checked by getDate)
	dateMin = getDate(document.getElementById("from"));
	dateMax = getDate(document.getElementById("to"));
	
	if(!dateMin || !dateMax ) {
		return;
	} else {
		
	//calculate difference
	var differenceTime = dateMax.getTime() - dateMin.getTime();
	var differenceDays = Math.round(differenceTime / (1000 * 3600 * 24));
	
	let dateRange = {
		until: dateMax.toISOString(),
		days: differenceDays
	};
	return dateRange;
	}
	
}


function generateTable(json, tablename) {
	console.log(json);
	let checkboxHeader = { title: '', data: 'checkbox' }
	var headers = [checkboxHeader];
	delete json['headers'][0];
	json['headers'].forEach((header, i) => { // add headers to headers arr
		let h = {
			title: header,
			data: header
		};
		headers.push(h);
	});
	console.log(headers);

	let colDefs = [{ orderable: false, searchable: false, className: 'select-checkbox', target: 0 }];
	for (i = 1; i < headers.length; i++) {
		colDefs.push({ orderable: true, searchable: false, target: i });
	}
	console.log(colDefs);
	
    var table = $('#'+tablename).DataTable({ // generate table
    'dom': 'frtBi',
    	"searching": false,
		columns: headers,
		columnDefs: colDefs,
        responsive: true,
        paging: true,
        data: json['rows'],
        createdRow: function( row, data, dataIndex ) { 
		      if(data['auth'] == false){
		        $(row).addClass('row-disabled');
		        let td = $(row).children('td').first();
		        td.removeClass('select-checkbox');
		        td.css('text-align', 'center');
		        $(td).html('Unauthorized');
		      } 
		 },
		info: false,
		pagingType: "full_numbers",
		"lengthMenu": [[25, 50, 100, -1], [25, 50, 100, "All"]],
        select: {
            style:    'multi',
            selector: 'td:first-child'
        },
        order: [[ 1, 'asc' ]]
	});
	table.on('user-select', function (e, dt, type, cell, originalEvent) {
	  var row = dt.row( cell.index().row );
	  if ( row.data().auth == false ) {
	    e.preventDefault();
	  }
	 });
	return table;
};

function submit(mailboxes, dateRange)
{
	let obfuscate = $('#obfuscate').is(':checked');
	let sanitize = $('#sanitize').is(':checked');
	let name = document.querySelector("#name").value;
	let submitJson = hbdApi.graphSubmitView(mailboxes, dateRange.until, dateRange.days, obfuscate, sanitize, name);
	let batchId = submitJson['batch'];
	if(name=='') {
		name=batchId;
	}
	console.log(name);
	console.log(batchId);
    console.log(submitJson);
	
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
	
	if(successList.length != 0 && failList.length ==0)
	{
		submitAlertModal[0].classList.add("bg-primary");
		modalHeaderText[0].textContent = 'Created Batch ' + "'" + name + "'";
		modalTextInfo[0].childNodes[1].textContent =  "Analyzing " + successListString;
		modalTextInfo[0].childNodes[3].textContent = '';
		reportLink.show();
		reportLink.html("View Batch "+ "'" + name + "'");
		reportLink[0].setAttribute("href", '/'+batchId+'/report?name='+name);
	} else if(successList.length != 0 && failList.length !=0) {
		submitAlertModal[0].classList.add("bg-warning");
		modalHeaderText[0].textContent =  "Created Batch " + "'" + name + "'";
		modalTextInfo[0].childNodes[1].textContent =  "Analyzing " + successListString;
		modalTextInfo[0].childNodes[3].textContent = "Failed to submit " + failListString  + " for analysis";
		reportLink.show();
		reportLink.html("View Batch "+"'" + name + "'");
		reportLink[0].setAttribute("href", '/'+batchId+'/report?name='+name);
	} else {
		submitAlertModal[0].classList.add("bg-danger");
		modalHeaderText[0].textContent =  "Failed to create Batch";
		modalTextInfo[0].childNodes[1].textContent = "Failed to submit " + failListString + " for analysis";
		modalTextInfo[0].childNodes[3].textContent = '';
		reportLink.hide();
	};

	mailModal.modal('show');
}
/*let submitForm = $("#modalSubmit");
submitForm.on('click', submit); 
console.log(submitForm);*/