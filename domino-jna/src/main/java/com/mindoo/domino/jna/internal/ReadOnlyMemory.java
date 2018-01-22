package com.mindoo.domino.jna.internal;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

/**
 * Subclass of {@link Memory} that prevents modifying the memory (as good as we can)
 * 
 * @author Karsten Lehmann
 */
public class ReadOnlyMemory extends Memory {
	private boolean m_sealed;

	/**
	 * Allocate space in the native heap via a call to C's <code>malloc</code>.
	 *
	 * @param size number of <em>bytes</em> of space to allocate
	 */
	public ReadOnlyMemory(long size) {
		super(size);
	}

	/**
	 * After calling this method, writes via the available write methods result
	 * in an {@link UnsupportedOperationException}
	 */
	public void seal() {
		m_sealed = true;
	}

	@Override
	protected void finalize() {
	}
	
	@Override
	public void write(long bOff, byte[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, char[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, double[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, float[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, int[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, long[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, Pointer[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, short[] buf, int index, int length) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.write(bOff, buf, index, length);
	}

	@Override
	public void setByte(long offset, byte value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setByte(offset, value);
	}

	@Override
	public void setChar(long offset, char value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setChar(offset, value);
	}

	@Override
	public void setDouble(long offset, double value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setDouble(offset, value);
	}

	@Override
	public void setFloat(long offset, float value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setFloat(offset, value);
	}

	@Override
	public void setInt(long offset, int value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setInt(offset, value);
	}

	@Override
	public void setLong(long offset, long value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setLong(offset, value);
	}

	@Override
	public void setMemory(long offset, long length, byte value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setMemory(offset, length, value);
	}

	@Override
	public void setNativeLong(long offset, NativeLong value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setNativeLong(offset, value);
	}

	@Override
	public void setPointer(long offset, Pointer value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setPointer(offset, value);
	}

	@Override
	public void setShort(long offset, short value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setShort(offset, value);
	}

	@Override
	public void setString(long offset, String value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setString(offset, value);
	}

	@Override
	public void setString(long offset, String value, boolean wide) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setString(offset, value, wide);
	}

	@Override
	public void setString(long offset, String value, String encoding) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setString(offset, value, encoding);
	}

	@Override
	public void setString(long offset, WString value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setString(offset, value);
	}

	@Override
	public void setWideString(long offset, String value) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.setWideString(offset, value);
	}

	@Override
	public Memory align(int byteBoundary) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		return super.align(byteBoundary);
	}

	@Override
	public void clear() {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.clear();
	}

	@Override
	public void clear(long size) {
		if (m_sealed)
			throw new UnsupportedOperationException();

		super.clear(size);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		if (m_sealed)
			throw new UnsupportedOperationException();

		return super.clone();
	}

	/**
	 * Provide a view of this memory using the given offset as the base address.  The
	 * returned {@link Pointer} will have a size equal to that of the original
	 * minus the offset.
	 * @throws IndexOutOfBoundsException if the requested memory is outside
	 * the allocated bounds.
	 */
	@Override
	public Pointer share(long offset) {
		return share(offset, size() - offset);
	}

	/**
	 * Provide a view of this memory using the given offset as the base
	 * address, bounds-limited with the given size.  Maintains a reference to
	 * the original {@link Memory} object to avoid GC as long as the shared
	 * memory is referenced.
	 * @throws IndexOutOfBoundsException if the requested memory is outside
	 * the allocated bounds.
	 */
	@Override
	public Pointer share(long offset, long sz) {
		boundsCheck(offset, sz);
		return new ReadOnlySharedMemory(offset, sz);
	}

	/** Provide a view into the original memory.  Keeps an implicit reference
	 * to the original to prevent GC.
	 */
	private class ReadOnlySharedMemory extends Memory {
		public ReadOnlySharedMemory(long offset, long size) {
			this.size = size;
			this.peer = ReadOnlyMemory.this.peer + offset;
		}
		
		/** No need to free memory. */
		@Override
		protected void dispose() {
			this.peer = 0;
		}
		
		/** Pass bounds check to parent. */
		@Override
		protected void boundsCheck(long off, long sz) {
			ReadOnlyMemory.this.boundsCheck(this.peer - ReadOnlyMemory.this.peer + off, sz);
		}

		@Override
		public String toString() {
			return super.toString() + " (shared from " + ReadOnlyMemory.this.toString() + ")";
		}

		@Override
		public void write(long bOff, byte[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, char[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, double[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, float[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, int[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, long[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, Pointer[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void write(long bOff, short[] buf, int index, int length) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.write(bOff, buf, index, length);
		}

		@Override
		public void setByte(long offset, byte value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setByte(offset, value);
		}

		@Override
		public void setChar(long offset, char value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setChar(offset, value);
		}

		@Override
		public void setDouble(long offset, double value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setDouble(offset, value);
		}

		@Override
		public void setFloat(long offset, float value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setFloat(offset, value);
		}

		@Override
		public void setInt(long offset, int value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setInt(offset, value);
		}

		@Override
		public void setLong(long offset, long value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setLong(offset, value);
		}

		@Override
		public void setMemory(long offset, long length, byte value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setMemory(offset, length, value);
		}

		@Override
		public void setNativeLong(long offset, NativeLong value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setNativeLong(offset, value);
		}

		@Override
		public void setPointer(long offset, Pointer value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setPointer(offset, value);
		}

		@Override
		public void setShort(long offset, short value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setShort(offset, value);
		}

		@Override
		public void setString(long offset, String value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setString(offset, value);
		}

		@Override
		public void setString(long offset, String value, boolean wide) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setString(offset, value, wide);
		}

		@Override
		public void setString(long offset, String value, String encoding) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setString(offset, value, encoding);
		}

		@Override
		public void setString(long offset, WString value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setString(offset, value);
		}

		@Override
		public void setWideString(long offset, String value) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.setWideString(offset, value);
		}

		@Override
		public Memory align(int byteBoundary) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			return super.align(byteBoundary);
		}

		@Override
		public void clear() {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.clear();
		}

		@Override
		public void clear(long size) {
			if (m_sealed)
				throw new UnsupportedOperationException();

			super.clear(size);
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			if (m_sealed)
				throw new UnsupportedOperationException();

			return super.clone();
		}
	}
}
