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

package org.lightcouch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Query parameters to append to find requests.
 * <p>Example: 
 * <pre>
 * dbClient.find(Foo.class, "doc-id", new Params().revsInfo().attachments());
 * </pre>
 * @see CouchDbClientBase#find(Class, String, Params)
 * @since 0.0.6
 * @author Ahmed Yehia
 * 
 */
public class Params {

	private List<String> params = new ArrayList<String>();

	public Params revsInfo() {
		params.add("revs_info=true");
		return this;
	}

	public Params attachments() {
		params.add("attachments=true");
		return this;
	}

	public Params revisions() {
		params.add("revs=true");
		return this;
	}

	public Params rev(String rev) {
		params.add(String.format("rev=%s", rev));
		return this;
	}

	public Params conflicts() {
		params.add("conflicts=true");
		return this;
	}

	public Params localSeq() {
		params.add("local_seq=true");
		return this;
	}

	public Params addParam(String name, String value) {
		try {
			name = URLEncoder.encode(name, "UTF-8");
			value = URLEncoder.encode(value, "UTF-8");
			params.add(String.format("%s=%s", name, value));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
		return this;
	}

	public List<String> getParams() {
		return params.isEmpty() ? null : params;
	}
}
