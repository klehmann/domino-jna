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
 * 
 * This structure specifies the start of a "hot" region in a rich text field.<br>
 * <br>
 * Clicking on a hot region causes some other action to occur.<br>
 * <br>
 * For instance, clicking on a popup will cause a block of text associated with that popup to be displayed.
 */
public class NotesCdHotspotBeginStruct extends BaseStructure implements IAdaptable {
	// C type : WSIG */
	
	public short Signature; /* ORed with WORDRECORDLENGTH */
	public short Length;    /* (length is inclusive with this struct) */
	public short Type;
	public int Flags;
	public short DataLength;
	
	public NotesCdHotspotBeginStruct() {
		super();
	}
	
	public static NotesCdHotspotBeginStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCdHotspotBeginStruct>() {

			@Override
			public NotesCdHotspotBeginStruct run() {
				return new NotesCdHotspotBeginStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "Type", "Flags", "DataLength");
	}
	
	public NotesCdHotspotBeginStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesCdHotspotBeginStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCdHotspotBeginStruct>() {

			@Override
			public NotesCdHotspotBeginStruct run() {
				return new NotesCdHotspotBeginStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesCdHotspotBeginStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCdHotspotBeginStruct implements Structure.ByValue {
		
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCdHotspotBeginStruct.class) {
			return (T) this;
		}
		return null;
	}
	
}
