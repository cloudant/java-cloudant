package org.lightcouch.tests;

import org.lightcouch.Document;

public class Bar extends Document {
	private String bar;

	public String getBar() {
		return bar;
	}

	public void setBar(String bar) {
		this.bar = bar;
	}

	@Override
	public String toString() {
		return "Bar [bar=" + bar + "]";
	}
}
