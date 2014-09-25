package org.blitvin.nioserver.throughputtest;

import org.blitvin.nioserver.ClientContext;
import org.blitvin.nioserver.MessageProcessor;

public class EchoMessageProcessor implements MessageProcessor<ClientContext> {

	@Override
	public void processData(ClientContext clientContext) throws Exception {
		clientContext.setReply(clientContext.getRequest());
	}

}
