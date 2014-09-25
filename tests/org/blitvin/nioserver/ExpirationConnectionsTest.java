package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
public class ExpirationConnectionsTest {
	
	static NioServer<TestClientContext, TestMP> server = null;
	@BeforeClass
	public static void setup() throws IOException{
			NioServerInitializer<TestClientContext, TestMP> initializer = new NioServerInitializer<>(12345,TestClientContext.class, TestMP.class,null);
			initializer.setClientTimeout(1000);
			server= new NioServer<>(initializer);
			Thread serverThread = new Thread(server);
			serverThread.setDaemon(true);
			serverThread.start();
	}
	
	@AfterClass
	public static void shutdown() {
		if(server != null)
		server.shutdown();
	}
	
	@Test
	public void testExpiration() throws ClassNotFoundException, RemoteExecutionException, IOException{
		LVClientHelper helper1  = null;
		LVClientHelper helper2 = null;
		try {
			helper1 = new LVClientHelper("localhost", 12345);
			Thread.sleep(3000);
			helper2 = new LVClientHelper("localhost", 12345);	
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			TestMessage message = new TestMessage();
			message.id = 1;
			helper1.sendObjectRequest(message);
			fail("helper1 expected to expire");
		}
		catch(IOException ex){
			assertEquals("peer closed connection", ex.getMessage());
		}
		
		TestMessage message = new TestMessage();
		message.id = 100;
		message.count = 1;
		message = (TestMessage) helper2.sendObjectRequest(message);
		assertEquals(100, message.id);
		assertEquals(2,message.count);
	}
}
