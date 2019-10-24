package com.mindoo.domino.jna.richtext;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.internal.structs.compoundtext.IFieldHtmlPropsProvider;
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
	
	private static class FieldPropAdaptable implements IAdaptable, IFieldHtmlPropsProvider {
		private Memory m_fieldRecordWithHeader;
		private Memory m_idNameCDRecord;
		
		public FieldPropAdaptable(Memory fieldRecordWithHeader, Memory idNameCDRecord) {
			m_fieldRecordWithHeader = fieldRecordWithHeader;
			m_idNameCDRecord = idNameCDRecord;
		}

		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (clazz == NotesCDFieldStruct.class && m_fieldRecordWithHeader!=null) {
				NotesCDFieldStruct fieldStruct = NotesCDFieldStruct.newInstance(m_fieldRecordWithHeader);
				fieldStruct.read();
				return (T) fieldStruct;
			}
			else if (clazz == IFieldHtmlPropsProvider.class) {
				return (T) this;
			}
			return null;
		}

		@Override
		public Memory getCDRecordWithHeaderAndIDNameStruct() {
			return m_idNameCDRecord;
		}
	}
}
