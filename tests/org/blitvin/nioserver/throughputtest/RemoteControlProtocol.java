package org.blitvin.nioserver.throughputtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

public class RemoteControlProtocol {

	/* PROTOCOL:
	 * CONNECT to SERVER -> CLIENT SENDS hostname ->SERVER SENDS SetupMessage
	 * ->SERVER SENDS 1 byte message to start benchmark
	 * -> CLIENT SENDS ReplyMessage (NumOfThread *RESULT_MESSAGE_PER_CLIENT_CHUNK)  
	 */
	//setup message offsets
	public static final int PORT_OFFSET = 0;
	public static final int MSG_SIZE_OFFSET =2;
	public static final int NUM_OF_THREADS_OFFSET = 6;
	public static final int WORKING_TIME_OFFSET = 7;
	public static final int PING_ONLY_CONNECTIONS_OFFSET = 11;
	public static final int PING_INTERVAL_OFFSET=12;
	public static final int ID_OFFSET = 16;
	
	public static final int PORT_IDX = 0;
	public static final int MSG_SIZE_IDX =1;
	public static final int NUM_OF_THREADS_IDX = 2;
	public static final int WORKING_TIME_IDX = 3;
	public static final int PING_ONLY_CONNECTIONS_IDX = 4;
	public static final int PING_INTERVAL_IDX=5;
	public static final int ID_IDX = 6;
	
	private static final int[] SETUP_MSG_OFFSETS = {PORT_OFFSET,MSG_SIZE_OFFSET,NUM_OF_THREADS_OFFSET,
		WORKING_TIME_OFFSET,PING_ONLY_CONNECTIONS_OFFSET,PING_INTERVAL_OFFSET,ID_OFFSET};
	private static final int[] SETUP_MSG_SIZES = {2,4,1,4,1,4,4};
	
	//results message offsets
	public static final int REQUESTS_DONE_OFFSET = 0;
	public static final int MAX_LATENCY_OFFSET = 4;
	public static final int MIN_LATENCY_OFFSET = 8;
	public static final int TOTAL_TIME_OFFFSET = 12;
	public static final int ERRORS_OFFSET = 16;
	
	public static final int RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK = 20;
	public static final int SETUP_MESSAGE_LEN = 20;
	
	private Socket admConnection;
	private InputStream admInput;
	private OutputStream admOutput;
	private final byte[] setupMsg;
	private final byte[] resultsMsg;
	private final CountDownLatch startLatch;
	public RemoteControlProtocol(String hostname,int port) throws UnknownHostException, IOException {
		admConnection = new Socket(hostname, port);
		BufferedReader r = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()));
		String myHostname = r.readLine();
		r.close();
		
		admInput = admConnection.getInputStream();
		admOutput = admConnection.getOutputStream();
		admOutput.write(RemoteControlProtocol.encodeString(myHostname));
		setupMsg = new  byte[RemoteControlProtocol.SETUP_MESSAGE_LEN];
		admInput.read(setupMsg);
		resultsMsg = new byte[getValue(NUM_OF_THREADS_IDX) * RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK];
		startLatch = new CountDownLatch(1);
	}
	
	public int getValue(int paramIdx){
		return decodeNum(setupMsg, SETUP_MSG_OFFSETS[paramIdx], SETUP_MSG_SIZES[paramIdx]);
	}
	
	public void reportResults(int idx, int requestsDone, int maxLatency, int minLatency, int totalTime, int errors){
		buildResultsMessage(resultsMsg,idx*RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK,
				requestsDone,maxLatency,minLatency,totalTime,errors);
	}
	
	public void waitForTestStart(){
		try {
			admInput.read();
		} catch (IOException e) {
			
		}
		startLatch.countDown();
	}
	public void waitForTestStartInThread(){
		try {
			startLatch.await();
		} catch (InterruptedException e) {
		}
	}
	public void sendResultsAndCloseConnection(){
		try {
			admOutput.write(resultsMsg);
		} catch (IOException e) {
			System.err.println("cant send results");
			e.printStackTrace();
		}
		try {
			admConnection.close();
		} catch (IOException e) {
			
		}
	}
	static byte[] buildSetupMessage(int port, int messageSize, int numOfThreads,int workingTime,
			int pingOnlyConnections,int pingInterval,int id){
		byte[] retVal = new byte[SETUP_MESSAGE_LEN];
		encodeNum(retVal, PORT_OFFSET, 2, port);
		encodeNum(retVal,MSG_SIZE_OFFSET,4,messageSize);
		encodeNum(retVal, NUM_OF_THREADS_OFFSET, 1, numOfThreads);
		encodeNum(retVal, WORKING_TIME_OFFSET, 4, workingTime);
		encodeNum(retVal, PING_ONLY_CONNECTIONS_OFFSET, 1, pingOnlyConnections);
		encodeNum(retVal, PING_INTERVAL_OFFSET, 4, pingInterval);
		encodeNum(retVal,ID_OFFSET,4, id);
		return retVal;
	}
	
	static void buildResultsMessage(byte[] array, int offset, int requestsDone,int maxLatency,
						int minLatency, int totalTime,int errors){
		encodeNum(array, offset+REQUESTS_DONE_OFFSET, 4, requestsDone);
		encodeNum(array, offset+MAX_LATENCY_OFFSET, 4, maxLatency);
		encodeNum(array, offset+MIN_LATENCY_OFFSET, 4, minLatency);
		encodeNum(array, offset+TOTAL_TIME_OFFFSET,4, totalTime);
		encodeNum(array, offset+ERRORS_OFFSET, 4, errors);
		
	}
	static int decodeNum(byte[] array, int offset,int len){
		int retVal =0;
		for(int i =len-1; i >=0 ; --i){
			retVal <<= 8;
			retVal |= 0xFF & array[offset+i];
		}
		return retVal;
	}
	
	static void encodeNum(byte[] array, int offset, int len, int val){
		for(int i = 0 ; i < len ; ++i){
			array[offset+i] = (byte)(val & 0xFF);
			val >>=8;
		}
	}
	
	static int encodeString(byte[] array,int offset, String val){
		byte[] bytes = val.getBytes();
		encodeNum(array,offset,4,bytes.length);
		System.arraycopy(bytes, 0, array, offset+4,bytes.length);
		return bytes.length;
	}
	
	static byte[] encodeString(String val){
		if (val == null || val.length() == 0){
			byte[] retVal = new byte[4];
			for(int i=0; i< 4;++i)
				retVal[i]=0;
			return retVal;
		}
		byte[] stringBytes = val.getBytes();
		byte[] retVal = new byte[stringBytes.length+4];
		encodeNum(retVal, 0, 4, stringBytes.length);
		System.arraycopy(stringBytes, 0, retVal, 4, stringBytes.length);
		return retVal;
	}
	static String decodeString(byte[] array,int offset){
		int len = decodeNum(array,offset,4);
		if (len == 0) return ""; // spare from user check for null...
		return new String(array,offset+4,len);
	}
	static int encodeStringWoLen(byte[] array,int offset, String val){
		byte[] bytes = val.getBytes();
		encodeNum(array,offset,4,bytes.length);
		System.arraycopy(bytes, 0, array, offset+4,bytes.length);
		return bytes.length;
	}
	static String decodeString(byte[] array,int offset,int len){
		//int len = decodeNum(array,offset,4);
		return new String(array,offset,len);
	}
	
	public static void main(String[] args){
		int i = 5634567;
		byte[] str = new byte[128];
		encodeNum(str, 5, 4, i);
		System.out.println("Decode:"+decodeNum(str,5,4));
		
		String s= "testaaaa";
		encodeString(str, 1, s);
		System.out.println("Decode str:"+decodeString(str, 1));
	}
}
