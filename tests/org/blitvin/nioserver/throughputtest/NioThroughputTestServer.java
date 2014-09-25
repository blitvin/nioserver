package org.blitvin.nioserver.throughputtest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.blitvin.nioserver.ClientContext;
import org.blitvin.nioserver.NioServer;

public class NioThroughputTestServer {
	private NioServer<ClientContext,EchoMessageProcessor> server;
	//private NioServer<ClientContext,TestMessageProcessor> server;
	private int timeout;
	private int numOfClients;
	private final AtomicReference<ClientStatistics>[] results;
	//private final AtomicInteger allocatedOffsets;
	private final CountDownLatch startLatch;
	private final CountDownLatch endLatch;
	private int threadsPerClient;
	private final Properties p;
	private ServerSocket controlSocket;
	public final CountDownLatch clientInitializedLatch;
	public static final String PORT_PROPERTY_NAME = "port";
	public static final String MSG_SIZE_PROPERTY_NAME = "message_size";
	public static final String NUM_OF_CLIENTS_PROPERTY_NAME ="clients";
	public static final String NUM_OF_CLIENT_THREADS_PROPERTY_NAME ="client_threads";
	public static final String WORKING_TIME_PROPERTY_NAME = "working_time";
	public static final String PING_ONLY_CONNECTIONS_PROPERTY_NAME ="ping_only_connections";
	public static final String PING_INTERVAL_PROPERTY_NAME ="ping_interval";
	public static final String CONTROL_PORT_PROPERTY_NAME = "control";
	private static final String CLIENT_CONTEXT_CLASS = null;
	
	//public static final int REPLY_LEN_PER_THREAD = 0;
	/* setting positive int in byte array 
	private void setMsgVal(byte[] storage,Properties p,String propertyName, int offset, int size){
		int val = 0;
		try {
			val = Integer.parseInt(p.getProperty(propertyName));
			for(int i = 1 ; i<= size ; i++){
				storage[offset+size -i] = (byte)(0xFF & val);
				val >>= 8;
			}
		}
		catch(NumberFormatException ex){
			System.err.println("Can't parse string parameter" + propertyName+ 
					" value="+System.getProperty(propertyName));
			throw ex;
		}
	}
	*/
	
	
	public NioThroughputTestServer(Properties p) throws IOException {
		this.p = p;
		numOfClients = Integer.parseInt(p.getProperty(NUM_OF_CLIENTS_PROPERTY_NAME));
		threadsPerClient = Integer.parseInt(p.getProperty(NUM_OF_CLIENT_THREADS_PROPERTY_NAME));
		results = new AtomicReference[numOfClients * threadsPerClient];
		startLatch = new CountDownLatch(1);
		clientInitializedLatch = new CountDownLatch(numOfClients);
		endLatch = new CountDownLatch(numOfClients);
		try {
			int port = Integer.parseInt(p.getProperty(CONTROL_PORT_PROPERTY_NAME));
			controlSocket = new ServerSocket(port);
			
		} catch (IOException e) {
			System.err.println("failed to allocate socket");
			throw e;
		}
		System.out.println("administrative port is "+controlSocket.getLocalPort());
		int port = Integer.parseInt(p.getProperty(PORT_PROPERTY_NAME));
		System.out.println("server port is "+port);
		TestMessageProcessor.MSG_LEN = Integer.parseInt(p.getProperty(MSG_SIZE_PROPERTY_NAME));
		server = new NioServer<>(port, ClientContext.class,EchoMessageProcessor.class,null);
	}
	
	private static class ClientComThread extends Thread{
		
		NioThroughputTestServer parent;
		Socket sock;
		int offset;
		int id;
		public ClientComThread(NioThroughputTestServer parent, Socket sock, int offset,int id){
			super();
			this.parent = parent;
			this.sock = sock;
			this.offset = offset;
			this.id = id;
		}
		
		private void readBytes(InputStream is, byte[] arr,int len) throws IOException{
			int readsofar = 0;
			int chank = 0;
			while (readsofar < len) {
				chank = is.read(arr,readsofar,len - readsofar);
				if (chank == -1) {
					throw new IOException("premature close of client socket");
				}
				readsofar += chank;
			}
		}
		static int getIntValue(Properties p, String propertyName, String hostName){
			String strVal;
			strVal = p.getProperty(hostName+'.'+propertyName);
			if (strVal == null){
				strVal = p.getProperty(propertyName);
			}
			try {
				return Integer.parseInt(strVal);
			} catch(NumberFormatException ex){
				System.err.println("Can't parse string parameter" + propertyName+ 
						" value="+strVal);
				throw ex;
			}
		}
		
		@Override
		public void run(){
			try {
				
				InputStream is = sock.getInputStream();
				OutputStream os = sock.getOutputStream();
				byte[] lenBuf = new byte[4];
				readBytes(is, lenBuf, 4);
				int hostNameLen = RemoteControlProtocol.decodeNum(lenBuf,0,4);
				byte[] stringBuf = new byte[hostNameLen];
				readBytes(is, stringBuf, hostNameLen);
				String clientHostName = RemoteControlProtocol.decodeString(stringBuf, 0, hostNameLen);
				byte[] setupMessage = RemoteControlProtocol.buildSetupMessage(
						getIntValue(parent.p,PORT_PROPERTY_NAME, clientHostName),
						getIntValue(parent.p,MSG_SIZE_PROPERTY_NAME, clientHostName),
						getIntValue(parent.p, NUM_OF_CLIENT_THREADS_PROPERTY_NAME,clientHostName),
						getIntValue(parent.p, WORKING_TIME_PROPERTY_NAME,clientHostName), 
						getIntValue(parent.p, PING_ONLY_CONNECTIONS_PROPERTY_NAME, clientHostName),
						getIntValue(parent.p, PING_INTERVAL_PROPERTY_NAME, clientHostName),id);
				os.write(setupMessage);
				parent.clientInitializedLatch.countDown();
				parent.startLatch.await();
				os.write(lenBuf,0,1);
				// now client starts working and is going to send result upon completion
				int reply_bytes = RemoteControlProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK * parent.threadsPerClient;
				byte[] reply = new byte[reply_bytes];
				readBytes(is,reply,reply_bytes);
				for(int i = 0 ; i < parent.threadsPerClient; ++i){
					parent.results[offset+i] = new AtomicReference<>(
							new ClientStatistics(reply , i* RemoteControlProtocol.RESULT_MESSAGE_PER_CLIENT_THREAD_CHUNK,clientHostName));
				}
			} catch (IOException ex) {
				// put interruption of main thread
				// ex.printStackTrace();
			} catch (InterruptedException e) {
				
			}
			finally {
				parent.endLatch.countDown();
				try {
					sock.close();
				} catch (IOException e) {
				}
			}
			
		}
	}
	
	
	
	private void  test(){
		new Thread(server).start();
		int offset = 0;
		for(int i = 0 ; i< numOfClients; ++i){
			try {
				Socket cl = controlSocket.accept();
				ClientComThread comThread = new ClientComThread(this, cl,  offset, i*100);
				comThread.start();
				offset +=  threadsPerClient;
				System.out.println("Got connection "+i);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			clientInitializedLatch.await();
			System.out.println("All clients connected");
			Thread.sleep(1000); // to ensure last client got setup
			startLatch.countDown();// benchmark start
			endLatch.await();
			System.out.println("All clients finished");
			server.shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();  
		}
		int totalRequests = 0;
		int minLatency = Integer.MAX_VALUE;
		int maxLatency = 0;
		int totalErrors = 0;
		int totTime = 0;
		for(int i = 0 ; i < results.length; ++i){
			if (results[i] == null)
				continue;
			ClientStatistics s = results[i].get();
			System.out.println(s.toString());
			totalRequests += s.requestsDone;
			if (minLatency > s.min)
				minLatency = s.min;
			if (maxLatency < s.max)
				maxLatency = s.max;
			totTime += s.total;
			totalErrors += s.errors;
		}
		System.out.println(" Total: min="+minLatency+" max="+maxLatency+" avg="+
				(totalRequests>0?totTime/totalRequests:0)+ " requests="+ totalRequests+ 
				" errors="+totalErrors + " Requests/sec = "+ (1000L * totalRequests/ Integer.parseInt(p.getProperty(WORKING_TIME_PROPERTY_NAME))));
	}
	public static void main(String[] args){
		
		Properties p = new Properties();
		if (args.length != 1){
			System.err.println("Usage : java org.blitvin.nioserver.NioThroughputTestServer properties_file");
			return;
		}
		try {
			p.load(new BufferedReader(new FileReader(args[0])));
		} catch (IOException e) {
			System.err.println(args[0]+" is not a valid properties file");
			return;
		}
		
		
		try {
			NioThroughputTestServer testServer = new NioThroughputTestServer(p);
			testServer.test();
		} catch (IOException e) {
			System.err.println("org.blitvin.nioserver.NioThroughputTestServer got IOException:"+e);
			e.printStackTrace();
		}
	}
}
