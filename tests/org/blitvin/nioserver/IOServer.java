package org.blitvin.nioserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.blitvin.nioserver.ClientContext;
import org.blitvin.nioserver.MessageProcessor;
import org.blitvin.nioserver.throughputtest.EchoMessageProcessor;

public class IOServer<CC extends ClientContext,MP extends MessageProcessor<CC>> implements Runnable  {

	private final MessageProcessorFactory<CC> mpFactory;
	private final ClientContextFactory<CC> ccFactory;
	private class ClientThread<CC extends ClientContext,MP extends MessageProcessor<CC>> extends Thread {
		final private Socket sock;
		final private InputStream is;
		final private OutputStream os;
		final private CC cc;
		final private LVServerProtocol protocol;
		
		private final byte[] headerBuf;
		private final ByteBuffer len;
		private final MessageProcessorFactory<CC> mpFactory;
		public ClientThread(Socket sock, CC cc,
							MessageProcessorFactory<CC> mpFactory) throws IOException{
			this.sock = sock;
			this.is = sock.getInputStream();
			this.os = sock.getOutputStream();
			this.cc = cc;
			this.mpFactory = mpFactory;
			this.protocol = new LVServerProtocol();
			headerBuf =  new byte[LVProtocol.HEADER_LENGTH];
			len = ByteBuffer.allocate(LVProtocol.HEADER_LENGTH);
			setDaemon(true);
			
		}
		@Override
		public void run() {
			ByteBuffer msgBuf = null;
			while(true){
				try {
					int msgLen = readLen();
					switch(msgLen){
					case(ClientServerProtocol.KEEPALIVE_NOTIFICATION):
						break;
					case(ClientServerProtocol.CLOSE_NOTIFICATION):
						sock.close();
						return;
					default:
						if (msgLen > 0) {
							byte[] msg = new byte[msgLen];
							readBytes(msg, msgLen);
							if (msgBuf == null || msgBuf.capacity() != msgLen+4) 
								msgBuf = ByteBuffer.allocate(msgLen+4);
							else
								msgBuf.clear();
							msgBuf.putInt(msgLen);
							msgBuf.put(msg);
							msgBuf.flip();
							protocol.addPart(msgBuf);
							MP mp = null;
							if (!cc.shouldRetainMP() || cc.mp == null ) {
								mp = (MP)mpFactory.newInstance();
								cc.setMessageProcessor(mp);
							}
							else
								mp = (MP)cc.mp;
							try {
								mp.processData(cc);
								byte[] reply = new byte[cc.outboundData.remaining()];
								cc.outboundData.get(reply);
								os.write(reply);
								os.flush();
								cc.outboundData  = null;
								protocol.inboundMessageHasBeenConsumed();
								if (!cc.shouldRetainMP()) {
									MessageProcessor<CC> mpt =  cc.unlinkMessageProcessor();
									if (mpFactory.cacheInstances())
										mpFactory.reclaimUnused(mpt);
								}
							} catch (Exception e) {
								System.err.println("exception in data proc"+e.toString());
								e.printStackTrace();
							}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					try {
							sock.close();
						} catch (IOException ee) {
							// TODO Auto-generated catch block
							ee.printStackTrace();
						}
					return;
				}
			}
			
		}
		
		void readBytes(byte[] buf,int len) throws IOException {
			int chunk=0, off = 0;
			
			do {
				chunk = is.read(buf, off, len - off);
				if (chunk == -1)
 
				off += chunk;
			} while (off < len);
		}
		 int readLen() throws IOException {
				readBytes(headerBuf,LVProtocol.HEADER_LENGTH);
				len.clear();
				len.put(headerBuf);
				len.flip();
				return len.getInt();
			}
	}
	private final ServerSocket sock;
	public IOServer(int port, ClientContextFactory<CC> ccFactory, 
			MessageProcessorFactory<CC> mpFactory) throws IOException{
		sock = new ServerSocket(port);
		this.ccFactory = ccFactory;
		this.mpFactory = mpFactory;
	}

@Override
public void run() {
	while(true) {
		try {
			Socket newCon = sock.accept();
			ClientThread<CC,MP> th = new ClientThread<CC,MP>(newCon,ccFactory.newInstance(),mpFactory);
			th.start();
		} catch (IOException e) {
			// TBD real handling
			e.printStackTrace();
		}
	}
	
	
}

}

