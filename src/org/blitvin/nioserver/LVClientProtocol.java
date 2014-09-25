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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
/**
 * Default implementation of client protocol
 * @author blitvin
 *
 */
public class LVClientProtocol extends LVProtocol implements ClientSideProtocol {
	private boolean exceptionThrown = false;
	@Override
	public ByteBuffer sendServiceMessage(int messageCode) {
		if (messageCode >=0 || messageCode < ClientServerProtocol.KEEPALIVE_NOTIFICATION)
			throw new IllegalArgumentException("message code expected to be one of defined in ClientServerProtocol, got code="+messageCode);
		ByteBuffer outboundData = ByteBuffer.allocate(HEADER_LENGTH);
		outboundData.putInt(messageCode);
		outboundData.flip();
		return outboundData;
	}

	@Override
	protected int handleNegativeLen(int totalLen) {
		if (totalLen < 0 ) {
			exceptionThrown = true;
			return -totalLen;
		}
		return totalLen;
	}
	@Override
	public byte[] getReply() throws RemoteExecutionException, IOException, ClassNotFoundException {
		if (exceptionThrown) {
			Exception remote;
			ByteArrayInputStream bis = new ByteArrayInputStream(msgData);
			ObjectInput in = null;
			try {
			  in = new ObjectInputStream(bis);
			  remote = (Exception) in.readObject();
			  throw new RemoteExecutionException(remote);
			}finally {
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
		return msgData;
	}

	@Override
	public 
	void inboundMessageHasBeenConsumed(){
		super.inboundMessageHasBeenConsumed();
		exceptionThrown = false;
	}
}
