package com.mindoo.domino.jna.test;

import java.io.StringWriter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.DatabaseOption;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;

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
	
	@Test
	public void testSearchWithLargeSummarySupport() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb((db) -> {
					db.setOption(DatabaseOption.LARGE_BUCKETS_ENABLED, true);
					
					StringWriter sampleDataWriter = new StringWriter();
					produceTestData(12000, sampleDataWriter);
					String sampleData = sampleDataWriter.toString();
					
					//write note with large summary buffer
					NotesNote note = db.createNote();
					for (int i=1; i<=10; i++) {
						note.replaceItemValue("testitem"+i, sampleData);
					}
					note.replaceItemValue("Subject", "Test");
					note.update();
					
					//now prepare an NSF search where we request more summary data that it can
					//actually handle (<=V11); in this case, the NotesSearch class opens the NotesNote
					//in summary only mode internally to access the data and returns a fake
					//IItemTableData to read the values
					Map<String,String> columnFormulas = new HashMap<>();
					for (int i=1; i<=10; i++) {
						columnFormulas.put("testitem"+i, "");
					}

					AtomicInteger matchCount = new AtomicInteger();
					
					NotesSearch.search(db, null, "Subject=\"Test\"", columnFormulas, "-", 
							EnumSet.of(Search.SUMMARY),
							EnumSet.of(NoteClass.DATA), null,
							new NotesSearch.SearchCallback() {

						@Override
						public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
								IItemTableData summaryBufferData) {
							matchCount.incrementAndGet();
							
							for (int i=1; i<=10; i++) {
								String val = summaryBufferData.getAsString("testitem"+i, null);
								Assert.assertEquals(sampleData, val);
							}
							return Action.Continue;
						}
					});
					
					Assert.assertEquals(1, matchCount.get());
				});
				return null;
			}
		});
	}
}
