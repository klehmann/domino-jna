package com.mindoo.domino.jna.test;

import java.util.EnumSet;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.DesignElement;
import com.mindoo.domino.jna.NotesDatabase.SignCallback;
import com.mindoo.domino.jna.NotesUserId;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.StringUtil;

import lotus.domino.Session;

/**
 * Tests cases for database searches
 * 
 * @author Karsten Lehmann
 */
public class TestSignDb extends BaseJNATestClass {

//	@Test
	public void testSignDb() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String signDbServer = System.getenv("SIGN_DB_SERVER");
				if (signDbServer==null) {
					signDbServer = "";
				}
				String signDbFilePath = System.getenv("SIGN_DB_FILEPATH");

				if (StringUtil.isEmpty(signDbFilePath)) {
					System.out.println("SIGN_DB_SERVER / SIGN_DB_FILEPATH must be set in environment to run DB sign test");
					return null;
				}

				NotesDatabase db = new NotesDatabase(session, signDbServer, signDbFilePath);

				System.out.println("Signing database "+db.getServer()+"!!"+db.getRelativeFilePath()+" as "+IDUtils.getIdUsername());
				
				db.signAll(EnumSet.of(NoteClass.ALLNONDATA), new SignCallback() {

					@Override
					public boolean shouldSign(DesignElement de, String currSigner) {
						return true;
					}
					
					@Override
					public Action noteSigned(DesignElement de) {
						System.out.println("Note signed: ID="+de.getNoteId()+", data="+de);
						return Action.Continue;
					}
					
				});
				
				return null;
			}
		});
	}

	@Test
	public void testSignDbWithOtherId() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String userNameToSign = System.getenv("SIGNASUSER_USERNAME");
				String userPasswordToSign = System.getenv("SIGNASUSER_PASSWORD");
				String idVaultServer = System.getenv("SIGNASUSER_IDVAULTSERVER");
				
				if (StringUtil.isEmpty(userNameToSign) || StringUtil.isEmpty(userPasswordToSign)
						|| StringUtil.isEmpty(idVaultServer)) {
					System.out.println("SIGNASUSER_USERNAME / SIGNASUSER_PASSWORD / SIGNASUSER_IDVAULTSERVER must be set in environment to run sign as other user test");
					return null;
				}

				String signDbServer = System.getenv("SIGNASUSER_DB_SERVER");
				if (signDbServer==null) {
					signDbServer = "";
				}
				String signDbFilePath = System.getenv("SIGNASUSER_DB_FILEPATH");

				if (StringUtil.isEmpty(signDbFilePath)) {
					System.out.println("SIGNASUSER_DB_SERVER / SIGNASUSER_DB_FILEPATH must be set in environment to run sign as other user test");
					return null;
				}

				NotesDatabase db;
				if (IDUtils.isOnServer()) {
					//open DB as signer user so that $UpdatedBy contains the right value (does not work in client, maybe if we are added to the trusted servers group)
					db = new NotesDatabase(signDbServer, signDbFilePath, userNameToSign);
				}
				else {
					db = new NotesDatabase(session, signDbServer, signDbFilePath);
				}
				
				//fetch id from vault
				NotesUserId userId = IDUtils.getUserIdFromVault(userNameToSign, userPasswordToSign, idVaultServer);
				
				//probably not required for signing design elements:
				boolean signNotesIfMimePresent = false;
				
				System.out.println("Signing database "+db.getServer()+"!!"+db.getRelativeFilePath()+" as "+userId.getUsername());
				
				db.signAll(userId, EnumSet.of(NoteClass.ALLNONDATA), signNotesIfMimePresent,
						new SignCallback() {

					@Override
					public boolean shouldSign(DesignElement de, String currSigner) {
						return true;
					}
					
					@Override
					public Action noteSigned(DesignElement de) {
						System.out.println("Note signed: ID="+de.getNoteId()+", data="+de);
						return Action.Continue;
					}
				});
				
				return null;
			}
		});
	}

}
