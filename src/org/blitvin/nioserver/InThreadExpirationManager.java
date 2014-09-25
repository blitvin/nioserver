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
import java.util.HashSet;

/* obsoleted class - NioServer uses SimpleExpirationManager */
public class InThreadExpirationManager<V> implements ExpirationManager<V> {

	
	private int generation = 0;
	HashSet<V> currentGen;
	HashSet<V> prevGen;
	HashSet<V> obsoleted;
	private Ticker ticker;
	private void updateGeneration() {
		int tickergen = ticker.generation;
		if (generation == tickergen)
			return;
		
		if (obsoleted.isEmpty()) //getExpired called during tick
			obsoleted = prevGen;
		else
			obsoleted.addAll(prevGen);
		
		if (generation == tickergen - 1) {
			prevGen = currentGen;
		} else { // no updates for more than generation
			prevGen  = new HashSet<V>(); // can't do prevGen.clear because possible alias to obsoleted
			obsoleted.addAll(currentGen);
		}
		currentGen = new HashSet<V>();
		generation = tickergen;
	}
	private static class Ticker extends Thread {
		private final int tick;
		volatile int generation;
		Ticker(int tick) {
			this.tick = tick;
			generation = 0;
		}
		@Override
		public void run(){
			while(true){
				try {
					Thread.sleep(tick);
					generation++;
				
				} catch (InterruptedException e) { 
				}
			}
		}
	}	
	@Override
	public int add(V v) {
		updateGeneration();
		currentGen.add(v);
		return generation;
	}

	@Override
	public int touch(V v, int hint) {
		updateGeneration();
		if (hint == generation)
			return hint;
		if (hint == generation -1) {
			prevGen.remove(v);
		} else {
			obsoleted.remove(v);
		}
		currentGen.add(v);
		return generation;
	}

	@Override
	public boolean remove(V v, int hint) {
		updateGeneration();
		if (hint == generation)
			return  currentGen.remove(v);
		if (hint == generation - 1)
			return prevGen.remove(v);
		else
			return obsoleted.remove(v);
	}

	@Override
	public void start() {
		ticker.start();
	}

	@Override
	public Collection<V> getExpired() {
		updateGeneration();
		HashSet<V> s = obsoleted;
		obsoleted = null;
		return s;
	}
	public InThreadExpirationManager(int timeout){
		currentGen = new HashSet<>();
		prevGen = new HashSet<>();
		obsoleted = new HashSet<>();
		ticker = new Ticker(timeout);
	}

	@Override
	public void stop() {
		
	}
}
