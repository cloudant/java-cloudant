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

package com.cloudant.client.org.lightcouch.internal;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.cloudant.client.org.lightcouch.Params;

/**
 * Helper class for construction of HTTP request URIs.
 * @since 0.0.2
 * @author Ahmed Yehia
 * 
 */
public class URIBuilder {
	private String scheme;
	private String host;
	private int port;
	private String path = "";
	private String pathToEncode = "";
	/* The final query */
	private final StringBuilder query = new StringBuilder();
	/* key=value params */
	private final List<String> qParams = new ArrayList<String>();

	public static URIBuilder buildUri() {
		return new URIBuilder();
	}

	public static URIBuilder buildUri(URI uri) {
		URIBuilder builder = URIBuilder.buildUri().scheme(uri.getScheme())
				.host(uri.getHost()).port(uri.getPort()).path(uri.getPath());
		return builder;
	}

	public URI build() {
		try {
			for (int i = 0; i < qParams.size(); i++) {
				String amp = (i != qParams.size() - 1) ? "&" : "";
				query.append(qParams.get(i) + amp);
			}
			String q = (query.length() == 0) ? null : query.toString();
			return new URI(scheme, null, host, port, path, q, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public URI buildEncoded() {
		for (int i = 0; i < qParams.size(); i++) {
			String amp = (i != qParams.size() - 1) ? "&" : "";
			query.append(qParams.get(i) + amp);
		}
		try {
			String q = (query.length() == 0) ? "" : "?" + query;
			String uri = String.format("%s://%s:%s%s%s%s", new Object[]{scheme, host, port, path, pathToEncode, q});
			return new URI(uri);
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
	
	public URIBuilder pathToEncode(String path) {
		try {
			pathToEncode = URLEncoder.encode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
		return this;
	}

	public URIBuilder query(String name, Object value) {
		if (name != null && value != null)
			this.qParams.add(String.format("%s=%s", name, value));
		return this;
	}

	public URIBuilder query(String query) {
		if (query != null)
			this.query.append(query);
		return this;
	}

	public URIBuilder query(Params params) {
		if (params.getParams() != null)
			this.qParams.addAll(params.getParams());
		return this;
	}
	
}
