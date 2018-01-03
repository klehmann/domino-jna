package com.mindoo.domino.jna;

import com.mindoo.domino.jna.internal.structs.ReplServStatsStruct;
import com.sun.jna.Pointer;

/**
 * This structure is returned from {@link NotesDatabase#replicateWithServer(String, java.util.EnumSet, int)} and
 * {@link NotesDatabase#replicateDbsWithServer(String, java.util.EnumSet, java.util.List, int)}.<br>
 * <br>
 * It contains the returned replication statistics.
 * 
 * @author Karsten Lehmann
 */
public class NotesReplicationStats {
	private ReplServStatsStruct m_struct;
	
	public NotesReplicationStats(IAdaptable adaptable) {
		ReplServStatsStruct struct = adaptable.getAdapter(ReplServStatsStruct.class);
		if (struct!=null) {
			m_struct = struct;
			return;
		}
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			m_struct = ReplServStatsStruct.newInstance(p);
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	}
	
	/* general stats */
	
	public long getStubsInitialized() {
		return m_struct.StubsInitialized.longValue();
	}
	
	public long getTotalUnreadExchanges() {
		return m_struct.TotalUnreadExchanges.longValue();
	}
	
	public long getNumberErrors() {
		return m_struct.NumberErrors.longValue();
	}

	/* Pull stats */

	public long getPullTotalFiles() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.TotalFiles.longValue();
		}
		return 0;
	}
	
	public long getPullFilesCompleted() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.FilesCompleted.longValue();
		}
		return 0;
	}
	
	public long getPullNotesAdded() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.NotesAdded.longValue();
		}
		return 0;
	}
	
	public long getPullNotesDeleted() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.NotesDeleted.longValue();
		}
		return 0;
	}
	
	public long getPullSuccessful() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.Successful.longValue();
		}
		return 0;
	}
	
	public long getPullFailed() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.Failed.longValue();
		}
		return 0;
	}
	
	public long getPullNumberErrors() {
		if (m_struct.Pull!=null) {
			return m_struct.Pull.NumberErrors.longValue();
		}
		return 0;
	}
	
	/* Push stats */
	
	public long getPushTotalFiles() {
		if (m_struct.Push!=null) {
			return m_struct.Push.TotalFiles.longValue();
		}
		return 0;
	}
	
	public long getPushFilesCompleted() {
		if (m_struct.Push!=null) {
			return m_struct.Push.FilesCompleted.longValue();
		}
		return 0;
	}
	
	public long getPushNotesAdded() {
		if (m_struct.Push!=null) {
			return m_struct.Push.NotesAdded.longValue();
		}
		return 0;
	}
	
	public long getPushNotesDeleted() {
		if (m_struct.Push!=null) {
			return m_struct.Push.NotesDeleted.longValue();
		}
		return 0;
	}
	
	public long getPushSuccessful() {
		if (m_struct.Push!=null) {
			return m_struct.Push.Successful.longValue();
		}
		return 0;
	}
	
	public long getPushFailed() {
		if (m_struct.Push!=null) {
			return m_struct.Push.Failed.longValue();
		}
		return 0;
	}
	
	public long getPushNumberErrors() {
		if (m_struct.Push!=null) {
			return m_struct.Push.NumberErrors.longValue();
		}
		return 0;
	}
}
