package com.cloudant.client.org.lightcouch.internal;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonHelper {

	/**
	 * Builds {@link Gson} and registers any required serializer/deserializer.
	 * @return {@link Gson} instance
	 */
	public static GsonBuilder initGson(GsonBuilder gsonBuilder) {
		gsonBuilder.registerTypeAdapter(JsonObject.class, new JsonDeserializer<JsonObject>() {
			public JsonObject deserialize(JsonElement json,
					Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				return json.getAsJsonObject();
			}
		});
		gsonBuilder.registerTypeAdapter(JsonObject.class, new JsonSerializer<JsonObject>() {
			public JsonElement serialize(JsonObject src, Type typeOfSrc,
					JsonSerializationContext context) {
				return src.getAsJsonObject();
			}
			
		});
		
		return gsonBuilder;
	}
}
