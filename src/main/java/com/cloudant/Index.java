package com.cloudant;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.cloudant.IndexField.SortOrder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Encapsulates a Cloudant Index definition
 * @since 0.0.1
 * @author Mario Briggs
 *
 */
public class Index {

	private String ddoc;
	private String name;
	private String type;
	private List<IndexField> def = new ArrayList<IndexField>();
	
	/**
	 * @return the designDocId
	 */
	public String getDesignDocId() {
		return ddoc;
	}
	
	
	/**
	 * @return the index name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * @return the index type e.g. json
	 */
	public String getType() {
		return type;
	}
	
	
	/**
	 * @return the IndexFields
	 */
	public Iterator<IndexField> getFields() {
		return def.iterator();
	}
	
	public String toString() {
		String index =  "ddoc: " + ddoc + ", name: " + name + ", type: " + type + ", fields: [" ;
		Iterator<IndexField> flds = getFields();
		while ( flds.hasNext()) {
			index += flds.next().toString() + ",";
		}
		index += "]";
		return index;
	}
	
	
	Index(String designDocId, String name, String type) {
		this.ddoc = designDocId;
		this.name = name;
		this.type = type;
	}
	
	void addIndexField(String fieldName, SortOrder order) {
		def.add(new IndexField(fieldName, order));
	}
}


class IndexDeserializer implements JsonDeserializer<List<Index>> {

	@Override
	public List<Index> deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		
		final List<Index> indices = new ArrayList<Index>();
		
		final JsonObject jsonObject = json.getAsJsonObject();
		JsonArray indArray = jsonObject.get("indexes").getAsJsonArray();
		for ( int i = 0; i < indArray.size(); i++ ) {
			JsonObject ind = indArray.get(i).getAsJsonObject();
			String ddoc = null;
			if ( !ind.get("ddoc").isJsonNull() ) { // ddoc is optional
				ddoc = ind.get("ddoc").getAsString();
			}
			Index idx = new Index(ddoc,ind.get("name").getAsString(),
								ind.get("type").getAsString());
			JsonArray fldArray = ind.get("def").getAsJsonObject().get("fields").getAsJsonArray();
			for ( int j = 0; j < fldArray.size(); j++ ) {
				Set<Map.Entry<String,JsonElement>>  fld = fldArray.get(j).getAsJsonObject().entrySet();
				for ( Entry<String,JsonElement> entry : fld ) {
					idx.addIndexField(entry.getKey(),
								SortOrder.valueOf(entry.getValue().getAsString())
								);
				}
			}//end fldArray
			indices.add(idx);
			
		}// end indexes
		
		return indices;
	}
  }
