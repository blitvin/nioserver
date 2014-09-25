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

public interface ClientServerProtocol {
	public static final int CLOSE_NOTIFICATION = -1;
	public static final int KEEPALIVE_NOTIFICATION = -2;
	
	boolean addPart(ByteBuffer input);
	boolean hasCompleteMessage();
	
	void inboundMessageHasBeenConsumed();
	ByteBuffer encodeMessage(byte[] msg);
	//ByteBuffer encodeObject(Object msg);
	ByteBuffer encodeEmptyMessage();
}
