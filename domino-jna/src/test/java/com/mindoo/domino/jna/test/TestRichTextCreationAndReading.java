package com.mindoo.domino.jna.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.IAttachmentProducer;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.RichTextUtils;
import com.mindoo.domino.jna.richtext.TextStyle;
import com.mindoo.domino.jna.richtext.TextStyle.Justify;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;

import lotus.domino.Session;

/**
 * Test cases that read and write richtext data (items of type TYPE_COMPOSITE)
 * 
 * @author Karsten Lehmann
 */
public class TestRichTextCreationAndReading extends BaseJNATestClass {

	/**
	 * Creates a new document in the mail database with an embedded file in the
	 * body richtext item.
	 */
	@Test
	public void testAddFileToRichtext() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//sample only works when run against the Notes Client
				NotesDatabase db = NotesDatabase.openMailDatabase();

				//create a note with some richtext
				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Memo");


				// use this syntax to attach a file from disk:
				// attachFile(String filePathOnDisk, String uniqueFileNameInNote, Compression compression)
				// note.attachFile("/tmp/testfile.txt", "testfile.txt", Compression.NONE);

				// or use the IAttachmentProducer to generate the file data on the fly:
				IAttachmentProducer attProducer = new IAttachmentProducer() {

					@Override
					public void produceAttachment(OutputStream out) throws IOException {
						String txt = "Hello. This is a testfile.";
						out.write(txt.getBytes(Charset.forName("UTF-8")));
					}

					@Override
					public int getSizeEstimation() {
						//returning a value here would help allocating the right binary object
						//in the database, but it's not required (it's auto-growing)
						return -1;
					}
				};

				NotesTimeDate fileCreated = NotesTimeDate.adjustedFromNow(0, 0, 5, 0, 0, 0);
				NotesTimeDate fileModified = NotesTimeDate.adjustedFromNow(0, 0, 2, 0, 0, 0);
				NotesAttachment att = note.attachFile(attProducer, "testfile.txt", fileCreated.toDate(), fileModified.toDate());

				try (RichTextBuilder rt = note.createRichTextItem("Body");) {
					rt
					.addText("Here is the file:\n")
					.addFileHotspot(att, "captiontext for testfile.txt");

					// use this for more advanced options, e.g. use a different file icon and caption color/text placement:
					// rt.addFileHotspot(attachment, filenameToDisplay, captionText, captionStyle, captionPos, captionColorRed, captionColorGreen, captionColorBlue, resizeToWidth, resizeToHeight, fileSize, imageData);

					// code richtext item
				}
				
				// save document
				note.update();

				System.out.println("Document created with Notes URL Notes://"+
						NotesNamingUtils.toCommonName(db.getServer())+"/"+
						db.getReplicaID()+"/0/"+note.getUNID());

				// use this syntax to modify existing richtext items;
				// these converters rewrite the richtext item only if they find any matches, e.g. a
				// file icon for the specfied attachment's unique name

				// note.convertRichTextItem("Body", new AppendFileHotspotConversion(att, "filename.txt"));
				// note.convertRichTextItem("Body", new RemoveFileHotspotConversion(att));

				return null;
			}
		});
	}

	@Test
	public void testRichtextAddPNG() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//sample only works when run against the Notes Client
				NotesDatabase db = NotesDatabase.openMailDatabase();

				//create a note with some richtext
				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Memo");

				try (RichTextBuilder rt = note.createRichTextItem("Body")) {
					//add some multiline text
					rt.addText("Line 1\nLine2\n");

					//add another paragraph with formatted text
					TextStyle txtStyle = new TextStyle("Test").setAlign(Justify.RIGHT);
					FontStyle fontStyle = new FontStyle().setItalic(true);
					rt.addText("Formatted text", txtStyle, fontStyle);

					//now add a PNG image (that's not possible in the Notes Client UI)
					String imgResourcePath = "/images/test-png-large.png";
					URL imgUrl = getClass().getResource(imgResourcePath);
					if (imgUrl==null) {
						throw new FileNotFoundException("Image resource not found at "+imgResourcePath);
					}
					URLConnection conn = imgUrl.openConnection();
					int fileSize = conn.getContentLength();

					InputStream imageStream = conn.getInputStream();
					try {
						rt.addImage(fileSize, imageStream);
					}
					finally {
						imageStream.close();
					}
				}

				note.update();
				System.out.println("Document created with Notes URL Notes://"+
						NotesNamingUtils.toCommonName(db.getServer())+"/"+
						db.getReplicaID()+"/0/"+note.getUNID());


				//now read the text using the convenience method
				String allTxt = note.getRichtextContentAsText("Body");
				Assert.assertTrue("Text is not empty", allTxt.length()>0);

				//and compare with manual CD record traversal
				final StringBuilder sb = new StringBuilder();
				IRichTextNavigator rtNav = note.getRichtextNavigator("Body");
				if (rtNav.gotoFirst()) {
					do {
						if (CDRecordType.TEXT.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
							int txtLen = rtNav.getCurrentRecordDataLength() - 4;
							if (txtLen>0) {
								String txt = NotesStringUtils.fromLMBCS(rtNav.getCurrentRecordData().share(4), txtLen);
								sb.append(txt);
							}
						}
						else if (CDRecordType.PARAGRAPH.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
							sb.append("\n");
						}
					}
					while (rtNav.gotoNext());
				}

				Assert.assertEquals("Text content matches", allTxt, sb.toString());

				System.out.println("Content read from richtext item:\n"+allTxt);
				return null;
			}
		});
	}

	@Test
	public void testRichtextScanForFiles() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//sample only works when run against the Notes Client
				NotesDatabase db = NotesDatabase.openMailDatabase();

				//create a note with some richtext
				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Memo");

				LinkedHashSet<String> insertedFieldBodyItem = new LinkedHashSet<>();
				
				{
					//write richtext item 1
					try (RichTextBuilder rt = note.createRichTextItem("Body");) {

						//add some multiline text
						rt.addText("Line 1\nLine2\n");

						NotesAttachment file1 = note.attachFile(new IAttachmentProducer() {

							@Override
							public void produceAttachment(OutputStream out) throws IOException {
								out.write("test".getBytes(Charset.forName("UTF-8")));
							}

							@Override
							public int getSizeEstimation() {
								return -1;
							}
						}, "file1.txt", new Date(), new Date());

						rt.addText("More text");

						NotesAttachment file2 = note.attachFile(new IAttachmentProducer() {

							@Override
							public void produceAttachment(OutputStream out) throws IOException {
								out.write("test2".getBytes(Charset.forName("UTF-8")));
							}

							@Override
							public int getSizeEstimation() {
								return -1;
							}
						}, "file2.txt", new Date(), new Date());

						//insert in reverse order, to test if our scan results get returned in order of appearance
						rt.addFileHotspot(file2, "file2-dsp.txt");
						rt.addFileHotspot(file1, "file1-dsp.txt");

						insertedFieldBodyItem.add(file2.getFileName());
						insertedFieldBodyItem.add(file1.getFileName());

					}
				}

				LinkedHashSet<String> insertedFieldBody2Item = new LinkedHashSet<>();

				{
					//write richtext item 1
					try (RichTextBuilder rt = note.createRichTextItem("Body2");) {

						NotesAttachment file3 = note.attachFile(new IAttachmentProducer() {

							@Override
							public void produceAttachment(OutputStream out) throws IOException {
								out.write("test3".getBytes(Charset.forName("UTF-8")));
							}

							@Override
							public int getSizeEstimation() {
								return -1;
							}
						}, "file3.txt", new Date(), new Date());

						rt.addFileHotspot(file3, "file3-dsp.txt");

						insertedFieldBody2Item.add(file3.getFileName());
					}
				}

				// add a file that is not part of a richtext item
				NotesAttachment file4 = note.attachFile(new IAttachmentProducer() {

					@Override
					public void produceAttachment(OutputStream out) throws IOException {
						out.write("test4".getBytes(Charset.forName("UTF-8")));
					}

					@Override
					public int getSizeEstimation() {
						return -1;
					}
				}, "file4.txt", new Date(), new Date());


				//search for filenames in item body
				Set<String> fileNamesItem1;
				{
					IRichTextNavigator rtNav = note.getRichtextNavigator("Body");
					fileNamesItem1 = RichTextUtils.collectAttachmentNames(rtNav);

					System.out.println("Files found in richtext: "+fileNamesItem1);

					Assert.assertEquals("Correct files found in body item", insertedFieldBodyItem, fileNamesItem1);
				}

				//search for filenames in item body2
				Set<String> fileNamesItem2;
				{
					IRichTextNavigator rtNav = note.getRichtextNavigator("Body2");
					fileNamesItem2 = RichTextUtils.collectAttachmentNames(rtNav);

					System.out.println("Files found in richtext: "+fileNamesItem2);

					Assert.assertEquals("Correct files found in body2 item", insertedFieldBody2Item, fileNamesItem2);
				}
				
				//search for attachment icon filenames in all note items
				Map<String,LinkedHashSet<String>> attNamesByItem = RichTextUtils.collectAllAttachmentNamesByField(note);

				System.out.println("Found attachments hashed by their richtext item: "+attNamesByItem);
				
				Assert.assertEquals("Found attachments in 2 items", 2, attNamesByItem.size());
				Assert.assertEquals("Attachments in item body found", fileNamesItem1, attNamesByItem.get("bODy"));
				Assert.assertEquals("Attachments in item body2 found", fileNamesItem2, attNamesByItem.get("bODy2"));
				
				return null;
			}
		});

	}
}