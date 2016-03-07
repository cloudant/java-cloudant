function(doc){
  if(doc.title){
    emit(doc.title, 1);
  }
}