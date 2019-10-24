package com.mindoo.domino.jna.internal.structs.compoundtext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
/**
 * This structure specifies a format for paragraphs in a rich-text field.<br>
 * There may be more than one paragraph using the same paragraph format, but there may
 * be no more than one CDPABDEFINITION with the same ID in a rich-text field.  
 */
public class NotesCDPabDefinitionStruct extends BaseStructure implements IAdaptable {
	/** ORed with WORDRECORDLENGTH */
	public short Signature;
	/** (length is inclusive with this struct) */
	public short Length;
	/** ID of this PAB */
	public short PABID;
	/** paragraph justification type */
	public short JustifyMode;
	/** (2*(Line Spacing-1)) (0:1,1:1.5,2:2,etc) */
	public short LineSpacing;
	/** # LineSpacing units above para */
	public short ParagraphSpacingBefore;
	/** # LineSpacing units below para */
	public short ParagraphSpacingAfter;
	/** leftmost margin, twips rel to abs left */
	public short LeftMargin;
	/** rightmost margin, twips rel to abs right */
	public short RightMargin;
	/** leftmost margin on first line */
	public short FirstLineLeftMargin;
	/** number of tab stops in table */
	public short Tabs;
	/**
	 * table of tab stop positions, negative<br>
	 * C type : SWORD[MAXTABS]
	 */
	public short[] Tab = new short[NotesConstants.MAXTABS];
	/** paragraph attribute flags - PABFLAG_xxx */
	public short Flags;
	/** 2 bits per tab */
	public int TabTypes;
	/** extra paragraph attribute flags - PABFLAG2_xxx */
	public short Flags2;
	
	public NotesCDPabDefinitionStruct() {
		super();
	}
	
	public static NotesCDPabDefinitionStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDPabDefinitionStruct>() {

			@Override
			public NotesCDPabDefinitionStruct run() {
				return new NotesCDPabDefinitionStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "PABID", "JustifyMode", "LineSpacing", "ParagraphSpacingBefore", "ParagraphSpacingAfter", "LeftMargin", "RightMargin", "FirstLineLeftMargin", "Tabs", "Tab", "Flags", "TabTypes", "Flags2");
	}
	
	public NotesCDPabDefinitionStruct(Pointer peer) {
		super(peer);
	}

	public static NotesCDPabDefinitionStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDPabDefinitionStruct>() {

			@Override
			public NotesCDPabDefinitionStruct run() {
				return new NotesCDPabDefinitionStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesCDPabDefinitionStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCDPabDefinitionStruct implements Structure.ByValue {
		
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCDPabDefinitionStruct.class) {
			return (T) this;
		}
		return null;
	}

}
