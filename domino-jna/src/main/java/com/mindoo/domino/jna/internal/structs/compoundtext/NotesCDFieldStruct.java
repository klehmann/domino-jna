package com.mindoo.domino.jna.internal.structs.compoundtext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.mindoo.domino.jna.internal.structs.NotesNFMTStruct;
import com.mindoo.domino.jna.internal.structs.NotesTFMTStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Data of a CDFIELD compound context record
 */
public class NotesCDFieldStruct extends BaseStructure implements IAdaptable {
	/** ORed with WORDRECORDLENGTH */
	public short Signature;
	/** (length is inclusive with this struct) */
	public short Length;
	/** Field Flags (see Fxxx) */
	public short Flags;
	/** Alleged NSF Data Type */
	public short DataType;
	/**
	 * List Delimiters (LDELIM_xxx and<br>
	 * LDDELIM_xxx)
	 */
	public short ListDelim;
	/**
	 * Number format, if applicable<br>
	 * C type : NFMT
	 */
	public NotesNFMTStruct NumberFormat;
	/**
	 * Time format, if applicable<br>
	 * C type : TFMT
	 */
	public NotesTFMTStruct TimeFormat;
	/**
	 * displayed font<br>
	 * C type : FONTID
	 */
	public NotesFontIDFieldsStruct FontID;
	
	/** Default Value Formula */
	public short DVLength;
	/** Input Translation Formula */
	public short ITLength;
	/** Order in tabbing sequence */
	public short TabOrder;
	/** Input Validity Check Formula */
	public short IVLength;
	/** NSF Item Name */
	public short NameLength;
	/** Description of the item */
	public short DescLength;
	
	/**
	 * (Text List) List of valid text<br>
	 * values
	 */
	public short TextValueLength;
	
	public NotesCDFieldStruct() {
		super();
	}
	
	public static NotesCDFieldStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDFieldStruct>() {

			@Override
			public NotesCDFieldStruct run() {
				return new NotesCDFieldStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "Flags", "DataType", "ListDelim", "NumberFormat", "TimeFormat", "FontID", "DVLength", "ITLength", "TabOrder", "IVLength", "NameLength", "DescLength", "TextValueLength");
	}
	
	public NotesCDFieldStruct(Pointer peer) {
		super(peer);
	}

	public static NotesCDFieldStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDFieldStruct>() {

			@Override
			public NotesCDFieldStruct run() {
				return new NotesCDFieldStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesCDFieldStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCDFieldStruct implements Structure.ByValue {
		
	};
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCDFieldStruct.class) {
			return (T) this;
		}
		return null;
	}
	
}
