package com.mindoo.domino.jna.internal;

import com.sun.jna.Memory;

/**
 * Subclass of {@link Memory} that can explicitely be disposed to reduce memory usage.
 * 
 * @author Karsten Lehmann
 */
public class DisposableMemory extends Memory {

	/**
	 * Allocate space in the native heap via a call to C's <code>malloc</code>.
	 *
	 * @param size number of <em>bytes</em> of space to allocate
	 */
	public DisposableMemory(long size) {
		super(size);
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
	}
	
	/**
	 * Checks if this memory is already disposed
	 * 
	 * @return true if disposed
	 */
	public boolean isDisposed() {
		return peer == 0;
	}
}
