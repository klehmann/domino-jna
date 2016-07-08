package com.mindoo.domino.jna.test;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.sun.jna.Pointer;

import lotus.domino.Session;

/**
 * Tests cases for string utilities
 * 
 * @author Karsten Lehmann
 */
public class TestIdTable extends BaseJNATestClass {

	/**
	 * Tests reading the replication info from a database and conversion functions between
	 * innards arrays and hex strings
	 */
	@Test
	public void testStringUtils_replicaIdInnards() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting id table test");
				
				NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

				NotesIDTable table = new NotesIDTable();

//				table.addNote(Integer.parseInt("1CAF", 16));
				long t0=System.currentTimeMillis();
				int currNoteId = 1;
				for (int i=0; i<40000; i++) {
					table.addNote(currNoteId);
					currNoteId += 1;
				}
				
//				int currNoteId = 40000;
//				for (int i=40000; i>=0; i--) {
//					table.addNote(currNoteId);
//					currNoteId -= 1;
//				}

//				currNoteId = 40000;
//				for (int i=40000; i>=0; i--) {
//					table.addNote(currNoteId);
//					currNoteId -= 1;
//				}
long t1=System.currentTimeMillis();

				System.out.println("Entries: "+table.getCount()+" after "+(t1-t0)+"ms");
				
				Pointer ptr;
				if (NotesJNAContext.is64Bit()) {
					ptr = notesAPI.b64_OSLockObject(table.getHandle64());
				}
				else {
					ptr = notesAPI.b32_OSLockObject(table.getHandle32());
				}
				
				try {
					int sizeInBytes;
					if (NotesJNAContext.is64Bit()) {
						sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
					}
					else {
						sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
					}
					
					System.out.println("sizeInBytes: "+sizeInBytes);
					
					if (sizeInBytes>0) {
						ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
						for (int i=0; i<sizeInBytes; i++) {
							if (i>0) {
								System.out.print(" ");
							}
							System.out.print(Integer.toHexString(buf.get(i)));
						}
						System.out.println();
					}
				}
				finally {
					if (NotesJNAContext.is64Bit()) {
						notesAPI.b64_OSUnlockObject(table.getHandle64());
					}
					else {
						notesAPI.b32_OSUnlockObject(table.getHandle32());
					}
				}
			
				System.out.println("Done with id table test");
				return null;
			}
		});
	}

}
