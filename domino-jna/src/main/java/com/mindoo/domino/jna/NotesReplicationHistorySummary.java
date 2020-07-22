package com.mindoo.domino.jna;

import java.util.Set;

import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;

/**
 * One entry of the database replication history
 */
public class NotesReplicationHistorySummary {
	private NotesTimeDate m_replicationTime;
	private AclLevel m_accessLevel;
	private Set<AclFlag> m_accessFlags;
	private ReplicationDirection m_direction;
	private String m_server;
	private String m_filepath;

	/**
	 * These values describe the direction member of the {@link NotesReplicationHistorySummary}
	 * entry (the direction of the replication in the replication history). 
	 */
	public enum ReplicationDirection {

		NEVER((short) 0),
		SEND((short) 1),
		RECEIVE((short) 2);

		private Short m_value;

		private ReplicationDirection(short value) {
			m_value = value;
		}

		public short getValue() {
			return m_value;
		}
	}

	
	public NotesReplicationHistorySummary(NotesTimeDate replicationTime, AclLevel accessLevel,
			Set<AclFlag> accessFlags, ReplicationDirection direction, String server, String filePath) {
		m_replicationTime = replicationTime;
		m_accessLevel = accessLevel;
		m_accessFlags = accessFlags;
		m_direction = direction;
		m_server = server;
		m_filepath = filePath;
	}
	
	public NotesTimeDate getReplicationTime() {
		return m_replicationTime;
	}
	
	public AclLevel getAccessLevel() {
		return m_accessLevel;
	}
	
	public Set<AclFlag> getAccessFlags() {
		return m_accessFlags;
	}
	
	public ReplicationDirection getReplicationDirection() {
		return m_direction;
	}

	public String getServer() {
		return m_server;
	}
	
	public String getFilePath() {
		return m_filepath;
	}

	@Override
	public String toString() {
		return "ReplicationHistorySummary [server=" + m_server + ", filepath=" + m_filepath
				+ ", replicationtime=" + m_replicationTime + ", direction=" + m_direction
				+ ", accesslevel=" + m_accessLevel + ", accessflags=" + m_accessFlags + "]";
	}

	
}
