/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
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

package org.lightcouch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for construction of HTTP request URIs.
 * 
 * @author Ahmed Yehia
 * 
 */
class URIBuilder {
	private String scheme;
	private String host;
	private int port;
	private String path = "";
	/* The final query */
	private final StringBuilder query = new StringBuilder();
	private final List<String> queries = new ArrayList<String>();

	public static URIBuilder builder() {
		return new URIBuilder();
	}

	public static URIBuilder builder(URI uri) {
		URIBuilder builder = URIBuilder.builder().scheme(uri.getScheme())
				.host(uri.getHost()).port(uri.getPort()).path(uri.getPath());
		return builder;
	}

	public URI build() {
		try {
			for (int i = 0; i < queries.size(); i++) {
				query.append(queries.get(i));
				if (i != queries.size() - 1)
					query.append("&");
			}
			String q = (query.length() == 0) ? null : query.toString();
			return new URI(scheme, null, host, port, path, q, null);
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

	public URIBuilder query(String name, Object value) {
		if (name != null && value != null)
			this.queries.add(String.format("%s=%s", name, value));
		return this;
	}

	public URIBuilder query(String query) {
		if (query != null)
			this.query.append(query);
		return this;
	}

	public URIBuilder query(Params params) {
		if (params.getParams() != null)
			this.queries.addAll(params.getParams());
		return this;
	}
}
