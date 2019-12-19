package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;

/**
 * Richtext conversion class that copies the whole richtext content and appends a file hotspot at the end.<br>
 * 
 * @author Karsten Lehmann
 */
public class AppendFileHotspotConversion implements IRichTextConversion {
	private NotesAttachment m_att;
	private String m_fileNameToDisplay;
	
	public AppendFileHotspotConversion(NotesAttachment att, String fileNameToDisplay) {
		m_att = att;
		m_fileNameToDisplay = fileNameToDisplay;
	}
	
	@Override
	public void richtextNavigationStart() {
	}
	
	@Override
	public void richtextNavigationEnd() {
	}
	
	@Override
	public boolean isMatch(IRichTextNavigator nav) {
		//always append
		return true;
	}

	@Override
	public void convert(IRichTextNavigator source, ICompoundText target) {
		//TODO provide another method to append file hotspots with less copy operations, e.g. by modifying the last item value of the last TYPE_COMPOSITE item or add another item if the hotspot would exceed the segment size
		if (source.gotoFirst()) {
			do {
				source.copyCurrentRecordTo(target);
			}
			while (source.gotoNext());
		}
		target.addFileHotspot(m_att, m_fileNameToDisplay);
	}

}
