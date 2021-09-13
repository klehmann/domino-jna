package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesAttachment.IDataCallback;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.IAttachmentProducer;

import lotus.domino.Session;

/**
 * Test for the note attachment creation via callback that produces a
 * dynamically growing binary object in the database.<br>
 * We've had issues producing the object in the fast because reallocating
 * the object with more size deleted previously written data. The new
 * implementation handles this internally, allocates a new object and
 * copies data from the old object.
 * 
 * @author Karsten Lehmann
 */
public class TestGrowingAttachSize extends BaseJNATestClass {

	@Test
	public void testAttachFile() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb((db) -> {
					NotesNote mailNote = db.createNote();
					
					byte[] testData = produceTestData(14000000);
					
					NotesAttachment att;
					try (ByteArrayInputStream fIn = new ByteArrayInputStream(testData)) {
						att = mailNote.attachFile(new IAttachmentProducer() {

							@Override
							public void produceAttachment(OutputStream out) throws IOException {
								byte[] buffer = new byte[16384];
								int len;

								while ((len = fIn.read(buffer))>0) {
									out.write(buffer, 0, len);
								}
							}

							@Override
							public int getSizeEstimation() {
								//enforce dynamic db object resizing
								return -1;
							}
						}, "testfile.txt", new Date(), new Date());

					}

					AtomicInteger offset = new AtomicInteger();
					
					//check if file content is correct
					att.readData(new IDataCallback() {

						@Override
						public Action read(byte[] data) {
							int currOffset = offset.get();
							
							for (int i=0; i<data.length; i++) {
								assertEquals("Content mismatch at offset "+(currOffset+i), testData[currOffset + i], data[i]);
							}
							offset.addAndGet(data.length);
							
							return Action.Continue;
						}
						
					});
					
				});

				return null;
			}
		});
	}

}
