package com.mindoo.domino.jna.test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Tests cases for legacy API
 * 
 * @author Karsten Lehmann
 */
public class TestLegacyAPI extends BaseJNATestClass {

	/**
	 * Tests session creation as another user
	 */
	@Test
	public void testLegacyAPI_createSession() {
		final Session[] sessionAsUser = new Session[1];

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				sessionAsUser[0] = LegacyAPIUtils.createSessionAs(Arrays.asList(
						"CN=Test User/O=Mindoo",
						"Group1"
						), EnumSet.of(Privileges.FullAdminAccess));

				Database dbAsUser = sessionAsUser[0].getDatabase("", BaseJNATestClass.DBPATH_FAKENAMES_NSF);
				Document docTmp = dbAsUser.createDocument();
				Vector<?> userNamesList = sessionAsUser[0].evaluate("@UserNamesList", docTmp);
				
				Assert.assertTrue("Usernameslist of session contains specified user", userNamesList.contains("CN=Test User/O=Mindoo"));
				Assert.assertTrue("Usernameslist of session contains specified group", userNamesList.contains("Group1"));
				
				return null;
			}
		});

		boolean isRecycled = false;
		try {
			sessionAsUser[0].isOnServer();
		}
		catch (NotesException e) {
			if (e.id==4376 || e.id==4466)
				isRecycled = true;
		}
		
		Assert.assertTrue("Legacy session has been recycled by auto GC", isRecycled);
	}
}
