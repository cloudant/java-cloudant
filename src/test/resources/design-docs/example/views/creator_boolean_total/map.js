function(doc) {
 emit([doc.contentArray[0].creator, doc.contentArray[0].boolean,
 doc.contentArray[0].created], [doc.title, doc.contentArray[0].total]);
}
