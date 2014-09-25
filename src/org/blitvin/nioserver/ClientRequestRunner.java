/*
 * (C) Copyright Boris Litvin 2014
 * This file is part of NioServer library.
 *
 *  NioServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   NioServer is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with NioServer.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.blitvin.nioserver;

import java.nio.channels.Selector;
import java.util.concurrent.LinkedBlockingQueue;
/**
 *  ClientRequestRunner is a wrapper that ensures proper execution of MessageProcessor and
 *  translation of results (including reply, empty response, exception etc.) back to main 
 *  NioServer thread.Instances of ClientRequestRunner are supplied to a thread pool to run
 * 
 */
class ClientRequestRunner<CC extends ClientContext> implements Runnable {

	Selector selector;
	CC context; 
	LinkedBlockingQueue<CC> replyQueue;
	ServerSideProtocol protocol;
	
	public ClientRequestRunner(Selector selector, CC context, 
							LinkedBlockingQueue<CC> replyQueue,
							ServerSideProtocol protocol){
		this.selector = selector;
		this.context = context;
		this.replyQueue = replyQueue;
		this.protocol = protocol;
		
	}
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		try {
			context.setRequest(protocol.getRequest());
			context.mp.processData(context);
			context.setOutboundData(protocol.encodeMessage(context.getReply()));
		}
		catch (Exception e){
			context.setOutboundData(protocol.putExceptionReply(e));
		}
		finally {
			protocol.inboundMessageHasBeenConsumed();
			context.setReply(null);
			context.setRequest(null);
		}
		
		replyQueue.add(context);
		selector.wakeup();
	}

}
