package org.blitvin.nioserver;

public class TestMessageProcessor<CC extends ClientContext> implements MessageProcessor<CC> {

	@Override
	public void processData(CC clientContext) {
		byte[] msg = clientContext.getRequest();
		for(int i = 0 ; i < msg.length; ++i)
			System.out.print(':' + (0xFF & msg[i]));
		System.out.println();
		clientContext.setReply(msg);
		
	}

}
