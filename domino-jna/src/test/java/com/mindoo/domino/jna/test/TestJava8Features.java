package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.IItemCallback;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.Session;

public class TestJava8Features extends BaseJNATestClass {

	@Test
	public void testJava8() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NotesCollection peopleView = db.openCollectionByName("People");
				
				List<NotesViewEntryData> entries = peopleView.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY),
						1, EnumSet.of(ReadMask.NOTEID), new NotesCollection.EntriesAsListCallback(1));

				NotesNote note = db.openNoteById(entries.get(0).getNoteId());
				
				AtomicInteger idx = new AtomicInteger();
				
				note.getItems(new IItemCallback() {

					@Override
					public Action itemFound(NotesItem item) {
						System.out.println("#"+idx.getAndIncrement()+"\tItem name: "+item.getName());

//						return Action.Stop;
						
						return Action.Continue;
					}
					
				});
				System.out.println();
				
				note.getItems((item,loop) -> {
					System.out.println("#"+loop.getIndex()+"\tItem name: "+item.getName()+", isfirst="+loop.isFirst()+", islast="+loop.isLast());
					loop.stop();
				});
				
				return null;
			}
		});
	}

}
