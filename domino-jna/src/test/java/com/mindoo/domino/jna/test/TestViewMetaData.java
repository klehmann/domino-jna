package com.mindoo.domino.jna.test;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollationInfo;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesViewColumn;
import com.mindoo.domino.jna.NotesViewFormat;
import com.mindoo.domino.jna.NotesCollection.Direction;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewColumn;

/**
 * Test cases that read data from views
 * 
 * @author Karsten Lehmann
 */
public class TestViewMetaData extends BaseJNATestClass {

//	@Test
	public void testViewMetaData_columnTitleLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbDataLegacy = getFakeNamesDbLegacy();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				colFromDbData.update();

				View viewPeople = dbDataLegacy.getView("People");
				int topLevelEntriesLegacy = viewPeople.getTopLevelEntryCount();
				
				int topLevelEntries = colFromDbData.getTopLevelEntries();
				System.out.println("Top level entries: "+topLevelEntries);
				
				Assert.assertEquals("Top level entries of JNA call is equal to legacy call", topLevelEntriesLegacy, topLevelEntries);
				
				return null;
			}
		});
	}

//	@Test
	public void testViewMetaData_collations() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				colFromDbData.update();

				NotesNote viewNote = dbData.openNoteByUnid(colFromDbData.getUNID());
				List<Object> colInfo0List = viewNote.getItemValue("$Collation");
				NotesCollationInfo colInfo0 = (NotesCollationInfo) (colInfo0List==null || colInfo0List.isEmpty() ? null : colInfo0List.get(0));
				
				List<Object> colInfo1List = viewNote.getItemValue("$Collation1");
				NotesCollationInfo colInfo1 = (NotesCollationInfo) (colInfo1List==null || colInfo1List.isEmpty() ? null : colInfo1List.get(0));
				
				List<Object> colInfo2List = viewNote.getItemValue("$Collation2");
				NotesCollationInfo colInfo2 = (NotesCollationInfo) (colInfo2List==null || colInfo2List.isEmpty() ? null : colInfo2List.get(0));
				
				List<Object> colInfo3List = viewNote.getItemValue("$Collation3");
				NotesCollationInfo colInfo3 = (NotesCollationInfo) (colInfo3List==null || colInfo3List.isEmpty() ? null : colInfo3List.get(0));
				
				return null;
			}
		});
	}

//	@Test
	public void testViewMetaData_columns() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
//				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleWithHideWhen");
				colFromDbData.update();

				NotesNote viewNote = dbData.openNoteByUnid(colFromDbData.getUNID());
				List<Object> viewFormatList = viewNote.getItemValue("$VIEWFORMAT");
				NotesViewFormat viewFormat = (NotesViewFormat) (viewFormatList==null || viewFormatList.isEmpty() ? null : viewFormatList.get(0));
				List<NotesViewColumn> columns = viewFormat.getColumns();
				for (int i=0; i<columns.size(); i++) {
					NotesViewColumn currCol = columns.get(i);
					String currItemName = currCol.getItemName();
					String currTitle = currCol.getTitle();
					String formula = currCol.getFormula();
					
					System.out.println("Column #"+i);
					System.out.println("Item name: "+currItemName);
					System.out.println("Title: "+currTitle);
					System.out.println("Formula: "+formula);
					System.out.println("Constant: "+currCol.isConstant());
					System.out.println("Column values index: "+currCol.getColumnValuesIndex());
				}
				return null;
			}
		});
	}
	
	@Test
	public void testViewMetaData_catView() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbDataLegacy = getFakeNamesDbLegacy();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("ViewDesignTest");
				colFromDbData.update();

				Set<String> colValues = colFromDbData.getColumnValues("Lastname", Locale.GERMAN);
				System.out.println(colValues);

				List<NotesViewColumn> columns = colFromDbData.getColumns();
				
				View viewDesignTest = dbDataLegacy.getView("ViewDesignTest");
				Vector<ViewColumn> columnsLegacy = viewDesignTest.getColumns();
				
				Assert.assertEquals("No of view column correct",  columnsLegacy.size(), columnsLegacy.size());
				
				for (int i=0; i<columns.size(); i++) {
					NotesViewColumn currCol = columns.get(i);
					int pos = currCol.getPosition();
					int colValuesIdx = currCol.getColumnValuesIndex();
					String title = currCol.getTitle();
					boolean isCategory = currCol.isCategory();
					System.out.println("Column #"+pos+", title="+title);
					System.out.println("getColumnValuesIndex = "+colValuesIdx);
					System.out.println("isCategory = "+isCategory);

					ViewColumn currColLegacy = columnsLegacy.get(i);
					
					Assert.assertEquals("Position correct", currCol.getPosition(), currColLegacy.getPosition());
					Assert.assertEquals("Column values index correct", currCol.getColumnValuesIndex(), currColLegacy.getColumnValuesIndex());
					Assert.assertEquals("Item name correct", currCol.getItemName(), currColLegacy.getItemName());
					Assert.assertEquals("Title correct", currCol.getTitle(), currColLegacy.getTitle());
					Assert.assertEquals("Formula correct", currCol.getFormula(), currColLegacy.getFormula());
					
				}
				
				return null;
			}
		});
	}
}