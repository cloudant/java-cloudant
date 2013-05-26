function(doc, req) {
  if (doc.title == req.query.somekey1) {
    return true;
  } else {
    return false;
  }
}