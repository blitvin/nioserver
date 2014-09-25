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
 * Base class for both client and server side of default communication protocol used
 * by nioserver
 * @author blitvin
 *
 */
public class LVProtocol implements ClientServerProtocol {

	private int totalLen = -1;
	private int readsofar = 0;
	protected byte[] msgData = null;
	protected final static int HEADER_LENGTH = Integer.SIZE/Byte.SIZE;
	private ByteBuffer prereadData = null;
	private ByteBuffer lenData = ByteBuffer.allocate(HEADER_LENGTH);
	
	@Override
	public boolean addPart(ByteBuffer input) {
		if (totalLen < 0) { // -1 actually
			if (prereadData != null && prereadData.remaining() > 0) {
				while(lenData.remaining() >0 && prereadData.remaining() >0)
					lenData.put(prereadData.get());
			}
			if (lenData.remaining() > 0 )
				while(lenData.remaining() > 0 && input.remaining() > 0)
					lenData.put(input.get());
			if (lenData.remaining() >0)
				return false; // didn't get all the bytes of length
			lenData.flip();
			totalLen = lenData.getInt();
			lenData.clear();
			totalLen = handleNegativeLen(totalLen);
			//if (totalLen < KEEPALIVE_NOTIFICATION)
			//	throw new IllegalArgumentException("got negative length of message");
			if (totalLen >=0) {
				msgData = new byte[totalLen];
				readsofar  = 0;
			}
			else {
				if (input.hasRemaining()) {
					prereadData = ByteBuffer.allocate(input.remaining());
					prereadData.put(input);
					prereadData.flip();
					return true; // special notifications
				}
			}
		}
		
		if (prereadData != null && prereadData.remaining() >0) {
			readsofar = prereadData.remaining() > totalLen ? totalLen:prereadData.remaining();
			prereadData.get(msgData, 0, readsofar);
		}
		if (readsofar < totalLen && input.hasRemaining()) {
			int addedFromBuf = input.remaining() > totalLen - readsofar ? totalLen - readsofar: input.remaining();
			input.get(msgData, readsofar, addedFromBuf);
			readsofar += addedFromBuf;
			if (input.hasRemaining()) {
				prereadData = ByteBuffer.allocate(input.remaining());
				prereadData.put(input);
				prereadData.flip();
			}
		}
		return readsofar == totalLen;
	}
	/**
	 * negative length indicate special object passing, in case of message from server to
	 * client this indicates exception is transmitted
	 * in case of message from client to server negative length means service message e.g. 
	 * keepalive ping
	 * @param totalLen
	 * @return
	 */
	protected int handleNegativeLen(int totalLen) {
		return (totalLen < KEEPALIVE_NOTIFICATION) ? -totalLen: totalLen;
	}
	@Override
	public boolean hasCompleteMessage() {
		if (msgData == null && prereadData != null) {
			if (prereadData.remaining() >= HEADER_LENGTH) {
				totalLen = prereadData.getInt();
				totalLen = handleNegativeLen(totalLen);
				//if (totalLen <0)
				//	throw new IllegalArgumentException("message length is negative");
				msgData = new byte[totalLen];
				readsofar = prereadData.remaining() >= totalLen ? totalLen: prereadData.remaining();
				prereadData.get(msgData,0,readsofar);
				if (!prereadData.hasRemaining())
					prereadData = null;
			}
		}
		if (msgData != null)
			return totalLen >= 0 && readsofar == totalLen;
		return false;
	}

	@Override
	public void inboundMessageHasBeenConsumed() {
		totalLen = -1;
		readsofar = 0;
		msgData = null;
	}

	@Override
	public ByteBuffer encodeMessage(byte[] msg) {
		if (msg == null)
			return encodeEmptyMessage();
		ByteBuffer outboundData  = ByteBuffer.allocate(msg.length + HEADER_LENGTH);
		outboundData.putInt(msg.length);
		if (msg.length > 0)
			outboundData.put(msg);
		outboundData.flip();
		return outboundData;
	}



	@Override
	public ByteBuffer encodeEmptyMessage() {
		ByteBuffer outboundData = ByteBuffer.allocate(HEADER_LENGTH);
		outboundData.putInt(0);
		outboundData.flip();
		return outboundData;
	}
}
