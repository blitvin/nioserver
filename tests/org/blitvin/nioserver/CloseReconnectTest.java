package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CloseReconnectTest {

	static NioServer<TestClientContext, TestMP> server = null;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		server= new NioServer<>(12345, TestClientContext.class, TestMP.class,null);
		Thread serverThread = new Thread(server);
		serverThread.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.shutdown();
	}
	Object sendObjectMessage(ClientHelper helper,Object message) throws IOException, ClassNotFoundException, RemoteExecutionException{
		return ObjectEncoderDecoder.decode(helper.sendRequest(ObjectEncoderDecoder.encode(message)));
	}
	@Test
	public void test() throws Exception {
		ClientHelper helper = new ClientHelper("localhost", 12345);
		TestMessage message = new TestMessage();
		message.id = 1;
		for(int i =1 ; i < 10 ; i++){
			message.count = i;
			message = (TestMessage) sendObjectMessage(helper,message);
			assertEquals(i+1, message.count);
			assertEquals(1,message.id);
		}
		helper.closeConnection();
		helper = new ClientHelper("localhost", 12345);
		message.id = 101;
		for(int i =1 ; i < 10 ; i++){
			message.count = i;
			message = (TestMessage) sendObjectMessage(helper,message);
			assertEquals(i+1, message.count);
			assertEquals(101,message.id);
		}
		
	}

}
