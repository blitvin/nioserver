package org.blitvin.nioserver;

public class TestBytesMP implements MessageProcessor<TestClientContext> {

	@Override
	public void processData(TestClientContext clientContext) throws Exception {
		byte[] msg = clientContext.getRequest();
		if (msg.length != 2)
			throw new IllegalArgumentException(" message length expected to be 2, actually is "+ msg.length);
		if (clientContext.lastId == 0 ) {
			clientContext.lastId = msg[0];
		} else if (clientContext.lastId != msg[0])
			throw new IllegalArgumentException(" message id mismatch, expecting "+ clientContext.lastId + "and got"+ msg[0]);
		if (clientContext.lastCount+1 != msg[1])
			throw new IllegalArgumentException(" msg count wrong "+ msg[1] +" less than last " + clientContext.lastCount);
		else
			clientContext.lastCount++;
		msg[1]++;
		clientContext.setReply(msg);

	}

}
