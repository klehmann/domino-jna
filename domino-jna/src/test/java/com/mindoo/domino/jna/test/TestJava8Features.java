package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.gc.NotesGC;

import lotus.domino.Session;

public class TestJava8Features extends BaseJNATestClass {

//	@Test
	public void testJava8() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NotesCollection peopleView = db.openCollectionByName("People");

				List<NotesViewEntryData> entries = peopleView.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY),
						1, EnumSet.of(ReadMask.NOTEID), new NotesCollection.EntriesAsListCallback(1));

				NotesNote note = db.openNoteById(entries.get(0).getNoteId());

				note.getItems((item,loop) -> {
					System.out.println("#"+loop.getIndex()+"\tItem name: "+item.getName()+", isfirst="+loop.isFirst()+", islast="+loop.isLast());
					loop.stop();
				});

				return null;
			}
		});
	}

	@Test
	public void testJavaDateTimeItems() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb((db) -> {
					NotesGC.setPreferNotesTimeDate(true);

					NotesNote note = db.createNote();
					LocalDate ld;
					{
						ld = LocalDate.of(2021, 4, 5);
						note.replaceItemValue("date", ld);
						NotesTimeDate ndt = note.getItemValueAsTimeDate("date");
						assertNotNull(ndt);
						assertTrue(ndt.hasDate());
						assertFalse(ndt.hasTime());
						assertEquals(ld, ndt.toLocalDate());
					}
					LocalTime lt;
					{
						int hundredthSec = 50;
						int milliSec = hundredthSec*10;
						int nanoSec = milliSec* 1000000;
						lt = LocalTime.of(13, 54, 34, nanoSec);
						note.replaceItemValue("time", lt);
						NotesTimeDate ndt = note.getItemValueAsTimeDate("time");
						assertNotNull(ndt);
						assertFalse(ndt.hasDate());
						assertTrue(ndt.hasTime());
						assertEquals(lt, ndt.toLocalTime());
					}
					{
						OffsetDateTime odt = OffsetDateTime.of(ld, lt, ZoneOffset.UTC);
						note.replaceItemValue("offsetdatetime", odt);
						NotesTimeDate ndt = note.getItemValueAsTimeDate("offsetdatetime");
						assertNotNull(ndt);
						assertTrue(ndt.hasDate());
						assertTrue(ndt.hasTime());
						assertEquals(odt, ndt.toOffsetDateTime());
					}
					{
						ZonedDateTime zdt = ZonedDateTime.of(ld, lt, ZoneId.of("Asia/Kolkata"));
						note.replaceItemValue("zoneddatetime", zdt);
						NotesTimeDate ndt = note.getItemValueAsTimeDate("zoneddatetime");
						assertNotNull(ndt);
						assertTrue(ndt.hasDate());
						assertTrue(ndt.hasTime());
						assertEquals(zdt.toOffsetDateTime(), ndt.toOffsetDateTime());
					}
				});
				return null;
			}
		});
	}
}
