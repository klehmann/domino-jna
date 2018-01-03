package com.mindoo.domino.jna.internal.structs;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

/**
 * JNA class for the COLLECTIONDATA type
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionDataStruct extends BaseStructure {

	/** Total number of documents in the collection */
	public int docCount;
	/** Total number of bytes occupied by the document entries in the collection */
	public int docTotalSize;
	/** Number of B-Tree leaf nodes for this index. */
	public int btreeLeafNodes;
	/** Number of B-tree levels for this index. */
	public short btreeDepth;
	/** Unused */
	public short spare;

	/**
	 * Offset of ITEM_VALUE_TABLE<br>
	 * for each 10th-percentile key value.<br>
	 * A series of ITEM_VALUE_TABLEs follows<br>
	 * this structure.<br>
	 * C type : DWORD[PERCENTILE_COUNT]
	 */
	public int[] keyOffset = new int[NotesConstants.PERCENTILE_COUNT];

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesCollectionDataStruct() {
		super();
	}

	public static NotesCollectionDataStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCollectionDataStruct>() {

			@Override
			public NotesCollectionDataStruct run() {
				return new NotesCollectionDataStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("docCount", "docTotalSize", "btreeLeafNodes", "btreeDepth", "spare", "keyOffset");
	}

	/**
	 * @param DocCount Total number of documents in the<br>
	 * collection<br>
	 * @param DocTotalSize Total number of bytes occupied by the<br>
	 * document entries in the collection<br>
	 * @param BTreeLeafNodes Number of B-Tree leaf nodes for this<br>
	 * index.<br>
	 * @param BTreeDepth Number of B-tree levels for this index.<br>
	 * @param Spare Unused<br>
	 * @param KeyOffset Offset of ITEM_VALUE_TABLE<br>
	 * for each 10th-percentile key value.<br>
	 * A series of ITEM_VALUE_TABLEs follows<br>
	 * this structure.<br>
	 * C type : DWORD[PERCENTILE_COUNT]
	 */
	public NotesCollectionDataStruct(int DocCount, int DocTotalSize, int BTreeLeafNodes, short BTreeDepth, short Spare, int[] KeyOffset) {
		super();
		this.docCount = DocCount;
		this.docTotalSize = DocTotalSize;
		this.btreeLeafNodes = BTreeLeafNodes;
		this.btreeDepth = BTreeDepth;
		this.spare = Spare;
		if ((KeyOffset.length != this.keyOffset.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.keyOffset = KeyOffset;
	}

	public static NotesCollectionDataStruct newInstance(final int docCount, final int docTotalSize, final int btreeLeafNodes,
			final short btreeDepth, final short spare, final int[] keyOffset) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCollectionDataStruct>() {

			@Override
			public NotesCollectionDataStruct run() {
				return new NotesCollectionDataStruct(docCount, docTotalSize, btreeLeafNodes, btreeDepth, spare, keyOffset);
			}
		});
	}

	public NotesCollectionDataStruct(Pointer peer) {
		super(peer);
	}

	public static NotesCollectionDataStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesCollectionDataStruct>() {

			@Override
			public NotesCollectionDataStruct run() {
				return new NotesCollectionDataStruct(p);
			}
		});
	}

	public static class ByReference extends NotesCollectionDataStruct implements Structure.ByReference {

	};

	public static class ByValue extends NotesCollectionDataStruct implements Structure.ByValue {

	};
}
