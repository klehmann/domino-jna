package com.mindoo.domino.jna.internal.structs.compoundtext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
/**
 * This record contains the "Hide When" formula for a paragraph attributes block.<br>
 * <br>
 * This record is followed by the actual formula.
 */
public class NotesCDPabHideStruct extends BaseStructure implements IAdaptable {
	/** ORed with WORDRECORDLENGTH */
	public short Signature;
	/** (length is inclusive with this struct) */
	public short Length;
	public short PABID;
	/** C type : BYTE[8] */
	public byte[] Reserved = new byte[8];
	
	public NotesCDPabHideStruct() {
		super();
	}

	public static NotesCDPabHideStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDPabHideStruct>() {

			@Override
			public NotesCDPabHideStruct run() {
				return new NotesCDPabHideStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "PABID", "Reserved");
	}
	
	public NotesCDPabHideStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesCDPabHideStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDPabHideStruct>() {

			@Override
			public NotesCDPabHideStruct run() {
				return new NotesCDPabHideStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesCDPabHideStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCDPabHideStruct implements Structure.ByValue {
		
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCDPabHideStruct.class) {
			return (T) this;
		}
		return null;
	}

}
