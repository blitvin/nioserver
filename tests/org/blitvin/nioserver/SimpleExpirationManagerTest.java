package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.Test;

public class SimpleExpirationManagerTest {

	
	@Test
	public void smokeTest() {
		SimpleExpirationManager<Integer> mgr = new SimpleExpirationManager<>(3000);
		mgr.start();
		int h1 = mgr.add(1);
		int h2 = mgr.add(2);
		Collection<Integer> obs = mgr.getExpired();
		assertTrue(obs.isEmpty());
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		h2=mgr.touch(2, h2);
		assertEquals(h1, h2);
		waitUsecs(2000);
		
		h2=mgr.touch(2, h2);
		assertEquals(h1+1, h2);
		obs = mgr.getExpired();
		assertTrue( obs.isEmpty());
		
		waitUsecs(2000);
		
		obs = mgr.getExpired();
		
		
		assertFalse(obs.isEmpty());
		assertEquals(obs.size(), 1);
		for(Integer i:obs){
			assertEquals(i.intValue() ,1);
		}
	}
	
	private void waitUsecs(int delay){
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			//e.printStackTrace(); // swallow?
		}
	}
	@Test
	public void testGenerations(){
		SimpleExpirationManager<Integer> mgr = new SimpleExpirationManager<>(3000);
		mgr.start();
		
		int h1 = mgr.add(1);
		int h2 = mgr.add(2);
		int h3 = mgr.add(3);
		waitUsecs(2000);
		h1 = mgr.touch(1,h1);
		assertEquals(0, h1);
		waitUsecs(2000);
		h1 = mgr.touch(1, h1);
		assertEquals(1, h1);
		assertTrue(mgr.getExpired().isEmpty());
		waitUsecs(3000);
		h2 = mgr.touch(2, h2);
		assertEquals(2,h2);
		waitUsecs(3000);
		Collection<Integer>obs = mgr.getExpired();
		assertEquals(2, obs.size());
		int returns[] = new int[2];
		int cnt = 0;
		for(Integer i :obs){
			returns[cnt++] = i;
		}
		assertNotEquals(returns[0], returns[1]);
		assertTrue(returns[0]  == 1 || returns[1] == 1);
		assertTrue(returns[0] == 3 || returns[1] == 3);
			
		
	}

}
