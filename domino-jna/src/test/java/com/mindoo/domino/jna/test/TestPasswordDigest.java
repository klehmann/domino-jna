package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.utils.ServerUtils;
import com.mindoo.domino.jna.utils.ServerUtils.PasswordDigestType;

import lotus.domino.Session;

public class TestPasswordDigest extends BaseJNATestClass {

	@Test
	public void testDigest() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String password = "ABCabc123-123";
				
				String digest1 = ServerUtils.hashPassword(password, PasswordDigestType.V1);
				String digest2 = ServerUtils.hashPassword(password, PasswordDigestType.V2);
				String digest3 = ServerUtils.hashPassword(password, PasswordDigestType.V3);
				
				ServerUtils.verifyPassword(password, digest1);
				ServerUtils.verifyPassword(password, digest2);
				ServerUtils.verifyPassword(password, digest3);
				
				assertThrows(NotesError.class, () -> {
					ServerUtils.verifyPassword("xyz", digest1);
				});
				assertThrows(NotesError.class, () -> {
					ServerUtils.verifyPassword("xyz", digest2);
				});
				assertThrows(NotesError.class, () -> {
					ServerUtils.verifyPassword("xyz", digest3);
				});
				
				return null;
			}
		});
		
	}
}
