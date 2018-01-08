{
  "ddoc": "_design/testindexddoc",
  "name": "complexjson",
  "type": "json",
  "def": {
    "partial_filter_selector": {
      "year": {
        "$gt": 2010
      }
    },
    "fields": [{
      "Person_name": "asc"
    }, {
      "Movie_year": "desc"
    }]
  }
}
