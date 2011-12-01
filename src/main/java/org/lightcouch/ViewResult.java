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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Holds a view result entries. 
 * @see View
 * @author Ahmed Yehia
 */
public class ViewResult<K, V, T> {
	
	@SerializedName("total_rows")
	private long totalRows; 
	@SerializedName("update_seq")
	private long updateSeq; 
	private int offset;
	private List<Rows> rows = new ArrayList<Rows>();
	
	public long getTotalRows() {
		return totalRows;
	}

	public long getUpdateSeq() {
		return updateSeq;
	}

	public int getOffset() {
		return offset;
	}

	public List<Rows> getRows() {
		return rows;
	}

	public void setTotalRows(long totalRows) {
		this.totalRows = totalRows;
	}

	public void setUpdateSeq(long updateSeq) {
		this.updateSeq = updateSeq;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void setRows(List<Rows> rows) {
		this.rows = rows;
	}
	
	@Override
	public String toString() {
		return "ViewResult [totalRows=" + totalRows + ", updateSeq=" + updateSeq
				+ ", offset=" + offset + ", rows=" + rows + "]";
	}

	/**
	 * Inner class holding the view rows.
	 */
	public class Rows {
		private String id;
		private K key;
		private V value;
		private T doc;
		
		public String getId() {
			return id;
		}
		public K getKey() {
			return key;
		}
		public V getValue() {
			return value;
		}
		public T getDoc() {
			return doc;
		}
		public void setId(String id) {
			this.id = id;
		}
		public void setKey(K key) {
			this.key = key;
		}
		public void setValue(V value) {
			this.value = value;
		}
		public void setDoc(T doc) {
			this.doc = doc;
		}
		@Override
		public String toString() {
			return "Rows [id=" + id + "]";
		}
	}
}
