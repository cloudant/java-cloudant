{
  "ddoc": "_design/testindexddoc",
  "name": "simpleselector",
  "type": "text",
  "def": {
    "default_analyzer": "keyword",
    "default_field": {},
    "selector": {
      "year": {
        "$gt": 2010
      }
    },
    "fields": [{
      "Movie_name": "string"
    }],
    "index_array_lengths": true
  }
}
