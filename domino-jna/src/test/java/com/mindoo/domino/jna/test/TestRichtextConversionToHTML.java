package com.mindoo.domino.jna.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesAttachment.IDataCallback;
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
import com.mindoo.domino.jna.html.IHtmlApiReference;
import com.mindoo.domino.jna.html.IHtmlApiUrlTargetComponent;
import com.mindoo.domino.jna.html.IHtmlConversionResult;
import com.mindoo.domino.jna.html.IHtmlImageRef;
import com.mindoo.domino.jna.html.ReferenceType;
import com.mindoo.domino.jna.html.TargetType;

import lotus.domino.Session;

/**
 * Tests case to convert richtext as HTML including all images and files
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
				
				//not all of these options seemed to work in our tests (e.g. the FontConversion did nothing
				//in a Notes 10 client)
				EnumSet<HtmlConvertOption> convOptions = EnumSet.of(
						HtmlConvertOption.ForceOutlineExpand,
						HtmlConvertOption.ForceSectionExpand,
						HtmlConvertOption.RowAtATimeTableAlt,
						HtmlConvertOption.FontConversion,
						HtmlConvertOption.ListFidelity,
						HtmlConvertOption.TextExactSpacing);
				
				File tmpFolder = new File(System.getProperty("java.io.tmpdir"));
				File targetHtmlFileName = new File(tmpFolder, "htmlexport-"+lastMailNote.getUNID()+".html");
				
				writeRichtext(lastMailNote, "body", targetHtmlFileName, convOptions);
				
				// use null for the itemName argument to convert the document with its form:
				// writeRichtext(lastMailNote, null, targetHtmlFileName, convOptions);
				
				System.out.println("Conversion result written to:\n"+targetHtmlFileName.getAbsolutePath());
				
				return null;
			}
			
			/**
			 * Converts the richtext of a {@link NotesNote} to HTML and writes the result to disk.
			 * 
			 * @param note note to convert
			 * @param itemName name of richtext item to convert, use <code>null</code> to convert the whole note
			 * @param bodyHtmlFile target file to write HTML
			 * @param convOptions richtext conversion options
			 * @throws IOException in case of I/O errors
			 */
			private void writeRichtext(NotesNote note, String itemName, File bodyHtmlFile, EnumSet<HtmlConvertOption> convOptions) throws IOException {
				File parentOutputFolder = bodyHtmlFile.getParentFile();
				
				if (bodyHtmlFile.exists()) {
					if (!bodyHtmlFile.delete()) {
						throw new IOException("Unable to overwrite HTML file "+bodyHtmlFile.getAbsolutePath());
					}
				}
				
				IHtmlConversionResult convResult;
				if (itemName==null) {
					//convert the specified item to HTML format
					convResult = note.convertNoteToHtml(convOptions);
				}
				else {
					//convert the whole note with its form to HTML format
					convResult = note.convertItemToHtml(itemName, convOptions);
				}

				//get body content as HTML:
				String bodyAsHTML = convResult.getText();

				//now process all contained images
				List<IHtmlImageRef> images = convResult.getImages();

				
				int imgCount = 0;
				for (IHtmlImageRef currImageRef : images) {
					imgCount++;
					
					String refText = currImageRef.getReferenceText();

					File imageFile = new File(parentOutputFolder, "htmlexport-"+note.getUNID()+"-img-"+imgCount+".jpg");
					
					//extract image data to disk
					writeImage(note, currImageRef, imageFile);
					
					//and replace the <img src=..."> value with the name of the just written image file
					bodyAsHTML = bodyAsHTML.replace(refText, imageFile.getName());
				}

				//collect all links to files, e.g. /mail%5Cklehmann.nsf/0/6cd002863447d326c125846b002618ba/$FILE/testfile.txt,
				//let the HTML API extract the filename part and and hash them by their full reference txt
				
				Map<String,String> fileNamesByRefTxt = new HashMap<String,String>();
				
				List<IHtmlApiReference> allRefs = convResult.getReferences();
				for (IHtmlApiReference currRef : allRefs) {
					if (currRef.getType() == ReferenceType.HREF) {
						String currRefTxt = currRef.getReferenceText();
						List<IHtmlApiUrlTargetComponent<?>> targets = currRef.getTargets();
						
						String fileName = null;
						
						//targets contain all separate parts of the URL, e.g. the database name; we scan
						//the targets list to find the extracted filename
						for (IHtmlApiUrlTargetComponent<?> currTarget : targets) {
							if (currTarget.getType() == TargetType.FILENAME && String.class.equals(currTarget.getValueClass())) {
								//found the filename part of the url
								fileName = (String) currTarget.getValue();
								break;
							}
						}
						
						if (fileName!=null) {
							fileNamesByRefTxt.put(currRefTxt, fileName);
						}
					}
				}
				
				//now try to find matching files in the document
				File tmpFolder = bodyHtmlFile.getParentFile();
				
				for (Entry<String,String> currEntry : fileNamesByRefTxt.entrySet()) {
					String currRefTxt = currEntry.getKey();
					String currFileName = currEntry.getValue();
					
					NotesAttachment att = note.getAttachment(currFileName);
					if (att!=null) {
						File targetFile = new File(tmpFolder, "htmlexport-"+note.getUNID()+"-file-"+currFileName);
						if (targetFile.exists()) {
							if (!targetFile.delete()) {
								throw new IOException("Error overwriting existing attachment "+targetFile.getAbsolutePath());
							}
						}
						
						extractAttachment(att, targetFile);
						
						//replace file link in HTML
						bodyAsHTML = bodyAsHTML.replace(currRefTxt, targetFile.getName());
					}
				}
				
				if (!bodyAsHTML.startsWith("<html")) {
					//add a HTML header to define that the file was written with UTF-8 encoding
					bodyAsHTML = "<html><head><meta charset=\"utf-8\"></head><body>" + bodyAsHTML + "</body></html>";
				}
				
				//as last step, write the modified HTML content to disk
				FileOutputStream fBodyOut = new FileOutputStream(bodyHtmlFile);
				try {
					fBodyOut.write(bodyAsHTML.getBytes(Charset.forName("UTF-8")));
				}
				finally {
					fBodyOut.close();
				}
			}
			
			/**
			 * Writes a {@link NotesAttachment} to disk
			 * 
			 * @param att attachment
			 * @param targetFile target file
			 * @throws IOException in case of I/O errors
			 */
			private void extractAttachment(NotesAttachment att, File targetFile) throws IOException {
				if (targetFile.exists()) {
					if (!targetFile.delete()) {
						throw new IOException("Cannot overwrite file "+targetFile.getAbsolutePath()+" with document attachment");
					}
				}
				
				final FileOutputStream fOut = new FileOutputStream(targetFile);
				try {
					final IOException[] ex = new IOException[1];
					
					att.readData(new IDataCallback() {

						@Override
						public Action read(byte[] data) {
							try {
								fOut.write(data);
							} catch (IOException e) {
								ex[0] = e;
								return Action.Stop;
							}
							return Action.Continue;
						}
					});
					
					if (ex[0]!=null) {
						throw ex[0];
					}
				}
				finally {
					fOut.close();
				}
			}
			
			/**
			 * Writes an embedded image to disk
			 * 
			 * @param note parent note
			 * @param imageFile target image file
			 * @param currImage image reference
			 * @throws IOException
			 */
			private void writeImage(NotesNote note, IHtmlImageRef currImage, File imageFile) throws IOException {
				if (imageFile.exists()) {
					if (!imageFile.delete()) {
						throw new IOException("Error overwriting image file at "+imageFile.getAbsolutePath());
					}
				}
				
				final FileOutputStream fOut = new FileOutputStream(imageFile);
				
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
				}
				finally {
					fOut.close();
				}
			}
		});
	}

}
