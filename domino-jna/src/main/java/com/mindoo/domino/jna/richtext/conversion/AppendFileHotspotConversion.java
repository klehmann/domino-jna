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
	private String m_attachmentProgrammaticName;
	private String m_fileNameToDisplay;
	
	/**
	 * Creates a new instance
	 * 
	 * @param attachmentProgrammaticName name returned by {@link NotesAttachment#getFileName()}
	 * @param fileNameToDisplay filename to display below the file icon, not necessarily the same as {@link NotesAttachment#getFileName()}
	 */
	public AppendFileHotspotConversion(String attachmentProgrammaticName, String fileNameToDisplay) {
		m_attachmentProgrammaticName = attachmentProgrammaticName;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param att attachment to add an icon for
	 * @param fileNameToDisplay filename to display below the file icon, not necessarily the same as {@link NotesAttachment#getFileName()}
	 */
	public AppendFileHotspotConversion(NotesAttachment att, String fileNameToDisplay) {
		m_attachmentProgrammaticName = att.getFileName();
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
		target.addFileHotspot(m_attachmentProgrammaticName, m_fileNameToDisplay);
	}

}
