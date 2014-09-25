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

/**
 * message processor interface defines API for business logic using nioserver.
 * Once full message is received and stored in client context, processData is called with
 * the context object. Business logic should retrieve message using getRequest. After processing
 * has been completed business logic code should send response by invoking setReply  
 * @author blitvin
 *
 * @param <CC> client context class used by message processor
 */
public interface MessageProcessor<CC extends ClientContext> {
	void processData(CC clientContext) throws Exception;
}
