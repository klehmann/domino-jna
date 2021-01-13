package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.OpenNote;

import lotus.domino.Session;

public class TestResponseDocs extends BaseJNATestClass {

	@Test
	public void testResponseCount() throws Exception {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb(database -> {
					String parentUnid;
					{
						NotesNote parent = database.createNote();
						parent.replaceItemValue("Form", "Parent");
						parent.update();
						assertEquals("", parent.getParentNoteUNID());
						parentUnid = parent.getUNID();
					}
					
					for(int i = 0; i < 10; i++) {
						NotesNote child = database.createNote();
						child.replaceItemValue("Form", "Child");
						child.makeResponse(parentUnid);
						child.update();
					}
					
					NotesNote parent = database.openNoteByUnid(parentUnid);
					assertEquals(10, parent.getResponseCount());
				});
				return null;
			}
		});
	}
	
	@Test
	public void testResponseTable() throws Exception {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb(database -> {
					String parentUnid;
					{
						NotesNote parent = database.createNote();
						parent.replaceItemValue("Form", "Parent");
						parent.update();
						assertEquals("", parent.getParentNoteUNID());
						parentUnid = parent.getUNID();
					}
					
					Set<String> childIds = new HashSet<>();
					
					for(int i = 0; i < 10; i++) {
						NotesNote child = database.createNote();
						child.replaceItemValue("Form", "Child");
						child.makeResponse(parentUnid);
						child.update();
						childIds.add(child.getUNID());
					}
					
					NotesNote parent = database.openNoteByUnid(parentUnid, EnumSet.of(OpenNote.RESPONSE_ID_TABLE));
					assertEquals(10, parent.getResponseCount());
					NotesIDTable children = parent.getResponses();
					Set<String> foundChildIds = children.stream()
						.map(database::openNoteById)
						.map(NotesNote::getUNID)
						.collect(Collectors.toSet());
					assertEquals(childIds, foundChildIds);
				});
				return null;
			}
		});
		
		
	}
}
