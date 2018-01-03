package com.mindoo.domino.jna.internal.structs;
import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
/**
 * JNA class for the FT_INDEX_STATS type
 * 
 * @author Karsten Lehmann
 */
public class NotesFTIndexStatsStruct extends BaseStructure {
	/** # of new documents */
	public int DocsAdded;
	/** # of revised documents */
	public int DocsUpdated;
	/** # of deleted documents */
	public int DocsDeleted;
	/** # of bytes indexed */
	public int BytesIndexed;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesFTIndexStatsStruct() {
		super();
	}
	
	public static NotesFTIndexStatsStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesFTIndexStatsStruct>() {

			@Override
			public NotesFTIndexStatsStruct run() {
				return new NotesFTIndexStatsStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("DocsAdded", "DocsUpdated", "DocsDeleted", "BytesIndexed");
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param DocsAdded # of new documents<br>
	 * @param DocsUpdated # of revised documents<br>
	 * @param DocsDeleted # of deleted documents<br>
	 * @param BytesIndexed # of bytes indexed
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesFTIndexStatsStruct(int DocsAdded, int DocsUpdated, int DocsDeleted, int BytesIndexed) {
		super();
		this.DocsAdded = DocsAdded;
		this.DocsUpdated = DocsUpdated;
		this.DocsDeleted = DocsDeleted;
		this.BytesIndexed = BytesIndexed;
	}
	
	public static NotesFTIndexStatsStruct newInstance(final int docsAdded, final int docsUpdated, final int docsDeleted, final int bytesIndexed) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesFTIndexStatsStruct>() {

			@Override
			public NotesFTIndexStatsStruct run() {
				return new NotesFTIndexStatsStruct(docsAdded, docsUpdated, docsDeleted, bytesIndexed);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * @param peer pointer
	 */
	public NotesFTIndexStatsStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesFTIndexStatsStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesFTIndexStatsStruct>() {

			@Override
			public NotesFTIndexStatsStruct run() {
				return new NotesFTIndexStatsStruct(peer);
			}
		});
	}
	
	public static class ByReference extends NotesFTIndexStatsStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesFTIndexStatsStruct implements Structure.ByValue {
		
	};
}
