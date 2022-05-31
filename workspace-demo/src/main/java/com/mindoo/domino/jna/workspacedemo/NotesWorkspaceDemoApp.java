package com.mindoo.domino.jna.workspacedemo;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.Callable;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesWorkspace;
import com.mindoo.domino.jna.NotesWorkspace.IconColor;
import com.mindoo.domino.jna.NotesWorkspace.WorkspaceIcon;
import com.mindoo.domino.jna.NotesWorkspace.WorkspacePage;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.NotesInitUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.StringUtil;

import lotus.domino.NotesThread;

public class NotesWorkspaceDemoApp {

	private static String stripQuotes(String str) {
		if (str!=null) {
			if (str.startsWith("\"")) {
				str = str.substring(1);
			}
			if (str.endsWith("\"")) {
				str = str.substring(0, str.length()-1);
			}
		}
		return str;
	}
	public static void main(String[] args) {
		System.out.println("Environment: "+System.getenv());
		System.out.println();
		System.out.println("PATH: "+System.getenv("PATH"));
		
		String notesProgramDirPath = null;
		String notesIniPath = null;
		
		// we support setting the notes program directory and notes.ini filepath as command line parameter
		// and environment variables
		for (int i=0; i<args.length; i++) {
			String currArg = args[i];
			
			if (StringUtil.startsWithIgnoreCase(currArg, "-notesdir:")) {
				notesProgramDirPath = currArg.substring("-notesdir:".length());
			}
			if (StringUtil.startsWithIgnoreCase(currArg, "-ini:")) {
				notesIniPath = currArg.substring("-ini:".length());
			}
		}
		
		if (StringUtil.isEmpty(notesProgramDirPath)) {
			notesProgramDirPath = System.getenv("Notes_ExecDirectory");
		}
		
		if (StringUtil.isEmpty(notesIniPath)) {
			notesIniPath = System.getenv("NotesINI");
		}
		
		notesProgramDirPath = stripQuotes(notesProgramDirPath);
		notesIniPath = stripQuotes(notesIniPath);
		
		String[] notesInitArgs;
		if (!StringUtil.isEmpty(notesProgramDirPath) && !StringUtil.isEmpty(notesIniPath)) {
			notesInitArgs = new String[] {notesProgramDirPath, "="+notesIniPath };
		}
		else {
			notesInitArgs = new String[0];
		}
		
		int exitStatus = 0;
		boolean notesInitialized = false;
		//call notesInitExtended on app startup
		try {
			System.out.println("Initializing Notes API with launch arguments: "+Arrays.toString(notesInitArgs));
			
			NotesInitUtils.notesInitExtended(notesInitArgs);
			notesInitialized = true;
			
			NotesWorkspaceDemoApp app = new NotesWorkspaceDemoApp();
			app.run();
		}
		catch (NotesError e) {
			e.printStackTrace();
			exitStatus=-1;

			if (e.getId() == 421) {
				//421 happens most of the time
				System.err.println();
				System.err.println("Please make sure that the Notes.ini exists and specify Notes program dir and notes.ini path like this:");
				System.err.println("Mac:");
				System.err.println("\"-notesdir=/Applications/IBM Notes.app/Contents/MacOS\" \"-ini:/Users/klehmann/Library/Preferences/Notes Preferences\"");
				System.err.println("Windows:");
				System.err.println("\"-notesdir=C:\\Program Files (x86)\\IBM\\Notes\" \"-ini:C:\\Program Files (x86)\\IBM\\Notes\\Notes.ini\"");
				System.err.println();
				System.err.println("As an alternative, use environment variables Notes_ExecDirectory and NotesINI for those two paths.");
			}
			else if (e.getId() == 258) {
				System.err.println();
				System.err.println("If using macOS Catalina, make sure that the java process has full disk access rights in the"
						+ " macOS security settings. Looks like we cannot access the Notes directories.");
			}
			else {
				System.err.println();
				System.err.println("Notes init failed with error code "+e.getId());
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			exitStatus=-1;
		}
		finally {
			if (notesInitialized) {
				NotesInitUtils.notesTerm();
			}
		}
		
		System.exit(exitStatus);
	}
	
	public void run() throws Exception {

		try {
			//initial Notes/Domino access for current thread (running single-threaded here)
			NotesThread.sinitThread();
			
			//launch run method within runWithAutoGC block to let it collect/dispose C handles
			NotesGC.runWithAutoGC(new Callable<Object>() {

				public Object call() throws Exception {
					// use IDUtils.switchToId if you want to unlock the ID file and switch the current process
					// to this ID; should only be used in standalone applications
					// if this is missing, you will be prompted for your ID password the first time the
					// id certs are required
					String notesIdFilePath = System.getProperty("idfilepath");
					String idPassword = System.getProperty("idpw");
					
					if (notesIdFilePath!=null && notesIdFilePath.length()>0 && idPassword!=null && idPassword.length()>0) {
						// don't change Keyfileowner and other Notes.ini variables to this ID, so Notes Client can
						// keep on running concurrently with his own ID
						boolean dontSetEnvVar = true;
						IDUtils.switchToId(notesIdFilePath, idPassword, dontSetEnvVar);
					}
					
					NotesGC.setPreferNotesTimeDate(true);
					
					readNotesData();

					return null;
				}
			});
		}
		finally {
			//terminate Notes/Domino access for current thread 
			NotesThread.stermThread();
		}
	}
	
	private void readNotesData() {
		System.out.println("Domino JNA Notes workspace test application");
		System.out.println("==========================");
		System.out.println("Username of Notes ID: "+IDUtils.getIdUsername());
		boolean isOnServer = IDUtils.isOnServer();
		System.out.println("Running on "+(isOnServer ? "server" : "client"));
		
		String server = "";
		String filePath = "desktop8.ndk";
		
		// empty string here opens the database as the Notes ID user, e.g. active Notes Client user
		String openAsUser = "";
		
		//open Notes desktop DB
		NotesDatabase dbWorkspace = new NotesDatabase(server, filePath, openAsUser);
		
		//decode workspace content
		NotesWorkspace ws = new NotesWorkspace(dbWorkspace);
		
		listWorkspaceContent(ws);
		
		createPageAndIconOnWorkspace(ws);
	}
	
	private void listWorkspaceContent(NotesWorkspace ws) {
		ws
		.getPages()
		.forEach((currPage) -> {
			System.out.println(currPage);
			System.out.println("==============================================================================================");
			
			currPage.getIcons()
			.stream()
			.forEach((currIcon) -> {
				System.out.println(currIcon);
			});

			System.out.println("\n");
		});
	}

	private void createPageAndIconOnWorkspace(NotesWorkspace ws) {
		//let's create a new page if it's not already there
		final String newPageName = "JNA Added Page";

		WorkspacePage newPage = ws
				.getPage(newPageName)
				.orElseGet(() -> {
					return ws.addPage(newPageName);
				});
		//set page color to purple; method will find the best match for rgb values in the color palette
		newPage.setColorIndex(NotesWorkspace.findNearestTabColor(165, 137, 193));
		
		//create a new icon
		
		String newIconTitle = "Domino JNA Testicon";
		String newIconServer = "CN=TestServer123/O=Mindoo";
		String newIconFilePath = "dominojnatest.nsf";
		String newIconReplicaId = "1234123412341234";
		//0-based x/y position
		int newIconPosX = 3;
		int newIconPosY = 3;
		
		WorkspaceIcon ourNewIcon = ws.getIcons()
		.stream()
		.filter((currIcon) -> {
			//let's see if we've already created the icon previously
			return newIconTitle.equals(currIcon.getTitle()) &&
					NotesNamingUtils.equalNames(newIconServer, currIcon.getServer()) &&
					newIconFilePath.equals(currIcon.getFilePath()) &&
					newIconReplicaId.equals(currIcon.getReplicaID());
		})
		.findFirst()
		.orElseGet(() -> {
			//now we create a new chicklet, clear its default icon (blue Domino logo) and draw a red rectangle;
			
			//Please note: there's also a NotesWorkspace.addIcon method with a NotesDatabase argument that copies the
			//classic/modern DB icon image from the NSF design
			
			WorkspaceIcon newIcon = newPage
					.addIcon(newIconTitle, newIconServer, newIconFilePath, newIconReplicaId, newIconPosX, newIconPosY)
					.clearIcon();
			
			for (int i=0; i<32; i++) {
				newIcon.setIconColor(i, 0, IconColor.RED);
				newIcon.setIconColor(0, i, IconColor.RED);
				newIcon.setIconColor(i, 31, IconColor.RED);
				newIcon.setIconColor(31, i, IconColor.RED);
			}
			
			return newIcon;
		});
		
		//if the icon's position is different, move it back to our default x/y values
		if (ourNewIcon.getPosX() != newIconPosX || ourNewIcon.getPosY() != newIconPosY) {
			ourNewIcon.move(newPage, newIconPosX, newIconPosY);
		}
		
		if (ws.isModified()) {
			ws.store();
		}
	}
	
}
