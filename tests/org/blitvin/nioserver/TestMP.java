package org.blitvin.nioserver;

public class TestMP implements MessageProcessor<TestClientContext> {

	@Override
	public void processData(TestClientContext clientContext) throws Exception {
		TestMessage msg = (TestMessage) ObjectEncoderDecoder.decode(clientContext.getRequest());
		//System.out.println("Got message"+ msg.id + ":"+msg.count);
		if (clientContext.lastId == 0 ) {
			clientContext.lastId = msg.id;
		} else if (clientContext.lastId != msg.id)
			throw new IllegalArgumentException(" message id mismatch, expecting "+ clientContext.lastId + "and got"+ msg.id);
		if (clientContext.lastCount+1 != msg.count)
			throw new IllegalArgumentException(" msg count wrong "+ msg.count +" less than last " + clientContext.lastCount);
		else
			clientContext.lastCount++;
		msg.count++;
		clientContext.setReply(ObjectEncoderDecoder.encode(msg));
	}

}
