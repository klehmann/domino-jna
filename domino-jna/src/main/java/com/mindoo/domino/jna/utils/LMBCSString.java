package com.mindoo.domino.jna.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.mindoo.domino.jna.internal.LMBCSStringConversionCache;

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
		
		//handle simple case of a pure ASCII string to have more readible string values in the Java debugger
		boolean isPureAscii = true;
		for (int i=0; i<m_data.length; i++) {
			byte b = m_data[i];
			if (b <= 0x1f || b >= 0x80) {
				isPureAscii = false;
				break;
			}
		}

		if (isPureAscii) {
			m_strValue = new String(m_data, StandardCharsets.US_ASCII);
		}
	}
	
	/**
	 * Returns the size of the internal data in bytes
	 * 
	 * @return size
	 */
	public int size() {
		return m_data.length;
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
			LMBCSString otherLMBCSStr = (LMBCSString) obj;
			
			if (m_hashCode!=0 && otherLMBCSStr.m_hashCode!=0 && m_hashCode!=otherLMBCSStr.m_hashCode) {
				return false;
			}
			
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

	@Override
	public String toString() {
		String strValueIfCached = m_strValue!=null ? m_strValue : LMBCSStringConversionCache.get(this, true);
		
		if (strValueIfCached!=null) {
			return "LMBCSString [decoded value=" + m_strValue + "]";
		}
		else {
			return "LMBCSString [not decoded yet]";
		}
	}
	
}
