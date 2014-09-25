package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class LVProtocolTest {

	LVClientProtocol client = new LVClientProtocol();
	LVServerProtocol server = new LVServerProtocol();
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@After
	public void teardown(){
		client.inboundMessageHasBeenConsumed();
		server.inboundMessageHasBeenConsumed();
	}
	
	private void sendMessageToServer(){
		byte[] clientMessage =  {1,2,3,4,5};
		
		ByteBuffer clientBuf = client.encodeMessage(clientMessage);
	
		server.addPart(clientBuf);
		assertTrue(server.hasCompleteMessage());
		assertEquals(0, server.getServiceMessage()); 
		byte[] serverMessage = server.getRequest();
		assertArrayEquals(clientMessage,serverMessage);
		server.inboundMessageHasBeenConsumed();
		assertFalse(server.hasCompleteMessage());
		assertNull(server.msgData);
		assertEquals(0,server.getServiceMessage());
	}
	
	private void sendMessageToClient(){
		byte[] serverMessage = {5,4,3,2,1,0};
		ByteBuffer serverBuf = server.encodeMessage(serverMessage);
		client.addPart(serverBuf);
		assertTrue(client.hasCompleteMessage());
		try {
			byte[] reply = client.getReply();
			assertArrayEquals(serverMessage, reply);
		} catch (ClassNotFoundException | RemoteExecutionException | IOException e) {
			fail("Exception was thrown :"+ e);
		}
		client.inboundMessageHasBeenConsumed();
		assertFalse(client.hasCompleteMessage());
		assertNull(client.msgData);
	}
	
	@Test
	public void testMsgPass() {
		for(int i  = 0 ; i < 3; ++i) {
			sendMessageToServer();
			sendMessageToClient();
		}
	}
	
	@Test(expected=RemoteExecutionException.class)
	public void testExceptionReply() throws ClassNotFoundException, RemoteExecutionException, IOException{
		ByteBuffer replyBuf =server.putExceptionReply(new IOException("test exception"));
		client.addPart(replyBuf);
		assertTrue(client.hasCompleteMessage());
		byte[] reply = client.getReply();
		reply[0] = 0;	
		
	}
	
	@Test
	public void testServiceMessage(){
		ByteBuffer clientBuf = client.sendServiceMessage(ClientServerProtocol.CLOSE_NOTIFICATION);
		server.addPart(clientBuf);
		assertTrue(server.hasCompleteMessage());
		assertEquals(ClientServerProtocol.CLOSE_NOTIFICATION,server.getServiceMessage());
		server.inboundMessageHasBeenConsumed();
		assertFalse(server.hasCompleteMessage());
		assertEquals(0,server.getServiceMessage());
		sendMessageToServer();// to check service code is not stuck
	}
	
	@Test
	public void testMultipleMessagesInSingleTransmission(){
		ByteBuffer b = ByteBuffer.allocate(13);
		byte t[] = {0,0,0,2,1,2,0,0,0,3,11,12,13};
		b.put(t,0,t.length);
		b.flip();
		server.addPart(b);
		assertTrue(server.hasCompleteMessage());
		byte[] msg = server.getRequest();
		assertEquals(2, msg.length);
		assertEquals(1, msg[0]);
		assertEquals(2,msg[1]);
		assertTrue(server.hasCompleteMessage());
		msg = server.getRequest();
		assertEquals(2, msg.length);
		assertEquals(1, msg[0]);
		assertEquals(2,msg[1]);
		server.inboundMessageHasBeenConsumed();
		assertTrue(server.hasCompleteMessage());
		msg = server.getRequest();
		assertEquals(3, msg.length);
		assertEquals(11, msg[0]);
		assertEquals(12,msg[1]);
		assertEquals(13,msg[2]);
	}
	
	@Test
	public void testMessageInMultipleTransmissions(){
		ByteBuffer b = ByteBuffer.allocate(10);
		byte[] t = {0,0,0,4,2};
		byte[] t2 = {1,3,5};
		b.put(t,0,3);
		b.flip();
		server.addPart(b);
		assertFalse(server.hasCompleteMessage());
		b.clear();
		b.put(t, 3, 2);
		b.flip();
		server.addPart(b);
		assertFalse(server.hasCompleteMessage());
		b.clear();
		b.put(t2,0,3);
		b.flip();
		server.addPart(b);
		assertTrue(server.hasCompleteMessage());
		byte[] msg = server.getRequest();
		assertEquals(4, msg.length);
		assertEquals(2, msg[0]);
		assertEquals(1,msg[1]);
		assertEquals(3, msg[2]);
		assertEquals(5,msg[3]);		
	}
	
	@Test
	public void testSplitMessage(){
		ByteBuffer b = ByteBuffer.allocate(10);
		byte[] t = {0,0,0,1,2};
		b.put(t,0,3);
		b.flip();
		server.addPart(b);
		assertFalse(server.hasCompleteMessage());
		b.clear();
		b.put(t, 3, 2);
		b.flip();
		server.addPart(b);
		assertTrue(server.hasCompleteMessage());
		byte[] r = server.getRequest();
		assertEquals(r.length, 1);
		assertEquals(2,r[0]);
		server.inboundMessageHasBeenConsumed();
	}

}
