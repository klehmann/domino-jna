package com.mindoo.domino.jna.test;

import java.util.EnumSet;

import org.junit.Test;
import org.omg.PortableServer.ID_UNIQUENESS_POLICY_ID;

import com.mindoo.domino.jna.NotesACL;
import com.mindoo.domino.jna.NotesACL.NotesACLAccess;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.AccessInfoAndFlags;
import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.utils.IDUtils;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Session;

/**
 * Tests cases for database access checks
 * 
 * @author Karsten Lehmann
 */
public class TestDbGetAccess extends BaseJNATestClass {

	@Test
	public void testDbGetAccess() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacy = getFakeNamesDbLegacy();
				
				AccessInfoAndFlags accessInfo = dbData.getAccessInfoAndFlags();
				AclLevel aclLevel = accessInfo.getAclLevel();
				EnumSet<AclFlag> aclFlags = accessInfo.getAclFlags();
				
				int legacyAclLevel = dbLegacy.queryAccess(session.getEffectiveUserName());
				Assert.assertEquals("ACL level is the same", aclLevel.getValue(), legacyAclLevel);
				
				System.out.println("Access level: "+accessInfo.getAclLevel());
				System.out.println("Access flags: "+accessInfo.getAclFlags());
				return null;
			}
		});
	
	}
	
	@Test
	public void testDbGetACL() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacy = getFakeNamesDbLegacy();
				AccessInfoAndFlags accessInfo = dbData.getAccessInfoAndFlags();
				System.out.println("Access level: "+accessInfo.getAclLevel());
				System.out.println("Access flags: "+accessInfo.getAclFlags());
				
				NotesACL acl = dbData.getACL();
				NotesACLAccess aclAccess = acl.lookupAccess(IDUtils.getIdUsername());

				Assert.assertEquals("ACL level is equal", accessInfo.getAclLevel(), aclAccess.getAclLevel());
				Assert.assertEquals("ACL flags are equal", accessInfo.getAclFlags(), aclAccess.getAclFlags());
				Assert.assertEquals("ACL roles are equal", dbLegacy.queryAccessRoles(session.getEffectiveUserName()),
						aclAccess.getRoles());
				return null;
			}
		});
	
	}
}
