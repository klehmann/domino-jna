package com.mindoo.domino.jna.mime.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.junit.Test;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.mime.MIMEData;
import com.mindoo.domino.jna.mime.MIMEStream;
import com.mindoo.domino.jna.mime.NotesMimeUtils;
import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.MIMEEntity;
import lotus.domino.MIMEHeader;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.Stream;

public class TestReadMimeAttachment extends BaseJNATestClass {
	private static final String TEST_IMAGE_PATH = "/images/test-png-large.png";

	@Test
	public void testReadMimeAttachment() {
		runWithSession(new IDominoCallable<Object>() {

			private String createDocWithMIMEAttachmentFromFile(Session session, Database db,
					String resourcePath) throws NotesException, IOException {

				URL resourceUrl = getClass().getResource(resourcePath);
				if (resourceUrl==null) {
					throw new FileNotFoundException("Resource with path "+resourcePath+" not found");
				}

				Document doc = db.createDocument();

				ByteArrayOutputStream fileDataOut = new ByteArrayOutputStream();
				try (InputStream in = resourceUrl.openStream()) {
					byte[] buf = new byte[16384];
					int len;

					while ((len = in.read(buf))>0) {
						fileDataOut.write(buf, 0, len);
					}
				}

				String strFilename = resourceUrl.getFile();
				int idx = strFilename.lastIndexOf('/');
				strFilename = strFilename.substring(idx+1);

				doc.replaceItemValue("Form", "Attachment");
				doc.replaceItemValue("Subject", strFilename+" "+(new Date()));

				String fileDataBase64 = Base64.getEncoder().encodeToString(fileDataOut.toByteArray());

				Stream stream = session.createStream();
				stream.writeText(fileDataBase64);

				MIMEEntity body = doc.createMIMEEntity("Body");
				MIMEHeader header = body.createHeader("Content-Type");
				header.setHeaderVal("multipart/mixed");

				MIMEEntity child = body.createChildEntity();
				MIMEHeader childheader = child.createHeader("Content-Disposition");
				String urlEncodedFilename = URLEncoder.encode(strFilename, StandardCharsets.UTF_8.toString());
				childheader.setHeaderVal("attachment;filename=\"" + strFilename + "\";filename*=utf-8''" + urlEncodedFilename);
				child.setContentFromText(stream, "", MIMEEntity.ENC_BASE64);
				child.decodeContent();

				doc.save(true, false);
				String unid = doc.getUniversalID();
				stream.recycle();

				return unid;
			}

			@Override
			public Object call(Session session) throws Exception {
				URL resourceUrl = getClass().getResource(TEST_IMAGE_PATH);
				if (resourceUrl==null) {
					throw new FileNotFoundException("Resource with path "+TEST_IMAGE_PATH+" not found");
				}
				URLConnection conn = resourceUrl.openConnection();
				long fileSize;
				try {
					fileSize = Long.parseLong(conn.getHeaderField("content-length"));
				}
				finally {
					if (conn instanceof HttpURLConnection) {
						((HttpURLConnection)conn).disconnect();
					}
				}

				withTempDb((dbTemp) -> {
					Database dbTempLegacy = session.getDatabase("", dbTemp.getAbsoluteFilePathOnLocal());
					String unid = createDocWithMIMEAttachmentFromFile(session, dbTempLegacy, TEST_IMAGE_PATH);
					dbTempLegacy.recycle();

					NotesNote note = dbTemp.openNoteByUnid(unid);

					MIMEData mimeData = NotesMimeUtils.getMimeData(note, "body");

					IMimeAttachment attachment = mimeData.getAttachments().stream().findFirst().get();
					assertEquals(fileSize, attachment.getFileSize());

//					Path tmpFile1 = Paths.get("/tmp/dominojna-via-mimestream");
//					Files.deleteIfExists(tmpFile1);
//
//					Path tmpFile2 = Paths.get("/tmp/dominojna-via-inputstream");
//					Files.deleteIfExists(tmpFile2);

					ByteArrayOutputStream bodyOutViaOutputStream = new ByteArrayOutputStream();
					ByteArrayOutputStream bodyOutViaBufferArray = new ByteArrayOutputStream();
					ByteArrayOutputStream bodyOutViaInputStream = new ByteArrayOutputStream();

					{
						MIMEStream mimeStream = MIMEStream.newStreamForRead(note, "body");
						mimeStream.readInto(bodyOutViaOutputStream);
						System.out.println("Received "+bodyOutViaOutputStream.size()+" bytes in OutputStream");
						
//						Files.write(tmpFile1, bodyOutViaOutputStream.toByteArray());
					}
					
					System.out.println("******************************");
					
					{
						byte[] buf = new byte[3000000];
						int len;
						int totalRead = 0;

						MIMEStream mimeStream = MIMEStream.newStreamForRead(note, "body");
						while ((len = mimeStream.readInto(buf))>0) {
							bodyOutViaBufferArray.write(buf, 0, len);
							totalRead += len;
							System.out.println("Received "+len+" bytes from InputStream, total: "+totalRead);
						}
						
//						Files.write(tmpFile2, bodyOutViaInputStream.toByteArray());
					}
					
					{
						byte[] buf = new byte[3000000];
						int len;

						try (InputStream bodyIn = MIMEStream.getMIMEAsInputStream(note, "body");) {
							int totalRead = 0;
							while ((len = bodyIn.read(buf))>0) {
								bodyOutViaInputStream.write(buf, 0, len);
								totalRead += len;
								System.out.println("Received "+len+" bytes from InputStream, total: "+totalRead);
							}
						}

//						Files.write(tmpFile2, bodyOutViaInputStream.toByteArray());
					}

					assertArrayEquals(bodyOutViaOutputStream.toByteArray(), bodyOutViaBufferArray.toByteArray());
					assertArrayEquals(bodyOutViaOutputStream.toByteArray(), bodyOutViaInputStream.toByteArray());

				});

				return null;
			}
		});
	}

}
