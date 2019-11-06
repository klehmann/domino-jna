package com.mindoo.domino.jna.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Class converting a {@link Reader} into an {@link InputStream}.
 * 
 * @author Tammo Riedinger
 */
public class ReaderInputStream extends InputStream
{
	protected Reader m_reader;
	protected String m_encoding;
	private byte m_lastByte[];
	private byte m_converterBuffer[];
	private int m_currentConverterPos;
	
	public ReaderInputStream(Reader reader) {
		if(reader == null)
			throw new IllegalArgumentException("ReaderInputStream: a source reader has to be provided");

		m_lastByte = new byte[1];
		m_reader = reader;
	}
	
	public ReaderInputStream(Reader reader, String encoding) {
		m_reader = reader;
		m_encoding = encoding;

		if(reader == null)
			throw new IllegalArgumentException("ReaderInputStream: a source reader has to be provided");

	}
	
	public synchronized int read() throws IOException {
		if(read(m_lastByte, 0, 1) == -1)
			return -1;
		else
			return m_lastByte[0];
	}
	
	public synchronized int read(byte b[], int off, int len) throws IOException {
		if(b == null)
			throw new IllegalArgumentException("ReaderInputStream: a buffer has to  be provided.");
		if(off < 0 || len < 0 || off > b.length || off + len > b.length)
			throw new ArrayIndexOutOfBoundsException();
		
		if(len == 0)
			return 0;
		
		int bytesRead = 0;
		if(m_converterBuffer != null){
			int bufferLength = readBuffer(b, off, len);
			if(bufferLength == len)
				return len;
			len -= bufferLength;
			off += bufferLength;
			bytesRead += bufferLength;
		}
		if(m_converterBuffer == null) {
			char buffer[] = new char[len];
			int result = m_reader.read(buffer, 0, len);
			if(result == -1)
				return bytesRead <= 0 ? -1 : bytesRead;
			String data = new String(buffer, 0, result);
			if(m_encoding == null)
				m_converterBuffer = data.getBytes();
			else
				m_converterBuffer = data.getBytes(m_encoding);
			return readBuffer(b, off, len);
		} else
			return -1;
	}
	
	private int readBuffer(byte b[], int off, int len) {
		int bufferLength = m_converterBuffer.length - m_currentConverterPos;
		if(bufferLength >= len){
			System.arraycopy(m_converterBuffer, m_currentConverterPos, b, off, len);
			m_currentConverterPos += len;
			return len;
		} else {
			System.arraycopy(m_converterBuffer, m_currentConverterPos, b, off, bufferLength);
			m_converterBuffer = null;
			m_currentConverterPos = 0;
			return bufferLength;
		}
	}
	
	public synchronized void reset() throws IOException {
		m_reader.reset();
	}
	
	public synchronized void close() throws IOException	{
		m_reader.close();
	}
}