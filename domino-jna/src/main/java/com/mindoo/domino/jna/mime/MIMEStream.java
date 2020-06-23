package com.mindoo.domino.jna.mime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.mindoo.domino.jna.NotesDatabase;
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
	public static Reader getMIMEReader(NotesNote note, String itemName) {
		return getMIMEReader(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
	}

	/**
	 * Returns a {@link Reader} to read the MIME data of an item
	 * 
	 * @param note note
	 * @param itemName item name
	 * @param flags open flags (e.g. whether to include RFC822 and headers)
	 * @return reader
	 */
	public static Reader getMIMEReader(NotesNote note, String itemName, EnumSet<MimeStreamOpenOptions> flags) {
		MIMEStream stream = newStreamForRead(note, itemName, flags);
		return new MIMEStreamReader(stream, 2000);
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
	 * Writes the content of a {@link Message} to a {@link NotesNote}.
	 * 
	 * @param note note to write MIME data
	 * @param itemName item name that receives the content (e.g. "body")
	 * @param message message containing the MIME data
	 * @param itemizeFlags used to select which data should be written (MIME headers, body or both)
	 * @throws IOException in case of I/O errors writing MIME
	 * @throws MessagingException in case of errors reading the MIME message
	 */
	public static void writeMIMEMessage(NotesNote note, String itemName, Message message,
			EnumSet<MimeStreamItemizeOptions> itemizeFlags) throws IOException, MessagingException {
		
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
				stream.write(message);
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
				stream.write(message);
				stream.itemize(itemizeFlags);
			}
			finally {
				stream.recycle();
			}
		}
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
	public static void writeRawMIME(NotesNote note, String itemName, Reader reader,
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
				stream.write(reader);
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
				stream.write(reader);
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
	 * @param writer writer to receive the MIME data
	 * @param openFlags specifies whether MIME headers or RFC822 items should be exported or just the content of <code>itemName</code>
	 * @throws IOException in case of I/O errors
	 */
	public static void readRawMIME(NotesNote note, String itemName, Writer writer, EnumSet<MimeStreamOpenOptions> openFlags) throws IOException {
		MIMEStream stream = newStreamForRead(note, itemName, openFlags);
		try {
			stream.read(writer);
		}
		finally {
			stream.recycle();
		}
	}
	
	/**
	 * Reads the MIME content of a {@link NotesNote} and parses it as {@link MimeMessage}.<br>
	 * Please make sure to have sufficient memory so that the MIME data can fit into the Java heap.
	 * Otherwise use {@link #readRawMIME(NotesNote, String, Writer, EnumSet)} instead which
	 * allows streaming of the data.
	 * 
	 * @param note note with MIME data
	 * @param itemName MIME item containing the data, should be "body" in most of the cases
	 * @param openFlags specifies whether MIME headers or RFC822 items should be exported or just the content of <code>itemName</code>
	 * @return parsed MIME message
	 * @throws NotesError if something goes wrong extracting or parsing the data
	 */
	public static MimeMessage readMIMEMessage(NotesNote note, String itemName,
			EnumSet<MimeStreamOpenOptions> openFlags) {
		final Exception[] ex = new Exception[1];
		
		MimeMessage msg = AccessController.doPrivileged(new PrivilegedAction<MimeMessage>() {

			@Override
			public MimeMessage run() {
				MIMEStream stream = newStreamForRead(note, itemName, openFlags);
				
				File tmpFile = null;
				try {
					//use a temp file to not store the MIME content twice in memory (raw + parsed)
					tmpFile = File.createTempFile("dominojna_mime_", ".tmp");
					
					try (FileWriter writer = new FileWriter(tmpFile)) {
						stream.read(writer);
					}
					
					try (FileInputStream fIn = new FileInputStream(tmpFile);
							BufferedInputStream bufIn = new BufferedInputStream(fIn)) {
						
						Properties props = System.getProperties(); 
						javax.mail.Session mailSession = javax.mail.Session.getInstance(props, null);
						MimeMessage message = new MimeMessage(mailSession, bufIn);
						return message;
					}
				}
				catch (Exception e) {
					ex[0] = e;
					return null;
				}
				finally {
					if (tmpFile.exists() && !tmpFile.delete()) {
						tmpFile.deleteOnExit();
					}
					stream.recycle();
				}
			}
		});
		
		if (ex[0] != null) {
			NotesDatabase parentDb = note.getParent();
			throw new NotesError(0, "Error parsing the MIME content of document with UNID "+
					note.getUNID()+" and item name "+itemName+
					" in database "+parentDb.getServer()+"!!"+parentDb.getRelativeFilePath(), ex[0]);
		}
		return msg;
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
		return 0;
	}

	@Override
	public long getHandle64() {
		return 0;
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
	 * Reads a single line from the input MIME stream.
	 * 
	 * @param target {@link Appendable} to receive the line
	 * @return true if End Of Stream, false otherwise
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public MimeStreamResult readLine(Appendable target) throws IOException {
		checkRecycled();

		DisposableMemory retLine = new DisposableMemory(400);
		try {
			if (PlatformUtils.is64Bit()) {
				int resultAsInt = NotesNativeAPI64.get().MIMEStreamGetLine(retLine, (int) retLine.size(), m_hMIMEStream);
				MimeStreamResult result = toStreamResult(resultAsInt);
				
				String line = NotesStringUtils.fromLMBCS(retLine, -1);
				target.append(line);
				
				return result;
			}
			else {
				int resultAsInt = NotesNativeAPI32.get().MIMEStreamGetLine(retLine, (int) retLine.size(), m_hMIMEStream);
				MimeStreamResult result = toStreamResult(resultAsInt);
				
				String line = NotesStringUtils.fromLMBCS(retLine, -1);
				target.append(line);
				
				return result;
			}
		}
		finally {
			retLine.dispose();
		}
	}

	/**
	 * This function copies the MIME stream content into a {@link Writer}.
	 * 
	 * @param writer writer to receive the MIME stream data
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public void read(Writer writer) throws IOException {
		checkRecycled();
		
		int MAX_BUFFER_SIZE = 60000;
		DisposableMemory pchData = new DisposableMemory(MAX_BUFFER_SIZE);
		try {
			IntByReference puiDataLen = new IntByReference();

			MimeStreamResult result = null;
			while (result != MimeStreamResult.EOS) {
				result = toStreamResult(NotesNativeAPI.get().MIMEStreamRead(pchData,
						puiDataLen, MAX_BUFFER_SIZE, m_hMIMEStream));
				int len = puiDataLen.getValue();
				if (len > 0) {
					String txt = NotesStringUtils.fromLMBCS(pchData, len);
					writer.write(txt);
				}
				else {
					return;
				}
			}
		}
		finally {
			pchData.dispose();
		}
	}

	/**
	 * This function copies the MIME stream content into a {@link Writer}.
	 * 
	 * @param appendable appendable to receive the MIME stream data
	 * @param maxBufferSize max characters to read from the stream into the appendable
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public int read(Appendable appendable, int maxBufferSize) throws IOException {
		checkRecycled();
		
		DisposableMemory pchData = new DisposableMemory(maxBufferSize);
		try {
			IntByReference puiDataLen = new IntByReference();

			MimeStreamResult result = toStreamResult(NotesNativeAPI.get().MIMEStreamRead(pchData,
					puiDataLen, maxBufferSize, m_hMIMEStream));
			int len = puiDataLen.getValue();
			if (len > 0) {
				String txt = NotesStringUtils.fromLMBCS(pchData, len);
				appendable.append(txt);
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
	 * This function writes a buffer to the input MIME stream.
	 * 
	 * @param reader reader to get the MIME stream data
	 * @return this instance
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public MIMEStream write(Reader reader) throws IOException {
		char[] buffer = new char[60000];
		int len;
		
		while ((len=reader.read(buffer))>0) {
			String txt = new String(buffer, 0, len);
			Memory txtMem = NotesStringUtils.toLMBCS(txt, false, false);
			
			int resultAsInt = NotesNativeAPI.get().MIMEStreamWrite(txtMem, (short) (txtMem.size() & 0xffff), m_hMIMEStream);
			
			if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
				throw new IOException("I/O error received during MIME stream operation");
			}

		}
		return this;
	}
	
	/**
	 * Writes the MIME content of a {@link Message} to the stream
	 * 
	 * @param message message to append to the stream
	 * @return this instance
	 * @throws IOException in case of MIME stream I/O errors
	 * @throws MessagingException in case of read errors from the {@link Message}
	 */
	public MIMEStream write(Message message) throws IOException, MessagingException {
		//size of in-memory buffer to transfer MIME data from Message object to Domino MIME stream
		final int BUFFERSIZE = 16384;
		
		final DisposableMemory buf = new DisposableMemory(BUFFERSIZE);
		
		message.writeTo(new OutputStream() {
			int bytesInBuffer = 0;

			@Override
			public void write(int b) throws IOException {
				buf.setByte(bytesInBuffer, (byte) (b & 0xff));
				bytesInBuffer++;
				if (bytesInBuffer == buf.size()) {
					flushBuffer();
				}
			}

			@Override
			public void close() throws IOException {
				flushBuffer();
			}

			private void flushBuffer() throws IOException {
				if (bytesInBuffer > 0) {
					int resultAsInt = NotesNativeAPI.get().MIMEStreamWrite(buf, bytesInBuffer, m_hMIMEStream);

					if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
						throw new IOException("I/O error received during MIME stream operation");
					}

					bytesInBuffer = 0;
				}
			}
		});
		
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
	 * Adapter between {@link MIMEStream} and {@link Reader}

	 * @author Karsten Lehmann
	 */
	private static class MIMEStreamReader extends Reader {
		private MIMEStream m_mimeStream;
		private int m_bufSize;
		private StringBuilder m_buffer;
		
		/**
		 * Creates a new instance
		 * 
		 * @param mimeStream MIME stream to read from
		 * @param bufSize size of internal load buffer
		 */
		public MIMEStreamReader(MIMEStream mimeStream, int bufSize) {
			m_mimeStream = mimeStream;
			m_bufSize = bufSize;
			m_buffer = new StringBuilder();
		}

		/**
		 * Reads one character from the buffer
		 * 
		 * @return character or -1 for end of stream
		 * @throws IOException
		 */
		public int readFromBuffer() throws IOException {
			if (m_buffer.length() == 0) {
				int read = m_mimeStream.read(m_buffer, m_bufSize);
				if (read==-1 || m_buffer.length()==0) {
					return -1;
				}
			}
			
			char c = m_buffer.charAt(0);
			m_buffer.deleteCharAt(0);
			return c;
		}
		
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			for (int i=0; i<len; i++) {
				int c = readFromBuffer();
				
				if (c==-1) {
					if (i==0) {
						return -1;
					}
					else {
						return i;
					}
				}
				else {
					cbuf[off + i] = (char) c;
				}
			}
			
			return len;
		}

		@Override
		public void close() throws IOException {
			m_mimeStream.close();
		}
	}
}
