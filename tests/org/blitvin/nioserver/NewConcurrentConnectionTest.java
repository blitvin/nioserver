package org.blitvin.nioserver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.blitvin.nioserver.ClientHelper;
import org.blitvin.nioserver.NioServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



	class NewConcurrentConnectionTestRunner extends Thread{
		static final int MESSAGES = 50;

		private CountDownLatch startLatch;
		private CountDownLatch endLatch;
		private int id;
		private final AtomicInteger flagError;
		ClientHelper helper;
		public NewConcurrentConnectionTestRunner(int id, CountDownLatch startLatch, CountDownLatch endLatch,
				AtomicInteger flagError) throws IOException, Exception{
			this.id = id;
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.flagError = flagError;
			this.helper = new ClientHelper("localhost", 12345);
			this.setName("Runner"+ id);
		}
		
		@Override
		public void run(){
			startLatch.countDown();
			System.out.println("count down on startlatch:"+id );
			try {
				startLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			byte[] message = new byte[2];
			message[0] = (byte)id;
			for(int i = 1 ; i <= MESSAGES; ++i){
				try {
					message[1] = (byte)i;
					message = helper.sendRequest(message);
				} catch (Exception e) {
					System.err.println("sender "+ id + " : got exception "+ e.toString() + " on iteration " + i);
					flagError.incrementAndGet();
					//e.printStackTrace();
					break;
				}
				if (message[1] != i+1) {
					System.err.print("sender "+ id + ": expecting count "+ (i+1)+ " and got"+ message[1]);
					flagError.incrementAndGet();
					break;
				}
				if (message[0] != id) {
					System.err.print("sender "+ id + ": got message with id "+ message[0]);
					flagError.incrementAndGet();
					break;
				}
			}
			
			helper.closeConnection();
			
			
			try {
				Thread.sleep(20*id); // this prevents syn flood 
				helper = new ClientHelper("localhost", 12345);
				assertNotNull(helper);
				
				
				for(int i = 1 ; i <= MESSAGES; ++i){
					try {
						message[1] = (byte) i;
						message = helper.sendRequest(message);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.err.println("sender "+ id + " : got exception "+ e.toString() + " on run 2 iteration " + i);
						e.getCause().printStackTrace();
						flagError.incrementAndGet();
						//e.printStackTrace();
						break;
					}
					if (message[1] != i+1) {
						System.err.print("sender "+ id + ": run 2 -expecting count "+ (i+1)+ " and got "+ message[1]);
						flagError.incrementAndGet();
						break;
					}
					if (message[0] != id) {
						System.err.print("sender "+ id + ": run 2 - got message with id "+ message[0]);
						flagError.incrementAndGet();
						break;
					}
				}
				helper.closeConnection();
			} catch (IOException e) {
				System.err.println("Got IOException during second run : sender" + id + " exception " + e.toString());
				e.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			endLatch.countDown();
		}
	}
	
	public class NewConcurrentConnectionTest {
		static final int NUM_OF_RUNNERS = 127;
		static NioServer<TestClientContext, TestBytesMP> server = null;
		@BeforeClass
		public static void setup() throws IOException{
				server= new NioServer<>(12345, TestClientContext.class, TestBytesMP.class,null);
				Thread serverThread = new Thread(server);
				serverThread.setDaemon(true);
				serverThread.start();
		}
		
		@AfterClass
		public static void shutdown() {
			if(server != null)
			server.shutdown();
		}
		private volatile AtomicInteger gotError = new AtomicInteger(0);
		//private CyclicBarrier sync = new CyclicBarrier(30);
		private CountDownLatch startLatch = new CountDownLatch(NUM_OF_RUNNERS);
		private CountDownLatch endLatch = new CountDownLatch(NUM_OF_RUNNERS);
		
		@Test
		public void testConcurrent() throws IOException, Exception{
			for(int i = 1 ; i <= NUM_OF_RUNNERS ; ++i) {
				NewConcurrentConnectionTestRunner runner = new NewConcurrentConnectionTestRunner(i, startLatch, endLatch, gotError);
				runner.start();
			}
			try {
				endLatch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail("Got interrupted exception");
			}
			/*System.out.println("Server accepts:"+server.getAccepts());
			System.out.println("Server reads:"+server.getReads());
			System.out.println("Server writes:"+server.getWrites());
			System.out.println("Server errors:"+server.getErrors());
			System.out.println("Server closes:"+server.getCloses());
			System.out.println("Server close  notifications:"+server.getCloseNotifications());*/
			assertEquals(0, gotError.get());
		}
	

}
