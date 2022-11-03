package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesBuildVersion;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.DatabaseOption;
import com.mindoo.domino.jna.constants.ItemType;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Session;

public class TestLargeSummarySupport extends BaseJNATestClass {

//	@Test
	public void testLargeSummarySupport() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withLargeDataEnabledTempDb(LargeDataLevel.R12, (db) -> {
					assertTrue(db.getOption(DatabaseOption.LARGE_BUCKETS_ENABLED));
					
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
	
//	@Test
	public void testSearchWithLargeSummarySupport() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withLargeDataEnabledTempDb(LargeDataLevel.R12, (db) -> {
					assertTrue(db.getOption(DatabaseOption.LARGE_BUCKETS_ENABLED));
					
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

	/**
	 * Writes large items lists in a doc of an R12 database, reads them and compare both
	 * values.
	 * 
	 * @throws Exception in case of errors
	 */
	@Test
	public void testLargeItemListSupport() throws Exception {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withLargeDataEnabledTempDb(LargeDataLevel.R12, (db) -> {
					assertTrue(db.getOption(DatabaseOption.LARGE_BUCKETS_ENABLED));
					assertTrue(db.getOption(DatabaseOption.LARGE_ITEMS_ENABLED));

					// Check if we're running on V12 in the abstract first.
					// This should avoid test trouble on at least V11 macOS
					{
						NotesBuildVersion buildVersion = db.getParentServerMajMinVersion();
						if (buildVersion.getMajorVersion() < 12) {
							// large item storage not supported by this API version
							return;
						}
					}
					
					//ODS 55+
					assertTrue(db.getNSFVersionInfo().getMajorVersion()>=55);

					NotesNote noteLarge = db.createNote();
					
					final StringWriter summaryTextWriter = new StringWriter();
					produceTestData(32767, summaryTextWriter);
					final String summaryText = summaryTextWriter.toString();

					List<String> largeList = new ArrayList<>();
					for (int i = 0; i < 1000; i++) {
						largeList.add(summaryText);
					}

					//non-summary item:
					NotesItem largeItem = noteLarge.replaceItemValue("textlistitem", EnumSet.noneOf(ItemType.class), largeList);
					noteLarge.update();

					final int noteId = noteLarge.getNoteId();
					noteLarge.recycle();
					noteLarge = db.openNoteById(noteId);

					Database dbLegacy = session.getDatabase("", db.getAbsoluteFilePathOnLocal(), false);
					Document doc = noteLarge.toDocument(dbLegacy);
					Item itm = doc.getFirstItem("textlistitem");
					
					List<String> testLargeList = noteLarge.getItemValueStringList("textlistitem");
					assertNotNull(testLargeList);
					assertEquals(largeList.size(), testLargeList.size());

					for (int i = 0; i < largeList.size(); i++) {
						assertEquals(largeList.get(i), testLargeList.get(i));
					}
					
				});
				
				return null;
			}
		});

	}

}
