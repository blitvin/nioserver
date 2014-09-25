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
 * This interface defines factory for creation message processor instances
 * @author blitvin
 *
 * @param <CC> concrete class of ClientContext
 */
public interface MessageProcessorFactory<CC extends ClientContext>{
	/**
	 * the method for  new instances of MessageProcessor 
	 * @return new instance
	 */
	MessageProcessor<CC> newInstance();
	/**
	 * notifies framework whether the factory caches (pools) message processor instances. In this
	 * case reclaimUnused is invoked with instance of message processor, that finished request 
	 * processing and not indicated by business logic to be retained with particular CC instance 
	 * @return true if the factory maintains pool of cached instances of message processors.
	 */
	boolean cacheInstances();
	/**
	 * callback method for returning instance of message processor to pool retained by the factory
	 * @param p - object of message processor unlinked from client context
	 */
	void reclaimUnused(MessageProcessor<CC> p);
}
