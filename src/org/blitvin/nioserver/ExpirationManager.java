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

import java.util.Collection;
/**
 * interface of expiration manager i.e. component determining what connection is timed out
 * @author blitvin
 *
 * @param <V>
 */
public interface ExpirationManager<V> {
	/**
	 * notifies the manager of new element 
	 * @param v new item to manage
	 * @return hint that allows implementation of ExpirationManager finding of v faster
	 */
	int add(V v);
	/**
	 * notifies manager that v is in use 
	 * @param v item that is in use
	 * @param hint data supplied by add or previous touch
	 * @return new hint
	 */
	int touch(V v, int hint);
	/**
	 * notification of the manager that v is no longer valid
	 * @param v
	 * @param hint
	 * @return
	 */
	boolean remove(V v, int hint);
	/**
	 * lifecycle method , start of usage notification
	 */
	void start();
	/**
	 * lifecycle method, shutdown of the manager
	 */
	void stop();
	/**
	 * method returning collection of outdated items. After expired items returned with this mehtod,
	 *  monitoring of those is cancelled
	 * @return list of outdated items
	 */
	Collection<V> getExpired();
}
