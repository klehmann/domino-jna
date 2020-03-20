package com.mindoo.domino.jna.richtext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.internal.FieldPropAdaptable;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.richtext.IRichTextNavigator.RichTextNavPosition;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;

/**
 * Collection of useful methods to process richtext CD records
 * 
 * @author Karsten Lehmann
 */
public class RichTextUtils {

	/**
	 * Calls {@link #collectFields(IRichTextNavigator)} and just returns the
	 * field names
	 * 
	 * @param rtNav richtext navigator
	 * @return field names
	 */
	public static List<String> collectFieldNames(IRichTextNavigator rtNav) {
		List<FieldInfo> fields = collectFields(rtNav);
		List<String> fieldNames = new ArrayList<String>(fields.size());
		for (FieldInfo currField : fields) {
			fieldNames.add(currField.getName());
		}
		return fieldNames;
	}
	
	/**
	 * Traverses the richtext CD records (of a design element's $body item)
	 * and collects all contained fields
	 * with data type, name, description and formulas. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param rtNav richtext navigator
	 * @return fields
	 */
	public static List<FieldInfo> collectFields(IRichTextNavigator rtNav) {
		List<FieldInfo> fields = new ArrayList<FieldInfo>();
		
		if (rtNav==null) {
			return fields;
		}
		
		RichTextNavPosition oldPos = rtNav.getCurrentRecordPosition();
		
		if (rtNav.gotoFirst()) {
			boolean inBeginEnd = false;
			
			do {
				if (CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = true;
				}
				else if (CDRecordType.END.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = false;
				}
				else if (CDRecordType.FIELD.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					//found record of type SIG_CD_FIELD
					Memory fieldRecordWithHeader = rtNav.getCurrentRecordDataWithHeader();
					
					//search for additional fields
					RichTextNavPosition fieldPos = rtNav.getCurrentRecordPosition();
					
					Memory idNameCDRecordWithHeader = null;

					if (inBeginEnd) {
						if (rtNav.gotoNext()) {
							do {
								// check if we have reached the end of the field block
								if (CDRecordType.END.getConstant() == rtNav.getCurrentRecordTypeAsShort() ||
										CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
									break;
								}
								else if (CDRecordType.IDNAME.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
									idNameCDRecordWithHeader = rtNav.getCurrentRecordDataWithHeader();
								}
							}
							while (rtNav.gotoNext());
						}
					}

					rtNav.restoreCurrentRecordPosition(fieldPos);

					FieldPropAdaptable fieldData = new FieldPropAdaptable(fieldRecordWithHeader, idNameCDRecordWithHeader);
					FieldInfo fldInfo = new FieldInfo(fieldData);
					fields.add(fldInfo);
				}
			}
			while (rtNav.gotoNext());
		}
		
		if (oldPos!=null) {
			rtNav.restoreCurrentRecordPosition(oldPos);
		}
		
		return fields;
	}
	
	/**
	 * Scans all richtext items of a note to find all attachment icons and
	 * return the attachment names hashed by richtext item name
	 * 
	 * @param note note to scan
	 * @return map with item name as key (case insensitive) and attachment names as value in order of appearance
	 */
	public static Map<String,LinkedHashSet<String>> collectAllAttachmentNamesByField(NotesNote note) {
		Map<String,LinkedHashSet<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		Set<String> allRichtextItemNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		
		note.getItems((item,loop) -> {
			if (item.getType() == NotesItem.TYPE_COMPOSITE) {
				allRichtextItemNames.add(item.getName());
			}
		});
		
		for (String currItemName : allRichtextItemNames) {
			IRichTextNavigator rtNav = note.getRichtextNavigator(currItemName);
			LinkedHashSet<String> attachmentNamesInItem = collectAttachmentNames(rtNav);
			result.put(currItemName, attachmentNamesInItem);
		}
		
		return result;
	}
	
	/**
	 * Traverses the richtext CD records and collects the filenames of all
	 * attachment icons. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param note note to scan
	 * @param itemName name of richtext item
	 * @return attachment names in order of appearance
	 */
	public static LinkedHashSet<String> collectAttachmentNames(NotesNote note, String itemName) {
		IRichTextNavigator rtNav = note.getRichtextNavigator(itemName);
		if (rtNav!=null) {
			return collectAttachmentNames(rtNav);
		}
		else {
			return new LinkedHashSet<>();
		}
	}
	
	/**
	 * Traverses the richtext CD records and collects the filenames of all
	 * attachment icons. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param rtNav richtext navigator
	 * @return attachment names in order of appearance
	 */
	public static LinkedHashSet<String> collectAttachmentNames(IRichTextNavigator rtNav) {
		LinkedHashSet<String> attNames = new LinkedHashSet<>();
		
		if (rtNav==null) {
			return attNames;
		}
		
		RichTextNavPosition oldPos = rtNav.getCurrentRecordPosition();
		
		if (rtNav.gotoFirst()) {
			do {
				if (CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
//					typedef struct {
//						   BSIG Header;    /* Signature and length of this record */
//						   WORD Version;		
//						   WORD Signature; /* Signature of record begin is for */
//						} CDBEGINRECORD;
					Memory beginDataBuf = rtNav.getCurrentRecordData();
					
					int version = beginDataBuf.getShort(0);
					int signature = beginDataBuf.share(2).getShort(0);
					
					if (signature == NotesConstants.SIG_CD_V4HOTSPOTBEGIN) {
						RichTextNavPosition savedPos = rtNav.getCurrentRecordPosition();
						if (rtNav.gotoNext()) {
							//check what is next
							if (CDRecordType.HOTSPOTBEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
//								typedef struct {
//								   WSIG  Header; /* Signature and length of this record */	
//								   WORD  Type;
//								   DWORD Flags;
//								   WORD  DataLength;
//								   Data follows...
									/*  if HOTSPOTREC_RUNFLAG_SIGNED, WORD SigLen then SigData follows. */
//								} CDHOTSPOTBEGIN;

								Memory hotspotRecordDataBuf = rtNav.getCurrentRecordData();
								
								short type = hotspotRecordDataBuf.getShort(0);
								if (type == NotesConstants.HOTSPOTREC_TYPE_FILE) {
									String uniqueFileName = NotesStringUtils.fromLMBCS(hotspotRecordDataBuf.share(8), -1);
									attNames.add(uniqueFileName);
								}
							}
						}
						rtNav.restoreCurrentRecordPosition(savedPos);
					}
				}
			}
			while (rtNav.gotoNext());
			
		}
		
		if (oldPos!=null) {
			rtNav.restoreCurrentRecordPosition(oldPos);
		}
		
		return attNames;
	}
}
