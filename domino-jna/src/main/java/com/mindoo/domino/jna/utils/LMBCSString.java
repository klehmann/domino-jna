package com.mindoo.domino.jna.utils;

import java.util.Arrays;

/**
 * Utility class that lazily converts a string from LMBCS format to Java String
 * 
 * @author Karsten Lehmann
 */
public class LMBCSString {
	private String m_strValue;
	private byte[] m_data;
	private int m_hashCode;
	
	/**
	 * Creates a new instance
	 * 
	 * @param data data in LMBCS format
	 */
	public LMBCSString(byte[] data) {
		m_data = data;
	}
	
	public byte[] getData() {
		return m_data;
	}
	
	public int hashCode() {
		if (m_hashCode==0) {
			m_hashCode = Arrays.hashCode(m_data);
		}
		return m_hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LMBCSString) {
			boolean equal = Arrays.equals(m_data, ((LMBCSString)obj).m_data);
			return equal;
		}
		return false;
	}
	
	/**
	 * Returns the string value. Converts from LMBCS on the first call.
	 * 
	 * @return value
	 */
	public String getValue() {
		if (m_strValue==null) {
			m_strValue = LMBCSStringConversionCache.get(this);
		}
		return m_strValue;
	}

}
