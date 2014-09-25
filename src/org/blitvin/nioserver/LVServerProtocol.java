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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

/**
 * server side part of LV (default) protocol
 * @author blitvin
 *
 */
public class LVServerProtocol extends LVProtocol implements ServerSideProtocol {

	private int serviceCode = 0;
	
	@Override
	public 
	void inboundMessageHasBeenConsumed(){
		super.inboundMessageHasBeenConsumed();
		serviceCode = 0;
	}
	@Override
	public int getServiceMessage() {
		return serviceCode;
	}
	
	@Override
	protected int handleNegativeLen(int totalLen) {
		switch(totalLen){
		case CLOSE_NOTIFICATION:
		case KEEPALIVE_NOTIFICATION:
			return serviceCode = totalLen;
		default:
			if (totalLen >= 0)
				return totalLen;
			throw new IllegalArgumentException("message length is negative");
		}
	}
	
	@Override
	public ByteBuffer putExceptionReply(Exception exception) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] serialized = null;
		try {
		  out = new ObjectOutputStream(bos);   
		  out.writeObject(exception);
		  serialized = bos.toByteArray();
		} catch (IOException e) {
		} finally {
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
		ByteBuffer outboundData = ByteBuffer.allocate(serialized.length +LVProtocol.HEADER_LENGTH);
			outboundData.putInt(-serialized.length);
		outboundData.put(serialized);
		outboundData.flip();
		return outboundData;
	}

	@Override
	public byte[] getRequest() {
		return msgData;
	}

	@Override
	public boolean hasCompleteMessage(){
		if (serviceCode <0)
			return true;
		return super.hasCompleteMessage();
	}
}
