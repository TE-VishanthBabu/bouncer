
class HbdApi {
	static baseUrl = window.location.origin + "/proxy/";
	constructor() {
	}
	
	http_request(url, method, body, encoding) {
		console.log(method + ' ' + url);
		if(body != null) {
			console.log(body);
		}
		var xhr = new XMLHttpRequest();
		xhr.open(method, url, false);
		if(encoding != null) {
			xhr.setRequestHeader('Content-Type', encoding);
		}
		xhr.send(body);
		console.log(xhr.responseText);

		let json_data = JSON.parse(xhr.responseText);
		return json_data['data'];
	}

	get_request(url) {
		return this.http_request(url, "GET", null);
	}

	post_request(url, body) {
		return this.http_request(url, "POST", body, 'application/x-www-form-urlencoded;charset=UTF-8');
	}
	
	post_request(url, body, encoding) {
		return this.http_request(url, "POST", body, encoding);
	}
	
	delete_request(url) {
		return this.http_request(url, "DELETE", null);
	}
	
	put_request(url) {
		return this.http_request(url, "PUT", null);
	}

	getBatchView(offset, length, order) {
		let url = new URL(HbdApi.baseUrl + 'batchview');
		url.searchParams.append('offset', offset);
		url.searchParams.append('length', length);
		url.searchParams.append('orderDirection', order);
		return this.get_request(url);
	}

	getTaskView(batch, offset, length, order, orderColumn) {
		let url = new URL(HbdApi.baseUrl + batch + '/taskview');
		url.searchParams.append('offset', offset);
		url.searchParams.append('length', length);
		url.searchParams.append('orderDirection', order);
		if(orderColumn!=null)
		{
			url.searchParams.append('orderColumn', orderColumn);
		}
		return this.get_request(url);
	};

	getFileView(task, offset, length, order, orderColumn, query) {
		let url = new URL(HbdApi.baseUrl + task + '/filesubmissionsview');
		url.searchParams.append('offset', offset);
		url.searchParams.append('length', length);
		url.searchParams.append('orderDirection', order);
		if(orderColumn!=null)
		{
			url.searchParams.append('orderColumn', orderColumn);
		}
		if(query!=null)
		{
			url.searchParams.append('query', query);
		}
		return this.get_request(url);
	}

	getIndicatorView(siAnalysisId) {
		let url = new URL(HbdApi.baseUrl + siAnalysisId + '/indicatorview');
		return this.get_request(url);
	};

	getFileDetailsView(fileAttributeId) {
		let url = new URL(HbdApi.baseUrl + fileAttributeId + '/filedetailsview');
		return this.get_request(url);
	};

	getFilesView() {
		let url = new URL(HbdApi.baseUrl + 'filesview');
		return this.get_request(url);
	};

	submitView(name, files, obfuscate, sanitize) {
		let url = new URL(HbdApi.baseUrl + 'submitview');
		url.searchParams.append('files', files);
		url.searchParams.append('obfuscate', obfuscate);
		url.searchParams.append('sanitize', sanitize);
		url.searchParams.append('name', name);
		let body = {files: files};
		return this.post_request(url, this.createUrlEncodedBody(body));
	};
	
	getMailboxesView(offset, length) {
		let url = new URL(HbdApi.baseUrl + 'graph/mailboxes');
		url.searchParams.append('offset', offset);
		url.searchParams.append('length', length);
		return this.get_request(url);
	}
	
	graphSubmitView(mailboxes, until, days, obfuscate, sanitize, name) {
		let url = new URL(HbdApi.baseUrl + 'graph/graphsubmitview');
		let body = {'mailboxes': mailboxes};
		url.searchParams.append('until', until);
		url.searchParams.append('days', days);
		url.searchParams.append('obfuscate', obfuscate);
		url.searchParams.append('sanitize', sanitize);
		url.searchParams.append('name', name);
		return this.post_request(url, this.createUrlEncodedBody(body));
	}

	filetypeSummaryView(batch) {
		let url = new URL(HbdApi.baseUrl + batch + '/filetypeSummaryView')
		return this.get_request(url);
	}
	
	async delete(entity, id) {
		let url = new URL(HbdApi.baseUrl + entity + '/' + id + '/delete');
		this.delete_request(url);
	}
	
	put(entity, field, id, value) {
		let url = new URL(HbdApi.baseUrl + entity + '/' + id + '/' + field);
		url.searchParams.append('value', value);
		return this.put_request(url);
	}
	
	getOrder(data)
	{
		if(data['order'][0]['dir'] == 'asc')
		{
			return 1;
		} else {
			return -1;
		}
	};
	
	getOrderColumn(data)
	{
		console.log(data['columns']);
		console.log(data['order'][0]['column']);
		console.log(data['columns'][data['order'][0]['column']]['name']);
		return data['columns'][data['order'][0]['column']]['name'];
	};
	
	getQuery(data)
	{
		return data['search']['value'];
	}
	
	createUrlEncodedBody(data) {
		var formBody = [];
		for (var property in data) {
		  var encodedKey = encodeURIComponent(property);
		  var encodedValue = encodeURIComponent(data[property]);
		  formBody.push(encodedKey + "=" + encodedValue);
		}
		formBody = formBody.join("&");
		return formBody;
	}

}