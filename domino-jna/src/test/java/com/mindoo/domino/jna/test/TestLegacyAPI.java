package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.ItemType;
import com.mindoo.domino.jna.errors.NotesError;
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
	
	@Test
	public void testNoteItemValuesWithLegacyAPI() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String strVal1 = "ABCäöü";
				String strVal2 = "ABC\nDEFäöü";
				
				int iVal1 = 12212;
				
				double dVal1 = 121.213;
				
				List<String> strListVal1 = Arrays.asList("ABC", "DEF", "GHI");

				List<Number> dListVal1 = Arrays.asList(123.0, 456.0, 12.5, 7666.4345);

				long currDateTime1 = System.currentTimeMillis();
				currDateTime1 = 1000 * (currDateTime1 % 1000);
				Date dateVal1 = new Date(currDateTime1);
				
				long currDateTime2 = currDateTime1 - 30000;
				Date dateVal2 = new Date(currDateTime2);
				
				List<Date> dateListVal1 = Arrays.asList(dateVal1, dateVal2);
				
				NotesDatabase db = createTempDb();
				Database dbLegacy = null;
				
				String tmpFilePath = db.getAbsoluteFilePathOnLocal();
				NotesError deleteDbError = null;
				try {
					NotesNote note = db.createNote();
					note.replaceItemValue("strval1", strVal1);
					note.replaceItemValue("strval2", strVal2);
					
					note.replaceItemValue("ival1", iVal1);
					
					note.replaceItemValue("dval1", dVal1);
					
					note.replaceItemValue("strlistval1", strListVal1);
					
					note.replaceItemValue("dlistval1", dListVal1);
					
					note.replaceItemValue("dateval1", dateVal1);
					
					note.replaceItemValue("datelistval1", dateListVal1);
					
					note.replaceItemValue("strval2_keeplinebreaks", EnumSet.of(ItemType.KEEPLINEBREAKS), strVal2);
					
					note.update();
					String unid = note.getUNID();
					note.recycle();
					
					db.recycle();
					
					dbLegacy = session.getDatabase("", tmpFilePath, false);
					Document doc = dbLegacy.getDocumentByUNID(unid);
					
					String checkStrVal1 = doc.getItemValueString("strval1");
					assertEquals(strVal1, checkStrVal1);
					
					String checkStrVal2 = doc.getItemValueString("strval2");
					assertEquals(strVal2, checkStrVal2);
					
					int checkIVal1 = doc.getItemValueInteger("ival1");
					assertEquals(iVal1, checkIVal1);
					
					double checkDVal1 = doc.getItemValueDouble("dVal1");
					assertEquals(dVal1, checkDVal1, 0);
					
					List<Object> checkStrListVal1 = doc.getItemValue("strlistval1");
					assertEquals(strListVal1, checkStrListVal1);
					
					List<Object> checkDListVal1 = doc.getItemValue("dlistval1");
					assertEquals(dListVal1, checkDListVal1);
					
					List<Object> checkDateVal1 = doc.getItemValueDateTimeArray("dateval1");
					assertEquals(dateVal1, ((lotus.domino.DateTime)checkDateVal1.get(0)).toJavaDate());

					List<Date> checkDateListVal1 = ((Vector<lotus.domino.DateTime>)doc.getItemValueDateTimeArray("datelistval1"))
							.stream()
							.map((val) -> { try {
								return val.toJavaDate();
							} catch (NotesException e) {
								throw new RuntimeException(e);
							} })
							.collect(Collectors.toList());
					assertEquals(dateListVal1, checkDateListVal1);
					
					String checkStrVal2KeepLineBreaks = doc.getItemValueString("strval2_keeplinebreaks");
					assertEquals(strVal2, checkStrVal2KeepLineBreaks);
					
				}
				finally {
					if (!db.isRecycled()) {
						db.recycle();
					}
					if (dbLegacy!=null) {
						dbLegacy.recycle();
					}
					
					try {
						NotesDatabase.deleteDatabase("", tmpFilePath);
					}
					catch (NotesError e) {
						deleteDbError = e;
					}
				}
				
				if (deleteDbError!=null) {
					throw deleteDbError;
				}

				return null;
			}
		});
		
	}
}
