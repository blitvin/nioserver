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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * This helper class provided for use on client. It provide means to remotely invoke service
 * on NioServer with provided parameters. The calls are syncroneos, i.e. sendRequest doesn't
 * return until reply is received. Technical details like keepalive notifications, 
 * remote exceptions etc. are handled by the class 
 * 
 * @author blitvin
 *
 */
public class ClientHelper {
	private ClientSideProtocol protocol;
	private SocketChannel channel;
	private final ByteBuffer input ;
	private Selector selector;
	
	/**
	 * Constructor receiving socketchannel already connected to the server, and protocol handler
	 * @param channel open channel connected to nioserver
	 * @param protocol protocol handler object. If this parameter is null, default implementation
	 * assumed
	 * @throws IOException
	 */
	public ClientHelper(SocketChannel channel, ClientSideProtocol protocol) throws IOException{
		this.channel  = channel;
		this.channel.configureBlocking(false);
		if (protocol == null) 
			this.protocol  = new LVClientProtocol();
		else
			this.protocol = protocol;
		
		Integer bufs = Integer.getInteger(System.getProperty(NioServerInitializer.BUFFER_SIZE_STRING));
		int bufferSize = (bufs != null)?bufs.intValue():NioServerInitializer.DEFAULT_BUFFER_SIZE;
		input = ByteBuffer.allocate(bufferSize);
		selector = SelectorProvider.provider().openSelector();
		this.channel.register(this.selector, SelectionKey.OP_READ);
	}
	
	/**
	 * constructor opening channel to nioserver specified by hostname and port and 
	 * asuming usage of default (LV) protocol
	 * @param hostname - name of host nioserver runs on
	 * @param port - port of nioserver on the host
	 * @throws IOException
	 */
	public ClientHelper(String hostname,int port) throws IOException{
		this(hostname,port,null);
	}
	
	/**
	 * constructor that opens connection to nioserver and allows specification of 
	 * protocol handler object
	 * @param hostname - host name nioserver runs on
	 * @param port - port of nioserver
	 * @param protocol - protocol handling object
	 * @throws IOException
	 */
	public ClientHelper(String hostname, int port,ClientSideProtocol protocol) throws IOException {
		this(SocketChannel.open(new InetSocketAddress(hostname, port)),protocol);
	}
	
	/**
	 * call this method when you wish to close connection to the nioserver
	 */
	public void closeConnection(){
		try {
			channel.write(protocol.sendServiceMessage(ClientServerProtocol.CLOSE_NOTIFICATION));
			channel.close();
			channel.keyFor(selector).cancel();
			channel = null;
			selector = null;
		} catch (IOException e) {
		}
	}
	/**
	 * cleanup method that supposed to catch  case of closeConnection is not called
	 */
	@Override
	public void finalize(){
		if (channel != null ) {
			try {
				channel.close();
			} catch (IOException e) {
			
			}
			channel.keyFor(selector).cancel();
		}
			
	}
	/**
	 * send keepalive ping. This notifies server that connection is still active even if no
	 * data passes for a timeout. In absence of data passage and keepalive notifications
	 * servers closes connection after some timeout 
	 * @see NioServerInitializer
	 */
	public void keepAlive(){
		try {
			channel.write(protocol.sendServiceMessage(ClientServerProtocol.KEEPALIVE_NOTIFICATION));
		} catch (IOException e) {
			// TBD what if transmission fails?
		
		}
	}
	
	/**
	 * this method sends request and retrieves responce from the server. The method handles 
	 * remote exceptions i.e. if business logic code throws exception, the method throws 
	 * {@link RemoteExecutionExcdeption} with cause field containing deserialized exception
	 * as caught by the framework
	 * @param msg parameter of the request call
	 * @return response of the server
	 * @throws IOException 
	 * @throws ClassNotFoundException - this exception is thrown if exception happen on server
	 * is not recognized on client
	 * @throws RemoteExecutionException - thrown if business logic code on server throws exception
	 */
	public byte[] sendRequest(byte[] msg) throws IOException, ClassNotFoundException, RemoteExecutionException{
		channel.write(protocol.encodeMessage(msg));
		
		
		do {
			input.clear();
			selector.select();
			if (channel.read(input) == -1){
		      channel.close();
		      throw new IOException("peer closed connection");
	  	  }
	   	  input.flip();
	   	  protocol.addPart(input);
		}
		while(!protocol.hasCompleteMessage());
		byte[] retVal = protocol.getReply();
		protocol.inboundMessageHasBeenConsumed();
		return retVal;
		
	}
}
