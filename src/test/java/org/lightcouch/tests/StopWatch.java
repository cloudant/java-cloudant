package org.lightcouch.tests;

// not thread safe
public final class StopWatch {
	
	private StopWatch() {}
	
	private static long start;
	private static long stop;

	public static void start() {
		start = System.currentTimeMillis();
		stop = 0;
	}
	
	public static long stop() {
		stop = System.currentTimeMillis();
		long value = stop - start; 
		start = 0;
		return value;
	}
}
