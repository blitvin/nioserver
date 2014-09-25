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
import java.util.Collections;
import java.util.HashSet;
/**
 * This class is "null" expiration manager i.e. it never reports anything as
 * expired
 * @author blitvin
 *
 * @param <V>
 */
public class NoExpirationManager<V> implements ExpirationManager<V> {

	final private Collection<V> empty = Collections.unmodifiableCollection(new HashSet<V>());
	
	@Override
	public int add(V v) {
		return 0;
	}

	@Override
	public int touch(V v, int hint) {
		return 0;
	}

	@Override
	public boolean remove(V v, int hint) {
		return false;
	}

	@Override
	public void start() {	
	}

	@Override
	public void stop() {
	}

	@Override
	public Collection<V> getExpired() {
		return empty;
	}

}
