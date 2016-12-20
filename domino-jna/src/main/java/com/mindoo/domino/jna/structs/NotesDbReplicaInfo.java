package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

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
 * The Replica ID is a {@link NotesTimeDate} structure that contains the time/date
 * of the replica's creation, used to uniquely identify the database replicas
 * to each other.<br>
 * <br>
 * This time/date is NOT normalized to Greenwich Mean Time (GMT), as keeping the local
 * time zone and daylight savings time settings will further ensure that it is a unique time/date.
 */
public class NotesDbReplicaInfo extends BaseStructure {
	/**
	 * ID that is same for all replica files<br>
	 * C type : TIMEDATE
	 */
	public NotesTimeDate ID;
	/** Replication flags */
	public short Flags;
	/**
	 * Automatic Replication Cutoff<br>
	 * Interval (Days)
	 */
	public short CutoffInterval;
	/**
	 * Replication cutoff date<br>
	 * C type : TIMEDATE
	 */
	public NotesTimeDate Cutoff;
	
	public NotesDbReplicaInfo() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("ID", "Flags", "CutoffInterval", "Cutoff");
	}
	/**
	 * @param ID ID that is same for all replica files<br>
	 * C type : TIMEDATE<br>
	 * @param Flags Replication flags<br>
	 * @param CutoffInterval Automatic Replication Cutoff<br>
	 * Interval (Days)<br>
	 * @param Cutoff Replication cutoff date<br>
	 * C type : TIMEDATE
	 */
	public NotesDbReplicaInfo(NotesTimeDate ID, short Flags, short CutoffInterval, NotesTimeDate Cutoff) {
		super();
		this.ID = ID;
		this.Flags = Flags;
		this.CutoffInterval = CutoffInterval;
		this.Cutoff = Cutoff;
	}
	public NotesDbReplicaInfo(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesDbReplicaInfo implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesDbReplicaInfo implements Structure.ByValue {
		
	};
	
	/**
	 * Returns the replica ID ({@link #ID}) as hex encoded string with 16 characters
	 * 
	 * @return replica id
	 */
	public String getReplicaID() {
		return NotesStringUtils.innardsToReplicaId(this.ID.Innards);
	}
	
	/**
	 * Method to set the replica ID ({@link #ID}) as hex encoded string with 16 characters
	 * 
	 * @param replicaId new replica id, either 16 characters of 8:8 format
	 */
	public void setReplicaID(String replicaId) {
		if (replicaId.contains(":"))
			replicaId = replicaId.replace(":", "");
		
		if (replicaId.length() != 16) {
			throw new IllegalArgumentException("Replica ID is expected to have 16 characters");
		}
		
		this.ID.Innards[1] = Integer.parseInt(replicaId.substring(0,8), 16);
		this.ID.Innards[0] = Integer.parseInt(replicaId.substring(8), 16);
		write();
	}
}
