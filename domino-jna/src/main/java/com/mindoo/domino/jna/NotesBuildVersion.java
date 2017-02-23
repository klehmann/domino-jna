package com.mindoo.domino.jna;

/**
 * Structure returned by NSFDbGetMajMinVersion to determine code level at runtime
 */
public class NotesBuildVersion {
	/** Major version identifier */
	private int m_majorVersion;
	/** Minor version identifier */
	private int m_minorVersion;
	/** Maintenance Release identifier */
	private int m_qmrNumber;
	/** Maintenance Update identifier */
	private int m_qmuNumber;
	/** Hotfixes installed on machine */
	private int m_hotfixNumber;
	/** See BUILDVERFLAGS_xxx */
	private int m_flags;
	/** Fixpack version installed on machine */
	private int m_fixpackNumber;

	public NotesBuildVersion(int majorVersion, int minorVersion, int qmrNumber, int qmuNumber, int hotfixNumber, int flags, int fixpackNumber, int Spare[]) {
		m_majorVersion = majorVersion;
		m_minorVersion = minorVersion;
		m_qmrNumber = qmrNumber;
		m_qmuNumber = qmuNumber;
		m_hotfixNumber = hotfixNumber;
		m_flags = flags;
		m_fixpackNumber = fixpackNumber;
	}
	
	/**
	 * Returns the major version identifier
	 * 
	 * @return identifier
	 */
	public int getMajorVersion() {
		return m_majorVersion;
	}
	
	/**
	 * Returns the minor version identifier
	 * 
	 * @return identifier
	 */
	public int getMinorVersion() {
		return m_minorVersion;
	}
	
	/**
	 * Returns the Maintenance Release identifier
	 * 
	 * @return identifier
	 */
	public int getQMRNumber() {
		return m_qmrNumber;
	}
	
	/**
	 * Returns the Maintenance Update identifier
	 * 
	 * @return identifier
	 */
	public int getQMUNumber() {
		return m_qmuNumber;
	}
	
	/**
	 * Returns the Hotfixes installed on machine
	 * 
	 * @return hotfixes
	 */
	public int getHotfixNumber() {
		return m_hotfixNumber;
	}
	
	/**
	 * Returns flags. See BUILDVERFLAGS_xxx
	 * 
	 * @return flags
	 */
	public int getFlags() {
		return m_flags;
	}
	
	/**
	 * Returns the Fixpack/feature version installed on machine
	 * 
	 * @return fixpack/feature version
	 */
	public int getFixpackNumber() {
		return m_fixpackNumber;
	}
}
