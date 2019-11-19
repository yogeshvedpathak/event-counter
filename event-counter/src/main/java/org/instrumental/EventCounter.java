package org.instrumental;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 

This event counter keeps track when the event happened in last window of time. The window is defined in seconds with 
default set to 300 (5 minutes). The higher the window the higher time is needed to count events. To signal the event has occurred user can can call method logEvent. To count the events 
happened user can call method countEvents(). This method takes a parameter as seconds to count event happened since that time and now. 
<br>
<br>
This event counter is designed to count up to 2 million events per second. Any calls to logEvent above that limit will result is 
incorrect count. If countEvent is called with value less than 0 it returns 0. If it is called value more window then only events 
happened in entire window of time are returned. 
<br>
<br>
This count does maintain a state internally with background threads. Hence it needs open and close method. 
Before using any of above methods user needs to call open method. To release resources user needs to call close method. 
If any of the method called before calling open or after calling close may result in runtime exception. 

Example:

<pre>
<code>

	EventCounter counter = new EventCounter(); //Creates counter with 5 minute window. 
	counter.open(); // Initialize counter
	counter.logEvent(); // Signal event happened now
	counter.countEvent(30); // Count events happened in last 30 seconds 
	counter.close(); // Free resources
</code>
</pre>

 * @author yogeshvedpathak
 *
 */
public class EventCounter implements Runnable {
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	private int window = 300;
	private int startIndex = 0;
	private volatile int currentIndex = 0;
	private List<AtomicInteger> counters = new ArrayList<AtomicInteger>();
	private ScheduledFuture<?> tiktask;
	private AtomicBoolean firstRollover = new AtomicBoolean(false); 
	/**
	 * Default constructor with window size set to 5 minutes
	 */
	public EventCounter() {
		this(300);
	}
	/**
	 * Creates even counter with specified window size.  
	 * @param window size in seconds 
	 * @throws IllegalArgumentException if window size is <= 0
	 */
	public EventCounter(int window) {
		if(window <= 0) {
			throw new IllegalArgumentException("Window size should be greater than 0");
		}
		this.startIndex = -1;
		this.currentIndex = -1;
		this.window = window;		
		for(int i=0; i<= window; i++) {
			counters.add(new AtomicInteger(0));
		}
	}
	
	/**
	 * 	Initializes counter and creates required resources
	 */	
	public void open() {
		startIndex = (int)(System.currentTimeMillis() / 1000) % window;
		currentIndex = startIndex;
		tiktask = scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
	}
	/**
	 * Frees resources used by this counter
	 */
	public void close() {
		startIndex = -1;
		currentIndex = -1;
		tiktask.cancel(true);
	}
	/*
	 * Signals the counter that event has happened. The counter uses it's internal timer to record the time 
	 * the event occurred. The internal timer's granularity is seconds.  
	 */
	public void logEvent() {
		int cIndex = currentIndex;
		counters.get(cIndex).incrementAndGet();
	}
	
	/**
	 * Counts all event that happened since the given time. The method returns 0 if the time is less than 1
	 * @param since time in seconds
	 * @return count 
	 */
	public long countEvents(int since) {
		long count = 0;
		if(since > 0) {
			int realSince = Math.min(since, window);
			int cIndex = currentIndex;
			int index = 0;
			for(int i = 0; i < realSince ; i++) {
				index = (cIndex - i) % window;
				index = index < 0 ? window + index : index;
				count += counters.get(index).get();
			}
		}
		return count;
	}
	
	/**
	 * This method is used to maintain internal timer
	 */
	public final void run() {
		currentIndex = (int)(System.currentTimeMillis() / 1000) % window;;
		if(currentIndex == startIndex) {
			firstRollover.compareAndSet(false, true);
		}
		if(firstRollover.get()) {
			counters.get(currentIndex).set(0);
		}		
	}
}
