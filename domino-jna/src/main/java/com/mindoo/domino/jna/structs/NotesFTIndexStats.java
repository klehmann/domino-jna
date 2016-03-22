package com.mindoo.domino.jna.structs;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * JNA class for the FT_INDEX_STATS type
 * 
 * @author Karsten Lehmann
 */
public class NotesFTIndexStats extends Structure {
	/** # of new documents */
	public int DocsAdded;
	/** # of revised documents */
	public int DocsUpdated;
	/** # of deleted documents */
	public int DocsDeleted;
	/** # of bytes indexed */
	public int BytesIndexed;
	public NotesFTIndexStats() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("DocsAdded", "DocsUpdated", "DocsDeleted", "BytesIndexed");
	}
	/**
	 * @param DocsAdded # of new documents<br>
	 * @param DocsUpdated # of revised documents<br>
	 * @param DocsDeleted # of deleted documents<br>
	 * @param BytesIndexed # of bytes indexed
	 */
	public NotesFTIndexStats(int DocsAdded, int DocsUpdated, int DocsDeleted, int BytesIndexed) {
		super();
		this.DocsAdded = DocsAdded;
		this.DocsUpdated = DocsUpdated;
		this.DocsDeleted = DocsDeleted;
		this.BytesIndexed = BytesIndexed;
	}
	public NotesFTIndexStats(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesFTIndexStats implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesFTIndexStats implements Structure.ByValue {
		
	};
}
