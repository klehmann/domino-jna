package com.mindoo.domino.jna.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.mindoo.domino.jna.IViewColumn.ColumnSort;
import com.mindoo.domino.jna.IViewEntryData;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewColumn;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn;
import com.mindoo.domino.jna.virtualviews.VirtualViewEntryData;
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator;

/**
 * Utility class to dump entries of a {@link NotesCollection} as a Markdown table.
 * See <a href="https://www.markdownguide.org/extended-syntax/#tables">this page</a>
 * for an example / format description.<br>
 * <br>
 * Basic usage:<br>
 * <br>
 * <code>
 * new NotesMarkdownTable(collection, System.out)<br>
 * .addColumn(NotesMarkdownTable.EXPANDSTATE)<br>
 * .addColumn(NotesMarkdownTable.POS)<br>
 * .addColumn(NotesMarkdownTable.NOTEID)<br>
 * .addColumn(NotesMarkdownTable.UNID)<br>
 * .addColumn("CustomVal1", 30, (entry) -&gt; {<br>
 * &nbsp;&nbsp;return entry.getAsString("lastname", "") + "," +entry.getAsString("firstname", "");<br>
 *  })<br>
 * .addAllViewColumns()<br>
 * .printHeader()<br>
 * .printRows(entries)<br>
 * .printFooter();<br>
 *  </code>
 * 
 * @author Karsten Lehmann
 */
public class NotesMarkdownTable {
	private NotesCollection m_realView;
	
	private VirtualViewNavigator m_virtualViewNav;
	private VirtualView m_virtualView;
	
	private Writer m_writer;
	private List<ColumnInfo> m_customColumns;

	private int m_docEntryCount;
	private int m_categoryEntryCount;
	private int m_totalEntryCount;

	/**
	 * Creates a new instance
	 * 
	 * @param view view to read column infos
	 * @param out target steam
	 */
	public NotesMarkdownTable(NotesCollection view, OutputStream out) {
		this(view, new OutputStreamWriter(out));
	}

	/**
	 * Creates a new instance
	 * 
	 * @param view view to read column infos
	 * @param writer target writer
	 */
	public NotesMarkdownTable(NotesCollection view, Writer writer) {
		m_realView = view;
		m_customColumns = new ArrayList<>();

		m_writer = writer;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param viewNav virtual view navigator to read expand states, its parent view and column infos
	 * @param out target steam
	 */
	public NotesMarkdownTable(VirtualViewNavigator viewNav, OutputStream out) {
		this(viewNav, new OutputStreamWriter(out));
	}
	

	/**
	 * Creates a new instance
	 * 
	 * @param viewNav virtual view navigator to read expand states, its parent view and column infos
	 * @param writer target writer
	 */
	public NotesMarkdownTable(VirtualViewNavigator viewNav, Writer writer) {
		m_virtualViewNav = viewNav;
		m_virtualView = viewNav.getView();
		m_customColumns = new ArrayList<>();

		m_writer = writer;
	}
	
	protected boolean isVisible(NotesViewColumn col) {
		return true;
	}

	private String format(String str, int len) {
		if (str.length() < len) {
			return StringUtil.pad(str, len, ' ', true);
		}
		else {
			return str.substring(0, len);
		}
	}

	public static class ColumnInfo {
		private String title;
		private int width;
		private BiFunction<NotesMarkdownTable, IViewEntryData,String> fct;

		public ColumnInfo(String title, int width, BiFunction<NotesMarkdownTable, IViewEntryData,String> fct) {
			this.title = title;
			this.width = width;
			this.fct = fct;
		}

		public String getTitle() {
			return title;
		}

		public int getWidth() {
			return width;
		}

		public BiFunction<NotesMarkdownTable, IViewEntryData, String> getFunction() {
			return fct;
		}

	}

	/**
	 * Method to change the width of a column
	 * 
	 * @param title column title
	 * @param width new width
	 * @return this table
	 */
	public NotesMarkdownTable setColumnWidth(String title, int width) {
		for (ColumnInfo currCol : m_customColumns) {
			if (title.equalsIgnoreCase(currCol.getTitle())) {
				currCol.width = width;
				break;
			}
		}
		return this;
	}
	
	/**
	 * Method to change the width of a column
	 * 
	 * @param idx column index (0-based)
	 * @param width new width
	 * @return this table
	 */
	public NotesMarkdownTable setColumnWidth(int idx, int width) {
		m_customColumns.get(idx).width = width;
		return this;
	}	
	
	/**
	 * Adds a computed column to the table
	 * 
	 * @param title column title
	 * @param width column width in characters
	 * @param fct function to compute the value from a {@link NotesViewEntryData}
	 * @return this table
	 */
	public NotesMarkdownTable addColumn(String title, int width, BiFunction<NotesMarkdownTable, IViewEntryData,String> fct) {
		ColumnInfo col = new ColumnInfo(title, width, fct);
		m_customColumns.add(col);
		return this;
	}
	
	/**
	 * Adds a computed column to the table, e.g.<br>
	 * <ul>
	 * <li>{@link #EXPANDSTATE}</li>
	 * <li>{@link #POS}</li>
	 * <li>{@link #NOTEID}</li>
	 * <li>{@link #UNID}</li>
	 * </ul>
	 * 
	 * @param col column
	 * @return this table
	 */
	public NotesMarkdownTable addColumn(ColumnInfo col) {
		m_customColumns.add(col);
		return this;
	}
	
	/**
	 * Adds the columns {@link #EXPANDSTATE}, {@link #POS}, {@link #NOTEID} and {@link #UNID}.
	 * 
	 * @return this table
	 */
	public NotesMarkdownTable addAllStandardColumns() {
		addColumn(EXPANDSTATE);
		addColumn(POS);
		addColumn(NOTEID);
		addColumn(UNID);
		return this;
	}
	
	/**
	 * Adds all columns of the view
	 * 
	 * @return this table
	 */
	public NotesMarkdownTable addAllViewColumns() {
		if (m_realView != null) {
			m_realView
			.getColumns()
			.stream()
			.map((col) -> { return col.getItemName(); } )
			.forEach(this::addViewColumn);			
		}
		else {
			m_virtualView
			.getColumns()
			.stream()
			.map((col) -> { return col.getItemName(); })
			.forEach(this::addViewColumn);
		}
		
		return this;
	}
	
	/**
	 * Adds a column of the view to the table
	 * 
	 * @param itemName programmatic item name
	 * @return this table
	 */
	public NotesMarkdownTable addViewColumn(String itemName) {
		return addViewColumn(itemName, 40);
	}
	
	/**
	 * Adds a column of the view to the table
	 * 
	 * @param itemName programmatic item name
	 * @param width column width in characters
	 * @return this table
	 */
	public NotesMarkdownTable addViewColumn(String itemName, int width) {
		String title;
		if (m_realView != null) {
			NotesViewColumn col = m_realView
					.getColumns()
					.stream()
					.filter((currCol) -> {
						return itemName.equalsIgnoreCase(currCol.getItemName());
					})
					.findFirst()
					.orElseThrow(
							() -> new IllegalArgumentException(MessageFormat.format("Column {0} not found", itemName)));

			String sortingArrow = col.getSorting() == ColumnSort.ASCENDING ? "↑" : col.getSorting() == ColumnSort.DESCENDING ? "↓" : "";
			
			title = (col.isHidden() ? "[" : "") + col.getTitle() + (col.isHidden() ? "]" : "") +  sortingArrow + " (" + col.getItemName() + ")";
		} else {
			VirtualViewColumn col = m_virtualView
					.getColumns()
					.stream()
					.filter((currCol) -> {
						return itemName.equalsIgnoreCase(currCol.getItemName());
					})
					.findFirst()
					.orElseThrow(
							() -> new IllegalArgumentException(MessageFormat.format("Column {0} not found", itemName)));

			String sortingArrow = col.getSorting() == ColumnSort.ASCENDING ? "↑" : col.getSorting() == ColumnSort.DESCENDING ? "↓" : "";

			title = (col.isHidden() ? "[" : "") + col.getTitle() + (col.isHidden() ? "]" : "") +  sortingArrow + " (" + col.getItemName() + ")";
		}
		
		return addColumn(title, width, (table, entry) -> {
			Object val = entry.get(itemName);
			return valueToString(val);
		});
		
	}
	
	/**
	 * Prints the table header
	 * 
	 * @return this table
	 */
	public NotesMarkdownTable printHeader() {
		try {
			StringBuilder sbLine1 = new StringBuilder();
			StringBuilder sbLine2 = new StringBuilder();

			int idx=0;

			sbLine1.append("| ");
			sbLine2.append("| ");


			for (ColumnInfo currCol : m_customColumns) {
				String colTitle = currCol.getTitle();
				int colWidth = currCol.getWidth();

				if (idx>0) {
					//close column to the left
					sbLine1.append(" |");
					sbLine2.append(" |");
				}
				sbLine1.append(" ").append(format(colTitle, colWidth));
				sbLine2.append(" ").append(StringUtil.repeat('-', colWidth));

				idx++;
			}

			//close last column
			sbLine1.append(" |");
			sbLine2.append(" |");

			m_writer.write(sbLine1.toString());
			m_writer.write("\n");
			m_writer.write(sbLine2.toString());
			m_writer.write("\n");
			m_writer.flush();

			return this;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public NotesMarkdownTable printRows(Stream<? extends IViewEntryData> entries) {
		entries.forEach(this::printRow);
		return this;
	}

	public NotesMarkdownTable printRows(Collection<? extends IViewEntryData> entries) {
		for (IViewEntryData currEntry : entries) {
			printRow(currEntry);
		}
		return this;
	}

	public NotesMarkdownTable printRow(IViewEntryData entry) {
		try {
			if (entry instanceof NotesViewEntryData) {
				((NotesViewEntryData)entry).setPreferNotesTimeDates(true);
			}

			int idx=0;

			m_writer.write("| ");
			
			for (ColumnInfo currCol : m_customColumns) {
				int colWidth = currCol.getWidth();
				String colValue = currCol.getFunction().apply(this, entry);
				if (colValue==null) {
					colValue = "";
				}

				if (idx>0) {
					//close column to the left
					m_writer.write(" |");
				}
				m_writer.write(" ");
				m_writer.write(format(colValue, colWidth));

				idx++;
			}

			//close last column
			m_writer.write(" |\n");
			m_writer.flush();

			if (entry.isDocument()) {
				m_docEntryCount++;
			}
			else if (entry.isCategory()) {
				m_categoryEntryCount++;
			}
			else if (entry.isTotal()) {
				m_totalEntryCount++;
			}

			return this;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private String valueToString(Object val) {
		String sVal;
		{
			if (val==null) {
				sVal = "";
			}
			else if (val instanceof String) {
				sVal = (String) val;
			}
			else if (val instanceof LMBCSString) {
				sVal = ((LMBCSString)val).getValue();
			}
			else {
				sVal = val.toString();
			}
		}
		return sVal.replace("\n", "\\n").replace("\t", "\\t");
	}

	public NotesMarkdownTable printFooter() {
		try {
			m_writer.write(Integer.toString(m_docEntryCount)+" documents, "+m_categoryEntryCount+" categories, "+
					m_totalEntryCount+" totals");
			m_writer.write("\n");
			m_writer.flush();
			return this;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public int getDocEntryCount() {
		return m_docEntryCount;
	}

	public int getCategoryEntryCount() {
		return m_categoryEntryCount;
	}

	public int getTotalEntryCount() {
		return m_totalEntryCount;
	}

	/**
	 * Table column for the note id of the view entry
	 */
	public static final ColumnInfo NOTEID = new ColumnInfo("NoteID", 12, (table, entry) -> {
		return Integer.toString(entry.getNoteId());
	});

	/**
	 * Table column for the UNID of the view entry
	 */
	public static final ColumnInfo UNID = new ColumnInfo("UNID", 32, (table, entry) -> {
		return entry.getUNID().replace("00000000000000000000000000000000", "");
	});

	/**
	 * Table column for the child count of the view entry
	 */
	public static final ColumnInfo CHILDCOUNT = new ColumnInfo("ChildCount", 10, (table, entry) -> {
		return Integer.toString(entry.getChildCount());
	});
	
	/**
	 * Table column for the sibling count of the view entry
	 */
	public static final ColumnInfo SIBLINGCOUNT = new ColumnInfo("SiblingCount", 12, (table, entry) -> {
		return Integer.toString(entry.getSiblingCount());
	});

	/**
	 * Table column for the descendant count of the view entry
	 */
	public static final ColumnInfo DESCENDANTCOUNT = new ColumnInfo("DescendantCount", 15, (table, entry) -> {
		return Integer.toString(entry.getDescendantCount());
	});

	/**
	 * Table column for the indent levels of the view entry
	 */
	public static final ColumnInfo INDENTLEVELS = new ColumnInfo("IndentLevels", 12, (table, entry) -> {
		return Integer.toString(entry.getIndentLevels());
	});

	/**
	 * Table column for the COLLECTIONPOSITION of the view entry
	 */
	public static final ColumnInfo POS = new ColumnInfo("#", 20, (table, entry) -> { return entry.getPositionStr();});

	/**
	 * Table column for the level of the view entry (0 for root of virtual view, 1 for first level, ...)
	 */
	public static final ColumnInfo LEVEL = new ColumnInfo("Level", 5, (table, entry) -> { return String.valueOf(entry.getLevel()); });

	private static enum ExpandState {NONE, EXPANDED, COLLAPSED};

	/**
	 * Table column for the exand state of the view entry. Returns an empty string for
	 * entries with childCount==0, a "+" for collapsed entries with children and
	 * "-" for expanded entries with children.
	 */
	public static final ColumnInfo EXPANDSTATE = new ColumnInfo("", 1,
			new BiFunction<NotesMarkdownTable, IViewEntryData, String>() {

		private WeakHashMap<NotesIDTable,Boolean> collapsedListInverted = new WeakHashMap<>();
		
		private ExpandState getExpandState(NotesMarkdownTable table, IViewEntryData entry) {
			if (entry instanceof NotesViewEntryData) {
				
				NotesIDTable collapsedList = table.m_realView.getCollapsedList();
				boolean inverted = collapsedListInverted.computeIfAbsent(collapsedList, (idTable) -> { return idTable.isInverted(); });

				int noteId = entry.getNoteId();
				if (noteId==0) {
					return ExpandState.NONE;
				}

				int childCount = entry.getChildCount();
				if (childCount==0) {
					return ExpandState.NONE;
				}

				if (inverted) {
					//expand list
					return collapsedList.contains(noteId) ? ExpandState.EXPANDED : ExpandState.COLLAPSED;
				}
				else {
					return collapsedList.contains(noteId) ? ExpandState.COLLAPSED : ExpandState.EXPANDED;
				}
			}
			else if (entry instanceof VirtualViewEntryData) {				
				VirtualViewEntryData virtualViewEntry = (VirtualViewEntryData) entry;
				if (table.m_virtualViewNav.isExpanded(virtualViewEntry)) {
					return ExpandState.EXPANDED;
				}
				else {
					return ExpandState.COLLAPSED;
				}
			}
            return ExpandState.NONE;
		}

		@Override
		public String apply(NotesMarkdownTable table, IViewEntryData entry) {
			ExpandState expandState = getExpandState(table, entry);
			switch (expandState) {
			case COLLAPSED:
				return "+";
			case EXPANDED:
				return "-";
			case NONE:
			default:
				return " ";
			}
		}

	});

	public static final ColumnInfo CATEGORY = new ColumnInfo("Category", 40,
			(table, entry) -> {
				if (entry.isCategory()) {
					String sVal;
					int level = entry.getLevel();
					int indentLevels = entry.getIndentLevels();
					
					if (table.m_realView != null) {
						if (level == -1) {
							//ReadMask.INDEX_POSITION not loaded from view
							return "(no index position found)";
						}

						Object categoryVal = null;
						for (int i=table.m_realView.getColumns().size()-1; i>=0; i--) {
							NotesViewColumn col = table.m_realView.getColumns().get(i);
							if (col.isCategory()) {
								categoryVal = entry.get(col.getItemName());
								if (categoryVal != null) {
									break;
								}
							}
						}
						if (categoryVal == null) {
							categoryVal = "(Not categorized)";
						}
						sVal = StringUtil.repeat(' ', level + indentLevels) + String.valueOf(categoryVal);
					}
					else {
						if (level == -1) {
							//for virtual views, -1 means the artificial root entry
							return "";
						}
						Object categoryVal = ((VirtualViewEntryData)entry).getCategoryValue();
						if (categoryVal == null) {
							categoryVal = "(Not categorized)";
						}
						sVal = StringUtil.repeat(' ', level + indentLevels) + String.valueOf(categoryVal);
					}
					return sVal;
				}
				else {
					return "";
				}
			});
}
