
self.addEventListener('message', function(message) {
	let batchData = getBatchView(message.data.baseUrl, message.data.data['start'], message.data.data['length'], getOrder(message.data.data))['rows'];
	//console.log(batchData);
	self.postMessage(batchData);
}, false);


function http_request(url, method, body, encoding) {
	console.log(method + ' ' + url);
	if (body != null) {
		console.log(body);
	}
	var xhr = new XMLHttpRequest();
	xhr.open(method, url, false);
	if (encoding != null) {
		xhr.setRequestHeader('Content-Type', encoding);
	}
	xhr.send(body);
	//console.log(xhr.responseText);

	let json_data = JSON.parse(xhr.responseText);
	return json_data['data'];
}

function get_request(url) {
	return http_request(url, "GET", null);
}

function getBatchView(baseUrl, offset, length, order) {
	let url = new URL(baseUrl + 'batchview');
	url.searchParams.append('offset', offset);
	url.searchParams.append('length', length);
	url.searchParams.append('orderDirection', order);
	return get_request(url);
}

function getOrder(data) {
	if (data['order'][0]['dir'] == 'asc') {
		return 1;
	} else {
		return -1;
	}
};