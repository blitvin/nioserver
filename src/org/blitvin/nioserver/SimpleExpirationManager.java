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
 * SimpleExpiration manager is implementation of Expiration manager that aims to minimize
 * overhead, in order to achieve this it assumes several things: it is ok not to evict an 
 * object immediately after timeout elapsed, and there is no congestion on touch, add methods
 * so synchronization on intrinsic lock is good enough. For NioServer both requirements are 
 * OK. It can take as much as 2 ticks (timeout intervals) before connection marked for eviction
 * but probably there is no big deal. Expiration manager is used exclusively by NioServer
 * communication thread ( one with select and read/accept/write ) so synchronized method can
 * block only when ticker thread updates generation (see below), which is rare event.
 * On the other hand, the manager doesn't need to constantly check for current time against
 * list of expiration times of active connections.
 * Expiration manager maintains generations which are just int returned by add and touch.
 * 3 buckets are maintained - one for all objects that are seen during current generation,
 * second one is for previous generation and third one is for expired elements. Hint passed by
 * touch is just generation of the object , so for object that already seen during current tick
 * no work done at all, and for previous ones it is easy to see which bucket to look the object for 
 * Maintaining 3 buckets ensures that expired objects are untouched for at least one tick  -
 * they stay for tick in previous generation bucket. And no modification of values required
 * per object, so complexity is kind of O(1)...
 * @author blitvin
 *
 * @param <V>
 */
public class SimpleExpirationManager<V>  implements ExpirationManager<V> {
	
	/**
	 * Ticker is thread that advances generation once in tick i.e. timeout period
	 * calling synchronized method ensures no race on add/touch invocation
	 * @author blitvin
	 *
	 * @param <V>
	 */
	private static class Ticker<V> extends Thread {
		private final int tick;
		
		private final SimpleExpirationManager<V> mgr;
		Ticker(int tick,SimpleExpirationManager<V> mgr) {
			this.mgr = mgr;
			this.tick = tick;
		}
		@Override
		public void run(){
			while(true){
				try {
					Thread.sleep(tick);	
					synchronized (mgr) {
						mgr.updateGen();
					}
				} catch (InterruptedException e) { 
				}
				if (mgr.shouldStopTicking)
					return;
				
			}
		}
	}
	
	
	private volatile int generation= 0;
	private volatile HashSet<V> currentGen;
	private volatile HashSet<V> prevGen;
	private volatile HashSet<V> obsoleted;
	Ticker<V> ticker;
	private volatile boolean shouldStopTicking;
	final private Collection<V> empty;
	private void updateGen(){
		if (obsoleted == null || obsoleted.isEmpty()) //getExpired called during tick
			obsoleted = prevGen;
		else
			obsoleted.addAll(prevGen);
		prevGen = currentGen;
		
		currentGen = new HashSet<V>();
		generation++;
	}
	
	@Override
	public synchronized int add(V v) {
		currentGen.add(v);
		return generation;
	}
	@Override
	public synchronized int touch(V v, int hint) {
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
	public synchronized boolean remove(V v, int hint) {
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
	public synchronized Collection<V> getExpired() {
		HashSet<V> s = obsoleted;
		obsoleted = null;
		return s == null? empty: s; // don't return null, so client doesn't need to check for null...
	}
	public SimpleExpirationManager(int timeout){
		empty = Collections.unmodifiableCollection(new HashSet<V>());
		currentGen = new HashSet<>();
		prevGen = new HashSet<>();
		obsoleted = new HashSet<>();
		shouldStopTicking = false;
		ticker = new Ticker<V>(timeout,this);
		ticker.setDaemon(true);
	}

	@Override
	public void stop() {
		shouldStopTicking = true;
		ticker.interrupt();
		
	}
}
