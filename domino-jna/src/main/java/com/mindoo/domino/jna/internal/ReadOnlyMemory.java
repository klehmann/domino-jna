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
	//keep reference to prevent garbage collection of this wrapped memory until we get garbage collected
	private Memory m_wrappedMemory;
	private boolean m_sealed;
	
	/**
	 * Creates a new instance
	 * 
	 * @param wrappedMemory writable Memory that we want to wrap to prevent data changes
	 */
	public ReadOnlyMemory(Memory wrappedMemory) {
		super();
		this.peer = Memory.nativeValue(wrappedMemory);
		this.size = wrappedMemory.size();
		//keep reference on original memory to prevent GC
		m_wrappedMemory = wrappedMemory;
		m_sealed = true;
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param wrappedMemory writable Memory that we want to wrap to prevent data changes
	 * @param offset offset in <code>wrappedMemory</code> to start
	 */
	public ReadOnlyMemory(Memory wrappedMemory, int offset) {
		this(wrappedMemory, offset, (int) wrappedMemory.size() - offset);
	}

	/**
	 * Creates a new instance
	 * 
	 * @param wrappedMemory writable Memory that we want to wrap to prevent data changes
	 * @param offset offset in <code>wrappedMemory</code> to start
	 * @param size new size of this Memory object
	 */
	public ReadOnlyMemory(Memory wrappedMemory, int offset, int size) {
		super();
		this.peer = Memory.nativeValue(wrappedMemory) + offset;
		this.size = size;
		if ((offset+size)>wrappedMemory.size())
			throw new IllegalArgumentException("Exceeded size of wrapped memory, offset="+offset+", size="+size+" while size of wrapped memory is "+wrappedMemory.size());
		
		//keep reference on original memory to prevent GC
		m_wrappedMemory = wrappedMemory;
		m_sealed = true;
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
	
	@Override
	public Pointer share(long offset) {
		return new ReadOnlyMemory(this, (int) offset);
	}

	@Override
	public Pointer share(long offset, long sz) {
		return new ReadOnlyMemory(this, (int) offset, (int) sz);
	}
	
	
}
