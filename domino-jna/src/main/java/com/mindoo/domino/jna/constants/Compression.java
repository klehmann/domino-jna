package com.mindoo.domino.jna.constants;

/**
 * Specifies compression algorithm used when attaching file to a note.
 * 
 * @author Karsten Lehmann
 */
public enum Compression {
	
	/** no compression */
	NONE(0),
	/** huffman encoding for compression  */
	HUFF(1),
	/** LZ1 compression */
	LZ1(2),
	/** Huffman compression even if server supports LZ1 */
	RECOMPRESS_HUFF(3);

	private int m_val;

	Compression(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}

}
