package com.mindoo.domino.jna.test;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.transactions.ITransactionCallable;
import com.mindoo.domino.jna.transactions.RollbackException;
import com.mindoo.domino.jna.transactions.Transactions;

import lotus.domino.Session;

/**
 * Tests cases for NSF transactions
 * 
 * @author Karsten Lehmann
 */
public class TestTransactions extends BaseJNATestClass {

	/**
	 * Creates a document in the database, rolls back the transaction and checks if it has been properly rolled back
	 */
	@Test
	public void testTransactions_documentCreation() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting transaction test");

				NotesDatabase dbData = getFakeNamesDb();
				final int[] noteId = new int[1];

				try {
					Transactions.runInDbTransaction(dbData, new ITransactionCallable<Object>() {

						@Override
						public Object runInDbTransaction(NotesDatabase db) throws Exception {
							NotesNote note = db.createNote();
							note.setItemValueString("Form", "TransactionTest", true);
							note.update(EnumSet.noneOf(UpdateNote.class));
							noteId[0] = note.getNoteId();
							throw new RollbackException();
						}
					});
				}
				catch (RollbackException e) {
					//
				}

				boolean noteFound = false;
				try {
					NotesNote note = dbData.openNoteById(noteId[0], EnumSet.noneOf(OpenNote.class));
					noteFound = true;
					note.recycle();
				}
				catch (Throwable t) {
					noteFound = false;
				}
				
				Assert.assertFalse("Note creation has been rolled back", noteFound);
				return null;
			}
		});
	}


}
