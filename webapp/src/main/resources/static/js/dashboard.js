/**
 * 
 */


var hbdApi = new HbdApi();
var batchViewTable = null;
var recordsTotal;
var editingBatch = {
	status: false,
	row: null
}
var buttonPaths = {
	edit: "/img/button/editbutton.png",
	confirm: "/img/button/confirmbutton-green-check.png",
	delete: "/img/button/deletebutton-red-x.png",
	active: 0
};
var buttonImg = {
	edit: new Image(20, 20),
	confirm: new Image(20, 20),
	delete: new Image(20, 20)
};
var currentName = "";
var current_d = null;

var tableState = {
	data: null,
};

var refreshWorker = new Worker(new URL('./js/workers/batchViewWorker.js', window.location.origin));

$(document).ready(function() {
	// cache images
	buttonImg.edit.src = buttonPaths.edit;
	buttonImg.confirm.src = buttonPaths.confirm;
	buttonImg.delete.src = buttonPaths.delete;

	let batches = hbdApi.getBatchView(0, 0, -1);
	recordsTotal = batches['recordsTotal'];
	generateTable(batches, 'batchTable');

	/*refreshWorker.addEventListener('message', function(e) {
		console.log(e);
		batchViewTable.rows().every(function(rowIdx, tableLoop, rowLoop) {
			var row = batchViewTable.row(rowIdx).data();
			let idx = e.data.findIndex(x => x.batchId == row.batchId);
			//console.log(idx);
			if (idx == -1) {
				//console.log('removing '+idx);
				batchViewTable.rows().remove(rowIdx);
			} else {
				e.data.splice(idx, 1);
			}
		});
		if (e.data != []) {
			batchViewTable.rows.add(batches).draw();
			console.log('reloaded');
			console.log('loaded rows ' + e.data);
		};
	}, false);*/

	setInterval(function() {
		if (!editingBatch.status) {
			batchViewTable.ajax.reload(null, false);
			/*let message = {
				baseUrl: HbdApi.baseUrl,
				data: tableState.data
			};
			console.log(message);

			refreshWorker.postMessage(message);*/
		};
	}, 10000);

});


$("#batchTable").on('click', 'img', function(event) {
	//console.log('click');
	let id = $(this).attr('id');
	let splitId = id.split("-");
	let rowId = splitId[1];
	let batchId = batchViewTable.row(rowId).data().batchId;
	let action = splitId[0];
	console.log('action ' + action + ' rowid ' + rowId);
	if (action == 'delete') {
		hbdApi.delete('batch', batchId);
		batchViewTable.rows().remove(rowId);
		batchViewTable.ajax.reload();
		// console.log("deleted batch "+batchId);
	} else if (action == 'edit' && !editingBatch.status) {
		let div = $('#text-' + rowId);
		currentName = $(div).text();
		// make div editable
		$('#text-' + rowId).prop('contenteditable', true);
		$(this).prop('src', buttonPaths.confirm);
		$(this).prop('id', 'confirm-' + rowId);
		buttonPaths.active = 1;
		editingBatch.status = true;
		editingBatch.row = rowId;
	} else if (action == 'confirm') {
		let div = $('#text-' + rowId);
		$(div).prop('contenteditable', false);
		$(this).prop('src', buttonPaths.edit);
		$(this).prop('id', 'edit-' + rowId);
		let name = $(div).text();
		// console.log(name);
		editingBatch.status = false;
		editingBatch.row = null;
		buttonPaths.active = 0;
		if (name != currentName) {
			if (name == "") name = null;
			hbdApi.put('batch', 'name', batchId, name);
		};
	};
});

function generateTable(json, tablename) {
	var headers = [{
		data: null,
		"fnCreatedCell": function(nTd, sData, oData, iRow, iCol) {
			$(nTd).html("<a href='/" + oData.batchId + "/report?name=" + oData.Batch + "'>View Report</a>");
		}
	},];
	json['headers'].forEach((header, i) => { // add headers to headers arr
		let h = {
			title: header['name'],
              data: header['data'],
              name: header['name'],
              };
        if(i<6 && i>2) {
		// custom html to display preloader if batch in progress
		h["fnCreatedCell"] = function (nTd, sData, oData, iRow, iCol) {
			if(oData.Completion != 100) {
				$(nTd).html('<img src="/img/preloaders/fadingcircles.gif" height="10px"/>');
			} else {
				$(nTd).html(oData[header['name']]);
			};
		};
		};
		headers.push(h);
	});
	

	headers[3]["fnCreatedCell"] = function (nTd, sData, oData, iRow, iCol) {
		if(oData.Completion != 'Processing...') {
		$(nTd).html("<progress value='" + oData.Completion + "' max=\"100\"></progress>");
		} else {
			$(nTd).html(oData.Completion);
		};
	};
		
	let colDefs = [
		{ orderable: false, searchable: false, target: 0, "width": "15%" },
		{ orderable: false, searchable: false, target: 1, visible: false },
		{
			target: 2, "width": "15%", render: function(data, type, row, meta) {
				return "<span id='cellspan-" + meta.row + "'><div id='text-" + meta.row + "' contenteditable='" + editingBatch + "'>" + data + "</div></span>";

			}
		}
	];
	for (i = 3; i < headers.length; i++) {
		colDefs.push({ orderable: false, searchable: false, target: i });
	}
	colDefs[3]["width"] = "20%";
	colDefs[7]['render'] = function(data, type, row, meta) {
		return generateEditConfirmButtonHtml(meta.row) + generateDeleteButtonHtml(meta.row);
	}
	colDefs[8]["width"] = "15%";
	console.log(colDefs);
	let rows = {};
	rows['data'] = json['rows'];
	batchViewTable = $('#' + tablename).DataTable({ // generate table
		//data: json['rows'],
		serverSide: true,
		processing: true,
		pagingType: "full_numbers",
		autoWidth: false,
		dom: 'lrtip',
		ajax: loadTableData,
		cache: true,
		columns: headers,
		columnDefs: colDefs,
		order: [[1, 'desc']], // sort by taskId
		responsive: true,
	});
};

function loadTableData(data, callback, settings) {
	let d;
	let message = {
		baseUrl: HbdApi.baseUrl,
		data: data
	};
	refreshWorker.postMessage(message);
	refreshWorker.addEventListener('message', messageCallback, false);

	function messageCallback(m) {
		if(editingBatch.status) return false;
		refreshWorker.removeEventListener('message', messageCallback);
		d = {
			data: m.data,
			recordsFiltered: recordsTotal,
			recordsTotal: recordsTotal
		};
		current_d = d;
		// do not display processing dots after first table load
		$('.dataTables_processing').css('z-index', '-1');
		callback(d)
	};
};

function generateEditConfirmButtonHtml(rowId) {
	let path, action;
	//console.log('active '+buttonPaths.active);
	if (buttonPaths.active == 0 || editingBatch.row != rowId) {
		path = buttonPaths.edit;
		action = 'edit';
	} else {
		path = buttonPaths.confirm;
		action = 'confirm';
	}
	return '<img src="' + path + '" width="20px" style="cursor: pointer" id="' + action + '-' + rowId + '"/>';
};

function generateDeleteButtonHtml(rowId) {
	return '<img src="/img/button/deletebutton-red-x.png" width="20px" style="cursor: pointer" id="delete-' + rowId + '"/>';
};