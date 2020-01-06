package com.mindoo.domino.jna.internal.mime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.Compression;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils.LineBreakConversion;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

public class MimeWriterUtil {

	/**
	 * Writes all items of the message into a document as if it got received from an external SMTP server,
	 * which covers RFC822 headers (like "To" which is copied into a "SendTo" item), MIME headers and binary MIME part data
	 * 
	 * @param message MIME message, either creates via standard JavaMail API or a high level project like <a href="https://commons.apache.org/proper/commons-email/" target="_blank">Apache Commons Email</a>
	 * @param targetNote target note that receives the data
	 */
	public static void itemizeMIMEMessage(final Message message, final NotesNote targetNote) {
		if (targetNote.isRecycled()) {
			throw new NotesError(0, "Note already recycled");
		}

		//size of in-memory buffer to transfer MIME data from Message object to Domino MIME stream
		final int BUFFERSIZE = 16384;

		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethMIMEStream = new LongByReference();

			result = NotesNativeAPI64.get().MIMEStreamOpen(targetNote.getHandle64(), null,
					(short) 0, NotesConstants.MIME_STREAM_OPEN_WRITE, rethMIMEStream);
			NotesErrorUtils.checkResult(result);

			final long hMIMEStream = rethMIMEStream.getValue();

			final DisposableMemory buf = new DisposableMemory(BUFFERSIZE);
			try {
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
							int mimeStatus = NotesNativeAPI64.get().MIMEStreamWrite(buf, bytesInBuffer, hMIMEStream);
							System.out.println("MIME status: "+mimeStatus);
							if (mimeStatus == NotesConstants.MIME_STREAM_IO) {
								throw new IOException("Received I/O error from Domino when writing to the MIME stream");
							}

							bytesInBuffer = 0;
						}
					}
				});

				//itemize the mime stream to the note
				result = NotesNativeAPI64.get().MIMEStreamItemize(targetNote.getHandle64(),
						null, (short) 0, NotesConstants.MIME_STREAM_ITEMIZE_FULL, hMIMEStream);
				NotesErrorUtils.checkResult(result);

			} catch (IOException e) {
				throw new NotesError(0, "Error writing MIME data to MIME stream", e);
			} catch (MessagingException e) {
				throw new NotesError(0, "Error writing MIME data to MIME stream", e);
			}
			finally {
				buf.dispose();
				NotesNativeAPI64.get().MIMEStreamClose(hMIMEStream);
			}
		}
		else {
			IntByReference rethMIMEStream = new IntByReference();

			result = NotesNativeAPI32.get().MIMEStreamOpen(targetNote.getHandle32(), null,
					(short) 0, NotesConstants.MIME_STREAM_OPEN_WRITE, rethMIMEStream);
			NotesErrorUtils.checkResult(result);

			final int hMIMEStream = rethMIMEStream.getValue();

			final DisposableMemory buf = new DisposableMemory(BUFFERSIZE);
			try {
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
							int mimeStatus = NotesNativeAPI32.get().MIMEStreamWrite(buf, bytesInBuffer, hMIMEStream);
							if (mimeStatus == NotesConstants.MIME_STREAM_IO) {
								throw new IOException("Received I/O error from Domino when writing to the MIME stream");
							}

							bytesInBuffer = 0;
						}
					}
				});

				//itemize the mime stream to the note
				result = NotesNativeAPI64.get().MIMEStreamItemize(targetNote.getHandle64(),
						null, (short) 0, NotesConstants.MIME_STREAM_ITEMIZE_FULL, hMIMEStream);
				NotesErrorUtils.checkResult(result);

			} catch (IOException e) {
				throw new NotesError(0, "Error writing MIME data to MIME stream", e);
			} catch (MessagingException e) {
				throw new NotesError(0, "Error writing MIME data to MIME stream", e);
			}
			finally {
				buf.dispose();
				NotesNativeAPI32.get().MIMEStreamClose(hMIMEStream);
			}
		}
	}

	/**
	 * Writes all items of the message into a document, which covers RFC822 headers (like "To" which is copied into a
	 * "SendTo" item), MIME headers and binary MIME part data
	 * 
	 * @param mimeMessageData {@link InputStream} with MIME data including RFC822 and MIME headers as well as MIME part data
	 * @param targetNote target note that receives the data
	 */
	public static void itemizeMIMEMessage(InputStream mimeMessageData, final NotesNote targetNote) {
		if (targetNote.isRecycled()) {
			throw new NotesError(0, "Note already recycled");
		}

		//size of in-memory buffer to transfer MIME data from Message object to Domino MIME stream
		final int BUFFERSIZE = 16384;

		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethMIMEStream = new LongByReference();

			result = NotesNativeAPI64.get().MIMEStreamOpen(targetNote.getHandle64(), null,
					(short) 0, NotesConstants.MIME_STREAM_OPEN_WRITE, rethMIMEStream);
			NotesErrorUtils.checkResult(result);

			final long hMIMEStream = rethMIMEStream.getValue();

			DisposableMemory bufMem = new DisposableMemory(BUFFERSIZE);
			try {
				byte[] buf = new byte[BUFFERSIZE];
				int len;

				while ((len=mimeMessageData.read(buf))>0) {
					bufMem.write(0, buf, 0, len);

					int mimeStatus = NotesNativeAPI64.get().MIMEStreamWrite(bufMem, len, hMIMEStream);
					System.out.println("MIME status: "+mimeStatus);
					if (mimeStatus == NotesConstants.MIME_STREAM_IO) {
						throw new IOException("Received I/O error from Domino when writing to the MIME stream");
					}
				}

				//itemize the mime stream to the note
				result = NotesNativeAPI64.get().MIMEStreamItemize(targetNote.getHandle64(),
						null, (short) 0, NotesConstants.MIME_STREAM_ITEMIZE_FULL, hMIMEStream);
				NotesErrorUtils.checkResult(result);

			} catch (IOException e) {
				throw new NotesError(0, "Error writing MIME data to MIME stream", e);
			}
			finally {
				bufMem.dispose();
				NotesNativeAPI64.get().MIMEStreamClose(hMIMEStream);
			}
		}
		else {
			IntByReference rethMIMEStream = new IntByReference();

			result = NotesNativeAPI32.get().MIMEStreamOpen(targetNote.getHandle32(), null,
					(short) 0, NotesConstants.MIME_STREAM_OPEN_WRITE, rethMIMEStream);
			NotesErrorUtils.checkResult(result);

			final int hMIMEStream = rethMIMEStream.getValue();

			DisposableMemory bufMem = new DisposableMemory(BUFFERSIZE);
			try {
				byte[] buf = new byte[BUFFERSIZE];
				int len;

				while ((len=mimeMessageData.read(buf))>0) {
					bufMem.write(0, buf, 0, len);

					int mimeStatus = NotesNativeAPI32.get().MIMEStreamWrite(bufMem, len, hMIMEStream);
					if (mimeStatus == NotesConstants.MIME_STREAM_IO) {
						throw new IOException("Received I/O error from Domino when writing to the MIME stream");
					}
				}

				//itemize the mime stream to the note
				result = NotesNativeAPI32.get().MIMEStreamItemize(targetNote.getHandle32(),
						null, (short) 0, NotesConstants.MIME_STREAM_ITEMIZE_FULL, hMIMEStream);
				NotesErrorUtils.checkResult(result);

			} catch (IOException e) {
				throw new NotesError(0, "Error writing MIME data to MIME stream", e);
			}
			finally {
				bufMem.dispose();
				NotesNativeAPI32.get().MIMEStreamClose(hMIMEStream);
			}
		}
	}

	private static byte[] CRLF;

	static {
		CRLF = new byte[2];
		CRLF[0] = (byte)'\r';
		CRLF[1] = (byte)'\n';
	}


	public static void writeMIMEMultipart(MimeMultipart mime, NotesNote targetNote, String itemName) throws MessagingException, IOException {
		if (targetNote.isRecycled()) {
			throw new NotesError(0, "Note already recycled");
		}
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);

		_writeMIMEMultipart(mime, null, targetNote, itemNameMem);
	}


	private final static short TRUE = 1;
	private final static short FALSE = 0;
    
    private static void writePossiblyLargeStringToMIME64(long hCtx, String str) {
    	StringBuilder writeBuf = new StringBuilder();
		final int MAXWRITEBUF = 30000;

		for (int i=0; i<str.length(); i++) {
			writeBuf.append(str.charAt(i));
			if ((writeBuf.length() >= MAXWRITEBUF) || i==(str.length()-1)) {
				DisposableMemory writeBufMem = NotesStringUtils.toLMBCSNoCache(writeBuf.toString(), false, LineBreakConversion.ORIGINAL);
				try {
					short result = NotesNativeAPI64.get().NSFMimePartAppendStream(hCtx, writeBufMem, (short) (writeBufMem.size() & 0xffff));
					NotesErrorUtils.checkResult(result);
				}
				finally {
					writeBufMem.dispose();
				}

				writeBuf.setLength(0);
			}
		}
    }
    
    private static void writePossiblyLargeStringToMIME32(int hCtx, String str) {
    	StringBuilder writeBuf = new StringBuilder();
		final int MAXWRITEBUF = 30000;

		for (int i=0; i<str.length(); i++) {
			writeBuf.append(str.charAt(i));
			if ((writeBuf.length() >= MAXWRITEBUF) || i==(str.length()-1)) {
				DisposableMemory writeBufMem = NotesStringUtils.toLMBCSNoCache(writeBuf.toString(), false, LineBreakConversion.ORIGINAL);
				try {
					short result = NotesNativeAPI32.get().NSFMimePartAppendStream(hCtx, writeBufMem, (short) (writeBufMem.size() & 0xffff));
					NotesErrorUtils.checkResult(result);
				}
				finally {
					writeBufMem.dispose();
				}

				writeBuf.setLength(0);
			}
		}
    }
    
    public static enum HasHeaders {TRUE, FALSE}
    public static enum HasBoundary {TRUE, FALSE}
    
    public static void writeStringToMIME(String str, HasHeaders hasHeaders, HasBoundary hasBoundary,
    		NotesNote targetNote, String itemName) {
    	Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
    	
    	writeStringToMIME(str, hasHeaders, hasBoundary, targetNote, itemNameMem);
    }
    
    private static void writeStringToMIME(String str, HasHeaders hasHeaders, HasBoundary hasBoundary,
    		NotesNote targetNote, Memory itemNameMem) {
    	
    	System.out.println("_writeStringToMIME:\n"+str+"\n"+"--------");
    	
		int dwFlags = 0;
		if (hasHeaders == HasHeaders.TRUE) {
			dwFlags = dwFlags | NotesConstants.MIME_PART_HAS_HEADERS;
		}
		if (hasBoundary == HasBoundary.TRUE) {
			dwFlags = dwFlags | NotesConstants.MIME_PART_HAS_BOUNDARY;
		}
		
    	short result;
    	if (PlatformUtils.is64Bit()) {
			LongByReference phCtx = new LongByReference();
			
    		result = NotesNativeAPI64.get().NSFMimePartCreateStream(targetNote.getHandle64(), itemNameMem,
					(short) (itemNameMem.size() & 0xffff), NotesConstants.MIME_PART_BODY, 
					dwFlags, phCtx);
			NotesErrorUtils.checkResult(result);
			
			long hCtx = phCtx.getValue();
			boolean update = false;
			
			try {
				writePossiblyLargeStringToMIME64(hCtx, str);
				update = true;
			}
			finally {
				result = NotesNativeAPI64.get().NSFMimePartCloseStream(hCtx, update ? TRUE : FALSE);
				NotesErrorUtils.checkResult(result);
			}
    	}
    	else {
    		IntByReference phCtx = new IntByReference();
			
    		result = NotesNativeAPI32.get().NSFMimePartCreateStream(targetNote.getHandle32(), itemNameMem,
					(short) (itemNameMem.size() & 0xffff), NotesConstants.MIME_PART_BODY, 
					dwFlags, phCtx);
			NotesErrorUtils.checkResult(result);
			
			int hCtx = phCtx.getValue();
			boolean update = false;
			
			try {
				writePossiblyLargeStringToMIME32(hCtx, str);
				update = true;
			}
			finally {
				result = NotesNativeAPI32.get().NSFMimePartCloseStream(hCtx, update ? TRUE : FALSE);
				NotesErrorUtils.checkResult(result);
			}
    	}
    }
    
	private static void _writeMIMEMultipart(Multipart multiPart, String outerBoundary, NotesNote targetNote, Memory itemNameMem) throws MessagingException, IOException {
	    String contentType = multiPart.getContentType();
		
		//write content type with boundary
		StringBuilder sb = new StringBuilder();
		
		if (outerBoundary!=null) {
			sb.append(outerBoundary).append("\r\n");
		}
		
		sb.append("Content-type: ").append(contentType).append("\r\n")
		.append("\r\n");
		
		if (multiPart instanceof MimeMultipart) {
			String preamble = ((MimeMultipart)multiPart).getPreamble();
			if (!StringUtil.isEmpty(preamble)) {
				sb.append(preamble);
				
				if (!preamble.endsWith("\r\n")) {
					sb.append("\r\n");
				}
				sb.append("\r\n");
			}
		}
		
		writeStringToMIME(sb.toString(), HasHeaders.TRUE, HasBoundary.FALSE, targetNote, itemNameMem);
		
		String boundary = "--" + (new ContentType(contentType)).getParameter("boundary");

		for (int i=0; i<multiPart.getCount(); i++) {
			BodyPart currBodyPart = multiPart.getBodyPart(i);
			
			_writeMIMEBodyPart(currBodyPart, boundary, targetNote, itemNameMem);
		}
		
		writeStringToMIME("\r\n" + boundary + "--", HasHeaders.FALSE, HasBoundary.TRUE, targetNote, itemNameMem);
	}
	
	private static String guessMimeType(String fileName) {
		if (StringUtil.endsWithIgnoreCase(fileName, ".jpg")) {
			return "image/jpeg";
		}
		else if (StringUtil.endsWithIgnoreCase(fileName, ".png")) {
			return "image/png";
		}
		else if (StringUtil.endsWithIgnoreCase(fileName, ".html")) {
			return "text/html";
		}
		else {
			return "application/octet-stream";
		}
	}
	
	private static void _writeMIMEBodyPart(BodyPart bodyPart, String boundary, NotesNote targetNote, Memory itemNameMem) throws MessagingException, IOException {
		// First, write out the header
//		if (bodyPart instanceof MimeBodyPart) {
//			Enumeration<String> hdrLines
//			= ((MimeBodyPart)bodyPart).getNonMatchingHeaderLines(null);
//			while (hdrLines.hasMoreElements()) {
//				System.out.println(hdrLines.nextElement());
//			}
//		}
		
		StringBuilder boundaryAndHeadersConc = new StringBuilder();
		boundaryAndHeadersConc.append(boundary).append("\r\n");
		
		Object content = bodyPart.getContent();

		Enumeration<Header> headers = bodyPart.getNonMatchingHeaders(null);
		boolean hasHeaders = headers.hasMoreElements();
		while (headers.hasMoreElements()) {
			Header currHeader = headers.nextElement();
			
			String currHeaderName = currHeader.getName();
			String currHeaderValue = currHeader.getValue();

			boolean headerOk;
			if (content instanceof InputStream) {
				if ("Content-Disposition".equalsIgnoreCase(currHeaderName)) {
					if (currHeaderValue.startsWith("inline")) {
						currHeaderValue = "inline";
					}
				}
				
				if (!"Content-Transfer-Encoding".equalsIgnoreCase(currHeaderName)) {
					headerOk = true;
				}
				else {
					headerOk = false;
				}
			}
			else {
				headerOk = true;
			}
			
			if (headerOk) {
				boundaryAndHeadersConc.append(currHeaderName).append(": ").append(currHeaderValue);
				boundaryAndHeadersConc.append("\r\n"); 
			}
		}

//		if (content instanceof String) {
//			if (bodyPart instanceof MimePart) {
//				MimePart mimeBodyPart = (MimePart) bodyPart;
//				String encoding = mimeBodyPart.getEncoding();
//				if (StringUtil.isEmpty(encoding)) {
//					encoding = "7bit";
//				}
//				boundaryAndHeadersConc.append("Content-Transfer-Encoding: ").append(encoding);
//				boundaryAndHeadersConc.append("\r\n"); 
//			}
//		}
		
		if (!(content instanceof MimeMultipart)) {
			String contentType = null;
			
			DataHandler dataHandler = bodyPart.getDataHandler();
			if (dataHandler!=null) {
				contentType = dataHandler.getContentType();
			}
			
			
			if (StringUtil.isEmpty(contentType)) {
				if (content instanceof InputStream) {
					String fileName = bodyPart.getFileName();
					if (!StringUtil.isEmpty(fileName)) {
						contentType = guessMimeType(fileName);
					}
					else {
						contentType = "application/octet-stream";
					}
				}
				else if (content instanceof String) {
					String strContent = (String) content;
					if (StringUtil.startsWithIgnoreCase(strContent, "<html>")) {
						contentType = "text/html";
					}
				}
			}
			
			if (!StringUtil.isEmpty(contentType)) {
				boundaryAndHeadersConc.append("Content-Type: ").append(contentType);
				boundaryAndHeadersConc.append("\r\n"); 
			}
		}
		
		
		boolean embedAsBase64 = false;

		if (content instanceof InputStream) {
			if (embedAsBase64) {
				boundaryAndHeadersConc.append("Content-Transfer-Encoding: base64\r\n");
			}
			else {
				boundaryAndHeadersConc.append("Content-Transfer-Encoding: binary\r\n");
			}
		}

		boundaryAndHeadersConc.append("\r\n"); 


		int dwFlags = NotesConstants.MIME_PART_HAS_BOUNDARY;

		if (hasHeaders) {
			dwFlags = dwFlags | NotesConstants.MIME_PART_HAS_HEADERS;
		}

		if (content instanceof String) {
			writeStringToMIME(boundaryAndHeadersConc + (String) content + "\r\n" + "\r\n", hasHeaders ? HasHeaders.TRUE : HasHeaders.FALSE,
					HasBoundary.TRUE, targetNote, itemNameMem);
		}
		else if (content instanceof InputStream) {
			InputStream contentAsStream = (InputStream) content;
			
			//temporarily write binary data to file
			File tmpFile = null;
			try {
				tmpFile = File.createTempFile("jnamimewrite", ".jpg");
				FileOutputStream fOut = new FileOutputStream(tmpFile);
				try {
					byte[] buf = new byte[16384];
					int len;

					while ((len=contentAsStream.read(buf))>0) {
						fOut.write(buf, 0, len);
					}
				}
				finally {
					fOut.close();
				}

				String filePath = tmpFile.getAbsolutePath();
				DisposableMemory filePathMem = NotesStringUtils.toLMBCSNoCache(filePath, true, LineBreakConversion.ORIGINAL);
				try {
					if (PlatformUtils.is64Bit()) {
						LongByReference phCtx = new LongByReference();
						
						short result = NotesNativeAPI64.get().NSFMimePartCreateStream(targetNote.getHandle64(), itemNameMem,
								(short) (itemNameMem.size() & 0xffff), NotesConstants.MIME_PART_BODY, 
								dwFlags, phCtx);
						NotesErrorUtils.checkResult(result);
						
						long hCtx = phCtx.getValue();
						
						System.out.println("Header for file:\n"+boundaryAndHeadersConc);
						
						DisposableMemory boundaryAndHeadersConcMem = NotesStringUtils.toLMBCSNoCache(boundaryAndHeadersConc.toString(),
								false, LineBreakConversion.ORIGINAL);
						
						boolean success = false;
						try {
							result = NotesNativeAPI64.get().NSFMimePartAppendStream(hCtx, boundaryAndHeadersConcMem, (short) (boundaryAndHeadersConcMem.size() & 0xffff));
							
							if (embedAsBase64) {
								result = NotesNativeAPI64.get().NSFMimePartAppendFileToStream(hCtx, filePathMem);
								NotesErrorUtils.checkResult(result);
							}
							else {
								NotesAttachment att = targetNote.attachFile(filePath, tmpFile.getName(), Compression.NONE);
								String uniqueFilename = att.getFileName();
								Memory uniqueFilenameMem = NotesStringUtils.toLMBCS(uniqueFilename, true);
								result = NotesNativeAPI64.get().NSFMimePartAppendObjectToStream(hCtx, uniqueFilenameMem);
								NotesErrorUtils.checkResult(result);
							}
							
							success = true;
						}
						finally {
							result = NotesNativeAPI64.get().NSFMimePartCloseStream(hCtx, success ? TRUE : FALSE);
							NotesErrorUtils.checkResult(result);
							
							boundaryAndHeadersConcMem.dispose();
						}
					}
					else {
						IntByReference phCtx = new IntByReference();
						
						short result = NotesNativeAPI32.get().NSFMimePartCreateStream(targetNote.getHandle32(), itemNameMem,
								(short) (itemNameMem.size() & 0xffff), NotesConstants.MIME_PART_BODY, 
								dwFlags, phCtx);
						NotesErrorUtils.checkResult(result);
						
						int hCtx = phCtx.getValue();
						
						DisposableMemory boundaryAndHeadersConcMem = NotesStringUtils.toLMBCSNoCache(boundaryAndHeadersConc.toString(),
								false, LineBreakConversion.ORIGINAL);

						boolean success = false;
						try {
							result = NotesNativeAPI32.get().NSFMimePartAppendStream(hCtx, boundaryAndHeadersConcMem, (short) (boundaryAndHeadersConcMem.size() & 0xffff));
							
							if (embedAsBase64) {
								result = NotesNativeAPI32.get().NSFMimePartAppendFileToStream(hCtx, filePathMem);
								NotesErrorUtils.checkResult(result);
							}
							else {
								NotesAttachment att = targetNote.attachFile(filePath, tmpFile.getName(), Compression.NONE);
								String uniqueFilename = att.getFileName();
								Memory uniqueFilenameMem = NotesStringUtils.toLMBCS(uniqueFilename, true);
								result = NotesNativeAPI32.get().NSFMimePartAppendObjectToStream(hCtx, uniqueFilenameMem);
								NotesErrorUtils.checkResult(result);
							}
							success = true;
						}
						finally {
							result = NotesNativeAPI32.get().NSFMimePartCloseStream(hCtx, success ? TRUE : FALSE);
							NotesErrorUtils.checkResult(result);
							
							boundaryAndHeadersConcMem.dispose();
						}
					}
				}
				finally {
					filePathMem.dispose();
				}
			}
			finally {
				contentAsStream.close();
				
				if (tmpFile!=null && !tmpFile.delete()) {
					tmpFile.deleteOnExit();
				}
			}
		}
		else if (content instanceof Multipart) {
//			_writeStringToMIME(boundaryAndHeadersConc.toString(), hasHeaders ? HasHeaders.TRUE : HasHeaders.FALSE,
//					HasBoundary.TRUE, targetNote, itemNameMem);

			_writeMIMEMultipart((Multipart) content, boundary, targetNote, itemNameMem);
		}
	}
}