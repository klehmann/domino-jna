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
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class NotesCDExtFieldStruct extends BaseStructure implements IAdaptable {
	// C type : WSIG */
	
	public short Signature; /* ORed with WORDRECORDLENGTH */
	public short Length;    /* (length is inclusive with this struct) */
	
	/** Field Flags (see FEXT_xxx) */
	public int Flags1;
	public int Flags2;
	/**
	 * Field entry helper type<br>
	 * (see FIELD_HELPER_xxx)
	 */
	public short EntryHelper;
	/** Entry helper DB name length */
	public short EntryDBNameLen;
	/** Entry helper View name length */
	public short EntryViewNameLen;
	/** Entry helper column number */
	public short EntryColumnNumber;
	
	public NotesCDExtFieldStruct() {
		super();
	}
	
	public static NotesCDExtFieldStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDExtFieldStruct>() {

			@Override
			public NotesCDExtFieldStruct run() {
				return new NotesCDExtFieldStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "Flags1", "Flags2", "EntryHelper", "EntryDBNameLen", "EntryViewNameLen", "EntryColumnNumber");
	}

	public NotesCDExtFieldStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesCDExtFieldStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDExtFieldStruct>() {

			@Override
			public NotesCDExtFieldStruct run() {
				return new NotesCDExtFieldStruct(peer);
			}
		});
	}
	
	public static class ByReference extends NotesCDExtFieldStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCDExtFieldStruct implements Structure.ByValue {
		
	};
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCDExtFieldStruct.class) {
			return (T) this;
		}
		return null;
	}

}
