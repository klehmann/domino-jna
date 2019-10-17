package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.FormulaCompiler;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.mindoo.domino.jna.richtext.FieldInfo;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;

/**
 * Abstract base class to convert fields in design richtext. Conversion includes field name and description
 * change as well as text replacement and recompilation of default value, input translation and
 * input validity check formulas.
 * 
 * @author Karsten Lehmann
 */
public abstract class AbstractFieldConversion implements IRichTextConversion {
	/** type of field formula */
	public static enum FormulaType { DEFAULTVALUE, INPUTTRANSLATION, INPUTVALIDITYCHECK }
	
	/**
	 * Check here if a formula needs a change
	 * 
	 * @param type type of formula
	 * @param formula formula
	 * @return true if change is required
	 */
	protected abstract boolean fieldFormulaContainsMatch(FormulaType type, String formula);
	
	/**
	 * Apply change to formula
	 * 
	 * @param type type of formula
	 * @param formula formula
	 * @return changed formula
	 */
	protected abstract String replaceAllMatchesInFieldFormula(FormulaType type, String formula);

	// ===================

	/**
	 * Check here if the field name needs a change
	 * 
	 * @param fieldName field name
	 * @return true if change is required
	 */
	protected abstract boolean fieldNameContainsMatch(String fieldName);
	
	/**
	 * Apply change to field name
	 * 
	 * @param fieldName field name
	 * @return new field name
	 */
	protected abstract String replaceAllMatchesInFieldName(String fieldName);
	
	// ===================
	
	/**
	 * Check here if the field description needs a change
	 * 
	 * @param fieldDesc field description
	 * @return true if change is required
	 */
	protected abstract boolean fieldDescriptionContainsMatch(String fieldDesc);
	
	/**
	 * Apply change to field description
	 * @param fieldDesc field description
	 * @return new field description
	 */
	protected abstract String replaceAllMatchesInFieldDescription(String fieldDesc);
	
	/**
	 * Override this method to check if a custom change to the CDField record
	 * is required
	 * 
	 * @param cdFieldStruct CDField structure
	 * @return true if change is required, default implementation returns false
	 */
	protected boolean isCustomFieldChangeRequired(NotesCDFieldStruct cdFieldStruct) {
		return false;
	}
	
	/**
	 * Empty method, can be used to apply custom field changes
	 * @param cdFieldStruct CDField structure
	 */
	protected void applyCustomFieldChanges(NotesCDFieldStruct cdFieldStruct) {
		//
	}
	
	@Override
	public boolean isMatch(IRichTextNavigator nav) {
		if (nav.gotoFirst()) {
			do {
				if (CDRecordType.FIELD.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					Memory recordData = nav.getCurrentRecordDataWithHeader();
					
					NotesCDFieldStruct cdField = NotesCDFieldStruct.newInstance(recordData);
					FieldInfo fieldInfo = new FieldInfo(cdField);
					
					if (fieldNameContainsMatch(fieldInfo.getName())) {
						return true;
					}
					
					if (!StringUtil.isEmpty(fieldInfo.getDescription()) && fieldDescriptionContainsMatch(fieldInfo.getDescription())) {
						return true;
					}
					
					String defaultValueFormula = fieldInfo.getDefaultValueFormula();
					if (!StringUtil.isEmpty(defaultValueFormula) && fieldFormulaContainsMatch(FormulaType.DEFAULTVALUE, defaultValueFormula)) {
						return true;
					}
					
					String itFormula = fieldInfo.getInputTranslationFormula();
					if (!StringUtil.isEmpty(itFormula) && fieldFormulaContainsMatch(FormulaType.INPUTTRANSLATION, itFormula)) {
						return true;
					}
					
					String ivFormula = fieldInfo.getInputValidityCheckFormula();
					if (!StringUtil.isEmpty(ivFormula) && fieldFormulaContainsMatch(FormulaType.INPUTVALIDITYCHECK, ivFormula)) {
						return true;
					}
				}
			}
			while (nav.gotoNext());
		}
		return false;
	}

	@Override
	public void convert(IRichTextNavigator source, ICompoundText target) {
		if (source.gotoFirst()) {
			do {
				if (CDRecordType.FIELD.getConstant() == source.getCurrentRecordTypeAsShort()) {
					Memory recordDataWithHeader = source.getCurrentRecordDataWithHeader();
					
					NotesCDFieldStruct cdField = NotesCDFieldStruct.newInstance(recordDataWithHeader);
					FieldInfo fieldInfo = new FieldInfo(cdField);
					
					String fieldName = fieldInfo.getName();
					String fieldDesc = fieldInfo.getDescription();
					
					String defaultValueFormula = fieldInfo.getDefaultValueFormula();
					String itFormula = fieldInfo.getInputTranslationFormula();
					String ivFormula = fieldInfo.getInputValidityCheckFormula();
					
					boolean hasMatch = false;
					
					if (fieldNameContainsMatch(fieldName)) {
						hasMatch = true;
						fieldName = replaceAllMatchesInFieldName(fieldName);
					}
					
					if (!StringUtil.isEmpty(fieldDesc) && fieldDescriptionContainsMatch(fieldDesc)) {
						hasMatch = true;
						fieldDesc = replaceAllMatchesInFieldDescription(fieldDesc);
					}

					if (!StringUtil.isEmpty(itFormula) && fieldFormulaContainsMatch(FormulaType.DEFAULTVALUE, defaultValueFormula)) {
						hasMatch = true;
						defaultValueFormula = replaceAllMatchesInFieldFormula(FormulaType.DEFAULTVALUE, defaultValueFormula);
					}

					if (!StringUtil.isEmpty(itFormula) && fieldFormulaContainsMatch(FormulaType.INPUTTRANSLATION, itFormula)) {
						hasMatch = true;
						itFormula = replaceAllMatchesInFieldFormula(FormulaType.INPUTTRANSLATION, itFormula);
					}
					
					if (!StringUtil.isEmpty(ivFormula) && fieldFormulaContainsMatch(FormulaType.INPUTVALIDITYCHECK, ivFormula)) {
						hasMatch = true;
						ivFormula = replaceAllMatchesInFieldFormula(FormulaType.INPUTVALIDITYCHECK, ivFormula);
					}
					
					if (!hasMatch) {
						source.copyCurrentRecordTo(target);
						return;
					}
					
					//recompile formulas
					
					byte[] compiledDefaultValueFormula;
					try {
						if (!StringUtil.isEmpty(defaultValueFormula)) {
							compiledDefaultValueFormula = FormulaCompiler.compileFormula(defaultValueFormula);
						}
						else {
							compiledDefaultValueFormula = new byte[0];
						}
					}
					catch (FormulaCompilationError e) {
						throw new NotesError(0, "Error compiling default value formula of field "+fieldInfo.getName(), e);
					}
					
					byte[] compiledItFormula;
					try {
						if (!StringUtil.isEmpty(itFormula)) {
							compiledItFormula = FormulaCompiler.compileFormula(itFormula);
						}
						else {
							compiledItFormula = new byte[0];
						}
					}
					catch (FormulaCompilationError e) {
						throw new NotesError(0, "Error compiling input translation formula of field "+fieldInfo.getName(), e);
					}
					
					byte[] compiledIvFormula;
					try {
						if (!StringUtil.isEmpty(ivFormula)) {
							compiledIvFormula = FormulaCompiler.compileFormula(ivFormula);
						}
						else {
							compiledIvFormula = new byte[0];
						}
					}
					catch (FormulaCompilationError e) {
						throw new NotesError(0, "Error compiling input validity check formula of field "+fieldInfo.getName(), e);
					}

					Memory fieldNameMem = NotesStringUtils.toLMBCS(fieldName, false);
					Memory fieldDescMem = NotesStringUtils.toLMBCS(fieldDesc, false);

					//allocate enough memory for the new CDfield structure and the texts/formulas
					Memory newCdFieldStructureWithHeaderMem = new Memory(
							NotesConstants.notesCDFieldStructSize +

							compiledDefaultValueFormula.length +
							compiledItFormula.length +
							compiledIvFormula.length +

							fieldNameMem.size() + 
							(fieldDescMem==null ? 0 : fieldDescMem.size())
							);

					//copy the old data for the CDField structure into byte array
					byte[] oldCdFieldDataWithHeader = recordDataWithHeader.getByteArray(0, NotesConstants.notesCDFieldStructSize);
					//and into newCdFieldStructureWithHeaderMem
					newCdFieldStructureWithHeaderMem.write(0, oldCdFieldDataWithHeader, 0, oldCdFieldDataWithHeader.length);
					
					NotesCDFieldStruct newCdField = NotesCDFieldStruct.newInstance(newCdFieldStructureWithHeaderMem);
					newCdField.read();
					
					applyCustomFieldChanges(newCdField);
					
					//write new total lengths of CD record including signature
					newCdField.Length = (short) (newCdFieldStructureWithHeaderMem.size() & 0xffff);
					
					//write lengths of compiled formulas and name/description
					newCdField.DVLength = (short) ((compiledDefaultValueFormula==null ? 0 : compiledDefaultValueFormula.length) & 0xffff);
					newCdField.ITLength = (short) ((compiledItFormula==null ? 0 : compiledItFormula.length) & 0xffff);
					newCdField.IVLength = (short) ((compiledIvFormula==null ? 0 : compiledIvFormula.length) & 0xffff);
					newCdField.NameLength = (short) ((fieldNameMem==null ? 0 : fieldNameMem.size()) & 0xffff);
					newCdField.DescLength = (short) ((fieldDescMem==null ? 0 : fieldDescMem.size()) & 0xffff);
					
					newCdField.write();

					//write flexible data into CD record
					int offset = NotesConstants.notesCDFieldStructSize;
					if (compiledDefaultValueFormula.length>0) {
						newCdFieldStructureWithHeaderMem.write(offset, compiledDefaultValueFormula, 0, compiledDefaultValueFormula.length);
						offset += compiledDefaultValueFormula.length;
					}

					if (compiledItFormula.length>0) {
						newCdFieldStructureWithHeaderMem.write(offset, compiledItFormula, 0, compiledItFormula.length);
						offset += compiledItFormula.length;
					}

					if (compiledIvFormula.length>0) {
						newCdFieldStructureWithHeaderMem.write(offset, compiledIvFormula, 0, compiledIvFormula.length);
						offset += compiledIvFormula.length;
					}

					newCdFieldStructureWithHeaderMem.write(offset, fieldNameMem.getByteArray(0, (int) fieldNameMem.size()),
							0, (int) fieldNameMem.size());
					offset += fieldNameMem.size();
					
					if (fieldDescMem!=null) {
						newCdFieldStructureWithHeaderMem.write(offset, fieldDescMem.getByteArray(0, (int) fieldDescMem.size()),
								0, (int) fieldDescMem.size());
						offset += fieldDescMem.size();
					}

					//write new data to target
					target.addCDRecords(newCdFieldStructureWithHeaderMem);
				}
				else {
					source.copyCurrentRecordTo(target);
				}
			}
			while (source.gotoNext());
		}
	}

}
