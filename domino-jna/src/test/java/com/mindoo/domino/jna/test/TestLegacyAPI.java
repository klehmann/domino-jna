package com.mindoo.domino.jna.test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Tests cases for legacy API
 * 
 * @author Karsten Lehmann
 */
public class TestLegacyAPI extends BaseJNATestClass {

	/**
	 * Tests conversion functions from legacy API database and document to JNA API objects
	 */
	@Test
	public void testLegacyAPI_createSession() {
		final Session[] sessionAsFakeUser = new Session[1];

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//we create a new session for a fake user
				sessionAsFakeUser[0] = LegacyAPIUtils.createSessionAs(Arrays.asList(
						"CN=Test User/O=Mindoo",
						"Group1"
						), EnumSet.of(Privileges.FullAdminAccess));

				Database legacyDbAsFakeUser = sessionAsFakeUser[0].getDatabase("", BaseJNATestClass.DBPATH_FAKENAMES_NSF);
				Document docTmp = legacyDbAsFakeUser.createDocument();
				Vector<?> userNamesList = sessionAsFakeUser[0].evaluate("@UserNamesList", docTmp);
				docTmp.recycle();
				
				Assert.assertTrue("Usernameslist of session contains specified user", userNamesList.contains("CN=Test User/O=Mindoo"));
				Assert.assertTrue("Usernameslist of session contains specified group", userNamesList.contains("Group1"));
				
				NotesDatabase notesDatabaseForlegacyDbAsFakeUser = LegacyAPIUtils.toNotesDatabase(legacyDbAsFakeUser);
				
				String absFilePath = notesDatabaseForlegacyDbAsFakeUser.getAbsoluteFilePathOnLocal();
				String relFilePath = notesDatabaseForlegacyDbAsFakeUser.getRelativeFilePath();
				
				System.out.println("Absolute filePath: "+absFilePath);
				System.out.println("Relative filePath: "+relFilePath);
				
				Assert.assertEquals("Filepaths are equal", legacyDbAsFakeUser.getFilePath(), relFilePath);
				
				//wrap a new legacy document into a NotesNote and check whether both point to the
				//same memory area
				Document docTest = legacyDbAsFakeUser.createDocument();
				NotesNote noteForDocTest = LegacyAPIUtils.toNotesNote(docTest);
				docTest.replaceItemValue("field1", "abc");
				String testValue = noteForDocTest.getItemValueString("field1");
				
				Assert.assertEquals("Document and NotesNote point to the same memory",  "abc", testValue);
				
				return null;
			}
		});

		boolean isRecycled = false;
		try {
			sessionAsFakeUser[0].isOnServer();
		}
		catch (NotesException e) {
			if (e.id==4376 || e.id==4466)
				isRecycled = true;
		}
		
		Assert.assertTrue("Legacy session has been recycled by auto GC", isRecycled);
	}
}
