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
 * This CD record defines a resource within a database.<br>
 * There may be many resources defined within a particular database.<br>
 * A resource can be an image, an applet, a shared field or a script library.
 */
public class NotesCDResourceStruct extends BaseStructure implements IAdaptable {
	/** ORed with WORDRECORDLENGTH */
	public short Signature;
	/** (length is inclusive with this struct) */
	public short Length;
	/** one of CDRESOURCE_FLAGS_xxx */
	public int Flags;
	/** one of CDRESOURCE_TYPE_xxx */
	public short Type;
	/** one of CDRESOURCE_CLASS_xxx */
	public short ResourceClass;
	/** meaning depends on Type */
	public short Length1;
	/** length of the server hint */
	public short ServerHintLength;
	/** length of the file hint */
	public short FileHintLength;
	/** C type : BYTE[8] */
	public byte[] Reserved = new byte[8];
	
	public NotesCDResourceStruct() {
		super();
	}

	public static NotesCDResourceStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDResourceStruct>() {

			@Override
			public NotesCDResourceStruct run() {
				return new NotesCDResourceStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "Flags", "Type", "ResourceClass", "Length1", "ServerHintLength", "FileHintLength", "Reserved");
	}
	
	public NotesCDResourceStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesCDResourceStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDResourceStruct>() {

			@Override
			public NotesCDResourceStruct run() {
				return new NotesCDResourceStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesCDResourceStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCDResourceStruct implements Structure.ByValue {
		
	};
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCDResourceStruct.class) {
			return (T) this;
		}
		return null;
	}

}
