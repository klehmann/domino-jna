package com.mindoo.domino.jna.mime.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.MimeStreamItemizeOptions;
import com.mindoo.domino.jna.constants.MimeStreamOpenOptions;
import com.mindoo.domino.jna.mime.MIMEData;
import com.mindoo.domino.jna.mime.MIMEStream;
import com.mindoo.domino.jna.mime.attachments.ByteArrayMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.UrlMimeAttachment;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

import lotus.domino.Session;

/**
 * This testcase creates a document with MIM content and then creates a 1:1
 * copy of that content in another document.
 * 
 * @author Karsten Lehmann
 */
public class TestCopyMimeStreamData extends BaseJNATestClass {
	private static final String TEST_IMAGE_PATH = "/images/test-png-large.png";

	@Test
	public void testCopyMimeData() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbMail = NotesDatabase.openMailDatabase();
				if (dbMail==null) {
					//only works in the local Notes Client
					return null;
				}
				
				NotesNote mailNoteOrig = dbMail.createNote();
				mailNoteOrig.replaceItemValue("Form", "Memo");
				mailNoteOrig.replaceItemValue("Subject", "MIME copy test, source doc");

				{
					//compose the MIME data using our simple API
					MIMEData writtenMimeData = new MIMEData();

					//embed an image via URL and remember its content id (cid)
					URL url = getClass().getResource(TEST_IMAGE_PATH);
					Assert.assertNotNull("Test image can be found: "+TEST_IMAGE_PATH, url);
					String cid = writtenMimeData.embed(new UrlMimeAttachment(url));

					//add the first test file
					int writtenAttachment1Size = 50000;
					byte[] writtenAttachment1Data = produceTestData(writtenAttachment1Size);
					writtenMimeData.attach(new ByteArrayMimeAttachment(writtenAttachment1Data, "test.txt"));

					//and the second one
					int writtenAttachment2Size = 20000;
					byte[] writtenAttachment2Data = produceTestData(writtenAttachment2Size);
					writtenMimeData.attach(new ByteArrayMimeAttachment(writtenAttachment2Data, "test2.txt"));

					//set html (with link to embedded image) and alternative plaintext
					String html = "<html><body>This is <b>formatted</b> text and an image:<br><img src=\"cid:"+cid+"\"></body></html>";
					writtenMimeData.setHtml(html);
					String plainText = "This is alternative plaintext";
					writtenMimeData.setPlainText(plainText);
					
					//and write it to a new in-memory memo document
					mailNoteOrig.replaceItemValue("Body", writtenMimeData);
				}

				//next we create the target note where we want to create the MIME data copy
				NotesNote mailNoteCopy = dbMail.createNote();
				mailNoteCopy.replaceItemValue("Form", "Memo");
				mailNoteCopy.replaceItemValue("Subject", "MIME copy test, target doc");
				
				//copy raw MIME data from mailNote to mailNoteCopy
				copyMimeData(mailNoteOrig, "body", mailNoteCopy, "body");
				
				
				//now compare the RAW MIME content of both documents
				ByteArrayOutputStream outOrig = new ByteArrayOutputStream();
				readMimeData(mailNoteOrig, "body", outOrig);
				ByteArrayOutputStream outCopy = new ByteArrayOutputStream();
				readMimeData(mailNoteCopy, "body", outCopy);

				byte[] origArr = outOrig.toByteArray();
				byte[] copyArr = outCopy.toByteArray();
				
				//and test if both MIME structures are equal

				Assert.assertArrayEquals(origArr, copyArr);

//				mailNoteOrig.update();
//				mailNoteCopy.update();
//				System.out.println("Created mail note copy: "+getNotesUrl(mailNoteCopy));
				
				return null;
			}
		});
	
	}
	
	/**
	 * Returns the MIME item data as a string
	 * 
	 * @param note note
	 * @param itemName mime item name
	 * @param out stream to receive binary MIME stream data
	 * @throws IOException
	 */
	private void readMimeData(NotesNote note, String itemName, OutputStream out) throws IOException {
		try (InputStream in = MIMEStream.getMIMEAsInputStream(note, itemName, 
				EnumSet.of(MimeStreamOpenOptions.MIME_INCLUDE_HEADERS))) {
			byte[] buffer = new byte[16384];
			int len;

			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
		}
	}
	
	/**
	 * This method uses the MIMEStream API to create a 1:1 copy of MIME data
	 * 
	 * @param sourceNote source note containing MIME data
	 * @param sourceItemName source mime item name
	 * @param targetNote target note to write the data
	 * @param targetItemName target mime item name
	 * @throws IOException in case of I/O errors
	 */
	private void copyMimeData(NotesNote sourceNote, String sourceItemName,
			NotesNote targetNote, String targetItemName) throws IOException {
		
		// commented out because of a bug in Domino JNA 0.9.41 where we got I/O errors
		// writing incomplete lines of MIME content:
		// https://github.com/klehmann/domino-jna/commit/52d0be313e836a1a8213b7490e25849b253514a0
		
//		try (Reader mimeReader =
//				MIMEStream.getMIMEReader(mailNote, "body",
//						EnumSet.of(MimeStreamOpenOptions.MIME_INCLUDE_HEADERS))) {
//			
//			MIMEStream.writeRawMIME(mailNoteCopy, "body", mimeReader, EnumSet.of(MimeStreamItemizeOptions.ITEMIZE_BODY));
//		}
		
		//our alternative approach for Domino JNA < 0.9.42 using raw byte arrays
		try (InputStream in = MIMEStream.getMIMEAsInputStream(sourceNote, sourceItemName,
						EnumSet.of(MimeStreamOpenOptions.MIME_INCLUDE_HEADERS))) {
			
			MIMEStream mimeStream = MIMEStream.newStreamForWrite(targetNote, targetItemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
			
			byte[] buf = new byte[16384];
			int len;
			while ((len = in.read(buf)) >0) {
				mimeStream.writeFrom(buf, 0, len);
			}
			
			mimeStream.itemize(EnumSet.of(MimeStreamItemizeOptions.ITEMIZE_BODY));
			mimeStream.close();
		}
	}
	
	private String getNotesUrl(NotesNote note) {
		NotesDatabase dbParent = note.getParent();
		String dbParentReplicaId = dbParent.getReplicaID();
		
		NotesCollection defaultCollection = dbParent.openDefaultCollection();
		String defaultCollectionUnid = defaultCollection.getUNID();
		defaultCollection.recycle();
		
		String dbParentServerCN = NotesNamingUtils.toCommonName(dbParent.getServer());
		
		return "Notes://" + dbParentServerCN + "/" + dbParentReplicaId + "/" + defaultCollectionUnid + "/" + note.getUNID();
	}
}
