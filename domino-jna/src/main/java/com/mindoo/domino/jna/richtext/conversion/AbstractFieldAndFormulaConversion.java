package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.FormulaCompiler;
import com.mindoo.domino.jna.internal.FormulaDecompiler;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDPabHideStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDResourceStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCdHotspotBeginStruct;
import com.mindoo.domino.jna.richtext.FieldInfo;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Abstract base class to convert fields in design richtext. Conversion includes field name and description
 * change as well as text replacement and recompilation of default value, input translation and
 * input validity check formulas.
 * 
 * @author Karsten Lehmann
 */
public abstract class AbstractFieldAndFormulaConversion implements IRichTextConversion {
	/** type of field formula */
	public static enum FormulaType { DEFAULTVALUE, INPUTTRANSLATION, INPUTVALIDITYCHECK }
	
	/**
	 * Check here if a formula needs a change
	 * 
	 * @param fieldName name of current field
	 * @param type type of formula
	 * @param formula formula
	 * @return true if change is required
	 */
	protected abstract boolean fieldFormulaContainsMatch(String fieldName, FormulaType type, String formula);
	
	/**
	 * Apply change to formula
	 * 
	 * @param fieldName name of current field
	 * @param type type of formula
	 * @param formula formula
	 * @return changed formula
	 */
	protected abstract String replaceAllMatchesInFieldFormula(String fieldName, FormulaType type, String formula);

	/**
	 * Check here if a hide when formula needs a change
	 * 
	 * @param formula hide when formula
	 * @return true if change is required
	 */
	protected abstract boolean hideWhenFormulaContainsMatch(String formula);

	/**
	 * Apply change to hide when formula
	 * 
	 * @param formula hide when formula
	 * @return changed formula
	 */
	protected abstract String replaceAllMatchesInHideWhenFormula(String formula);

	/**
	 * Check here if a hotspot formula needs a change
	 * 
	 * @param formula hotspot formula
	 * @return true if change is required
	 */
	protected abstract boolean hotspotFormulaContainsMatch(String formula);

	/**
	 * Apply change to hotspot formula
	 * 
	 * @param formula hotspot formula
	 * @return changed formula
	 */
	protected abstract String replaceAllMatchesInHotspotFormula(String formula);

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
	 * @param fieldName name of current field
	 * @param fieldDesc field description
	 * @return true if change is required
	 */
	protected abstract boolean fieldDescriptionContainsMatch(String fieldName, String fieldDesc);
	
	/**
	 * Apply change to field description
	 * 
	 * @param fieldName name of current field
	 * @param fieldDesc field description
	 * @return new field description
	 */
	protected abstract String replaceAllMatchesInFieldDescription(String fieldName, String fieldDesc);
	
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
					
					String fieldName = fieldInfo.getName();
					
					if (fieldNameContainsMatch(fieldName)) {
						return true;
					}
					
					if (!StringUtil.isEmpty(fieldInfo.getDescription()) && fieldDescriptionContainsMatch(fieldName, fieldInfo.getDescription())) {
						return true;
					}
					
					String defaultValueFormula = fieldInfo.getDefaultValueFormula();
					if (!StringUtil.isEmpty(defaultValueFormula) && fieldFormulaContainsMatch(fieldName, FormulaType.DEFAULTVALUE, defaultValueFormula)) {
						return true;
					}
					
					String itFormula = fieldInfo.getInputTranslationFormula();
					if (!StringUtil.isEmpty(itFormula) && fieldFormulaContainsMatch(fieldName, FormulaType.INPUTTRANSLATION, itFormula)) {
						return true;
					}
					
					String ivFormula = fieldInfo.getInputValidityCheckFormula();
					if (!StringUtil.isEmpty(ivFormula) && fieldFormulaContainsMatch(fieldName, FormulaType.INPUTVALIDITYCHECK, ivFormula)) {
						return true;
					}
				}
				else if (CDRecordType.PABHIDE.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					Memory recordData = nav.getCurrentRecordDataWithHeader();
					
					NotesCDPabHideStruct hideWhenStruct = NotesCDPabHideStruct.newInstance(recordData);
					hideWhenStruct.read();
					
					int formulaLen = (int) (recordData.size() - NotesConstants.notesCDPabhideStructSize);
					if (formulaLen>0) {
						Pointer formulaPtr = recordData.share(NotesConstants.notesCDPabhideStructSize);
						String hwFormula = FormulaDecompiler.decompileFormula(formulaPtr);
						if (!StringUtil.isEmpty(hwFormula) && hideWhenFormulaContainsMatch(hwFormula)) {
							return true;
						}
					}
				}
				else if (CDRecordType.HOTSPOTBEGIN.getConstant() == nav.getCurrentRecordTypeAsShort() ||
						CDRecordType.V4HOTSPOTBEGIN.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					Memory recordData = nav.getCurrentRecordDataWithHeader();
					
					NotesCdHotspotBeginStruct hotspotStruct = NotesCdHotspotBeginStruct.newInstance(recordData);
					hotspotStruct.read();
					
					if ((hotspotStruct.Flags & NotesConstants.HOTSPOTREC_RUNFLAG_FORMULA) == NotesConstants.HOTSPOTREC_RUNFLAG_FORMULA) {
						int dataLengthAsInt = hotspotStruct.DataLength & 0xffff;
						
						if (dataLengthAsInt > 0) {
							Pointer ptrFormula = recordData.share(NotesConstants.notesCDHotspotBeginStructSize);
							String formula = FormulaDecompiler.decompileFormula(ptrFormula);
							
							if (!StringUtil.isEmpty(formula) && hotspotFormulaContainsMatch(formula)) {
								return true;
							}
						}
					}
				}
				else if (CDRecordType.HREF.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					// e.g. picture element with computed filename
					
					Memory recordData = nav.getCurrentRecordDataWithHeader();
					
					NotesCDResourceStruct resourceStruct = NotesCDResourceStruct.newInstance(recordData);
					resourceStruct.read();
					
					int cdResourceSize = NotesConstants.notesCDResourceStructSize; // 34
					Pointer ptr = recordData.share(cdResourceSize);
					int serverHintLengthAsInt = (int) (resourceStruct.ServerHintLength & 0xffff);
					
					String serverHint="";
					if (serverHintLengthAsInt>0) {
						serverHint = NotesStringUtils.fromLMBCS(ptr, serverHintLengthAsInt);
						ptr = ptr.share(serverHintLengthAsInt);
					}
					
					int fileHintLengthAsInt = (int) (resourceStruct.FileHintLength & 0xffff);
					
					String fileHint="";
					if (fileHintLengthAsInt>0) {
						fileHint = NotesStringUtils.fromLMBCS(ptr, fileHintLengthAsInt);
						ptr = ptr.share(fileHintLengthAsInt);
					}
					
					if (resourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_URL) {
						if((resourceStruct.Flags & NotesConstants.CDRESOURCE_FLAGS_FORMULA) == NotesConstants.CDRESOURCE_FLAGS_FORMULA) {
							int formulaLengthAsInt = (int) (resourceStruct.Length1  & 0xffff);
							if (formulaLengthAsInt>0) {
								String formula = FormulaDecompiler.decompileFormula(ptr);
								if (!StringUtil.isEmpty(formula) && hotspotFormulaContainsMatch(formula)) {
									return true;
								}
							}
						}
						
					}
					else if (resourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_NAMEDELEMENT) {
						//DBID to target DB or 0 for current database
						NotesTimeDateStruct replicaId = NotesTimeDateStruct.newInstance(ptr);
						ptr = ptr.share(NotesConstants.timeDateSize);
						
						if((resourceStruct.Flags & NotesConstants.CDRESOURCE_FLAGS_FORMULA) == NotesConstants.CDRESOURCE_FLAGS_FORMULA) {
							int formulaLengthAsInt = (int) (resourceStruct.Length1  & 0xffff);
							if (formulaLengthAsInt>0) {
								String formula = FormulaDecompiler.decompileFormula(ptr);
								if (!StringUtil.isEmpty(formula) && hotspotFormulaContainsMatch(formula)) {
									return true;
								}
							}
						}
					}
					else if (resourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_ACTION) {
						if((resourceStruct.Flags & NotesConstants.CDRESOURCE_FLAGS_FORMULA) == NotesConstants.CDRESOURCE_FLAGS_FORMULA) {
							int formulaLengthAsInt = (int) (resourceStruct.Length1  & 0xffff);
							if (formulaLengthAsInt>0) {
								String formula = FormulaDecompiler.decompileFormula(ptr);
								if (!StringUtil.isEmpty(formula) && hotspotFormulaContainsMatch(formula)) {
									return true;
								}
							}
						}
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
					
					String origFieldName = fieldInfo.getName();
					String fieldName = origFieldName;
					String fieldDesc = fieldInfo.getDescription();
					
					String defaultValueFormula = fieldInfo.getDefaultValueFormula();
					String itFormula = fieldInfo.getInputTranslationFormula();
					String ivFormula = fieldInfo.getInputValidityCheckFormula();
					
					boolean hasMatch = false;
					
					if (fieldNameContainsMatch(fieldName)) {
						hasMatch = true;
						fieldName = replaceAllMatchesInFieldName(fieldName);
					}
					
					if (!StringUtil.isEmpty(fieldDesc) && fieldDescriptionContainsMatch(origFieldName, fieldDesc)) {
						hasMatch = true;
						fieldDesc = replaceAllMatchesInFieldDescription(origFieldName, fieldDesc);
					}

					if (!StringUtil.isEmpty(itFormula) && fieldFormulaContainsMatch(origFieldName, FormulaType.DEFAULTVALUE, defaultValueFormula)) {
						hasMatch = true;
						defaultValueFormula = replaceAllMatchesInFieldFormula(origFieldName, FormulaType.DEFAULTVALUE, defaultValueFormula);
					}

					if (!StringUtil.isEmpty(itFormula) && fieldFormulaContainsMatch(origFieldName, FormulaType.INPUTTRANSLATION, itFormula)) {
						hasMatch = true;
						itFormula = replaceAllMatchesInFieldFormula(origFieldName, FormulaType.INPUTTRANSLATION, itFormula);
					}
					
					if (!StringUtil.isEmpty(ivFormula) && fieldFormulaContainsMatch(origFieldName, FormulaType.INPUTVALIDITYCHECK, ivFormula)) {
						hasMatch = true;
						ivFormula = replaceAllMatchesInFieldFormula(origFieldName, FormulaType.INPUTVALIDITYCHECK, ivFormula);
					}
					
					if (hasMatch) {
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
							throw new NotesError(0, "Error compiling default value formula of field "+origFieldName, e);
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
							throw new NotesError(0, "Error compiling input translation formula of field "+origFieldName, e);
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
							throw new NotesError(0, "Error compiling input validity check formula of field "+origFieldName, e);
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
				else if (CDRecordType.PABHIDE.getConstant() == source.getCurrentRecordTypeAsShort()) {
					Memory recordData = source.getCurrentRecordDataWithHeader();
					
					NotesCDPabHideStruct hideWhenStruct = NotesCDPabHideStruct.newInstance(recordData);
					hideWhenStruct.read();
					
					boolean hasMatch = false;
					
					int formulaLen = (int) (recordData.size() - NotesConstants.notesCDPabhideStructSize);
					if (formulaLen>0) {
						Pointer formulaPtr = recordData.share(NotesConstants.notesCDPabhideStructSize);
						String hwFormula = FormulaDecompiler.decompileFormula(formulaPtr);
						
						if (!StringUtil.isEmpty(hwFormula) && hideWhenFormulaContainsMatch(hwFormula)) {
							hwFormula = replaceAllMatchesInHideWhenFormula(hwFormula);
							
							byte[] compiledHwFormula;
							try {
								if (!StringUtil.isEmpty(hwFormula)) {
									compiledHwFormula = FormulaCompiler.compileFormula(hwFormula);
								}
								else {
									compiledHwFormula = new byte[0];
								}
							}
							catch (FormulaCompilationError e) {
								throw new NotesError(0, "Error compiling hide when formula", e);
							}
							
							int newRecordLength = NotesConstants.notesCDPabhideStructSize + compiledHwFormula.length;
							Memory newCdPabHideStructureWithHeaderMem = new Memory(newRecordLength);
							//copy old data
							newCdPabHideStructureWithHeaderMem.write(0, recordData.getByteArray(0, NotesConstants.notesCDPabhideStructSize), 0, NotesConstants.notesCDPabhideStructSize);
							
							NotesCDPabHideStruct newHideWhenStruct = NotesCDPabHideStruct.newInstance(newCdPabHideStructureWithHeaderMem);
							newHideWhenStruct.read();
							newHideWhenStruct.Length = (short) (newRecordLength & 0xffff);
							newHideWhenStruct.write();

							//append new compiled formula
							newCdPabHideStructureWithHeaderMem.write(NotesConstants.notesCDPabhideStructSize, compiledHwFormula, 0, compiledHwFormula.length);

							//write new data to target
							target.addCDRecords(newCdPabHideStructureWithHeaderMem);
							
							hasMatch = true;
						}
					}
					
					if (!hasMatch) {
						source.copyCurrentRecordTo(target);
					}
				}
				else if (CDRecordType.HOTSPOTBEGIN.getConstant() == source.getCurrentRecordTypeAsShort() ||
						CDRecordType.V4HOTSPOTBEGIN.getConstant() == source.getCurrentRecordTypeAsShort()) {
					Memory recordData = source.getCurrentRecordDataWithHeader();
							
					NotesCdHotspotBeginStruct hotspotStruct = NotesCdHotspotBeginStruct.newInstance(recordData);
					hotspotStruct.read();
					
					boolean hasMatch = false;
					
					if ((hotspotStruct.Flags & NotesConstants.HOTSPOTREC_RUNFLAG_FORMULA) == NotesConstants.HOTSPOTREC_RUNFLAG_FORMULA) {
						int dataLengthAsInt = hotspotStruct.DataLength & 0xffff;
						
						if (dataLengthAsInt > 0) {
							Pointer ptrFormula = recordData.share(NotesConstants.notesCDHotspotBeginStructSize);
							String hotspotFormula = FormulaDecompiler.decompileFormula(ptrFormula);
							
							if (!StringUtil.isEmpty(hotspotFormula) && hotspotFormulaContainsMatch(hotspotFormula)) {
								hotspotFormula = replaceAllMatchesInHotspotFormula(hotspotFormula);
								
								byte[] compiledHotspotFormula;
								try {
									if (!StringUtil.isEmpty(hotspotFormula)) {
										compiledHotspotFormula = FormulaCompiler.compileFormula(hotspotFormula);
									}
									else {
										compiledHotspotFormula = new byte[0];
									}
								}
								catch (FormulaCompilationError e) {
									throw new NotesError(0, "Error compiling hotspot formula", e);
								}
								
								int newRecordLength = NotesConstants.notesCDHotspotBeginStructSize + compiledHotspotFormula.length;
								Memory newCdHotspotBeginStructureWithHeaderMem = new Memory(newRecordLength);
								//copy old data
								newCdHotspotBeginStructureWithHeaderMem.write(0, recordData.getByteArray(0, NotesConstants.notesCDHotspotBeginStructSize), 0, NotesConstants.notesCDHotspotBeginStructSize);
								
								NotesCdHotspotBeginStruct newHotspotBeginStruct = NotesCdHotspotBeginStruct.newInstance(newCdHotspotBeginStructureWithHeaderMem);
								newHotspotBeginStruct.read();
								newHotspotBeginStruct.Length = (short) (newRecordLength & 0xffff);
								newHotspotBeginStruct.DataLength = (short) (compiledHotspotFormula.length & 0xffff);
								//unsign hotspot
								if ((newHotspotBeginStruct.Flags & NotesConstants.HOTSPOTREC_RUNFLAG_SIGNED) == NotesConstants.HOTSPOTREC_RUNFLAG_SIGNED) {
									newHotspotBeginStruct.Flags -= NotesConstants.HOTSPOTREC_RUNFLAG_SIGNED;
								}
								
								newHotspotBeginStruct.write();

								//append new compiled formula
								newCdHotspotBeginStructureWithHeaderMem.write(NotesConstants.notesCDHotspotBeginStructSize, compiledHotspotFormula, 0, compiledHotspotFormula.length);

								//write new data to target
								target.addCDRecords(newCdHotspotBeginStructureWithHeaderMem);
								
								hasMatch = true;
							}
						}
					}
					
					if (!hasMatch) {
						source.copyCurrentRecordTo(target);
					}
				}
				else if (CDRecordType.HREF.getConstant() == source.getCurrentRecordTypeAsShort()) {
					// e.g. picture element with computed filename
					
					Memory recordData = source.getCurrentRecordDataWithHeader();
					
					NotesCDResourceStruct resourceStruct = NotesCDResourceStruct.newInstance(recordData);
					resourceStruct.read();
					
					int cdResourceSize = NotesConstants.notesCDResourceStructSize; // 34
					Pointer ptr = recordData.share(cdResourceSize);
					int serverHintLengthAsInt = (int) (resourceStruct.ServerHintLength & 0xffff);
					
					String serverHint="";
					if (serverHintLengthAsInt>0) {
						serverHint = NotesStringUtils.fromLMBCS(ptr, serverHintLengthAsInt);
						ptr = ptr.share(serverHintLengthAsInt);
					}
					
					int fileHintLengthAsInt = (int) (resourceStruct.FileHintLength & 0xffff);
					
					String fileHint="";
					if (fileHintLengthAsInt>0) {
						fileHint = NotesStringUtils.fromLMBCS(ptr, fileHintLengthAsInt);
						ptr = ptr.share(fileHintLengthAsInt);
					}
					
					boolean isMatch = false;
					
					if (resourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_URL) {
						if((resourceStruct.Flags & NotesConstants.CDRESOURCE_FLAGS_FORMULA) == NotesConstants.CDRESOURCE_FLAGS_FORMULA) {
							int formulaLengthAsInt = (int) (resourceStruct.Length1  & 0xffff);
							if (formulaLengthAsInt>0) {
								String formula = FormulaDecompiler.decompileFormula(ptr);
								if (!StringUtil.isEmpty(formula)) {
									String newFormula = replaceAllMatchesInHotspotFormula(formula);
									
									byte[] compiledFormula;
									try {
										if (!StringUtil.isEmpty(newFormula)) {
											compiledFormula = FormulaCompiler.compileFormula(newFormula);
										}
										else {
											compiledFormula = new byte[0];
										}
									}
									catch (FormulaCompilationError e) {
										throw new NotesError(0, "Error compiling resource formula", e);
									}
									
									int newRecordLengthNoFormula = NotesConstants.notesCDResourceStructSize +
											serverHintLengthAsInt +
											fileHintLengthAsInt;

									int newRecordLengthWithFormula = newRecordLengthNoFormula +
											compiledFormula.length;
									
									Memory newRecordDataWithHeader = new Memory(newRecordLengthWithFormula);
									
									//copy header data, server hint and file int
									newRecordDataWithHeader.write(0, recordData.getByteArray(0,
											newRecordLengthNoFormula), 0, newRecordLengthNoFormula);
									newRecordDataWithHeader.write(newRecordLengthNoFormula, compiledFormula, 0, compiledFormula.length);
									
									NotesCDResourceStruct newResourceCDStruct = NotesCDResourceStruct.newInstance(newRecordDataWithHeader);
									newResourceCDStruct.read();
									
									newResourceCDStruct.Length = (short) (newRecordLengthWithFormula & 0xffff);
									newResourceCDStruct.Length1 = (short) (compiledFormula.length & 0xffff);
									newResourceCDStruct.write();
									
									target.addCDRecords(newRecordDataWithHeader);
									
									isMatch = true;
								}
							}
						}
						
					}
					else if (resourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_NAMEDELEMENT) {
						//DBID to target DB or 0 for current database
						NotesTimeDateStruct replicaId = NotesTimeDateStruct.newInstance(ptr);
						ptr = ptr.share(NotesConstants.timeDateSize);
						
						if((resourceStruct.Flags & NotesConstants.CDRESOURCE_FLAGS_FORMULA) == NotesConstants.CDRESOURCE_FLAGS_FORMULA) {
							int formulaLengthAsInt = (int) (resourceStruct.Length1  & 0xffff);
							if (formulaLengthAsInt>0) {
								String formula = FormulaDecompiler.decompileFormula(ptr);
								if (!StringUtil.isEmpty(formula) && hotspotFormulaContainsMatch(formula)) {
									String newFormula = replaceAllMatchesInHotspotFormula(formula);
									
									byte[] compiledFormula;
									try {
										if (!StringUtil.isEmpty(newFormula)) {
											compiledFormula = FormulaCompiler.compileFormula(newFormula);
										}
										else {
											compiledFormula = new byte[0];
										}
									}
									catch (FormulaCompilationError e) {
										throw new NotesError(0, "Error compiling resource formula", e);
									}
									
									int newRecordLengthNoFormula = NotesConstants.notesCDResourceStructSize +
											serverHintLengthAsInt +
											fileHintLengthAsInt +
											NotesConstants.timeDateSize;

									int newRecordLengthWithFormula = newRecordLengthNoFormula +
											compiledFormula.length;
									
									Memory newRecordDataWithHeader = new Memory(newRecordLengthWithFormula);
									
									//copy header data, server hint and file int
									newRecordDataWithHeader.write(0, recordData.getByteArray(0,
											newRecordLengthNoFormula), 0, newRecordLengthNoFormula);
									newRecordDataWithHeader.write(newRecordLengthNoFormula, compiledFormula, 0, compiledFormula.length);
									
									NotesCDResourceStruct newResourceCDStruct = NotesCDResourceStruct.newInstance(newRecordDataWithHeader);
									newResourceCDStruct.read();
									
									newResourceCDStruct.Length = (short) (newRecordLengthWithFormula & 0xffff);
									newResourceCDStruct.Length1 = (short) (compiledFormula.length & 0xffff);
									newResourceCDStruct.write();
									
									target.addCDRecords(newRecordDataWithHeader);
									
									isMatch = true;
								}
							}
						}
					}
					else if (resourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_ACTION) {
						if((resourceStruct.Flags & NotesConstants.CDRESOURCE_FLAGS_FORMULA) == NotesConstants.CDRESOURCE_FLAGS_FORMULA) {
							int formulaLengthAsInt = (int) (resourceStruct.Length1  & 0xffff);
							if (formulaLengthAsInt>0) {
								String formula = FormulaDecompiler.decompileFormula(ptr);
								if (!StringUtil.isEmpty(formula) && hotspotFormulaContainsMatch(formula)) {
									String newFormula = replaceAllMatchesInHotspotFormula(formula);
									
									byte[] compiledFormula;
									try {
										if (!StringUtil.isEmpty(newFormula)) {
											compiledFormula = FormulaCompiler.compileFormula(newFormula);
										}
										else {
											compiledFormula = new byte[0];
										}
									}
									catch (FormulaCompilationError e) {
										throw new NotesError(0, "Error compiling resource formula", e);
									}
									
									int newRecordLengthNoFormula = NotesConstants.notesCDResourceStructSize +
											serverHintLengthAsInt +
											fileHintLengthAsInt;

									int newRecordLengthWithFormula = newRecordLengthNoFormula +
											compiledFormula.length;
									
									Memory newRecordDataWithHeader = new Memory(newRecordLengthWithFormula);
									
									//copy header data, server hint and file int
									newRecordDataWithHeader.write(0, recordData.getByteArray(0,
											newRecordLengthNoFormula), 0, newRecordLengthNoFormula);
									newRecordDataWithHeader.write(newRecordLengthNoFormula, compiledFormula, 0, compiledFormula.length);
									
									NotesCDResourceStruct newResourceCDStruct = NotesCDResourceStruct.newInstance(newRecordDataWithHeader);
									newResourceCDStruct.read();
									
									newResourceCDStruct.Length = (short) (newRecordLengthWithFormula & 0xffff);
									newResourceCDStruct.Length1 = (short) (compiledFormula.length & 0xffff);
									newResourceCDStruct.write();
									
									target.addCDRecords(newRecordDataWithHeader);
									
									isMatch = true;
								}
							}
						}
					}
					
					if (!isMatch) {
						source.copyCurrentRecordTo(target);
					}
					 
				}
				else {
					source.copyCurrentRecordTo(target);
				}
			}
			while (source.gotoNext());
		}
	}

}
