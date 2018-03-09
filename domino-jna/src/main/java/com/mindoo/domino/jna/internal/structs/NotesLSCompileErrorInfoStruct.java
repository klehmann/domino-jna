package com.mindoo.domino.jna.internal.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class NotesLSCompileErrorInfoStruct extends BaseStructure {
	public short Version;
	public short Line;
	public /* char* */Pointer pErrText;
	public /* char* */Pointer pErrFile;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesLSCompileErrorInfoStruct() {
		super();
	}
	
	public static NotesLSCompileErrorInfoStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesLSCompileErrorInfoStruct>() {

			@Override
			public NotesLSCompileErrorInfoStruct run() {
				return new NotesLSCompileErrorInfoStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("Version", "Line", "pErrText", "pErrFile");
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param version allows for future expansion - currently always 1
	 * @param line source line number of error, relative to LotusScript module containing the error, if applicable
	 * @param pErrText error text
	 * @param pErrFile file name, if applicable
	 */
	public NotesLSCompileErrorInfoStruct(short version, short line, Pointer pErrText, Pointer pErrFile) {
		super();
		this.Version = version;
		this.Line = line;
		this.pErrText = pErrText;
		this.pErrFile = pErrFile;
	}
	
	public static NotesLSCompileErrorInfoStruct newInstance(final short version, final short line, final Pointer pErrText, final Pointer pErrFile) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesLSCompileErrorInfoStruct>() {

			@Override
			public NotesLSCompileErrorInfoStruct run() {
				return new NotesLSCompileErrorInfoStruct(version, line, pErrText, pErrFile);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesLSCompileErrorInfoStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesLSCompileErrorInfoStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesLSCompileErrorInfoStruct>() {

			@Override
			public NotesLSCompileErrorInfoStruct run() {
				return new NotesLSCompileErrorInfoStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesLSCompileErrorInfoStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesLSCompileErrorInfoStruct implements Structure.ByValue {
		
	};
	
	public int getVersionAsInt() {
		return Version & 0xffff;
	}

	public int getLineAsInt() {
		return Line & 0xffff;
	}

}
