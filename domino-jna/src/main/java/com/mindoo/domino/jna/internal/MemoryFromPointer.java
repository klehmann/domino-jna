package com.mindoo.domino.jna.internal;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Special {@link Memory} subclass that has a constructor to set
 * a memory pointer and the size.
 * 
 * @author Karsten Lehmann
 */
public class MemoryFromPointer extends Memory {

	public MemoryFromPointer(Pointer ptr, long size) {
		super();
		this.peer = Pointer.nativeValue(ptr);
		this.size = size;
	}

	protected MemoryFromPointer() {
		super();
	}

	@Override
	protected synchronized void dispose() {
		//
	}

	@Override
	public Pointer share(long offset) {
		if (offset > size()) {
            throw new IndexOutOfBoundsException("Invalid offset: " + offset);
		}
        return share(offset, size() - offset);
	}
	
	@Override
	public Pointer share(long offset, long sz) {
		boundsCheck(offset, sz);
		return new SharedBoundedPointer(offset, sz);
	}

	/** Provide a view into the original memory.  Keeps an implicit reference
	 * to the original to prevent GC.
	 */
	private class SharedBoundedPointer extends MemoryFromPointer {
		public SharedBoundedPointer(long offset, long size) {
			this.size = size;
			this.peer = MemoryFromPointer.this.peer + offset;
		}
		/** No need to free memory. */
		@Override
		protected void dispose() {
			this.peer = 0;
		}
		/** Pass bounds check to parent. */
		@Override
		protected void boundsCheck(long off, long sz) {
			MemoryFromPointer.this.boundsCheck(this.peer - MemoryFromPointer.this.peer + off, sz);
		}
		@Override
		public String toString() {
			return super.toString() + " (shared from " + MemoryFromPointer.this.toString() + ")";
		}
	}

}
