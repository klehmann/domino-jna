package com.mindoo.domino.jna;

import com.mindoo.domino.jna.structs.NotesBuildVersionStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Structure returned by NSFDbGetMajMinVersion to determine code level at runtime
 */
public class NotesBuildVersion implements IAdaptable {
	private NotesBuildVersionStruct m_struct;
	
	public NotesBuildVersion() {
		this(NotesBuildVersionStruct.newInstance());
	}
	
	public NotesBuildVersion(NotesBuildVersionStruct struct) {
		m_struct = struct;
	}
	
	public NotesBuildVersion(Pointer p) {
		this(NotesBuildVersionStruct.newInstance(p));
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesBuildVersionStruct.class || clazz == Structure.class) {
			return (T) m_struct;
		}
		return null;
	}
	
	/**
	 * Returns the major version identifier
	 * 
	 * @return identifier
	 */
	public int getMajorVersion() {
		return m_struct.MajorVersion;
	}
	
	/**
	 * Returns the minor version identifier
	 * 
	 * @return identifier
	 */
	public int getMinorVersion() {
		return m_struct.MinorVersion;
	}
	
	/**
	 * Returns the Maintenance Release identifier
	 * 
	 * @return identifier
	 */
	public int getQMRNumber() {
		return m_struct.QMRNumber;
	}
	
	/**
	 * Returns the Maintenance Update identifier
	 * 
	 * @return identifier
	 */
	public int getQMUNumber() {
		return m_struct.QMUNumber;
	}
	
	/**
	 * Returns the Hotfixes installed on machine
	 * 
	 * @return hotfixes
	 */
	public int getHotfixNumber() {
		return m_struct.HotfixNumber;
	}
	
	/**
	 * Returns flags. See BUILDVERFLAGS_xxx
	 * 
	 * @return flags
	 */
	public int getFlags() {
		return m_struct.Flags;
	}
	
	/**
	 * Returns the Fixpack/feature version installed on machine
	 * 
	 * @return fixpack/feature version
	 */
	public int getFixpackNumber() {
		return m_struct.FixpackNumber;
	}
}
