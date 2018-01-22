package com.mindoo.domino.jna.utils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.ReadOnlyMemory;
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
	//max number of entries in the string-lmbcs cache
	private static final int MAX_STRING2LMBCS_ENTRIES = 1000;
	//max length of each string-limbcs cache entry
	private static final int MAX_STRING2LMBCS_KEY_LENGTH = 500;
	
	private static ConcurrentHashMap<String,Memory> m_string2LMBCSCache_withoutnull = new ConcurrentHashMap<String,Memory>();
	private static ConcurrentLinkedQueue<String> m_string2LMBCSLastKeys_withoutnull = new ConcurrentLinkedQueue<String>();
	
	private static ConcurrentHashMap<String,Memory> m_string2LMBCSCache_withnull = new ConcurrentHashMap<String,Memory>();
	private static ConcurrentLinkedQueue<String> m_string2LMBCSLastKeys_withnull = new ConcurrentLinkedQueue<String>();
	
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
			
			m_string2LMBCSCache_withoutnull.clear();
			m_string2LMBCSLastKeys_withoutnull.clear();
			m_string2LMBCSCache_withnull.clear();
			m_string2LMBCSLastKeys_withnull.clear();
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
	 * Scans the Memory object for null values
	 * 
	 * @param in memory
	 * @return number of bytes before null byte in memory
	 */
	public static int getNullTerminatedLength(Memory in) {
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
	 * @param inPtr pointer in memory
	 * @param textLen length of text, use -1 to let the method search for a terminating \0
	 * @return decoded String
	 */
	public static String fromLMBCS(Pointer inPtr, int textLen) {
		return fromLMBCS(inPtr, textLen, false);
	}
	
	/**
	 * Converts an LMBCS string to a Java String
	 * 
	 * @param inPtr pointer in memory
	 * @param textLen length of text, use -1 to let the method search for a terminating \0
	 * @param skipAsciiCheck true to skip the check whether the memory contains pure ASCII (parameter added to avoid a duplicate check), will always result in a C API call to convert the string
	 * @return decoded String
	 */
	public static String fromLMBCS(Pointer inPtr, int textLen, boolean skipAsciiCheck) {
		if (inPtr==null || textLen==0) {
			return "";
		}
		
		if (textLen==-1) {
			int foundLen = 0;
			int offset = 0;
			while (true) {
				if (inPtr.getByte(offset)==0) {
					break;
				}
				foundLen++;
				offset++;
			}
			textLen = foundLen;
		}

		if (!skipAsciiCheck) {
			boolean isPureAscii = true;
			byte[] data = inPtr.getByteArray(0, textLen);
			for (int i=0; i < textLen; i++) {
				byte b = data[i];
				if (b <= 0x1f || b >= 0x80) {
					isPureAscii = false;
					break;
				}
			}
			
			if (isPureAscii) {
				String asciiStr = new String(data, Charset.forName("ASCII"));
				return asciiStr;
			}
		}
	
		Pointer pText = inPtr;
		boolean useOSLineBreak = isUseOSLineDelimiter();
		
		Memory pBuf_utf8 = null;
		
		StringBuilder result = new StringBuilder(textLen + 5);
		while (textLen > 0) {
			long len=(textLen>NotesConstants.MAXPATH) ? NotesConstants.MAXPATH : textLen;
			long outLen=2*len;
			
			if (pBuf_utf8==null || pBuf_utf8.size()!=(outLen+1)) {
				pBuf_utf8 = new Memory(outLen+1);
			}

			//convert text from LMBCS to utf8
			int len_utf8 = NotesNativeAPI.get().OSTranslate(NotesConstants.OS_TRANSLATE_LMBCS_TO_UTF8, pText, (short) (len & 0xffff), pBuf_utf8, (short) (outLen & 0xffff));
			pBuf_utf8.setByte(len_utf8, (byte) 0);
			
			// copy 
			String currConvertedStr;
			try {
				currConvertedStr = new String(pBuf_utf8.getByteArray(0, len_utf8), 0, len_utf8, "UTF-8");
				if (currConvertedStr.contains("\0")) {
					//Notes uses \0 for multiline strings
					if (PlatformUtils.isWindows() && useOSLineBreak) {
						currConvertedStr = currConvertedStr.replace("\0", "\r\n");
					}
					else {
						currConvertedStr = currConvertedStr.replace("\0", "\n");
					}
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unknown encoding UTF-8", e);
			}
			
			textLen -= len;
			
			//shortcut for short strings
			if (result==null && textLen<=0) {
				return currConvertedStr;
			}
			
			if (result==null) {
				result = new StringBuilder();
			}
			result.append(currConvertedStr);
			
			pText = pText.share(len);
		}
		return result==null ? "" : result.toString();
	}

	/**
	 * Converts a string to LMBCS format
	 * 
	 * @param inStr string
	 * @param addNull tre to terminate the string with a null byte
	 * @return encoded string in memory, might be a shared copy if the string could be find in the cache
	 */
	public static Memory toLMBCS(String inStr, boolean addNull) {
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
		
		if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
			Memory cachedMem = addNull ? m_string2LMBCSCache_withnull.get(inStr) : m_string2LMBCSCache_withoutnull.get(inStr);
			if (cachedMem!=null) {
				return cachedMem;
			}
		}
		
		//check if string only contains ascii characters that map 1:1 to LMBCS;
		//in this case we can skip the OSTranslate call
		boolean isPureAscii = true;
		for (int i=0; i<inStr.length(); i++) {
			char c = inStr.charAt(i);
			if (c <= 0x1f || c >= 0x80) {
				isPureAscii = false;
				break;
			}
		}
		
		if (isPureAscii) {
			byte[] asciiBytes = inStr.getBytes(Charset.forName("ASCII"));
			
			if (addNull) {
				ReadOnlyMemory m = new ReadOnlyMemory(asciiBytes.length + 1);
				m.write(0, asciiBytes, 0, asciiBytes.length);
				m.setByte(asciiBytes.length, (byte) 0);
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					m.seal();
					
					m_string2LMBCSCache_withnull.put(inStr, m);
					m_string2LMBCSLastKeys_withnull.add(inStr);
					
					//compress cache
					while (m_string2LMBCSLastKeys_withnull.size()>MAX_STRING2LMBCS_ENTRIES) {
						String currStr = m_string2LMBCSLastKeys_withnull.poll();
						if (currStr==null)
							break;
						m_string2LMBCSCache_withnull.remove(currStr);
					}
				}
				return m;
			}
			else {
				ReadOnlyMemory m = new ReadOnlyMemory(asciiBytes.length);
				m.write(0, asciiBytes, 0, asciiBytes.length);
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					m.seal();
					
					m_string2LMBCSCache_withoutnull.put(inStr, m);
					m_string2LMBCSLastKeys_withoutnull.add(inStr);
					
					//compress cache
					while (m_string2LMBCSLastKeys_withoutnull.size()>MAX_STRING2LMBCS_ENTRIES) {
						String currStr = m_string2LMBCSLastKeys_withoutnull.poll();
						if (currStr==null)
							break;
						m_string2LMBCSCache_withoutnull.remove(currStr);
					}
				}
				return m;
			}
		}
			
		if (inStr.contains("\n")) {
			//replace line breaks with null characters
			String[] lines = inStr.split("\\r?\\n", -1);
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<lines.length; i++) {
				if (i>0) {
					sb.append('\0');
				}
				sb.append(lines[i]);
			}
			inStr = sb.toString();
		}

		String currRemainingStr = inStr;
		
		final int maxStrSize = 32767;
		
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		
		while (currRemainingStr.length()>0) {
			//decide how much text we want to process; we need to do some calculations
			//to not exceed the max buffer size for the UTF-8 characters of 65535 bytes (length is specified as WORD)
			int numWorkCharacters = Math.min(currRemainingStr.length(), maxStrSize);
			
			@SuppressWarnings("unused")
			int remainingStrUtf8Size;
			while ((remainingStrUtf8Size = StringUtil.stringLengthInUTF8(currRemainingStr.substring(0, numWorkCharacters))) > 32767) {
				numWorkCharacters -= 10;
			}
			
			String currWorkStr = currRemainingStr.substring(0, numWorkCharacters);
			currRemainingStr = currRemainingStr.substring(numWorkCharacters);
			
			
			byte[] currWorkStrAsBytes;
			try {
				currWorkStrAsBytes = currWorkStr.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unknown encoding UTF-8", e);
			}
			Memory in = new Memory(currWorkStrAsBytes.length);
			in.write(0, currWorkStrAsBytes, 0, currWorkStrAsBytes.length);

			Memory out = new Memory(in.size() * 2);
			if (out.size() >= 65535) {
				throw new IllegalStateException("out buffer is expected to be in WORD range. "+out.size()+" >= 65535");
			}
			
			short outContentLength = NotesNativeAPI.get().OSTranslate(NotesConstants.OS_TRANSLATE_UTF8_TO_LMBCS, in, (short) (in.size() & 0xffff), out, (short) (out.size() & 0xffff));
			byte[] outAsBytes = new byte[outContentLength];
			
			out.read(0, outAsBytes, 0, outContentLength);
			bOut.write(outAsBytes, 0, outContentLength);
		}
		
		if (addNull) {
			ReadOnlyMemory all = new ReadOnlyMemory(bOut.size()+1);
			byte[] allAsBytes = bOut.toByteArray();
			all.write(0, allAsBytes, 0, bOut.size());
			all.setByte(all.size()-1, (byte) 0); 
			
			if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
				all.seal();
				
				m_string2LMBCSCache_withnull.put(inStr, all);
				m_string2LMBCSLastKeys_withnull.add(inStr);
				
				//compress cache
				while (m_string2LMBCSLastKeys_withnull.size()>MAX_STRING2LMBCS_ENTRIES) {
					String currStr = m_string2LMBCSLastKeys_withnull.poll();
					if (currStr==null)
						break;
					m_string2LMBCSCache_withnull.remove(currStr);
				}
			}
			return all;			
		}
		else {
			ReadOnlyMemory all = new ReadOnlyMemory(bOut.size());
			byte[] allAsBytes = bOut.toByteArray();
			all.write(0, allAsBytes, 0, bOut.size());
			
			if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
				all.seal();
				
				m_string2LMBCSCache_withoutnull.put(inStr, all);
				m_string2LMBCSLastKeys_withoutnull.add(inStr);
				
				//compress cache
				while (m_string2LMBCSLastKeys_withoutnull.size()>MAX_STRING2LMBCS_ENTRIES) {
					String currStr = m_string2LMBCSLastKeys_withoutnull.poll();
					if (currStr==null)
						break;
					m_string2LMBCSCache_withoutnull.remove(currStr);
				}
			}
			return all;
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
		Memory portNameMem = toLMBCS(portName, true);
		Memory serverNameMem = toLMBCS(serverName, true);
		Memory fileNameMem = toLMBCS(fileName, true);
		
		Memory retPathMem = new Memory(NotesConstants.MAXPATH);
		
		short result = NotesNativeAPI.get().OSPathNetConstruct(portNameMem, serverNameMem, fileNameMem, retPathMem);
		NotesErrorUtils.checkResult(result);
		String retPath = fromLMBCS(retPathMem, getNullTerminatedLength(retPathMem));
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
		Memory retPortNameMem = new Memory(NotesConstants.MAXPATH);
		Memory retServerNameMem = new Memory(NotesConstants.MAXPATH);
		Memory retFileNameMem = new Memory(NotesConstants.MAXPATH);
		
		Memory pathNameMem = toLMBCS(pathName, true);
		short result = NotesNativeAPI.get().OSPathNetParse(pathNameMem, retPortNameMem, retServerNameMem, retFileNameMem);
		NotesErrorUtils.checkResult(result);
		
		String portName = fromLMBCS(retPortNameMem, getNullTerminatedLength(retPortNameMem));
		String serverName = fromLMBCS(retServerNameMem, getNullTerminatedLength(retServerNameMem));
		String fileName = fromLMBCS(retFileNameMem, getNullTerminatedLength(retFileNameMem));
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
}
