package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;

public class AppendFileHotspotConversion implements IRichTextConversion {
	private NotesAttachment m_att;
	private String m_fileNameToDisplay;
	
	public AppendFileHotspotConversion(NotesAttachment att, String fileNameToDisplay) {
		m_att = att;
		m_fileNameToDisplay = fileNameToDisplay;
	}
	
	@Override
	public boolean isMatch(IRichTextNavigator nav) {
		//always append
		return true;
	}

	@Override
	public void convert(IRichTextNavigator source, ICompoundText target) {
		if (source.gotoFirst()) {
			do {
				source.copyCurrentRecordTo(target);
			}
			while (source.gotoNext());
		}
		target.addFileHotspot(m_att, m_fileNameToDisplay);
	}

}
