{
  "ddoc": "_design/testindexddoc",
  "name": "complextext",
  "type": "text",
  "def": {
    "default_analyzer": {
      "name": "perfield",
      "default": "english",
      "fields": {
        "spanish": "spanish",
        "german": "german"
      }
    },
    "default_field": {
      "enabled": true,
      "analyzer": "spanish"
    },
    "partial_filter_selector": {
      "year": {
        "$gt": 2010
      }
    },
    "fields": [{
      "Movie_name": "string"
    }, {
      "Movie_runtime": "number"
    }, {
      "Movie_wonaward": "boolean"
    }],
    "index_array_lengths": true
  }
}
