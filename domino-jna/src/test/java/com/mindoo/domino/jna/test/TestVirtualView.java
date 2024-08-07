package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.mindoo.domino.jna.INoteSummary;
import com.mindoo.domino.jna.IViewColumn.ColumnSort;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.utils.NotesIniUtils;
import com.mindoo.domino.jna.utils.NotesMarkdownTable;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualView.CategorizationStyle;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Category;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Hidden;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Total;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumnValueFunction;
import com.mindoo.domino.jna.virtualviews.VirtualViewEntryData;
import com.mindoo.domino.jna.virtualviews.VirtualViewFactory;
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator;
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator.SelectedOnly;

import lotus.domino.Session;

public class TestVirtualView extends BaseJNATestClass {

	/**
	 * Sample to compute sums/averages, use categories and fetch additional data from an external source
	 * (poor man's join)
	 */
//	@Test
	public void testDataJoin() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				Map<String,String> someExternalData = new HashMap<>();
				someExternalData.put("Revoco", "Revoco Street 5, Los Angeles");
				someExternalData.put("Omnis", "Omnis Boulevard 12, New York");

				long update_t0=System.currentTimeMillis();

				//by using "createViewOnce", we mark the view to be stored in memory, as a version "1" and to auto discard it
				//after 5 minute of inactivity (just for testing, in production you'd use a higher value)
				
				//changing the version number to "2" would force a new view to be created
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_origindb_namelenghts",
						1, // version "1"
						5, TimeUnit.MINUTES, // auto discard after 5 minute of inactivity (calling createViewOnce resets the counting)
						(id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Lastname", "Lastname",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname"),

							new VirtualViewColumn("Total Name Length", "TotalNameLength",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.SUM,
									new VirtualViewColumnValueFunction<Integer>(1) { // this 1 is a version number for the column function, might become relevant later when we store the index to disk

								@Override
								public Integer getValue(String origin, String itemName,
										INoteSummary columnValues) {
									return columnValues.getAsString("Firstname", "").length() + 1 +
											columnValues.getAsString("Lastname", "").length();
								}
							}),

							new VirtualViewColumn("Average Name Length", "AverageNameLength",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.AVERAGE,
									new VirtualViewColumnValueFunction<Integer>(1) {


								@Override
								public Integer getValue(String origin, String itemName,
										INoteSummary columnValues) {
									return columnValues.getAsString("Firstname", "").length() + 1 +
											columnValues.getAsString("Lastname", "").length();
								}
							}),
							
							new VirtualViewColumn("Company Address", "CompanyAddress",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									new VirtualViewColumnValueFunction<String>(1) {

										@Override
										public String getValue(String origin, String itemName,
												INoteSummary columnValues) {
											//poor man's JOIN :-)
											//we fetch the company address from a map using the company name as key
											
											String companyName = columnValues.getAsString("CompanyName", "");
											return someExternalData.getOrDefault(companyName, "");
										}
									}),

							new VirtualViewColumn("Last Update", "LastMod",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									"LastMod"),

							//required to have the CompanyName value available in the summary buffer so that the Java column function can use it
							new VirtualViewColumn("Company Name", "CompanyName",
									Category.NO, Hidden.YES, ColumnSort.NONE, Total.NONE,
									"CompanyName")

							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\"")
							.build();
				});
				
				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				Thread.sleep(5000);
				VirtualViewFactory.INSTANCE.cleanupExpiredViews();
				
				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator nav = view
						.createViewNav()
						.withCategories()
						.withDocuments()
						.build()
						.expandAll();

				//render the view as markdown table to the console
				new NotesMarkdownTable(nav, pWriter)
				.addColumn(NotesMarkdownTable.EXPANDSTATE)
				.addColumn(NotesMarkdownTable.POS)
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addColumn(NotesMarkdownTable.NOTEID)
				.addColumn(NotesMarkdownTable.UNID)
				.addAllViewColumns()

				.printHeader()
				.printRows(nav.entriesForward(SelectedOnly.NO)) // convenience function that navigates the view and returns all expanded entries as a Stream
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);


				return null;
			}
		});

	}
	
	/**
	 * Sample to compute sums/averages, use categories and fetch additional data from an external source
	 * (poor man's join)
	 */
//	@Test
	public void testForOriginAndReaders() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_origindb_namelenghts",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Origin", "Origin",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									new VirtualViewColumnValueFunction<Object>(1) {

								@Override
								public Object getValue(String origin, String itemName,
										INoteSummary columnValues) {
									return origin;
								}
							}),
							
							new VirtualViewColumn("Readers", "Readers",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									new VirtualViewColumnValueFunction<Object>(1) {

								@Override
								public Object getValue(String origin, String itemName,
										INoteSummary columnValues) {
									//readers are stored by NIF/NSFSearch in an undocumented multi-value field
									List<String> readers = columnValues.getAsStringList("$C1$", null);
									if (readers==null || readers.isEmpty()) {
										return "All";
									} else {
										return readers;
									}
								}
							}),

							new VirtualViewColumn("Lastname", "Lastname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")

							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\"")
							.build();
				});
				
				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator nav = view
						.createViewNav()
						.withCategories()
						.withDocuments()
						.build()
						.expandAll();

				new NotesMarkdownTable(nav, pWriter)
				.addColumn(NotesMarkdownTable.EXPANDSTATE)
				.addColumn(NotesMarkdownTable.POS)
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addColumn(NotesMarkdownTable.NOTEID)
				.addColumn(NotesMarkdownTable.UNID)
				.addAllViewColumns()

				.printHeader()
				.printRows(nav.entriesForward(SelectedOnly.NO))
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);

				return null;
			}
		});

	}

	/**
	 * To hide empty categories, we accumulate the readers lists of documents in their parent categories
	 * together with a count, plus a separate count of documents that have no readers at all.<br>
	 * <br>
	 * This test case reads these collected information and displays them in the markdown table.
	 */
//	@Test
	public void testForCategoryReaders() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_origindb_readerstats",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Origin", "Origin",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									new VirtualViewColumnValueFunction<Object>(1) {

								@Override
								public Object getValue(String origin, String itemName,
										INoteSummary columnValues) {
									return origin;
								}
							}),

							new VirtualViewColumn("Lastname", "Lastname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")

							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\"")
							.build();
				});
				
				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				VirtualViewEntryData rootEntry = view.getRoot();
				System.out.println("Reader items stats for the whole view:");
				for (String currOrigin : rootEntry.getCategoryReadersListOrigins()) {
					System.out.println("Origin: " + currOrigin);
					
					Collection<String> readers = rootEntry.getCategoryReadersList(currOrigin);
					if (readers == null) {
                        System.out.println("  All readers");
                    }
                    else {
                        System.out.println("  " + readers.stream().collect(Collectors.joining(", ")));
                    }
					System.out.println("-----");
				}
				
				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator nav = view
						.createViewNav()
						.withCategories()
						.withDocuments()
						.build()
						.expandAll();

				new NotesMarkdownTable(nav, pWriter)
				.addColumn(NotesMarkdownTable.EXPANDSTATE)
				.addColumn(NotesMarkdownTable.POS)
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addColumn(NotesMarkdownTable.NOTEID)
				.addColumn(NotesMarkdownTable.UNID)
				.addColumn("Readers", 70, (table, entry) -> {
					if (entry instanceof VirtualViewEntryData) {
						VirtualViewEntryData virtualEntry = (VirtualViewEntryData) entry;
						
						if (entry.isDocument()) {
							Collection<String> readers = virtualEntry.getDocReadersList();
							if (readers == null) {
								return "";							
							}
							else {
								return readers.stream().collect(Collectors.joining(", "));
							}							
						}
						else if (entry.isCategory()) {
							TreeSet<String> readers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
							for (String currOrigin : virtualEntry.getCategoryReadersListOrigins()) {
								Collection<String> currReaders = virtualEntry.getCategoryReadersList(currOrigin);
								if (currReaders != null) {
									readers.addAll(currReaders);
								}
							}
							return readers.stream().collect(Collectors.joining(", "));
						}
					}
					return "";
				})
				.addAllViewColumns()

				.printHeader()
				.printRows(nav.entriesForward(SelectedOnly.NO))
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);

				return null;
			}
		});

	}
	
//	@Test
	public void testRangeLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("keylookup1",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Lastname", "Lastname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")

							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\" & Lastname!=\"\" & Firstname!=\"\"")
							.build();
				});
				
				//test with small dataset first

				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator nav = view
						.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.build()
						.expandAll();
				
				new NotesMarkdownTable(nav, pWriter)
				.addAllViewColumns()

				.printHeader()
				.printRows(nav.childDocumentsBetween(view.getRoot(), "Aberna", "B", false))
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);


				return null;
			}
		});

	}

//	@Test
	public void testKeyLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("keylookup1",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Lastname", "Lastname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")

							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\" & Lastname!=\"\" & Firstname!=\"\"")
							.build();
				});
				
				//test with small dataset first

				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator nav = view
						.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.build()
						.expandAll();
				
				new NotesMarkdownTable(nav, pWriter)
				.addAllViewColumns()

				.printHeader()
				.printRows(nav.childDocumentsByKey(view.getRoot(), "Abe", false, false))
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);


				return null;
			}
		});

	}


//	@Test
	public void testWithTemplateView() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = new NotesDatabase("", DBPATH_FAKENAMES_NSF, "");
				NotesCollection col = db.openCollectionByName("People");
				
				VirtualView virtualView = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_people", 
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createViewFromTemplate(col)
					.withDbSearch("fakenames1", "", DBPATH_FAKENAMES_NSF,
							col.getSelectionFormula())
					.build();
				});
				
				VirtualViewNavigator virtualViewNav = virtualView.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.build()
						.expandAll();

				new NotesMarkdownTable(virtualViewNav, System.out)
				.addAllStandardColumns()
				.addAllViewColumns()
				.printHeader()
				.printRows(virtualViewNav.entriesForward(SelectedOnly.NO))
				.printFooter();
				
				return null;
			}
		});
	}	

//	@Test
	public void testFakenameDesignViews() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();

				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_design_views", 
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("View title", "ViewTitle",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"$TITLE"),

							new VirtualViewColumn("Creation Date", "CreationDate",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									"@Created")

							)
							.withDbSearch("myfakenamesviews1", "", DBPATH_FAKENAMES_NSF, null, EnumSet.of(NoteClass.VIEW),
									null, null,
									null,
									null)
							.build();
				});

				long update_t1=System.currentTimeMillis();
				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				long nav_t0=System.currentTimeMillis();
				
				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				VirtualViewNavigator virtualViewNav = view.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.build()
						.expandAll();
				
				new NotesMarkdownTable(virtualViewNav, pWriter)
				.addAllStandardColumns()
				.addAllViewColumns()
				.printHeader()
				.printRows(virtualViewNav.entriesForward(SelectedOnly.NO))
				.printFooter();
				
				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);
				
				return null;
			}
		});
	}
	
//	@Test
	public void testMailProfileDocs() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String mailServer = NotesIniUtils.getEnvironmentString("MailServer");
				String mailFile = NotesIniUtils.getEnvironmentString("MailFile");

				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("mail_profiledocs_byusername1",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Profile name", "ProfileName",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									new VirtualViewColumnValueFunction<String>(1) {

										@Override
										public String getValue(String origin, String itemName,
												INoteSummary columnValues) {
											return columnValues.getProfileName();
										}
									}),

							new VirtualViewColumn("Profile username", "ProfileUsername",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									new VirtualViewColumnValueFunction<String>(1) {

										@Override
										public String getValue(String origin, String itemName,
												INoteSummary columnValues) {
											return columnValues.getProfileUserName();
										}
									}),
							
							new VirtualViewColumn("Creation Date", "CreationDate",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									"@Created"),
							
							//add $Name column so that it's read from the doc summary buffer and available in our Java column functions
							new VirtualViewColumn("$Name", "$Name",
									Category.NO, Hidden.YES, ColumnSort.NONE, Total.NONE,
									"$Name")
							)

							.withProfileDocs("mailprofiles", mailServer, mailFile,
									"@Contains($name;\"admin lehmann\")", null, null)
							.build();
				});

				
				VirtualViewNavigator virtualViewNav = view.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.build()
						.expandAll();
				
				new NotesMarkdownTable(virtualViewNav, System.out)
				.addAllStandardColumns()
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addAllViewColumns()
				.printHeader()
				.printRows(virtualViewNav.entriesForward(SelectedOnly.NO))
				.printFooter();
				
				return null;
			}
		});
	}

	/**
	 * Categorizes the fulltext search results for "Newsletter" in the mail database by year and month
	 */
//	@Test
	public void testMailFTSearch() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String ftQuery = "Newsletter";
				
				String mailServer = NotesIniUtils.getEnvironmentString("MailServer");
				String mailFile = NotesIniUtils.getEnvironmentString("MailFile");

				long update_t0=System.currentTimeMillis();

				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("mail_ftsearch_"+ftQuery, 
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							
							new VirtualViewColumn("Year", "$Year",
									Category.YES, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"@Year(@Created)"),

							new VirtualViewColumn("Month", "$Month",
									Category.YES, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"@Month(@Created)"),

							new VirtualViewColumn("Creation Date", "CreationDate",
									Category.NO, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"@Created"),

							new VirtualViewColumn("Subject", "Subject",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									"Subject")
							)
	
							.withDbSearch("maildocs", mailServer, mailFile, "Form=\"Memo\"",
									EnumSet.of(NoteClass.DATA),
									ftQuery, null,
									null,
									null)
							.build();
				});

				long update_t1=System.currentTimeMillis();
								
				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator virtualViewNav = view.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.build()
						.expandAll();
				
				new NotesMarkdownTable(virtualViewNav, pWriter)
				.addAllStandardColumns()
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addAllViewColumns()
				.printHeader()
				.printRows(virtualViewNav.entriesForward(SelectedOnly.NO))
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);
				
				return null;
			}
		});
	}
	
	/**
	 * Same as {@link #testMailFTSearch()}, but only shows entries for the year 2024
	 * (single category view)
	 */
//	@Test
	public void testMailFTSearchSingleCategory() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String ftQuery = "Newsletter";
				
				String mailServer = NotesIniUtils.getEnvironmentString("MailServer");
				String mailFile = NotesIniUtils.getEnvironmentString("MailFile");

				long update_t0=System.currentTimeMillis();

				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("mail_ftsearch_"+ftQuery, 
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							
							new VirtualViewColumn("Year", "$Year",
									Category.YES, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"@Year(@Created)"),

							new VirtualViewColumn("Month", "$Month",
									Category.YES, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"@Month(@Created)"),

							new VirtualViewColumn("Creation Date", "CreationDate",
									Category.NO, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"@Created"),

							new VirtualViewColumn("Subject", "Subject",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									"Subject")
							)
	
							.withDbSearch("maildocs", mailServer, mailFile, "Form=\"Memo\"",
									EnumSet.of(NoteClass.DATA),
									ftQuery, null,
									null,
									null)
							.build();
				});

				long update_t1=System.currentTimeMillis();
								
				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);

				long nav_t0=System.currentTimeMillis();

				VirtualViewNavigator virtualViewNav = view.createViewNav()
						.withEffectiveUserName(session.getEffectiveUserName())
						.buildFromCategory(Arrays.asList(2024))
						.expandAll();
				
				new NotesMarkdownTable(virtualViewNav, pWriter)
				.addAllStandardColumns()
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addAllViewColumns()
				.printHeader()
				.printRows(virtualViewNav.entriesForward(SelectedOnly.NO))
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				System.out.println(sWriter);
				
				return null;
			}
		});
	}
	
//	@Test
	public void testVirtualViewWithFolder() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String folderName = "virtualviewtest";
				
				NotesDatabase db = getFakeNamesDb();
				NotesCollection folder = db.openCollectionByName(folderName);
				if (folder == null) {
					db.createFolder(folderName);
				}
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("folderdocs", 
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Letter", "$1", 
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"@Left(Lastname;1) + \"\\\\\" + @Left(Lastname;2)"),

							new VirtualViewColumn("Lastname", "Lastname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),
							
							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")
							)
							.withFolderEntries("folderdata", "", DBPATH_FAKENAMES_NSF,
									folderName, null)
							.build();
				});
				
				long update_t0=System.currentTimeMillis();
				
				for (int i=0; i<10; i++) {
					long updateFromFolder_t0=System.currentTimeMillis();
					
					System.out.println("Updating from folder " + folderName);
					view.update("folderdata");

					long updateFromFolder_t1=System.currentTimeMillis();
					System.out.println("Time to update from folder: "+(updateFromFolder_t1-updateFromFolder_t0)+"ms");

					long nav_t0=System.currentTimeMillis();
					
					VirtualViewNavigator nav = view.createViewNav()
							.withEffectiveUserName(session.getEffectiveUserName())
							.build()
							.expandAll();

					new NotesMarkdownTable(nav, System.out)
					.addAllStandardColumns()
					.addColumn(NotesMarkdownTable.CATEGORY)
					.addAllViewColumns()
					.printHeader()
					.printRows(nav.entriesForward(SelectedOnly.NO))
					.printFooter();
					
					long nav_t1=System.currentTimeMillis();
					System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");
					
					System.out.println("Waiting 5s...");
					Thread.sleep(5000);
				}
				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");
				
				return null;
			}
		});
		
	}
	
	/**
	 * Sample to just read category structures from the virtual view and skip any documents
	 */
//	@Test
	public void testNavigatorWithCategoriesOnly() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_origindb_lastnamefirstname_catsonly",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Lastname", "Lastname",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")



							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\"")
							.withCategorizationStyle(CategorizationStyle.DOCUMENT_THEN_CATEGORY)
							.build();
				});
				
				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				{
					//read the whole category structure
					StringWriter sWriter = new StringWriter();
					PrintWriter pWriter = new PrintWriter(sWriter);
					
					long nav_t0=System.currentTimeMillis();
					
					VirtualViewNavigator nav = view
							.createViewNav()
							.withCategories()
							.build()
							.expandAll();

					new NotesMarkdownTable(nav, pWriter)
					.addColumn(NotesMarkdownTable.EXPANDSTATE)
					.addColumn(NotesMarkdownTable.POS)
					.addColumn(NotesMarkdownTable.CATEGORY)
					.addColumn(NotesMarkdownTable.NOTEID)
					.addColumn(NotesMarkdownTable.UNID)
					.addAllViewColumns()

					.printHeader()
					.printRows(nav.entriesForward(SelectedOnly.NO))
					.printFooter();

					long nav_t1=System.currentTimeMillis();
					System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");
					
					System.out.println(sWriter);
				}
				
				{
					//read a single category
					StringWriter sWriter = new StringWriter();
					PrintWriter pWriter = new PrintWriter(sWriter);

					long nav_t0=System.currentTimeMillis();
					
					VirtualViewNavigator nav = view
							.createViewNav()
							.withCategories()
							.buildFromCategory("Abbott")
							.expandAll();

					new NotesMarkdownTable(nav, pWriter)
					.addColumn(NotesMarkdownTable.EXPANDSTATE)
					.addColumn(NotesMarkdownTable.POS)
					.addColumn(NotesMarkdownTable.CATEGORY)
					.addColumn(NotesMarkdownTable.NOTEID)
					.addColumn(NotesMarkdownTable.UNID)
					.addAllViewColumns()

					.printHeader()
					.printRows(nav.entriesForward(SelectedOnly.NO))
					.printFooter();

					long nav_t1=System.currentTimeMillis();
					System.out.println("Time to navigate view structure for single category Abbott: "+(nav_t1-nav_t0)+"ms");
					
					System.out.println(sWriter);
				}

				{
					//combine two category lookups
					long nav_t0=System.currentTimeMillis();
					
					VirtualViewNavigator nav1 = view
							.createViewNav()
							.withCategories()
							.buildFromCategory("Zamora")
							.expandAll();

					VirtualViewNavigator nav2 = view
							.createViewNav()
							.withCategories()
							.buildFromCategory("Adams")
							.expandAll();

					Stream<VirtualViewEntryData> stream1 = nav1.entriesForward(SelectedOnly.NO);
					Stream<VirtualViewEntryData> stream2 = nav2.entriesForward(SelectedOnly.NO);
					
					//concatenate the two streams and sort by category name (firstname)
					Stream<VirtualViewEntryData> combinedStream = Stream
							.concat(stream1, stream2)
							.sorted((e1, e2) -> String.valueOf(e1.getCategoryValue()).compareTo(String.valueOf(e2.getCategoryValue())));
					
					System.out.println("All firstnames in categories Zamora and Adams:");
					combinedStream.forEach(e -> System.out.println(e.getCategoryValue()));

					long nav_t1=System.currentTimeMillis();
					System.out.println("Time to navigate view structure for single category Abbott: "+(nav_t1-nav_t0)+"ms");
				}
				
				return null;
			}
		});

	}
	

	/**
	 * Sample to resort all docs in memory
	 */
//	@Test
	public void testManualResort() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				long update_t0=System.currentTimeMillis();
				
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_origindb_lastnamefirstname_catsonly",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							new VirtualViewColumn("Lastname", "Lastname",
									Category.NO, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname",
									Category.NO, Hidden.NO, ColumnSort.DESCENDING, Total.NONE,
									"Firstname"),

							new VirtualViewColumn("Company", "CompanyName",
									Category.NO, Hidden.NO, ColumnSort.NONE, Total.NONE,
									"CompanyName")


							)
							.withDbSearch("myfakenames1",
									"", DBPATH_FAKENAMES_NSF,
									"Form=\"Person\"")
							.withCategorizationStyle(CategorizationStyle.CATEGORY_THEN_DOCUMENT) // let's move the categories above the docs like in Windows Explorer
							.build();
				});
				
				long update_t1=System.currentTimeMillis();

				System.out.println("Time to generate view structure: "+(update_t1-update_t0)+"ms");

				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);
				
				long nav_t0=System.currentTimeMillis();
				
				VirtualViewNavigator nav = view
						.createViewNav()
						.dontShowEmptyCategories()
						.build()
						.expandAll();

				//sort 40.000 entries in memory by company name, lastname and then firstname; this is just a test to see how long it takes (1 sec on my machine);
				//this is not a recommended way to sort entries, because it consumes lots of CPU and memory, but it's possible
				Stream<VirtualViewEntryData> entries = nav
						.entriesForward(SelectedOnly.NO)
						.filter((entry) -> entry.isDocument())
						.sorted((e1, e2) -> {
							int cmp = e1.getAsString("companyname", "").compareToIgnoreCase(e2.getAsString("companyname", ""));
							if (cmp != 0) {
								return cmp;
							}
							cmp = e1.getAsString("lastname", "").compareToIgnoreCase(e2.getAsString("lastname", ""));
							if (cmp != 0) {
								return cmp;
							}
							return e1.getAsString("firstname", "").compareToIgnoreCase(e2.getAsString("firstname", ""));
						});

				new NotesMarkdownTable(nav, pWriter)
				.addColumn(NotesMarkdownTable.EXPANDSTATE)
				.addColumn(NotesMarkdownTable.POS)
				.addColumn(NotesMarkdownTable.CATEGORY)
				.addColumn(NotesMarkdownTable.NOTEID)
				.addColumn(NotesMarkdownTable.UNID)
				.addAllViewColumns()
				.addColumn("Company", 70, (table, entry) -> { return entry.getAsString("companyname", ""); })
				.addColumn("Lastname", 70, (table, entry) -> { return entry.getAsString("lastname", ""); })
				.addColumn("Firstname", 70, (table, entry) -> { return entry.getAsString("firstname", ""); })

				.printHeader()
				.printRows(entries)
				.printFooter();

				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");
				
				System.out.println(sWriter);				
				return null;
			}
		});

	}
	
//	@Test
	public void testSingleCategoryNavigatorScope() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames2_bycontinent",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(
							
							new VirtualViewColumn("Lastname letter", "$2", Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
							"@Left(Lastname;1) + \"\\\\\" + Lastname"),

							new VirtualViewColumn("Lastname", "Lastname", Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname", Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname"),

							new VirtualViewColumn("CompanyName", "CompanyName", Category.NO, Hidden.YES, ColumnSort.NONE, Total.NONE,
									"CompanyName")

							)
							.withDbSearch("myfakenames1",
									"", "fakenames.nsf",
									"Form=\"Person\"")
							.build();
				});
				
				VirtualViewNavigator nav = view
						.createViewNav()
						.dontShowEmptyCategories()
						.buildFromCategory("A") // probably "1", an any case not "5" where we will try to start reading
						.expandAll();

				Stream<VirtualViewEntryData> entries = nav.entriesForwardFromPosition("5", SelectedOnly.NO).limit(100);
				assertTrue(entries.count() == 0);

				return null;
			}
		});
	}
	

//	@Test
	public void testCustomAccessCheck() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				VirtualView view = VirtualViewFactory.INSTANCE.createViewOnce("fakenames_bylastname",
						1, 1, TimeUnit.MINUTES, (id) -> {
					return VirtualViewFactory.createView(

							new VirtualViewColumn("Lastname", "Lastname", Category.YES, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Lastname"),

							new VirtualViewColumn("Firstname", "Firstname", Category.NO, Hidden.NO, ColumnSort.ASCENDING, Total.NONE,
									"Firstname")

							)
							.withDbSearch("myfakenames",
									"", "fakenames.nsf",
									"Form=\"Person\"")
							.build();
				});
				
				long nav_t0=System.currentTimeMillis();
				
				VirtualViewNavigator nav = view
						.createViewNav()
						.dontShowEmptyCategories()
						.withCustomAccessCheck((ourNav, entry) -> {
							// simulate that we can only read entries that have a lastname starting with "A"
							if (entry.isCategory()) {
								return true;
							}
							return entry.getAsString("lastname", "").endsWith("r");
						})
						.build()
						.expandAll();

				AtomicInteger docCount = new AtomicInteger(0);
				AtomicInteger catCount = new AtomicInteger(0);
				
				nav.entriesForward().forEach(e -> {
					if (e.isCategory()) {
						catCount.incrementAndGet();
					} else {
						docCount.incrementAndGet();
						assertTrue(e.getAsString("lastname", "").endsWith("r"));
					}
				});

				assertTrue(catCount.get() > 0);
				assertTrue(docCount.get() > 0);
				
				long nav_t1=System.currentTimeMillis();
				System.out.println("Time to navigate view structure: "+(nav_t1-nav_t0)+"ms");

				return null;
			}
		});
	}
	
}
