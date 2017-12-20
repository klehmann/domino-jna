package com.mindoo.domino.jna.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.ISearchCallback;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.FileType;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.constants.UpdateCollectionFilters;
import com.mindoo.domino.jna.directory.DirectoryScanner;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;

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
	public void testDbSearch_searchSelectedNoteIds() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();

				//PeopleFlatMultiColumnSort is sorted by lastname and has columns "firstname" / "lastname" that we read later
				NotesCollection col = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				
				LinkedHashSet<Integer> idsToSearch = col.getAllIdsByKey(EnumSet.of(Find.PARTIAL, Find.CASE_INSENSITIVE), "A");
				System.out.println("Found "+idsToSearch.size()+" ids in the view with lastname starting with 'A'");
				
				Assert.assertTrue("Lookup could find ids in view ", !idsToSearch.isEmpty());
				
				NotesIDTable idTable = new NotesIDTable(idsToSearch);
				NotesIDTable filteredTable = idTable.filter(dbData, "@Begins(Firstname;\"E\")");
				System.out.println(filteredTable.getCount()+" of them have a firstname starting with 'E'");

				Assert.assertTrue("Formula search operation further reduced the note count", filteredTable.getCount() < idsToSearch.size());
				
				//now to back to the view and read the filtered data
				NotesIDTable selectedList = col.getSelectedList();
				selectedList.clear();
				selectedList.addTable(filteredTable);
				
				//push selection list changes to remote server, noop locally
				col.updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));
				
				//read all matching data into a list, demonstrating a custom "Person"
				//return value type for the ViewLookupCallback; as an alternative, you
				//could use NotesCollection.EntriesAsListCallback(Integer.MAX_VALUE) as
				//callback which returns a generic List<NotesViewEntryData>
				List<Person> persons = col.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEUNID,
						ReadMask.SUMMARYVALUES, ReadMask.NOTEID), new NotesCollection.ViewLookupCallback<List<Person>>() {

							//callback may get gestarted if view changes while reading
							@Override
							public List<Person> startingLookup() {
								return new ArrayList<Person>();
							}

							@Override
							public Action entryRead(List<Person> result, NotesViewEntryData entryData) {
								String unid = entryData.getUNID();
								int noteId = entryData.getNoteId();
								String firstName = entryData.getAsString("firstname", "");
								String lastName = entryData.getAsString("lastname", "");

								Person person = new Person(unid, noteId, firstName, lastName);
								result.add(person);
								
								return Action.Continue;
							}

							@Override
							public List<Person> lookupDone(List<Person> result) {
								//optional method for further processing of the list
								return result;
							}
				});
				
				for (int i=0; i<persons.size(); i++) {
					Person currPerson = persons.get(i);
					System.out.println("#"+i+"\t"+currPerson.toString());
				}
				
				return null;
			}
		});
	
	}
	
	private class Person {
		private String m_unid;
		private int m_noteId;
		private String m_firstName;
		private String m_lastName;
		
		public Person(String unid, int noteId, String firstName, String lastName) {
			m_unid = unid;
			m_noteId = noteId;
			m_firstName = firstName;
			m_lastName = lastName;
		}
		
		public String getUNID() {
			return m_unid;
		}
		
		public int getNoteId() {
			return m_noteId;
		}
		
		public String getFirstName() {
			return m_firstName;
		}
		
		public String getLastName() {
			return m_lastName;
		}
		
		@Override
		public String toString() {
			return "Person [unid="+m_unid+", noteid="+m_noteId+", lastname="+m_lastName+", firstname="+m_firstName+"]";
		}
	}
	
	@Test
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
				
				long t0=System.currentTimeMillis();
				System.out.println("Running database search with formula: "+formula);
				final int[] cnt = new int[1];
				
				//since = null to search in all documents
				NotesTimeDate since = null;
				NotesTimeDate endTimeDate = dbData.search(formula, viewTitle, searchFlags, EnumSet.of(NoteClass.DOCUMENT), since, new ISearchCallback() {

					@Override
					public Action noteFound(NotesDatabase parentDb, int noteId, EnumSet<NoteClass> noteClass, NotesTimeDate dbCreated,
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
						return Action.Continue;
					}
				});
				Assert.assertNotNull("Returned end timedate is not null", endTimeDate);
				
				System.out.println("Returned end timedate: "+endTimeDate);
				
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
				EnumSet<FileType> fileTypes = EnumSet.of(FileType.DBANY, FileType.DIRS);
				
				//check if our local fakenames database is in the returned list
				final boolean[] fakeNamesDbFound = new boolean[1];
				
				System.out.println("Scanning top level of local directory");
				DirectoryScanner scanner = new DirectoryScanner(server, directory, fileTypes) {
					private String toString(Calendar cal) {
						return cal==null ? "null" : cal.getTime().toString();
					}
					
					@Override
					protected Action entryRead(SearchResultData data) {
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
						return Action.Continue;
					}
				};
				scanner.scan();
				System.out.println("Done scanning top level of local directory");
				
				Assert.assertTrue("Fakenames database has been found in the directory", fakeNamesDbFound[0]);
				return null;
			}
		});
	
	}
	
	@Test
	public void testDbSearch_dbByReplicaId() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				String replicaId = dbData.getReplicaID();
				
				String server = "";

				String dbPathForReplicaId = NotesDatabase.findDatabaseByReplicaId(server, replicaId);
				Assert.assertTrue("Database could be found by replica id in base dir", dbData.getRelativeFilePath().equalsIgnoreCase(dbPathForReplicaId));

				String otherReplicaId = "AAAABBBBCCCCDDDD";
				
				String dbPathForFakeReplicaId = NotesDatabase.findDatabaseByReplicaId(server, otherReplicaId);
				Assert.assertEquals("Database could be found by replica id in base dir", null, dbPathForFakeReplicaId);
				return null;
			}
		});
	}
}
