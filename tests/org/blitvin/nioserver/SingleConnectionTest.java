package org.blitvin.nioserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

public class SingleConnectionTest {
	static Thread serverThread = null;
	@BeforeClass
	static public void setup() throws IOException{
		NioServer<TestClientContext, TestBytesMP> server= new NioServer<>(12345, TestClientContext.class, TestBytesMP.class,null);
		Thread serverThread = new Thread(server);
		serverThread.setDaemon(true);
		serverThread.start();
	}
	
	
	@Test
	public void testSingleConnection() throws IOException, ClassNotFoundException, RemoteExecutionException{
		ClientHelper helper = new ClientHelper("localhost", 12345);
		
		byte[] message = new byte[2];
		message[0] = 1;
		message[1]= 0;
		for(int i =1 ; i < 10 ; i++){
			message[1] = (byte)i;
			message = helper.sendRequest(message);
			assertEquals(2,message.length);
			assertEquals(i+1, message[1]);
			assertEquals(1,message[0]);
		}
		
		try {
			message[0]= 2;
			message = helper.sendRequest(message);
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
			message[0] = 1;
			message[1] = 1;
			message = helper.sendRequest(message);
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
