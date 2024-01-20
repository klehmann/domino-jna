package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;

import com.mindoo.domino.jna.NotesIDTable;

import lotus.domino.Session;

public class IDTableInsertRangeTest extends BaseJNATestClass {

	/**
	 * Tests correct insertion of multiple note ids into the IDTable
	 */
	@Test
	public void testIDTableAddNotes() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				int[] noteIdsToInsert = new int[] {-2147478912, 299462, 193030, 192518, -2147478908, -2147478904, 192330, 229642, 248586, 4618, -2147478900, 238094, 248590, -2147478896, 192402, 242898, -2147478892, 192538, -2147478888, 232794, -2147478884, -2147478880, 193382, 193958, 242790, 192554, 194858, 194026, 192622, 224494, 238130, 194038, 283698, -2147478916};
				
				List<Integer> noteIdsToInsertAsList = Arrays
						.stream(noteIdsToInsert)
						.mapToObj((id) -> { return Integer.valueOf(id); })
						.collect(Collectors.toList());
				
				NotesIDTable idTable = new NotesIDTable();
				idTable.addNotes(noteIdsToInsertAsList);

				List<Integer> idsViaToArray = Arrays.stream(idTable.toArray()).mapToObj((id) -> { return Integer.valueOf(id); }).collect(Collectors.toList());
				
				List<Integer> idsViaIterator = StreamSupport.stream(idTable.spliterator(), false).collect(Collectors.toList());
				
				assertEquals(idsViaToArray, idsViaIterator);
				
				assertEquals(noteIdsToInsertAsList.size(), idsViaToArray.size());
				
				//make sure all ids from noteIdsToInsertAsList are in idsViaToArray
				for (Integer currId : noteIdsToInsertAsList) {
					assertTrue(idsViaToArray.contains(currId));
				}
				
				return null;
			}
		});
	}
	
}
