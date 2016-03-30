package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
/**
 * JNA class for the COLLECTIONSTATS type
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionStats extends Structure {
	/** # top level entries (level 0) */
	public int TopLevelEntries;
	/** 0 */
	public int LastModifiedTime;
	public NotesCollectionStats() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("TopLevelEntries", "LastModifiedTime");
	}
	/**
	 * @param TopLevelEntries # top level entries (level 0)<br>
	 * @param LastModifiedTime 0
	 */
	public NotesCollectionStats(int TopLevelEntries, int LastModifiedTime) {
		super();
		this.TopLevelEntries = TopLevelEntries;
		this.LastModifiedTime = LastModifiedTime;
	}
	public NotesCollectionStats(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesCollectionStats implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesCollectionStats implements Structure.ByValue {
		
	};
}
