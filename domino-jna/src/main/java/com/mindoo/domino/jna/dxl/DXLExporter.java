package com.mindoo.domino.jna.dxl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.LMBCSStringList;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.WriterOutputStream;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * DXL Exporter<br>
 * <br>
 * <b>Please make sure that you include the file jvm/lib/ext/websvc.jar of the Notes Client / Domino server
 * directory to the Java classpath if your code is not running within Notes/Domino,
 * but in a standalone application.<br>
 * <br>
 * Otherwise you might experience crashes during DXL export (we had crashes when testing the DB export).<br>
 * </b>
 * 
 * @author Karsten Lehmann
 */
public class DXLExporter implements IAllocatedMemory {
	private int m_hExporter;
	
	public DXLExporter() {
		IntByReference rethDXLExport = new IntByReference();
		short result = NotesNativeAPI.get().DXLCreateExporter(rethDXLExport);
		NotesErrorUtils.checkResult(result);
		m_hExporter = rethDXLExport.getValue();
		if (m_hExporter==0) {
			throw new NotesError(0, "Failed to allocate DXL exporter");
		}
		NotesGC.__memoryAllocated(this);
	}

	@Override
	public void free() {
		if (isFreed()) {
			return;
		}
		
		if (m_hExporter!=0) {
			NotesNativeAPI.get().DXLDeleteExporter(m_hExporter);
			m_hExporter = 0;
		}
	}

	@Override
	public boolean isFreed() {
		return m_hExporter==0;
	}

	@Override
	public int getHandle32() {
		return m_hExporter;
	}

	@Override
	public long getHandle64() {
		return m_hExporter;
	}
	
	private void checkHandle() {
		if (m_hExporter==0)
			throw new NotesError(0, "DXL exporter already freed");
		
		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidMemHandle(DXLExporter.class, m_hExporter);
		}
		else {
			NotesGC.__b32_checkValidMemHandle(DXLExporter.class, m_hExporter);
		}
	}
	
	public boolean exportErrorWasLogged() {
		checkHandle();
		
		short logged = NotesNativeAPI.get().DXLExportWasErrorLogged(m_hExporter);
		return logged == 1;
	}
	
	/**
	 * Export a single Note into XML format.
	 * 
	 * @param note note to export
	 * @param out result writer
	 * @throws IOException in case of I/O errors
	 */
	public void exportNote(NotesNote note, Writer out) throws IOException {
		WriterOutputStream outStream = new WriterOutputStream(out, Charset.forName("UTF-8"));
		exportNote(note, outStream);
		outStream.flush();
	}
	
	/**
	 * Export a single Note into XML format.
	 * 
	 * @param note note to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportNote(NotesNote note, final OutputStream out) throws IOException {
		checkHandle();
		if (note.isRecycled()) {
			throw new NotesError(0, "Note is recycled");
		}
		
		NotesCallbacks.XML_WRITE_FUNCTION callback;
		
		final Exception[] ex = new Exception[1];
		
		if (PlatformUtils.isWin32()) {
			callback = new Win32NotesCallbacks.XML_WRITE_FUNCTIONWin32() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		else {
			callback = new NotesCallbacks.XML_WRITE_FUNCTION() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().DXLExportNote(m_hExporter, callback, note.getHandle64(), (Pointer) null);
		}
		else {
			result = NotesNativeAPI32.get().DXLExportNote(m_hExporter, callback, note.getHandle32(), (Pointer) null);
		}
		NotesErrorUtils.checkResult(result);
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of note "+note+" in database "+
					note.getParent().getServer()+"!!"+note.getParent().getRelativeFilePath(), ex[0]);
		}
	}

	/**
	 * Export an IDTable of notes into XML format.
	 * 
	 * @param db database containing the export ids
	 * @param ids ids to export
	 * @param out result writer
	 * @throws IOException in case of I/O errors
	 */
	public void exportIDs(NotesDatabase db, Collection<Integer> ids, Writer out) throws IOException {
		NotesIDTable idTable = new NotesIDTable(ids);
		try {
			exportIDTable(db, idTable, out);
		}
		finally {
			idTable.recycle();
		}
	}
	
	/**
	 * Export an IDTable of notes into XML format.
	 * 
	 * @param db database containing the export ids
	 * @param idTable IDTable to export
	 * @param out result writer
	 * @throws IOException in case of I/O errors
	 */
	public void exportIDTable(NotesDatabase db, NotesIDTable idTable, Writer out) throws IOException {
		WriterOutputStream outStream = new WriterOutputStream(out, Charset.forName("UTF-8"));
		exportIDTable(db, idTable, outStream);
		outStream.flush();
	}
	
	/**
	 * Export an IDTable of notes into XML format.
	 * 
	 * @param db database containing the export ids
	 * @param idTable IDTable to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportIDTable(NotesDatabase db, NotesIDTable idTable, final OutputStream out) throws IOException {
		checkHandle();
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is recycled");
		}
		if (idTable.isRecycled()) {
			throw new NotesError(0, "IDTable is recycled");
		}
		
		NotesCallbacks.XML_WRITE_FUNCTION callback;
		
		final Exception[] ex = new Exception[1];
		
		if (PlatformUtils.isWin32()) {
			callback = new Win32NotesCallbacks.XML_WRITE_FUNCTIONWin32() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		else {
			callback = new NotesCallbacks.XML_WRITE_FUNCTION() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().DXLExportIDTable(m_hExporter, callback, db.getHandle64(), idTable.getHandle64(), (Pointer) null);
		}
		else {
			result = NotesNativeAPI32.get().DXLExportIDTable(m_hExporter, callback, db.getHandle32(), idTable.getHandle32(), (Pointer) null);
		}
		NotesErrorUtils.checkResult(result);
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of "+idTable+" in database "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
	}
	
	/**
	 * Export an entire database in XML format.
	 * 
	 * @param db database to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportDatabase(NotesDatabase db, final OutputStream out) throws IOException {
		checkHandle();
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is recycled");
		}
		
		NotesCallbacks.XML_WRITE_FUNCTION callback;
		
		final Exception[] ex = new Exception[1];
		
		if (PlatformUtils.isWin32()) {
			callback = new Win32NotesCallbacks.XML_WRITE_FUNCTIONWin32() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		else {
			callback = new NotesCallbacks.XML_WRITE_FUNCTION() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().DXLExportDatabase(m_hExporter, callback, db.getHandle64(), (Pointer) null);
		}
		else {
			result = NotesNativeAPI32.get().DXLExportDatabase(m_hExporter, callback, db.getHandle32(), (Pointer) null);
		}
		NotesErrorUtils.checkResult(result);
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of database "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
	}
	
	/**
	 * Export the ACL of the specified database in XML format.
	 * 
	 * @param db database to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportACL(NotesDatabase db, final OutputStream out) throws IOException {
		checkHandle();
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is recycled");
		}
		
		NotesCallbacks.XML_WRITE_FUNCTION callback;
		
		final Exception[] ex = new Exception[1];
		
		if (PlatformUtils.isWin32()) {
			callback = new Win32NotesCallbacks.XML_WRITE_FUNCTIONWin32() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		else {
			callback = new NotesCallbacks.XML_WRITE_FUNCTION() {

				@Override
				public void invoke(Pointer bBuffer, int length, Pointer pAction) {
					if (ex[0] == null && length>0) {
						try {
							byte[] data = bBuffer.getByteArray(0, length);
							out.write(data);
						}
						catch (Exception t) {
							ex[0] = t;
						}
					}
				}
			};
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().DXLExportACL(m_hExporter, callback, db.getHandle64(), (Pointer) null);
		}
		else {
			result = NotesNativeAPI32.get().DXLExportACL(m_hExporter, callback, db.getHandle32(), (Pointer) null);
		}
		NotesErrorUtils.checkResult(result);
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of database ACL for "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
	}
	
	private void tbd(Session session) throws NotesException {
		//TODO
		
		session.createDxlExporter().getAttachmentOmittedText();
		session.createDxlExporter().getConvertNotesBitmapsToGIF();
		session.createDxlExporter().getDoctypeSYSTEM();
		session.createDxlExporter().getExitOnFirstFatalError();
		session.createDxlExporter().getForceNoteFormat();
		session.createDxlExporter().getLog();
		session.createDxlExporter().getLogComment();
		session.createDxlExporter().getMIMEOption();
		session.createDxlExporter().getOLEObjectOmittedText();
		session.createDxlExporter().getOmitItemNames();
		session.createDxlExporter().getOmitMiscFileObjects();
		session.createDxlExporter().getOmitOLEObjects();
		session.createDxlExporter().getOmitRichtextAttachments();
		session.createDxlExporter().getOmitRichtextPictures();
		session.createDxlExporter().getOutputDOCTYPE();
		session.createDxlExporter().getPictureOmittedText();
		session.createDxlExporter().getRestrictToItemNames();
		session.createDxlExporter().getRichTextOption();
		session.createDxlExporter().getUncompressAttachments();
	}
	
	private void setBooleanProperty(short index, boolean value) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(Native.BOOL_SIZE);
		try {
			m.setByte(0, (byte) (value ? 1 : 0));
			short result = NotesNativeAPI.get().DXLSetExporterProperty(m_hExporter, index, m);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			m.dispose();
		}

	}
	
	private boolean getBooleanProperty(short index) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(2);
		try {
			short result = NotesNativeAPI.get().DXLGetExporterProperty(m_hExporter, index, m);
			NotesErrorUtils.checkResult(result);
			short boolAsShort = m.getShort(0);
			return boolAsShort != 0;
		}
		finally {
			m.dispose();
		}
	}
	
	private void setStringProperty(short index, String str) {
		checkHandle();
		
		if (str==null) {
			str = "";
		}
		
		Memory strAsLMBCs = NotesStringUtils.toLMBCS(str, true);
		short result = NotesNativeAPI.get().DXLSetExporterProperty(m_hExporter, index, strAsLMBCs);
		NotesErrorUtils.checkResult(result);
	}
	
	private String getStringFromMemhandle(short index) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(4);
		try {
			short result = NotesNativeAPI.get().DXLGetExporterProperty(m_hExporter, index, m);
			NotesErrorUtils.checkResult(result);
			
			int memHandle = m.getInt(0);
			if (memHandle==0) {
				return "";
			}
			
			if (PlatformUtils.is64Bit()) {
				Pointer ptr = Mem64.OSMemoryLock(memHandle);
				try {
					String str = NotesStringUtils.fromLMBCS(ptr, -1);
					return str;
				}
				finally {
					Mem64.OSMemoryUnlock(memHandle);
					Mem64.OSMemoryFree(memHandle);
				}
			}
			else {
				Pointer ptr = Mem32.OSMemoryLock(memHandle);
				try {
					String str = NotesStringUtils.fromLMBCS(ptr, -1);
					return str;
				}
				finally {
					Mem32.OSMemoryUnlock(memHandle);
					Mem32.OSMemoryFree(memHandle);
				}
			}
		}
		finally {
			m.dispose();
		}
	}
	
	private void setStringList(short index, List<String> values) {
		LMBCSStringList lmbcsStrList = new LMBCSStringList(values, false);
		try {
			if (PlatformUtils.is64Bit()) {
				DisposableMemory m = new DisposableMemory(8);
				try {
					m.setLong(0, lmbcsStrList.getHandle64());
					short result = NotesNativeAPI.get().DXLSetExporterProperty(m_hExporter, index, m);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					m.dispose();
				}
			}
			else {
				DisposableMemory m = new DisposableMemory(4);
				try {
					m.setInt(0, lmbcsStrList.getHandle32());
					short result = NotesNativeAPI.get().DXLSetExporterProperty(m_hExporter, index, m);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					m.dispose();
				}
			}
		}
		finally {
			lmbcsStrList.free();
		}
	}
	
	public List<String> getStringList(short index) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(4);
		try {
			short result = NotesNativeAPI.get().DXLGetExporterProperty(m_hExporter, index, m);
			NotesErrorUtils.checkResult(result);
			
			int handle = m.getInt(0);
			if (handle==0) {
				return Collections.emptyList();
			}
			
			if (PlatformUtils.is64Bit()) {
				Pointer pList = Mem64.OSLockObject(handle);
				try {
					@SuppressWarnings("rawtypes")
					List list = ItemDecoder.decodeTextListValue(pList, false);
					return list;
				}
				finally {
					Mem64.OSUnlockObject(handle);
					result = Mem64.OSMemFree(handle);
					NotesErrorUtils.checkResult(result);
				}
			}
			else {
				Pointer pList = Mem32.OSLockObject(handle);
				try {
					@SuppressWarnings("rawtypes")
					List list = ItemDecoder.decodeTextListValue(pList, false);
					return list;
				}
				finally {
					Mem32.OSUnlockObject(handle);
					result = Mem32.OSMemFree(handle);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		finally {
			m.dispose();
		}
	}
	
	public boolean isOutputXmlDecl() {
		return getBooleanProperty(NotesConstants.eOutputXmlDecl);
	}
	
	public void setOutputXmlDecl(boolean b) {
		setBooleanProperty(NotesConstants.eOutputXmlDecl, b);
	}
	
	public boolean isOutputDoctype() {
		return getBooleanProperty(NotesConstants.eOutputDOCTYPE);
	}
	
	public void setOutputDoctype(boolean b) {
		setBooleanProperty(NotesConstants.eOutputDOCTYPE, b);
	}
	
	public boolean isConvertNotesbitmapsToGIF() {
		return getBooleanProperty(NotesConstants.eConvertNotesbitmapsToGIF);
	}
	
	public void setConvertNotesbitmapsToGIF(boolean b) {
		setBooleanProperty(NotesConstants.eConvertNotesbitmapsToGIF, b);
	}
	
	public boolean isOmitRichtextAttachments() {
		return getBooleanProperty(NotesConstants.eOmitRichtextAttachments);
	}
	
	public void setOmitRichtextAttachments(boolean b) {
		setBooleanProperty(NotesConstants.eOmitRichtextAttachments, b);
	}
	
	public boolean isOmitOLEObjects() {
		return getBooleanProperty(NotesConstants.eOmitOLEObjects);
	}
	
	public void setOmitOLEObjects(boolean b) {
		setBooleanProperty(NotesConstants.eOmitOLEObjects, b);
	}
	
	public boolean isOmitMiscFileObjects() {
		return getBooleanProperty(NotesConstants.eOmitMiscFileObjects);
	}
	
	public void setOmitMiscFileObjects(boolean b) {
		setBooleanProperty(NotesConstants.eOmitMiscFileObjects, b);
	}
	
	public boolean isOmitPictures() {
		return getBooleanProperty(NotesConstants.eOmitPictures);
	}
	
	public void setOmitPictures(boolean b) {
		setBooleanProperty(NotesConstants.eOmitPictures, b);
	}
	
	public boolean isUncompressAttachments() {
		return getBooleanProperty(NotesConstants.eUncompressAttachments);
	}
	
	public void setUncompressAttachments(boolean b) {
		setBooleanProperty(NotesConstants.eUncompressAttachments, b);
	}
	
	public String getDxlExportResultLog() {
		return getStringFromMemhandle(NotesConstants.eDxlExportResultLog);
	}
	
	public String getDefaultDoctypeSYSTEM() {
		return getStringFromMemhandle(NotesConstants.eDefaultDoctypeSYSTEM);
	}
	
	public String getDoctypeSYSTEM() {
		return getStringFromMemhandle(NotesConstants.eDoctypeSYSTEM);
	}

	public void setDoctypeSYSTEM(String docType) {
		setStringProperty(NotesConstants.eDoctypeSYSTEM, docType);
	}
	
	public String getDXLBannerComments() {
		return getStringFromMemhandle(NotesConstants.eDXLBannerComments);
	}
	
	public void setDXLBannerComments(String comments) {
		setStringProperty(NotesConstants.eDXLBannerComments, comments);
	}
	
	public String getDxlExportResultLogComment() {
		return getStringFromMemhandle(NotesConstants.eDxlExportResultLogComment);
	}
	
	public void setDxlExportResultLogComment(String comment) {
		setStringProperty(NotesConstants.eDxlExportResultLogComment, comment);
	}
	
	public String getDxlDefaultSchemaLocation() {
		return getStringFromMemhandle(NotesConstants.eDxlDefaultSchemaLocation);
	}
	
	public String getDxlSchemaLocation() {
		return getStringFromMemhandle(NotesConstants.eDxlSchemaLocation);
	}
	
	public void setDxlSchemaLocation(String loc) {
		setStringProperty(NotesConstants.eDxlSchemaLocation, loc);
	}
	
	public String getAttachmentOmittedText() {
		return getStringFromMemhandle(NotesConstants.eAttachmentOmittedText);
	}
	
	public void setAttachmentOmittedText(String txt) {
		setStringProperty(NotesConstants.eAttachmentOmittedText, txt);
	}
	
	public String getOLEObjectOmittedText() {
		return getStringFromMemhandle(NotesConstants.eOLEObjectOmittedText);
	}
	
	public void setOLEObjectOmittedText(String txt) {
		setStringProperty(NotesConstants.eOLEObjectOmittedText, txt);
	}
	
	public String getPictureOmittedText() {
		return getStringFromMemhandle(NotesConstants.ePictureOmittedText);
	}
	
	public void setPictureOmittedText(String txt) {
		setStringProperty(NotesConstants.ePictureOmittedText, txt);
	}
	
	public List<String> getOmitItemNames() {
		return getStringList(NotesConstants.eOmitItemNames);
	}
	
	public void setOmitItemNames(List<String> itemNames) {
		setStringList(NotesConstants.eOmitItemNames, itemNames);
	}
	
	public List<String> getRestrictToItemNames() {
		return getStringList(NotesConstants.eRestrictToItemNames);
	}
	
	public void setRestrictToItemNames(List<String> itemNames) {
		setStringList(NotesConstants.eRestrictToItemNames, itemNames);
	}
	
}
