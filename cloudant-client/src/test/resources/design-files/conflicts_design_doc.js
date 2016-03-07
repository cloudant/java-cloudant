{
  "_id": "_design/conflicts",
  "language": "javascript",
  "views": {
    "conflict": {
      "map": "function(doc){ if(doc._conflicts) emit(doc._conflicts, null);}\n"
    }
  }
}
