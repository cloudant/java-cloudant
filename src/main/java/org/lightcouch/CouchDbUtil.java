package org.lightcouch;

import static java.lang.String.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

import org.apache.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Provides various utility methods, for internal use.
 * @author Ahmed Yehia
 */
final class CouchDbUtil {

	private CouchDbUtil() {
		// Utility class
	}
	
	public static void assertNotEmpty(Object object, String prefix) throws IllegalArgumentException {
		if(object == null) {
			throw new IllegalArgumentException(format("%s may not be null.", prefix));
		} else if(object instanceof String && ((String)object).length() == 0) {
			throw new IllegalArgumentException(format("%s may not be empty.", prefix));
		} 
	}
	
	public static void assertNull(Object object, String prefix) throws IllegalArgumentException {
		if(object != null) {
			throw new IllegalArgumentException(format("%s should be null.", prefix));
		} 
	}
	
	public static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	public static String removeExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}
	
	// ------------------------------------------------------- JSON

	public static JsonObject objectToJson(Gson gson, Object object) {
		if(object instanceof JsonObject) {
			return (JsonObject) object;
		}
		return gson.toJsonTree(object).getAsJsonObject();
	}
	
	public static <T> T JsonToObject(Gson gson, JsonElement elem, String key, Class<T> classType) {
		return gson.fromJson(elem.getAsJsonObject().get(key), classType);
	}

	/**
	 * @return A JSON element as a String, or null if not found.
	 */
	public static String getElement(JsonObject j, String e) {
		return (j.get(e) == null) ? null : j.get(e).getAsString();  
	}
	
	public static long getElementAsLong(JsonObject j, String e) {
		return (j.get(e) == null) ? 0L : j.get(e).getAsLong();
	}
	
	public static int getElementAsInt(JsonObject j, String e) {
		return (j.get(e) == null) ? 0 : j.get(e).getAsInt();
	}
	
	// ----------------------------------------------------- Streams
	
	private static final String LINE_SEP = System.getProperty("line.separator");

	public static String readFile(File file) {
		StringBuilder content = new StringBuilder((int)file.length());
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			while(scanner.hasNextLine()) {        
				content.append(scanner.nextLine() + LINE_SEP);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} finally {
			scanner.close();
		}
		return content.toString();
	}
	
	public static URL getURL(String resource) {
		return Thread.currentThread().getContextClassLoader().getResource(resource);
	}
	
	  /**
	 * Closes the response input stream.
	 * 
	 * @param response The {@link HttpResponse}
	 */
	public static void close(HttpResponse response) {
		try {
			response.getEntity().getContent().close();
		} catch (Exception e) {}
	}
	
	/**
	 * Closes an input stream.
	 * 
	 * @param instream The {@link InputStream}
	 */
	public static void close(InputStream instream) {
		try {
			instream.close();
		} catch (Exception e) {}
	}
}
