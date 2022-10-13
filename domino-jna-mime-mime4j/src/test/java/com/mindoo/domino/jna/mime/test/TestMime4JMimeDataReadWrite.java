package com.mindoo.domino.jna.mime.test;

import static org.junit.Assert.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.mime.MIMEData;
import com.mindoo.domino.jna.mime.MimeConversionControl;
import com.mindoo.domino.jna.mime.MimeConversionControl.MessageContentEncoding;
import com.mindoo.domino.jna.mime.NotesMimeUtils;
import com.mindoo.domino.jna.mime.attachments.ByteArrayMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.UrlMimeAttachment;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.TextStyle;
import com.mindoo.domino.jna.richtext.TextStyle.Justify;
import com.mindoo.domino.jna.utils.StringUtil;

import lotus.domino.Session;

public class TestMime4JMimeDataReadWrite extends BaseJNATestClass {
	private static final String TEST_IMAGE_PATH = "/images/test-png-large.png";;

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
	public void testMimeDataRoundtrip() throws Exception {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbFakenames = getFakeNamesDb();
				
				String timestamp = new NotesTimeDate().toString();
				
				//embed an image via URL and remember its content id (cid)
				URL url = getClass().getResource(TEST_IMAGE_PATH);
				Assert.assertNotNull("Test image can be found: "+TEST_IMAGE_PATH, url);

				List<String> mailRecipients = new ArrayList<>();
				
				String mailRecipientsEnv = System.getenv("MAILRECIPIENTS");
				if (!StringUtil.isEmpty(mailRecipientsEnv)) {
					StringTokenizer st = new StringTokenizer(mailRecipientsEnv, ",");
					while (st.hasMoreTokens()) {
						String currToken = st.nextToken().trim();
						if (!StringUtil.isEmpty(currToken)) {
							mailRecipients.add(currToken);
						}
					}
				}
				
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "Only text "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String txt = "This is plaintext";
					mimeData.setPlainText(txt);
					
					note.replaceItemValue("Body", mimeData);
					
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(txt, testMimeData.getPlainText());
					
					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "Only text with attachment "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String txt = "This is plaintext";
					mimeData.setPlainText(txt);
					
					mimeData.attach(new ByteArrayMimeAttachment(
							"Hello World".getBytes(StandardCharsets.UTF_8),
							"test.txt"));
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(txt, testMimeData.getPlainText());
					assertTrue(testMimeData.hasAttachments());
					
					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "Only HTML "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String html = "This is <b><u>HTML!</u><b>";
					mimeData.setHtml(html);
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					
					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "Only HTML with embeds "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String cid = mimeData.embed(new UrlMimeAttachment(url));
					String html = "This is <b><u>HTML!</u><b><br><img src=\"cid:"+cid+"\">";
					mimeData.setHtml(html);
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertTrue(testMimeData.hasEmbeds());
					
					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "Only HTML with attachment "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String html = "This is <b><u>HTML!</u><b>";
					mimeData.setHtml(html);
					
					mimeData.attach(new ByteArrayMimeAttachment(
							"Hello World".getBytes(StandardCharsets.UTF_8),
							"test.txt"));
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertTrue(testMimeData.hasAttachments());
					
					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "Only HTML with embeds and attachment "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String cid = mimeData.embed(new UrlMimeAttachment(url));
					String html = "This is <b><u>HTML!</u><b><br><img src=\"cid:"+cid+"\">";
					mimeData.setHtml(html);
					
					mimeData.attach(new ByteArrayMimeAttachment(
							"Hello World".getBytes(StandardCharsets.UTF_8),
							"test.txt"));
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertTrue(testMimeData.hasAttachments());

					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "HTML and plaintext "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String html = "<b>This is <b><u>HTML!</u><b></b>";
					mimeData.setHtml(html);
					String txt = "This is plaintext";
					mimeData.setPlainText(txt);
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertEquals(txt, testMimeData.getPlainText());

					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "HTML with embeds and plaintext "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String cid = mimeData.embed(new UrlMimeAttachment(url));
					String html = "This is <b><u>HTML!</u><b><br><img src=\"cid:"+cid+"\">";
					mimeData.setHtml(html);
					String txt = "This is plaintext";
					mimeData.setPlainText(txt);
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertEquals(txt, testMimeData.getPlainText());

					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "HTML and plaintext with attachment "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String html = "<b>This is <b><u>HTML!</u><b></b>";
					mimeData.setHtml(html);
					String txt = "This is plaintext";
					mimeData.setPlainText(txt);
					
					mimeData.attach(new ByteArrayMimeAttachment(
							"Hello World".getBytes(StandardCharsets.UTF_8),
							"test.txt"));
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertEquals(txt, testMimeData.getPlainText());
					assertTrue(testMimeData.hasAttachments());
					
					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
				{
					NotesNote note = dbFakenames.createNote();
					note.replaceItemValue("Subject", "HTML with embeds and plaintext with attachment "+timestamp);
					
					MIMEData mimeData = new MIMEData();
					String cid = mimeData.embed(new UrlMimeAttachment(url));
					String html = "This is <b><u>HTML!</u><b><br><img src=\"cid:"+cid+"\">";
					mimeData.setHtml(html);
					String txt = "This is plaintext";
					mimeData.setPlainText(txt);
					
					mimeData.attach(new ByteArrayMimeAttachment(
							"Hello World".getBytes(StandardCharsets.UTF_8),
							"test.txt"));
					
					note.replaceItemValue("Body", mimeData);
					
					MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
					assertEquals(html, testMimeData.getHtml());
					assertEquals(txt, testMimeData.getPlainText());
					assertTrue(testMimeData.hasAttachments());

					if (!mailRecipients.isEmpty()) {
						note.send(mailRecipients);
					}
				}
			{
				//try to add additional text data as MIME part, here embedded experience / OpenSocial data
				NotesNote note = dbFakenames.createNote();
				note.replaceItemValue("Subject", "HTML, plaintext and embedded json "+timestamp);
				
				MIMEData mimeData = new MIMEData();
				String html = "<b>This is <b><u>HTML!</u><b></b>";
				mimeData.setHtml(html);
				String txt = "This is plaintext";
				mimeData.setPlainText(txt);
				String json = "{\r\n"
						+ "  \"gadget\" : \"http://www.socialnetwork.com/embedded/commentgadget.xml\",\r\n"
						+ "  \"context\" : 123\r\n"
						+ "}";
				mimeData.setBodyContent("application/embed+json", json);
				
				note.replaceItemValue("Body", mimeData);
				
				MIMEData testMimeData = NotesMimeUtils.getMimeData(note, "Body");
				assertEquals(html, testMimeData.getHtml());
				assertEquals(txt, testMimeData.getPlainText());
				assertEquals(json, testMimeData.getBodyContent("application/embed+json").orElse(""));

				if (!mailRecipients.isEmpty()) {
					note.send(mailRecipients);
				}
			}
				
				return null;
			}
		});
	}
	
}
