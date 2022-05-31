package com.mindoo.domino.jna.test;

import static com.mindoo.domino.jna.dql.DQL.*;
import static org.junit.Assert.*;

import java.io.Reader;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.Encryption;
import com.mindoo.domino.jna.NotesDbQueryResult;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesQueryResultsProcessor;
import com.mindoo.domino.jna.NotesQueryResultsProcessor.Categorized;
import com.mindoo.domino.jna.NotesQueryResultsProcessor.Hidden;
import com.mindoo.domino.jna.NotesQueryResultsProcessor.SortOrder;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.constants.CreateDatabase;
import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.dql.DQL.DQLTerm;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.utils.IDUtils;

import lotus.domino.Session;

public class TestQueryResultsProcessor extends BaseJNATestClass {

	protected void withQRPTestDb(final DatabaseConsumer c) throws Exception {

		final boolean useFixedViewTestDb = "true".equalsIgnoreCase(System.getProperty("jna.qrptest.usefixeddb"));

		if (useFixedViewTestDb) {
			final String dbFilePath = "jna/testqrp.nsf";
			NotesDatabase db;
			try {
				db = new NotesDatabase("", dbFilePath, "");
			} catch (final NotesError e) {
				if (e.getId() != 259) {
					throw e;
				}

				System.out.println("Generated database " + dbFilePath);

				NotesDatabase.createDatabase("", dbFilePath, DBClass.V10NOTEFILE, true,
						EnumSet.of(CreateDatabase.LARGE_UNKTABLE), Encryption.None, 0, "QRP temp Db "+(new Date()), 
						AclLevel.MANAGER, IDUtils.getIdUsername(), true);

				db = new NotesDatabase("", dbFilePath, "");
				generateNABPersons(db, 400);
			}

			c.accept(db);
		} else {
			this.withTempDb(db -> {
				generateNABPersons(db, 400);

				c.accept(db);
			});
		}
	}
	
	@Test
	public void testQRPJsonWithWriter() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withQRPTestDb((dbQRP) -> {
					DQLTerm dql = and(
							item("form").isEqualTo("Person")
							);
					
					NotesDbQueryResult dqlResult = dbQRP.query(dql);
					NotesIDTable dqlResultTable = dqlResult.getIDTable();
					assertTrue(dqlResultTable.getCount()>0);

					StringBuilder sb = new StringBuilder();
					
					new NotesQueryResultsProcessor(dbQRP)
					.addNoteIds(dbQRP, dqlResultTable.asSet(), "people")
					.addColumn("lastname", "Lastname", "Lastname", SortOrder.DESCENDING, Hidden.FALSE, Categorized.TRUE)
					.addColumn("firstname", "Firstname", "Firstname", SortOrder.DESCENDING, Hidden.FALSE, Categorized.FALSE)
					.addColumn("_created")
					.addFormula("@Created", "_created", "people")
					.executeToJSON(sb, null);
					
					JSONObject json = new JSONObject(sb.toString());
					assertTrue(json.has("StreamResults"));
					JSONArray jsonArr = json.getJSONArray("StreamResults");
					assertTrue(jsonArr.length()>0);
				});
				return null;
			}
		});
	}
	

	@Test
	public void testQRPJsonWithReader() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withQRPTestDb((dbQRP) -> {
					DQLTerm dql = and(
							item("form").isEqualTo("Person")
							);
					
					NotesDbQueryResult dqlResult = dbQRP.query(dql);
					NotesIDTable dqlResultTable = dqlResult.getIDTable();
					assertTrue(dqlResultTable.getCount()>0);
					
					Reader jsonReader = new NotesQueryResultsProcessor(dbQRP)
					.addNoteIds(dbQRP, dqlResultTable.asSet(), "people")
					.addColumn("lastname", "Lastname", "Lastname", SortOrder.DESCENDING, Hidden.FALSE, Categorized.TRUE)
					.addColumn("firstname", "Firstname", "Firstname", SortOrder.DESCENDING, Hidden.FALSE, Categorized.FALSE)
					.addColumn("_created")
					.addFormula("@Created", "_created", "people")
					.executeToJSON(null);
					
					//very memory efficient, we're not storing the JSON string in the Java heap
					JSONObject json = new JSONObject(new JSONTokener(jsonReader));
					assertTrue(json.has("StreamResults"));
					JSONArray jsonArr = json.getJSONArray("StreamResults");
					assertTrue(jsonArr.length()>0);
					
				});
				return null;
			}
		});
	}
	
//	@Test
	public void testQRPView() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withQRPTestDb((dbQRP) -> {
					NotesDatabase db = getFakeNamesDb();
					NotesCollection viewPeople = db.openCollectionByName("People");
					LinkedHashSet<Integer> allIds = viewPeople.getAllIds(Navigate.NEXT);
					
					String qrpViewName = "qrp_"+(new Date());
					
					int viewNoteId = new NotesQueryResultsProcessor(dbQRP)
					.addNoteIds(db, allIds, "people")
					.addColumn("lastname", "Lastname", "Lastname", SortOrder.DESCENDING, Hidden.FALSE, Categorized.TRUE)
					.addColumn("firstname", "Firstname", "Firstname", SortOrder.DESCENDING, Hidden.FALSE, Categorized.FALSE)
					.addColumn("_created")
					.addFormula("@Created", "_created", "people")
					.executeToView(qrpViewName, 1, Arrays.asList(IDUtils.getIdUsername()));

					//let's check if our created QRP view has category and document rows
					NotesCollection qrpCol = dbQRP.openCollection(viewNoteId, EnumSet.of(OpenCollection.NOUPDATE));
					AtomicInteger categoryCount = new AtomicInteger();
					AtomicInteger docCount = new AtomicInteger();
					
					qrpCol.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT),
							Integer.MAX_VALUE, EnumSet.of( ReadMask.NOTEID),
							new NotesCollection.EntriesAsListCallback(Integer.MAX_VALUE))
					.forEach((entry) -> {
						if (entry.isCategory()) {
							categoryCount.incrementAndGet();
						}
						else if (entry.isDocument()) {
							docCount.incrementAndGet();
						}
					});
					qrpCol.recycle();
					
					assertTrue(categoryCount.get() > 0);
					assertTrue(docCount.get() > 0);					
				});
				
				return null;
			}
		});
	}
}
