package com.mindoo.domino.jna.test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesAgent;
import com.mindoo.domino.jna.NotesAgentRunContext;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNamesList;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

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
//				NotesDatabase dbData = getFakeNamesDb();
				
				int rnd = (int) (10000*Math.random());
				
				String fakeUsername = "Fake User"+rnd+"/Mindoo";
				String fakeCommonName = "Fake User"+rnd;
				String fakeGroup = "FakeGroup_"+rnd;
				String fakeRole = "[FakeRole_"+rnd+"]";

				List<String> namesListStr = Arrays.asList(
						fakeUsername,
						fakeCommonName,
						"*/Mindoo",
						"*",
						fakeGroup,
						fakeRole
						);
				NotesNamesList namesList = NotesNamingUtils.writeNewNamesList(namesListStr);
				
				NotesDatabase dbData = new NotesDatabase(getSession(), "", "fakenames.nsf", namesListStr);
				NotesAgent testAgent = dbData.getAgent("AgentRun Test LS");
				System.out.println("Agent.isRunAsWebUser(): "+testAgent.isRunAsWebUser());
				
				StringWriter stdOut = new StringWriter();
				
				boolean checkSecurity = false;
				boolean runAsSigner = true;
				int timeoutSeconds = 0;
				
				NotesNote note = dbData.createNote();
				note.setItemValueString("testitem", "1234", true);
				

				testAgent.run(
						new NotesAgentRunContext()
						.setCheckSecurity(checkSecurity)
						.setReopenDbAsSigner(runAsSigner)
						.setOutputWriter(stdOut)
						.setTimeoutSeconds(timeoutSeconds)
						.setDocumentContextAsNote(note)
						.setParamDocId(12345)
						.setUsernameAsStringList(namesListStr)
						);
				
				List<Object> userNamesList = note.getItemValue("UserNamesList");
				System.out.println("@UserNamesList: "+userNamesList);
				
				note.recycle();
				
				System.out.println(stdOut);
				
				return null;
			}
		});
	}
	
//	@Test
	public void testAgentExecution_runAgentWithLegacyDoc() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbDataLegacy = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				NotesAgent testAgent = dbData.getAgent("AgentRun Test LS");
				
				StringWriter stdOut = new StringWriter();
				
				boolean checkSecurity = true;
				boolean reopenDbAsSigner = false;
				int timeoutSeconds = 0;
				
				Document doc = dbDataLegacy.createDocument();
				doc.replaceItemValue("testitem", "1234");

				testAgent.run(
						new NotesAgentRunContext()
						.setCheckSecurity(checkSecurity)
						.setReopenDbAsSigner(reopenDbAsSigner)
						.setOutputWriter(stdOut)
						.setTimeoutSeconds(timeoutSeconds)
						.setDocumentContextAsLegacyDoc(doc)
						);

				String retValue = doc.getItemValueString("returnValue");
				System.out.println("Return value: "+retValue);
				
				doc.recycle();
				
				System.out.println(stdOut);
				
				return null;
			}
		});
	}
}
