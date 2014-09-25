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

import java.io.IOException;
import java.net.InetSocketAddress;
//import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * NioServer is NIO based server( as its name suggests). 
 * Regular net.Socket require allocating thread for each active connection. Nio allows to maintain 
 * thread pool with number of workers as number of concurrent requests.
 * The framework assumes syncroneous work on each channel i.e. client sends request and waits for 
 * the response. For better usability, ClientHelper class is provided. It takes care of all the protocol 
 * matters including remote exception handling, one need only use sendRequest(byte[]) for request.
 * NioServer is parameterized by two types - client context and message processor.
 * MessageProcessor is interface defining method processData(ClientContext) which represent business logic
 * of provided service.
 * ClientContext is conversational state of the requests. Normally, all the data that needs to be stored 
 * between requests should be placed in a class that extends ClientContext and MessageProcessor should be
 * stateless. NioServer provides a way to retain the same MessageProcessor object for consequent requests,
 * by invoking ClientContext method requestMessageProcessorRetention(true), but this should be used with
 * care because retention of message processor can increase memory footprint of the application.
 * 
 * Usage of the package should be something like:
 * on server 
 *    new Thread(new NioServer<CC,MP>(port,CC.class, MP.class)).start();
 *    
 * and on client
 *    helper = new ClientHelper("hostname", port);
 *   ...
 *   reply = helper.sendMessage(arg);
 *   ...
 *   reply = helper.sendMessage(arg);
 *   ...
 *   
 *   helper.closeConnection()
 *  
 * For more sophisticated management of thread pools, control of creation of 
 * ClientContext and MessageProcessor objects during NioServer work, on can use 
 * NioServer(NioServerInitializer) constructor. It follows builder pattern, one specifies 
 *  parameters for tuning of the NioServer execution.
 * 
 * @author blitvin
 *
 */
public class NioServer<CC extends ClientContext,MP extends MessageProcessor<CC>> implements Runnable {
	
	
	// The channel on which we'll accept connections
		  private final ServerSocketChannel serverChannel;
		  
		  // The selector we'll be monitoring
		  private final Selector selector;

		  // The buffer into which we'll read data when it's available
		  private ByteBuffer readBuffer; 
		  
		  private final ExpirationManager<SocketChannel> expirationMgr;
		  private final HashMap<SocketChannel,ServerContext<CC>> channelContexts; // business logic state corresponding to client
		  private final HashMap<SocketChannel, ServerContext<CC> > outboundData;
		  private final HashMap<CC,ServerContext<CC>> workingContexts;
		  private final LinkedBlockingQueue<CC> replyQueue;
		  private final ExecutorService threadPool;
		  private final MessageProcessorFactory<CC> mpFactory;
		  private final ClientContextFactory<CC> ccFactory;
		  private final ServerSideProtocolFactory protFactory;
		 
		  private boolean shutdownRequestd = false;
		  private ErrorListener errorListener;
		  
	@Override	  
	public void run() {
		expirationMgr.start();
		while (true) {
		      try {
		        // Wait for an event one of the registered channels
		        selector.select();
		        if (registerOutboundDataAndCheckShouldExit()) {
		        	expirationMgr.stop();
		        	shutdownRequestd = true;
		        }
		        // Iterate over the set of keys for which events are available
		        Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
		        while (selectedKeys.hasNext()) {
		          SelectionKey key =  selectedKeys.next();
		          selectedKeys.remove();

		          if (!key.isValid()) {
		            continue;
		          }

		          // Check what event is available and deal with it
		          if (key.isAcceptable() && !shutdownRequestd) {
		            accept(key);
		          } else if (key.isReadable() && !shutdownRequestd) {
		              read(key);
		          } else if (key.isWritable()) {
		        	  write(key);
		          }
		        }
		        if (shutdownRequestd && workingContexts.isEmpty()) {
		        	threadPool.shutdown();
		        	for(SocketChannel channel :channelContexts.keySet()){
		        		closeConnection(channel.keyFor(selector), channel);
		        	}
		        	return;
		        }
		      } catch (Exception e) {
		    	if (errorListener != null)
		    		errorListener.notifyException(e);
		      }
		    }
	}
	private void write(SelectionKey key) throws IOException {
		SocketChannel channel2Write = (SocketChannel)key.channel();
		ServerContext<CC> context= outboundData.get(channel2Write);
		ByteBuffer outboundBuffer = context.getClientContext().outboundData;
		channel2Write.write(outboundBuffer);
		if(outboundBuffer.remaining() == 0) {
			context.getClientContext().outboundData = null;
			outboundData.remove(key);
			if (context.getProtocol().hasCompleteMessage()) {// already have complete message
				proceedWithMessage(context);
				
			}
			else {
				workingContexts.remove(context.clientContext);
				
				
				if (shutdownRequestd) {
					return; //don't accept further requests from this channel
				}
				key.interestOps(SelectionKey.OP_READ);
				context.expirationManagerHint = expirationMgr.add(channel2Write);
			}
		}
	}
	/**
	 * 
	 * @return true if server is to shut down
	 */
	private boolean registerOutboundDataAndCheckShouldExit() {
		CC reply = null;
		while((reply = replyQueue.poll()) != null) {
			ServerContext<CC> serverContext = workingContexts.get(reply);
			if (serverContext == null) { 
				return true;
			}
			if (!reply.shouldRetainMP() || reply.isSessionEnded()) {
				@SuppressWarnings("unchecked")
				MessageProcessor<CC> mp = reply.unlinkMessageProcessor();
				if (mpFactory.cacheInstances()){
					mpFactory.reclaimUnused(mp);
				}
			}
			outboundData.put(serverContext.channel, serverContext /*reply*/);
			try {
				serverContext.channel.register(selector, SelectionKey.OP_WRITE);
			} catch (ClosedChannelException e) {
				// channel is closed , no one listens, nothing to do..
				outboundData.remove(serverContext.channel);
			}
		}
		
		return false;
	}
	
	

	private void accept(SelectionKey key) throws IOException {
	    // For an accept to be pending the channel must be a server socket channel.
	    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

	    // Accept the connection and make it non-blocking
	    SocketChannel socketChannel = serverSocketChannel.accept();
	    socketChannel.configureBlocking(false);
	    
	    //CC cont = ccFactory.newInstance();
	    ServerContext<CC> serverContext = new ServerContext<CC>(protFactory.newInstance(),
	    		socketChannel,ccFactory.newInstance());
	   
	    channelContexts.put(socketChannel, serverContext);
	    serverContext.expirationManagerHint = expirationMgr.add(socketChannel);

	    // Register the new SocketChannel with our Selector, indicating
	    // we'd like to be notified when there's data waiting to be read
	    socketChannel.register(this.selector, SelectionKey.OP_READ);
	    Collection<SocketChannel> expired = expirationMgr.getExpired();
	    if (!expired.isEmpty()) {
	    	for(SocketChannel expiredChannel: expired) {
	    		closeConnection(expiredChannel.keyFor(selector),expiredChannel );
	    	}
	    }
	  }


	@SuppressWarnings("unchecked")
	private void closeConnection(SelectionKey key,SocketChannel socketChannel) throws IOException {
//		closes++;
		key.cancel();
	    key.channel().close();
	    ServerContext<CC> tmp =channelContexts.remove(socketChannel);
	    expirationMgr.remove(socketChannel, tmp.expirationManagerHint);
	    if (tmp.getClientContext().shouldRetainMP()) {
	      mpFactory.reclaimUnused(tmp.getClientContext().unlinkMessageProcessor());
	    }
	}
	
	private void read(SelectionKey key) throws IOException {
	    SocketChannel socketChannel = (SocketChannel) key.channel();

	   
	    readBuffer.clear();
	    
	    // Attempt to read off the channel
	    int numRead;
	    try {
	      numRead = socketChannel.read(readBuffer);
	    } catch (IOException e) {
	      // The remote forcibly closed the connection, cancel
	      // the selection key and close the channel.
	     closeConnection(key, socketChannel);
	     return;
	    }

	    if (numRead == -1) {
	      // Remote entity shut the socket down cleanly. Do the
	      // same from our end and cancel the channel.
	     closeConnection(key, socketChannel);
	      return;
	    }
	    
	    readBuffer.flip();
	    ServerContext<CC> context = channelContexts.get(socketChannel);
	    if (context.protocol.addPart(readBuffer)) {
	    // has complete message	
	    	if (filterServiceMessage(context, key, socketChannel)) { 
	    		socketChannel.keyFor(selector).cancel();
	    		expirationMgr.remove(socketChannel, context.expirationManagerHint);
	    		workingContexts.put(context.getClientContext(), context);
	    		proceedWithMessage(context);
	    	}
	    }
	    
	  }
	
	/*
	 * returns true if message is not service message
	 * otherwise performs appropriate actions and return false
	 */
	private boolean filterServiceMessage(ServerContext<CC> context, SelectionKey key, SocketChannel socketChannel) throws IOException {
		switch (context.protocol.getServiceMessage()) {
		case ClientServerProtocol.CLOSE_NOTIFICATION:
//			closeNotifications++;
			closeConnection(key, socketChannel);
			return false;
		case ClientServerProtocol.KEEPALIVE_NOTIFICATION:
			expirationMgr.touch(socketChannel, context.expirationManagerHint);
			return false;
		default:
			return true;
		}
	}
	
	private void proceedWithMessage(ServerContext<CC> context){
		CC c = context.getClientContext();
		if (!c.shouldRetainMP() || c.mp == null) {
    		c.setMessageProcessor(mpFactory.newInstance());
    	}
    	threadPool.execute(new ClientRequestRunner<CC>(selector, c, replyQueue,context.getProtocol()));
	}
	
	/**
	 *  This constructor works only if -Dorg.blitvin.nioserver.clientContextClass and 
	 *  -Dorg.blitvin.nioserver.messageProcessorClass are defined in system properties
	 * @param port - port for the server to bind to
	 * @throws IOException
	 */
	public NioServer(int port) throws IOException{
		this(port,null,null,null);
	}
	public NioServer(int port, Class<CC> clientContextClass, 
			Class<MP> messageProcessorClass, Class<ServerSideProtocol> protocolClass) throws IOException{
		this(new NioServerInitializer<>(port, clientContextClass, 
				messageProcessorClass,protocolClass));
	}
	/**
	 * This constructor can be used to control various aspects of NioServer execution e.g. thread pool tuning can 
	 * be done by creating thread pool with desired characteristics, passing to NioServerInitializer and then passing
	 * the initializer to the constructor
	 * @see NioServerInitializer 
	 * @param initializer - builder object with all relevant settings set up
	 * @throws IOException
	 */
	public NioServer(NioServerInitializer<CC, MP> initializer) throws IOException {
		threadPool = initializer.getThreadPool();
		readBuffer = ByteBuffer.allocate(initializer.getBufferSize());
		// Create a new non-blocking server socket channel
	    serverChannel = ServerSocketChannel.open();
	    serverChannel.configureBlocking(false);
	    InetSocketAddress isa = new InetSocketAddress(initializer.getPort());
	    serverChannel.socket().bind(isa);
	    
	    channelContexts = new HashMap<SocketChannel,ServerContext<CC>>();
	    outboundData = new HashMap<SocketChannel,ServerContext<CC>>();
	    workingContexts = new HashMap<>();
	    replyQueue = new LinkedBlockingQueue<>();
	    
	    mpFactory = initializer.getProcessorFactory();
	    ccFactory = initializer.getContextFactory();
	    protFactory = initializer.getProtocolFactory();
	    selector = SelectorProvider.provider().openSelector();
	    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	    expirationMgr = initializer.getExpirationManager();
	    errorListener = initializer.getErrorListener();
	}

	/**
	 * can be called to request shut down. The shutdown is not immediate - the server tries to proceed 
	 * requests that it had already got, and only after that proper shutdown (close connection, shutdown of 
	 * thread pool etc.) is performed. Also note that shutdown procedure is not initiated immediately.
	 */
	public void shutdown(){
		replyQueue.add(ccFactory.newInstance());// possible race , if newInstance is not threadsafe
		selector.wakeup();
	}
	
}
