package com.mindoo.domino.jna.utils;

import java.io.UnsupportedEncodingException;

import com.mindoo.domino.jna.NotesCAPI;
import com.mindoo.domino.jna.NotesContext;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * String conversion functions between Java and LMBCS
 * 
 * @author Karsten Lehmann
 */
public class NotesStringUtils {

	public static String fromLMBCS(Memory in) {
		
//		byte[] outAsBytes = new byte[in.limit() * 2];
//		ByteBuffer out = ByteBuffer.wrap(outAsBytes);

		NotesCAPI notesAPI = NotesContext.getNotesAPI();
	//	public short OSTranslate(short translateMode, Memory in, short inLength, Memory out, short outLength);

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

	public static String fromLMBCS(Pointer inPtr, short bufSizeInBytes) {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		
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

	public static Memory toLMBCS(String inStr) {
		if (inStr==null)
			return null;
		
		if ("".equals(inStr)) {
			Memory m = new Memory(1);
			m.setByte(0, (byte) 0);
			return m;
		}
		
		NotesCAPI notesAPI = NotesContext.getNotesAPI();

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

}
