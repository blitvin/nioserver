package org.blitvin.nioserver.throughputtest;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.blitvin.nioserver.LVClientHelper;
import org.blitvin.nioserver.RemoteExecutionException;

public class TestRunner  implements Runnable{
	final private int idx;
	final private String hostname;
	final private RemoteControlProtocol testProtocol;
	
	public TestRunner(String hostname,int port, int idx, 
			RemoteControlProtocol testProtocol ){
		this.idx = idx;
		this.hostname = hostname;
		this.testProtocol = testProtocol;
	}
	@Override
	public void run() {
		
		int msgSize = testProtocol.getValue(RemoteControlProtocol.MSG_SIZE_IDX);
		byte[] message = new byte[8 + msgSize];
		RemoteControlProtocol.encodeNum(message, 0, 4, idx);
		int id = testProtocol.getValue(RemoteControlProtocol.ID_IDX) + idx;
		for(int i = 0 ; i< msgSize; ++i)
			message[8+i] = (byte)(i &0xFF);
		int requestsDone = 0;
		int maxLatency = 0;
		int minLatency = Integer.MAX_VALUE;
		long totalTime = 0;
		int errors = 0;
		byte[] reply;
		try {
			LVClientHelper client = new LVClientHelper(hostname,testProtocol.getValue(RemoteControlProtocol.PORT_IDX));
			int worktime = testProtocol.getValue(RemoteControlProtocol.WORKING_TIME_IDX);
			System.out.println("waiting for test start");
			testProtocol.waitForTestStartInThread();
			long endTime = System.currentTimeMillis() + worktime;
			
			do {
				long startTime = System.nanoTime();
				reply = client.sendRequest(message);
				int delta = (int)(System.nanoTime() - startTime);
				if (delta < minLatency)
					minLatency = delta;
				if (delta > maxLatency)
					maxLatency = delta;
				totalTime += delta;
				requestsDone++;
				if (!Arrays.equals(message, reply))
					errors++;
				else
					++requestsDone;
				for(int i = 4 ; i< 8 ; ++i ){
					if (message[i] == 127 || message[i] < 0) {
						message[i] = 0;
					} else {
						message[i]++;
						break;
					}
					
				}
			}while (System.currentTimeMillis() < endTime);
			
		} catch (UnknownHostException e) {
			System.err.println("Thread "+id +":Got UnknownHostException"+e.toString()+ ":"+hostname);
		} catch (IOException e) {
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		testProtocol.reportResults(idx, requestsDone, maxLatency, minLatency, (int)(totalTime /1000000L), errors);
		System.out.println("ID="+id+" requests="+requestsDone + " errors="+errors + "minLatency="+minLatency + " maxLatency="+maxLatency);
	}
}
