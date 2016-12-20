package com.mindoo.domino.jna.structs;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * CD Field data structure
 */
public class NotesCDField extends BaseStructure {
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
	
	//NFMT structure embedded
	/** Number of decimal digits */
	public byte Digits;
	/** Display Format */
	public byte Format;
	/** Display Attributes */
	public byte Attributes;
	public byte Unused;
	
	//TFMT structure embedded
	/** Date Display Format */
	public byte Date;
	/** Time Display Format */
	public byte Time;
	/** Time Zone Display Format */
	public byte Zone;
	/** Overall Date/Time Structure */
	public byte Structure;
	
	/** displayed font */
	public int FontID;
	
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
	public NotesCDField() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("Signature", "Length", "Flags", "DataType", "ListDelim", "Digits", "Format", "Attributes", "Unused", "Date", "Time", "Zone", "Structure", "FontID", "DVLength", "ITLength", "TabOrder", "IVLength", "NameLength", "DescLength", "TextValueLength");
	}
	public NotesCDField(Pointer peer) {
		super(peer);
//		setAlignType(ALIGN_DEFAULT);
	}
	public static class ByReference extends NotesCDField implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesCDField implements Structure.ByValue {
		
	};
}
