package com.mindoo.domino.jna.test;

import java.io.StringWriter;

import org.junit.Test;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.DatabaseOption;

import lotus.domino.Session;

public class TestLargeSummarySupport extends BaseJNATestClass {

	@Test
	public void testLargeSummarySupport() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb((db) -> {
					db.setOption(DatabaseOption.LARGE_BUCKETS_ENABLED, true);
					
					StringWriter sampleDataWriter = new StringWriter();
					produceTestData(12000, sampleDataWriter);
					String sampleData = sampleDataWriter.toString();
					
					NotesNote note = db.createNote();
					for (int i=1; i<=10; i++) {
						note.replaceItemValue("testitem"+i, sampleData);
					}
					
					//this fails with "Field is too large (32K) or View's column & selection formulas are
					//too large (error code: 561, raw error with all flags: 561)"
					//if large bucket support is not enabled
					note.update();
				});
				return null;
			}
		});
	}
}
