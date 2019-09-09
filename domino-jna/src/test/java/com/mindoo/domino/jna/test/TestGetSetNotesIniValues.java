package com.mindoo.domino.jna.test;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesIniUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;

import lotus.domino.Session;

/**
 * Tests cases for reading/writing Notes.ini values
 * 
 * @author Karsten Lehmann
 */
public class TestGetSetNotesIniValues extends BaseJNATestClass {

	@Test
	public void testSetGet() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Testing get/set of NotesTimeDate in Notes.ini");
				final String tdVarName = "$tdTest";

				NotesIniUtils.setEnvironmentString(tdVarName, "");
				NotesTimeDate tdOld = NotesIniUtils.getEnvironmentTimeDate(tdVarName);
				System.out.println("old timedate variable value: "+tdVarName+"="+tdOld);
				Assert.assertNull("Empty string is returned as null", tdOld);
				
				String wrongTdVal = "XYZ123";
				NotesIniUtils.setEnvironmentString(tdVarName, wrongTdVal);
				System.out.println("Wrote value with wrong format : "+tdVarName+"="+wrongTdVal);
				NotesTimeDate tdWrongVal = NotesIniUtils.getEnvironmentTimeDate(tdVarName);
				Assert.assertNull("Unparsable value is returned as null", tdWrongVal);
				
				NotesTimeDate tdOrig = NotesTimeDate.now();
				System.out.println("Writing correct value to Notes.ini: "+tdOrig);
				NotesIniUtils.setEnvironmentTimeDate(tdVarName, tdOrig);
				
				//this value will be a DBID type value like "C1258470:00263163":
				String tdValAsStr = NotesIniUtils.getEnvironmentString(tdVarName);
				System.out.println("new timedate variable value as string: "+tdValAsStr);
				int[] parsedInnards = NotesStringUtils.replicaIdToInnards(tdValAsStr);
				Assert.assertArrayEquals("Notes.ini value is formatted as DBID", tdOrig.getInnards(), parsedInnards);
				
				NotesTimeDate tdNew = NotesIniUtils.getEnvironmentTimeDate(tdVarName);
				System.out.println("new timedate variable value: "+tdVarName+"="+tdNew);
				Assert.assertEquals("Roundtrip of value works, orig="+tdOrig+", read="+tdNew, tdOrig, tdNew);
				
				System.out.println("Done testing get/set of NotesTimeDate in Notes.ini");

				return null;
			}
		});
	}

}
