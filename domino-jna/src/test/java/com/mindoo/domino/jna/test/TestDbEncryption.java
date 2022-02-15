package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.Encryption;
import com.mindoo.domino.jna.NotesDatabase.EncryptionInfo;
import com.mindoo.domino.jna.NotesDatabase.EncryptionState;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.constants.CreateDatabase;
import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.constants.DBCompact;
import com.mindoo.domino.jna.constants.DBCompact2;
import com.mindoo.domino.jna.utils.IDUtils;

import lotus.domino.Session;

/**
 * Test case for database encryption and decryption
 * 
 * @author Karsten Lehmann
 */
public class TestDbEncryption extends BaseJNATestClass {

	@Test
	public void testInvalidInnards() {
		final Encryption newEncryption = Encryption.AES128;

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				File tmpFile = File.createTempFile("jnatmp_", ".nsf");
				String tmpFilePath = tmpFile.getAbsolutePath();
				tmpFile.delete();

				NotesDatabase.createDatabase("", tmpFilePath, DBClass.V10NOTEFILE, true,
						EnumSet.of(CreateDatabase.LARGE_UNKTABLE), Encryption.None, 0,
						"Temp Db", 
						AclLevel.MANAGER, IDUtils.getIdUsername(), true);
				NotesDatabase db = null;
				try {
					Set<DBCompact> compactOptions = EnumSet.of(
							DBCompact.IGNORE_ERRORS);
					Set<DBCompact2> compactOptions2 = EnumSet.of(DBCompact2.FORCE);
					
					{
						//first we mark the database for encryption, compact it
						//and make sure it got encrypted
						db = new NotesDatabase("", tmpFilePath, "");

						EncryptionInfo encInfo = db.getLocalEncryptionInfo();

						assertNotNull(encInfo);
						assertEquals(Encryption.None, encInfo.getStrength());
						assertEquals(EncryptionState.UNENCRYPTED, encInfo.getState());

						db.setLocalEncryptionInfo(newEncryption, null);

						EncryptionInfo encInfoAfterSet = db.getLocalEncryptionInfo();

						assertEquals(EncryptionState.PENDING_ENCRYPTION, encInfoAfterSet.getState());
						assertEquals(newEncryption, encInfoAfterSet.getStrength());

						//close db
						db.recycle();

						//short syntax works as well:
						//NotesDatabase.compact(tmpFilePath);

						NotesDatabase.compact(tmpFilePath, compactOptions, compactOptions2);

						db = new NotesDatabase("", tmpFilePath, "");

						EncryptionInfo encInfoAfterCompact = db.getLocalEncryptionInfo();

						assertEquals(EncryptionState.ENCRYPTED, encInfoAfterCompact.getState());
						assertEquals(newEncryption, encInfoAfterCompact.getStrength());
					}

					{
						//next we mark the database for decryption
						//and check if this works
						db.setLocalEncryptionInfo(Encryption.None, null);

						EncryptionInfo encInfoAfterReset = db.getLocalEncryptionInfo();

						assertEquals(EncryptionState.PENDING_DECRYPTION, encInfoAfterReset.getState());
						assertEquals(newEncryption, encInfoAfterReset.getStrength());

						db.recycle();

						//short syntax works as well:
						//NotesDatabase.compact(tmpFilePath);

						NotesDatabase.compact(tmpFilePath, compactOptions, compactOptions2);

						db = new NotesDatabase("", tmpFilePath, "");

						EncryptionInfo encInfoAfterCompact2 = db.getLocalEncryptionInfo();

						assertEquals(EncryptionState.UNENCRYPTED, encInfoAfterCompact2.getState());
						assertEquals(Encryption.None, encInfoAfterCompact2.getStrength());
					}

				}
				finally {
					if (db!=null) {
						//close database and free handle
						db.recycle();
					}
					
					//cleanup temp db
					NotesDatabase.deleteDatabase("", tmpFilePath);
				}
				return null;
			}
		});
	}

}
