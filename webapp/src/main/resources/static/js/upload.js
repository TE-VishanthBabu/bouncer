
var uploadsTable;
var progress = {};
var json = {
	headers: [
		{title: 'Filename', data: 'Filename'},
		{title: 'Progress', data: 'Progress', fnCreatedCell: function (nTd, sData, oData, iRow, iCol) {
			$(nTd).html("<progress value='" + oData.Progress + "' max=\""+oData.total+"\"></progress>");
		}},
		{title: 'total', data: 'total'}
	],
	colDefs: [
		{target: 2, visible: false}
	],
	rows: []
};

$(document).ready(function () { 
	
	function onFile() {
		
		var files = $("#upload").prop("files");
        
        $.each(files, async function(idx, file) {
			// console.log(file);
			// do not restart if upload is currently in progress
			if (file.name in progress && progress[file.name].status != 'fail') {
				return;
	        }
			if (file.size < (21474836480)) {
	            upload.parentNode.className = 'area'
	        } else {
	            window.alert('File size is too large, please ensure you are uploading a file of less than 20GB');
	        };
			let filename = file.name;
			progress[filename] = {
				current: 0,
				total: Math.ceil(file.size/chunkSize),
				status: 'success'
			};
  			let worker = new Worker(new URL('./js/workers/uploadWorker.js', window.location.origin));
  			let message = {
				file: file,
				baseUrl: window.location.origin
			};
			
  			worker.postMessage(message);
  			worker.addEventListener('message', function(e) {
				console.log('Worker said: ', e);
				progress[e.data.filename].current = e.data.current;
				progress[e.data.filename].status = e.data.status;
				if(e.data.status != 'success') {
					worker.terminate();
					alert('Failed to upload file ' + file.name + '. Please try again');
				};
			//console.log('final progress');
			//console.log(progress[e.data.filename]);
			}, false);
		});
		$('#file-form').trigger('reset');
};
        
    uploadsTable = createUploadsTable();
    
    // update every second
    setInterval(updateUploadsTable, 1000);
        
    var upload = document.getElementById('upload');
    upload.addEventListener('dragenter', function (e) {
    	upload.parentNode.className = 'area dragging';
	}, false);
	
	upload.addEventListener('dragleave', function (e) {
	    upload.parentNode.className = 'area';
	}, false);
	
	upload.addEventListener('dragdrop', function (e) {
	    onFile();
	}, false);
	
	upload.addEventListener('change', function (e) {
	    onFile();
	}, false);
	
	// block user from leaving page if download in progress
	let block = false;
	$(window).on('beforeunload', function(){
	  	$.each(progress, function(filename, fileProgress) {
		console.log(fileProgress.current + ' ' + fileProgress.total + ' ' + fileProgress.status);
			if(fileProgress.current != fileProgress.total && fileProgress.status != 'fail') {
				block = true;
			}
		});
		if(block) return '';
	});
	$(window).on('unload', function(){
	  	$.each(progress, function(filename, fileProgress) {
		console.log(fileProgress.current + ' ' + fileProgress.total + ' ' + fileProgress.status);
			if(fileProgress.current != fileProgress.total && fileProgress.status != 'fail') {
				block = true;
			}
		});
		if(block) return '';
	});
 });
  
 
function updateUploadsTable() {
	json.rows = [];
	$.each(progress, function(filename, fileProgress) {
		let row = {Filename: filename, Progress: fileProgress.current, total: fileProgress.total};
		json.rows.push(row);
	});
	uploadsTable.ajax.reload();
};
  
  
function createUploadsTable() {
	return $('#uploadsTable').DataTable({
		dom: 'frti',
    	searching: false,
		columns: json.headers,
		columnDefs: json.colDefs,
        responsive: true,
        ajax: function (data, callback, settings) {
				let d = {
					data: json.rows,
					recordsFiltered: json.rows.length,
					recordsTotal: json.rows.length
				};
				callback(d);
    	},
    	language: {
        	"emptyTable": "Upload progress will display here"
    	}
	});
};


/* async
function uploadFile(file) {
	console.log("uploading file "+file.name);
	let chunkNum = 0;
	let ref = uuidv4();
	for(let start = 0; start < file.size; start+=chunkSize) {
		const chunk = file.slice(start, start+chunkSize);
		console.log("chunk "+chunkNum+" size "+chunk.size);	
		let lastChunk = start+chunkSize>file.size;
		
		const fd = new FormData()
	  	fd.set('chunk', chunk)
	  	let url = new URL(uploadUrl);
	  	url.searchParams.append('chunkNum', chunkNum);
      	url.searchParams.append('filename', file.name);
      	url.searchParams.append('ref', ref);
      	// console.log(chunkNum+' last chunk? '+ lastChunk);
      	url.searchParams.append('lastChunk', lastChunk);
	  	//let api = new HbdApi();
	  	
		(async () => {
		  const rawResponse = await fetch(url, {
		    method: 'POST',
		    headers: {
		      'Accept': 'application/json',
		    },
		    body: fd
		  });
		  
		  console.log(rawResponse);
		})().then(function() {
			let p = progress[file.name];
			if(p.current < chunkNum) {
				p.current = chunkNum;
			};
			if(chunkNum==p.total) {
				p.merged = true;
			};
			progress[file.name] = p;
			console.log("progress");
			console.log(progress);
		});

      chunkNum++;
	};
};*/

function uuidv4() {
  return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
    (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
  );
}