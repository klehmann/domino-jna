package com.mindoo.domino.jna.mime;

import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.mime.MimeConversionControl.MessageContentEncoding;
import com.mindoo.domino.jna.mime.attachments.ByteArrayMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.UrlMimeAttachment;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.TextStyle;
import com.mindoo.domino.jna.richtext.TextStyle.Justify;

import lotus.domino.Session;

public class TestJakartaMimeDataReadWrite extends BaseJNATestClass {
	private static final String TEST_IMAGE_PATH = "/images/test-png-large.png";;

	private byte[] produceTestData(int size) {
		byte[] data = new byte[size];

		int offset = 0;

		while (offset < size) {
			for (char c='A'; c<='Z' && offset<size; c++) {
				data[offset++] = (byte) (c & 0xff);
			}
		}

		return data;
	}

	/**
	 * We check how the API behaves when reading {@link MIMEData} from
	 * non MIME_PART items
	 * 
	 * @throws Exception in case of errors
	 */
	@Test
	public void testReadInvalidMimeData() throws Exception {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbFakenames = getFakeNamesDb();
				NotesNote note = dbFakenames.createNote();

				MIMEData mimeData = NotesMimeUtils.getMimeData(note, "ItemDoesNotExist");
				Assert.assertNull(mimeData);

				note.replaceItemValue("TextItem", "Hello world.");

				{
					//using the MIME stream C API on a non MIME_PART item should throw an error
					NotesError ex = null;
					try {
						NotesMimeUtils.getMimeData(note, "TextItem");
					}
					catch (NotesError e) {
						ex = e;
					}
					Assert.assertNotNull(ex);
					Assert.assertTrue(ex.getId() == 1184); // "MIME part not found"
				}

				//now we create formatted richtext content
				try (RichTextBuilder rtBuilder = note.createRichTextItem("Body");) {
					rtBuilder.addText("Hello World.",
							new TextStyle("MyStyle").setAlign(Justify.RIGHT),
							new FontStyle().setBold(true));
				}

				NotesItem itm = note.getFirstItem("Body");
				Assert.assertNotNull(itm);
				//make sure it's richtext
				Assert.assertEquals(itm.getType(), NotesItem.TYPE_COMPOSITE);

				{
					//should also throw an error, still no MIME_PART item
					NotesError ex = null;
					try {
						NotesMimeUtils.getMimeData(note, "Body");
					}
					catch (NotesError e) {
						ex = e;
					}
					Assert.assertNotNull(ex);
					Assert.assertTrue(ex.getId() == 1184); // "MIME part not found"
				}

				//now we convert the richtext item to mime
				note.convertToMime(
						new MimeConversionControl()
						.setDefaults()
						.setMessageContentEncoding(MessageContentEncoding.TEXT_PLAIN_AND_HTML_WITH_IMAGES_ATTACHMENTS));

				//make sure it's really MIME_PART
				itm = note.getFirstItem("Body");
				Assert.assertNotNull(itm);
				Assert.assertEquals(itm.getType(), NotesItem.TYPE_MIME_PART);

				MIMEData mimeDataFromRichText = NotesMimeUtils.getMimeData(note, "Body");
				Assert.assertNotNull(mimeDataFromRichText);

				// <html><body><div align="right"><font size="1" face="serif"><b>Hello World.</b></font></div></body></html>
				String html = mimeDataFromRichText.getHtml();
				//                                                                Hello World.
				String text = mimeDataFromRichText.getPlainText();

				Assert.assertTrue(html.length()>0);
				Assert.assertTrue(text.length()>0);

				return null;
			}
		});
	}

	@Test
	public void testWriteReadMimeData() throws Exception {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbFakenames = getFakeNamesDb();

				//compose the MIME data to write:
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

				//and write it to a temp document
				NotesNote note = dbFakenames.createNote();
				note.replaceItemValue("BodyMime", writtenMimeData);

				//now read the MIME data and compare it with what we have just written
				MIMEData checkMimeData = NotesMimeUtils.getMimeData(note, "BodyMime");
				Assert.assertNotNull("MIMEItemData not null", checkMimeData);

				String checkHtml = checkMimeData.getHtml();
				Assert.assertEquals(html, checkHtml);
				String checkPlainText = checkMimeData.getPlainText();
				Assert.assertEquals(plainText, checkPlainText);

				//check that no embed has been removed
				for (String currWrittenCid : writtenMimeData.getContentIds()) {
					IMimeAttachment checkEmbed = checkMimeData.getEmbed(currWrittenCid);

					Assert.assertNotNull("Embed with cid "+currWrittenCid+" not null", checkEmbed);
				}

				//check that no embed has been added
				for (String currCheckCid : checkMimeData.getContentIds()) {
					IMimeAttachment currEmbed = writtenMimeData.getEmbed(currCheckCid);

					Assert.assertNotNull("Embed with cid "+currCheckCid+" not null", currEmbed);
				}

				List<IMimeAttachment> writtenAttachments = writtenMimeData.getAttachments();
				List<IMimeAttachment> checkAttachments = checkMimeData.getAttachments();

				//check that no attachment has been removed
				for (IMimeAttachment currAtt : writtenAttachments) {
					String currFilename = currAtt.getFileName();

					IMimeAttachment checkAtt = null;
					for (IMimeAttachment currCheckAtt : checkAttachments) {
						if (currFilename.equals(currCheckAtt.getFileName())) {
							checkAtt = currCheckAtt;
							break;
						}
					}

					Assert.assertNotNull("file "+currFilename+" could be found", checkAtt);
				}

				//check that no attachment has been added
				for (IMimeAttachment currCheckAtt : checkAttachments) {
					String currFilename = currCheckAtt.getFileName();

					IMimeAttachment writtenAtt = null;
					for (IMimeAttachment currWrittenAtt : writtenAttachments) {
						if (currFilename.equals(currWrittenAtt.getFileName())) {
							writtenAtt = currCheckAtt;
							break;
						}
					}

					Assert.assertNotNull("file "+currFilename+" could be found", writtenAtt);
				}
				return null;
			}
		});

	}
}
