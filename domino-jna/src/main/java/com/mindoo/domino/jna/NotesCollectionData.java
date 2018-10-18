package com.mindoo.domino.jna;

public class NotesCollectionData {
	/**
	 * Keys in a COLLECTIONDATA structure are divided into percentiles - divisions corresponding
	 * to one-tenth of the total range of keys - and a table of the keys marking the divisions
	 * is returned with that structure.<br>
	 * This constant gives the number of entries in that table.
	 */
	public static int PERCENTILE_COUNT = 11;
	/** Table entry for first key in collection */
	public static int PERCENTILE_0 = 0;
	/**  Table entry for first 1/10 of collection */
	public static int PERCENTILE_10 = 1;
	/** Table entry for second 1/10 of collection */
	public static int PERCENTILE_20 = 2;
	/**  Table entry for third 1/10 of collection */
	public static int PERCENTILE_30 = 3;
	/** Table entry for fourth 1/10 of collection */
	public static int PERCENTILE_40 = 4;
	/** Table entry for the first half of a collection */
	public static int PERCENTILE_50 = 5;
	/** Table entry for sixth 1/10 of collection */
	public static int PERCENTILE_60 = 6;
	/** Table entry for seventh 1/10 of collection */
	public static int PERCENTILE_70 = 7;
	/** Table entry for eighth 1/10 of collection */
	public static int PERCENTILE_80 = 8;
	/** Table entry for ninth 1/10 of collection 1/10 of collection */
	public static int PERCENTILE_90 = 9;
	/** Table entry for last key in collection */
	public static int PERCENTILE_100 = 10;
	
	/** Total number of documents in the collection */
	public int m_docCount;
	/** Total number of bytes occupied by the document entries in the collection */
	public int m_docTotalSize;
	/** Number of B-Tree leaf nodes for this index. */
	public int m_btreeLeafNodes;
	/** Number of B-tree levels for this index. */
	public short m_btreeDepth;
	private IItemValueTableData[] m_itemValueTables;
	
	NotesCollectionData(int docCount, int docTotalSize, int btreeLeafNodes, short btreeDepth, IItemValueTableData[] itemValueTables) {
		m_docCount = docCount;
		m_docTotalSize = docTotalSize;
		m_btreeLeafNodes = btreeLeafNodes;
		m_btreeDepth = btreeDepth;
		m_itemValueTables = itemValueTables;
	}
	
	/**
	 * Returns the total number of documents in the collection
	 * 
	 * @return count
	 */
	public int getDocCount() {
		return m_docCount;
	}
	
	/**
	 * Returns the total number of bytes occupied by the document entries in the collection
	 * 
	 * @return size
	 */
	public int getDocTotalSize() {
		return m_docTotalSize;
	}
	
	/**
	 * Returns the number of B-Tree leaf nodes for this index.
	 * 
	 * @return nodes
	 */
	public int getBTreeLeafNodes() {
		return m_btreeLeafNodes;
	}
	
	/**
	 * Returns the number of B-tree levels for this index.
	 * 
	 * @return levels
	 */
	public int getBTreeDepth() {
		return m_btreeLeafNodes;
	}
	
	/**
	 * The key values used to index this collection are sorted into percentiles.<br>
	 * The first key and every key that corresponds to one-tenth of the collection are returned
	 * by this method in a table.<br>
	 * <br>
	 * The {@link #PERCENTILE_0} entry in the KeyOffset table is the first value in the index,
	 * and the {@link #PERCENTILE_100} entry is the last value in the index.

	 * @param index index between 0 and {@link #PERCENTILE_COUNT}
	 * @return item value table
	 */
	public IItemValueTableData getItemValueTable(int index) {
		return m_itemValueTables[index];
	}
}
