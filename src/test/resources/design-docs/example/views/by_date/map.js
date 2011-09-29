function(doc){
  if(doc.title && doc.complexDate){
    emit(doc.complexDate, 1);
  }
}