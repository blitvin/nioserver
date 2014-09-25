package org.blitvin.nioserver.throughputtest;

import java.io.IOException;

public class NioThroughputTestClient2 {
	public static void main(String[] args) {
		if(args.length != 2){
			System.err.println("Usage: java obj.blitvin.nioserver.throughputtest.NioThroughputTestClient2 host port");
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
		
		RemoteControlProtocol admConnection = null;
		try {
			admConnection =  new RemoteControlProtocol(args[0], port);
			
		} catch ( IOException e) {
			System.err.println("NioThroughputTestClient(): got exception"+e);
			e.printStackTrace();
		}
		
		int threadNum = admConnection.getValue(RemoteControlProtocol.NUM_OF_THREADS_IDX);
		int serverPort= admConnection.getValue(RemoteControlProtocol.PORT_IDX);
		Thread[] threads = new Thread[threadNum];
		
		for(int i = 0 ; i< threadNum; ++i) {
			threads[i] = new Thread(new TestRunner(args[0], serverPort, i, admConnection));
			threads[i].start();
		}
		admConnection.waitForTestStart();
		for(int i = 0 ; i< threadNum; ++i)
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				
			}
		admConnection.sendResultsAndCloseConnection();
	}
}
