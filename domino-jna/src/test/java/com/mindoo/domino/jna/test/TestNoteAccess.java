package com.mindoo.domino.jna.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesAttachment.IDataCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.IItemCallback;
import com.mindoo.domino.jna.constants.OpenNote;

import lotus.domino.ACL;
import lotus.domino.ACLEntry;
import lotus.domino.Database;
import lotus.domino.DateRange;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.RichTextItem;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * Tests cases for note access
 * 
 * @author Karsten Lehmann
 */
public class TestNoteAccess extends BaseJNATestClass {

	/**
	 * The test case opens the database as a user that has read access to
	 * the database and checks whether the NOTE_FLAG_READONLY is set
	 * in the note info accordingly (which indicates that the current user
	 * is not allowed to edit the note).
	 */
	@Test
	public void testNoteAccess_readOnlyCheck() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				final String userNameReadOnly = "CN=ReadOnly User/O=Mindoo";

				Database dbLegacyAPI = session.getDatabase("", "fakenames.nsf");
				
				boolean aclModified = false;
				
				//tweak the ACL so that the fake user has read access
				ACL acl = dbLegacyAPI.getACL();
				ACLEntry readOnlyEntry = acl.getEntry(userNameReadOnly);
				if (readOnlyEntry==null) {
					readOnlyEntry = acl.createACLEntry(userNameReadOnly, ACL.LEVEL_READER);
					aclModified = true;
				}
				else {
					if (readOnlyEntry.getLevel() != ACL.LEVEL_READER) {
						readOnlyEntry.setLevel(ACL.LEVEL_READER);
						aclModified = true;
					}
				}
				
				if (aclModified) {
					acl.save();
				}
				
				NotesDatabase dbData = new NotesDatabase(getSession(), "", "fakenames.nsf", userNameReadOnly);

				View peopleView = dbLegacyAPI.getView("People");
				peopleView.refresh();
				
				//find document with Umlaut values
				Document doc = peopleView.getDocumentByKey("Umlaut", false);

				NotesNote note = dbData.openNoteById(Integer.parseInt(doc.getNoteID(), 16),
						EnumSet.noneOf(OpenNote.class));

				//check if read-only flag is set as expected
				Assert.assertTrue("The note is read-only for "+userNameReadOnly, note.isReadOnly());
				
				return null;
			}
		});
	}
	
	/**
	 * Various checks to make sure that item values and attachments are read
	 * correctly and that the API handles special characters like Umlauts and
	 * newlines the right way.<br>
	 * The method also contains code to check direct attachment streaming functionality.
	 */
	@Test
	public void testNoteAccess_readItems() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting note access test");
				
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				View peopleView = dbLegacyAPI.getView("People");
				peopleView.refresh();
				
				//find document with Umlaut values
				Document doc = peopleView.getDocumentByKey("Umlaut", false);

				//add some fields to check for all data types if missing
				boolean docModified = false;
				
				if (!doc.hasItem("MyNumberList")) {
					Vector<Double> numberValues = new Vector<Double>(Arrays.asList(1.5, 4.5, 9.8, 13.3));
					doc.replaceItemValue("MyNumberList", numberValues);
					docModified=true;
				}

				if (!doc.hasItem("MyDateRange")) {
					DateTime fromDate = session.createDateTime(new Date());
					fromDate.adjustDay(-5);
					DateTime toDate = session.createDateTime(new Date());
					toDate.adjustDay(10);
					
					DateRange range = session.createDateRange(fromDate, toDate);
					doc.replaceItemValue("MyDateRange", range);
					docModified=true;
				}

				if (!doc.hasItem("MyTextWithLineBreak")) {
					doc.replaceItemValue("MyTextWithLineBreak", "line1\nline2\nline3");
					docModified=true;
				}

				if (!doc.hasItem("MyTextListWithLineBreak")) {
					doc.replaceItemValue("MyTextListWithLineBreak", new Vector<String>(Arrays.asList(
							"#1 line1\nline2\nline3",
							"#2 line1\nline2\nline3"
							)));
					docModified=true;
				}

				final int TEST_FILE_SIZE = 1 * 1024 * 1024;
				final String ITEMNAME_FILES = "rt_Files";
				
				//add some attachments
				Vector<?> attachmentNames = session.evaluate("@AttachmentNames", doc);
				if (attachmentNames==null || attachmentNames.size()==0 || (attachmentNames.size()==1 && "".equals(attachmentNames.get(0)))) {
					Item itm = doc.getFirstItem(ITEMNAME_FILES);
					RichTextItem itmFiles=null;
					if (itm==null) {
						itmFiles = doc.createRichTextItem(ITEMNAME_FILES);
					}
					else if (itm!=null && !(itm instanceof RichTextItem)) {
						itm.remove();
						itmFiles = doc.createRichTextItem(ITEMNAME_FILES);
					}
					else {
						itmFiles = (RichTextItem) itm;
					}
					
					for (int i=0; i<3; i++) {
						File currFile = File.createTempFile("test", ".bin");
						
						FileOutputStream fOut = new FileOutputStream(currFile);
						
						for (int j=0; j<TEST_FILE_SIZE; j++) {
							fOut.write(j % 255);
						}
						fOut.flush();
						fOut.close();
						
						itmFiles.embedObject(EmbeddedObject.EMBED_ATTACHMENT, "", currFile.getAbsolutePath(), currFile.getName());
					}
					docModified=true;
				}
				
//				if (!doc.hasItem("MyDateRangeList")) {
//					Vector<DateRange> dateRangeValues = new Vector<DateRange>();
//					{
//						DateTime fromDate = session.createDateTime(new Date());
//						fromDate.adjustDay(-5);
//						DateTime toDate = session.createDateTime(new Date());
//						toDate.adjustDay(10);
//						DateRange range = session.createDateRange(fromDate, toDate);
//						dateRangeValues.add(range);
//					}
//					{
//						DateTime fromDate = session.createDateTime(new Date());
//						fromDate.adjustDay(-30);
//						DateTime toDate = session.createDateTime(new Date());
//						toDate.adjustDay(20);
//						DateRange range = session.createDateRange(fromDate, toDate);
//						dateRangeValues.add(range);
//					}
//					
//					doc.replaceItemValue("MyDateRangeList", dateRangeValues);
//					docModified=true;
//				}

				if (docModified) {
					doc.save(true, false);
				}
				
				NotesNote note = dbData.openNoteById(Integer.parseInt(doc.getNoteID(), 16),
						EnumSet.noneOf(OpenNote.class));
				
				note.getItems("$file", new IItemCallback() {

					@Override
					public void itemNotFound() {
						System.out.println("Item not found");
					}

					@Override
					public Action itemFound(NotesItem itemInfo) {
						if (itemInfo.getType() == NotesItem.TYPE_OBJECT) {
							List<Object> values = itemInfo.getValues();
							if (!values.isEmpty()) {
								Object o = values.get(0);
								if (o instanceof NotesAttachment) {
									NotesAttachment att = (NotesAttachment) o;
									final AtomicInteger length = new AtomicInteger();
									
									System.out.println("Reading file "+att.getFileName());
									
									att.readData(new IDataCallback() {

										@Override
										public Action read(byte[] data) {
											length.addAndGet(data.length);
											return Action.Continue;
										}
									});
									System.out.println("Done reading file, size="+length.get());
									Assert.assertEquals("Length correct", length.get(), TEST_FILE_SIZE);
								}
							}
						}
						return Action.Continue;
					}
				});
				
				
				{
					//check text items
					String[] textItems = new String[] {"Firstname", "Lastname", "MyTextWithLineBreak"};
//					String[] textItems = new String[] {"MyTextWithLineBreak"};
					
					for (String currItem : textItems) {
						String currItemValueLegacy = doc.getItemValueString(currItem);
						String currItemValueJNA = note.getItemValueString(currItem);
						List<Object> currItemValueJNAGeneric = note.getItemValue(currItem);
						
						Assert.assertNotNull("JNA text value not null for item "+currItem, currItemValueJNA);
						Assert.assertNotNull("JNA generic text value not null for item "+currItem, currItemValueJNAGeneric);
						
						Assert.assertEquals("JNA generic text value has one element for item "+currItem, currItemValueJNAGeneric.size(), 1);
						Assert.assertEquals("JNA generic text value has correct value for item "+currItem, currItemValueLegacy, currItemValueJNAGeneric.get(0));
						
						Assert.assertEquals("JNA text value is correct for item "+currItem, currItemValueLegacy, currItemValueJNA);
					}
				}

				{
					//decode text list item
					String[] textListItems = new String[] {"$UpdatedBy", "MyTextListWithLineBreak"};
//					String[] textListItems = new String[] {"MyTextListWithLineBreak"};
					
					for (String currItem : textListItems) {
						Vector<?> currItemValueLegacy = doc.getItemValue(currItem);
						List<String> currItemValueJNA = note.getItemValueStringList(currItem);
						List<Object> currItemValueGeneric = note.getItemValue(currItem);
						
						Assert.assertNotNull("JNA textlist value not null for item "+currItem, currItemValueJNA);
						Assert.assertNotNull("JNA generic textlist value not null for item "+currItem, currItemValueGeneric);

						Assert.assertEquals("JNA textlist has correct size", currItemValueLegacy.size(), currItemValueJNA.size());
						Assert.assertEquals("JNA generic text list has correct size", currItemValueLegacy.size(), currItemValueGeneric.size());
						
						Assert.assertArrayEquals("JNA textlist has correct content", currItemValueLegacy.toArray(new Object[currItemValueLegacy.size()]), currItemValueJNA.toArray(new Object[currItemValueJNA.size()]));
						Assert.assertArrayEquals("JNA generic textlist has correct content", currItemValueLegacy.toArray(new Object[currItemValueLegacy.size()]), currItemValueGeneric.toArray(new Object[currItemValueJNA.size()]));
					}
				}
				
				{
					//decode number item
					String[] numberItems = new String[] {"RoamCleanPer"};
					
					for (String currItem : numberItems) {
						double currItemValueLegacy = doc.getItemValueDouble(currItem);
						double currItemValueJNA = note.getItemValueDouble(currItem);
						List<Object> currItemValueJNAGeneric = note.getItemValue(currItem);

						Assert.assertNotNull("JNA generic number value not null for item "+currItem, currItemValueJNAGeneric);
						
						Assert.assertEquals("JNA generic number value has one element for item "+currItem, currItemValueJNAGeneric.size(), 1);
						Assert.assertEquals("JNA generic number value has correct value for item "+currItem, Double.valueOf(currItemValueLegacy), currItemValueJNAGeneric.get(0));
						
						Assert.assertEquals("JNA number value is correct for item "+currItem, currItemValueLegacy, currItemValueJNA, 0);
					}
				}
				
				{
					String[] dateItems = new String[] {"HTTPPasswordChangeDate"};
					
					for (String currItem : dateItems) {
						Vector<?> currItemValueLegacy = doc.getItemValueDateTimeArray(currItem);
						Vector<Calendar> convertedLegacyValues = new Vector<Calendar>(currItemValueLegacy.size());
						if (currItemValueLegacy!=null && !currItemValueLegacy.isEmpty()) {
							for (int i=0; i<currItemValueLegacy.size(); i++) {
								Object currObj = currItemValueLegacy.get(i);
								if (currObj instanceof DateTime) {
									DateTime currDateTime = (DateTime) currObj;
									Calendar cal = Calendar.getInstance();
									cal.setTime(currDateTime.toJavaDate());
									convertedLegacyValues.add(cal);
								}
							}
						}
						Calendar currItemValueJNA = note.getItemValueDateTime(currItem);
						List<Object> currItemValueJNAGeneric = note.getItemValue(currItem);
						
						Assert.assertNotNull("Legacy datetime value not null for item "+currItem, currItemValueLegacy);
						Assert.assertNotNull("JNA datetime value not null for item "+currItem, currItemValueJNA);
						Assert.assertNotNull("JNA generic datetime value not null for item "+currItem, currItemValueJNAGeneric);
						
						Assert.assertEquals("Legacy datetime value has one element for item "+currItem, currItemValueLegacy.size(), 1);
						Assert.assertEquals("JNA generic datetime value has one element for item "+currItem, currItemValueJNAGeneric.size(), 1);
						Assert.assertEquals("JNA generic datetime value has correct value for item "+currItem, convertedLegacyValues.get(0), currItemValueJNAGeneric.get(0));
						
						Assert.assertEquals("JNA datetime value is correct for item "+currItem, convertedLegacyValues.get(0), currItemValueJNA);
					}
				}
				
				{
					String[] dateListItems = new String[] {"$Revisions"};
					
					for (String currItem : dateListItems) {
						Vector<?> currItemValueLegacy = doc.getItemValueDateTimeArray(currItem);
						Vector<Calendar> convertedLegacyValues = new Vector<Calendar>(currItemValueLegacy.size());
						if (currItemValueLegacy!=null && !currItemValueLegacy.isEmpty()) {
							for (int i=0; i<currItemValueLegacy.size(); i++) {
								Object currObj = currItemValueLegacy.get(i);
								if (currObj instanceof DateTime) {
									DateTime currDateTime = (DateTime) currObj;
									Calendar cal = Calendar.getInstance();
									cal.setTime(currDateTime.toJavaDate());
									convertedLegacyValues.add(cal);
								}
							}
						}
						List<Object> currItemValueJNAGeneric = note.getItemValue(currItem);
						
						Assert.assertNotNull("JNA generic datetimelist value not null for item "+currItem, currItemValueJNAGeneric);
						Assert.assertEquals("JNA generic datetimelist has correct size", convertedLegacyValues.size(), currItemValueJNAGeneric.size());
						Assert.assertArrayEquals("JNA generic datetimelist has correct content", convertedLegacyValues.toArray(new Object[currItemValueLegacy.size()]), currItemValueJNAGeneric.toArray(new Object[currItemValueJNAGeneric.size()]));
					}
				}

				{
					String[] dateRangeItems = new String[] {"MyDateRange"};
					
					for (String currItem : dateRangeItems) {
						Vector<?> currItemValueLegacy = doc.getItemValue(currItem);
						List<Object> currItemValueJNAGeneric = note.getItemValue(currItem);
						
						Assert.assertNotNull("Legacy value not null for item "+currItem, currItemValueLegacy);

						//the legacy API produces a Vector of two DateTime's for a DateRange
						Assert.assertEquals("Legacy date range values size ok", currItemValueLegacy.size(), 2);
						DateTime rangeStart = (DateTime) currItemValueLegacy.get(0);
						DateTime rangeEnd = (DateTime) currItemValueLegacy.get(1);
						
						Calendar rangeStartCal = Calendar.getInstance();
						rangeStartCal.setTime(rangeStart.toJavaDate());

						Calendar rangeEndCal = Calendar.getInstance();
						rangeEndCal.setTime(rangeEnd.toJavaDate());

						Assert.assertNotNull("JNA generic value not null for item "+currItem, currItemValueJNAGeneric);
						Assert.assertEquals("JNA generic value has correct size for item "+currItem, currItemValueJNAGeneric.size(), 1);
						
						Calendar[] currItemGenericRangeValues = (Calendar[]) currItemValueJNAGeneric.get(0);
						Assert.assertEquals("Start range datetime equal for item "+currItem, rangeStartCal, currItemGenericRangeValues[0]);
						Assert.assertEquals("End range datetime equal for item "+currItem, rangeEndCal, currItemGenericRangeValues[1]);
					}
				}
				
				{
					String itemName = "XXXX";
					
					//round trip check with linebreak
					String testVal = "line1\nline2\nline3";
					note.setItemValueString(itemName, testVal, true);
					String checkTestVal = note.getItemValueString(itemName);
					
					Assert.assertEquals("setItemValue / getItemValue do not change the value", testVal, checkTestVal);
					
					//check removeItem
					Assert.assertTrue("Note changed with setItemValue", note.hasItem(itemName));
					
					note.removeItem(itemName);
					
					Assert.assertFalse("Item removed by removeItem", note.hasItem(itemName));
				}
				
				
				System.out.println("Done with note access test");
				return null;
			}
		});
	
	}

}
