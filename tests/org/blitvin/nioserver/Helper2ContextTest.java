package org.blitvin.nioserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Helper2ContextTest {

	PipedOutputStream cl2ccOut; 
	PipedOutputStream cc2clOut;
	PipedInputStream cl2ccIn; 
	PipedInputStream cc2clIn;
	ClientContext context ;
	ByteBuffer buf ;
	LVClientHelper helper;
	LVServerProtocol prot;
	
	@Before
	public void setUp() throws Exception {
		 cl2ccOut = new PipedOutputStream();
		 cc2clOut = new PipedOutputStream();
		 cl2ccIn = new PipedInputStream(cl2ccOut);
		 cc2clIn = new PipedInputStream(cc2clOut);
		 context = new ClientContext(new TestMessageProcessor<ClientContext>());
		 buf = ByteBuffer.allocate(4096);
		 prot = new LVServerProtocol();
		 helper= new LVClientHelper(cc2clIn, cl2ccOut);
	}

	@After
	public void tearDown() throws Exception {
		cl2ccIn.close();
		cl2ccOut.close();
		cc2clIn.close();
		cc2clOut.close();
	}
	@Test
	public void test() throws Exception {
		byte[] msg = {1,2,3,4,5,6,7};
//		helper.sendMessage(msg);
		byte[] encoded = helper.encodeBytes(msg);
		cl2ccOut.write(encoded);
		byte[] dec = new byte[encoded.length];
		cl2ccIn.read(dec, 0, dec.length);
		prot.addPart(ByteBuffer.wrap(dec));
		context.mp.processData(context);
		byte[] ret = new byte[context.outboundData.capacity()];
		context.outboundData.get(ret);
		cc2clOut.write(ret);
		int len = helper.readLen();
		assertTrue(len > 0);
		byte[] res = new byte[len];
		helper.readBytes(res, len);
		assertEquals(len,msg.length);
		for(int i = 0; i < msg.length; ++i)
			assertEquals(res[i], msg[i]);
		
		
	}
	
	void readObjectFromStreamToContext() throws Exception {
		byte[] objLen = new byte[LVProtocol.HEADER_LENGTH];
		cl2ccIn.read(objLen, 0, LVProtocol.HEADER_LENGTH);
		ByteBuffer b = ByteBuffer.wrap(objLen);
		b.mark();
		int objIntlen = b.getInt();
		assertTrue(objIntlen >= 0);
		b.reset();
		prot.addPart(b);
		byte[] encoded  = new byte[objIntlen];
		cl2ccIn.read(encoded,0,objIntlen);
		prot.addPart(ByteBuffer.wrap(encoded));
	}
	
	@Test
	public void testNullArgs() throws Exception {
		cl2ccOut.write(helper.encodeObject(null));
		readObjectFromStreamToContext();
		assertNull(ObjectEncoderDecoder.decode(prot.getRequest()));
		context.outboundData = prot.encodeMessage(ObjectEncoderDecoder.encode(null));
		byte[] dst = new byte[context.outboundData.capacity()];
		context.outboundData.get(dst);
		cc2clOut.write(dst);
		int len= helper.readLen();
		assertTrue(len > 0);
		byte[] res = new byte[len];
		helper.readBytes(res, len);
		assertNull(helper.decodeObject(res, len));
	}
	
	/*private int readIntFromArray(byte[] dst) {
		readBytes(headerBuf,ClientContext.HEADER_LENGTH);
		len.clear();
		len.put(headerBuf);
		len.flip();
		return len.getInt();
	}*/
	@Test
	public void testExceptionReply() throws Exception {
		prot.putExceptionReply(new IOException("test ioexception"));
		byte[] dst = new byte[context.outboundData.capacity()];
		context.outboundData.get(dst);
		cc2clOut.write(dst);
		int len= helper.readLen();
		assertTrue(len < 0);
		byte[] exceptionBytes = new byte[-len];
		try {
			helper.readBytes(exceptionBytes, len);
			fail("expecting to throw exception");
		}
		catch(IOException e) {
			assertEquals("test ioexception", e.getMessage());
		}
	}
}
