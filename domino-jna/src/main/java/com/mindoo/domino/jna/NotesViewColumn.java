package com.mindoo.domino.jna;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat2Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat3Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat4Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat5Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormatStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Container for all attributes that we extract from the view/collection design for a single column
 * 
 * @author Karsten Lehmann
 */
public class NotesViewColumn implements IViewColumn {
	private NotesViewFormat m_parentFormat;
	private String m_itemName;
	private String m_title;
	private byte[] m_formulaCompiled;
	private String m_formula;
	private byte[] m_constantValue;
	private int m_position;
	private int m_columnValuesIndex;
	private short m_flags1;
	private short m_flags2;
	
	private String m_resortToViewUNID;
	private int m_secondResortColumnIndex;
	
	public NotesViewColumn(NotesViewFormat parentFormat, String itemName, String title, byte[] formulaCompiled,
			byte[] constantValue, int position, int columnValuesIndex, IAdaptable data) {
		m_parentFormat = parentFormat;
		m_itemName = itemName;
		m_title = title;
		m_formulaCompiled = formulaCompiled;
		m_constantValue = constantValue;
		m_position = position;
		m_columnValuesIndex = columnValuesIndex;
		
		NotesViewColumnFormatStruct format = data.getAdapter(NotesViewColumnFormatStruct.class);
		if (format!=null)
			importData(format);
		NotesViewColumnFormat2Struct format2 = data.getAdapter(NotesViewColumnFormat2Struct.class);
		if (format2!=null)
			importData(format2);
		NotesViewColumnFormat3Struct format3 = data.getAdapter(NotesViewColumnFormat3Struct.class);
		if (format3!=null)
			importData(format3);
		NotesViewColumnFormat4Struct format4 = data.getAdapter(NotesViewColumnFormat4Struct.class);
		if (format4!=null)
			importData(format4);
		NotesViewColumnFormat5Struct format5 = data.getAdapter(NotesViewColumnFormat5Struct.class);
		if (format5!=null)
			importData(format5);
		
	}
	
	@Override
	public String getItemName() {
		return m_itemName;
	}
	
	@Override
	public String getTitle() {
		return m_title;
	}
	
	public int getPosition() {
		return m_position;
	}
	
	public int getColumnValuesIndex() {
		return m_columnValuesIndex;
	}
	
	public boolean isConstant() {
		return m_constantValue!=null && m_constantValue.length>0;
	}
	
	@Override
	public String getFormula() {
		if (m_formula==null) {
			if (m_formulaCompiled!=null && m_formulaCompiled.length>0) {
				//lazily decompile formula
				Memory formulaCompiledMem = new Memory(m_formulaCompiled.length);
				formulaCompiledMem.write(0, m_formulaCompiled, 0, m_formulaCompiled.length);
				
				short result;
				
				if (PlatformUtils.is64Bit()) {
					LongByReference rethFormulaText = new LongByReference();
					ShortByReference retFormulaTextLength = new ShortByReference();
					
					result = NotesNativeAPI64.get().NSFFormulaDecompile(formulaCompiledMem, true, rethFormulaText, retFormulaTextLength);
					NotesErrorUtils.checkResult(result);
					
					int iFormulaTextLength = (int) (retFormulaTextLength.getValue() & 0xffff);
					
					long hFormulaText = rethFormulaText.getValue();
					if (hFormulaText!=0) {
						Pointer ptr = Mem64.OSLockObject(hFormulaText);
						try {
							m_formula = NotesStringUtils.fromLMBCS(ptr, iFormulaTextLength);
						}
						finally {
							Mem64.OSUnlockObject(hFormulaText);
							result = Mem64.OSMemFree(hFormulaText);
							NotesErrorUtils.checkResult(result);
						}
					}
				}
				else {
					IntByReference rethFormulaText = new IntByReference();
					ShortByReference retFormulaTextLength = new ShortByReference();
					
					result = NotesNativeAPI32.get().NSFFormulaDecompile(formulaCompiledMem, true, rethFormulaText, retFormulaTextLength);
					NotesErrorUtils.checkResult(result);
					
					int iFormulaTextLength = (int) (retFormulaTextLength.getValue() & 0xffff);
					
					int hFormulaText = rethFormulaText.getValue();
					if (hFormulaText!=0) {
						Pointer ptr = Mem32.OSLockObject(hFormulaText);
						try {
							m_formula = NotesStringUtils.fromLMBCS(ptr, iFormulaTextLength);
						}
						finally {
							Mem32.OSUnlockObject(hFormulaText);
							result = Mem32.OSMemFree(hFormulaText);
							NotesErrorUtils.checkResult(result);
						}
					}
				}
			}
			else {
				m_formula = "";
			}
		}
		return m_formula;
	}

	private void importData(NotesViewColumnFormat5Struct format5) {
		// v5 formats not yet available, see ViewFormatDecoder for details
	}

	private void importData(NotesViewColumnFormat4Struct format4) {
		// v4 formats not yet available, see ViewFormatDecoder for details
	}

	private void importData(NotesViewColumnFormat3Struct format3) {
		// v3 formats not yet available, see ViewFormatDecoder for details
	}

	private void importData(NotesViewColumnFormat2Struct format2) {
		NotesUniversalNoteIdStruct resortToViewUNID = format2.ResortToViewUNID;
		m_resortToViewUNID = resortToViewUNID==null ? null : resortToViewUNID.toString();
		m_secondResortColumnIndex = (int) (format2.wSecondResortColumnIndex & 0xffff);
	}

	private void importData(NotesViewColumnFormatStruct format) {
		m_flags1 = format.Flags1;
		m_flags2 = format.Flags2;
	}

	public NotesViewFormat getParent() {
		return m_parentFormat;
	}

	public String getResortToViewUnid() {
		return m_resortToViewUNID;
	}
	
	public int getSecondResortColumnIndex() {
		return m_secondResortColumnIndex;
	}
	
	public boolean isSorted() {
		return (m_flags1 & NotesConstants.VCF1_M_Sort) == NotesConstants.VCF1_M_Sort;
	}
	
	public boolean isCategory() {
		return (m_flags1 & NotesConstants.VCF1_M_SortCategorize) == NotesConstants.VCF1_M_SortCategorize;
	}

	public boolean isSortedDescending() {
		return (m_flags1 & NotesConstants.VCF1_M_SortDescending) == NotesConstants.VCF1_M_SortDescending;
	}

	@Override
	public ColumnSort getSorting() {
		if (isSorted()) {
			if (isSortedDescending()) {
				return ColumnSort.DESCENDING;
			}
			else {
				return ColumnSort.ASCENDING;
			}
		}
		else {
			return ColumnSort.NONE;
		}
	}
	
	public boolean isHidden() {
		return (m_flags1 & NotesConstants.VCF1_M_Hidden) == NotesConstants.VCF1_M_Hidden;
	}

	public boolean isResponse() {
		return (m_flags1 & NotesConstants.VCF1_M_Response) == NotesConstants.VCF1_M_Response;
	}

	public boolean isHideDetail() {
		return (m_flags1 & NotesConstants.VCF1_M_HideDetail) == NotesConstants.VCF1_M_HideDetail;
	}

	public boolean isIcon() {
		return (m_flags1 & NotesConstants.VCF1_M_Icon) == NotesConstants.VCF1_M_Icon;
	}

	public boolean isResize() {
		return (m_flags1 & NotesConstants.VCF1_M_NoResize) != NotesConstants.VCF1_M_NoResize;
	}

	public boolean isResortAscending() {
		return (m_flags1 & NotesConstants.VCF1_M_ResortAscending) == NotesConstants.VCF1_M_ResortAscending;
	}

	public boolean isResortDescending() {
		return (m_flags1 & NotesConstants.VCF1_M_ResortDescending) == NotesConstants.VCF1_M_ResortDescending;
	}

	public boolean isShowTwistie() {
		return (m_flags1 & NotesConstants.VCF1_M_Twistie) == NotesConstants.VCF1_M_Twistie;
	}

	public boolean isResortToView() {
		return (m_flags1 & NotesConstants.VCF1_M_ResortToView) == NotesConstants.VCF1_M_ResortToView;
	}

	public boolean isSecondaryResort() {
		return (m_flags1 & NotesConstants.VCF1_M_SecondResort) == NotesConstants.VCF1_M_SecondResort;
	}

	public boolean isSecondaryResortDescending() {
		return (m_flags1 & NotesConstants.VCF1_M_SecondResortDescending) == NotesConstants.VCF1_M_SecondResortDescending;
	}

	public boolean isSortPermuted() {
		return (m_flags2 & NotesConstants.VCF2_M_SortPermute) == NotesConstants.VCF2_M_SortPermute;
	}

}
