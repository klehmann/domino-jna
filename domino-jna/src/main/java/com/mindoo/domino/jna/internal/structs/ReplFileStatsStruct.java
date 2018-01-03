package com.mindoo.domino.jna.internal.structs;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
/**
 * This structure is returned by ReplicateWithServer() and ReplicateWithServerExt().<br>
 * It contains the resulting replication statistics information.
 */
public class ReplFileStatsStruct extends BaseStructure {
	public NativeLong TotalFiles;
	public NativeLong FilesCompleted;
	public NativeLong NotesAdded;
	public NativeLong NotesDeleted;
	public NativeLong NotesUpdated;
	public NativeLong Successful;
	public NativeLong Failed;
	public NativeLong NumberErrors;
	
	public ReplFileStatsStruct() {
		super();
	}
	
	public static ReplFileStatsStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<ReplFileStatsStruct>() {

			@Override
			public ReplFileStatsStruct run() {
				return new ReplFileStatsStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("TotalFiles", "FilesCompleted", "NotesAdded", "NotesDeleted", "NotesUpdated", "Successful", "Failed", "NumberErrors");
	}
	
	public ReplFileStatsStruct(NativeLong TotalFiles, NativeLong FilesCompleted, NativeLong NotesAdded, NativeLong NotesDeleted, NativeLong NotesUpdated, NativeLong Successful, NativeLong Failed, NativeLong NumberErrors) {
		super();
		this.TotalFiles = TotalFiles;
		this.FilesCompleted = FilesCompleted;
		this.NotesAdded = NotesAdded;
		this.NotesDeleted = NotesDeleted;
		this.NotesUpdated = NotesUpdated;
		this.Successful = Successful;
		this.Failed = Failed;
		this.NumberErrors = NumberErrors;
	}

	public static ReplFileStatsStruct newInstance(final NativeLong TotalFiles, final NativeLong FilesCompleted, final NativeLong NotesAdded, final NativeLong NotesDeleted, final NativeLong NotesUpdated, final NativeLong Successful, final NativeLong Failed, final NativeLong NumberErrors) {
		return AccessController.doPrivileged(new PrivilegedAction<ReplFileStatsStruct>() {

			@Override
			public ReplFileStatsStruct run() {
				return new ReplFileStatsStruct(TotalFiles, FilesCompleted, NotesAdded, NotesDeleted, NotesUpdated, Successful, Failed, NumberErrors);
			}
		});
	}

	public ReplFileStatsStruct(Pointer peer) {
		super(peer);
	}
	
	public static ReplFileStatsStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<ReplFileStatsStruct>() {

			@Override
			public ReplFileStatsStruct run() {
				return new ReplFileStatsStruct(peer);
			}
		});
	}
	
	public static class ByReference extends ReplFileStatsStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends ReplFileStatsStruct implements Structure.ByValue {
		
	};
}
