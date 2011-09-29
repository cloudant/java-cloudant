package org.lightcouch.tests;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Foo {
	
	private String _id;
	private String _rev;
	
	private String title;
	private int position;
	private List<String> tags;
	private int[] complexDate;
	private Set<Bar> bars;
	
	public Foo() {
		super();
	}
	
	public Foo(String _id) {
		this._id = _id;
	}
	
	public Foo(String _id, String title, int position) {
		this._id = _id;
		this.title = title;
		this.position = position;
	}
	
	public String get_id() {
		return _id;
	}

	public String get_rev() {
		return _rev;
	}

	public String getTitle() {
		return title;
	}

	public int getPosition() {
		return position;
	}

	public List<String> getTags() {
		return tags;
	}

	public Set<Bar> getBars() {
		return bars;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public void set_rev(String _rev) {
		this._rev = _rev;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public void setBars(Set<Bar> bars) {
		this.bars = bars;
	}

	public void setComplexDate(int[] complexDate) {
		this.complexDate = complexDate;
	}

	public int[] getComplexDate() {
		return complexDate;
	}

	@Override
	public String toString() {
		return "Foo [_id=" + _id + ", _rev=" + _rev + ", title=" + title
				+ ", position=" + position + ", tags=" + tags + ", complexDate="
				+ Arrays.toString(complexDate) + ", bars=" + bars + "]";
	}
}
