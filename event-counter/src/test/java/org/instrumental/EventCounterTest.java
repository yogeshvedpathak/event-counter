package org.instrumental;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;


public class EventCounterTest {

	@Test
	public void testLogEvent() throws InterruptedException {
		EventCounter counter = new EventCounter(30);
		counter.open();
		for(int i=0; i<30; i++) {
			counter.logEvent();
		}
		Assert.assertEquals(30, counter.countEvents(30));
		Thread.sleep(5000);
		for(int i=0; i<30; i++) {
			counter.logEvent();
			Thread.sleep(100);
		}
		Assert.assertEquals(60, counter.countEvents(30));
		Thread.sleep(25000);
		//Past the 30 seconds of total window size
		Assert.assertEquals(30, counter.countEvents(900));
		for(int i=0; i<5; i++) {
			counter.logEvent();
			Thread.sleep(500);
		}
		Assert.assertEquals(5, counter.countEvents(3));
		
		Assert.assertEquals(0, counter.countEvents(0));
		Assert.assertEquals(0, counter.countEvents(-2));
		counter.close();
	}
	
	@Test
	public void testWindowRollover() throws InterruptedException {
		EventCounter counter = new EventCounter(10);
		counter.open();
		for(int i=1; i<25; i++) {
			counter.logEvent();
			Thread.sleep(1000);
		}
		Thread.sleep(11000);
		Assert.assertEquals(0, counter.countEvents(60));
		counter.close();
	}
	
	@Test
	public void testOpenClose() {
		final EventCounter counter = new EventCounter(30);
		try {
			counter.logEvent();
			Assert.fail("RTE expected");
		}catch(RuntimeException e) {
			//Expected
		}
		counter.open();
		counter.logEvent();
		Assert.assertEquals(1, counter.countEvents(30));
		
		counter.close();
		try {
			counter.logEvent();
			Assert.fail("RTE expected");
		}catch(RuntimeException e) {
			//Expected
		}
	}
	
	@Test
	public void testEventMultiThread() throws InterruptedException {
		final EventCounter counter = new EventCounter(30);
		counter.open();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean run = new AtomicBoolean(true);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
		
		Runnable logger = new Runnable() {
			public void run() {
				while(run.get()) {
					counter.logEvent();
				}
			}
		};
		scheduler.execute(logger);
		
		scheduler.schedule(new Runnable() {
			public void run() {
				run.compareAndSet(true,false);
				latch.countDown();
			}
		}, 70, TimeUnit.SECONDS);
		latch.await();
		Assert.assertTrue(counter.countEvents(900) > 0);
		
		Thread.sleep(31000);
		Assert.assertEquals(0, counter.countEvents(900));
		counter.close();
	}
}
