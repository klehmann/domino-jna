package com.mindoo.domino.jna.test;

import java.io.StringWriter;

import org.junit.Test;

import com.mindoo.domino.jna.NotesAgent;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;

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
				
				testAgent.run(checkSecurity, runAsSigner, stdOut, timeoutSeconds, note);
				
				note.recycle();
				
				System.out.println(stdOut);
				
				return null;
			}
		});
	}
	

}
