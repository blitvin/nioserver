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
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
/**
 * This is a helper class allowing conversion of objects to and from byte array
 * Such array can then be passed to the framework. Also intermediate operations e.g.
 * encryption can be applied before "raw" bytes are passed for transmission
 * @author blitvin
 *
 */
public class ObjectEncoderDecoder {
	/**
	 * convert object to byte array
	 * @param obj object to convert
	 * @return byte array serialized form of the object
	 * @throws IOException
	 */
	public static byte[] encode(Object obj) throws IOException{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(obj);
				return  bos.toByteArray();
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
	 * the method decodes object from byte array 
	 * @param bytes raw bytes
	 * @return reconstructed array
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object decode(byte[] bytes) throws IOException, ClassNotFoundException{
		

		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		try {
		  in = new ObjectInputStream(bis);
		  return in.readObject(); 
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
