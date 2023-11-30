var MYLIBRARY = MYLIBRARY || (function() {
	var _args = {}; // private

	return {
		init: function(Args) {
			_args = Args;
			// some other initialising
		},
		helloWorld: function(i) {
			return _args[i];
		}
	};
}());

hbdApi = new HbdApi();
var batchId = null;
var fileData = null;
var taskTable = null;
var taskRecordsTotal;
var fileRecordsTotal;
var fileViews = {};
var visibleRows = [];

$(document).ready(function() {
	batchId = MYLIBRARY.helloWorld(1);
});


function createFileViewTable(fVJson, tr, taskId) {
	var tableString = '<div class="slider"><table id=\"fileTable' + taskId + '\" class=\"display\" style=\"width:100%\">';
	//var rows = fVJson['rows'];
	var headers = fVJson['headers'];

	var headerString = '<thead><tr>';
	headers.forEach((header, i) => {
		headerString += '<th>' + header + '</th>';
	});
	tableString += headerString + '</tr></thead>';
	tableString += '</table></div>';
	return tableString;
}


$(document).ready(function() {
	var taskView = hbdApi.getTaskView(batchId, 0,0, -1); // get task data
	taskRecordsTotal = taskView['recordsTotal'];
	// console.log(taskView)
	taskTable = generateTaskTable(taskView, 'taskTable');

	// Add event listener for opening and closing details
	$('#taskTable tbody').on('click', 'td.dt-control', function() {
		visibleRows.push(0);
		console.log(visibleRows);
		var tr = $(this).closest('tr');
		console.log('taskId');
		var row = taskTable.row(tr);
		var taskId = row.data()['Task'];//$(tr).find('.sorting_1').text(); // find taskId by className
		console.log(taskId);
		var idx = $.inArray(taskId, visibleRows);
		console.log(idx);
		if (row.child.isShown()) {
			visibleRows.splice(idx, 1);
			// This row is already open - close it
			$('div.slider', row.child()).slideUp(function() {
				row.child.hide();
				tr.removeClass('shown');
			});
		} else {
			if ( idx === -1 ) {
				console.log('push');
                visibleRows.push(taskId);
            }
			// Open this row
			fileView = hbdApi.getFileView(taskId, 0, 0, -1, null);
			fileRecordsTotal = fileView['recordsTotal'];

			let rows = {};
			rows['data'] = fileView['rows'];
			//console.log(rows);
			row.child(createFileViewTable(fileView, tr, taskId)).show();
			let headers = [];
			fileView['headers'].forEach((header, i) => { // add headers to headers arr
				let h = {};
				if (header == 'sha256') {
					h = {
						"data": "sha256",
						"fnCreatedCell": function(nTd, sData, oData, iRow, iCol) {
							$(nTd).html('<a href=\"/' + oData.fileAttributeId + '/' + oData.sha256 + '/fileDetails\">' + oData.sha256 + '</a>');
						}
					};
				} else {
					h = {
						title: header,
						data: header,
						name: header,
					};
				};
				headers.push(h);
			});
			let colDefs = [{orderable: true, searchable: false, target: 0},{orderable: false, searchable: false, visible: false, target: 1}, {target: headers.length-1, orderable: true, searchable: false}];
			for(i=3;i<headers.length-1;i++)
			{
				colDefs.push({orderable: false, searchable: false, target: i});
			}
			console.log(colDefs);
			$('#fileTable' + taskId).DataTable({
				'autoWidth': false,
    			'dom': 'Blfrtip',
				responsive: true,
				paging: true,
				info: false,
				serverSide: true,
				pagingType: "full_numbers",
				"searching": true,
				filter: true,
				"ajax": function(data, callback, settings) {
					console.log(data);
					console.log(hbdApi.getQuery(data))
					let fileView = hbdApi.getFileView(taskId, data['start'], data['length'], hbdApi.getOrder(data), hbdApi.getOrderColumn(data), hbdApi.getQuery(data));
					let d = {
						data: fileView['rows'],
						recordsFiltered: fileRecordsTotal,
						recordsTotal: fileRecordsTotal
					};
					callback(d)
				},
				"language": {
    				"search": "Search by SHA256:"
 				 },
				columns: headers,
				"lengthMenu": [[25, 50, 100, -1], [25, 50, 100, "All"]],
				columnDefs: colDefs
			});
			tr.addClass('shown');
			$('div.slider', row.child()).slideDown();
		}
	});
/*	setInterval( function () {
    taskTable.ajax.reload();
}, 10000 );*/
});

function showFiletypeSummaryTable() {
	let summaryButton = document.getElementById('filetypeSummaryButton');
	summaryButton.setAttribute('hidden', 'hidden');
	let summaryCard = document.getElementById('filetypeSummaryCard');
	summaryCard.removeAttribute('hidden');
	let json = hbdApi.filetypeSummaryView(batchId);
	var summaryTable = generateFiletypeSummaryTable(json);
}

function generateTaskTable(json, tablename) {
	var headers = [{ // initialize headers with control button blank header
		className: 'dt-control',
		orderable: false,
		data: null,
		defaultContent: '',
	}];
	json['headers'].forEach((header, i) => { // add headers to headers arr
		let h = {
			title: header['name'],
              data: header['data'],
              name: header['name'],
              };
		headers.push(h);
	});
	// custom html to display preloader if task status is not complete or failed
	for(i=4;i<headers.length;i++) {
		headers[i]["fnCreatedCell"] = function (nTd, sData, oData, iRow, iCol) {
			if(oData.Status != 'Complete' && oData.Status != 'Failed') {
				$(nTd).html('<img src="/img/preloaders/fadingcircles.gif" height="10px"/>');
			} else {
				$(nTd).html(oData.Completion);
			};
		};
	};
	let cDefs = [{target:0,orderable:false}, {target: 1,visible: false,searchable: false, orderable:true},
				{target:2,orderable:true},
				{target:3,orderable:false, searchable:false}];
	for(i=4;i<headers.length;i++)
	{
		cDefs.push({target:i,orderable:true});
	};
	
	var table = $('#' + tablename).DataTable({ // generate table
		dom: 'lrtip',
		columns: headers,
		order: [[1, 'desc']], // sort by taskId
		responsive: true,
		serverSide: true,
		pagingType: "full_numbers",
		"ajax": function(data, callback, settings) {
					let taskView = hbdApi.getTaskView(batchId, data['start'], data['length'], hbdApi.getOrder(data), hbdApi.getOrderColumn(data));
					let d = {
						data: taskView['rows'],
						recordsFiltered: taskRecordsTotal,
						recordsTotal: taskRecordsTotal
					};
					callback(d)
				},
		columnDefs: cDefs,
	});
	return table;
};

function generateFiletypeSummaryTable(json) {
	console.log(json);
		var headers = []; 
	json['headers'].forEach((header, i) => { // add headers to headers arr
		let h = {title: header,
              data: header};
		headers.push(h);
	});
    var table = $('#filetypeSummaryTable').DataTable({ // generate table
    dom: 't',
	columns: headers,
        data: json['rows'],
        order: [[1, 'desc']], // sort by taskId
        responsive: true
});
	return table;
};
