{
   "_id": "_design/example",
   "language": "javascript",
   "views": {
       "by_tag": {
           "map": "function(doc){\n  if(doc.title && doc.tags){\n    for(var idx in doc.tags){\n      emit(doc.tags[idx], 1);\n    }\n  }\n}\n",
           "reduce": "function(keys, values){\n  return sum(values);\n}\n"
       },
       "boolean_creator_created": {
           "map": "function(doc) {\n  emit([doc.contentArray[0].boolean, doc.contentArray[0].creator,\n  doc.contentArray[0].created], doc);\n}\n"
       },
       "foo": {
           "map": "function(doc){\n  if(doc.Type == 'Foo' && doc.title){\n    emit(doc.title, doc.position);\n  }\n}\n"
       },
       "total_created": {
           "map": "function(doc) {\n emit([doc.contentArray[0].total, doc.contentArray[0].created], null);\n}\n"
       },
       "created_boolean_creator": {
           "map": "function(doc) {\n  emit([doc.contentArray[0].created, doc.contentArray[0].boolean,\n   doc.contentArray[0].creator], [doc]);\n}\n"
       },
       "by_date": {
           "map": "function(doc){\n  if(doc.title && doc.complexDate){\n    emit(doc.complexDate, 1);\n  }\n}\n",
           "reduce": "function(keys, values){\n  return sum(values);\n}\n"
       },
       "quotes_created": {
           "map": "function(doc) {\n emit([doc.contentArray[0].quotes, doc.contentArray[0].created], null);\n}\n"
       },
       "created": {
           "map": "function(doc) {\n emit([doc.contentArray[0].created, doc.contentArray[0].created]);\n}\n"
       },
       "spaces_created": {
           "map": "function(doc) {\n emit([doc.contentArray[0].spaces, doc.contentArray[0].created], null);\n}\n"
       },
       "doc_title": {
           "map": "function(doc){\n  if(doc.Type == 'Foo' && doc.title){\n    emit([doc.title, doc.contentArray[0].boolean], null);\n  }\n}\n"
       },
       "creator_boolean_total": {
           "map": "function(doc) {\n emit([doc.contentArray[0].creator, doc.contentArray[0].boolean,\n doc.contentArray[0].created], [doc.title, doc.contentArray[0].total]);\n}\n"
       },
       "creator_created": {
           "map": "function(doc) {\n emit([doc.contentArray[0].creator, doc.contentArray[0].created], null);\n}\n"
       },
       "boolean": {
           "map": "function(doc) {\n  emit(doc.contentArray[0].boolean, null);\n}\n"
       },
       "created_total": {
           "map": "function(doc) {\n emit([doc.contentArray[0].created, doc.contentArray[0].total], null);\n}\n"
       }
   },
   "validate_doc_update": "function(newDoc, oldDoc, userCtx) {}\n",
   "filters": {
       "example_filter": "function(doc, req) {\n  if (doc.title == req.query.somekey1) {\n    return true;\n  } else {\n    return false;\n  }\n}\n"
   },
   "shows": {
       "example_show_2": "function(doc, req) {\n  return '<h2>Example</h2>';\n}\n",
       "example_show_1": "function(doc, req) {\n  return '<h1>Example</h1>';\n}\n"
   },
   "lists": {
       "example_list": "function(head, req) {}\n"
   },
   "updates": {
       "example_update": "function(doc, req) {\n\tvar field = req.query.field;\n\tvar value = req.query.value;\n\tvar message = 'set '+field+' to '+value;\n\tdoc[field] = value;\n\treturn [doc, message];\n}\n",
       "get-uuid" : "function(doc, req) {\n\t return [null, req.uuid];\n}\n"
   },
   "rewrites": [
       {
           "from": "",
           "to": "index.html",
           "method": "GET",
           "query": {
           }
       }
   ],
   "fulltext": {
       "by_title": {
           "index": "function(doc) { var ret = new Document(); ret.add( doc.title ); return ret; }"
       },
       "by_content": {
           "index": "function(doc) { var ret = new Document(); ret.add( doc.content ); return ret; }"
       }
   }
}
