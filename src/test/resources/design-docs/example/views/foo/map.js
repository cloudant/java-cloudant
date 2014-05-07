function(doc){
  if(doc.Type == 'Foo' && doc.title){
    emit(doc.title, doc.position);
  }
}