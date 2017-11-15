package com.mindoo.domino.jna.test;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.ICDRecordCallback;
import com.mindoo.domino.jna.compoundtext.FontStyle;
import com.mindoo.domino.jna.compoundtext.RichTextBuilder;
import com.mindoo.domino.jna.compoundtext.TextStyle;
import com.mindoo.domino.jna.compoundtext.TextStyle.Justify;
import com.mindoo.domino.jna.constants.CDRecord;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;

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
				NotesDatabase db = getFakeNamesDb();
				
				//create a note with some richtext
				NotesNote note = db.createNote();
				
				RichTextBuilder rt = note.createRichTextItem("Body");
				
				//add some multiline text
				rt.addText("Line 1\nLine2\n");
				
				//add another paragraph with formatted text
				TextStyle txtStyle = new TextStyle("Test").setAlign(Justify.RIGHT);
				FontStyle fontStyle = new FontStyle().setItalic(true);
				rt.addText("Formatted text", txtStyle, fontStyle);
				rt.close();
				
				//now read the text using the convenience method
				String allTxt = note.getRichtextContentAsText("Body");
				Assert.assertTrue("Text is not empty", allTxt.length()>0);
				
				final StringBuilder sb = new StringBuilder();
				
				note.enumerateRichTextCDRecords("Body", new ICDRecordCallback() {

					@Override
					public Action recordVisited(ByteBuffer data, CDRecord parsedSignature, short signature,
							int dataLength) {
						
						if (parsedSignature == CDRecord.TEXT) {
							int txtLen = dataLength-4;
							if (txtLen>0) {
								Memory txtMem = new Memory(txtLen);
								for (int i=0; i<txtLen; i++) {
									txtMem.setByte(i, data.get(4+i));
								}
								
								String txt = NotesStringUtils.fromLMBCS(txtMem, txtLen);
								sb.append(txt);
							}
						}
						else if (parsedSignature == CDRecord.PARAGRAPH) {
							sb.append("\n");
						}
						return Action.Continue;
					}
					
				});

				Assert.assertEquals("Text content matches", allTxt, sb.toString());
				
				System.out.println("Content read from richtext item:\n"+allTxt);
				return null;
			}
		});

	}
	
}