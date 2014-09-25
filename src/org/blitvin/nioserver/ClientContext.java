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

import java.nio.ByteBuffer;


/**
 * ClientContext is a base class for extension of business logic related state of particular
 * connection. It persists state between remote calls and passed to business logic implementation
 * at each request.
 * ClientContext also allows business logic code to communicate to the framework, e.g. setting reply,
 * retrieving request parameter, notification of closing the connection by server side etc.
 *  
 * @author blitvin
 *
 */
public class ClientContext {
	@SuppressWarnings("rawtypes")
	MessageProcessor mp;
	private byte[] request;
	private byte[] reply;
	protected ByteBuffer outboundData = null; // this one should be outside the class,
											  // but client context is passed by replyQueue
											  // so in order for worker thread to encode
											  // reply need storage for encoded one...
	
	
	private boolean retainMP;
	private boolean sessionEnd;
	
	
	/**
	 * This is a constructor with given message processor
	 * @param mp
	 */
	@SuppressWarnings("rawtypes")
	public ClientContext(MessageProcessor mp) {
		this.mp = mp;
		this.retainMP = false;
		this.sessionEnd = false;
	}
	/**
	 * default constructor
	 */
	public ClientContext(){
		retainMP = false;
		sessionEnd = false;
		mp = null;
	}
	
	void setRequest(byte[] request){
		this.request = request;
	}
	
	byte[] getReply(){
		return reply;
	}
	
	/**
	 * Business logic should use this method to retrieve the message sent by client.
	 * The framework methods provide convenience class {@link ObjectEncoderDecoder} for 
	 * converting byte arrays to objects, if user needs to work with objects instead of 
	 * byte arrays
	 * @return byte array sent by client in the request
	 */
	public byte[] getRequest(){
		return request;
	}
	
	/**
	 * Business logic should use this method to specify response to the client. If user
	 * wishes to work with objects and not with byte arrays, he can use ObjectEncoderDecoder
	 * class for conversion
	 * @param replyBytes
	 * @see ObjectEncoderDecoder
	 */
	public void setReply(byte[] replyBytes){
		reply = replyBytes;
	}
	
	void setOutboundData(ByteBuffer outboundData){
		this.outboundData = outboundData;
	}
	/**
	 * method that ensures reference to message processor is not retained by the context
	 * @see MessageProcessorFactory
	 * @return reference of unlinked message processor, for framework to pass to 
	 * MessageProcessorFactory, if necessary
	 */
	@SuppressWarnings("rawtypes")
	MessageProcessor unlinkMessageProcessor(){
		MessageProcessor retVal = mp;
		mp = null;
		return retVal;
	}
	
	/**
	 * links particular message processor instance with the client context
	 * @param mp - message processor object to work with client context object
	 */
	@SuppressWarnings("rawtypes")
	void setMessageProcessor(MessageProcessor mp) {
		this.mp = mp;
	}
	
	
	ByteBuffer getOutboundData(){
		return outboundData;
	}
	
	/**
	 * notifies the framework whether message processor should not be recycled.
	 * If called with true, the same object of message processor that processes current request
	 * is going to get next request for the client represented by the ClientContext object
	 * Normally all the state is stored in subclass of ClientContext and message processor class 
	 * should be stateless, but if for some reason 
	 * developer wishes to work with the same message processor for processing requests of a client
	 * this method allows to do this
	 * @param retain if true  message processor should be retained 
	 */
	public void requestMessageProcessorRetention(boolean retain){
		retainMP = retain;
	}
	
	boolean shouldRetainMP(){
		return retainMP;
	}
	/**
	 * notifies the framework that connection to the client is closed by business logic, so underlying 
	 * resources should be released
	 */
	public void notifySessionEnd(){
		sessionEnd = true;
	}
	
	boolean isSessionEnded(){
		return sessionEnd;
	}
}
