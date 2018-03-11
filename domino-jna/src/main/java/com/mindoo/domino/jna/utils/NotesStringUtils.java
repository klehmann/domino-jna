package com.mindoo.domino.jna.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import com.ibm.icu.charset.CharsetICU;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
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
	
	private static LRUStringLMBCSCache m_string2LMBCSCacheWithNull = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	private static LRUStringLMBCSCache m_string2LMBCSCacheWithoutNull = new LRUStringLMBCSCache(MAX_STRING2LMBCS_SIZE_BYTES);
	//shared CharsetLMBCS instance
	private static Charset LMBCSCharset = CharsetICU.forNameICU("LMBCS");
	
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
			List<String> keysWithNull = m_string2LMBCSCacheWithNull.getKeys();
			for (String currKey : keysWithNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCacheWithNull.remove(currKey);
				}
			}
			List<String> keysWithoutNull = m_string2LMBCSCacheWithoutNull.getKeys();
			for (String currKey : keysWithoutNull) {
				if (currKey.indexOf('\n') != -1) {
					m_string2LMBCSCacheWithoutNull.remove(currKey);
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
		int startOffset = 0;
		
		List<String> lines = new ArrayList<String>();
		
		for (int i=0; i<data.length; i++) {
			if (data[i] == 0) {
				CharBuffer newLineBuf = LMBCSCharset.decode(ByteBuffer.wrap(data, startOffset, i-startOffset));
				String newLine = newLineBuf.toString();
				lines.add(newLine);
				startOffset = i+1;
				
				if (i==(data.length-1)) {
					lines.add("");
				}
			}
		}
		
		if (startOffset<data.length) {
			CharBuffer newLineBuf = LMBCSCharset.decode(ByteBuffer.wrap(data, startOffset, data.length-startOffset));
			String newLine = newLineBuf.toString();
			lines.add(newLine);
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
		if (textLen==-1) {
			textLen = getNullTerminatedLength(inPtr);
			CharBuffer charBuf = LMBCSCharset.decode(inPtr.getByteBuffer(0, textLen));
			String str = charBuf.toString();
			return str;
		}
		else {
			//check for \0 as newline delimiter
			byte[] dataArr = inPtr.getByteArray(0, textLen);
			int startOffset = 0;
			
			List<String> lines = new ArrayList<String>();
			
			for (int i=0; i<textLen; i++) {
				if (dataArr[i] == 0) {
					CharBuffer newLineBuf = LMBCSCharset.decode(ByteBuffer.wrap(dataArr, startOffset, i-startOffset));
					String newLine = newLineBuf.toString();
					lines.add(newLine);
					startOffset = i+1;
					
					if (i==(textLen-1)) {
						lines.add("");
					}
				}
			}
			
			if (startOffset<textLen) {
				CharBuffer newLineBuf = LMBCSCharset.decode(ByteBuffer.wrap(dataArr, startOffset, textLen-startOffset));
				String newLine = newLineBuf.toString();
				lines.add(newLine);
			}
			boolean useOSLineBreak = isUseOSLineDelimiter();
			if (PlatformUtils.isWindows() && useOSLineBreak) {
				return StringUtil.join(lines, "\r\n");
			}
			else {
				return StringUtil.join(lines, "\n");
			}
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
		
		Memory cachedMem;
		if (addNull) {
			cachedMem = m_string2LMBCSCacheWithNull.get(inStr);
		}
		else {
			cachedMem = m_string2LMBCSCacheWithoutNull.get(inStr);
		}
		
		if (cachedMem!=null) {
			return cachedMem;
		}
		
		if (inStr.contains("\n")) {
			//replace line breaks with null characters
			String[] lines = inStr.split("\\r?\\n", -1);
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			for (int i=0; i<lines.length; i++) {
				if (i>0)
					bOut.write(0);

				CharBuffer charBuf = CharBuffer.wrap(lines[i]);
				ByteBuffer byteBuf = LMBCSCharset.encode(charBuf);
				try {
					if (byteBuf.hasArray()) {
						bOut.write(byteBuf.array(), byteBuf.arrayOffset(), byteBuf.limit());
					}
					else {
						byte[] data = new byte[byteBuf.limit()];
						byteBuf.get(data);
						bOut.write(data);
					}
				}
				catch (IOException e) {
					throw new NotesError(0, "Error writing converted data", e);
				}
			}
			
			if (addNull) {
				int limit = bOut.size();
				ReadOnlyMemory m = new ReadOnlyMemory(limit + 1);
				byte[] data = bOut.toByteArray();
				m.write(0, data, 0, data.length);
				m.setByte(limit, (byte) 0);
				m.seal();

				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					m_string2LMBCSCacheWithNull.put(inStr, m);
				}
				return m;
			}
			else {
				ReadOnlyMemory m = new ReadOnlyMemory(bOut.size());
				byte[] data = bOut.toByteArray();
				m.write(0, data, 0, data.length);
				m.seal();
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					m_string2LMBCSCacheWithoutNull.put(inStr, m);
				}
				return m;
			}
		}
		else {
			CharBuffer charBuf = CharBuffer.wrap(inStr);
			ByteBuffer byteBuf = LMBCSCharset.encode(charBuf);
			
			if (addNull) {
				int limit = byteBuf.limit();
				
				ReadOnlyMemory m = new ReadOnlyMemory(limit + 1);
				if (byteBuf.hasArray()) {
					m.write(0, byteBuf.array(), byteBuf.arrayOffset(), limit);
				}
				else {
					byte[] dataArr = new byte[limit];
					byteBuf.get(dataArr);
					m.write(0, dataArr, 0, dataArr.length);
				}
				m.setByte(limit, (byte) 0);
				m.seal();
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					m_string2LMBCSCacheWithNull.put(inStr, m);
				}
				return m;
			}
			else {
				int limit = byteBuf.limit();
				
				ReadOnlyMemory m = new ReadOnlyMemory(limit);
				if (byteBuf.hasArray()) {
					m.write(0, byteBuf.array(), byteBuf.arrayOffset(), limit);
				}
				else {
					byte[] dataArr = new byte[limit];
					byteBuf.get(dataArr);
					m.write(0, dataArr, 0, dataArr.length);
				}
				m.seal();
				
				if (USE_STRING2LMBCS_CACHE && inStr.length()<=MAX_STRING2LMBCS_KEY_LENGTH) {
					m_string2LMBCSCacheWithoutNull.put(inStr, m);
				}
				return m;
			}
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
