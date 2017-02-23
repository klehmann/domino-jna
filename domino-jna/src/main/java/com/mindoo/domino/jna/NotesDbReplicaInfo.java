package com.mindoo.domino.jna;

import com.mindoo.domino.jna.structs.NotesDbReplicaInfoStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * This is the structure that identifies a replica database and stores the replication
 * options that affect how the Server's Replicator Task will manipulate the database.<br>
 * <br>
 * Some replication Flags, CutoffInterval, and Cutoff members correspond to the edit
 * controls in the the Workstation's Replication Settings dialog box (in the
 * File, Database, Properties InfoBox).<br>
 * <br>
 * The Replica ID is a {@link NotesTimeDateStruct} structure that contains the time/date
 * of the replica's creation, used to uniquely identify the database replicas
 * to each other.<br>
 * <br>
 * This time/date is NOT normalized to Greenwich Mean Time (GMT), as keeping the local
 * time zone and daylight savings time settings will further ensure that it is a unique time/date.
 */
public class NotesDbReplicaInfo implements IAdaptable {
	private NotesDbReplicaInfoStruct m_struct;
	
	public NotesDbReplicaInfo(NotesDbReplicaInfoStruct struct) {
		m_struct = struct;
	}
	
	public NotesDbReplicaInfo(Pointer p) {
		this(NotesDbReplicaInfoStruct.newInstance(p));
	}
	
	public NotesDbReplicaInfo() {
		this(NotesDbReplicaInfoStruct.newInstance());
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesDbReplicaInfoStruct.class || clazz == Structure.class) {
			return (T) m_struct;
		}
		return null;
	}

	/**
	 * Returns the replication ID which is same for all replica files
	 * 
	 * @return ID
	 */
	public NotesTimeDate getReplicaIDAsDate() {
		return m_struct.ID == null ? null : new NotesTimeDate(m_struct.ID);
	}

	/**
	 * Sets the replication ID which is same for all replica files
	 * 
	 * @param newID new ID
	 */
	public void setReplicaIDAsDate(NotesTimeDate newID) {
		m_struct.ID = NotesTimeDateStruct.newInstance(newID.getInnards());
		m_struct.write();
	}
	
	/**
	 * Returns replication flags
	 * 
	 * @return flags
	 */
	public int getFlags() {
		return (int) (m_struct.Flags & 0xffff);
	}

	/**
	 * Sets replication flags
	 * 
	 * @param newFlags new flags
	 */
	public void setFlags(int newFlags) {
		m_struct.Flags = (short) (newFlags & 0xffff);
		m_struct.write();
	}
	
	/**
	 * Automatic Replication Cutoff Interval (Days)
	 * 
	 * @return interval
	 */
	public int getCutOffInterval() {
		return (int) (m_struct.CutoffInterval & 0xffff);
		
	}

	/**
	 * Sets the Automatic Replication Cutoff Interval (Days)
	 * 
	 * @param interval (WORD)
	 */
	public void setCutOffInterval(int interval) {
		m_struct.CutoffInterval = (short) (interval & 0xffff);
		m_struct.write();
	}
	
	/**
	 * Replication cutoff date
	 * 
	 * @return cutoff date
	 */
	public NotesTimeDate getCutOff() {
		return m_struct.Cutoff==null ? null : new NotesTimeDate(m_struct.Cutoff);
	}
	
	/**
	 * Sets the new cutoff date
	 * 
	 * @param cutOff date
	 */
	public void setCutOff(NotesTimeDate cutOff) {
		m_struct.Cutoff = cutOff==null ? null : NotesTimeDateStruct.newInstance(cutOff.getInnards());
		m_struct.write();
	}
	
	/**
	 * Returns the replica ID as hex encoded string with 16 characters
	 * 
	 * @return replica id
	 */
	public String getReplicaID() {
		return NotesStringUtils.innardsToReplicaId(m_struct.ID.Innards);
	}
	
	/**
	 * Method to set the replica ID as hex encoded string with 16 characters
	 * 
	 * @param replicaId new replica id, either 16 characters of 8:8 format
	 */
	public void setReplicaID(String replicaId) {
		if (replicaId.contains(":"))
			replicaId = replicaId.replace(":", "");
		
		if (replicaId.length() != 16) {
			throw new IllegalArgumentException("Replica ID is expected to have 16 characters");
		}
		
		m_struct.ID.Innards[1] = Integer.parseInt(replicaId.substring(0,8), 16);
		m_struct.ID.Innards[0] = Integer.parseInt(replicaId.substring(8), 16);
		m_struct.write();
	}
}
