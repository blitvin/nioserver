package org.blitvin.nioserver.throughputtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.blitvin.nioserver.LVClientHelper;

public class NioThroughputTestClient {

	private Socket admConnection;
	private InputStream admInput;
	private OutputStream admOutput;
	final private int numOfWorkers;
	final private CountDownLatch startLatch;
	final private CountDownLatch endLatch;
	private final byte[] replyMsg;
	final private Benchmarker[] benchmarkers;
	final private PingOnlyClient[] pingClients;
	final private int pingOnlyConnections;
	private static class PingOnlyClient extends Thread{
		
		final private LVClientHelper helper;
		final private int pingInterval;
		final private CountDownLatch startLatch;
		public PingOnlyClient(String hostname, int port, int pingInterval, CountDownLatch startLatch) throws UnknownHostException, IOException {
			super();
			helper = new LVClientHelper(hostname, port);
			this.pingInterval = pingInterval;
			this.startLatch = startLatch;
			setDaemon(true);
		}
		
		@Override
		public void run(){
			try {
				startLatch.await();
				while(true) {
					Thread.sleep(pingInterval);
					helper.keepAliveConnection();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static class Benchmarker extends Thread{
		final private LVClientHelper helper;
		final private CountDownLatch startLatch;
		final private CountDownLatch endLatch;
		final private int id;
		final private int worktime;
		final private byte[] msg;
		final private byte[] dbgPrevMsg;
		private long totalTime = 0;
		private int requestsDone = 0;
		private int errors = 0;
		private int maxTime = 0;
		private int minTime = Integer.MAX_VALUE;
		private final byte[] statisticsMsg;
		private final int offset;
		public Benchmarker(String hostname,int port, CountDownLatch startLatch,CountDownLatch endLatch,
				int msgSize,int worktime, int id,final byte[] reply, int offset) throws UnknownHostException, IOException{
			super();
			helper = new LVClientHelper(hostname, port);
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.id = id;
			this.worktime = worktime;
			msg = new byte[4+4+msgSize];
			dbgPrevMsg = new byte[4+4+msgSize];
			for(int i = 0 ; i < msgSize; ++i)
				msg[8+i] = (byte)( 0xFF & i);
			RemoteControlProtocol.encodeNum(msg, 0, 4, id);
			for(int i = 0 ; i< 4 ; ++i)
				msg[4+i] = 0;
			this.statisticsMsg = reply;
			this.offset = offset;
		}
		
		@Override
		public void run(){
			long endTime;
			byte[] reply = null;
			try {
				startLatch.await();
				endTime = System.currentTimeMillis() + worktime;
				do {
					try {
						
						long startTime = System.nanoTime();
						try {
							reply =helper.sendRequest(msg);
						}
						catch(OutOfMemoryError er){
							System.err.println("Got out of memory error");
							System.err.println("Current message dump: length="+msg.length);
							for(int i =0; i < msg.length; ++i){
								System.err.print(" "+Integer.toHexString(0xFF & msg[i]));
								if (i % 32 == 31) System.err.println();
							}
							System.err.println();
							System.err.println("Last message dump: length="+dbgPrevMsg.length);
							for(int i =0; i < dbgPrevMsg.length; ++i){
								System.err.print(" "+Integer.toHexString(0xFF & dbgPrevMsg[i]));
								if (i % 32 == 31) System.err.println();
							}
							System.err.println();
							
							System.err.println("last reply="+reply);
							System.err.println("count="+requestsDone);
							System.err.println("errors="+errors);
							return;
						}
						int delta = (int)(System.nanoTime() - startTime);
						if (delta < minTime)
							minTime = delta;
						if (delta > maxTime)
							maxTime = delta;
						totalTime += delta;
						requestsDone++;
						if (!Arrays.equals(msg, reply))
							errors++;
						System.arraycopy(msg, 0, dbgPrevMsg, 0, msg.length);
					}
					catch(Exception e){
						errors++;
					}
					for(int i  = 4; i < 8 ; ++i){
						if ((0x7F & msg[i])  < 0x7F) {
							msg[i]++;
							break;
						}
						msg[i] = 0;
					}
				} while (System.currentTimeMillis() < endTime);
				try {
					helper.closeConnection();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("In close connection");
					e.printStackTrace();
				}
				RemoteControlProtocol.buildResultsMessage(statisticsMsg, offset,
						requestsDone, maxTime,minTime,(int)(totalTime /1000000L) , errors);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			}
			finally {
				endLatch.countDown();
			}
			System.out.println("ID="+id+" Requests="+requestsDone + " min=" + minTime + " max="+maxTime + " errors="+errors+ " total="+totalTime);
		}
	}
	
	public NioThroughputTestClient(String hostname, int adminstrativePort) throws UnknownHostException, IOException {
		admConnection = new Socket(hostname, adminstrativePort);
		BufferedReader r = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()));
		String myHostname = r.readLine();
		r.close();
		
		admInput = admConnection.getInputStream();
		admOutput = admConnection.getOutputStream();
		admOutput.write(RemoteControlProtocol.encodeString(myHostname));
		byte[] setupMsg = new  byte[RemoteControlProtocol.SETUP_MESSAGE_LEN];
		admInput.read(setupMsg);
		int port = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.PORT_OFFSET, 2);
		int msgSize = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.MSG_SIZE_OFFSET, 2);
		numOfWorkers = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.NUM_OF_THREADS_OFFSET,1);
		int worktime = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.WORKING_TIME_OFFSET,4);
		pingOnlyConnections = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.PING_ONLY_CONNECTIONS_OFFSET,1);
		int pingInterval = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.PING_INTERVAL_OFFSET,4);
		int id = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.ID_OFFSET, 4);
		startLatch  = new CountDownLatch(1);
		endLatch = new CountDownLatch(numOfWorkers);
		replyMsg = new byte[RemoteControlProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK * numOfWorkers];
		benchmarkers = new Benchmarker[numOfWorkers];
		System.out.println("Setup:id="+id+",workers="+numOfWorkers+",pings="+pingOnlyConnections+",msgSize="+msgSize+",time="+worktime+",pingInt="+pingInterval);
		for(int i = 0 ; i < numOfWorkers; ++i)
			benchmarkers[i] = new Benchmarker(hostname, port, startLatch, 
					endLatch, msgSize, worktime, id+i, replyMsg, i*RemoteControlProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK);
		pingClients = new PingOnlyClient[pingOnlyConnections];
		for(int i = 0 ; i < pingOnlyConnections; ++i)
			pingClients[i] = new PingOnlyClient(hostname, port, pingInterval, startLatch);
	}
	
	public void runTest(){
		// here start all the threads
		//collect results
		for(int i = 0 ; i < numOfWorkers; ++i)
			benchmarkers[i].start();
		for(int i = 0; i < pingOnlyConnections; ++i)
			pingClients[i].start();
		try {
			admInput.read();
		} catch (IOException e1) {
//			e1.printStackTrace();
		}
	/*	byte[] reply = new byte[ClientServerProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK * numOfWorkers];
		for(int i = 0 ;i < numOfWorkers; ++i)
			ClientServerProtocol.buildResultsMessage(reply, ClientServerProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK * i,
						5, 100, 50, 375 , 3);*/
		startLatch.countDown();
		try {
			endLatch.await();
		} catch (InterruptedException e1) {
			
		}
		try {
			admOutput.write(replyMsg);
		} catch (IOException e) {
			
			System.err.println("Got IOException "+e.toString());
			e.printStackTrace();
		}
		try {
			admConnection.close();
		} catch (IOException e) {
			System.err.println("In close :Got IOException "+e.toString());
			e.printStackTrace();
			
		}
	}
	
	public static void main(String[] args) {
		if(args.length != 2){
			System.err.println("Usage: java obj.blitvin.nioserver.throughputtest.NioThroughputTestClient host port");
			return;
		}
		boolean badPort=false;
		int port = -1;
		try {
			port = Integer.parseInt(args[1]);
		if (port < 1024 || port > 65535)
			badPort = true;
		}
		catch(NumberFormatException e){
			badPort = true;
		}
		if (badPort){
			System.err.println("Second argument must be port - integer between 1024 and 65535, got "+args[1]);
			return;
		}
		
		NioThroughputTestClient client=null;
		try {
			client = new NioThroughputTestClient(args[0], port);
			
		} catch ( IOException e) {
			System.err.println("NioThroughputTestClient(): got exception"+e);
			e.printStackTrace();
		}
		
		client.runTest();
	}
	

}
