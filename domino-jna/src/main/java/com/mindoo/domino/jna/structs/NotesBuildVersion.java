package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Structure returned by NSFDbGetMajMinVersion to determine code level at runtime
 */
public class NotesBuildVersion extends Structure {
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
	public NotesBuildVersion() {
		super();
	}
	protected List<? > getFieldOrder() {
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
	 */
	public NotesBuildVersion(int MajorVersion, int MinorVersion, int QMRNumber, int QMUNumber, int HotfixNumber, int Flags, int FixpackNumber, int Spare[]) {
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
	public NotesBuildVersion(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesBuildVersion implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesBuildVersion implements Structure.ByValue {
		
	};
}
