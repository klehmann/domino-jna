package com.mindoo.domino.jna.test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.TextStyle;
import com.mindoo.domino.jna.richtext.TextStyle.Justify;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Test cases that read and write richtext data (items of type TYPE_COMPOSITE)
 * 
 * @author Karsten Lehmann
 */
public class TestRichTextCreationAndReading extends BaseJNATestClass {

	@Test
	public void testViewMetaData_defaultCollection() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//sample only works when run against the Notes Client
				NotesDatabase db = NotesDatabase.openMailDatabase();
				
				//create a note with some richtext
				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Memo");
				
				RichTextBuilder rt = note.createRichTextItem("Body");
				
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
				
				rt.close();
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
	
}