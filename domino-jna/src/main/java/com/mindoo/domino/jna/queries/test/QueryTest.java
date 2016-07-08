package com.mindoo.domino.jna.queries.test;

import static com.mindoo.domino.jna.queries.condition.ColumnLookup.column;
import static com.mindoo.domino.jna.queries.condition.IDTableLookup.noteIdsContain;
import static com.mindoo.domino.jna.queries.condition.Operator.and;
import static com.mindoo.domino.jna.queries.condition.Operator.or;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.queries.condition.Relation;

public class QueryTest implements Relation {

	public void test(NotesCollection collection) {
		Iterator<NotesViewEntryData> entries = 
				collection.select(
						"firstname", "lastname", "city")
				.where(
						and (
								column("firstname", StartsWith, "kar"),
								column("lastname", StartsWith, "leh"),
								or (
										column("ZIP", Equals, 76189),
										column("ZIP", GreaterThan, 76200),
										noteIdsContain(2302, 2310),
										column("ZIP", NotEquals, 1234)
										)
								)
						)
				.orderBy("column")
				.skip(50)
				.count(100)
				.iterator();




		Iterator<NotesViewEntryData> entries2 = 
				collection.select(
						"firstname", "lastname", "city")
				.setFilter(
						column("firstname", StartsWith, "tam")
						)
				.setSkip(50)
				.setCount(100)
				.setOrderBy("column")
				.iterator();
	}
}
