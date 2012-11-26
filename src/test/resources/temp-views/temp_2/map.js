function(doc){
  if(doc.title){
    emit(doc.title, 2);
  }
}