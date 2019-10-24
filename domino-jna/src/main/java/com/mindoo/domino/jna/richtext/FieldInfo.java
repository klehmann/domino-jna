package com.mindoo.domino.jna.richtext;

import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.internal.FormulaDecompiler;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.compoundtext.IFieldHtmlPropsProvider;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDIdNameStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Information read for a field in the database design
 * 
 * @author Karsten Lehmann
 */
public class FieldInfo {
	private int m_dataType;
	
	private String m_defaultValueFormula;
	private String m_inputTranslationFormula;
	private String m_inputValidityCheckFormula;
	
	private String m_name;
	private String m_description;
	private List<String> m_textListValues;

	private String m_htmlId;
	private String m_htmlClassName;
	private String m_htmlStyle;
	private String m_htmlTitle;
	private String m_htmlExtraAttr;
	private String m_htmlName;
	
	public FieldInfo(int dataType, String defaultValueFormula, String inputTranslationFormula, String inputValidityCheckFormula,
			String name, String description, List<String> textListValues) {
		m_dataType = dataType;
		m_defaultValueFormula = defaultValueFormula;
		m_inputTranslationFormula = inputTranslationFormula;
		m_inputValidityCheckFormula = inputValidityCheckFormula;
		m_name = name;
		m_description = description;
		m_textListValues = textListValues;
	}

	/**
	 * Returns the field data type
	 * 
	 * @return data type, e.g. {@link NotesItem#TYPE_TEXT}
	 */
	public int getDataType() {
		return m_dataType;
	}

	public FieldInfo(IAdaptable adaptable) {
		NotesCDFieldStruct cdField = adaptable.getAdapter(NotesCDFieldStruct.class);
		if (cdField!=null) {
			cdField.read();
			
			m_dataType = cdField.DataType;
			
			int dvLength = cdField.DVLength & 0xffff;
			int itLength = cdField.ITLength & 0xffff;
			int ivLength = cdField.IVLength & 0xffff;
			int nameLength = cdField.NameLength & 0xffff;
			int descriptionLength = cdField.DescLength & 0xffff;
			
			//start of CD record data
			Pointer ptrStruct = cdField.getPointer();
			
			//flexible size data comes after struct fields
			Pointer ptrFlexDataStart = ptrStruct.share(NotesConstants.notesCDFieldStructSize);
			
			if (dvLength > 0) {
				Pointer defaultValueFormulaPtr = ptrFlexDataStart;
				m_defaultValueFormula = FormulaDecompiler.decompileFormula(defaultValueFormulaPtr);
			}
			
			if (itLength > 0) {
				Pointer inputTranslationFormulaPtr = ptrFlexDataStart.share(dvLength);
				m_inputTranslationFormula = FormulaDecompiler.decompileFormula(inputTranslationFormulaPtr);
			}
			
			if (ivLength > 0) {
				Pointer inputValidityCheckFormulaPtr = ptrFlexDataStart.share(dvLength + itLength);
				m_inputValidityCheckFormula = FormulaDecompiler.decompileFormula(inputValidityCheckFormulaPtr);
			}
			
			int namePtrOffset = dvLength + itLength + ivLength;
			Pointer namePtr = ptrFlexDataStart.share(namePtrOffset);
			m_name = NotesStringUtils.fromLMBCS(namePtr, nameLength);
			
			int descPtrOffset = dvLength + itLength + ivLength + nameLength;
			Pointer descriptionPtr = ptrFlexDataStart.share(descPtrOffset);
			m_description = NotesStringUtils.fromLMBCS(descriptionPtr, descriptionLength);
			
			int textValueLength = cdField.TextValueLength & 0xffff;
			if (textValueLength>0) {
				int textValuePtrOffset = dvLength + itLength + ivLength + nameLength + descriptionLength;
				Pointer textlistValuePtr = ptrFlexDataStart.share(textValuePtrOffset);
				m_textListValues = (List) ItemDecoder.decodeTextListValue(textlistValuePtr, false);
			}
		}
		else {
			throw new IllegalArgumentException("Could not find any supported adapter to read data");
		}
		
		//read optional HTML properties
		IFieldHtmlPropsProvider htmlPropsProvider = adaptable.getAdapter(IFieldHtmlPropsProvider.class);
		if (htmlPropsProvider!=null) {
			Memory idNameMem = htmlPropsProvider.getCDRecordWithHeaderAndIDNameStruct();
			if (idNameMem!=null) {
				NotesCDIdNameStruct idNameStruct = NotesCDIdNameStruct.newInstance(idNameMem);
				idNameStruct.read();

				Pointer ptrData = idNameMem.share(NotesConstants.notesCDIdNameStructSize);
				
				m_htmlId = "";
				if (idNameStruct.wIdLength != 0) {
					m_htmlId = NotesStringUtils.fromLMBCS(ptrData, (int) (idNameStruct.wIdLength & 0xffff) );
					
					ptrData = ptrData.share((int) (idNameStruct.wIdLength & 0xffff));
				}
				
				m_htmlClassName = "";
				if (idNameStruct.wClassLen != 0) {
					m_htmlClassName = NotesStringUtils.fromLMBCS(ptrData, (int) (idNameStruct.wClassLen & 0xffff) );
					
					ptrData = ptrData.share((int) (idNameStruct.wClassLen & 0xffff));
				}
				
				m_htmlStyle = "";
				if (idNameStruct.wStyleLen != 0) {
					m_htmlStyle = NotesStringUtils.fromLMBCS(ptrData, (int) (idNameStruct.wStyleLen & 0xffff) );
					
					ptrData = ptrData.share((int) (idNameStruct.wStyleLen & 0xffff));
				}
				
				m_htmlTitle = "";
				if (idNameStruct.wTitleLen != 0) {
					m_htmlTitle = NotesStringUtils.fromLMBCS(ptrData, (int) (idNameStruct.wTitleLen & 0xffff) );
					
					ptrData = ptrData.share((int) (idNameStruct.wTitleLen & 0xffff));
				}
				
				m_htmlExtraAttr = "";
				if (idNameStruct.wExtraLen != 0) {
					m_htmlExtraAttr = NotesStringUtils.fromLMBCS(ptrData, (int) (idNameStruct.wExtraLen & 0xffff) );
					
					ptrData = ptrData.share((int) (idNameStruct.wExtraLen & 0xffff));
				}
				
				m_htmlName = "";
				if (idNameStruct.wNameLen != 0) {
					m_htmlName = NotesStringUtils.fromLMBCS(ptrData, (int) (idNameStruct.wNameLen & 0xffff) );
					
					ptrData = ptrData.share((int) (idNameStruct.wNameLen & 0xffff));
				}
			}
		}
	}
	
	/**
	 * Returns the decompiled default value formula or <code>null</code> if not present
	 * 
	 * @return formula or null
	 */
	public String getDefaultValueFormula() {
		return m_defaultValueFormula;
	}

	/**
	 * Returns the decompiled default input translation formula or <code>null</code> if not present
	 * 
	 * @return formula or null
	 */
	public String getInputTranslationFormula() {
		return m_inputTranslationFormula;
	}

	/**
	 * Returns the decompiled input validation formula or <code>null</code> if not present
	 * 
	 * @return formula or null
	 */
	public String getInputValidityCheckFormula() {
		return m_inputValidityCheckFormula;
	}

	public String getName() {
		return m_name==null ? "" : m_name;
	}

	/**
	 * Returns the content of "Help description" in the field properties
	 * 
	 * @return description
	 */
	public String getDescription() {
		return m_description==null ? "" : m_description;
	}

	/**
	 * If the field is a static textlist, this method returns the text list values
	 * 
	 * @return values or null if not set
	 */
	public List<String> getTextListValues() {
		return m_textListValues;
	}
	
	public String getHtmlId() {
		return m_htmlId;
	}
	
	public String getHtmlClassName() {
		return m_htmlClassName;
	}
	
	public String getHtmlStyle() {
		return m_htmlStyle;
	}
	
	public String getHtmlTitle() {
		return m_htmlTitle;
	}
	
	public String getHtmlExtraAttr() {
		return m_htmlExtraAttr;
	}
	
	public String getHtmlName() {
		return m_htmlName;
	}

	
	@Override
	public String toString() {
		return "FieldInfo [name="+getName()+", type="+getDataType()+", description="+getDescription()+", default="+getDefaultValueFormula()+
				", inputtranslation="+getInputTranslationFormula()+", validation="+getInputValidityCheckFormula()+
				", textlistvalues="+getTextListValues()+", htmlid="+getHtmlId()+", htmlclassname="+getHtmlClassName()+
				", htmlstyle="+getHtmlStyle()+", htmltitle="+getHtmlTitle()+", htmlattr="+getHtmlExtraAttr()+
				", htmlname="+getHtmlName()+"]";

	}

}