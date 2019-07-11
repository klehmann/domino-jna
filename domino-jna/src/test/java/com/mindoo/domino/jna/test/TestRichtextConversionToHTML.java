package com.mindoo.domino.jna.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollectionPosition;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.NotesViewLookupResultData;
import com.mindoo.domino.jna.NotesNote.HtmlConvertOption;
import com.mindoo.domino.jna.NotesNote.IHtmlItemImageConversionCallback;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.html.IHtmlConversionResult;
import com.mindoo.domino.jna.html.IHtmlImageRef;

import lotus.domino.Session;

/**
 * Tests case to convert richtext as HTML including all images
 * 
 * @author Karsten Lehmann
 */
public class TestRichtextConversionToHTML extends BaseJNATestClass {

	@Test
	public void testSignDb() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//open the Notes Client user's mail database
				NotesDatabase db = NotesDatabase.openMailDatabase();
				if (db==null) {
					//test only working in the Notes Client, because openMailDatabase reads the mail db location from
					//the Notes.ini
					return null;
				}
				//open the inbox
				NotesCollection inboxFolder = db.openCollectionByName("($Inbox)");
				
				//skip all entries to read just the last one
				NotesViewLookupResultData readLastEntryLkResult =
						inboxFolder.readEntries(new NotesCollectionPosition("0"),
						EnumSet.of(Navigate.NEXT, Navigate.CONTINUE), Integer.MAX_VALUE, EnumSet.of(Navigate.CURRENT),
						1, EnumSet.of(ReadMask.NOTEID));
				
				List<NotesViewEntryData> entries = readLastEntryLkResult.getEntries();
				if (entries.isEmpty()) {
					System.out.println("Could not find any mail in the mail db "+db.getServer()+"!!"+db.getRelativeFilePath()+
							" view ($Inbox) to convert");
					return null;
				}

				NotesViewEntryData lastMail = entries.get(0);
				NotesNote lastMailNote = db.openNoteById(lastMail.getNoteId());

				System.out.println("Converting mail with UNID "+lastMailNote.getUNID()+" and subject "+
						lastMailNote.getItemValueString("Subject"));
				
				//convert the Body item to HTML format
				IHtmlConversionResult convResult = lastMailNote.convertItemToHtml("Body",
						EnumSet.of(HtmlConvertOption.ForceOutlineExpand, HtmlConvertOption.ForceSectionExpand));

				File bodyHtmlFile = File.createTempFile("htmlexport-"+lastMailNote.getUNID()+"-", ".html");
				//get body content as HTML:
				String bodyAsHTML = convResult.getText();

				//now process all contained images
				List<IHtmlImageRef> images = convResult.getImages();

				for (IHtmlImageRef currImage : images) {
					String refText = currImage.getReferenceText();

					//extract image data to disk
					File imgFile = writeImage(lastMailNote, currImage);
					
					//and replace the <img src=..."> value with the name of the just written image file
					bodyAsHTML = bodyAsHTML.replace(refText, imgFile.getName());
				}

				//as last step, write the modified HTML content to disk
				FileOutputStream fBodyOut = new FileOutputStream(bodyHtmlFile);
				try {
					fBodyOut.write(bodyAsHTML.getBytes(Charset.forName("UTF-8")));
				}
				finally {
					fBodyOut.close();
				}

				System.out.println("Conversion result written to:\n"+bodyHtmlFile.getAbsolutePath());
				
				return null;
			}

			private File writeImage(NotesNote note, IHtmlImageRef currImage) throws IOException {
				File imgFile = File.createTempFile("htmlexport-"+note.getUNID()+"-", ".jpg");
				final FileOutputStream fOut = new FileOutputStream(imgFile);
				
				try {
					currImage.readImage(new IHtmlItemImageConversionCallback() {

						@Override
						public int setSize(int size) {
							//skip nothing:
							int skipBytes = 0;
							return skipBytes;
						}

						@Override
						public Action read(byte[] data) {
							try {
								fOut.write(data);
							} catch (IOException e) {
								e.printStackTrace();
								return Action.Stop;
							}
							return Action.Continue;
						}});

					return imgFile;
				}
				finally {
					fOut.close();
				}
			}
		});
	}

}
