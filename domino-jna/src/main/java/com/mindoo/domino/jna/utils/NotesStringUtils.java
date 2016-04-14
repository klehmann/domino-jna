package com.mindoo.domino.jna.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Formatter;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesOriginatorId;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * String conversion functions between Java and LMBCS
 * 
 * @author Karsten Lehmann
 */
public class NotesStringUtils {

	/**
	 * Decodes an LMBCS encoded string to a Java String
	 * 
	 * @param in memory with encoded string
	 * @return decoded string
	 */
	public static String fromLMBCS(Memory in) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		short inSize = (short) in.size();
		//search for terminating null character
		for (short i=0; i<in.size(); i++) {
			byte b = in.getByte(i);
			if (b==0) {
				inSize = i;
				break;
			}
		}
		
		Memory outPtr = new Memory(NotesCAPI.MAXPATH);
		short outContentLength = notesAPI.OSTranslate(NotesCAPI.OS_TRANSLATE_LMBCS_TO_UTF8, in, inSize, outPtr, (short) outPtr.size());
		if (outContentLength > outPtr.size()) {
			outPtr = new Memory(outContentLength);
			outContentLength = notesAPI.OSTranslate(NotesCAPI.OS_TRANSLATE_LMBCS_TO_UTF8, in, inSize, outPtr, (short) outContentLength);
		}
		
		try {
			return new String(outPtr.getByteArray(0, outContentLength), 0, outContentLength, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown encoding UTF-8", e);
		}
	}

	/**
	 * Converts an LMBCS string to a Java String
	 * 
	 * @param inPtr pointer in memory
	 * @param bufSizeInBytes string lengths in bytes
	 * @return decoded String
	 */
	public static String fromLMBCS(Pointer inPtr, short bufSizeInBytes) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Memory outPtr = new Memory(NotesCAPI.MAXPATH);
		short outContentLength = notesAPI.OSTranslate(NotesCAPI.OS_TRANSLATE_LMBCS_TO_UTF8, inPtr, bufSizeInBytes, outPtr, (short) outPtr.size());
		if (outContentLength > outPtr.size()) {
			outPtr = new Memory(outContentLength);
			outContentLength = notesAPI.OSTranslate(NotesCAPI.OS_TRANSLATE_LMBCS_TO_UTF8, inPtr, bufSizeInBytes, outPtr, (short) outContentLength);
		}
		
		try {
			return new String(outPtr.getByteArray(0, outContentLength), 0, outContentLength, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown encoding UTF-8", e);
		}
	}

	/**
	 * Converts a string to LMBCS format
	 * 
	 * @param inStr string
	 * @return encoded string in memory
	 */
	public static Memory toLMBCS(String inStr) {
		if (inStr==null)
			return null;
		
		if ("".equals(inStr)) {
			Memory m = new Memory(1);
			m.setByte(0, (byte) 0);
			return m;
		}
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		byte[] inAsBytes;
		try {
			inAsBytes = inStr.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown encoding UTF-8", e);
		}
		Memory in = new Memory(inAsBytes.length);
		for (int i=0; i<inAsBytes.length; i++) {
			in.setByte(i, inAsBytes[i]);
		}

		Memory out = new Memory(inStr.length() * 2);
		short outContentLength = notesAPI.OSTranslate(NotesCAPI.OS_TRANSLATE_UTF8_TO_LMBCS, in, (short) inAsBytes.length, out, (short) out.size());
		if (outContentLength > out.size()) {
			out = new Memory(outContentLength);
			outContentLength = notesAPI.OSTranslate(NotesCAPI.OS_TRANSLATE_UTF8_TO_LMBCS, in, (short) inAsBytes.length, out, outContentLength);
		}
		Memory outResized = new Memory(outContentLength+1);
		for (int i=0; i<outContentLength; i++) {
			outResized.setByte(i, out.getByte(i));
		}
		outResized.setByte(outContentLength, (byte) 0);
		
		return outResized;
	}

	/**
	 * Extracts the UNID from a {@link NotesOriginatorId}
	 * 
	 * @param oid originator id
	 * @return unid
	 */
	public static String extractUNID(NotesOriginatorId oid) {
		oid.write();
		Pointer oidPtr = oid.getPointer();
		
		Formatter formatter = new Formatter();
		ByteBuffer data = oidPtr.getByteBuffer(0, 16).order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%16x", data.getLong());
		formatter.format("%16x", data.getLong());
		String unid = formatter.toString().toUpperCase();
		formatter.close();
		return unid;
	}
	
	/**
	 * Converts bytes in memory to a UNID
	 * 
	 * @param buf memory
	 * @return unid
	 */
	public static String toUNID(Memory buf) {
		Formatter formatter = new Formatter();
		ByteBuffer data = buf.getByteBuffer(0, buf.size()).order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%16x", data.getLong());
		formatter.format("%16x", data.getLong());
		String unid = formatter.toString().toUpperCase();
		formatter.close();
		return unid;
	}

	/**
	 * Converts bytes in memory to a UNID
	 * 
	 * @param buf memory
	 * @return unid
	 */
	public static String toUNID(ByteBuffer buf) {
		Formatter formatter = new Formatter();
		ByteBuffer data = buf.order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%16x", data.getLong());
		formatter.format("%16x", data.getLong());
		String unid = formatter.toString().toUpperCase();
		formatter.close();

		return unid;
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
		Memory portNameMem = toLMBCS(portName);
		Memory serverNameMem = toLMBCS(serverName);
		Memory fileNameMem = toLMBCS(fileName);
		
		Memory retPathMem = new Memory(NotesCAPI.MAXPATH);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result = notesAPI.OSPathNetConstruct(portNameMem, serverNameMem, fileNameMem, retPathMem);
		NotesErrorUtils.checkResult(result);
		String retPath = fromLMBCS(retPathMem);
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
		Memory retPortNameMem = new Memory(NotesCAPI.MAXPATH);
		Memory retServerNameMem = new Memory(NotesCAPI.MAXPATH);
		Memory retFileNameMem = new Memory(NotesCAPI.MAXPATH);
		
		Memory pathNameMem = toLMBCS(pathName);
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result = notesAPI.OSPathNetParse(pathNameMem, retPortNameMem, retServerNameMem, retFileNameMem);
		NotesErrorUtils.checkResult(result);
		
		String portName = fromLMBCS(retPortNameMem);
		String serverName = fromLMBCS(retServerNameMem);
		String fileName = fromLMBCS(retFileNameMem);
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
