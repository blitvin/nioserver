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

import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * NioServerInitializer is builder object for NioServer. Parameters controlling various
 * aspects of NioServer are set in an object of this class, and then the object is used
 * as argument of NioServer constructor
 * Parameters are set by setter, or retrieved from properties, if neither provided default
 * values are used
 * @author blitvin
 *
 * @param <CC> client context class
 * @param <MP> message processor class
 */
public class NioServerInitializer<CC extends ClientContext, MP extends MessageProcessor<CC>> {
	
	/**
	 * Property name for setting buffer size
	 */
	public static final String BUFFER_SIZE_STRING = "org.blitvin.nioserver.bufferSize";
	/**
	 * property name for flag disabling timeout mechanism
	 */
	public static final String TIMEOUT_DISABLED_VAR_STRING = "org.blitvin.nioserver.timeoutDisabled";
	/**
	 * property name for client connection timeout. After inactivity of at least timeout msecs the
	 * server can close connection
	 */
	public static final String CLIENT_TIMEOUT_STRING = "org.blitvin.nioserver.timeout";
	/**
	 * name of property containing class name of context class implementation
	 */
	public static final String CLIENT_CONTEXT_CLASS_STRING = "org.blitvin.nioserver.clientContextClass";
	/**
	 * name of property containing class name of message processor implementation
	 */
	public static final String MESSAGE_PROCESSOR_CLASS_STRING = "org.blitvin.nioserver.messageProcessorClass";
	/**
	 * name of property defining class implementing ServerSideProtocol
	 */
	public static final String PROTOCOL_CLASS_STRING="org.blitvin.nioserver.protocolClass";
	
	/**
	 * default protocol class name.
	 */
	public static final String DEFAULT_PROTOCOL_CLASS="org.blitvin.nioserver.LVServerProtocol";
	/**
	 * default size of buffer used by NioServer. 
	 */
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	/**
	 * default client timeout 
	 */
	public static final int DEFAULT_CLIENT_TIMEOUT = 120000; // 2 minutes
	/**
	 * This is minimal client timeout, timeout set for less than this value interpreted as no timeout
	 */
	public static final int MIN_CLIENT_TIMEOUT = 40;
	private ClientContextFactory<CC> contextFactory;
	private ServerSideProtocolFactory protocolFactory;
	private MessageProcessorFactory<CC> processorFactory;
	private Class<CC> clientContextClass;
	private Class<ServerSideProtocol> protocolClass;
	private Class<MP> messageProcessorClass;
	private int port;
	private int bufferSize;
	private ExecutorService pool;
	private int clientTimeout;
	private ErrorListener listener;
	private ExpirationManager<SocketChannel> expirationMgr;
	/**
	 * returns threadpool that is used for invoking business logic methods
	 * if none specified, Executors.newCachedThreadPool() used to create return value
	 * @return threadpool to use by NioServer
	 */
	public ExecutorService getThreadPool() {
		if (pool != null)
			return pool;
		else 
			return Executors.newCachedThreadPool();
	}
	
	/**
	 * thread pool setter. NioServer uses this threadpool for business logic code dispatch
	 * @param pool 
	 */
	public void setPool(ExecutorService pool) {
		this.pool = pool;
	}
	/**
	 * NioServer requires several factories to be supplied. One of those is the factory
	 * producing ClientContext concrete objects
	 * @return factory for building ClientContexts
	 */
	public ClientContextFactory<CC> getContextFactory() {
		if (contextFactory == null)
		{
			contextFactory = new ClientContextFactory<CC>() {

				@Override
				public CC newInstance()  {
					try {
						return clientContextClass.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						// TBD here actual handling should be placed - or add throws to super..decide later
						//e.printStackTrace();
					}
					return null;
				}
				
			};
		}
		return contextFactory;
	}
	/**
	 *  NioServer requires several factories to be supplied. One of those is the factory
	 *  producing objects of server side protocol handler
	 * @return protocol creation factory
	 */
	public ServerSideProtocolFactory getProtocolFactory(){
		if (protocolFactory == null) {
			protocolFactory = new ServerSideProtocolFactory() {
				
				@Override
				public ServerSideProtocol newInstance() {
					try {
						return protocolClass.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						return null;
					}
				}
			};
		}
		return protocolFactory;
	}
	
	/**
	 * NioServer requires several factories to be supplied. One of those is the factory
	 * building message processors i.e. component incorporating business logic applied on
	 * messages sent from client
	 * @return message processor factory
	 */
	public MessageProcessorFactory<CC> getProcessorFactory() {
		if (processorFactory == null) {
			processorFactory = new MessageProcessorFactory<CC>() {

				@Override
				public MessageProcessor<CC> newInstance() {
					try {
						return messageProcessorClass.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						// The same as ClientContextFactory - add implementation or add throws to superclass
					}
					return null;
				}

				@Override
				public boolean cacheInstances() {
					return false;
				}

				@Override
				public void reclaimUnused(MessageProcessor<CC> p) {					
				}
			};
		}
		return processorFactory;
	}
	
	/**
	 * Message processor factory setter
	 * @param processorFactory factory for producing MessageProcessor
	 */
	public void setProcessorFactory(MessageProcessorFactory<CC> processorFactory) {
		this.processorFactory = processorFactory;
	}
	
	/**
	 * NioServerInitializer can create client context factory from class of ClientContext 
	 * subclass if one has default constructor. If this class supplied and setClientContextFactory
	 * didn't supply the factory object , one automatically constructed
	 * @param clientContextClass class object of implementation of client context
	 */
	public void setClientContextClass(Class<CC> clientContextClass) {
		this.clientContextClass = clientContextClass;
	}
	/**
	 * NioServerInitializer can create client context factory from class of ServerSideProtocol 
	 * subclass if one has default constructor. If this class supplied and setProtocolFactory
	 * didn't supply the factory object , one automatically constructed
	 * @param protocolClass class object of implementation of the protocol
	 */
	public void setProtocolClass(Class<ServerSideProtocol> protocolClass) {
		this.protocolClass = protocolClass;
	}
	
	/**
	 * NioServerInitializer can create client context factory from class of MessageProcessor 
	 * subclass if one has default constructor. If this class supplied and setProcessorFactory
	 * didn't supply the factory object , one automatically const.google.com/search?q=%40see+tag+javadoc+example&ie=UTF-8&oe=UTF-8ructed
	 * @param protocolClass class object of implementation of the protocol
	 */
	public void setMessageProcessorClass(Class<MP> messageProcessorClass) {
		this.messageProcessorClass = messageProcessorClass;
	}
	
	/**
	 * setter of client context factory
	 * @param contextFactory
	 */
	public void setContextFactory(ClientContextFactory<CC> contextFactory) {
		this.contextFactory = contextFactory;
	}
	
	/**
	 * returns port NioServer object should listen on
	 * @return port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * port the NioServer object to listen to  setter
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * returns input buffer size
	 * @return input buffer size used by Channel read
	 */
	public int getBufferSize() {
		return bufferSize;
	}
	/**
	 * input buffer size setter
	 * @param bufferSize
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	/**
	 * the method indicates that timeout mechanism should be disabled in constructed NioServer
	 */
	public void disableClientTimeout(){
		this.clientTimeout = -1;
		this.expirationMgr = null;
	}
	/**
	 * sets timeot after which client connection can be closed.
	 * Note that this method should be cancels expirationMgr set by setExpirationManager,
	 * that is default classes are instantiated
	 * @param timeout
	 */
	public void setClientTimeout(int timeout){
		this.clientTimeout = timeout;	
		this.expirationMgr = null;
	}
	
	/**
	 * 
	 * @return true if timeout mechanism enabled
	 */
	public boolean isClientTimeoutEnabled(){
		return clientTimeout >= MIN_CLIENT_TIMEOUT;
	}
	
	/**
	 * constructor requiring only port, rest of parameters are taken from defaults or changed
	 * by subsequent calls of setters
	 * @param port
	 */
	public NioServerInitializer(int port){
		this(port,null,null,null);
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * This constructor is for use with initializer-less constructor of NioServer
	 * @param port port to listen
	 * @param clientContextClass class of client context implementation
	 * @param messageProcessorClass class of message processor (i.e. business logic interface) 
	 * implementation
	 * @param protocolClass class of protocol handler. If left blank LV ( length value) protocol 
	 * assumed
	 */
	public NioServerInitializer(int port, Class<CC> clientContextClass, 
			Class<MP> messageProcessorClass,Class<ServerSideProtocol> protocolClass){
		this.port = port;
		Integer bufs = Integer.getInteger(System.getProperty(BUFFER_SIZE_STRING));
		this.bufferSize = (bufs != null)?bufs.intValue():DEFAULT_BUFFER_SIZE;
		if (Boolean.getBoolean(TIMEOUT_DISABLED_VAR_STRING))
			clientTimeout = -1;
		else {
			Integer clientTimeoutObj = Integer.getInteger(System.getProperty(CLIENT_TIMEOUT_STRING));
			clientTimeout =  (clientTimeoutObj != null)? clientTimeoutObj.intValue():DEFAULT_CLIENT_TIMEOUT;
		}
		this.clientContextClass = clientContextClass;
		this.messageProcessorClass = messageProcessorClass;
		
		if (clientContextClass == null) {
			try {
				this.clientContextClass = (Class<CC>) Class.forName(System.getProperty(CLIENT_CONTEXT_CLASS_STRING));
			} catch (ClassNotFoundException e) {
			}
		}
		if (messageProcessorClass == null) {
			try {
				this.messageProcessorClass = (Class<MP>) Class.forName(System.getProperty(MESSAGE_PROCESSOR_CLASS_STRING));
			}catch (ClassNotFoundException e) {
			}
			
		}
		if (protocolClass == null) {
			try {
				this.protocolClass = (Class<ServerSideProtocol>) Class.forName(System.getProperty(PROTOCOL_CLASS_STRING,DEFAULT_PROTOCOL_CLASS));
			}catch (ClassNotFoundException e) {
			}
			
		}
	}

	/**
	 * 
	 * @return client connection timeout
	 */
	public int getTimeout() {
		return clientTimeout;
	}

	/**
	 * Error listener is invoked if exception thrown and not handled in NioServer thread
	 * @return current error listener
	 */
	public ErrorListener getErrorListener() {
		return listener;
	}

	/**
	 * Error listener is invoked if exception thrown and not handled in NioServer thread.
	 * This method allows register such a listener
	 * @param listener errorlistener
	 */
	public void setErrorListener(ErrorListener listener) {
		this.listener = listener;
	}
	public void setExpirationManager(ExpirationManager<SocketChannel> expirationManager){
		this.expirationMgr = expirationManager;
	}
	
	public ExpirationManager<SocketChannel> getExpirationManager(){
		if (expirationMgr == null) {
			if (clientTimeout <  MIN_CLIENT_TIMEOUT)
				expirationMgr = new NoExpirationManager<>();
			else
				expirationMgr = new SimpleExpirationManager<>(clientTimeout);
		}
		return expirationMgr;
	}
}
