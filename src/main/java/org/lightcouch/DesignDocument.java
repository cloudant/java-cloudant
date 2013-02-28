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

import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a design document.
 * @see CouchDbDesign
 * @author Ahmed Yehia
 */
public class DesignDocument extends Document {
	
	private String language;
	private Map<String, MapReduce> views;
	@SerializedName("validate_doc_update")
	private String validateDocUpdate;
	private Map<String, String> filters;
	private Map<String, String> shows;
	private Map<String, String> lists;
	private Map<String, String> updates;

	public String getLanguage() {
		return language;
	}

	public Map<String, MapReduce> getViews() {
		return views;
	}

	public String getValidateDocUpdate() {
		return validateDocUpdate;
	}

	public Map<String, String> getFilters() {
		return filters;
	}

	public Map<String, String> getShows() {
		return shows;
	}

	public Map<String, String> getLists() {
		return lists;
	}
	
	public Map<String, String> getUpdates() {
		return updates;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setViews(Map<String, MapReduce> views) {
		this.views = views;
	}

	public void setValidateDocUpdate(String validateDocUpdate) {
		this.validateDocUpdate = validateDocUpdate;
	}

	public void setFilters(Map<String, String> filters) {
		this.filters = filters;
	}

	public void setShows(Map<String, String> shows) {
		this.shows = shows;
	}

	public void setLists(Map<String, String> lists) {
		this.lists = lists;
	}

	public void setUpdates(Map<String, String> updates) {
		this.updates = updates;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((filters == null) ? 0 : filters.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((lists == null) ? 0 : lists.hashCode());
		result = prime * result + ((shows == null) ? 0 : shows.hashCode());
		result = prime * result + ((updates == null) ? 0 : updates.hashCode());
		result = prime * result
				+ ((validateDocUpdate == null) ? 0 : validateDocUpdate.hashCode());
		result = prime * result + ((views == null) ? 0 : views.hashCode());
		return result;
	}

	/**
	 * Indicates whether some other design document is equals to this one. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DesignDocument other = (DesignDocument) obj;
		if (filters == null) {
			if (other.filters != null)
				return false;
		} else if (!filters.equals(other.filters))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (lists == null) {
			if (other.lists != null)
				return false;
		} else if (!lists.equals(other.lists))
			return false;
		if (shows == null) {
			if (other.shows != null)
				return false;
		} else if (!shows.equals(other.shows))
			return false;
		if (updates == null) {
			if (other.updates != null)
				return false;
		} else if (!updates.equals(other.updates))
			return false;
		if (validateDocUpdate == null) {
			if (other.validateDocUpdate != null)
				return false;
		} else if (!validateDocUpdate.equals(other.validateDocUpdate))
			return false;
		if (views == null) {
			if (other.views != null)
				return false;
		} else if (!views.equals(other.views))
			return false;
		return true;
	}

	/**
	 * Holds Map Reduce functions in a view.
	 * @author Ahmed Yehia
	 */
	public static class MapReduce {
		private String map;
		private String reduce;

		public String getMap() {
			return map;
		}

		public String getReduce() {
			return reduce;
		}

		public void setMap(String map) {
			this.map = map;
		}

		public void setReduce(String reduce) {
			this.reduce = reduce;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((map == null) ? 0 : map.hashCode());
			result = prime * result + ((reduce == null) ? 0 : reduce.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MapReduce other = (MapReduce) obj;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
				return false;
			if (reduce == null) {
				if (other.reduce != null)
					return false;
			} else if (!reduce.equals(other.reduce))
				return false;
			return true;
		}

	} // /class MapReduce
}
