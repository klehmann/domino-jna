package com.mindoo.domino.jna.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import com.mindoo.domino.jna.NotesACL;
import com.mindoo.domino.jna.NotesACL.NotesACLAccess;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.utils.IDUtils;

import lotus.domino.Session;

/**
 * Tests cases for ACL access
 * 
 * @author Karsten Lehmann
 */
public class TestACLAccess extends BaseJNATestClass {

	/**
	 * Reads the ACL entries
	 */
	@Test
	public void testReadACLEntries() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting ACL entries test");
				
				NotesDatabase dbJNA = getFakeNamesDb();
				NotesACL acl = dbJNA.getACL();

				List<String> allRoles = acl.getRoles();
				System.out.println("All ACL roles: "+allRoles);

				String testUsername = IDUtils.getIdUsername();
				
				NotesACLAccess testUserAccess = acl.lookupAccess(testUsername);
				System.out.println("Computed access for "+testUsername+": "+testUserAccess);

				System.out.println("Now listing all ACL entries and their access level:");
				
				LinkedHashMap<String,NotesACLAccess> entriesWithAccess = acl.getEntries();
				
				for (Entry<String,NotesACLAccess> currEntry : entriesWithAccess.entrySet()) {
					System.out.println(currEntry.getKey()+"\t"+currEntry.getValue());
				}

				System.out.println("Done with ACL entries test");
				return null;
			}
		});
	}

}
