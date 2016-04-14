package com.mindoo.domino.jna.test;

import java.util.Date;
import java.util.EnumSet;
import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.ViewLookupCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.NoteInfo;
import com.mindoo.domino.jna.NotesDatabase.NoteInfoExt;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.structs.NotesTimeDate;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Tests cases for note lookups
 * 
 * @author Karsten Lehmann
 */
public class TestNoteLookup extends BaseJNATestClass {

	@Test
	public void testNoteLookup_multi() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting multi note lookup");
				
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				final int nrOfNotesToRead = 20; // no more than we have in the view
				
				//grab some note ids from the People view
				NotesCollection col = dbData.openCollectionByName("People");
				int[] someNoteIds = col.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), 10, EnumSet.of(ReadMask.NOTEID), new ViewLookupCallback<int[]>() {
					
					int m_idx = 0;
					
					@Override
					public int[] startingLookup() {
						return new int[nrOfNotesToRead];
					}

					@Override
					public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(int[] result,
							NotesViewEntryData entryData) {
						if (m_idx < result.length) {
							result[m_idx] = entryData.getNoteId();
							m_idx++;
							return Action.Continue;
						}
						else {
							return Action.Stop;
						}
					}

					@Override
					public int[] lookupDone(int[] result) {
						return result;
					}
				});
				
				NoteInfo[] noteLookupResult = dbData.getMultiNoteInfo(someNoteIds);
				Assert.assertEquals("Input and output arrays have the same number of entries", someNoteIds.length, noteLookupResult.length);
				
				for (int i=0; i<noteLookupResult.length; i++) {
					NoteInfo currInfo = noteLookupResult[i];
					
					Assert.assertEquals("Input and output array have the same sort order", someNoteIds[i], currInfo.getNoteId()); 
					
					System.out.println("#"+(i+1)+"\tnoteid="+currInfo.getNoteId()+", unid="+currInfo.getUnid()+", seq="+currInfo.getSequence()+", seqtime="+currInfo.getSequenceTime()+", deleted="+currInfo.isDeleted()+", exists="+(currInfo.exists()));
					
					//required for the modified date comparison between legacy API and JNA:
					session.setTrackMillisecInJavaDates(true);
					
					if (currInfo.exists()) {
						Document currDoc = null;
						try {
							currDoc = dbLegacyAPI.getDocumentByUNID(currInfo.getUnid());
							Assert.assertEquals("Loaded doc has the expected note id", currInfo.getNoteId(), Integer.parseInt(currDoc.getNoteID(), 16));

							//Document.getLastModified() returns the value of "Modified in this file", which
							//we don't want here:
//							DateTime ndtLastModLegacy = currDoc.getLastModified();
							
							Vector<?> formulaResult = session.evaluate("@Modified", currDoc);
							DateTime ndtLastModLegacy = (DateTime) formulaResult.get(0);
							Date jdtLastModLegacy = ndtLastModLegacy.toJavaDate();
							
							NotesTimeDate lastModJNA = currInfo.getSequenceTime();
							Date jdtLastModJNA = lastModJNA.toDate();
							
							Assert.assertEquals("Last modified dates of note with id " + currInfo.getNoteId()+" matches between legacy API and JNA", jdtLastModLegacy, jdtLastModJNA);
						}
						catch (NotesException e) {
							e.printStackTrace();
							Assert.assertTrue("Document loading of UNID "+currInfo.getUnid()+" / note id "+currInfo.getNoteId()+" works", false);
						}
						finally {
							if (currDoc!=null) {
								currDoc.recycle();
							}
						}
					}
				}
				
				System.out.println("Done with multi note lookup");
				return null;
			}
		});
	}

	@Test
	public void testNoteLookup_single() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting single note lookup");
				
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				final int nrOfNotesToRead = 1; // no more than we have in the view
				
				//grab some note ids from the People view
				NotesCollection col = dbData.openCollectionByName("People");
				int[] someNoteIds = col.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), 2, EnumSet.of(ReadMask.NOTEID), new ViewLookupCallback<int[]>() {
					
					int m_idx = 0;
					
					@Override
					public int[] startingLookup() {
						return new int[nrOfNotesToRead];
					}

					@Override
					public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(int[] result,
							NotesViewEntryData entryData) {
						if (m_idx < result.length) {
							result[m_idx] = entryData.getNoteId();
							m_idx++;
							return Action.Continue;
						}
						else {
							return Action.Stop;
						}
					}

					@Override
					public int[] lookupDone(int[] result) {
						return result;
					}
				});
				
				//required for the modified date comparison between legacy API and JNA:
				session.setTrackMillisecInJavaDates(true);
				
				NoteInfoExt noteInfo = dbData.getNoteInfoExt(73843498);
				Assert.assertFalse("Check for nonexisting docs works", noteInfo.exists());
				
				noteInfo = dbData.getNoteInfoExt(someNoteIds[0]);
				
				if (noteInfo.exists()) {
					System.out.println("noteid="+noteInfo.getNoteId()+", unid="+noteInfo.getUnid()+
							", seq="+noteInfo.getSequence()+", seqtime="+noteInfo.getSequenceTime()+
							", deleted="+noteInfo.isDeleted()+", exists="+(noteInfo.exists()+
									", modified="+noteInfo.getModified()+", noteclass="+noteInfo.getNoteClass()+
									", parentid="+noteInfo.getParentNoteId()+", responsecount="+
									noteInfo.getResponseCount()+", addedtofile="+noteInfo.getAddedToFile()
									));
					
					Document doc = dbLegacyAPI.getDocumentByUNID(noteInfo.getUnid());
					
					Assert.assertEquals("Loaded doc has the expected note id", noteInfo.getNoteId(), Integer.parseInt(doc.getNoteID(), 16));

					Assert.assertEquals("Response count matches", noteInfo.getResponseCount(), doc.getResponses().getCount());

					Vector<?> formulaResult = session.evaluate("@Modified", doc);
					DateTime ndtLastModLegacy = (DateTime) formulaResult.get(0);
					Date jdtLastModLegacy = ndtLastModLegacy.toJavaDate();
					
					NotesTimeDate lastModJNA = noteInfo.getSequenceTime();
					Date jdtLastModJNA = lastModJNA.toDate();
					
					Assert.assertEquals("Last modified dates of note with id " + noteInfo.getNoteId()+
							" matches between legacy API and JNA", jdtLastModLegacy, jdtLastModJNA);
				}
				else {
					System.out.println("Note with id "+noteInfo.getNoteId()+" does not exist");
				}
				
				System.out.println("Done with single note lookup");
				return null;
			}
		});
	}

}
