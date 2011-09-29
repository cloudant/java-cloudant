function(doc){
  if(doc.title && doc.tags){
    for(var idx in doc.tags){
      emit(doc.tags[idx], 1);
    }
  }
}