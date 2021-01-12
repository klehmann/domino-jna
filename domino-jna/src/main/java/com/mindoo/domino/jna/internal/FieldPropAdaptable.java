package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.internal.structs.compoundtext.IFieldHtmlPropsProvider;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.sun.jna.Pointer;

public class FieldPropAdaptable implements IAdaptable, IFieldHtmlPropsProvider {
		private Pointer m_fieldRecordWithHeader;
		private Pointer m_idNameCDRecord;
		
		public FieldPropAdaptable(Pointer fieldRecordWithHeader, Pointer idNameCDRecord) {
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
		public Pointer getCDRecordWithHeaderAndIDNameStruct() {
			return m_idNameCDRecord;
		}
		
		@Override
		public Pointer getCDRecordWithHeaderAndFieldStruct() {
			return m_fieldRecordWithHeader;
		}
		
	}