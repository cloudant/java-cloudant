/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch.internal;

import static java.lang.String.format;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.lightcouch.CouchDbException;
import org.lightcouch.Response;

import com.cloudant.client.api.model.Permissions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Provides various utility methods, for internal use.
 * @author Ahmed Yehia
 */
final public class CouchDbUtil {

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
	
	// JSON
	
	public static <T> T JsonToObject(Gson gson, JsonElement elem, String key, Class<T> classType) {
		return gson.fromJson(elem.getAsJsonObject().get(key), classType);
	}

	/**
	 * @return A JSON element as a String, or null if not found.
	 */
	public static String getAsString(JsonObject j, String e) {
		return (j.get(e) == null) ? null : j.get(e).getAsString();  
	}
	
	/**
	 * @return A JSON element as <code>long</code>, or <code>0</code> if not found.
	 */
	public static long getAsLong(JsonObject j, String e) {
		return (j.get(e) == null) ? 0L : j.get(e).getAsLong();
	}
	
	/**
	 * @return A JSON element as <code>int</code>, or <code>0</code> if not found.
	 */
	public static int getAsInt(JsonObject j, String e) {
		return (j.get(e) == null) ? 0 : j.get(e).getAsInt();
	}
	
	// Files
	
	private static final String LINE_SEP = System.getProperty("line.separator");
	
	/**
	 * List directory contents for a resource folder. Not recursive.
	 * This is basically a brute-force implementation.
	 * Works for regular files and also JARs.
	 * 
	 * @author Greg Briggs (ah!!! i Mario Briggs, need to meet this Greg guy... never wrote code with another Briggs)
	 * @param clazz Any java class that lives in the same place as the resources you want.
	 * @param path Should end with "/", but not start with one.
	 * @return Just the name of each member item, not the full paths.
	 */
	public static List<String> listResources(String path)  {
		try {
			Class<CouchDbUtil> clazz = CouchDbUtil.class;
			URL dirURL = clazz.getClassLoader().getResource(path);
			if (dirURL != null && dirURL.getProtocol().equals("file")) {
				return Arrays.asList(new File(dirURL.toURI()).list());
			}
			if (dirURL != null && dirURL.getProtocol().equals("jar")) {
				String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); 
				JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
				Enumeration<JarEntry> entries = jar.entries(); 
				Set<String> result = new HashSet<String>(); 
				while(entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.startsWith(path)) { 
						String entry = name.substring(path.length());
						int checkSubdir = entry.indexOf("/");
						if (checkSubdir >= 0) {
							entry = entry.substring(0, checkSubdir);
						}
						if(entry.length() > 0) {
							result.add(entry);
						}
					}
				}
				jar.close(); //mdb
				return new ArrayList<String>(result);
			} 
			return null;
		} catch (Exception e) {
			throw new CouchDbException(e);
		}
	}
	
	public static String readFile(String path) {
		InputStream instream = CouchDbUtil.class.getResourceAsStream(path);
		StringBuilder content = new StringBuilder();
		Scanner scanner = null;
		try {
			scanner = new Scanner(instream);
			while(scanner.hasNextLine()) {        
				content.append(scanner.nextLine() + LINE_SEP);
			}
		} finally {
			scanner.close();
		}
		return content.toString();
	}
	
	/**
	 * @return {@link InputStream} of {@link HttpResponse}
	 */
	public static InputStream getStream(HttpResponse response) {
		try { 
			return response.getEntity().getContent();
		} catch (Exception e) {
			throw new CouchDbException("Error reading response. ", e);
		}
	}
	
	public static String removeExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}
	
	public static String streamToString(InputStream in) {
	    Scanner s = new Scanner(in);
	    s.useDelimiter("\\A");
	    String str = s.hasNext() ? s.next() : null;
	    close(in);
	    s.close();// mdb
	    return str;
	}
	
	/**
	 * Closes the response input stream.
	 * 
	 * @param response The {@link HttpResponse}
	 */
	public static void close(HttpResponse response) {
		try {
			close(response.getEntity().getContent());
		} catch (Exception e) {}
	}
	
	/**
	 * Closes a resource.
	 * 
	 * @param c The {@link Closeable} resource.
	 */
	public static void close(Closeable c) {
		try {
			c.close();
		} catch (Exception e) {}
	}
	
	
	
	/**
	 * @return A JSON element as a String, or null if not found, from the response
	 */
	public static String getAsString(HttpResponse response, String e) {
		InputStream instream = null;
		
		try {
			instream = getStream(response);
			Reader reader = new InputStreamReader(instream, "UTF-8");
			return getAsString(new JsonParser().parse(reader).getAsJsonObject(), e);
		} catch (UnsupportedEncodingException e1) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e1);
		}
		finally {
			close(instream);
		}
		
		
	}
	
	/**
	 * create a HTTP POST request.
	 * @return {@link HttpResponse}
	 */
	public static HttpPost createPost(URI uri, String body, String contentType) {
		HttpPost post = new HttpPost(uri);
		setEntity(post, body, contentType);
		return post;
	}
	
	/**
	 * Sets a JSON String as a request entity.
	 * @param httpRequest The request to set entity.
	 * @param json The JSON String to set.
	 */
	public static void setEntity(HttpEntityEnclosingRequestBase httpRequest, String body, String contentType) {
		StringEntity entity = new StringEntity(body, "UTF-8");
		entity.setContentType(contentType);
		httpRequest.setEntity(entity);
	}
	
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	public static  <T> List<T> getResponseList(HttpResponse response, Gson gson, Class<T> klazz, Type typeofT) throws CouchDbException {
		InputStream instream = getStream(response);
		try {
			Reader reader = new InputStreamReader(instream, "UTF-8");
			return gson.fromJson(reader,typeofT);
		} catch (UnsupportedEncodingException e) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	public static <T> T getResponse(HttpResponse response, Class<T> classType, Gson gson) throws CouchDbException {
		try {
			InputStreamReader reader = new InputStreamReader(getStream(response), "UTF-8");
			return gson.fromJson(reader, classType);
		} catch (UnsupportedEncodingException e) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e);
		}
	}
	 
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	public static  Map<String,EnumSet<Permissions>> getResponseMap(HttpResponse response, Gson gson, Type typeofT) throws CouchDbException {
		InputStream instream = getStream(response);
		try {
			Reader reader = new InputStreamReader(instream, "UTF-8");
			return gson.fromJson(reader,typeofT);
		} catch (UnsupportedEncodingException e) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e);
		}
	}
}
