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

/**
 * Server context contains all state of the connection, both business logic related i.e.
 * ClientContext sublcass and internal per channel state e.g. protocol state
 * @author blitvin
 *
 * @param <CC> client context implementation class
 */
public class ServerContext<CC extends ClientContext> {
	int expirationManagerHint = 0;
	final ServerSideProtocol protocol;
	final SocketChannel channel;
	final CC clientContext;
	
	
	public ServerContext(ServerSideProtocol protocol, SocketChannel channel, CC context){
		this.protocol = protocol;
		this.channel = channel;
		this.clientContext = context;
	}
	
	public ServerSideProtocol getProtocol(){
		return protocol;
	}
	
	SocketChannel getChannel(){
		return channel;
	}
	
	public CC getClientContext(){
		return clientContext;
	}
	
	public void updateExpirationHint(int newHint){
		expirationManagerHint = newHint;
	}
	
	public int getExpirationManagerHint(){
		return expirationManagerHint;
	}
}
