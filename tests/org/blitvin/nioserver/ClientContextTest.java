package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClientContextTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
/*
	@Test
	public void test() {
		ClientContext c = new ClientContext(null,null);
		ByteBuffer b = ByteBuffer.allocate(10);
		byte[] t = {0,0,0,1,2};
		b.put(t,0,3);
		b.flip();
		c.addPart(b);
		assertFalse(c.hasCompleteMessage());
		b.clear();
		b.put(t, 3, 2);
		b.flip();
		c.addPart(b);
		assertTrue(c.hasCompleteMessage());
		byte[] r = c.getMsgAsBytes();
		assertEquals(r.length, 1);
		assertEquals(2,r[0]);
		c.clearMessage();
		//fail("Not yet implemented");
		
	}
	@Test
	public void testMultParts() {
		ClientContext c = new ClientContext(null,null);
		ByteBuffer b = ByteBuffer.allocate(10);
		byte[] t = {0,0,0,4,2};
		byte[] t2 = {1,3,5};
		b.put(t,0,3);
		b.flip();
		c.addPart(b);
		assertFalse(c.hasCompleteMessage());
		b.clear();
		b.put(t, 3, 2);
		b.flip();
		c.addPart(b);
		assertFalse(c.hasCompleteMessage());
		b.clear();
		b.put(t2,0,3);
		b.flip();
		c.addPart(b);
		assertTrue(c.hasCompleteMessage());
		byte[] msg = c.getMsgAsBytes();
		assertEquals(4, msg.length);
		assertEquals(2, msg[0]);
		assertEquals(1,msg[1]);
		assertEquals(3, msg[2]);
		assertEquals(5,msg[3]);		
	}
	@Test
	public void testMultMsgs() {
		ClientContext c = new ClientContext(null,null);
		ByteBuffer b = ByteBuffer.allocate(13);
		byte t[] = {0,0,0,2,1,2,0,0,0,3,11,12,13};
		b.put(t,0,t.length);
		b.flip();
		c.addPart(b);
		assertTrue(c.hasCompleteMessage());
		byte[] msg = c.getMsgAsBytes();
		assertEquals(2, msg.length);
		assertEquals(1, msg[0]);
		assertEquals(2,msg[1]);
		assertTrue(c.hasCompleteMessage());
		msg = c.getMsgAsBytes();
		assertEquals(2, msg.length);
		assertEquals(1, msg[0]);
		assertEquals(2,msg[1]);
		c.clearMessage();
		assertTrue(c.hasCompleteMessage());
		msg = c.getMsgAsBytes();
		assertEquals(3, msg.length);
		assertEquals(11, msg[0]);
		assertEquals(12,msg[1]);
		assertEquals(13,msg[2]);
	}
*/
}
