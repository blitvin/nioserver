package org.blitvin.nioserver.throughputtest;

public class ClientStatistics {
	public String hostname;
	public int min;
	public int max;
	public int total;
	public int requestsDone;
	public int errors;

	public ClientStatistics(byte[] mesg, int offset, String hostname) {
		this.hostname = hostname;
		
		min = RemoteControlProtocol.decodeNum(mesg, offset + RemoteControlProtocol.MIN_LATENCY_OFFSET, 4);
		max = RemoteControlProtocol.decodeNum(mesg, offset + RemoteControlProtocol.MAX_LATENCY_OFFSET,4);
		total = RemoteControlProtocol.decodeNum(mesg, offset + RemoteControlProtocol.TOTAL_TIME_OFFFSET,4);
		requestsDone = RemoteControlProtocol.decodeNum(mesg, offset + RemoteControlProtocol.REQUESTS_DONE_OFFSET, 4);
		errors = RemoteControlProtocol.decodeNum(mesg, offset + RemoteControlProtocol.ERRORS_OFFSET, 4);
	}
	
	@Override
	public String toString(){
		return hostname+": min="+min+" max="+max+" avg="+(requestsDone>0?total/requestsDone:0)
				+ " requests="+requestsDone+ " errors="+errors;
	}
}