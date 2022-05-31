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
import java.util.function.Function;
import java.util.stream.Stream;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewColumn;
import com.mindoo.domino.jna.NotesViewEntryData;

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
	private NotesCollection m_view;
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
		m_view = view;
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
		private Function<NotesViewEntryData,String> fct;

		public ColumnInfo(String title, int width, Function<NotesViewEntryData,String> fct) {
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

		public Function<NotesViewEntryData, String> getFunction() {
			return fct;
		}

	}

	/**
	 * Adds a computed column to the table
	 * 
	 * @param title column title
	 * @param width column width in characters
	 * @param fct function to compute the value from a {@link NotesViewEntryData}
	 */
	public NotesMarkdownTable addColumn(String title, int width, Function<NotesViewEntryData,String> fct) {
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
		m_view
				.getColumns()
				.stream()
				.map((col) -> { return col.getItemName(); } )
				.forEach(this::addViewColumn);
		
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
		NotesViewColumn col = m_view.getColumns()
		.stream()
		.filter((currCol) -> { return itemName.equalsIgnoreCase(currCol.getItemName()); })
		.findFirst()
		.orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Column {0} not found", itemName)));
		
		String title = col.getTitle() + "(" + col.getItemName()+")";

		return addColumn(title, width, (entry) -> {
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

	public NotesMarkdownTable printRows(Stream<NotesViewEntryData> entries) {
		entries.forEach(this::printRow);
		return this;
	}

	public NotesMarkdownTable printRows(Collection<NotesViewEntryData> entries) {
		for (NotesViewEntryData currEntry : entries) {
			printRow(currEntry);
		}
		return this;
	}

	public NotesMarkdownTable printRow(NotesViewEntryData entry) {
		try {
			entry.setPreferNotesTimeDates(true);

			int idx=0;

			m_writer.write("| ");
			
			for (ColumnInfo currCol : m_customColumns) {
				int colWidth = currCol.getWidth();
				String colValue = currCol.getFunction().apply(entry);
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
	public static final ColumnInfo NOTEID = new ColumnInfo("NoteID", 12, (entry) -> {
		return Integer.toString(entry.getNoteId());
	});

	/**
	 * Table column for the UNID of the view entry
	 */
	public static final ColumnInfo UNID = new ColumnInfo("UNID", 32, (entry) -> {
		return entry.getUNID().replace("00000000000000000000000000000000", "");
	});

	/**
	 * Table column for the COLLECTIONPOSITION of the view entry
	 */
	public static final ColumnInfo POS = new ColumnInfo("#", 20, (entry) -> { return entry.getPositionStr();});

	private static enum ExpandState {NONE, EXPANDED, COLLAPSED};

	/**
	 * Table column for the exand state of the view entry. Returns an empty string for
	 * entries with childCount==0, a "+" for collapsed entries with children and
	 * "-" for expanded entries with children.
	 */
	public static final ColumnInfo EXPANDSTATE = new ColumnInfo("", 1,
			new Function<NotesViewEntryData,String>() {

		private Boolean m_collapsedListInverted;
		private NotesIDTable m_collapsedList;

		private ExpandState getExpandState(NotesViewEntryData entry) {
			if (m_collapsedList==null) {
				m_collapsedList = entry.getParent().getCollapsedList();
			}
			if (m_collapsedListInverted==null) {
				m_collapsedListInverted = m_collapsedList.isInverted();
			}

			int noteId = entry.getNoteId();
			if (noteId==0) {
				return ExpandState.NONE;
			}

			int childCount = entry.getChildCount();
			if (childCount==0) {
				return ExpandState.NONE;
			}

			if (m_collapsedListInverted) {
				//expand list
				return m_collapsedList.contains(noteId) ? ExpandState.EXPANDED : ExpandState.COLLAPSED;
			}
			else {
				return m_collapsedList.contains(noteId) ? ExpandState.COLLAPSED : ExpandState.EXPANDED;
			}
		}

		@Override
		public String apply(NotesViewEntryData entry) {
			ExpandState expandState = getExpandState(entry);
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

}
