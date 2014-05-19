function(doc) {
	if(doc.diet) {
		emit(doc.diet, 1);
	}
}