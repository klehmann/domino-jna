package com.mindoo.domino.jna.test;

import java.io.StringWriter;

import org.junit.Test;

import com.mindoo.domino.jna.NotesAgent;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;

/**
 * Tests cases for note access
 * 
 * @author Karsten Lehmann
 */
public class TestAgentExecution extends BaseJNATestClass {

	@Test
	public void testAgentExecution_runAgent() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesAgent testAgent = dbData.getAgent("AgentRun Test LS");
				
				StringWriter stdOut = new StringWriter();
				
				boolean checkSecurity = true;
				boolean runAsSigner = false;
				int timeoutSeconds = 0;
				
				NotesNote note = dbData.createNote();
				note.setItemValueString("testitem", "1234", true);
				
				testAgent.run(checkSecurity, runAsSigner, stdOut, timeoutSeconds, note, 12345);
				
				note.recycle();
				
				System.out.println(stdOut);
				
				return null;
			}
		});
	}
	
	@Test
	public void testAgentExecution_runAgentWithLegacyDoc() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbDataLegacy = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				NotesAgent testAgent = dbData.getAgent("AgentRun Test LS");
				
				StringWriter stdOut = new StringWriter();
				
				boolean checkSecurity = true;
				boolean runAsSigner = false;
				int timeoutSeconds = 0;
				
				Document doc = dbDataLegacy.createDocument();
				doc.replaceItemValue("testitem", "1234");
				
				testAgent.run(checkSecurity, runAsSigner, stdOut, timeoutSeconds, doc, 0);
				
				String retValue = doc.getItemValueString("returnValue");
				System.out.println("Return value: "+retValue);
				
				doc.recycle();
				
				System.out.println(stdOut);
				
				return null;
			}
		});
	}
}
