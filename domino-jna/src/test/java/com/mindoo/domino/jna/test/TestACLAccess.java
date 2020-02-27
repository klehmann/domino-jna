package com.mindoo.domino.jna.test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import com.mindoo.domino.jna.NotesACL;
import com.mindoo.domino.jna.NotesACL.NotesACLAccess;
import com.mindoo.domino.jna.NotesACL.NotesACLEntry;
import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.utils.IDUtils;

import junit.framework.Assert;
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
//	@Test
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
				
				LinkedHashMap<String,NotesACLEntry> entriesWithAccess = acl.getEntries();
				
				for (Entry<String,NotesACLEntry> currEntry : entriesWithAccess.entrySet()) {
					System.out.println(currEntry.getKey()+"\t"+currEntry.getValue());
				}

				System.out.println("Done with ACL entries test");
				return null;
			}
		});
	}

	/**
	 * Reads the ACL entries
	 */
	@Test
	public void testCreateACLEntry() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting ACL modification");
				
				NotesDatabase dbJNA = getFakeNamesDb();
				NotesACL acl = dbJNA.getACL();
				
				// role "NetCreator" will be converted to [NetCreator] by addEntry
				acl.addEntry("Testuser123/Mindoo", AclLevel.DESIGNER, Arrays.asList("[PolicyReader]", "NetCreator"), EnumSet.noneOf(AclFlag.class));
				
				NotesACLAccess aclEntry = acl.getEntry("cn=testuser123/o=mindoo");
				Assert.assertNotNull("Entry has been created", aclEntry);
				Assert.assertEquals("Entry has expected ACL level", AclLevel.DESIGNER, aclEntry.getAclLevel());
				Assert.assertTrue("Entry has expected role", aclEntry.getRoles().contains("[PolicyReader]"));
				Assert.assertTrue("Entry has expected role", aclEntry.getRoles().contains("[NetCreator]"));
				
				//don't save the ACL

				System.out.println("Done with ACL modification");
				return null;
			}
		});
	}
	
}
