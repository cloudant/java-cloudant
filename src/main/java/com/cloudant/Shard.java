package com.cloudant;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class Shard {

	private String range;
	private List<String> nodes;
	
	/**
	 * @return the range
	 */
	public String getRange() {
		return range;
	}
	/**
	 * @return the nodeNames
	 */
	public Iterator<String> getNodes() {
		return nodes.iterator();
	}
	
	Shard(String range, List<String> nodes) {
		super();
		this.range = range;
		this.nodes = nodes;
	}
}

class ShardDeserializer implements JsonDeserializer<List<Shard>> {

	@Override
	public List<Shard> deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		
		final List<Shard> shards = new ArrayList<Shard>();
		
		final JsonObject jsonObject = json.getAsJsonObject();
		Set<Map.Entry<String,JsonElement>> shardsObj = jsonObject.get("shards").getAsJsonObject().entrySet();
		
		for ( Entry<String,JsonElement> entry : shardsObj ) {
			String range = entry.getKey();
			List<String> nodeNames = context.deserialize(entry.getValue(), new TypeToken<List<String>>(){}.getType());
			shards.add( new Shard(range,nodeNames) );
		}
		
		return shards;
	}
  }
