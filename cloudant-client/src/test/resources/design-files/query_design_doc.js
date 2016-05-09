{
 "_id": "_design/testQuery",
 "language": "query",
 "views": {
  "testView": {
   "map": {"fields": {"Person_dob": "asc"}},
   "reduce": "_count",
   "options": {
    "def": {
     "fields": [
      "Person_dob"
     ]
    }
   }
  }
 }
}
