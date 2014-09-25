package org.blitvin.nioserver.throughputtest;

import org.blitvin.nioserver.ClientContext;
import org.blitvin.nioserver.MessageProcessor;

public class TestMessageProcessor implements MessageProcessor<ClientContext> {

	public static int MSG_LEN = 0;
	@Override
	public void processData(ClientContext clientContext) throws Exception {
		byte[] msg = clientContext.getRequest();
		for(int i = 0 ; i< MSG_LEN; ++i)
		{
			if ((0xFF & msg[i+8]) != (i &0xFF))
				System.out.println("Error at position "+i);
			
		}
		clientContext.setReply(msg);
	}

}
