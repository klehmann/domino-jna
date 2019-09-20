package com.mindoo.domino.jna.samples.standaloneapp;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.NotesInitUtils;
import com.mindoo.domino.jna.utils.StringUtil;

/**
 * To run this standalone sample app:<br>
 * <ul>
 * <li>add the Notes.jar to the Java classpath</li>
 * <li>add the Notes/Domino program directory to the PATH, e.g.
 * "/Applications/IBM Notes.app/Contents/MacOS" on macOS or
 * "C:\Program Files (x86)\IBM\Notes" on Windows</li>
 * <li>on macOS: set the environment variable DYLD_LIBRARY_PATH to the same value</li>
 * </ul>
 * @author Karsten Lehmann
 */
public class DominoJNAStandaloneSampleApp {

	public static void main(String[] args) {
		int status;
		
		String notesProgramDirPath = null;
		String notesIniPath = null;
		
		for (int i=0; i<args.length; i++) {
			String currArg = args[i];
			
			if (StringUtil.startsWithIgnoreCase(currArg, "-notesdir:")) {
				notesProgramDirPath = currArg.substring("-notesdir:".length());
			}
			if (StringUtil.startsWithIgnoreCase(currArg, "-ini:")) {
				notesIniPath = currArg.substring("-ini:".length());
			}
		}
		
		String[] notesInitArgs;
		if (!StringUtil.isEmpty(notesProgramDirPath) && !StringUtil.isEmpty(notesIniPath)) {
			notesInitArgs = new String[] {notesProgramDirPath, "="+notesIniPath };
		}
		else {
			notesInitArgs = new String[0];
		}
		
		//call notesInitExtended on app startup
		try {
			NotesInitUtils.notesInitExtended(notesInitArgs);
		}
		catch (NotesError e) {
			e.printStackTrace();
			
			if (e.getId() == 421) {
				//421 happens most of the time
				System.err.println("Please make sure that the Notes.ini exists and specify Notes program dir and notes.ini path like this:");
				System.err.println("Mac:");
				System.err.println("\"-notesdir=/Applications/IBM Notes.app/Contents/MacOS\" \"-ini:/Users/klehmann/Library/Preferences/Notes Preferences\"");
				System.err.println("Windows:");
				System.err.println("\"-notesdir=C:\\Program Files (x86)\\IBM\\Notes\" \"-ini:C:\\Program Files (x86)\\IBM\\Notes\\Notes.ini\"");
				System.exit(-1);
			}
			else {
				System.err.println("Notes init failed with error code "+e.getId());
				System.exit(-1);
			}
		}
		
		try {
			//initial Notes/Domino access for current thread (running single-threaded here)
			NotesInitUtils.notesInitThread();
			
			//launch run method within runWithAutoGC block to let it collect/dispose C handles
			NotesGC.runWithAutoGC(new Callable<Object>() {

				public Object call() throws Exception {
					DominoJNAStandaloneSampleApp app = new DominoJNAStandaloneSampleApp();
					app.run();

					return null;
				}
			});
			
			status = 0;
		}
		catch (Exception e) {
			e.printStackTrace();
			status = -1;
		}
		finally {
			//terminate Notes/Domino access for current thread 
			NotesInitUtils.notesTermThread();
			
			//call notesTerm on app shutdown
			NotesInitUtils.notesTerm();
		}
		System.exit(status);
	}
	
	public void run() {
		System.out.println("Domino JNA test application");
		System.out.println("==========================");
		System.out.println("Username of Notes ID: "+IDUtils.getIdUsername());
		boolean isOnServer = IDUtils.isOnServer();
		System.out.println("Running on "+(isOnServer ? "server" : "client"));
		
		String server = "";
		String filePath = "names.nsf";
		
		// empty string here opens the database as the Notes ID user, e.g. active Notes Client user or the Domino server
		String openAsUser = "";
		
		// could as well be any other user; only works on local databases or between machines listed in
		// the "Trusted servers" list
		// String openAsUser = "Peter Tester/MyOrg";
		
		//open address book database
		NotesDatabase dbNames = new NotesDatabase(server, filePath, openAsUser);
		
		// open main view
		System.out.println("Reading People view");
		NotesCollection peopleView = dbNames.openCollectionByName("People");
		
		// now start reading view data
		
		// "0" means one entry above the first row, not using "1" here, because that entry could be
		// hidden by reader fields
		String startPos = "0";
		// start with 1 here to move from the "0" position to the first document entry
		int skipCount = 1;
		// NEXT_NONCATEGORY means only return document entries; use NEXT to read categories as well
		// or NEXT_CATEGORY to return categories only
		EnumSet<Navigate> navigationType = EnumSet.of(Navigate.NEXT_NONCATEGORY);
		
		// we want to read all view rows at once, use a lower value for web data, e.g.
		// just return 50 or 100 entries per request and use a paging grid in the UI
		int count = Integer.MAX_VALUE;
		// since we want to read all view rows, fill the read buffer with all we can get (max 64K)
		int preloadEntryCount = Integer.MAX_VALUE;
		
		// decide which data to read; use SUMMARYVALUES to read column values;
		// use SUMMARYVALUES instead of SUMMARY to get more data into the buffer.
		// SUMMARY would not just return the column values but also the programmatic
		// column names, eating unnecessary buffer space
		EnumSet<ReadMask> readMask = EnumSet.of(ReadMask.NOTEUNID, ReadMask.SUMMARYVALUES);
		
		// if you are a more advanced user, take a look at the code behind the last
		// parameter; you don't have to work with NotesViewEntryData here, but
		// can produce your own application objects directly.
		List<NotesViewEntryData> viewEntries = peopleView.getAllEntries(startPos,
				skipCount, navigationType, preloadEntryCount, readMask, 
				new NotesCollection.EntriesAsListCallback(count));
		
		System.out.println("Read "+viewEntries.size()+" entries");
		
		for (NotesViewEntryData currEntry : viewEntries) {
			currEntry.setPreferNotesTimeDates(true);
			
			System.out.println(currEntry.getUNID() + "\t" + currEntry.getColumnDataAsMap());
		}
	
	}
}
