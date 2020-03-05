package com.mindoo.domino.jna.test;

import java.util.EnumSet;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.SignCallback;
import com.mindoo.domino.jna.NotesUserId;
import com.mindoo.domino.jna.NotesViewEntryData;
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
				NotesDatabase db = new NotesDatabase(session, "", "test/signtest.nsf");
				db.signAll(EnumSet.of(NoteClass.ALLNONDATA), new SignCallback() {

					@Override
					public boolean shouldSign(NotesViewEntryData noteData, String currSigner) {
						return true;
					}

					@Override
					public boolean shouldReadSummaryDataFromDesignCollection() {
						return true;
					}
					
					@Override
					public Action noteSigned(NotesViewEntryData noteData) {
						System.out.println("Note signed: ID="+noteData.getNoteId()+", data="+noteData.getColumnDataAsMap());
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
				NotesDatabase db = new NotesDatabase(session, "", "test/signtest.nsf");
				
				String userNameToSign = System.getenv("SIGN_USERNAME");
				String userPasswordToSign = System.getenv("SIGN_PASSWORD");
				String idVaultServer = System.getenv("SIGN_IDVAULTSERVER");
				
				if (StringUtil.isEmpty(userNameToSign) || StringUtil.isEmpty(userPasswordToSign)
						|| StringUtil.isEmpty(idVaultServer)) {
					System.out.println("SIGN_USERNAME / SIGN_PASSWORD / SIGN_IDVAULTSERVER must be set in environment to sign DB as someone else");
					return null;
				}
				
				//fetch id from vault
				NotesUserId userId = IDUtils.getUserIdFromVault(userNameToSign, userPasswordToSign, idVaultServer);
				
				//probably not required for signing design elements:
				boolean signNotesIfMimePresent = false;
				
				db.signAll(userId, EnumSet.of(NoteClass.ALLNONDATA), signNotesIfMimePresent,
						new SignCallback() {

					@Override
					public boolean shouldSign(NotesViewEntryData noteData, String currSigner) {
						return true;
					}

					@Override
					public boolean shouldReadSummaryDataFromDesignCollection() {
						return true;
					}
					
					@Override
					public Action noteSigned(NotesViewEntryData noteData) {
						System.out.println("Note signed: ID="+noteData.getNoteId()+", data="+noteData.getColumnDataAsMap());
						return Action.Continue;
					}
				});
				
				return null;
			}
		});
	}

}
