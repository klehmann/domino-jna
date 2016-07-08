package com.mindoo.domino.jna.test;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.ISearchCallback;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.directory.DirectoryScanner;
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

	//@Test
	public void testDbSearch_search() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				final Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				//example prefix string to read some data
				final String searchPrefix = "Tyso";
				//example view title returned by @ViewTitle when formula is evaluated
				final String viewTitle = "MyView";
				
				//use DEFAULT statements to add our own field values to the summary buffer data
				//to be returned in the search callback
				String formula = "DEFAULT _docLength := @DocLength;\n" + 
				"DEFAULT _viewTitle := @ViewTitle;\n" +
				"SELECT Form=\"Person\" & @Begins(Lastname;\""+searchPrefix+"\")";
				
				EnumSet<Search> searchFlags = EnumSet.of(Search.NOABSTRACTS,
						Search.SESSION_USERNAME, Search.SUMMARY);
				
				short noteClassMask = NotesCAPI.NOTE_CLASS_DOCUMENT;
				
				long t0=System.currentTimeMillis();
				System.out.println("Running database search with formula: "+formula);
				final int[] cnt = new int[1];
				
				//since = null to search in all documents
				NotesTimeDate since = null;
				dbData.search(formula, viewTitle, searchFlags, noteClassMask, since, new ISearchCallback() {

					@Override
					public void noteFound(NotesDatabase parentDb, int noteId, short noteClass, NotesTimeDate dbCreated,
							NotesTimeDate noteModified, ItemTableData summaryBufferData) {
						
						cnt[0]++;
						Map<String,Object> summaryData = summaryBufferData.asMap();
						Assert.assertTrue("Default value computed", summaryData.containsKey("_docLength"));
						Assert.assertTrue("@ViewTitle returns correct value", viewTitle.equals(summaryData.get("_viewTitle")));
						
						System.out.println("#"+cnt[0]+"\tnoteid="+noteId+", noteclass="+noteClass+", dbCreated="+dbCreated+", noteModified="+noteModified+", summary buffer="+summaryData);
						
						try {
							//load document from the database to verify that it really matches our formula
							Document doc = dbLegacyAPI.getDocumentByID(Integer.toString(noteId, 16));
							String lastName = doc.getItemValueString("Lastname");
							doc.recycle();
							Assert.assertTrue("Lastname "+lastName+" starts with 'Tyso'", lastName!=null && lastName.startsWith(searchPrefix));
						} catch (NotesException e) {
							e.printStackTrace();
						}
					}
				});
				long t1=System.currentTimeMillis();
				System.out.println("Database search done after "+(t1-t0)+"ms. "+cnt[0]+" documents found and processed");
				
				return null;
			}
		});
	}

	/**
	 * Tests the {@link DirectoryScanner} class which internally also uses the database
	 * search function (NSFSearch) to read directory data
	 */
	@Test
	public void testDbSearch_directoryScan() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				final String dbDataFilePath = dbData.getRelativeFilePath();

				String server = "";
				String directory = "";
				//return any NSF type (NS*) and directories; not recursive, since NotesCAPI.FILE_RECURSE is not set
				int fileType = NotesCAPI.FILE_DBANY + NotesCAPI.FILE_DIRS; // + NotesCAPI.FILE_RECURSE;

				//check if our local fakenames database is in the returned list
				final boolean[] fakeNamesDbFound = new boolean[1];
				
				System.out.println("Scanning top level of local directory");
				DirectoryScanner scanner = new DirectoryScanner(session, server, directory, fileType) {
					private String toString(Calendar cal) {
						return cal==null ? "null" : cal.getTime().toString();
					}
					
					@Override
					protected void entryRead(SearchResultData data) {
						if (data instanceof DatabaseData) {
							DatabaseData dbData = (DatabaseData) data;
							
							System.out.println("Database found:\ttitle="+dbData.getTitle()+
									", created="+toString(dbData.getCreated())+
									", modified="+toString(dbData.getModified())+
									", filename="+dbData.getFileName()+
									", filepath="+dbData.getFilePath()+", data="+dbData.getRawData());

							if (dbDataFilePath.equalsIgnoreCase(dbData.getFilePath())) {
								fakeNamesDbFound[0] = true;
							}
						}
						else if (data instanceof FolderData) {
							FolderData folderData = (FolderData) data;
							System.out.println("Folder found:\t"+folderData.getFolderName()+", filepath="+folderData.getFolderPath()+", data="+folderData.getRawData());

						}
						else {
							System.out.println("Unknown type found: data="+data.getRawData());
						}
					}
					
				};
				scanner.scan();
				System.out.println("Done scanning top level of local directory");
				
				Assert.assertTrue("Fakenames database has been found in the directory", fakeNamesDbFound[0]);
				return null;
			}
		});
	
	}
}
