package org.lightcouch;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helper class for construction of HTTP request URIs.
 * @author Ahmed Yehia
 *
 */
class URIBuilder {
	private String scheme;
	private String host;
	private int port;
	private String path = "";
	private String query;
	
	public static URIBuilder builder() {
		return new URIBuilder();
	}
	
	public static URIBuilder builder(URI uri) {
		URIBuilder builder = URIBuilder.builder()
	  		.scheme(uri.getScheme())
			.host(uri.getHost())
			.port(uri.getPort())
			.path(uri.getPath());
		return builder;
	}
	
	public URI build() {
		try {
			return new URI(scheme, null, host, port, path, query, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public URIBuilder scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}
	
	public URIBuilder host(String host) {
		this.host = host;
		return this;
	}
	
	public URIBuilder port(int port) {
		this.port = port;
		return this;
	}
	
	public URIBuilder path(String path) {
		this.path += path;
		return this;
	}
	
	public URIBuilder query(String key, String value) {
		this.query = (value != null) ? String.format("%s=%s", key, value) : null;
		return this;
	}
	
	public URIBuilder query(String query) {
		this.query = query;
		return this;
	}
}
