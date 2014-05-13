{
	"by_title": {
	      "index": "function(doc) { var ret = new Document(); ret.add( doc.title ); return ret; }"
	},
	"by_content": {
		"index": "function(doc) { var ret = new Document(); ret.add( doc.content ); return ret; }"
	}
}