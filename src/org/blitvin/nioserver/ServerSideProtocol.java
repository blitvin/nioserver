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
 * API of server side of communication protocol
 * @see ClientServerProtocol
 * @author blitvin
 *
 */
public interface ServerSideProtocol extends ClientServerProtocol{
	/**
	 * returns non-zero code of service message e.g. close connection notification.
	 * The code defined in ClientServerProtocol
	 * @return service code
	 */
	int getServiceMessage(); // 0 if message is not service message
	/**
	 * serializes exception happened during business logic execution for sending to
	 * client
	 * @param e exception as caught by business logic code wrapper
	 * @return serialized exception
	 */
	ByteBuffer putExceptionReply(Exception e);
	/**
	 * method for extraction of request information i.e. non-service message sent by client 
	 * @return
	 */
	byte[] getRequest();
	
}
