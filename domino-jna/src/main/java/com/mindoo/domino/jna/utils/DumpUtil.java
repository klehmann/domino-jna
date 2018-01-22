package com.mindoo.domino.jna.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;

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
	 * Reads memory content at the specified pointer and produces a String with hex codes and
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
	 * Reads memory content at the specified pointer and produces a String with hex codes and
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
}
