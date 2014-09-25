package org.blitvin.nioserver.throughputtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;


import org.blitvin.nioserver.LVClientHelper;



public class NioSingleThreadedThroughputClient {
	
	private Socket admConnection;
	private InputStream admInput;
	private OutputStream admOutput;
	final private int numOfWorkers;
	private final byte[] replyMsg;
	private final byte[] msg;
	final private LVClientHelper helper;
	private int worktime;
	private int msgSize;
	private int id;
	private long totalTime = 0;
	private int requestsDone = 0;
	private int errors = 0;
	private int maxTime = 0;
	private int minTime = Integer.MAX_VALUE;
	
	
	public NioSingleThreadedThroughputClient(String hostname, int adminstrativePort) throws UnknownHostException, IOException {
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
		msgSize = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.MSG_SIZE_OFFSET, 2);
		numOfWorkers = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.NUM_OF_THREADS_OFFSET,1);
		worktime = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.WORKING_TIME_OFFSET,4);
		int pingOnlyConnections = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.PING_ONLY_CONNECTIONS_OFFSET,1);
		int pingInterval = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.PING_INTERVAL_OFFSET,4);
		id = RemoteControlProtocol.decodeNum(setupMsg, RemoteControlProtocol.ID_OFFSET, 4);
		replyMsg = new byte[RemoteControlProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK * numOfWorkers];
		
		System.out.println("Setup:id="+id+",workers="+numOfWorkers+",pings="+pingOnlyConnections+",msgSize="+msgSize+",time="+worktime+",pingInt="+pingInterval);
		helper = new LVClientHelper(hostname, port);
		System.out.println("Has helper");
		msg = new byte[4+4+msgSize];
		for(int i = 0 ; i < msgSize; ++i)
			msg[8+i] = (byte)( 0xFF & i);
		RemoteControlProtocol.encodeNum(msg, 0, 4, id);
		for(int i = 0 ; i< 4 ; ++i)
			msg[4+i] = 0;
	}
	
	public void runTest(){
		try {
			admInput.read();
		} catch (IOException e1) {
//			e1.printStackTrace();
		}
		System.out.println("Starting");
		long endTime;
		
			endTime = System.currentTimeMillis() + worktime;
			do {
				try {
					long startTime = System.nanoTime();
					byte[] reply = helper.sendRequest(msg);
					int delta = (int)(System.nanoTime() - startTime);
					if (delta < minTime)
						minTime = delta;
					if (delta > maxTime)
						maxTime = delta;
					totalTime += delta;
					requestsDone++;
					if (!Arrays.equals(msg, reply))
						errors++;
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
			RemoteControlProtocol.buildResultsMessage(replyMsg, 0,
					requestsDone, maxTime,minTime,(int)(totalTime /1000000L) , errors);
		
		
		System.out.println("ID="+id+" Requests="+requestsDone + " min=" + minTime + " max="+maxTime + " errors="+errors+ " total="+totalTime);
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
			System.err.println("Usage: java obj.blitvin.nioserver.throughputtest.NioSingleThreadedThroughputClient host port");
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
		
		NioSingleThreadedThroughputClient client=null;
		try {
			client = new NioSingleThreadedThroughputClient(args[0], port);
			
		} catch ( IOException e) {
			System.err.println("NioThroughputTestClient(): got exception"+e);
			e.printStackTrace();
		}
		
		client.runTest();
	}

}
