package com.mindoo.domino.jna.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.INotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.ReadOnlyMemory;
import com.mindoo.domino.jna.internal.SizeLimitedLRUCache;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * String conversion functions between Java and LMBCS
 * 
 * @author Karsten Lehmann
 */
public class NotesStringUtils {
	private static final String PREF_USEOSLINEBREAK = "NotesStringUtils.useOSLineDelimiter";
	
	//use simple cache for string-lmbcs conversion of short string
	private static final boolean USE_STRING2LMBCS_CACHE = true;
	//max length of each string-lmbcs cache entry in characters
	private static final int MAX_STRING2LMBCS_KEY_LENGTH = 500;
	
	private static final int MAX_STRING2LMBCS_SIZE_BYTES = 1000000;
	
	private static LRUStringLMBCSCache m_string2LMBCSCache_NullTerminated_LinefeedLinebreaks = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	private static LRUStringLMBCSCache m_string2LMBCSCache_NotNullTerminated_LinefeedLinebreaks = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	
	private static LRUStringLMBCSCache m_string2LMBCSCache_NullTerminated_NullLinebreaks = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	private static LRUStringLMBCSCache m_string2LMBCSCache_NotNullTerminated_NullLinebreaks = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);

	private static LRUStringLMBCSCache m_string2LMBCSCache_NullTerminated_OriginalLinebreaks = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	private static LRUStringLMBCSCache m_string2LMBCSCache_NotNullTerminated_OriginalLinebreaks = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	
	private static final Charset charsetUTF8 = Charset.forName("UTF-8");

	public static void flushCache() {
		m_string2LMBCSCache_NullTerminated_LinefeedLinebreaks.clear();
		m_string2LMBCSCache_NotNullTerminated_LinefeedLinebreaks.clear();
		m_string2LMBCSCache_NullTerminated_NullLinebreaks.clear();
		m_string2LMBCSCache_NotNullTerminated_NullLinebreaks.clear();
		m_string2LMBCSCache_NullTerminated_OriginalLinebreaks.clear();
		m_string2LMBCSCache_NotNullTerminated_OriginalLinebreaks.clear();
	}
	
	/**
	 * Method to control the LMBCS / Java String conversion for newline characters. By default
	 * we insert \r\n on Windows and \n on other platforms like IBM does.<br>
	 * This setting is only valid for the current {@link NotesGC#runWithAutoGC(java.util.concurrent.Callable)}
	 * call.
	 * 
	 * @param b true to use \r\n as newline on Windows, false to use \n everywhere
	 */
	public static void setUseOSLineDelimiter(boolean b) {
		if (isUseOSLineDelimiter() != b) {
			NotesGC.setCustomValue(PREF_USEOSLINEBREAK, Boolean.valueOf(b));
			
			//remove all cached values that contain newlines
			List<String> keysWithNull = m_string2LMBCSCache_NullTerminated_LinefeedLinebreaks.getKeys();
			for (String currKey : keysWithNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCache_NullTerminated_LinefeedLinebreaks.remove(currKey);
				}
			}
			keysWithNull = m_string2LMBCSCache_NullTerminated_NullLinebreaks.getKeys();
			for (String currKey : keysWithNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCache_NullTerminated_NullLinebreaks.remove(currKey);
				}
			}
			keysWithNull = m_string2LMBCSCache_NullTerminated_OriginalLinebreaks.getKeys();
			for (String currKey : keysWithNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCache_NullTerminated_OriginalLinebreaks.remove(currKey);
				}
			}
			
			
			List<String> keysWithoutNull = m_string2LMBCSCache_NotNullTerminated_LinefeedLinebreaks.getKeys();
			for (String currKey : keysWithoutNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCache_NotNullTerminated_LinefeedLinebreaks.remove(currKey);
				}
			}
			keysWithoutNull = m_string2LMBCSCache_NotNullTerminated_NullLinebreaks.getKeys();
			for (String currKey : keysWithoutNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCache_NotNullTerminated_NullLinebreaks.remove(currKey);
				}
			}
			keysWithoutNull = m_string2LMBCSCache_NotNullTerminated_OriginalLinebreaks.getKeys();
			for (String currKey : keysWithoutNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCache_NotNullTerminated_OriginalLinebreaks.remove(currKey);
				}
			}
		}
	}
	
	/**
	 * Returns whether an OS specific newline is used when converting between LMBCS and Java String.
	 * By default we insert \r\n on Windows and \n on other platforms like IBM does.
	 * 
	 * @return true to use \r\n as newline on Windows, false to use \n everywhere
	 */
	public static boolean isUseOSLineDelimiter() {
		Boolean b = (Boolean) NotesGC.getCustomValue(PREF_USEOSLINEBREAK);
		if (b==null)
			return Boolean.TRUE;
		else
			return b.booleanValue();
	}
	
	/**
	 * Scans the byte array for null values
	 * 
	 * @param bytes array of bytes
	 * @return number of bytes before null byte in array
	 */
	public static int getNullTerminatedLength(byte[] bytes) {
		if (bytes == null) {
			return 0;
		}
		
		int textLen = bytes.length;
		
		//search for terminating null character
		for (int i=0; i<textLen; i++) {
			byte b = bytes[i];
			if (b==0) {
				textLen = i;
				break;
			}
		}

		return textLen;
	}
	
	/**
	 * Scans the Memory object for null values
	 * 
	 * @param in memory
	 * @return number of bytes before null byte in memory
	 */
	public static int getNullTerminatedLength(Memory in) {
		if (in == null) {
			return 0;
		}
		
		int textLen = (int) in.size();
		
		//search for terminating null character
		for (int i=0; i<textLen; i++) {
			byte b = in.getByte(i);
			if (b==0) {
				textLen = i;
				break;
			}
		}

		return textLen;
	}
	
	/**
	 * Scans the Pointer object for the first null value
	 * 
	 * @param in pointer
	 * @return number of bytes before null byte found
	 */
	public static int getNullTerminatedLength(Pointer in) {
		if(in == null) {
			return 0;
		}
		
		// Search for terminating null character
		int offset = 0;
		while(true) {
			byte b = in.getByte(offset);
			if(b == 0) {
				return offset;
			} else {
				offset++;
			}
		}
	}
	
	/**
	 * Reads a list of null terminated strings in LMBCS format at the specified pointer
	 * 
	 * @param inPtr pointer
	 * @param numEntries number of null terminated strings
	 * @return string list
	 */
	public static List<String> fromLMBCSStringList(Pointer inPtr, int numEntries) {
		List<String> stringList = new ArrayList<String>();
		
		Pointer ptrStartOfString = inPtr;
		
		for (int i=0; i<numEntries; i++) {
			int currStringOffset = 0;
			
			while(true) {
				byte b = ptrStartOfString.getByte(currStringOffset);
				currStringOffset++;
				if (b==0) {
					break;
				}
			}
			
			String currString = fromLMBCS(ptrStartOfString, currStringOffset-1);
			stringList.add(currString);
			
			if ((i+1)<numEntries) {
				ptrStartOfString = ptrStartOfString.share(currStringOffset);
			}
			else {
				break;
			}
		}
		return stringList;
	}

	/**
	 * Converts an LMBCS string to a Java String
	 * 
	 * @param data data array
	 * @return decoded String
	 */
	public static String fromLMBCS(byte[] data) {
		if (data==null || data.length==0)
			return "";
		
		int startOffset = 0;
		
		List<String> lines = new ArrayList<String>();
		
		INotesNativeAPI api = NotesNativeAPI.get();
		
		//output buffer shared across loop runs for each line
		DisposableMemory outBufUTF8 = null;
		try {
			for (int i=0; i<data.length; i++) {
				if (data[i] == 0) { // code for line break
					int lengthOfLineDataToConvert = i-startOffset;
					
					if (lengthOfLineDataToConvert==0) {
						lines.add("");
						startOffset = i+1;
						
						if (i==(data.length-1)) {
							lines.add("");
						}
						
						continue;
					}
					
					int worstCaseLengthOfConvertedData = 3*lengthOfLineDataToConvert;
					
					DisposableMemory inDataMem = new DisposableMemory(lengthOfLineDataToConvert);
					try {
						inDataMem.write(0, data, startOffset, lengthOfLineDataToConvert);

						do {
							if (outBufUTF8!=null && outBufUTF8.size() < (worstCaseLengthOfConvertedData)) {
								outBufUTF8 = new DisposableMemory(worstCaseLengthOfConvertedData);
							}
							
							if (outBufUTF8==null) {
								outBufUTF8 = new DisposableMemory(worstCaseLengthOfConvertedData);
							}
							
							int retOutBufLength =
									api.OSTranslate32(NotesConstants.OS_TRANSLATE_LMBCS_TO_UTF8,
											inDataMem, lengthOfLineDataToConvert,
											outBufUTF8, (int) outBufUTF8.size());
							
							if (retOutBufLength==outBufUTF8.size()) {
								// output buffer not large enough, increase it and retry (not expected to happen because of
								// our worst case computation)
								long oldOutBufSize = outBufUTF8.size();
								long newOutBufSize = (long) (((double) oldOutBufSize)*2);
								outBufUTF8.dispose();
								outBufUTF8 = new DisposableMemory(newOutBufSize);
								
								continue;
							}
							else if (retOutBufLength==0) {
								lines.add("");
								startOffset = i+1;
								
								break;
							}
							else {
								//success
								String lineAsStr = new String(outBufUTF8.getByteArray(0, retOutBufLength), 0, retOutBufLength, charsetUTF8);
								lines.add(lineAsStr);
								startOffset = i+1;
								
								if (i==(data.length-1)) {
									lines.add("");
								}
								
								break;
							}
						}
						while (true);
					}
					finally {
						inDataMem.dispose();
					}
				}
			}

			if (startOffset<data.length) {
				//convert remaining data
				int lengthOfLineDataToConvert = data.length-startOffset;
				int worstCaseLengthOfConvertedData = 3*lengthOfLineDataToConvert;

				DisposableMemory inDataMem = new DisposableMemory(lengthOfLineDataToConvert);
				try {
					inDataMem.write(0, data, startOffset, lengthOfLineDataToConvert);

					do {
						if (outBufUTF8!=null && outBufUTF8.size() < (worstCaseLengthOfConvertedData)) {
							outBufUTF8 = new DisposableMemory(worstCaseLengthOfConvertedData);
						}
						
						if (outBufUTF8==null) {
							outBufUTF8 = new DisposableMemory(worstCaseLengthOfConvertedData);
						}
						
						int retOutBufLength =
								api.OSTranslate32(NotesConstants.OS_TRANSLATE_LMBCS_TO_UTF8,
										inDataMem, lengthOfLineDataToConvert,
										outBufUTF8, (int) outBufUTF8.size());
						
						if (retOutBufLength==outBufUTF8.size()) {
							// output buffer not large enough, increase it and retry (not expected to happen because of
							// our worst case computation)
							long oldOutBufSize = outBufUTF8.size();
							long newOutBufSize = (long) (((double) oldOutBufSize)*2);
							outBufUTF8.dispose();
							outBufUTF8 = new DisposableMemory(newOutBufSize);
							
							continue;
						}
						else if (retOutBufLength==0) {
							lines.add("");
							
							break;
						}
						else {
							//success
							String lineAsStr = new String(outBufUTF8.getByteArray(0, retOutBufLength), 0, retOutBufLength, charsetUTF8);
							lines.add(lineAsStr);
							
							break;
						}
					}
					while (true);
				}
				finally {
					inDataMem.dispose();
				}
			}
		}
		finally {
			if (outBufUTF8!=null) {
				outBufUTF8.dispose();
			}
		}
		boolean useOSLineBreak = isUseOSLineDelimiter();
		if (PlatformUtils.isWindows() && useOSLineBreak) {
			return StringUtil.join(lines, "\r\n");
		}
		else {
			return StringUtil.join(lines, "\n");
		}
	}
	
	/**
	 * Converts an LMBCS string to a Java String
	 * 
	 * @param inPtr pointer in memory
	 * @param textLen length of text, use -1 to let the method search for a terminating \0
	 * @return decoded String
	 */
	public static String fromLMBCS(Pointer inPtr, int textLen) {
		if (inPtr==null || textLen==0) {
			return "";
		}
		else if (textLen==-1) {
			textLen = getNullTerminatedLength(inPtr);
			
			byte[] dataArr = inPtr.getByteArray(0, textLen);
			
			return fromLMBCS(dataArr);
		}
		else {
			//check for \0 as newline delimiter
			byte[] dataArr = inPtr.getByteArray(0, textLen);
			
			return fromLMBCS(dataArr);
		}
	}
	
	/**
	 * Converts a string to LMBCS format
	 * 
	 * @param inStr string
	 * @param addNull tre to terminate the string with a null byte
	 * @return encoded string in memory, might be a shared copy if the string could be find in the cache
	 */
	public static Memory toLMBCS(String inStr, boolean addNull) {
		return toLMBCS(inStr, addNull, true);
	}
	
	/**
	 * Converts a string to LMBCS format
	 * 
	 * @param inStr string
	 * @param addNull tre to terminate the string with a null byte
	 * @param replaceLineBreaks true to replace linebreaks with null bytes
	 * @return encoded string in memory, might be a shared copy if the string could be find in the cache
	 */
	public static Memory toLMBCS(String inStr, boolean addNull, boolean replaceLineBreaks) {
		return toLMBCS(inStr, addNull, replaceLineBreaks ? LineBreakConversion.NULL : LineBreakConversion.LINEFEED, false);
	}

	public static enum LineBreakConversion {
		/** keep original line break character */
		ORIGINAL,
		/** replace all line breaks with \0 */
		NULL,
		/** replace all line breaks with \n */
		LINEFEED}
	
	/**
	 * Converts a string to LMBCS format. Does not internally cache the computation result
	 * because it is unlikely that the same data will be converted again. Call
	 * {@link DisposableMemory#dispose()} on the returned object if possible to quickly
	 * free up memory.
	 * 
	 * @param inStr string
	 * @param addNull tre to terminate the string with a null byte
	 * @param lineBreakConversion how to convert linebreaks in the string
	 * @return encoded string in memory, might be a shared copy if the string could be find in the cache
	 */
	public static DisposableMemory toLMBCSNoCache(String inStr, boolean addNull, LineBreakConversion lineBreakConversion) {
		return (DisposableMemory) toLMBCS(inStr, addNull, lineBreakConversion, true);
	}
	
	/**
	 * Converts a string to LMBCS format
	 * 
	 * @param inStr string
	 * @param addNull tre to terminate the string with a null byte
	 * @param lineBreakConversion how to convert linebreaks in the string
	 * @param noCache true to not write the result to an internal cache; in this cache, the method returns a {@link DisposableMemory} object
	 * @return encoded string in memory, might be a shared copy if the string could be find in the cache
	 */
	private static Memory toLMBCS(String inStr, boolean addNull, LineBreakConversion lineBreakConversion, boolean noCache) {
		if (inStr==null)
			return null;
		
		if (inStr.length()==0) {
			if (addNull) {
				Memory m = new Memory(1);
				m.setByte(0, (byte) 0);
				return m;				
			}
			else {
				return null;
			}
		}
		
		LRUStringLMBCSCache cacheToUse;
		
		if (!noCache) {
			Memory cachedMem;
			if (addNull) {
				if (lineBreakConversion == LineBreakConversion.NULL) {
					cacheToUse = m_string2LMBCSCache_NullTerminated_NullLinebreaks;
				}
				else if (lineBreakConversion == LineBreakConversion.LINEFEED) {
					cacheToUse = m_string2LMBCSCache_NullTerminated_LinefeedLinebreaks;
				}
				else if (lineBreakConversion == LineBreakConversion.ORIGINAL) {
					cacheToUse = m_string2LMBCSCache_NullTerminated_OriginalLinebreaks;
				}
				else {
					throw new IllegalArgumentException("Unsupported line break conversion: "+lineBreakConversion);
				}
			}
			else {
				if (lineBreakConversion == LineBreakConversion.NULL) {
					cacheToUse = m_string2LMBCSCache_NotNullTerminated_NullLinebreaks;
				}
				else if (lineBreakConversion == LineBreakConversion.LINEFEED) {
					cacheToUse = m_string2LMBCSCache_NotNullTerminated_LinefeedLinebreaks;
				}
				else if (lineBreakConversion == LineBreakConversion.ORIGINAL) {
					cacheToUse = m_string2LMBCSCache_NotNullTerminated_OriginalLinebreaks;
				}
				else {
					throw new IllegalArgumentException("Unsupported line break conversion: "+lineBreakConversion);
				}
			}
			
			cachedMem = cacheToUse.get(inStr);
			
			if (cachedMem!=null) {
				return cachedMem;
			}
		}
		else {
			cacheToUse = null;
		}

		boolean inStrHasLinebreaks;
		String[] lines;
		if (inStr.contains("\n") && lineBreakConversion != LineBreakConversion.ORIGINAL) {
			lines = inStr.split("\\r?\\n", -1);
			inStrHasLinebreaks = true;
		}
		else {
			lines = new String[1];
			lines[0] = inStr;
			inStrHasLinebreaks = false;
		}
		
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		
		INotesNativeAPI api = NotesNativeAPI.get();
		
		DisposableMemory inputBufUTF8 = null;
		DisposableMemory outputBufLMBCS = null;
		try {
			for (int i=0; i<lines.length; i++) {
				if (inStrHasLinebreaks && i>0) {
					if (lineBreakConversion == LineBreakConversion.NULL) {
						//replace line breaks with null characters
						bOut.write(0);
					}
					else if (lineBreakConversion == LineBreakConversion.LINEFEED) {
						//replace line breaks (e.g. \r\n on Windows) with \n
						bOut.write('\n');
					}
					else {
						//should not happen
						throw new IllegalArgumentException("Unexpected line break conversion: "+lineBreakConversion);
					}
				}
				
				if (lines[i].length() == 0) {
					continue;
				}
				
				//check if string only contains ascii characters that map 1:1 to LMBCS;
				//in this case we can skip the OSTranslate call
				boolean isPureAscii = true;
				for (int x=0; x<lines[i].length(); x++) {
					char c = lines[i].charAt(x);
					if (c <= 0x1f || c >= 0x80) {
						isPureAscii = false;
						break;
					}
				}
				
				byte[] lineDataAsUTF8 = lines[i].getBytes(charsetUTF8);
				
				if (isPureAscii) {
					try {
						bOut.write(lineDataAsUTF8);
					} catch (IOException e) {
						throw new NotesError(0, "Error writing to temporary byte stream", e);
					}
				}
				else {
					int worstCaseLMBCSLength = 3 * lines[i].length();
					
					if (inputBufUTF8!=null && inputBufUTF8.size() < lineDataAsUTF8.length) {
						inputBufUTF8.dispose();
						inputBufUTF8 = null;
					}
					
					if (inputBufUTF8==null) {
						inputBufUTF8 = new DisposableMemory(lineDataAsUTF8.length);
					}
					inputBufUTF8.write(0, lineDataAsUTF8, 0, lineDataAsUTF8.length);
					
					if (outputBufLMBCS!=null && outputBufLMBCS.size() < worstCaseLMBCSLength) {
						outputBufLMBCS.dispose();
						outputBufLMBCS = null;
					}
					
					if (outputBufLMBCS==null) {
						outputBufLMBCS = new DisposableMemory(worstCaseLMBCSLength);
					}
					
					do {
						int retOutBufLength = api.OSTranslate32(
								NotesConstants.OS_TRANSLATE_UTF8_TO_LMBCS,
								inputBufUTF8, (int) lineDataAsUTF8.length,
								outputBufLMBCS, (int) outputBufLMBCS.size());
						
						if (retOutBufLength==outputBufLMBCS.size()) {
							// output buffer not large enough, increase it and retry (not expected to happen because of
							// our worst case computation)
							long oldOutBufSize = outputBufLMBCS.size();
							long newOutBufSize = (long) (((double) oldOutBufSize)*2);
							outputBufLMBCS.dispose();
							outputBufLMBCS = new DisposableMemory(newOutBufSize);

							continue;
						}
						else if (retOutBufLength>0) {
							//success
							byte[] data = outputBufLMBCS.getByteArray(0, retOutBufLength);
							try {
								bOut.write(data);
							} catch (IOException e) {
								throw new NotesError(0, "Error writing to temporary byte stream", e);
							}
							break;
						}
					}
					while (true);
				}
			}
		}
		finally {
			if (inputBufUTF8!=null) {
				inputBufUTF8.dispose();
			}
			
			if (outputBufLMBCS!=null) {
				outputBufLMBCS.dispose();
			}
		}
		
		if (addNull) {
			int limit = bOut.size();
			
			Memory m;
			if (noCache) {
				m = new DisposableMemory(limit + 1);
			}
			else {
				m = new ReadOnlyMemory(limit + 1);
			}
			
			byte[] data = bOut.toByteArray();
			m.write(0, data, 0, data.length);
			m.setByte(limit, (byte) 0);
			
			if (!noCache) {
				((ReadOnlyMemory)m).seal();
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					if (cacheToUse!=null) {
						cacheToUse.put(inStr, m);
					}
				}
			}

			return m;
		}
		else {
			Memory m;
			if (noCache) {
				m = new DisposableMemory(bOut.size());
			}
			else {
				m = new ReadOnlyMemory(bOut.size());
			}

			byte[] data = bOut.toByteArray();
			m.write(0, data, 0, data.length);
			
			if (!noCache) {
				((ReadOnlyMemory)m).seal();
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					if (cacheToUse!=null) {
						cacheToUse.put(inStr, m);
					}
				}
			}
			
			return m;
		}
	}

	/**
	 * Converts bytes in memory to a UNID
	 * 
	 * @param innardsFile innards of file part
	 * @param innardsNote innards of note part
	 * @return unid
	 */
	public static String toUNID(long innardsFile, long innardsNote) {
		Formatter formatter = new Formatter();
		
		formatter.format("%016x", innardsFile);
		formatter.format("%016x", innardsNote);
		String unid = formatter.toString().toUpperCase();
		formatter.close();
		return unid;
	}

	/**
	 * Reads a UNID from memory
	 * 
	 * @param ptr memory
	 * @return UNID as string
	 */
	public static String pointerToUnid(Pointer ptr) {
		Formatter formatter = new Formatter();
		ByteBuffer data = ptr.getByteBuffer(0, 16).order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%016x", data.getLong());
		formatter.format("%016x", data.getLong());
		String unidStr = formatter.toString().toUpperCase();
		formatter.close();
		return unidStr;
	}
	
	/**
	 * Writes a UNID string to memory
	 * 
	 * @param unidStr UNID string
	 * @param target target memory
	 */
	public static void unidToPointer(String unidStr, Pointer target) {
		try {
			int fileInnards1 = (int) (Long.parseLong(unidStr.substring(0,8), 16) & 0xffffffff);
			int fileInnards0 = (int) (Long.parseLong(unidStr.substring(8,16), 16) & 0xffffffff);

			int noteInnards1 = (int) (Long.parseLong(unidStr.substring(16,24), 16) & 0xffffffff);
			int noteInnards0 = (int) (Long.parseLong(unidStr.substring(24,32), 16) & 0xffffffff);

			target.setInt(0, fileInnards0);
			target.share(4).setInt(0, fileInnards1);
			target.share(8).setInt(0, noteInnards0);
			target.share(12).setInt(0, noteInnards1);
		}
		catch (Exception e) {
			throw new NotesError(0, "Could not convert UNID to memory: "+unidStr, e);
		}
	}
	
	/**
	 * This function takes a port name, a server name, and file path relative to the Domino or
	 * Notes data directory and creates a full network path specification for a Domino database
	 * file.<br>
	 * <br>
	 * To open a Domino database on a server, use this function to create the full path specification,
	 * and pass this specification as input to NSFDbOpen or NSFDbOpenExtended.
	 * 
	 * @param portName network port name or NULL to allow Domino or Notes to use the "most available" port to the given server
	 * @param serverName Name of the server (either in abbreviated format, canonical format or as common name)  or "" for local
	 * @param fileName filename of the Domino database you with to access, relative to the data directory
	 * @return fully qualified network path
	 */
	public static String osPathNetConstruct(String portName, String serverName, String fileName) {
		serverName = NotesNamingUtils.toCanonicalName(serverName);
		
		Memory portNameMem = toLMBCS(portName, true);
		Memory serverNameMem = toLMBCS(serverName, true);
		Memory fileNameMem = toLMBCS(fileName, true);
		
		DisposableMemory retPathMem = new DisposableMemory(NotesConstants.MAXPATH);
		
		short result = NotesNativeAPI.get().OSPathNetConstruct(portNameMem, serverNameMem, fileNameMem, retPathMem);
		NotesErrorUtils.checkResult(result);
		String retPath = fromLMBCS(retPathMem, getNullTerminatedLength(retPathMem));
		retPathMem.dispose();
		return retPath;
	}

	/**
	 * Given a fully-qualified network path to a Domino database file, this function breaks it
	 * into its port name, server name, and filename components.<br>
	 * If the fully qualified path contains just the port name and/or server name components,
	 * then they will be the only ones returned.<br>
	 * <br>
	 * Expanded database filepath syntax:<br>
	 * <br>
	 * {Port} NetworkSeparator {servername} Serversuffix {filename}<br>
	 * COM! {NetworkSeparator} NOTESBETA {ServerSuffix} NOTEFILE\APICOMMS.NSF<br>
	 * <br>
	 * Note: the NetworkSeparator and ServerSuffix are not system independent. To maintain the
	 * portability of your code, it is recommended that you make no explicit use of them
	 * anywhere in your programs.
	 * 
	 * @param pathName expanded path specification of a Domino database file
	 * @return String array of portname, servername, filename
	 */
	public static String[] osPathNetParse(String pathName) {
		DisposableMemory retPortNameMem = new DisposableMemory(NotesConstants.MAXPATH);
		DisposableMemory retServerNameMem = new DisposableMemory(NotesConstants.MAXPATH);
		DisposableMemory retFileNameMem = new DisposableMemory(NotesConstants.MAXPATH);
		
		Memory pathNameMem = toLMBCS(pathName, true);
		short result = NotesNativeAPI.get().OSPathNetParse(pathNameMem, retPortNameMem, retServerNameMem, retFileNameMem);
		NotesErrorUtils.checkResult(result);
		
		String portName = fromLMBCS(retPortNameMem, getNullTerminatedLength(retPortNameMem));
		String serverName = fromLMBCS(retServerNameMem, getNullTerminatedLength(retServerNameMem));
		String fileName = fromLMBCS(retFileNameMem, getNullTerminatedLength(retFileNameMem));
		
		retPortNameMem.dispose();
		retServerNameMem.dispose();
		retFileNameMem.dispose();
		
		return new String[] {portName, serverName, fileName};
	}

	/**
	 * Converts an innards array to hex format, e.g. used for replica ids
	 * 
	 * @param innards innards array with two elements
	 * @return replica id (16 character hex string)
	 */
	public static String innardsToReplicaId(int[] innards) {
		return StringUtil.pad(Integer.toHexString(innards[1]).toUpperCase(), 8, '0', false) +
				StringUtil.pad(Integer.toHexString(innards[0]).toUpperCase(), 8, '0', false);
	}

	/**
	 * Converts a replica id to an innards array
	 * 
	 * @param replicaId replica id
	 * @return innards array with two elements
	 */
	public static int[] replicaIdToInnards(String replicaId) {
		if (replicaId.contains(":"))
			replicaId = replicaId.replace(":", "");
		
		if (replicaId.length() != 16) {
			throw new IllegalArgumentException("Replica ID is expected to have 16 hex characters or 8:8 format");
		}
		
		int[] innards = new int[2];
		innards[1] = (int) (Long.parseLong(replicaId.substring(0,8), 16) & 0xffffffff);
		innards[0] = (int) (Long.parseLong(replicaId.substring(8), 16) & 0xffffffff);
		
		return innards;
	}
	
	private static class LRUStringLMBCSCache extends SizeLimitedLRUCache<String, Memory> {

		public LRUStringLMBCSCache(int maxSizeUnits) {
			super(maxSizeUnits);
		}

		@Override
		protected int computeSize(String key, Memory value) {
			return key.length()*2 + (int) value.size();
		}
		
	}
	
}
