package com.mindoo.domino.jna.richtext;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.mindoo.domino.jna.richtext.IRichTextNavigator.RichTextNavPosition;
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
			do {
				if (CDRecordType.FIELD.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					//found record of type SIG_CD_FIELD
					Memory recordMemWithHeader = rtNav.getCurrentRecordDataWithHeader();
					
					NotesCDFieldStruct cdFieldStruct = NotesCDFieldStruct.newInstance(recordMemWithHeader);
					cdFieldStruct.read();
					
					FieldInfo fldInfo = new FieldInfo(cdFieldStruct);
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
}
