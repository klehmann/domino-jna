package com.mindoo.domino.jna.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.EnumSet;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.MimeStreamItemizeOptions;
import com.mindoo.domino.jna.constants.MimeStreamOpenOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Utility class to read a {@link NotesNote} in MIME format or populate (itemize) a
 * document with data from a MIME document.<br>
 * 
 * @author Karsten Lehmann
 */
public class MIMEStream implements IRecyclableNotesObject, AutoCloseable {
	private NotesNote m_note;
	private String m_itemName;
	private Pointer m_hMIMEStream;
	private boolean m_recycled;
	
	/**
	 * Returns a {@link Reader} to read the MIME data of an item
	 * 
	 * @param note note
	 * @param itemName item name
	 * @return reader
	 */
	public static InputStream getMIMEAsInputStream(NotesNote note, String itemName) {
		return getMIMEAsInputStream(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
	}

	/**
	 * Returns a {@link Reader} to read the MIME data of an item
	 * 
	 * @param note note
	 * @param itemName item name
	 * @param flags open flags (e.g. whether to include RFC822 and headers)
	 * @return reader
	 */
	public static InputStream getMIMEAsInputStream(NotesNote note, String itemName, EnumSet<MimeStreamOpenOptions> flags) {
		MIMEStream stream = newStreamForRead(note, itemName, flags);
//		return new MIMEStreamAsInputStream(stream, 2000);
		return new MIMEStreamAsInputStream(stream, 3000000);
	}

	/**
	 *  Creates a MIME stream, serializes the named items into it, and returns the MIME stream.<br>
	 *  Returns just the MIME content for the specified item, no named / RF822 items of
	 *  the note.
	 * 
	 * @param note note
	 * @param itemName item name
	 * @return MIME stream
	 */
	public static MIMEStream newStreamForRead(NotesNote note, String itemName) {
		return newStreamForRead(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
	}
	
	/**
	 *  Creates a MIME stream, serializes the named items into it, and returns the MIME stream.<br>
	 *  Specify the flag {@link MimeStreamOpenOptions#RFC2822_INCLUDE_HEADERS}
	 *  to flush all RFC2822 headers to the output MIME stream; i.e.,
	 *  the message's initial headers -- To, From, Subject, Date, etc.<br>
	 *  <br>
	 *  Also specify the flag {@link MimeStreamOpenOptions#MIME_INCLUDE_HEADERS}
	 *  to flush all MIME entity headers to the output MIME stream.
	 * 
	 * @param note note
	 * @param itemName item name
	 * @param flags open flags (e.g. whether to include RFC822 and headers)
	 * @return MIME stream
	 */
	public static MIMEStream newStreamForRead(NotesNote note, String itemName, EnumSet<MimeStreamOpenOptions> flags) {
		EnumSet<MimeStreamOpenOptions> flagsClone = flags.clone();
		
		int dwOpenFlags = MimeStreamOpenOptions.toBitMaskInt(flagsClone);
		dwOpenFlags = dwOpenFlags | NotesConstants.MIME_STREAM_OPEN_READ;
		return new MIMEStream(note, itemName, dwOpenFlags);
	}

	/**
	 * Creates a MIME stream. Use {@link #putLine(String)}
	 * or {@link #write(Reader)} to specify the MIME data to be written, then
	 * call {@link #itemize(EnumSet)} to write the data to the document.
	 * 
	 * @param note note
	 * @param itemName mime item to write content
	 * @return MIME stream
	 */
	public static MIMEStream newStreamForWrite(NotesNote note, String itemName) {
		return newStreamForWrite(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
	}

	/**
	 * Creates a MIME stream. Use {@link #putLine(String)}
	 * or {@link #write(Reader)} to specify the MIME data to be written, then
	 * call {@link #itemize(EnumSet)} to write the data to the document.
	 * 
	 * @param note note
	 * @param itemName mime item to write content
	 * @param flags open flags
	 * @return MIME stream
	 */
	public static MIMEStream newStreamForWrite(NotesNote note, String itemName, EnumSet<MimeStreamOpenOptions> flags) {
		EnumSet<MimeStreamOpenOptions> flagsClone = flags.clone();

		int dwOpenFlags = MimeStreamOpenOptions.toBitMaskInt(flagsClone);
		dwOpenFlags = dwOpenFlags | NotesConstants.MIME_STREAM_OPEN_WRITE;
		return new MIMEStream(note, itemName, dwOpenFlags);
	}

	/**
	 * Writes MIME data to a {@link NotesNote}.
	 * 
	 * @param note note to write MIME
	 * @param itemName name of item to write MIME content (e.g. "body")
	 * @param reader reader used to read MIME content
	 * @param itemizeFlags used to select which data should be written (MIME headers, body or both)
	 * @throws IOException in case of I/O errors writing MIME
	 */
	public static void writeRawMIME(NotesNote note, String itemName, InputStream in,
			EnumSet<MimeStreamItemizeOptions> itemizeFlags) throws IOException {
		
		if ("$file".equalsIgnoreCase(itemName)) {
			throw new IllegalArgumentException("Invalid item name: "+itemName);
		}
		
		if (itemizeFlags.isEmpty()) {
			throw new IllegalArgumentException("Itemize flags cannot be empty");
		}
		
		if (itemizeFlags.contains(MimeStreamItemizeOptions.ITEMIZE_BODY) &&
				!itemizeFlags.contains(MimeStreamItemizeOptions.ITEMIZE_HEADERS)) {
			
			//write just the body
			NotesNote tmpNote = note.getParent().createNote();
			MIMEStream stream = newStreamForWrite(tmpNote, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
			try {
				byte[] buf = new byte[16384];
				int len;
				while ((len = in.read(buf))>0) {
					stream.writeFrom(buf, 0, len);
				}
				
				//use both ITEMIZE_BODY and ITEMIZE_HEADERS,
				//otherwise we end up having To: , Subject: etc. in the first Body item
				stream.itemize(EnumSet.of(MimeStreamItemizeOptions.ITEMIZE_BODY, MimeStreamItemizeOptions.ITEMIZE_HEADERS));
				
				//remove old items from target note
				while (note.hasItem(itemName)) {
					note.removeItem(itemName);
				}
				
				//copy created MIME items from temp note to target note
				tmpNote.getItems(itemName, (item, loop) -> {
					item.copyToNote(note, false);
				});
				
				//copy part data that exceeded 64k
				tmpNote.getItems("$file", (item, loop) -> {
					item.copyToNote(note, false);
				});
			}
			finally {
				stream.recycle();
				tmpNote.recycle();
			}
		}
		else {
			MIMEStream stream = newStreamForWrite(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
			try {
				byte[] buf = new byte[16384];
				int len;
				
				while ((len = stream.readInto(buf)) > 0) {
					stream.writeFrom(buf, 0, len);
				}
				stream.itemize(itemizeFlags);
			}
			finally {
				stream.recycle();
			}
		}
	}
	
	/**
	 * Convenience function that reads the MIME data of a {@link NotesNote} and
	 * streams it into a {@link Writer}.
	 * 
	 * @param note note
	 * @param itemName item that contains the MIME data
	 * @param out strea, to receive the MIME data
	 * @param openFlags specifies whether MIME headers or RFC822 items should be exported or just the content of <code>itemName</code>
	 * @throws IOException in case of I/O errors
	 */
	public static void readRawMIME(NotesNote note, String itemName, OutputStream out, EnumSet<MimeStreamOpenOptions> openFlags) throws IOException {
		MIMEStream stream = newStreamForRead(note, itemName, openFlags);
		try {
			stream.readInto(out);
		}
		finally {
			stream.recycle();
		}
	}

	/**
	 * Creates a new MIMEStream
	 * 
	 * @param note note to read/write MIME data
	 * @param itemName item to read/write MIME data
	 * @param dwOpenFlags open flags
	 */
	private MIMEStream(NotesNote note, String itemName, int dwOpenFlags) {
		if (note.isRecycled()) {
			throw new NotesError(0, "Note is recycled");
		}
		if ("$file".equalsIgnoreCase(itemName)) {
			throw new IllegalArgumentException("Invalid item name: "+itemName);
		}

		m_note = note;
		m_itemName = itemName;
		
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);

		PointerByReference rethMIMEStream = new PointerByReference();
		rethMIMEStream.setValue(null);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().MIMEStreamOpen(note.getHandle64(),
					itemNameMem, (short) (itemNameMem.size() & 0xffff), dwOpenFlags, rethMIMEStream);
			NotesErrorUtils.checkResult(result);
			m_hMIMEStream = rethMIMEStream.getValue();
		}
		else {
			result = NotesNativeAPI32.get().MIMEStreamOpen(note.getHandle32(),
					itemNameMem, (short) (itemNameMem.size() & 0xffff), dwOpenFlags, rethMIMEStream);
			NotesErrorUtils.checkResult(result);
			m_hMIMEStream = rethMIMEStream.getValue();
		}
		NotesGC.__objectCreated(MIMEStream.class, this);
	}

	private void checkRecycled() {
		if (isRecycled()) {
			throw new NotesError(0, "Stream already recycled");
		}
		else if (m_note.isRecycled()) {
			throw new NotesError(0, "Nnote is recycled");
		}
	}
	
	@Override
	public boolean isRecycled() {
		return m_recycled;
	}
	
	@Override
	public boolean isNoRecycle() {
		return false;
	}

	@Override
	public int getHandle32() {
		return m_hMIMEStream==null ? 0 : (int) (Pointer.nativeValue(m_hMIMEStream) & 0xffffffff);
	}

	@Override
	public long getHandle64() {
		return m_hMIMEStream==null ? 0 : Pointer.nativeValue(m_hMIMEStream);
	}
	
	@Override
	public void recycle() {
		if (isRecycled()) {
			return;
		}
		
		NotesGC.__objectBeeingBeRecycled(MIMEStream.class, this);
		NotesNativeAPI.get().MIMEStreamClose(m_hMIMEStream);
		m_recycled = true;
	}
	
	@Override
	public void close() {
		recycle();
	}
	
	public enum MimeStreamResult {
		/** successful MIME stream I/O. */
		SUCCESS,
		/** End Of Stream -- no more data in the MIME stream. */
		EOS
	}

	/**
	 * Writes a single line into the stream
	 * 
	 * @param line line
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public void putLine(String line) throws IOException {
		checkRecycled();
		
		Memory lineMem = NotesStringUtils.toLMBCS(line, true, false);

		int resultAsInt = NotesNativeAPI.get().MIMEStreamPutLine(lineMem, m_hMIMEStream);
		
		if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
			throw new IOException("I/O error received during MIME stream operation");
		}
	}

	private MimeStreamResult toStreamResult(int resultAsInt) throws IOException {
		if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
			throw new IOException("I/O error received during MIME stream operation");
		}
		else if (resultAsInt == NotesConstants.MIME_STREAM_EOS) {
			return MimeStreamResult.EOS;
		}
		else {
			return MimeStreamResult.SUCCESS;
		}
	}

	/**
	 * This function copies the MIME stream content into a {@link OutputStream}.
	 * 
	 * @param out stream to receive the MIME stream data
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public void readInto(OutputStream out) throws IOException {
		checkRecycled();
		
		int MAX_BUFFER_SIZE = 60000;
		byte[] buf = new byte[MAX_BUFFER_SIZE];
		int len;
		
		while ((len = readInto(buf))> 0) {
			out.write(buf, 0, len);
		}
		
		out.flush();
	}
	
	/**
	 * This function copies the MIME stream content into a {@link Writer}.
	 * 
	 * @param buffer buffer to receive the MIME stream data
	 * @param maxBufferSize max characters to read from the stream into the appendable
	 * @return number of bytes read or -1 for EOF
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public int readInto(byte[] buffer) throws IOException {
		checkRecycled();
		
		int maxBufferSize = buffer.length;
		DisposableMemory pchData = new DisposableMemory(maxBufferSize);
		try {
			IntByReference puiDataLen = new IntByReference();
			puiDataLen.setValue(0);
			MimeStreamResult result = toStreamResult(NotesNativeAPI.get().MIMEStreamRead(pchData,
					puiDataLen, maxBufferSize, m_hMIMEStream));

			int len = puiDataLen.getValue();
			if (len > 0) {
				pchData.read(0, buffer, 0, len);
				return len;
			}
			else {
				return -1;
			}
		}
		finally {
			pchData.dispose();
		}
	}

	/**
	 * This function parses the MIME stream content to create a Notes/Domino MIME format document.<br>
	 * <br>
	 * With {@link MimeStreamItemizeOptions#ITEMIZE_HEADERS}, we only parse and itemize the
	 * MIME streams initial RFC822 headers into Notes/Domino items, e.g. the Internet message header
	 * 'To:' is itemized to the Notes item 'SendTo'.<br>
	 * 
	 * We itemize the headers and the body parts of the input MIME stream if both
	 * {@link MimeStreamItemizeOptions#ITEMIZE_HEADERS} and {@link MimeStreamItemizeOptions#ITEMIZE_BODY}
	 * are set.
	 * 
	 * @param itemizeFlags flags to control the itemize operation
	 */
	public void itemize(EnumSet<MimeStreamItemizeOptions> itemizeFlags) {
		checkRecycled();
		
		Memory targetItemNameMem = NotesStringUtils.toLMBCS(m_itemName, false);
		
		int dwFlags = MimeStreamItemizeOptions.toBitMaskInt(itemizeFlags);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().MIMEStreamItemize(m_note.getHandle64(),
					targetItemNameMem, (short) (targetItemNameMem.size() & 0xffff), dwFlags, m_hMIMEStream);
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = NotesNativeAPI64.get().MIMEStreamItemize(m_note.getHandle64(),
					targetItemNameMem, (short) (targetItemNameMem.size() & 0xffff), dwFlags, m_hMIMEStream);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function rewinds a MIME stream to the beginning.
	 * 
	 * @return this instance
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public MIMEStream rewind() throws IOException {
		checkRecycled();
		
		int resultAsInt = NotesNativeAPI.get().MIMEStreamRewind(m_hMIMEStream);
		
		if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
			throw new IOException("I/O error received during MIME stream operation");
		}
		return this;
	}
	
	/**
	 * Writes all content of an {@link InputStream} to the stream
	 * 
	 * @param in stream
	 * @return this instance
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public MIMEStream writeFrom(InputStream in) throws IOException {
		byte[] buf = new byte[16384];
		int len;
		while ((len = in.read(buf))>0) {
			writeFrom(buf, 0, len);
		}
		
		return this;
	}
	
	/**
	 * Writes a byte buffer to the MIME stream
	 * 
	 * @param buffer byte buffer to write
	 * @param offset start offset of data to write
	 * @param length number of bytes to write
	 * @return this instance
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public MIMEStream writeFrom(byte[] buffer, int offset, int length) throws IOException {
		checkRecycled();
		
		if (length>0) {
			DisposableMemory bufferMem = new DisposableMemory(length);
			try {
				bufferMem.write(0, buffer, offset, length);

				int resultAsInt = NotesNativeAPI.get().MIMEStreamWrite(bufferMem, length, m_hMIMEStream);

				if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
					throw new IOException("I/O error received during MIME stream operation");
				}
			}
			finally {
				bufferMem.dispose();
			}
		}
		
		return this;
	}

	@Override
	public String toString() {
		if (!m_note.isRecycled() && !m_note.getParent().isRecycled()) {
			return "MIMEStream [noteid=" + m_note.getNoteId() + ", noteunid=" + m_note.getUNID() +
					", itemname=" + m_itemName +
					", db="+m_note.getParent().getServer()+"!!"+m_note.getParent().getRelativeFilePath() +
					"]";
		}
		else {
			return "MIMEStream [note=recycled, itemname="+m_itemName+"]";
		}
	}

	/**
	 * Adapter between {@link MIMEStream} and {@link InputStream}

	 * @author Karsten Lehmann
	 */
	private static class MIMEStreamAsInputStream extends InputStream {
		private MIMEStream m_mimeStream;
		private byte[] m_buffer;
		private int m_bufferPos;
		private int m_leftInBuffer;
		
		/**
		 * Creates a new instance
		 * 
		 * @param mimeStream MIME stream to read from
		 * @param bufSize size of internal load buffer
		 */
		public MIMEStreamAsInputStream(MIMEStream mimeStream, int bufSize) {
			m_mimeStream = mimeStream;
			if (bufSize <=0) {
				throw new IllegalArgumentException("Invalid buffer size: "+bufSize);
			}
			m_buffer = new byte[bufSize];
		}

		@Override
		public int read(byte[] b) throws IOException {
			 return read(b, 0, b.length);
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
	            throw new NullPointerException();
	        } else if (off < 0 || len < 0 || len > b.length - off) {
	            throw new IndexOutOfBoundsException();
	        } else if (len == 0) {
	            return 0;
	        }

	        int c = read();
	        if (c == -1) {
	            return -1;
	        }
	        b[off] = (byte)c;

	        int i = 1;
	        try {
	            for (; i < len ; i++) {
	                c = read();
	                if (c == -1) {
	                    break;
	                }
	                b[off + i] = (byte)c;
	            }
	        } catch (IOException ee) {
	        }
	        return i;
		}
		
		@Override
		public int read() throws IOException {
			if (m_leftInBuffer == 0) {
				//end reached, read more data
				int read = m_mimeStream.readInto(m_buffer);
				if (read==-1 || read==0) {
					return -1;
				}
				m_leftInBuffer = read;
				m_bufferPos = 0;
			}
			
			byte b = m_buffer[m_bufferPos++];
			m_leftInBuffer--;
			return (int) (b & 0xff);
		}
		
		@Override
		public void close() throws IOException {
			m_mimeStream.close();
		}
	}
}
