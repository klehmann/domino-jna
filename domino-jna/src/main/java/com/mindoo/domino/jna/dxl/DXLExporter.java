package com.mindoo.domino.jna.dxl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.WriterOutputStream;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * DXL Exporter<br>
 * <br>
 * <b>Please make sure that you include the file jvm/lib/ext/websvc.jar of the Notes Client / Domino server
 * directory to the Java classpath if your code is not running within Notes/Domino,
 * but in a standalone application.<br>
 * <br>
 * Otherwise you might experience crashes during DXL export (we had crashes when testing the DB export).<br>
 * </b><br>
 * <br>
 * Default values are set as follows:<br>
 * <br>
 * Note:	(i) = can input new value into the exporter.<br>
 * (o) = can get current value out of exporter.<br>
 * (io) = can do both.<br>
 * <br>
 * eDxlExportResultLog = (o) NULLMEMHANDLE<br>
 * eDefaultDoctypeSYSTEM = (o) default filename of dtd keyed to current version of DXL exporter.<br>
 * eDoctypeSYSTEM = (io) filename of dtd keyed to current version of DXL exporter.<br>
 * eDXLBannerComments = (io) NULLMEMHANDLE<br>
 * eDxlExportCharset = (io) {@link DXLExportCharset#UTF8}<br>
 * eDxlRichtextOption = (io) {@link DXLRichtextOption#DXL}<br>
 * eDxlExportResultLogComment = (io) NULLMEMHANDLE<br>
 * eForceNoteFormat = (io) FALSE<br>
 * eExitOnFirstFatalError = (io) TRUE<br>
 * eOutputRootAttrs = (io) TRUE<br>
 * eOutputXmlDecl = (io) TRUE<br>
 * eOutputDOCTYPE = (io) TRUE<br>
 * eConvertNotesbitmapToGIF	= (io) FALSE<br>
 * eDxlValidationStyle = (io) {@link DXLValidationStyle#DTD}<br>
 * eDxlDefaultSchemaLocation = (o) URI's of schema keyed to current version of DLX exporter.<br>
 * eDxlSchemaLocation = (io) filename of XML Schema keyed to current version of DXL exporter.<br>
 * 
 * @author Karsten Lehmann
 */
public class DXLExporter extends AbstractDXLTransfer implements IAllocatedMemory {
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
	
	@Override
	protected void checkHandle() {
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
		WriterOutputStream outStream = new WriterOutputStream(out, getJDKExportCharset());
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
	public void exportNote(final NotesNote note, final OutputStream out) throws IOException {
		checkHandle();
		if (note.isRecycled()) {
			throw new NotesError(0, "Note is recycled");
		}
		
		final NotesCallbacks.XML_WRITE_FUNCTION callback;
		
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
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI64.get().DXLExportNote(m_hExporter, callback, note.getHandle64(), (Pointer) null);
				}
			});
		}
		else {
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI32.get().DXLExportNote(m_hExporter, callback, note.getHandle32(), (Pointer) null);
				}
			});
		}
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of note "+note+" in database "+
					note.getParent().getServer()+"!!"+note.getParent().getRelativeFilePath(), ex[0]);
		}
		
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Export a set of note ids into XML format.
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
	 * Export a set of note ids into XML format.
	 * 
	 * @param db database containing the export ids
	 * @param ids ids to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportIDs(NotesDatabase db, Collection<Integer> ids, OutputStream out) throws IOException {
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
		WriterOutputStream outStream = new WriterOutputStream(out, getJDKExportCharset());
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
	public void exportIDTable(final NotesDatabase db, final NotesIDTable idTable, final OutputStream out) throws IOException {
		checkHandle();
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is recycled");
		}
		if (idTable.isRecycled()) {
			throw new NotesError(0, "IDTable is recycled");
		}
		
		final NotesCallbacks.XML_WRITE_FUNCTION callback;
		
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
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI64.get().DXLExportIDTable(m_hExporter, callback, db.getHandle64(), idTable.getHandle64(), (Pointer) null);
				}
			});
		}
		else {
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI32.get().DXLExportIDTable(m_hExporter, callback, db.getHandle32(), idTable.getHandle32(), (Pointer) null);
				}
			});
		}
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of "+idTable+" in database "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
		
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Export an entire database in XML format.
	 * 
	 * @param db database to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportDatabase(final NotesDatabase db, final OutputStream out) throws IOException {
		checkHandle();
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is recycled");
		}
		
		final NotesCallbacks.XML_WRITE_FUNCTION callback;
		
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
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI64.get().DXLExportDatabase(m_hExporter, callback, db.getHandle64(), (Pointer) null);
				}
			});
		}
		else {
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI32.get().DXLExportDatabase(m_hExporter, callback, db.getHandle32(), (Pointer) null);
				}
			});
		}
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of database "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
		
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Export the ACL of the specified database in XML format.
	 * 
	 * @param db database to export
	 * @param out result stream
	 * @throws IOException in case of I/O errors
	 */
	public void exportACL(final NotesDatabase db, final OutputStream out) throws IOException {
		checkHandle();
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is recycled");
		}
		
		final NotesCallbacks.XML_WRITE_FUNCTION callback;
		
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
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI64.get().DXLExportACL(m_hExporter, callback, db.getHandle64(), (Pointer) null);
				}
			});
		}
		else {
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI32.get().DXLExportACL(m_hExporter, callback, db.getHandle32(), (Pointer) null);
				}
			});
		}
		
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0]!=null) {
			throw new NotesError(0, "Error during DXL export of database ACL for "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
		
		NotesErrorUtils.checkResult(result);
	}
	
	@Override
	protected short getProperty(int index, Memory m) {
		return NotesNativeAPI.get().DXLGetExporterProperty(m_hExporter, index, m);
	}
	
	@Override
	protected short setProperty(int index, Memory m) {
		return NotesNativeAPI.get().DXLSetExporterProperty(m_hExporter, index, m);
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
	
	/**
	 * List of item names to omit from DXL
	 * 
	 * @return item names
	 */
	public List<String> getOmitItemNames() {
		return getStringList(NotesConstants.eOmitItemNames);
	}
	
	/**
	 * List of item names to omit from DXL
	 * 
	 * @param itemNames new item names
	 */
	public void setOmitItemNames(List<String> itemNames) {
		setStringList(NotesConstants.eOmitItemNames, itemNames);
	}
	
	/**
	 * List of item names; only items with one of these names will be included in the output DXL
	 * 
	 * @return item names
	 */
	public List<String> getRestrictToItemNames() {
		return getStringList(NotesConstants.eRestrictToItemNames);
	}
	
	/**
	 * List of item names; only items with one of these names will be included in the output DXL
	 * 
	 * @param itemNames item names
	 */
	public void setRestrictToItemNames(List<String> itemNames) {
		setStringList(NotesConstants.eRestrictToItemNames, itemNames);
	}

	/** Specifies output charset */
	public static enum DXLExportCharset {
		/** (default) "encoding =" attribute is set to utf8 and output charset is utf8 */
		UTF8,
		/** "encoding =" attribute is set to utf16 and charset is utf16 */
		UTF16
	}

	/**
	 * Returns the output charset
	 * 
	 * @return charset
	 */
	public DXLExportCharset getExportCharset() {
		int charsetAsInt = getInt(NotesConstants.eDxlExportCharset);
		if (charsetAsInt==NotesConstants.DXL_EXPORT_CHARSET_eDxlExportUtf8) {
			return DXLExportCharset.UTF8;
		}
		else if (charsetAsInt==NotesConstants.DXL_EXPORT_CHARSET_eDxlExportUtf16) {
			return DXLExportCharset.UTF16;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Returns the output charset as a {@link Charset}
	 * 
	 * @return charset
	 */
	public Charset getJDKExportCharset() {
		DXLExportCharset charset = getExportCharset();
		if (charset==DXLExportCharset.UTF16) {
			return Charset.forName("UTF-16");
		}
		else {
			return Charset.forName("UTF-8");
		}
	}
	
	/**
	 * Sets the output charset
	 * 
	 * @param charset new charset
	 */
	public void setExportCharset(DXLExportCharset charset) {
		int charsetAsInt;
		if (charset==DXLExportCharset.UTF8) {
			charsetAsInt = NotesConstants.DXL_EXPORT_CHARSET_eDxlExportUtf8;
		}
		else if (charset==DXLExportCharset.UTF16) {
			charsetAsInt = NotesConstants.DXL_EXPORT_CHARSET_eDxlExportUtf16;
		}
		else {
			throw new IllegalArgumentException("Unsupported charset value: "+charset+". Only supported: "+DXLExportCharset.values());
		}
		
		setInt(NotesConstants.eDxlExportCharset, charsetAsInt);
	}

	/** Specifies rule for exporting richtext */
	public static enum DXLRichtextOption {
		/** (default) output richtext as dxl with warning 
		   comments if uninterpretable CD records */
		DXL,
		/** output richtext as uninterpretted (base64'ed) item data */
		ITEMDATA
	}

	/**
	 * Returns the rule for exporting richtext
	 * 
	 * @return richtext option
	 */
	public DXLRichtextOption getRichtextOption() {
		int rtOption = getInt(NotesConstants.eDxlRichtextOption);
		if (rtOption == NotesConstants.DXL_RICHTEXT_OPTION_eRichtextAsDxl) {
			return DXLRichtextOption.DXL;
		}
		else if (rtOption == NotesConstants.DXL_RICHTEXT_OPTION_eRichtextAsItemdata) {
			return DXLRichtextOption.ITEMDATA;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Specifies rule for exporting richtext
	 * 
	 * @param option richtext option
	 */
	public void setRichtextOption(DXLRichtextOption option) {
		int rtOptionAsInt;
		switch (option) {
		case DXL:
			rtOptionAsInt = NotesConstants.DXL_RICHTEXT_OPTION_eRichtextAsDxl;
			break;
		case ITEMDATA:
			rtOptionAsInt = NotesConstants.DXL_RICHTEXT_OPTION_eRichtextAsItemdata;
			break;
			default:
				throw new IllegalArgumentException("Unknown richtext option: "+option+". Only supported: "+DXLRichtextOption.values());
		}
		
		setInt(NotesConstants.eDxlRichtextOption, rtOptionAsInt);
	}
	
	/** Specifies style of validation info emitted by exporter. Can override other settings, eg - output doctype */
	public static enum DXLValidationStyle { NONE, DTD, XMLSCHEMA }
	
	/**
	 * Returns the style of validation info emitted by exporter. Can override other settings, eg - output doctype
	 * 
	 * @return style
	 */
	public DXLValidationStyle getValidationStyle() {
		int styleAsInt = getInt(NotesConstants.eDxlValidationStyle);
		
		if (styleAsInt == NotesConstants.DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_None) {
			return DXLValidationStyle.NONE;
		}
		else if (styleAsInt == NotesConstants.DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_DTD) {
			return DXLValidationStyle.DTD;
		}
		else if (styleAsInt == NotesConstants.DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_XMLSchema) {
			return DXLValidationStyle.XMLSCHEMA;
		}
		else {
			return null;
		}
	}

	/**
	 * Specifies style of validation info emitted by exporter. Can override other settings, eg - output doctype
	 * 
	 * @param style style
	 */
	public void setValidationStyle(DXLValidationStyle style) {
		int styleAsInt;

		switch (style) {
		case NONE:
			styleAsInt = NotesConstants.DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_None;
			break;
		case DTD:
			styleAsInt = NotesConstants.DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_DTD;
			break;
		case XMLSCHEMA:
			styleAsInt = NotesConstants.DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_XMLSchema;
			break;
		default:
			throw new IllegalArgumentException("Unknown validation style: "+style+". Only supported: "+DXLValidationStyle.values());
		}

		setInt(NotesConstants.eDxlValidationStyle, styleAsInt);
	}

	/** Specifies rule for exporting native MIME */
	public static enum DXLMIMEOption {
		/** (default) output native MIME within &lt;mime&gt; element in DXL */
		DXL,
		/** output MIME as uninterpretted (base64'ed) item data */
		ITEMDATA }
	
	/**
	 * Returns the rule for exporting native MIME
	 * 
	 * @return MIME option
	 */
	public DXLMIMEOption getMIMEOption() {
		int mimeOptionAsInt = getInt(NotesConstants.eDxlMimeOption);
		
		if (mimeOptionAsInt == NotesConstants.DXL_MIME_OPTION_eMimeAsDxl) {
			return DXLMIMEOption.DXL;
		}
		else if (mimeOptionAsInt == NotesConstants.DXL_MIME_OPTION_eMimeAsItemdata) {
			return DXLMIMEOption.ITEMDATA;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Specifies rule for exporting native MIME
	 * 
	 * @param option MIME option
	 */
	public void setMIMEOption(DXLMIMEOption option) {
		int mimeOptionAsInt;

		switch (option) {
		case DXL:
			mimeOptionAsInt = NotesConstants.DXL_MIME_OPTION_eMimeAsDxl;
			break;
		case ITEMDATA:
			mimeOptionAsInt = NotesConstants.DXL_MIME_OPTION_eMimeAsItemdata;
			break;
		default:
			throw new IllegalArgumentException("Unknown MIME option: "+option+". Only supported: "+DXLMIMEOption.values());
		}

		setInt(NotesConstants.eDxlMimeOption, mimeOptionAsInt);
	}
}
