package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.Vector;

import lotus.domino.Base;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntry;

/**
 * Bridge between a {@link ViewEntry} and data we retrieve from C API calls
 * 
 * @author Karsten Lehmann
 */
public class ViewEntryImpl implements ViewEntry {
	private View m_parent;
	private NotesViewEntryData m_data;
	private Vector m_colValues;
	
	public ViewEntryImpl(View parent, NotesViewEntryData data) {
		m_data = data;
	}

	@Override
	public void recycle() throws NotesException {
		//noop, because we don't have a handle to release
	}

	@Override
	public void recycle(Vector paramVector) throws NotesException {
		if (paramVector!=null) {
			for (int i=0; i<paramVector.size(); i++) {
				Object obj = paramVector.get(i);
				if (obj instanceof Base) {
					try {
						((Base)obj).recycle();
					}
					catch (NotesException e) {
						//
					}
				}
			}
		}
	}

	@Override
	public boolean isDocument() throws NotesException {
		return !isCategory() && !isTotal();
	}

	@Override
	public boolean isCategory() throws NotesException {
		return (m_data.getNoteId() & NotesCAPI.NOTEID_CATEGORY) == NotesCAPI.NOTEID_CATEGORY;
	}

	@Override
	public boolean isTotal() throws NotesException {
		return (m_data.getNoteId() & NotesCAPI.NOTEID_CATEGORY_TOTAL) == NotesCAPI.NOTEID_CATEGORY_TOTAL;
	}

	@Override
	public Object getParent() throws NotesException {
		return m_parent;
	}

	@Override
	public Document getDocument() throws NotesException {
		try {
			return m_parent.getParent().getDocumentByID(Integer.toString(m_data.getNoteId(), 16));
		}
		catch (NotesException e) {
			if (e.id == 4091)
				return null;
			else
				throw e;
		}
	}

	@Override
	public int getSiblingCount() throws NotesException {
		return m_data.getSiblingCount();
	}

	@Override
	public int getChildCount() throws NotesException {
		return m_data.getChildCount();
	}

	@Override
	public int getDescendantCount() throws NotesException {
		return m_data.getDescendantCount();
	}

	@Override
	public int getIndentLevel() throws NotesException {
		int[] pos = m_data.getPosition();
		if (pos==null)
			return 0;
		return pos.length - 1;
	}

	@Override
	public int getColumnIndentLevel() throws NotesException {
		return m_data.getIndentLevels();
	}

	@Override
	public String getPosition(char paramChar) throws NotesException {
		int[] pos = m_data.getPosition();
		if (pos==null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<pos.length; i++) {
			if (sb.length()>0)
				sb.append(paramChar);
			
			sb.append(Integer.toString(pos[i]));
		}
		return sb.toString();
	}

	@Override
	public String getNoteID() throws NotesException {
		return Integer.toString(m_data.getNoteId(), 16);
	}

	@Override
	public int getNoteIDAsInt() throws NotesException {
		return m_data.getNoteId();
	}

	@Override
	public String getUniversalID() throws NotesException {
		return m_data.getUNID();
	}

	@Override
	public boolean isConflict() throws NotesException {
		return m_data.isConflict();
	}

	public boolean isResponse() {
		return m_data.isResponse();
	}
	
	@Override
	public Vector getColumnValues() throws NotesException {
		if (m_colValues==null) {
			Object[] colValuesArr = m_data.getColumnValues();

			if (colValuesArr==null) {
				m_colValues = new Vector(0);
			}
			else {
				m_colValues = toVector(colValuesArr);
			}
		}
		return m_colValues;
	}

	private Vector toVector(Object[] values) {
		Vector dataAsVector = new Vector(values.length);
		for (int i=0; i<values.length; i++) {
			if (values[i] instanceof Object[]) {
				Vector innerDataAsVector = toVector((Object[]) values[i]);
				dataAsVector.add(innerDataAsVector);
			}
			else if (values[i] instanceof Calendar) {
				//returns {@link Date} objects instead of {@link lotus.domino.DateTime}
				dataAsVector.add(((Calendar)values[i]).getTime());
			}
			else if (values[i] != null) {
				dataAsVector.add(values[i]);
			}
			else {
				dataAsVector.add("");
			}
		}
		return dataAsVector;
	}
	
	public Object[] getColumnValuesAsArray() {
		return m_data.getColumnValues();
	}
	
	@Override
	public int getFTSearchScore() throws NotesException {
		return m_data.getFTScore();
	}

	@Override
	public boolean isValid() throws NotesException {
		return m_data.getNoteId()!=0;
	}

	@Override
	public boolean getRead() throws NotesException {
		return !m_data.isUnread();
	}

	@Override
	public boolean getRead(String paramString) throws NotesException {
		return false;
	}

	@Override
	public boolean isPreferJavaDates() throws NotesException {
		return true;
	}

	@Override
	public void setPreferJavaDates(boolean paramBoolean) throws NotesException {
	}
	
}
