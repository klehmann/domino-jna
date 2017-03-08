package com.mindoo.domino.jna.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Structure returned by NSFDbGetMajMinVersion to determine code level at runtime
 */
public class NotesBuildVersionStruct extends BaseStructure {
	/** Major version identifier */
	public int MajorVersion;
	/** Minor version identifier */
	public int MinorVersion;
	/** Maintenance Release identifier */
	public int QMRNumber;
	/** Maintenance Update identifier */
	public int QMUNumber;
	/** Hotfixes installed on machine */
	public int HotfixNumber;
	/** See BUILDVERFLAGS_xxx */
	public int Flags;
	/** Fixpack version installed on machine */
	public int FixpackNumber;
	/**
	 * Room for growth<br>
	 * C type : DWORD[2]
	 */
	public int[] Spare = new int[2];
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesBuildVersionStruct() {
		super();
	}
	
	public static NotesBuildVersionStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesBuildVersionStruct>() {

			@Override
			public NotesBuildVersionStruct run() {
				return new NotesBuildVersionStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("MajorVersion", "MinorVersion", "QMRNumber", "QMUNumber", "HotfixNumber", "Flags", "FixpackNumber", "Spare");
	}
	
	/**
	 * @param MajorVersion Major version identifier<br>
	 * @param MinorVersion Minor version identifier<br>
	 * @param QMRNumber Maintenance Release identifier<br>
	 * @param QMUNumber Maintenance Update identifier<br>
	 * @param HotfixNumber Hotfixes installed on machine<br>
	 * @param Flags See BUILDVERFLAGS_xxx<br>
	 * @param FixpackNumber Fixpack version installed on machine<br>
	 * @param Spare Room for growth<br>
	 * C type : DWORD[2]
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesBuildVersionStruct(int MajorVersion, int MinorVersion, int QMRNumber, int QMUNumber, int HotfixNumber, int Flags, int FixpackNumber, int Spare[]) {
		super();
		this.MajorVersion = MajorVersion;
		this.MinorVersion = MinorVersion;
		this.QMRNumber = QMRNumber;
		this.QMUNumber = QMUNumber;
		this.HotfixNumber = HotfixNumber;
		this.Flags = Flags;
		this.FixpackNumber = FixpackNumber;
		if ((Spare.length != this.Spare.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.Spare = Spare;
	}
	
	public static NotesBuildVersionStruct newInstance(final int MajorVersion, final int MinorVersion, final int QMRNumber, final int QMUNumber, final int HotfixNumber, final int Flags, final int FixpackNumber, final int Spare[]) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesBuildVersionStruct>() {

			@Override
			public NotesBuildVersionStruct run() {
				return new NotesBuildVersionStruct(MajorVersion, MinorVersion, QMRNumber, QMUNumber, HotfixNumber, Flags, FixpackNumber, Spare);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	protected NotesBuildVersionStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesBuildVersionStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesBuildVersionStruct>() {

			@Override
			public NotesBuildVersionStruct run() {
				return new NotesBuildVersionStruct(peer);
			}
		});	
	}
	
	public static class ByReference extends NotesBuildVersionStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesBuildVersionStruct implements Structure.ByValue {
		
	};
}
