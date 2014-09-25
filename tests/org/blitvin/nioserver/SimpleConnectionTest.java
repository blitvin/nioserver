package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleConnectionTest {

	static Thread serverThread = null;
	@BeforeClass
	static public void setup() throws IOException{
		NioServer<TestClientContext, TestMP> server= new NioServer<>(12345, TestClientContext.class, TestMP.class,null);
		Thread serverThread = new Thread(server);
		serverThread.setDaemon(true);
		serverThread.start();
	}
	
	/*
	Object sendObjectMessage(ClientHelper2 helper,Object message) throws IOException, ClassNotFoundException, RemoteExecutionException{
		return ObjectEncoderDecoder.decode(helper.sendRequest(ObjectEncoderDecoder.encode(message)));
	}*/
	@Test
	public void testSIngleConnnection() throws Exception {
		LVClientHelper helper = new LVClientHelper("localhost", 12345);
		
		TestMessage message = new TestMessage();
		message.id = 1;
		for(int i =1 ; i < 10 ; i++){
			message.count = i;
			message = (TestMessage) helper.sendObjectRequest(message);
			assertEquals(i+1, message.count);
			assertEquals(1,message.id);
		}
		
		try {
			message.id = 2;
			message = (TestMessage) helper.sendObjectRequest(message);
			fail("Incorrect id sent - should throw exception");
		}
		catch(RemoteExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
			System.out.println("Got RemoteExecutionException exception "+e.toString());
			e.getCause().printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
			fail("Got unexpected exception"+e.toString());
			
		}
		
		try {
			message.id = 1;
			message.count = 1;
			message = (TestMessage) helper.sendObjectRequest(message);
			fail("Incorrect id sent - should throw exception");
		}
		catch(RemoteExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
			System.out.println("Got illegal argument exception "+e.toString());
			e.getCause().printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
			fail("Got unexpected exception"+e.toString());
			
		}
		
		
	}

}
