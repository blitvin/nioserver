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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
/**
 * This is a utility class simplifying writing clients for communication
 * with NioServer. It assumes default LV( length value) protocol used by the server 
 * @author blitvin
 *
 */
public class LVClientHelper {
	
	private final InputStream is;
	private final OutputStream os;
	private final byte[] headerBuf;
	private final ByteBuffer len; 
	private Socket socket;
	
	/**
	 * Constructor using socket
	 * @param socket -open socket to NioServer
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public LVClientHelper(Socket socket) throws UnknownHostException, IOException{
		this(socket.getInputStream(),socket.getOutputStream());
		this.socket = socket;
	}
	
	/**
	 * Constructor using input and output stream. This constructor's primary use
	 * is unitesting
	 * @param is
	 * @param os
	 */
	LVClientHelper(InputStream is , OutputStream os) {
		this.is = is;
		this.os = os;
		headerBuf =  new byte[LVProtocol.HEADER_LENGTH];
		len = ByteBuffer.allocate(LVProtocol.HEADER_LENGTH);
		socket = null;
	}
	
	/**
	 * Constructor of the helper
	 * @param hostname - hostname NioServer runs on
	 * @param port - port NioServer listens to
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public LVClientHelper(String hostname, int port) throws UnknownHostException, IOException{
		this(new Socket(hostname,port));
	}
	
	/**
	 * the method notifies server that connection is no more needed and closes underlying socket
	 * @throws IOException
	 */
	public void closeConnection() throws IOException{
		if (socket != null) {
			os.write(encodeServiceMessage(ClientServerProtocol.CLOSE_NOTIFICATION));
			os.flush();
			socket.close();
			socket = null;
		}
	}
	
	@Override
	public void finalize(){
		try {
			closeConnection();
		}
		catch(IOException e) {
			
		}
	}
	/**
	 * the method sends keep alive ping to server notifying it connection is active
	 * see also ExpirationManager. NioServer have expiration connection feature, so that 
	 * dangling connections (those that are not properly closed) don't consume server side resources
	 * There is a configurable timeout, connection without traffic for longer than the timeout
	 * considered inactive and closed ( and corresponding resources are released). To prevent this
	 * client should periodically send this notification , if no requests are sent .
	 *   
	 * @throws IOException
	 */
	public void keepAliveConnection() throws IOException {
		if (socket != null)
			os.write(encodeServiceMessage(ClientServerProtocol.KEEPALIVE_NOTIFICATION));
	}
	
	/**
	 * This method for sending request with byte array. The reply is also byte array. Raw data is
	 * passed both ways. Business logic and client are responsible for interpretation of data. 
	 * @param message - raw data byte array to send to the server
	 * @return - data returned by array
	 * @throws RemoteExecutionException - in case of exception happen on server side , it is wrapped in 
	 * RemoteException serialized and transmitted back to client. the method detects the condition
	 * deserializes the RemoteException and throws it. 
	 * @throws IOException - this exception is thrown in case of communication problems
	 * @throws ClassNotFoundException - thrown if exception sent from server is not known in client JVM
	 */
	public byte[] sendRequest(byte[] message) throws RemoteExecutionException, IOException, ClassNotFoundException {
		os.write(encodeBytes(message));
		int len = readLen();
		if (len == -1) // peer closed connection
			return null;
		
		if (len < 0)
			decodeObject(message, len); // throws exception
		
		byte[] retVal = new byte[len];
		readBytes(retVal,len);
		return retVal;
	}
	
	void readBytes(byte[] buf,int len) throws IOException {
		int chunk=0, off = 0;
		
		do {
			chunk = is.read(buf, off, len - off);
			if (chunk == -1)
				throw new IOException("peer closed connection");
			off += chunk;
		} while (off < len);
	}
	
	/**
	 * Method for sending request to the server with Serializable object as parameter 
	 * @param message - java object representing request argument 
	 * @return - return object sent back to client by server
	 * @throws RemoteExecutionException - in case of exception happen on server side , it is wrapped in 
	 * RemoteException serialized and transmitted back to client. the method detects the condition
	 * deserializes the RemoteException and throws it.
	 * @throws IOException - underlying communication error 
	 * @throws ClassNotFoundException - this exception is thrown if object sent back can be deserialized
	 * because corresponding class can't be found
	 */
	public Object sendObjectRequest(Object message) throws RemoteExecutionException, IOException, ClassNotFoundException {
		os.write(encodeObject(message));
		int len = readLen();
		byte[] msg= new byte[len>0?len:-len];
		readBytes(msg, msg.length);
		return decodeObject(msg, len);
	}
	
	 int readLen() throws IOException {
		readBytes(headerBuf,LVProtocol.HEADER_LENGTH);
		len.clear();
		len.put(headerBuf);
		len.flip();
		return len.getInt();
	}
	 
	private byte[] encodeServiceMessage(int code){
		byte[] retVal = new  byte[LVProtocol.HEADER_LENGTH];
		len.clear();
		len.putInt(code);
		len.flip();
		len.get(retVal, 0, LVProtocol.HEADER_LENGTH);
		return retVal;
	}
	/**
	 * this method wraps message according to NioServer communication protocol
	 * @param message - byte array to prepare for sending to NioServer
	 * @return wrapped message
	 */
	public byte[] encodeBytes(byte[] message){
		byte[] retVal = new  byte[message.length+ LVProtocol.HEADER_LENGTH];
		len.clear();
		len.putInt(message.length);
		len.flip();
		len.get(retVal, 0, LVProtocol.HEADER_LENGTH);
		System.arraycopy(message, 0, retVal, LVProtocol.HEADER_LENGTH, message.length);
		return retVal;
	}
	/**
	 * This is a convenience method for data manipulation. This method can be used for
	 * manipulations on data representation to be sent. E.g. if one wishes to encrypt java object 
	 * parameter for request one can use this method and then  encrypt resulting byte array and use
	 * sendByteMessage for actual sending  
	 * @param obj - object to convert to byte array
	 * @return byte array representing the object
	 * @throws IOException
	 */
	public byte[] encodeObject(Object obj) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(obj);
			return encodeBytes(bos.toByteArray());
		}
		finally {
			  try {
			    if (out != null) {
			    	out.close();
			    }
			  } catch (IOException ex) {
			    // ignore close exception
			  }
			  try {
			    bos.close();
			  } catch (IOException ex) {
			    // ignore close exception
			  }
		}
	}
	
	/**
	 * Convenience object performing decoding object from byte array
	 * @param message
	 * @param dataLen
	 * @return
	 * @throws RemoteExecutionException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public  Object decodeObject(byte[] message,int dataLen) throws RemoteExecutionException, IOException, ClassNotFoundException {
		if (dataLen == 0)
			return null;
		
		Object retVal = null;
		
		ByteArrayInputStream bis = new ByteArrayInputStream(message);
		ObjectInput in = null;
		try {
		  in = new ObjectInputStream(bis);
		  retVal = in.readObject();
		  if (dataLen <0) // exception
			  throw new RemoteExecutionException((Exception)retVal);
		  return retVal;
		} finally {
		  try {
		    bis.close();
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		  try {
		    if (in != null) {
		      in.close();
		    }
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		}
	}
}
