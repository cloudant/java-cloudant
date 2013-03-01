function(doc, req) {
	var field = req.query.field;
	var value = req.query.value;
	var message = 'set '+field+' to '+value;
	doc[field] = value;
	return [doc, message];
}