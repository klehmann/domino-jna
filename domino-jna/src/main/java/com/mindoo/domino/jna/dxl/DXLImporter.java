package com.mindoo.domino.jna.dxl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.tools.ant.util.TeeOutputStream;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.dxl.DXLExporter.DXLExportCharset;
import com.mindoo.domino.jna.dxl.DXLExporter.DXLMIMEOption;
import com.mindoo.domino.jna.dxl.DXLExporter.DXLRichtextOption;
import com.mindoo.domino.jna.dxl.DXLExporter.DXLValidationStyle;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.ReaderInputStream;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import lotus.domino.NotesException;
import lotus.domino.NotesThread;

/**
 * DXL importer<br>
 * <br>
 * Default values are set as follows:<br>
 * <br>
 * Note:	(i) = can input new value into the importer.<br>
 * (o) = can get current value out of importer.<br>
 * (io) = can do both.<br>
 * <br>
 * iACLImportOption	= (io) {@link DXLImportOption#IGNORE}<br>
 * iDesignImportOption = (io) {@link DXLImportOption#IGNORE}<br>
 * 	iDocumentsImportOption = (io) {@link DXLImportOption#CREATE}<br>
 * iCreateFullTextIndex = (io) FALSE<br>
 * Note:<br> To create a Full Text Index on a database, the iCreateFullTextIndex must be set to TRUE,
 * the iReplaceDbProperties must be set to TRUE and a schema element named
 * &lt;fulltextsettings&gt; must be defined.<br>
 * <br>
 * iReplaceDbProperties = (io) FALSE<br>
 * iInputValidationOption = (io) {@link XMLValidationOption#AUTO}<br>
 * iReplicaRequiredForReplaceOrUpdate = (io) TRUE<br>
 * iExitOnFirstFatalError = (io) TRUE<br>
 * iUnknownTokenLogOption = (io) {@link DXLLogOption#FATALERROR}<br>
 * iResultLogComment = (io) NULLMEMHANDLE<br>
 * iResultLog = (o) NULLMEMHANDLE<br>
 * iImportedNoteList = (o) NULLHANDLE
 * 
 * @author Karsten Lehmann
 */
public class DXLImporter extends AbstractDXLTransfer implements IAllocatedMemory {
	private int m_hImporter;

	public DXLImporter() {
		IntByReference rethDXLExport = new IntByReference();
		short result = NotesNativeAPI.get().DXLCreateImporter(rethDXLExport);
		NotesErrorUtils.checkResult(result);
		m_hImporter = rethDXLExport.getValue();
		if (m_hImporter==0) {
			throw new NotesError(0, "Failed to allocate DXL importer");
		}
		NotesGC.__memoryAllocated(this);
	}

	@Override
	public void free() {
		if (isFreed()) {
			return;
		}

		if (m_hImporter!=0) {
			NotesNativeAPI.get().DXLDeleteImporter(m_hImporter);
			m_hImporter = 0;
		}
	}

	@Override
	public boolean isFreed() {
		return m_hImporter==0;
	}

	@Override
	public int getHandle32() {
		return m_hImporter;
	}

	@Override
	public long getHandle64() {
		return m_hImporter;
	}

	@Override
	protected void checkHandle() {
		if (m_hImporter==0)
			throw new NotesError(0, "DXL importer already freed");

		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidMemHandle(DXLImporter.class, m_hImporter);
		}
		else {
			NotesGC.__b32_checkValidMemHandle(DXLImporter.class, m_hImporter);
		}
	}

	public boolean importErrorWasLogged() {
		checkHandle();

		short logged = NotesNativeAPI.get().DXLImportWasErrorLogged(m_hImporter);
		return logged == 1;
	}

	@Override
	protected short getProperty(int index, Memory m) {
		return NotesNativeAPI.get().DXLGetImporterProperty(m_hImporter, index, m);
	}

	@Override
	protected short setProperty(int index, Memory m) {
		return NotesNativeAPI.get().DXLSetImporterProperty(m_hImporter, index, m);
	}

	public static enum DXLImportOption {
		/** ignore imported data */
		IGNORE(1),
		/** create new data from imported data */
		CREATE(2),
		/** if imported data matches existing data, ignore the imported data, otherwise create it */
		IGNORE_ELSE_CREATE(3),
		/** do not used - reserved for future variation of create option */
		CREATE_RESERVED2(4),
		/** if imported data matches existing data, then replace existing data with imported data, else ignore imported data. */
		REPLACE_ELSE_IGNORE(5),
		/** if imported data matches existing data, then replace existing data with imported data, else create
		 * new data from imported data */
		REPLACE_ELSE_CREATE(6),
		/** do not used - reserved for future variation of replace option */
		REPLACE_RESERVED1(7),
		/** do not used - reserved for future variation of replace option */
		REPLACE_RESERVED2(8),
		/** if imported data matches existing data, then update existing data with imported data, else ignore imported data. */
		UPDATE_ELSE_IGNORE(9),
		/** if imported data matches existing data, then update existing data with imported data, else create
		 * new data from imported data */
		UPDATE_ELSE_CREATE(10),
		/** do not used - reserved for future variation of update option */
		UPDATE_RESERVED1(11),
		/** do not used - reserved for future variation of update option */
		UPDATE_RESERVED2(12);

		private int m_option;

		private DXLImportOption(int option) {
			m_option = option;
		}

		public int getValue() {
			return m_option;
		}
	}

	public static enum DXLLogOption {
		/** ignore the action. don't log anything and just continue */
		IGNORE(1),
		/** log the problem as a warning */
		WARNING(2),
		/** log the problem as an error */
		ERROR(3),
		/** log the problem as a fatal error */
		FATALERROR(4);

		private int m_option;

		private DXLLogOption(int option) {
			m_option = option;
		}

		public int getValue() {
			return m_option;
		}
	}
	
	public static enum XMLValidationOption {
		NEVER(0),
		ALWAYS(1),
		AUTO(2);
		
		private int m_option;
		
		private XMLValidationOption(int option) {
			m_option = option;
		}
		
		public int getValue() {
			return m_option;
		}
	}
	
	/**
	 * Returns the import option for the ACL
	 * 
	 * @return option or null if unknown value
	 */
	public DXLImportOption getACLImportOption() {
		short optionAsShort = getShort(NotesConstants.iACLImportOption);
		int optionAsInt = (int) (optionAsShort & 0xffff);
		
		for (DXLImportOption currOpt : DXLImportOption.values()) {
			if (currOpt.getValue() == optionAsInt) {
				return currOpt;
			}
		}
		return null;
	}
	
	/**
	 * Sets the import option for the ACL
	 * 
	 * @param option option
	 */
	public void setACLImportOption(DXLImportOption option) {
		int optionAsInt = option.getValue();
		short optionAsShort = (short) (optionAsInt & 0xffff);
		
		setShort(NotesConstants.iACLImportOption, optionAsShort);
	}
	
	/**
	 * Returns the import option for design
	 * 
	 * @return option or null if unknown value
	 */
	public DXLImportOption getDesignImportOption() {
		short optionAsShort = getShort(NotesConstants.iDesignImportOption);
		int optionAsInt = (int) (optionAsShort & 0xffff);
		
		for (DXLImportOption currOpt : DXLImportOption.values()) {
			if (currOpt.getValue() == optionAsInt) {
				return currOpt;
			}
		}
		return null;
	}
	
	/**
	 * Sets the import option for design
	 * 
	 * @param option option
	 */
	public void setDesignImportOption(DXLImportOption option) {
		int optionAsInt = option.getValue();
		short optionAsShort = (short) (optionAsInt & 0xffff);
		
		setShort(NotesConstants.iDesignImportOption, optionAsShort);
	}
	
	/**
	 * Returns the import option for data documents
	 * 
	 * @return option or null if unknown value
	 */
	public DXLImportOption getDocumentsImportOption() {
		short optionAsShort = getShort(NotesConstants.iDocumentsImportOption);
		int optionAsInt = (int) (optionAsShort & 0xffff);
		
		for (DXLImportOption currOpt : DXLImportOption.values()) {
			if (currOpt.getValue() == optionAsInt) {
				return currOpt;
			}
		}
		return null;
	}
	
	/**
	 * Sets the import option for data documents
	 * 
	 * @param option option
	 */
	public void setDocumentsImportOption(DXLImportOption option) {
		int optionAsInt = option.getValue();
		short optionAsShort = (short) (optionAsInt & 0xffff);
		
		setShort(NotesConstants.iDocumentsImportOption, optionAsShort);
	}
	
	/**
	 * Returns whether to create a fulltext index
	 * 
	 * @return BOOL, TRUE = create full text index, FALSE Do NOT create full text index
	 */
	public boolean isCreateFullTextIndex() {
		return getBooleanProperty(NotesConstants.iCreateFullTextIndex);
	}
	
	/**
	 * Sets whether to create a fulltext index
	 * @param b BOOL, TRUE = create full text index, FALSE Do NOT create full text index
	 */
	public void setCreateFullTextIndex(boolean b) {
		setBooleanProperty(NotesConstants.iCreateFullTextIndex, b);
	}
	
	/**
	 * Returns whether to replace database properties
	 * @return BOOL, TRUE = replace database properties, FALSE Do NOT replace database propertie
	 */
	public boolean isReplaceDbProperties() {
		return getBooleanProperty(NotesConstants.iReplaceDbProperties);
	}
	
	/**
	 * Sets whether to replace database properties
	 * 
	 * @param b BOOL, TRUE = replace database properties, FALSE Do NOT replace database propertie
	 */
	public void setReplaceDbProperties(boolean b) {
		setBooleanProperty(NotesConstants.iReplaceDbProperties, b);
	}
	
	/**
	 * Returns whether the input data should bbe validated
	 * 
	 * @return option or null if unknown value
	 */
	public XMLValidationOption getInputValidationOption() {
		int optionAsInt = getInt(NotesConstants.iInputValidationOption);
		for (XMLValidationOption currOpt : XMLValidationOption.values()) {
			if (currOpt.getValue() == optionAsInt) {
				return currOpt;
			}
		}
		return null;
	}
	
	/**
	 * Defines whether the input data should bbe validated
	 * 
	 * @param option validation option
	 */
	public void setInputValidationOption(XMLValidationOption option) {
		int optionAsInt = option.getValue();
		setInt(NotesConstants.iInputValidationOption, optionAsInt);
	}
	
	/**
	 * Returns if replica id must match
	 * 
	 * @return TRUE = skip replace/update ops if target DB and import DXL do not have same replicaid's, FALSE = allow replace/update ops even if target DB and import DXL do not have same replicaid's
	 */
	public boolean isReplicaRequiredForReplaceOrUpdate() {
		return getBooleanProperty(NotesConstants.iReplicaRequiredForReplaceOrUpdate);
	}
	
	/**
	 * Sets if replica id must match
	 * 
	 * @param b TRUE = skip replace/update ops if target DB and import DXL do not have same replicaid's, FALSE = allow replace/update ops even if target DB and import DXL do not have same replicaid's
	 */
	public void setReplicaRequiredForReplaceOrUpdate(boolean b) {
		setBooleanProperty(NotesConstants.iReplicaRequiredForReplaceOrUpdate, b);
	}
	
	/**
	 * Returns the behavior when errors occur
	 * 
	 * @return TRUE = importer exits on first fatal error, FALSE = importer continues even if fatal error found
	 */
	public boolean isExitOnFirstFatalError() {
		return getBooleanProperty(NotesConstants.iExitOnFirstFatalError);
	}
	
	/**
	 * Sets the behavior when errors occur
	 * 
	 * @param b TRUE = importer exits on first fatal error, FALSE = importer continues even if fatal error found
	 */
	public void setExitOnFirstFatalError(boolean b) {
		setBooleanProperty(NotesConstants.iExitOnFirstFatalError, b);
	}
	
	/**
	 * Returns what to do if DXL contains an unknown element or attribute
	 * 
	 * @return log option or null if unknown value
	 */
	public DXLLogOption getUnknownTokenLogOption() {
		short optionAsShort = getShort(NotesConstants.iUnknownTokenLogOption);
		int optionAsInt = (int) (optionAsShort & 0xffff);
		
		for (DXLLogOption currOpt : DXLLogOption.values()) {
			if (optionAsInt == currOpt.getValue()) {
				return currOpt;
			}
		}
		return null;
	}
	
	/**
	 * Specifies what to do if DXL contains an unknown element or attribute
	 * 
	 * @param option log option
	 */
	public void setUnknownTokenLogOption(DXLLogOption option) {
		int optionAsInt = option.getValue();
		short optionAsShort = (short) (optionAsInt & 0xffff);
		
		setShort(NotesConstants.iUnknownTokenLogOption, optionAsShort);
	}
	
	/**
	 * Returns the string to be added as comment to top of result log
	 * 
	 * @return comment
	 */
	public String getResultLogComment() {
		return getStringFromMemhandle(NotesConstants.iResultLogComment);
	}
	
	/**
	 * Sets the string to be added as comment to top of result log
	 * 
	 * @param comment comment
	 */
	public void setResultLogComment(String comment) {
		setStringProperty(NotesConstants.iResultLogComment, comment);
	}
	
	/**
	 * Returns the result log from the last import
	 * 
	 * @return og
	 */
	public String getResultLog() {
		return getStringFromMemhandle(NotesConstants.iResultLog);
	}
	
	/**
	 * Returns a {@link NotesIDTable} listing the notes imported by the last import operation
	 * 
	 * @return id table (copy of the table returned by the C API call) or null
	 */
	public NotesIDTable getImportedNoteList() {
		return getIDTableFromHandle(NotesConstants.iImportedNoteList);
	}
	
	/**
	 * This function imports XML data into Domino Data based on the {@link DXLImportOption} options set.
	 * 
	 * @param dxl DXL to be imported
	 * @param db Domino database that is to be imported
	 * @throws IOException in case of I/O errors
	 */
	public void importDxl(String dxl, NotesDatabase db) throws IOException {
		importDxl(new StringReader(dxl), db);
	}
	
	/**
	 * This function imports XML data into Domino Data based on the {@link DXLImportOption} options set.
	 * 
	 * @param in reader returning the DXL
	 * @param db Domino database that is to be imported
	 * @throws IOException in case of I/O errors
	 */
	public void importDxl(Reader in, NotesDatabase db) throws IOException {
		importDxl(new ReaderInputStream(in, "UTF-8"), db);
	}
	
	/**
	 * This function imports XML data into Domino Data based on the {@link DXLImportOption} options set.
	 * 
	 * @param in input stream returning the DXL
	 * @param db Domino database that is to be imported
	 * @throws IOException in case of I/O errors
	 */
	public void importDxl(final InputStream in, final NotesDatabase db) throws IOException {
		checkHandle();
		
		final NotesCallbacks.XML_READ_FUNCTION callback;
		
		final Exception[] ex = new Exception[1];
		
		if (PlatformUtils.isWin32()) {
			callback = new Win32NotesCallbacks.XML_READ_FUNCTIONWin32() {

				@Override
				public int invoke(Pointer pBuffer, int length, Pointer pAction) {
					if (ex[0] == null) {
						byte[] buf = new byte[length];
						try {
							int readBytes = in.read(buf);
							if (readBytes>0) {
								pBuffer.write(0, buf, 0, readBytes);
							}
							return readBytes;
						} catch (Exception e) {
							ex[0] = e;
							return 0;
						}
					}
					else {
						return 0;
					}
				}
				
			};
		}
		else {
			callback = new NotesCallbacks.XML_READ_FUNCTION() {
				
				@Override
				public int invoke(Pointer pBuffer, int length, Pointer pAction) {
					if (ex[0] == null) {
						byte[] buf = new byte[length];
						try {
							int readBytes = in.read(buf);
							if (readBytes>0) {
								pBuffer.write(0, buf, 0, readBytes);
							}
							return readBytes;
						} catch (Exception e) {
							ex[0] = e;
							return 0;
						}
					}
					else {
						return 0;
					}
				}
			};
		}
		
		short result;
		
		if (PlatformUtils.is64Bit()) {
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI64.get().DXLImport(m_hImporter, callback, db.getHandle64(), null);
				}
			});
		}
		else {
			result = AccessController.doPrivileged(new PrivilegedAction<Short>() {

				@Override
				public Short run() {
					return NotesNativeAPI32.get().DXLImport(m_hImporter, callback, db.getHandle32(), null);
				}
			});
		}
		if (ex[0] instanceof IOException) {
			throw (IOException) ex[0];
		}
		else if (ex[0] != null) {
			throw new NotesError(0, "Error during DXL import into database "+
					db.getServer()+"!!"+db.getRelativeFilePath(), ex[0]);
		}
		
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Imports DXL of a source database by piping the data between an DXL producing thread and this {@link DXLImporter}
	 * 
	 * @param dbSource source database
	 * @param exporter DXL exporter used to read exporter properties and recreate a DXLExporter in the producer thread
	 * @param dbTarget target database
	 * @param debugExportOut optional {@link OutputStream} to mirror the generated DXL content for debugging purpose, or null
	 */
	public void importDxlFromDBExporter(final NotesDatabase dbSource, DXLExporter exporter, final NotesDatabase dbTarget,
			final OutputStream debugExportOut) {
		importDxlFromExporter(dbSource, (Collection<Integer>) null, false, exporter, dbTarget, debugExportOut);
	}

	/**
	 * Imports DXL of the source database ACL by piping the data between an DXL producing thread and this {@link DXLImporter}
	 * 
	 * @param dbSource source database
	 * @param exporter DXL exporter used to read exporter properties and recreate a DXLExporter in the producer thread
	 * @param dbTarget target database
	 * @param debugExportOut optional {@link OutputStream} to mirror the generated DXL content for debugging purpose, or null
	 */
	public void importDxlFromACLExporter(NotesDatabase dbSource, DXLExporter exporter, final NotesDatabase dbTarget,
			final OutputStream debugExportOut) {
		importDxlFromExporter(dbSource, (Collection<Integer>) null, true, exporter, dbTarget, debugExportOut);
	}

	/**
	 * Imports DXL of {@link NotesNote} objects in the source database by piping the data
	 * between an DXL producing thread and this {@link DXLImporter}
	 * 
	 * @param dbSource source database
	 * @param ids note ids to export
	 * @param exporter DXL exporter used to read exporter properties and recreate a DXLExporter in the producer thread
	 * @param dbTarget target database
	 * @param debugExportOut optional {@link OutputStream} to mirror the generated DXL content for debugging purpose, or null
	 */
	public void importDxlFromIDExporter(final NotesDatabase dbSource, final Collection<Integer> ids,
			DXLExporter exporter, final NotesDatabase dbTarget, final OutputStream debugExportOut) {
		importDxlFromExporter(dbSource, ids, false, exporter, dbTarget, debugExportOut);
	}

	/**
	 * Imports DXL by piping the data between an DXL producing thread and this {@link DXLImporter}
	 * 
	 * @param dbSource source database
	 * @param ids optional note ids to export
	 * @param onlyACL true to export the db ACL
	 * @param exporter DXL exporter used to read exporter properties and recreate a DXLExporter in the producer thread
	 * @param dbTarget target database
	 * @param debugExportOut optional {@link OutputStream} to mirror the generated DXL content for debugging purpose, or null
	 */
	private void importDxlFromExporter(final NotesDatabase dbSource, final Collection<Integer> ids, final boolean onlyACL,
			DXLExporter exporter, final NotesDatabase dbTarget, final OutputStream debugExportOut) {
		
		final Exception exporterEx[] = new Exception[1];
		
		final PipedOutputStream dxlPipedOut = new PipedOutputStream();
		PipedInputStream dxlPipedIn;
		try {
			dxlPipedIn = new PipedInputStream(dxlPipedOut) {
				@Override
				public synchronized int read() throws IOException {
					if (exporterEx[0] instanceof IOException) {
						throw (IOException) exporterEx[0];
					}
					else if (exporterEx[0]!=null) {
						throw new NotesError(0, "Error in DXL producer thread", exporterEx[0]);
					}
					else {
						return super.read();
					}
				}
				
				@Override
				public int read(byte[] b) throws IOException {
					if (exporterEx[0] instanceof IOException) {
						throw (IOException) exporterEx[0];
					}
					else if (exporterEx[0]!=null) {
						throw new NotesError(0, "Error in DXL producer thread", exporterEx[0]);
					}
					else {
						return super.read(b);
					}
				}
				
				@Override
				public synchronized int read(byte[] b, int off, int len) throws IOException {
					if (exporterEx[0] instanceof IOException) {
						throw (IOException) exporterEx[0];
					}
					else if (exporterEx[0]!=null) {
						throw new NotesError(0, "Error in DXL producer thread", exporterEx[0]);
					}
					else {
						return super.read(b, off, len);
					}
				}
			};
		} catch (IOException e2) {
			throw new NotesError(0, "Error creating PipedInputStream", e2);
		}

		final String attOmittedTxt = exporter.getAttachmentOmittedText();
		final String docTypeSYSTEM = exporter.getDoctypeSYSTEM();
		final String dxlBannerComments = exporter.getDXLBannerComments();
		final String dxlExportResultLogComment = exporter.getDxlExportResultLogComment();
		final String dxlSchemaLocation = exporter.getDxlSchemaLocation();
		final DXLExportCharset charset = exporter.getExportCharset();
		final DXLMIMEOption mimeOption = exporter.getMIMEOption();
		final String oleObjectOmittedTxt = exporter.getOLEObjectOmittedText();
		final List<String> omitItemNames = exporter.getOmitItemNames();
		final String pictureOmittedTxt = exporter.getPictureOmittedText();
		final List<String> restrictToItemNames = exporter.getRestrictToItemNames();
		final DXLRichtextOption rtOption = exporter.getRichtextOption();
		final DXLValidationStyle validationStyle = exporter.getValidationStyle();
		
		
		final AtomicReference<String> dxlExportResultLogInThread = new AtomicReference<String>();
		
		//use separate thread to connect DXL producer and consumer
		NotesThread producerThread = new NotesThread() {
			@Override
			public void runNotes() throws NotesException {
				final AtomicReference<OutputStream> dxlOut = new AtomicReference<OutputStream>();
				try {
					NotesGC.runWithAutoGC(new Callable<Object>() {

						@Override
						public Object call() throws Exception {
							//reopen source database in this thread
							NotesDatabase dbSourceInThread = dbSource.reopenDatabase();
							
							//mirror DXLExporter in this thread
							DXLExporter exporterInThread = new DXLExporter();
							if (attOmittedTxt!=null) {
								exporterInThread.setAttachmentOmittedText(attOmittedTxt);
							}
							if (docTypeSYSTEM!=null) {
								exporterInThread.setDoctypeSYSTEM(docTypeSYSTEM);
							}
							if (dxlBannerComments!=null) {
								exporterInThread.setDXLBannerComments(dxlBannerComments);
							}
							if (dxlExportResultLogComment!=null) {
								exporterInThread.setDxlExportResultLogComment(dxlExportResultLogComment);
							}
							if (dxlSchemaLocation!=null) {
								exporterInThread.setDxlSchemaLocation(dxlSchemaLocation);
							}
							if (charset!=null) {
								exporterInThread.setExportCharset(charset);
							}
							if (mimeOption!=null) {
								exporterInThread.setMIMEOption(mimeOption);
							}
							if (oleObjectOmittedTxt!=null) {
								exporterInThread.setOLEObjectOmittedText(oleObjectOmittedTxt);
							}
							if (omitItemNames!=null) {
								exporterInThread.setOmitItemNames(omitItemNames);
							}
							if (pictureOmittedTxt!=null) {
								exporterInThread.setPictureOmittedText(pictureOmittedTxt);
							}
							if (restrictToItemNames!=null) {
								exporterInThread.setRestrictToItemNames(restrictToItemNames);
							}
							if (rtOption!=null) {
								exporterInThread.setRichtextOption(rtOption);
							}
							if (validationStyle!=null) {
								exporterInThread.setValidationStyle(validationStyle);
							}
							
							if (debugExportOut!=null) {
								//support mirroring the DXL export data to another stream for debugging purpose
								dxlOut.set(new TeeOutputStream(debugExportOut, dxlPipedOut));
							}
							else {
								dxlOut.set(dxlPipedOut);
							}
							
							if (onlyACL) {
								exporterInThread.exportACL(dbSourceInThread, dxlOut.get());
							}
							else if (ids!=null) {
								exporterInThread.exportIDs(dbSourceInThread, ids, dxlOut.get());
							}
							else {
								exporterInThread.exportDatabase(dbSourceInThread, dxlOut.get());
							}
							dxlOut.get().flush();
							
							dxlExportResultLogInThread.set(exporterInThread.getDxlExportResultLog());

							return null;
						}
					});
				} catch (Exception e) {
					exporterEx[0] = e;
				}
				finally {
					if (dxlOut.get()!=null) {
						try {
							dxlOut.get().close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						dxlOut.set(null);
					}
				}
			}
		};
		producerThread.start();
		
		try {
			importDxl(dxlPipedIn, dbTarget);
		}
		catch (Exception e) {
			String exporterLog = dxlExportResultLogInThread.get();
			
			if (exporterEx[0] != null) {
				if (!StringUtil.isEmpty(exporterLog)) {
					throw new NotesError(0, "Error exporting DXL in producer thread from database "+dbSource.getServer()+"!!"+dbSource.getRelativeFilePath()+". Log: "+exporterLog, exporterEx[0]);
				}
				else {
					throw new NotesError(0, "Error exporting DXL in producer thread from database "+dbSource.getServer()+"!!"+dbSource.getRelativeFilePath(), exporterEx[0]);
				}
			}
			else {
				if (!StringUtil.isEmpty(exporterLog)) {
					throw new NotesError(0, "Error importing DXL into database "+dbTarget.getServer()+"!!"+dbTarget.getRelativeFilePath()+". Log: "+exporterLog, e);
				}
				else {
					throw new NotesError(0, "Error importing DXL into database "+dbTarget.getServer()+"!!"+dbTarget.getRelativeFilePath(), e);
				}
			}
		}
	}
}
