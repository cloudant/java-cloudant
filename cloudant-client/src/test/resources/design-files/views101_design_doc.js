{
  "_id": "_design/views101",
  "language": "javascript",
  "views": {
    "latin_name_jssum": {
      "map": "function(doc) {  \n\tif(doc.latin_name) {    \n\t\temit(doc.latin_name, doc.latin_name.length);  \n\t}\n}\n",
      "reduce": "function (key, values, rereduce) {  \n\treturn sum(values);\n}\n"
    },
    "latin_name": {
      "map": "function(doc) { \n\tif(doc.latin_name){\n\t\temit(doc.latin_name, doc.latin_name.length);\n\t}\n}\n"
    },
    "diet_sum": {
      "map": "function(doc) {\n\tif(doc.diet) {\n\t\temit(doc.diet, 1);\n\t}\n}\n",
      "reduce": "_sum\n"
    }
  },
  "indexes": {
    "animals": {
      "index": "function(doc) { index(\"default\", doc._id); if(doc.min_length) {   index(\"min_length\", doc.min_length, {\"store\": \"yes\",\"facet\": true});  } if(doc.diet){   index(\"diet\", doc.diet, {\"store\": \"yes\",\"facet\": true});  }  if (doc.latin_name){    index(\"latin_name\", doc.latin_name, {\"store\": \"yes\"}); }  if (doc['class']){    index(\"class\", doc['class'], {\"store\": \"yes\",\"facet\": true});  }}"
    }
  }
}
