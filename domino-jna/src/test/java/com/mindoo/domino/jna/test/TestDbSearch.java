package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.ISearchCallback;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.structs.NotesTimeDate;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Tests cases for database searches
 * 
 * @author Karsten Lehmann
 */
public class TestDbSearch extends BaseJNATestClass {

	@Test
	public void testDbSearch_search() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				final Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				final String searchPrefix = "Tyso";
				final String viewTitle = "MyView";
				
				String formula = "DEFAULT _docLength := @DocLength;\n" + 
				"DEFAULT _viewTitle := @ViewTitle;\n" +
				"SELECT Form=\"Person\" & @Begins(Lastname;\""+searchPrefix+"\")";
				EnumSet<Search> searchFlags = EnumSet.of(Search.NOABSTRACTS,
						Search.SESSION_USERNAME, Search.SUMMARY);
				
				short noteClassMask = NotesCAPI.NOTE_CLASS_DOCUMENT;
				
				dbData.search(formula, viewTitle, searchFlags, noteClassMask, null, new ISearchCallback() {

					@Override
					public void noteFound(NotesDatabase parentDb, int noteId, short noteClass, NotesTimeDate dbCreated,
							NotesTimeDate noteModified, ItemTableData summaryBufferData) {
						
						Map<String,Object> summaryData = summaryBufferData.asMap();
						Assert.assertTrue("Default value computed", summaryData.containsKey("_docLength"));
						Assert.assertTrue("@ViewTitle returns correct value", viewTitle.equals(summaryData.get("_viewTitle")));
						
						try {
							Document doc = dbLegacyAPI.getDocumentByID(Integer.toString(noteId, 16));
							String lastName = doc.getItemValueString("Lastname");
							doc.recycle();
							Assert.assertTrue("Lastname "+lastName+" starts with 'Tyso'", lastName!=null && lastName.startsWith(searchPrefix));
						} catch (NotesException e) {
							e.printStackTrace();
						}
					}
					
				});
				
				return null;
			}
		});
	}
}
