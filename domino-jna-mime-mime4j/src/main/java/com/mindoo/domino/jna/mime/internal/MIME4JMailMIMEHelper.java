package com.mindoo.domino.jna.mime.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.stream.MimeConfig;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.MimeStreamItemizeOptions;
import com.mindoo.domino.jna.constants.MimeStreamOpenOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.mime.MIMEStream;

public class MIME4JMailMIMEHelper {

	/**
	 * Reads the MIME content of a {@link NotesNote} and parses it as {@link Message}.<br>
	 * Please make sure to have sufficient memory so that the MIME data can fit into the Java heap.
	 * Otherwise use {@link MIMEStream#readRawMIME(NotesNote, String, OutputStream, EnumSet)} instead which
	 * allows streaming of the data.
	 * 
	 * @param note note with MIME data
	 * @param itemName MIME item containing the data, should be "body" in most of the cases
	 * @param openFlags specifies whether MIME headers or RFC822 items should be exported or just the content of <code>itemName</code>
	 * @return parsed MIME message
	 * @throws NotesError if something goes wrong extracting or parsing the data
	 */
	public static Message readMIMEMessage(NotesNote note, String itemName,
			EnumSet<MimeStreamOpenOptions> openFlags) {
		final Exception[] ex = new Exception[1];
		
		Message msg = AccessController.doPrivileged(new PrivilegedAction<Message>() {

			@Override
			public Message run() {
				MIMEStream stream = MIMEStream.newStreamForRead(note, itemName, openFlags);
				
				File tmpFile = null;
				try {
					//use a temp file to not store the MIME content twice in memory (raw + parsed)
					tmpFile = File.createTempFile("dominojna_mime_", ".tmp");
					
					try (FileOutputStream out = new FileOutputStream(tmpFile)) {
						stream.readInto(out);
					}

					DefaultMessageBuilder defaultMessageBuilder = new DefaultMessageBuilder();
					defaultMessageBuilder.setMimeEntityConfig(MimeConfig.PERMISSIVE);

					try (FileInputStream fIn = new FileInputStream(tmpFile);
							BufferedInputStream bufIn = new BufferedInputStream(fIn)) {
						
						Message message = defaultMessageBuilder.parseMessage(bufIn);
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
	 * Writes the content of a {@link Message} to a {@link NotesNote}.
	 * 
	 * @param note note to write MIME data
	 * @param itemName item name that receives the content (e.g. "body")
	 * @param message message containing the MIME data
	 * @param itemizeFlags used to select which data should be written (MIME headers, body or both)
	 * @throws IOException in case of I/O errors writing MIME
	 */
	public static void writeMIMEMessage(NotesNote note, String itemName, Message message,
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
			MIMEStream stream = MIMEStream.newStreamForWrite(tmpNote, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
			try {
				write(stream, message);
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
			MIMEStream stream = MIMEStream.newStreamForWrite(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
			try {
				write(stream, message);
				stream.itemize(itemizeFlags);
			}
			finally {
				stream.recycle();
			}
		}
		
	}
	
	/**
	 * Writes the MIME content of a {@link Message} to the stream
	 * 
	 * @param stream MIME stream
	 * @param message message to append to the stream
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public static void write(final MIMEStream stream, Message message) throws IOException {
		//size of in-memory buffer to transfer MIME data from Message object to Domino MIME stream
		final int BUFFERSIZE = 16384;
		
		final byte[] buffer = new byte[BUFFERSIZE];
		
		OutputStream out = new OutputStream() {
			int bytesInBuffer = 0;

			@Override
			public void write(int b) throws IOException {
				buffer[bytesInBuffer] = (byte) (b & 0xff);
				bytesInBuffer++;
				if (bytesInBuffer == buffer.length) {
					flushBuffer();
				}
			}

			@Override
			public void close() throws IOException {
				flushBuffer();
			}

			private void flushBuffer() throws IOException {
				if (bytesInBuffer > 0) {
					stream.writeFrom(buffer, 0, bytesInBuffer);

					bytesInBuffer = 0;
				}
			}
		};
		
		try {
			MessageWriter writer = new DefaultMessageWriter();
			writer.writeMessage(message, out);
		}
		finally {
			out.close();
		}
	}
}
