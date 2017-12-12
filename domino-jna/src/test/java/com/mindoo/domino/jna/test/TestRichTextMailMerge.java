package com.mindoo.domino.jna.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.TextStyle;
import com.mindoo.domino.jna.richtext.conversion.SimpleMailMergeConversion;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Test cases for the richtext mail merge conversion
 * 
 * @author Karsten Lehmann
 */
public class TestRichTextMailMerge extends BaseJNATestClass {

	
	@Test
	public void testMailMerge() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();

				NotesNote note = db.createNote();
				
				String inputStr = "Text before <<Value1>>, <<value1>>,\n <<value|2>> <<\"valuE3\">> Text after";
				
				String find1 = "<<value1>>";
				String find2 = "<<value|2>>";
				String find3 = "<<\"value3\">>";
				
				String replace1 = "abc";
				String replace2 = "\"123\"";
				String replace3 = "~|~";
				
				String expectedOutputStr = "Text before abc, abc,\n \"123\" ~|~ Text after";

				RichTextBuilder rtBuilder = note.createRichTextItem("Body");
				rtBuilder.addText(inputStr, (TextStyle) null, (FontStyle) null, false);
				rtBuilder.close();
				
				String importedTxt = note.getRichtextContentAsText("Body");
				Assert.assertEquals("Text got imported without extra newlines or other changes.", inputStr, importedTxt);
				
				Map<String,String> mailMergeFromTo = new HashMap<String,String>();
				mailMergeFromTo.put(find1, replace1);
				mailMergeFromTo.put(find2, replace2);
				mailMergeFromTo.put(find3, replace3);
				
				note.convertRichTextItem("Body", new SimpleMailMergeConversion(mailMergeFromTo, true));
				
				String outputStr = note.getRichtextContentAsText("Body");
				Assert.assertEquals("Output string matches expectation: "+outputStr, expectedOutputStr, outputStr);
				return null;
			}
		});

	}
}