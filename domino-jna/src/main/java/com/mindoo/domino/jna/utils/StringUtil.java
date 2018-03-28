package com.mindoo.domino.jna.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Collection of string utilities
 * 
 * @author Karsten Lehmann
 */
public class StringUtil {

	/**
	 * Returns the nth value of a string array. The first array element is at position 1.
	 * If the array does not have enough entries, the method returns an empty string.
	 * 
	 * @param strArr array
	 * @param pos position
	 * @return value
	 */
	public static String wWord(String[] strArr, int pos) {
		return getNth(strArr, pos-1);
	}
	
	/**
	 * Returns the nth value of a string array beginning with position 0.
	 * If the array does not have enough entries, the method returns an empty string.
	 * 
	 * @param strArr array
	 * @param index array index
	 * @return value
	 */
	public static String getNth(String[] strArr, int index) {
		if (index<strArr.length)
			return strArr[index];
		else
			return "";
	}
	
	/**
	 * Method to change a value in a string array beginning with position 0. If the
	 * specified array is not big enough, the method creates a new array and transfers
	 * all values first. The newly created array is returned. Otherwise, the method
	 * just sets the new value and returns the modified array
	 * 
	 * @param strArr array
	 * @param index array index
	 * @param newValue new array value
	 * @return modified array
	 */
	public static String[] setNth(String[] strArr, int index, String newValue) {
		if (index<strArr.length) {
			strArr[index]=newValue;
			return strArr;
		}
		else {
			String[] newArr=new String[index+1];
			for (int i=0; i<strArr.length; i++) {
				newArr[i]=strArr[i];
			}
			newArr[index]=newValue;
			return newArr;
		}
	}

	/**
	 * The method checks of the specified string is <code>null</code> or an empty string
	 * 
	 * @param strValue string
	 * @return <code>true</code> if empty
	 */
	public static boolean isEmpty(String strValue) {
		return strValue==null || strValue.length()==0;
	}

	/**
	 * The method checks whether the specified string value is not null and
	 * its size is greater than zero characters
	 * 
	 * @param str string
	 * @return <code>true</code> if not empty
	 */
	public static boolean isNotEmpty(String str) {
		return (str != null) && (str.length() > 0);
	}

	/**
	 * Repeats a string a number of times
	 * 
	 * @param str string
	 * @param repetitions nr of repetitions
	 * @return resulting string
	 */
	public static String repeat(String str, int repetitions) {
		if (repetitions==0)
			return "";
		else if (repetitions==1)
			return str;
		char[] chars=new char[str.length()*repetitions];
		int index=0;
		for (int i=0; i<repetitions; i++) {
			for (int j=0; j<str.length(); j++) {
				chars[index]=str.charAt(j);
				index++;
			}
		}
		return new String(chars);
	}

	/**
	 * Repeats a string a number of times
	 * 
	 * @param c character
	 * @param repetitions nr of repetitions
	 * @return resulting string
	 */
	public static String repeat(Character c, int repetitions) {
		if (repetitions==0)
			return "";
		else if (repetitions==1)
			return c.toString();
		
		char[] chars=new char[repetitions];
		Arrays.fill(chars, c);
		return new String(chars);
	}
	
	/**
	 * Method to add character to a string until it gets the right length
	 * 
	 * @param str string
	 * @param targetLength length for the result string
	 * @param padChar character to add
	 * @param appendChars true to append the character, false to insert them at the beginning of the string
	 * @return processed string
	 */
	public static String pad(String str, int targetLength, char padChar, boolean appendChars) {
		if (str.length()>=targetLength)
			return str;
		StringBuilder sb=new StringBuilder();
		if (appendChars) {
			sb.append(str);
			while (sb.length()<targetLength)
				sb.append(padChar);
			return sb.toString();
		}
		else {
			for (int i=0; i<(targetLength-str.length()); i++) {
				sb.append(padChar);
			}
			sb.append(str);
			return sb.toString();
		}
	}
	
	/**
	 * Same as {@link String#startsWith(String)}, but ignoring case
	 * 
	 * @param p_sStr string value
	 * @param p_sSubStr sub string
	 * @return true, if substring
	 */
	public static boolean startsWithIgnoreCase(String p_sStr, String p_sSubStr) {
		if (p_sSubStr.length()>p_sStr.length())
			return false;
		for (int i=0; i<p_sSubStr.length(); i++) {
			char cSubStr=p_sSubStr.charAt(i);
			char cStr=p_sStr.charAt(i);
			char cSubStrLC=Character.toLowerCase(cSubStr);
			char cStrLC=Character.toLowerCase(cStr);
			
			if (cSubStrLC != cStrLC)
				return false;
		}
		return true;
	}
	
	/**
	 * Same as {@link String#endsWith(String)}, but ignoring case
	 * 
	 * @param p_sStr string value
	 * @param p_sSubStr sub string
	 * @return true, if substring
	 */
	public static boolean endsWithIgnoreCase(String p_sStr, String p_sSubStr) {
		if (p_sSubStr.length()>p_sStr.length())
			return false;
		int nSubStrLen=p_sSubStr.length();
		int nStrLen=p_sStr.length();
		int nIdx=1;
		for (int i=nSubStrLen-1; i>=0; i--) {
			char cSubStr=p_sSubStr.charAt(i);
			char cStr=p_sStr.charAt(nStrLen-nIdx);
			char cSubStrLC=Character.toLowerCase(cSubStr);
			char cStrLC=Character.toLowerCase(cStr);
			
			if (cSubStrLC != cStrLC)
				return false;
			
			nIdx++;
		}
		
		return true;
	}

	/**
	 * The method concatenates a list of strings with the given delimiter
	 * 
	 * @param p_sStrList string list
	 * @param p_sDelimiter delimiter
	 * @return concatenated strings
	 */
	public static String join(List<String> p_sStrList, String p_sDelimiter) {
		StringBuilder sb=new StringBuilder();
		for (int i=0; i<p_sStrList.size(); i++) {
			String sCurrStr = p_sStrList.get(i);
			if (i>0)
				sb.append(p_sDelimiter);
			sb.append(sCurrStr);
		}
		return sb.toString();
	}

	/**
	 * Computes the number of bytes a string would allocate if converted to UTF-8
	 * 
	 * @param str string
	 * @return number of bytes
	 */
	public static int stringLengthInUTF8(String str) {
		int retLength = 0;
		int strLen = str.length();

		for (int i = 0; i < strLen; i++) {
			char c = str.charAt(i);
			
			if (c <= 0x7F) {
				retLength++;
			} else if (c <= 0x7FF) {
				retLength += 2;
			} else if (Character.isHighSurrogate(c)) {
				retLength += 4;
				i++;
			} else {
				retLength += 3;
			}
		}
		return retLength;
	}
}
