package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import com.mindoo.domino.jna.NotesACL;
import com.mindoo.domino.jna.NotesACL.NotesACLAccess;
import com.mindoo.domino.jna.NotesACL.NotesACLEntry;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
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
				withTempDb((db) -> {
					NotesACL acl = db.getACL();
					acl.addRole("Role1");
					acl.addRole("[Role2]");
					acl.save();
					
					List<String> allRoles = acl.getRoles();
					System.out.println("All ACL roles: "+allRoles);
					assertNotNull(allRoles);
					assertTrue(!allRoles.isEmpty());
					
					String testUsername = IDUtils.getIdUsername();
					NotesACLAccess testUserAccess = acl.lookupAccess(testUsername);
					System.out.println("Computed access for "+testUsername+": "+testUserAccess);
					assertEquals(AclLevel.MANAGER, testUserAccess.getAclLevel());

					System.out.println("Now listing all ACL entries and their access level:");
					
					LinkedHashMap<String,NotesACLEntry> entriesWithAccess = acl.getEntries();
					assertTrue(!entriesWithAccess.isEmpty());
					
					for (Entry<String,NotesACLEntry> currEntry : entriesWithAccess.entrySet()) {
						System.out.println(currEntry.getKey()+"\t"+currEntry.getValue());
					}

					System.out.println("Done with ACL entries test");

				});
				
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
				withTempDb((db) -> {
					System.out.println("Starting ACL modification");
					
					NotesACL acl = db.getACL();
					acl.addRole("[PolicyReader]");
					acl.addRole("[NetCreator]");
					
					// role "NetCreator" will be converted to [NetCreator] by addEntry
					acl.addEntry("Testuser123/Mindoo", AclLevel.DESIGNER, Arrays.asList("[PolicyReader]", "NetCreator"), EnumSet.noneOf(AclFlag.class));
					
					NotesACLAccess aclEntry = acl.getEntry("cn=testuser123/o=mindoo");
					assertNotNull("Entry has been created", aclEntry);
					assertEquals("Entry has expected ACL level", AclLevel.DESIGNER, aclEntry.getAclLevel());
					assertTrue("Entry has expected role", aclEntry.getRoles().contains("[PolicyReader]"));
					assertTrue("Entry has expected role", aclEntry.getRoles().contains("[NetCreator]"));
					
					//don't save the ACL

					System.out.println("Done with ACL modification");
					
				});
				return null;
			}
		});
	}
	
	@Test
	public void testDetachedAcl() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb((db) -> {
					System.out.println("Starting ACL detaching test");
					
					NotesACL acl = db.getACL();
					acl.addRole("[PolicyReader]");
					acl.addRole("[NetCreator]");

					// role "NetCreator" will be converted to [NetCreator] by addEntry
					acl.addEntry("Testuser123/Mindoo", AclLevel.DESIGNER,
							Arrays.asList("[PolicyReader]", "NetCreator"), EnumSet.noneOf(AclFlag.class));

					NotesACLAccess aclAccess = acl.lookupAccess("Testuser123/Mindoo");

					//this one can be cached in memory to run a number of queries on it
					//without the need to keep the NSF open
					NotesACL aclDetached = acl.cloneDetached();
					
					//we don't need the NSF anymore:
					db.recycle();

					//now look up the user we just added:
					NotesACLAccess detachedAccess = aclDetached.lookupAccess("Testuser123/Mindoo");
					
					assertEquals(aclAccess.getAclFlags(), detachedAccess.getAclFlags());
					assertEquals(aclAccess.getAclLevel(), detachedAccess.getAclLevel());
					
					assertTrue(aclAccess.getRoles().contains("[NetCreator]"));
					assertEquals(aclAccess.getRoles(), detachedAccess.getRoles());
					
					//don't save the ACL

					System.out.println("Done with ACL detaching test");
				});
				

				return null;
			}
		});
	}
}
