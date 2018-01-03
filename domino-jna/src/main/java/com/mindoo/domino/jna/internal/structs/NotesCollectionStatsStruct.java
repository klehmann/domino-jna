package com.mindoo.domino.jna.internal.structs;

import java.security.AccessController;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * If requested, this structure is returned by {@link NotesCollection#readEntries(com.mindoo.domino.jna.NotesCollectionPosition, java.util.EnumSet, int, java.util.EnumSet, int, java.util.EnumSet)}
 * at the front of the returned information buffer.<br>
 * The structure describes statistics about the overall collection.
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionStatsStruct extends BaseStructure {
	/** # top level entries (level 0) */
	public int TopLevelEntries;
	/** 0 */
	public int LastModifiedTime;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesCollectionStatsStruct() {
		super();
	}
	
	public static NotesCollectionStatsStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCollectionStatsStruct>() {

			@Override
			public NotesCollectionStatsStruct run() {
				return new NotesCollectionStatsStruct();
			}});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("TopLevelEntries", "LastModifiedTime");
	}
	
	/**
	 * @param TopLevelEntries # top level entries (level 0)<br>
	 * @param LastModifiedTime 0
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesCollectionStatsStruct(int TopLevelEntries, int LastModifiedTime) {
		super();
		this.TopLevelEntries = TopLevelEntries;
		this.LastModifiedTime = LastModifiedTime;
	}
	
	public static NotesCollectionStatsStruct newInstance(final int TopLevelEntries, final int LastModifiedTime) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCollectionStatsStruct>() {

			@Override
			public NotesCollectionStatsStruct run() {
				return new NotesCollectionStatsStruct(TopLevelEntries, LastModifiedTime);
			}});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesCollectionStatsStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesCollectionStatsStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCollectionStatsStruct>() {

			@Override
			public NotesCollectionStatsStruct run() {
				return new NotesCollectionStatsStruct(peer);
			}});
	}
	
	public static class ByReference extends NotesCollectionStatsStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesCollectionStatsStruct implements Structure.ByValue {
		
	};
}
