package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.Session;

/**
 * Test case to read counts from the view index
 * 
 * @author Karsten Lehmann
 */
public class TestViewTraversalReadCounts extends BaseJNATestClass {

	@Test
	public void testReadingCounts() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection companiesView = dbData.openCollectionByName("CompaniesHierarchical");

				String[] categoriesToLookUp = new String[] {"A", "A\\Abbas", "B", "B\\Balbus", "XYZXYZ--doesnotexist--"};

				for (String category : categoriesToLookUp) {
					List<NotesViewEntryData> catEntries = companiesView.getAllEntriesInCategory(category, 0,
							//stay at the category entry:
							EnumSet.of(Navigate.CURRENT),
							//pre-buffer only one row:
							1,
							//read index counts from view index;
							//ReadMask.SUMMARYVALUES is just here so that we can print the column values
							EnumSet.of(
									ReadMask.SUMMARYVALUES,
									ReadMask.INDEXCHILDREN,
									ReadMask.INDEXDESCENDANTS,
									ReadMask.INDEXSIBLINGS
									),
							//return entry data for exactly one entry:
							new NotesCollection.EntriesAsListCallback(1)
							);

					if (!catEntries.isEmpty()) {
						NotesViewEntryData catEntry = catEntries.get(0);
						Map<String,Object> colValues = catEntry.getColumnDataAsMap();
						int descendants = catEntry.getDescendantCount();
						int siblings = catEntry.getSiblingCount();
						int children = catEntry.getChildCount();

						System.out.println("Category found: "+category);
						System.out.println("Column values: "+colValues);
						System.out.println("descendants: "+descendants);
						System.out.println("siblings: "+siblings);
						System.out.println("children: "+children);
					}
					else {
						System.out.println("Category not found: "+category);
					}
					System.out.println("==============");
				}

				return null;
			}
		});
	};


}