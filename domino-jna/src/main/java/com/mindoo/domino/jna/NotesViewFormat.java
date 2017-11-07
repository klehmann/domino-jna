package com.mindoo.domino.jna;

import java.util.List;

import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormat2Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormat4Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormat5Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormatStruct;

public class NotesViewFormat {
	private List<NotesViewColumn> m_columns;
	
	public NotesViewFormat(IAdaptable data, List<NotesViewColumn> columns) {
		m_columns = columns;
		
		NotesViewTableFormatStruct format = data.getAdapter(NotesViewTableFormatStruct.class);
		if (format!=null)
			importData(format);
		NotesViewTableFormat2Struct format2 = data.getAdapter(NotesViewTableFormat2Struct.class);
		if (format2!=null)
			importData(format2);
		NotesViewTableFormat4Struct format4 = data.getAdapter(NotesViewTableFormat4Struct.class);
		if (format4!=null)
			importData(format4);
		NotesViewTableFormat5Struct format5 = data.getAdapter(NotesViewTableFormat5Struct.class);
		if (format5!=null)
			importData(format5);
		
	}
	
	private void importData(NotesViewTableFormat5Struct format5) {
		// v5 formats not yet available, see ViewFormatDecoder for details
	}

	private void importData(NotesViewTableFormat4Struct format4) {
		// v4 formats not yet available, see ViewFormatDecoder for details
	}

	private void importData(NotesViewTableFormat2Struct format2) {
		// TODO add code
	}

	private void importData(NotesViewTableFormatStruct format) {
	}

	public List<NotesViewColumn> getColumns() {
		return m_columns;
	}
}
