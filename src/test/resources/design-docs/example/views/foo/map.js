function(doc){
  if(doc.title){
    emit(doc.title, doc.position);
  }
}