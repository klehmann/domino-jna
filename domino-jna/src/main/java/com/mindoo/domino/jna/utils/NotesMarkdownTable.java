package com.mindoo.domino.jna.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewColumn;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.ReadMask;

/**
 * Utility class to dump entries of a {@link NotesCollection} as a Markdown table.
 * See <a href="https://www.markdownguide.org/extended-syntax/#tables">this page</a>
 * for an example / format description.<br>
 * <br>
 * Basic usage:<br>
 * <br>
 * <code>
 * new NotesMarkdownTable(notesCollection, System.out)<br>
 *  .withPosition(true)<br>
 *  .withNoteId(true)<br>
 *  .withUNID(true)<br>
 *  .withExpandState(true)<br>
 *  .printHeader()<br>
 *  .printRows(entries)<br>
 *  .printFooter();<br>
 *  </code>
 * 
 * @author Karsten Lehmann
 */
public class NotesMarkdownTable {
	private NotesCollection m_view;
	private Map<NotesViewColumn,Integer> m_visibleColumnWidths;
	private Writer m_writer;
	private boolean m_withExpandState;
	private boolean m_withPosition;
	private boolean m_withNoteId;
	private boolean m_withUNID;
	private NotesIDTable m_collapsedList;
	private boolean m_collapsedListInverted;
	
	private int m_docEntryCount;
	private int m_categoryEntryCount;
	private int m_totalEntryCount;
	
	/**
	 * 
	 * @param view
	 * @param out
	 */
	public NotesMarkdownTable(NotesCollection view, OutputStream out) {
		this(view, new OutputStreamWriter(out));
	}
	
	public NotesMarkdownTable(NotesCollection view, Writer writer) {
		m_view = view;
		m_collapsedList = m_view.getCollapsedList();
		m_collapsedListInverted = m_collapsedList.isInverted();
		
		m_writer = writer;
		
		m_visibleColumnWidths = new HashMap<>();
		for (NotesViewColumn col : m_view.getColumns()) {
			if (isVisible(col)) {
				m_visibleColumnWidths.put(col, getWidthInChars(col));
			}
		}
	}

	/**
	 * Activates a column to display a +/- in case the entry has children and is
	 * collapsed/expanded. Make sure to read
	 * the entry data with {@link ReadMask#INDEXCHILDREN} to have values to display.
	 * 
	 * @param b true to display
	 * @return this table
	 */
	public NotesMarkdownTable withExpandState(boolean b) {
		m_withExpandState = b;
		return this;
	}
	
	/**
	 * Activates a column to display the entry position. Make sure to read
	 * the entry data with {@link ReadMask#INDEXPOSITION} to have values to display.
	 * 
	 * @param b true to display
	 * @return this table
	 */
	public NotesMarkdownTable withPosition(boolean b) {
		m_withPosition = b;
		return this;
	}
	
	/**
	 * Activates a column to display the entry note id. Make sure to read
	 * the entry data with {@link ReadMask#NOTEID} to have values to display.
	 * 
	 * @param b true to display
	 * @return this table
	 */
	public NotesMarkdownTable withNoteId(boolean b) {
		m_withNoteId = b;
		return this;
	}
	
	/**
	 * Activates a column to display the entry note id. Make sure to read
	 * the entry data with {@link ReadMask#NOTEUNID} to have values to display.
	 * 
	 * @param b true to display
	 * @return this table
	 */
	public NotesMarkdownTable withUNID(boolean b) {
		m_withUNID = b;
		return this;
	}

	private enum ExpandState {NONE, EXPANDED, COLLAPSED};
	
	private ExpandState getExpandState(NotesViewEntryData entry) {
		int noteId = entry.getNoteId();
		if (noteId==0) {
			return ExpandState.NONE;
		}
		
		int childCount = entry.getChildCount();
		if (childCount==0) {
			return ExpandState.NONE;
		}
		
		NotesIDTable idTable = m_view.getCollapsedList();
		if (m_collapsedListInverted) {
			//expand list
			return idTable.contains(noteId) ? ExpandState.EXPANDED : ExpandState.COLLAPSED;
		}
		else {
			return idTable.contains(noteId) ? ExpandState.COLLAPSED : ExpandState.EXPANDED;
		}
	}
	
	protected int getWidthInChars(NotesViewColumn col) {
		return 40;
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
	
	protected int getPositionColumnWidth() {
		return 20;
	}
	
	public NotesMarkdownTable printHeader() {
		try {
			StringBuilder sbLine1 = new StringBuilder();
			StringBuilder sbLine2 = new StringBuilder();

			List<NotesViewColumn> cols = m_view.getColumns();

			int idx=0;

			sbLine1.append("| ");
			sbLine2.append("| ");

			if (m_withExpandState) {
				sbLine1.append("  |");
				sbLine2.append("- |");
			}

			if (m_withPosition) {
				if (idx>0) {
					//close column to the left
					sbLine1.append(" |");
					sbLine2.append(" |");
				}
				int colWidth = getPositionColumnWidth();
				sbLine1.append(" ").append(format("#", colWidth));
				sbLine2.append(" ").append(StringUtil.repeat('-', colWidth));

				idx++;
			}

			if (m_withNoteId) {
				if (idx>0) {
					//close column to the left
					sbLine1.append(" |");
					sbLine2.append(" |");
				}
				sbLine1.append(" ").append(format("NoteID", 12));
				sbLine2.append(" ").append(StringUtil.repeat('-', 12));

				idx++;
			}

			if (m_withUNID) {
				if (idx>0) {
					//close column to the left
					sbLine1.append(" |");
					sbLine2.append(" |");
				}
				sbLine1.append(" ").append(format("UNID", 32));
				sbLine2.append(" ").append(StringUtil.repeat('-', 32));

				idx++;
			}

			for (int i=0; i<cols.size(); i++) {
				NotesViewColumn currCol = cols.get(i);
				Integer colWidth = m_visibleColumnWidths.get(currCol);
				if (colWidth==null) {
					continue;
				}

				if (idx>0) {
					//close column to the left
					sbLine1.append(" |");
					sbLine2.append(" |");
				}

				String title = currCol.getTitle() + "(" + currCol.getItemName()+")";
				sbLine1.append(" ").append(format(title, colWidth));
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

			List<NotesViewColumn> cols = m_view.getColumns();

			int idx=0;

			m_writer.write("| ");

			if (m_withExpandState) {
				ExpandState expandState = getExpandState(entry);
				switch (expandState) {
				case COLLAPSED:
					m_writer.write("+");
					break;
				case EXPANDED:
					m_writer.write("-");
					break;
				case NONE:
					m_writer.write(" ");
				default:
				}
				idx++;
			}

			if (m_withPosition) {
				if (idx>0) {
					//close column to the left
					m_writer.write(" |");
				}
				int colWidth = getPositionColumnWidth();
				m_writer.write(" ");
				m_writer.write(format(entry.getPositionStr(), colWidth));
				idx++;
			}

			if (m_withNoteId) {
				if (idx>0) {
					//close column to the left
					m_writer.write(" |");
				}
				m_writer.write(" ");
				m_writer.write(format(Integer.toString(entry.getNoteId()), 12));
				idx++;
			}

			if (m_withUNID) {
				if (idx>0) {
					//close column to the left
					m_writer.write(" |");
				}
				m_writer.write(" ");
				m_writer.write(format(entry.getUNID().replace("00000000000000000000000000000000", ""), 32));

				idx++;
			}

			for (int i=0; i<cols.size(); i++) {
				NotesViewColumn currCol = cols.get(i);
				Integer colWidth = m_visibleColumnWidths.get(currCol);
				if (colWidth==null) {
					continue;
				}

				if (idx>0) {
					//close column to the left
					m_writer.write(" |");
				}

				String itemName = currCol.getItemName();
				Object val = entry.get(itemName);
				String sVal = valueToString(val);
				m_writer.write(" ");
				m_writer.write(format(sVal, colWidth));

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

}
