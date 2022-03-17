package com.mindoo.domino.jna.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.constants.CDRecordType.Area;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Utility class to dump memory content
 * 
 * @author Karsten Lehmann
 */
public class DumpUtil {

	/**
	 * Creates a log file and ensures its content is
	 * written to disk when the method is done (e.g. to write something to disk before
	 * a crash).<br>
	 * By default we use the temp directory (system property "java.io.tmpdir").
	 * By setting the system property "dominojna.dumpdir", the output directory can be changed.
	 * 
	 * @param suffix filename will be "domino-jnalog-" + suffix + uniquenr + ".txt"
	 * @param content file content
	 * @return created temp file or null in case of errors
	 */
	public static File writeLogFile(final String suffix, final String content) {
		return AccessController.doPrivileged(new PrivilegedAction<File>() {

			@Override
			public File run() {
				FileOutputStream fOut = null;
				Writer fWriter = null;
				try {
					String outDirPath = System.getProperty("dominojna.dumpdir");
					if (StringUtil.isEmpty(outDirPath)) {
						outDirPath = System.getProperty("java.io.tmpdir");
					}
					File outDir = new File(outDirPath);
					if (!outDir.exists())
						outDir.mkdirs();
					File dmpFile = File.createTempFile("domino-jnalog-"+suffix+"-", ".txt", outDir);
					
					fOut = new FileOutputStream(dmpFile);
					fWriter = new OutputStreamWriter(fOut, Charset.forName("UTF-8"));
					fWriter.write(content);
					fWriter.flush();
					FileChannel channel = fOut.getChannel();
					channel.force(true);
					return dmpFile;
				} catch (IOException e) {
					e.printStackTrace();
				}
				finally {
					if (fWriter!=null) {
						try {
							fWriter.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (fOut!=null) {
						try {
							fOut.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				
				return null;
			}
		});
	}
	
	/**
	 * Produces a String with hex codes for the specified byte array and
	 * character data in case the memory contains bytes in ascii range.
	 * 
	 * @param data byte array
	 * @return memory dump
	 */
	public static String dumpAsAscii(byte[] data) {
		return dumpAsAscii(ByteBuffer.wrap(data), data.length);
	}
	
	/**
	 * Reads memory content at the specified pointer and produces a String with hex codes and
	 * character data in case the memory contains bytes in ascii range. Calls {@link #dumpAsAscii(Pointer, int, int)}
	 * with cols = 8.
	 * 
	 * @param ptr pointer
	 * @param size number of bytes to read
	 * @return memory dump
	 */
	public static String dumpAsAscii(Pointer ptr, int size) {
		return dumpAsAscii(ptr, size, 8);
	}
	
	/**
	 * Reads memory content at the specified pointer and produces a String with hex codes and
	 * character data in case the memory contains bytes in ascii range.
	 * 
	 * @param ptr pointer
	 * @param size number of bytes to read
	 * @param cols number of bytes written in on eline
	 * @return memory dump
	 */
	public static String dumpAsAscii(Pointer ptr, int size, int cols) {
		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		
		if (ptr instanceof Memory) {
			size = (int) Math.min(size, ((Memory)ptr).size());
		}
		
		while (i < size) {
			sb.append("[");
			for (int c=0; c<cols; c++) {
				if (c>0)
					sb.append(' ');
				
				if ((i+c) < size) {
					byte b = ptr.getByte(i+c);
					 if (b >=0 && b < 16)
			                sb.append("0");
			            sb.append(Integer.toHexString(b & 0xFF));
				}
				else {
					sb.append("  ");
				}
			}
			sb.append("]");
			
			sb.append("   ");
			
			sb.append("[");
			for (int c=0; c<cols; c++) {
				if ((i+c) < size) {
					byte b = ptr.getByte(i+c);
					int bAsInt = (b & 0xff);
					
					if (bAsInt >= 32 && bAsInt<=126) {
						sb.append((char) (b & 0xFF));
					}
					else {
						sb.append(".");
					}
				}
				else {
					sb.append(" ");
				}
			}
			sb.append("]\n");

			i += cols;
		}
		return sb.toString();
	}

	/**
	 * Reads content of the {@link ByteBuffer} and produces a String with hex codes and
	 * character data in case the memory contains bytes in ascii range. Calls {@link #dumpAsAscii(ByteBuffer, int, int)}
	 * with cols = 8 and size = buf.limit().
	 * 
	 * @param buf byte buffer
	 * @return memory dump
	 * @since 1.0.32
	 */
	public static String dumpAsAscii(ByteBuffer buf) {
		return dumpAsAscii(buf, buf.limit());
	}

	/**
	 * Reads content of the {@link ByteBuffer} and produces a String with hex codes and
	 * character data in case the memory contains bytes in ascii range. Calls {@link #dumpAsAscii(Pointer, int, int)}
	 * with cols = 8.
	 * 
	 * @param buf byte buffer
	 * @param size number of bytes to read
	 * @return memory dump
	 */
	public static String dumpAsAscii(ByteBuffer buf, int size) {
		return dumpAsAscii(buf, size, 8);
	}

	/**
	 * Reads content of the {@link ByteBuffer} and produces a String with hex codes and
	 * character data in case the memory contains bytes in ascii range.
	 * 
	 * @param buf byte buffer
	 * @param size number of bytes to read
	 * @param cols number of bytes written in on eline
	 * @return memory dump
	 */
	public static String dumpAsAscii(ByteBuffer buf, int size, int cols) {
		StringBuilder sb = new StringBuilder();
		
		int i = 0;

		size = Math.min(size, buf.limit());

		while (i < size) {
			sb.append("[");
			for (int c=0; c<cols; c++) {
				if (c>0)
					sb.append(' ');
				
				if ((i+c) < size) {
					byte b = buf.get(i+c);
					 if (b >=0 && b < 16)
			                sb.append("0");
			            sb.append(Integer.toHexString(b & 0xFF));
				}
				else {
					sb.append("  ");
				}
			}
			sb.append("]");
			
			sb.append("   ");
			
			sb.append("[");
			for (int c=0; c<cols; c++) {
				if ((i+c) < size) {
					byte b = buf.get(i+c);
					int bAsInt = (b & 0xff);
					
					if (bAsInt >= 32 && bAsInt<=126) {
						sb.append((char) (b & 0xFF));
					}
					else {
						sb.append(".");
					}
				}
				else {
					sb.append(" ");
				}
			}
			sb.append("]\n");

			i += cols;
		}
		return sb.toString();
	}
	
	/**
	 * Specified which type of handle data to dump
	 */
	public static enum MemDump {
		PRIVATE(0x00000000),
		SHARED(0x00000001),
		/** Dump binary content of the allocated memory */
		CONTENTS(0x00000002),
		POOL(0x00000004),
		/** Only dump our process's shared and private. */
		PROCESS(0x00000008),
		/** dump OSLocal stuff */
		LOCAL(0x00000010),
		COMMANDLINE(0x00000020),
		FULL(0x00000040);
		
		private int type;
		
		private MemDump(int type) {
			this.type = type;
		}
		
		private int getType() {
			return this.type;
		}
		
	};
	
	/**
	 * Writes information about the currently accolated memory handles to disk
	 * (&lt;notesdata&gt;/IBM_TECHNICAL_SUPPORT/memory_*.dmp).
	 * 
	 * @param flags data to dump
	 * @param blkType dump blocks of this type, 0 for all 
	 */
	public static void dumpHandleTable(Set<MemDump> flags, int blkType) {
		int typeAsInt = 0;
		
		for (MemDump currType : flags) {
			typeAsInt = typeAsInt | currType.getType();
		}
		
		NotesNativeAPI.get().DEBUGDumpHandleTable(typeAsInt, (short) (blkType & 0xffff));
	}
	
	/**
	 * Dumps the current richtext CD record
	 * 
	 * @param nav richtext navigator
	 * @param out output stream
	 */
	public static void dumpCurrentRichtextRecord(IRichTextNavigator nav, PrintStream out) {
		short cdRecordTypeAsShort = nav.getCurrentRecordTypeAsShort();
		if (cdRecordTypeAsShort!=0) {
			CDRecordType cdRecordType = CDRecordType.getRecordTypeForConstant(cdRecordTypeAsShort, Area.TYPE_COMPOSITE);
			if (cdRecordType==null) {
				cdRecordType = CDRecordType.getRecordTypeForConstant(cdRecordTypeAsShort, Area.RESERVED_INTERNAL);
			}
			if (cdRecordType==null) {
				cdRecordType = CDRecordType.getRecordTypeForConstant(cdRecordTypeAsShort, Area.ALTERNATE_SEQ);
			}
			if (cdRecordType==null) {
				cdRecordType = CDRecordType.getRecordTypeForConstant(cdRecordTypeAsShort, Area.TARGET_FRAME);
			}
			if (cdRecordType==null) {
				cdRecordType = CDRecordType.getRecordTypeForConstant(cdRecordTypeAsShort, Area.FRAMESETS);
			}
			if (cdRecordType==null) {
				cdRecordType = CDRecordType.getRecordTypeForConstant(cdRecordTypeAsShort, Area.TYPE_VIEWMAP);
			}

			out.println("Record type: "+(cdRecordTypeAsShort & 0xffff)+(cdRecordType==null ? "" : " ("+cdRecordType+")"));
			out.println("Total length: "+nav.getCurrentRecordTotalLength());
			out.println("Header length: "+nav.getCurrentRecordHeaderLength());
			out.println("Data length: "+nav.getCurrentRecordDataLength());

			Memory memWithHeader = nav.getCurrentRecordDataWithHeader();
			if (memWithHeader!=null) {
				out.println("\n" + DumpUtil.dumpAsAscii(memWithHeader, (int) memWithHeader.size())+"\n");
			}
		}
	}
}
