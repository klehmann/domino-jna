package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.internal.structs.compoundtext.IFieldHtmlPropsProvider;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.sun.jna.Memory;

public class FieldPropAdaptable implements IAdaptable, IFieldHtmlPropsProvider {
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
		
		@Override
		public Memory getCDRecordWithHeaderAndFieldStruct() {
			return m_fieldRecordWithHeader;
		}
		
	}