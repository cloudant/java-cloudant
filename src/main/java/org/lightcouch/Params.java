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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

/**
 * Query parameters to append to find requests.
 * <p>Example: 
 * <pre>
 * dbClient.find(Foo.class, "doc-id", new Params().revsInfo().attachments());
 * 
 * @see CouchDbClient#find(Class, String, Params)
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
		params.add(format("rev=%s", rev));
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

	public Params addParam(String name, Object value) {
		params.add(format("%s=%s", name, value));
		return this;
	}

	public List<String> getParams() {
		return params.isEmpty() ? null : params;
	}
}
