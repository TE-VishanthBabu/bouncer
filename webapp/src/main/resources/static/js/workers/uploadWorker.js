const chunkSize = 6291456;
const uploadUrl = "/upload/chunk";

function uploadFile(data) {
	//console.log(data);
	let file = data.file;
	//console.log(file.size);
	// chunk indices start at 0
	let chunkNum = 0;
	//let ref = uuidv4();
	for(let start = 0; start < file.size; start+=chunkSize) {
		const chunk = file.slice(start, start+chunkSize);
		//console.log("chunk "+chunkNum+" size "+chunk.size);	
		let lastChunk = start+chunkSize>file.size;
		
		const fd = new FormData();
	  	fd.set('chunk', chunk);
	  	let url = new URL(data.baseUrl + uploadUrl);
	  	url.searchParams.append('chunkNum', chunkNum);
      	url.searchParams.append('filename', file.name);
      	//url.searchParams.append('ref', ref);
      	// console.log(chunkNum+' last chunk? '+ lastChunk);
      	url.searchParams.append('lastChunk', lastChunk);
      	
      	let res;
      	let status;
      	try {
      		res = post_request(url, fd, null);
      		//console.log(res);
      		status = res.status;
      	} catch(err) {
			console.log(err.message);
		    status = 'fail';
		};
		
      	let current = chunkNum+1;
		chunkNum++;
		let returnMessage = {
			filename: file.name,
			current: current,
			status: status
		};
		//console.log(data);
		self.postMessage(returnMessage);
		if(returnMessage.status == 'fail') {
			return false;
		};
    };
}

self.addEventListener('message', function(message) {
	uploadFile(message.data);
}, false);


function http_request(url, method, body, encoding) {
		console.log(method + ' ' + url);
		var xhr = new XMLHttpRequest();
		xhr.open(method, url, false);
		if(encoding != null) {
			xhr.setRequestHeader('Content-Type', encoding);
		}
		xhr.send(body);
		//console.log(xhr.responseText);

		let json_data = JSON.parse(xhr.responseText);
		return json_data['data'];
	}
	
function post_request(url, body, encoding) {
		return this.http_request(url, "POST", body, encoding);
	}