package com.mindoo.domino.jna.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Subclass of {@link Pointer} that adds bounds checks to memory access methods
 */
public class PointerWithBounds extends Pointer {
	protected long size;

	public PointerWithBounds(long peer, long size) {
		super(peer);
		this.size = size;
	}

	public PointerWithBounds(Pointer ptr, long size) {
		super(Pointer.nativeValue(ptr));
		this.size = size;
	}

	public long size() {
		return size;
	}

	/**
	 * Check that indirection won't cause us to write outside the
	 * malloc'ed space.
	 *
	 */
	protected void boundsCheck(long off, long sz) {
		if (off < 0) {
			throw new IndexOutOfBoundsException("Invalid offset: " + off);
		}
		if (off + sz > size) {
			String msg = "Bounds exceeds available space : size="
					+ size + ", offset=" + (off + sz);
			throw new IndexOutOfBoundsException(msg);
		}
	}

	/** Provide a view of this memory using the given offset as the base address.  The
	 * returned {@link Pointer} will have a size equal to that of the original
	 * minus the offset.
	 * @throws IndexOutOfBoundsException if the requested memory is outside
	 * the allocated bounds.
	 */
	@Override
	public Pointer share(long offset) {
		return share(offset, size() - offset);
	}

	/** Provide a view of this memory using the given offset as the base
	 * address, bounds-limited with the given size.  Maintains a reference to
	 * the original {@link Memory} object to avoid GC as long as the shared
	 * memory is referenced.
	 * @throws IndexOutOfBoundsException if the requested memory is outside
	 * the allocated bounds.
	 */
	@Override
	public Pointer share(long offset, long sz) {
		boundsCheck(offset, sz);
		return new PointerWithBounds(peer + offset, sz);
	}

	//////////////////////////////////////////////////////////////////////////
	// Raw read methods
	//////////////////////////////////////////////////////////////////////////

	@Override
	public void read(long bOff, byte[] buf, int index, int length) {
		boundsCheck(bOff, length * 1L);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, short[] buf, int index, int length) {
		boundsCheck(bOff, length * 2L);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, char[] buf, int index, int length) {
		boundsCheck(bOff, length * Native.WCHAR_SIZE);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, int[] buf, int index, int length) {
		boundsCheck(bOff, length * 4L);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, long[] buf, int index, int length) {
		boundsCheck(bOff, length * 8L);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, float[] buf, int index, int length) {
		boundsCheck(bOff, length * 4L);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, double[] buf, int index, int length) {
		boundsCheck(bOff, length * 8L);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void read(long bOff, Pointer[] buf, int index, int length) {
		boundsCheck(bOff, length * Native.POINTER_SIZE);
		super.read(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, byte[] buf, int index, int length) {
		boundsCheck(bOff, length * 1L);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, short[] buf, int index, int length) {
		boundsCheck(bOff, length * 2L);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, char[] buf, int index, int length) {
		boundsCheck(bOff, length * Native.WCHAR_SIZE);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, int[] buf, int index, int length) {
		boundsCheck(bOff, length * 4L);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, long[] buf, int index, int length) {
		boundsCheck(bOff, length * 8L);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, float[] buf, int index, int length) {
		boundsCheck(bOff, length * 4L);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, double[] buf, int index, int length) {
		boundsCheck(bOff, length * 8L);
		super.write(bOff, buf, index, length);
	}

	@Override
	public void write(long bOff, Pointer[] buf, int index, int length) {
		boundsCheck(bOff, length * Native.POINTER_SIZE);
		super.write(bOff, buf, index, length);
	}

	@Override
	public byte getByte(long offset) {
		boundsCheck(offset, 1);
		return super.getByte(offset);
	}

	@Override
	public char getChar(long offset) {
		boundsCheck(offset, Native.WCHAR_SIZE);
		return super.getChar(offset);
	}

	@Override
	public short getShort(long offset) {
		boundsCheck(offset, 2);
		return super.getShort(offset);
	}

	@Override
	public int getInt(long offset) {
		boundsCheck(offset, 4);
		return super.getInt(offset);
	}

	@Override
	public long getLong(long offset) {
		boundsCheck(offset, 8);
		return super.getLong(offset);
	}

	@Override
	public float getFloat(long offset) {
		boundsCheck(offset, 4);
		return super.getFloat(offset);
	}

	@Override
	public double getDouble(long offset) {
		boundsCheck(offset, 8);
		return super.getDouble(offset);
	}

	@Override
	public Pointer getPointer(long offset) {
		boundsCheck(offset, Native.POINTER_SIZE);
		return super.getPointer(offset);
	}

	@Override
	public ByteBuffer getByteBuffer(long offset, long length) {
		boundsCheck(offset, length);
		return super.getByteBuffer(offset, length);
	}

	@Override
	public String getString(long offset, String encoding) {
		// NOTE: we only make sure the start of the string is within bounds
		boundsCheck(offset, 0);
		return super.getString(offset, encoding);
	}

	@Override
	public String getWideString(long offset) {
		// NOTE: we only make sure the start of the string is within bounds
		boundsCheck(offset, 0);
		return super.getWideString(offset);
	}

	@Override
	public void setByte(long offset, byte value) {
		boundsCheck(offset, 1);
		super.setByte(offset, value);
	}

	@Override
	public void setChar(long offset, char value) {
		boundsCheck(offset, Native.WCHAR_SIZE);
		super.setChar(offset, value);
	}

	@Override
	public void setShort(long offset, short value) {
		boundsCheck(offset, 2);
		super.setShort(offset, value);
	}

	@Override
	public void setInt(long offset, int value) {
		boundsCheck(offset, 4);
		super.setInt(offset, value);
	}

	@Override
	public void setLong(long offset, long value) {
		boundsCheck(offset, 8);
		super.setLong(offset, value);
	}

	@Override
	public void setFloat(long offset, float value) {
		boundsCheck(offset, 4);
		super.setFloat(offset, value);
	}

	@Override
	public void setDouble(long offset, double value) {
		boundsCheck(offset, 8);
		super.setDouble(offset, value);
	}

	@Override
	public void setPointer(long offset, Pointer value) {
		boundsCheck(offset, Native.POINTER_SIZE);
		super.setPointer(offset, value);
	}

	@Override
	public void setString(long offset, String value, String encoding) {
		boundsCheck(offset, getBytes(value, encoding).length + 1L);
		super.setString(offset, value, encoding);
	}

	@Override
	public void setWideString(long offset, String value) {
		boundsCheck(offset, (value.length() + 1L) * Native.WCHAR_SIZE);
		super.setWideString(offset, value);
	}

	static byte[] getBytes(String s, String encoding) {
		return getBytes(s, getCharset(encoding));
	}

	static byte[] getBytes(String s, Charset charset) {
		return s.getBytes(charset);
	}

	public static final Charset DEFAULT_CHARSET;
	static {
		String nativeEncoding = System.getProperty("native.encoding");
		Charset nativeCharset = null;
		if (nativeEncoding != null) {
			try {
				nativeCharset = Charset.forName(nativeEncoding);
			} catch (Exception ex) {
				Logger.getLogger(PointerWithBounds.class.getName()).log(Level.WARNING, "Failed to get charset for native.encoding value : '" + nativeEncoding + "'", ex);
			}
		}
		if (nativeCharset == null) {
			nativeCharset = Charset.defaultCharset();
		}
		DEFAULT_CHARSET = nativeCharset;
	}


	private static Charset getCharset(String encoding) {
		Charset charset = null;
		if (encoding != null) {
			try {
				charset = Charset.forName(encoding);
			}
			catch(IllegalCharsetNameException e) {
				Logger.getLogger(PointerWithBounds.class.getName()).log(Level.WARNING, "JNA Warning: Encoding ''{0}'' is unsupported ({1})",
						new Object[]{encoding, e.getMessage()});
			}
			catch(UnsupportedCharsetException  e) {
				Logger.getLogger(PointerWithBounds.class.getName()).log(Level.WARNING, "JNA Warning: Encoding ''{0}'' is unsupported ({1})",
						new Object[]{encoding, e.getMessage()});
			}
		}
		if (charset == null) {
			Logger.getLogger(PointerWithBounds.class.getName()).log(Level.WARNING, "JNA Warning: Using fallback encoding {0}", DEFAULT_CHARSET);
			charset = DEFAULT_CHARSET;
		}
		return charset;
	}

}
