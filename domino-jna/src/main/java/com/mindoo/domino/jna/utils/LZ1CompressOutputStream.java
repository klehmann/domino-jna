package com.mindoo.domino.jna.utils;

import java.io.IOException;
import java.io.OutputStream;

import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * {@link OutputStream} that compresses the data with the LZ1 algorithm, which
 * compresses strings by replacing them with a token (which is itself huffman encoded)
 * pointing to an earlier occurance of the same string in the uncompressed (or decompressed)
 * buffer.<br>
 * <br>
 * <b><u>Please note:</u><br>
 * The underlying C function is currently configured to give up compressing once the size of the
 * output buffer reaches approximately 7/8 the size of the input buffer. In that case,
 * a {@link NotesError} of type {@link INotesErrorConstants#ERR_LZ1FAILED} is thrown.<br>
 * <br>
 * In that case, partial compression data will probably have been written to the
 * {@link OutputStream} specified as constructor argument.</b>
 * 
 * @author Karsten Lehmann
 */
public class LZ1CompressOutputStream extends OutputStream implements IAllocatedMemory {
	private OutputStream m_out;
	
	private byte[] m_uncompressedBuffer;
	private DisposableMemory m_uncompressedBufferMem;
	private int m_uncompressedBufferPos;
	
	private byte[] m_compressedBuffer;
	private DisposableMemory m_compressedBufferMem;
	private long m_hCompHT64;
	private int m_hCompHT32;
	
	/**
	 * Creates a new instance with a default buffer size of 100.000 bytes
	 * 
	 * @param out stream to write the compressed data
	 */
	public LZ1CompressOutputStream(OutputStream out) {
		this(out, 100000);
	}
	
	/**
	 * Creates a new instance with the specified buffer size.
	 * 
	 * @param out stream to write the compressed data
	 * @param bufferSize size of buffer
	 */
	public LZ1CompressOutputStream(OutputStream out, int bufferSize) {
		m_out = out;
		
		//buffer for the uncompressed data
		m_uncompressedBuffer = new byte[bufferSize];
		m_uncompressedBufferMem = new DisposableMemory(bufferSize);
		
		//buffer for the LZ1 compressed data
		m_compressedBuffer = new byte[bufferSize+1];
		m_compressedBufferMem = new DisposableMemory(bufferSize+1);

		//allocate memory buffer for a hash table used during compression
		//to find previously compressed tokens
		if (PlatformUtils.is64Bit()) {
			LongByReference retHandle = new LongByReference();
			short result = Mem64.OSMemAlloc((short) 0, 65535, retHandle);
			NotesErrorUtils.checkResult(result);
			m_hCompHT64 = retHandle.getValue();
			NotesGC.__memoryAllocated(this);
		}
		else {
			IntByReference retHandle = new IntByReference();
			short result = Mem32.OSMemAlloc((short) 0, 65535, retHandle);
			NotesErrorUtils.checkResult(result);
			m_hCompHT32 = retHandle.getValue();
			NotesGC.__memoryAllocated(this);
		}
	}

	@Override
	public void free() {
		if (isFreed())
			return;
		
		if (PlatformUtils.is64Bit()) {
			short result = Mem64.OSMemFree(m_hCompHT64);
			NotesErrorUtils.checkResult(result);
			m_hCompHT64 = 0;
		}
		else {
			short result = Mem32.OSMemFree(m_hCompHT32);
			NotesErrorUtils.checkResult(result);
			m_hCompHT32 = 0;
		}
		m_uncompressedBufferMem.dispose();
		m_uncompressedBufferMem = null;
		m_compressedBufferMem.dispose();
		m_compressedBufferMem = null;
	}

	@Override
	public boolean isFreed() {
		return PlatformUtils.is64Bit() ? m_hCompHT64==0 : m_hCompHT32==0;
	}

	@Override
	public int getHandle32() {
		return m_hCompHT32;
	}

	@Override
	public long getHandle64() {
		return m_hCompHT64;
	}

	@Override
	public void close() throws IOException {
		if (isFreed())
			return;
		
		flush();
		free();
		m_out.close();
	}
	
	@Override
	public void flush() throws IOException {
		if (m_uncompressedBufferPos==0)
			return;
		
		m_uncompressedBufferMem.write(0, m_uncompressedBuffer, 0, m_uncompressedBufferPos);
		m_compressedBufferMem.clear();
		short result;
		if (PlatformUtils.is64Bit()) {
			IntByReference retOutSize = new IntByReference();
			result = NotesNativeAPI64.get().LZ1Compress(m_uncompressedBufferMem, m_compressedBufferMem, m_uncompressedBufferPos, m_hCompHT64, retOutSize);
			NotesErrorUtils.checkResult(result);
			
			int iRetOutSize = retOutSize.getValue();
			m_compressedBufferMem.read(0, m_compressedBuffer, 0, iRetOutSize);
			m_out.write(m_compressedBuffer, 0, iRetOutSize);
		}
		else {
			IntByReference retOutSize = new IntByReference();
			result = NotesNativeAPI32.get().LZ1Compress(m_uncompressedBufferMem, m_compressedBufferMem, m_uncompressedBufferPos, m_hCompHT32, retOutSize);
			NotesErrorUtils.checkResult(result);
			
			int iRetOutSize = retOutSize.getValue();
			m_compressedBufferMem.read(0, m_compressedBuffer, 0, iRetOutSize);
			m_out.write(m_compressedBuffer, 0, iRetOutSize);
		}
		m_uncompressedBufferPos = 0;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (isFreed())
			throw new IllegalStateException("Memory already freed");

		m_uncompressedBuffer[m_uncompressedBufferPos++] = (byte) (b & 0xff);
		
		//check if end of buffer os reached
		if (m_uncompressedBufferPos==m_uncompressedBuffer.length) {
			//compress buffer data and reset buffer position to 0
			flush();
		}
	}

}
