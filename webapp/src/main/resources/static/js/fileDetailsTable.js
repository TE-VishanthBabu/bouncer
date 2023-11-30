/**
 * 
 */
 
 var MYLIBRARY = MYLIBRARY || (function(){
    var _args = {}; // private

    return {
        init : function(Args) {
            _args = Args;
            // some other initialising
        },
        helloWorld : function(i) {
            return _args[i];
        }
    };
}());

var hbdApi = new HbdApi();
var indicatorTable = null;
var fileDetailsTable = null;
var fileAnalysisTable = null;
var sanitizeAnalysisTable = null;
var sanitizeIndicatorTable = null;
var sha256 = null;
var fileAttributeId = null;

var verdictColors = {
	'Clean': '#50C878',
	'Suspicious': '#FFBF00',
	'Malicious': '#FF3131'
}
 
$(document).ready(function () {
	document.body.classList.add('sb-sidenav-toggled');
	fileAttributeId = MYLIBRARY.helloWorld(0);
	sha256 = MYLIBRARY.helloWorld(1);
	let FDVJson = hbdApi.getFileDetailsView(fileAttributeId);
	let detailsJson = FDVJson['details'];
    let analysisJson = FDVJson['analysis'];
    let sanitizeAnalysisJson = FDVJson['sanitizeAnalysis'];

	let fileDetailsTableHtml = generateFileDetailsTable(detailsJson);
    let fileAnalysisTableHtml = generateFileDetailsTable(analysisJson);
    let sanitizeAnalysisTableHtml = null;
    if(sanitizeAnalysisJson != null) {
    	sanitizeAnalysisTableHtml = generateFileDetailsTable(sanitizeAnalysisJson);
    };

    let detailsTbl = document.getElementById("fileDetailsTable");
    detailsTbl.innerHTML = fileDetailsTableHtml;

	let analysisTbl = document.getElementById("fileAnalysisTable");
    analysisTbl.innerHTML = fileAnalysisTableHtml;

	fileDetailsTable = $('#fileDetailsTable').DataTable({ // generate table
        responsive: true,
        dom: 't',
        sorting: false
});

        fileAnalysisTable = $('#fileAnalysisTable').DataTable({ // generate table
            responsive: true,
            dom: 't',
            sorting: false
    });
    
    let verdictHeading = document.getElementById('verdict'),
	verdict = analysisJson['metadata']['verdict'];
	verdictHeading.innerHTML = 'Verdict: '+verdict;
	verdictHeading.style.color = verdictColors[verdict];
    
    let siAnalysisIdS = analysisJson['metadata']['siAnalysisIdS'];
	console.log(siAnalysisIdS);
	let iVJson = hbdApi.getIndicatorView(siAnalysisIdS); // get indicator data
	indicatorTable = generateIndicatorTable(iVJson, 'indicatorTable');
    
    console.log(sanitizeAnalysisJson);
    if(sanitizeAnalysisJson != null) {
	let sanitizeCard = document.getElementById("sanitizeCard");
	sanitizeCard.removeAttribute("hidden");
	
	let sanitizedVerdictHeading = document.getElementById('sanitizeVerdict'),
	sanitizedVerdict = sanitizeAnalysisJson['metadata']['verdict'];;
	sanitizedVerdictHeading.innerHTML = 'Verdict: '+sanitizedVerdict;
	sanitizedVerdictHeading.style.color = verdictColors[sanitizedVerdict];
	
	    let sanitizeAnalysisTbl = document.getElementById("sanitizeAnalysisTable");
	    sanitizeAnalysisTbl.innerHTML = sanitizeAnalysisTableHtml;
	        sanitizeAnalysisTable = $('#sanitizeAnalysisTable').DataTable({ // generate table
	            responsive: true,
	            dom: 't',
	            sorting: false
	    });
	    let sanitizeSiAnalysisIdS = sanitizeAnalysisJson['metadata']['siAnalysisIdS'];
	console.log(sanitizeSiAnalysisIdS);
	let sanitizeIVJson = hbdApi.getIndicatorView(sanitizeSiAnalysisIdS); // get indicator data
	sanitizeIndicatorTable = generateIndicatorTable(sanitizeIVJson, 'sanitizeIndicatorTable');
    };

	
});

/* Formatting function for row details - modify as you need */
function generateFileDetailsTable(fDVJson) {
    // `d` is the original data object for the row
    let rows = fDVJson['rows'];
    let headers = fDVJson['headers'];
	var table = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;"><thead><tr><th></th><th></th></tr></thead><tbody>';
    headers.forEach((header, i) => {
        table += '<tr>' +
        '<td><strong>'+
        header+
        ':</strong></td>' +
        '<td>' +
        rows[header] +
        '</td>' +
        '</tr>';
        });
    table += '</tbody>';
        return table;
}

function generateIndicatorTable(json, tablename) {
	var headers = []; 
	json['headers'].forEach((header, i) => { // add headers to headers arr
		let h = {title: header,
              data: header};
		headers.push(h);
	});
    var table = $('#'+tablename).DataTable({ // generate table
    dom: '<"title"<"filter"f>>rltip',
	columns: headers,
        data: json['rows'],
        order: [[1, 'desc']], // sort by taskId
        responsive: true
});
	return table;
};