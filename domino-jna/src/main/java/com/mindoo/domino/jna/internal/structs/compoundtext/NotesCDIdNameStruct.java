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
 * This CD record describes the HTML field properties, ID, Class, Style, Title, Other and Name
 * associated for any given field defined within a Domino Form.
 */
public class NotesCDIdNameStruct extends BaseStructure implements IAdaptable {
	/** ORed with WORDRECORDLENGTH */
	public short Signature;
	/** (length is inclusive with this struct) */
	public short Length;
	/** Length of ID */
	public short wIdLength;
	/** Length of CLASS */
	public short wClassLen;
	/** Length of STYLE */
	public short wStyleLen;
	/** Length of TITLE */
	public short wTitleLen;
	/** Length of extra attribute/value pairs */
	public short wExtraLen;
	/** Length of NAME */
	public short wNameLen;
	/** C type : BYTE[10] */
	public byte[] reserved = new byte[10];

	public NotesCDIdNameStruct() {
		super();
	}

	public static NotesCDIdNameStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDIdNameStruct>() {

			@Override
			public NotesCDIdNameStruct run() {
				return new NotesCDIdNameStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("Signature", "Length", "wIdLength", "wClassLen", "wStyleLen", "wTitleLen", "wExtraLen", "wNameLen", "reserved");
	}

	public NotesCDIdNameStruct(Pointer peer) {
		super(peer);
	}

	public static NotesCDIdNameStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCDIdNameStruct>() {

			@Override
			public NotesCDIdNameStruct run() {
				return new NotesCDIdNameStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesCDIdNameStruct implements Structure.ByReference {

	};

	public static class ByValue extends NotesCDIdNameStruct implements Structure.ByValue {

	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCDIdNameStruct.class) {
			return (T) this;
		}
		return null;
	}
}
