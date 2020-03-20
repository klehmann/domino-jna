package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.IRichTextNavigator.RichTextNavPosition;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;

/**
 * Conversion class that removes all file hotspot occurrences from a richtext items that
 * point to the specified attachment
 * 
 * @author Karsten Lehmann
 */
public class RemoveFileHotspotConversion implements IRichTextConversion {
	private String m_attachmentFileName;
	
	public RemoveFileHotspotConversion(NotesAttachment att) {
		m_attachmentFileName = att.getFileName();
	}
	
	public RemoveFileHotspotConversion(String attachmentFileName) {
		m_attachmentFileName = attachmentFileName;
	}
	
	@Override
	public void richtextNavigationStart() {
	}
	
	@Override
	public void richtextNavigationEnd() {
	}
	
	/**
	 * Starts reading at "startPos" and searches for a record HOTSPOTEND followed by END. These two records
	 * mark the end of the file hotspot we want to skip.
	 * 
	 * @param nav navigator
	 * @param startPos start position or null to start at the beginning
	 * @return position of the data right after the END record or null if there is no such data
	 */
	private RichTextNavPosition findContentAfterHotspotEnd(IRichTextNavigator nav, RichTextNavPosition startPos) {
		if (startPos!=null) {
			nav.restoreCurrentRecordPosition(startPos);
		}
		else {
			if (!nav.gotoFirst()) {
				return null;
			}
		}
		
		do {
			if (CDRecordType.HOTSPOTEND.getConstant() == nav.getCurrentRecordTypeAsShort()) {
				RichTextNavPosition savedPos = nav.getCurrentRecordPosition();
				if (nav.gotoNext()) {
					//check what is next
					if (CDRecordType.END.getConstant() == nav.getCurrentRecordTypeAsShort()) {
						//get the position of the data right after END
						if (nav.gotoNext()) {
							RichTextNavPosition posOfContentAfterEnd = nav.getCurrentRecordPosition();
							return posOfContentAfterEnd;
						}
						else {
							//no more data
							return null;
						}
					}
				}
				nav.restoreCurrentRecordPosition(savedPos);
			}
		}
		while (nav.gotoNext());
		
		return null;
	}
	
	private RichTextNavPosition findBeginBeforeHotspot(IRichTextNavigator nav, RichTextNavPosition startPos) {
		return scanForBeginOfHotspot(nav, startPos, null);
	}
	
	/**
	 * Traverses the CD record stream searching for records BEGIN followed by HOTSPOTBEGIN with the unique
	 * filename we are searching for.
	 * 
	 * @param nav navigator
	 * @param startPos start position for the search or null to start at the beginning of the stream
	 * @param copyToTarget if not null, we copy all records we find until the right BEGIN record to this target
	 * @return position of the BEGIN record or null if not found
	 */
	private RichTextNavPosition scanForBeginOfHotspot(IRichTextNavigator nav, RichTextNavPosition startPos, ICompoundText copyToTarget) {
		if (startPos!=null) {
			nav.restoreCurrentRecordPosition(startPos);
		}
		else {
			if (!nav.gotoFirst()) {
				return null;
			}
		}
		
		do {
			if (CDRecordType.BEGIN.getConstant() == nav.getCurrentRecordTypeAsShort()) {
//				typedef struct {
//					   BSIG Header;    /* Signature and length of this record */
//					   WORD Version;		
//					   WORD Signature; /* Signature of record begin is for */
//					} CDBEGINRECORD;
				Memory beginDataBuf = nav.getCurrentRecordData();
				
				int version = beginDataBuf.getShort(0);
				int signature = beginDataBuf.share(2).getShort(0);
				
				if (signature == NotesConstants.SIG_CD_V4HOTSPOTBEGIN) {
					RichTextNavPosition savedPos = nav.getCurrentRecordPosition();
					if (nav.gotoNext()) {
						//check what is next
						if (CDRecordType.HOTSPOTBEGIN.getConstant() == nav.getCurrentRecordTypeAsShort()) {
//							typedef struct {
//							   WSIG  Header; /* Signature and length of this record */	
//							   WORD  Type;
//							   DWORD Flags;
//							   WORD  DataLength;
//							   Data follows...
								/*  if HOTSPOTREC_RUNFLAG_SIGNED, WORD SigLen then SigData follows. */
//							} CDHOTSPOTBEGIN;

							Memory hotspotRecordDataBuf = nav.getCurrentRecordData();
							
							short type = hotspotRecordDataBuf.getShort(0);
							if (type == NotesConstants.HOTSPOTREC_TYPE_FILE) {
								int flags = hotspotRecordDataBuf.share(2).getInt(0);
								int dataLength = (int) (hotspotRecordDataBuf.share(6).getShort(0)  & 0xffff);
								
								String uniqueFileName = NotesStringUtils.fromLMBCS(hotspotRecordDataBuf.share(8), -1);
								if (uniqueFileName.equalsIgnoreCase(m_attachmentFileName)) {
									return savedPos;
								}
							}
						}
					}
					nav.restoreCurrentRecordPosition(savedPos);
				}
				if (copyToTarget!=null) {
					nav.copyCurrentRecordTo(copyToTarget);
				}
			}
			else {
				if (copyToTarget!=null) {
					nav.copyCurrentRecordTo(copyToTarget);
				}
			}
		}
		while (nav.gotoNext());
		
		return null;
	}
	
	@Override
	public boolean isMatch(IRichTextNavigator nav) {
		RichTextNavPosition pos = findBeginBeforeHotspot(nav, null);
		if (pos!=null) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void convert(IRichTextNavigator source, ICompoundText target) {
		RichTextNavPosition currPos = null;
		if (source.gotoFirst()) {
			while (true) {
				//go through the CD stream, copying all records we find to "target", until we reach
				//a BEGIN, followed by a HOTSPOTBEGIN record with the filename we are searching for
				RichTextNavPosition nextBeginBeforeHotspot = scanForBeginOfHotspot(source, currPos, target);
				if (nextBeginBeforeHotspot!=null) {
					RichTextNavPosition contentAfterHotspotEnd = findContentAfterHotspotEnd(source, nextBeginBeforeHotspot);
					if (contentAfterHotspotEnd!=null) {
						//start here reading content at the next loop
						currPos = contentAfterHotspotEnd;
					}
					else {
						//we are done
						break;
					}
				}
				else {
					//no more hotspots found
					break;
				}
			}
		}
		else {
			//richtext empty
		}
	}

}
