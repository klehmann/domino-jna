package com.mindoo.domino.jna.test;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.EncryptionMode;
import com.mindoo.domino.jna.constants.ItemType;

import lotus.domino.Session;

/**
 * Tests cases for note encryption
 * 
 * @author Karsten Lehmann
 */
public class TestNoteEncryption extends BaseJNATestClass {

	@Test
	public void testNoteAccess_createNoteAndEncrypt() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting encrypt note test");
				
				NotesDatabase dbData = getFakeNamesDb();
				NotesNote note = dbData.createNote();
				note.setItemValueDateTime("Calendar", Calendar.getInstance());
				note.setItemValueDateTime("JavaDate_Dateonly", new Date(), true, false);
				note.setItemValueDateTime("JavaDate_Timeonly", new Date(), false, true);
				note.setItemValueDateTime("JavaDate_DateTime", new Date(), true, true);
				note.setItemValueDouble("Double", 1.5);
				note.setItemValueString("String", "ABC", true);
				note.replaceItemValue("EncryptItem", EnumSet.of(ItemType.SEAL), "123");
				
				NotesNote encryptedNote = note.copyAndEncrypt(null, EnumSet.of(EncryptionMode.ENCRYPT_WITH_USER_PUBLIC_KEY));
				Assert.assertFalse("Original note is encrypted", note.isSealed());
				Assert.assertTrue("Copied note is encrypted", encryptedNote.isSealed());
				
				encryptedNote.decrypt(null);
				Assert.assertFalse("Encrypted note has been decrypted", encryptedNote.isSealed());
				
				System.out.println("Done with encrypt note test");
				return null;
			}
		});
	}

}
