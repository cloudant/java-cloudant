function(doc) {
  emit(doc.contentArray[0].created, null);
}