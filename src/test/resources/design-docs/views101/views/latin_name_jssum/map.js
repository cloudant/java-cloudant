function(doc) {  
	if(doc.latin_name) {    
		emit(doc.latin_name, doc.latin_name.length);  
	}
}