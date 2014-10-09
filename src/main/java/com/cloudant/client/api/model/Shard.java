package com.cloudant.client.api.model;

import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates info about a Cloudant Shard
 * @since 0.0.1
 * @author Mario Briggs
 *
 */
public class Shard {

	private String range;
	private List<String> nodes;
	
	/**
	 * @return the range
	 */
	public String getRange() {
		return range;
	}
	/**
	 * @return the nodeNames in this shard
	 */
	public Iterator<String> getNodes() {
		return nodes.iterator();
	}
	
	public Shard(String range, List<String> nodes) {
		super();
		this.range = range;
		this.nodes = nodes;
	}
}


