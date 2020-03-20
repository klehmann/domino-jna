package com.mindoo.domino.jna.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCompoundStyleStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesFontIDFieldsStruct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesColorValueStruct;
import com.mindoo.domino.jna.richtext.CaptionPosition;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.StandaloneRichText;
import com.mindoo.domino.jna.richtext.TextStyle;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Internal helper class to write compound text with the high-level methods IBM is offering in their
 * C API (e.g. to create doclinks, add text, render other notes) and with some additions like methods to
 * add images or file hotspots.<br>
 * The class is used in {@link RichTextBuilder} and {@link StandaloneRichText} and should not be used
 * directly, since it has some potentially dangerous methods like adding C records via direct pointers that
 * might cause crashes when used in the wrong way.
 * 
 * @author Karsten Lehmann
 */
public class CompoundTextWriter implements IRecyclableNotesObject, ICompoundText, IAdaptable {
	private long m_handle64;
	private int m_handle32;
	
	private CloseResult m_closeResult;
	private boolean m_isStandalone;
	private boolean m_closed;
	private boolean m_hasData;
	
	private Map<Integer,Integer> m_definedStyleId;

	public CompoundTextWriter(long handle, boolean isStandalone) {
		if (!PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_handle64 = handle;
		m_isStandalone = isStandalone;
		m_definedStyleId = new HashMap<Integer, Integer>();
	}

	public CompoundTextWriter(int handle, boolean isStandalone) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_handle32 = handle;
		m_isStandalone = isStandalone;
		m_definedStyleId = new HashMap<Integer, Integer>();
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz==CompoundTextWriter.class) {
			return (T) this;
		}
		else {
			return null;
		}
	}
	
	public boolean isClosed() {
		return m_closed;
	}
	
	public void checkHandle() {
		if (PlatformUtils.is64Bit()) {
			if (m_handle64==0)
				throw new NotesError(0, "Compound text already recycled");
			NotesGC.__b64_checkValidObjectHandle(CompoundTextWriter.class, m_handle64);
		}
		else {
			if (m_handle32==0)
				throw new NotesError(0, "Compound text already recycled");
			NotesGC.__b32_checkValidObjectHandle(CompoundTextWriter.class, m_handle32);
		}
	}

	private int getDefaultFontId() {
		return getFontId(NotesConstants.FONT_FACE_SWISS, (byte) 0, (byte) 0, (byte) 10);
	}

	private int getFontId(byte face, byte attrib, byte color, byte pointSize) {
		NotesFontIDFieldsStruct fontIdStruct = NotesFontIDFieldsStruct.newInstance();
		fontIdStruct.Face = face;
		fontIdStruct.Attrib = attrib;
		fontIdStruct.Color = color;
		fontIdStruct.PointSize = pointSize;
		fontIdStruct.write();

		int fontId = fontIdStruct.getPointer().getInt(0);
		return fontId;
	}

	@Override
	public void addDocLink(NotesNote note, String comment) {
		if (note.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");
		
		NotesDatabase parentDb = note.getParent();
		String replicaId = parentDb.getReplicaID();
		String noteUnid = note.getUNID();
		NotesCollection defaultCollection = parentDb.openDefaultCollection();
		addDocLink(replicaId, defaultCollection.getUNID(), noteUnid, comment);
	}

	@Override
	public void addDocLink(String dbReplicaId, String viewUnid, String noteUNID, String comment) {
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");
		
		Memory commentMem = NotesStringUtils.toLMBCS(comment, true);
		short result;
		NotesUniversalNoteIdStruct viewUNIDStruct = NotesUniversalNoteIdStruct.fromString(viewUnid);
		NotesUniversalNoteIdStruct noteUNIDStruct = NotesUniversalNoteIdStruct.fromString(noteUNID);

		NotesUniversalNoteIdStruct.ByValue viewUNIDStructByVal = NotesUniversalNoteIdStruct.ByValue.newInstance();
		viewUNIDStructByVal.File = viewUNIDStruct.File;
		viewUNIDStructByVal.Note = viewUNIDStruct.Note;

		NotesUniversalNoteIdStruct.ByValue noteUNIDStructByVal = NotesUniversalNoteIdStruct.ByValue.newInstance();
		noteUNIDStructByVal.File = noteUNIDStruct.File;
		noteUNIDStructByVal.Note = noteUNIDStruct.Note;

		int[] dbReplicaIdInnards = NotesStringUtils.replicaIdToInnards(dbReplicaId);
		NotesTimeDateStruct.ByValue dbReplicaIdStructByVal = NotesTimeDateStruct.ByValue.newInstance();
		dbReplicaIdStructByVal.Innards[0] = dbReplicaIdInnards[0];
		dbReplicaIdStructByVal.Innards[1] = dbReplicaIdInnards[1];

		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAddDocLink(m_handle64, dbReplicaIdStructByVal, viewUNIDStructByVal, noteUNIDStructByVal, commentMem, 0);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAddDocLink(m_handle32, dbReplicaIdStructByVal, viewUNIDStructByVal, noteUNIDStructByVal, commentMem, 0);
		}
		NotesErrorUtils.checkResult(result);
		m_hasData=true;
	}

	@Override
	public void addRenderedNote(NotesNote note) {
		String formName = note.getItemValueString("Form");

		addRenderedNote(note, formName);
	}

	@Override
	public void addRenderedNote(NotesNote note, String form) {
		if (note.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");
		
		NotesDatabase db = note.getParent();
		NotesNote formNote = StringUtil.isEmpty(form) ? null : db.findDesignNote(form, NoteClass.FORM);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAddRenderedNote(m_handle64, note.getHandle64(), formNote==null ? 0 : formNote.getHandle64(), 0);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAddRenderedNote(m_handle32, note.getHandle32(), formNote==null ? 0 : formNote.getHandle32(), 0);
		}
		NotesErrorUtils.checkResult(result);
		m_hasData=true;
	}

	@Override
	public void addText(String txt) {
		addText(txt, (TextStyle) null, (FontStyle) null);
	}

	/**
	 * Converts the {@link TextStyle} to a style id, reusing already defined
	 * styles if all attributes are matching
	 * 
	 * @param style text style
	 * @return style id
	 */
	private int getStyleId(TextStyle style) {
		int styleHash = style.hashCode();
		Integer styleId = m_definedStyleId.get(styleHash);
		if (styleId==null) {
			checkHandle();
			if (isClosed())
				throw new NotesError(0, "CompoundText already closed");

			IntByReference retStyleId = new IntByReference();

			NotesCompoundStyleStruct styleStruct = style.getAdapter(NotesCompoundStyleStruct.class);
			if (styleStruct==null)
				throw new NotesError(0, "Unable to get style struct from TextStyle");

			Memory styleNameMem = NotesStringUtils.toLMBCS(style.getName(), true);

			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().CompoundTextDefineStyle(m_handle64, styleNameMem, styleStruct, retStyleId);
			}
			else {
				result = NotesNativeAPI32.get().CompoundTextDefineStyle(m_handle32, styleNameMem, styleStruct, retStyleId);

			}
			NotesErrorUtils.checkResult(result);
			styleId = retStyleId.getValue();

			m_definedStyleId.put(styleHash, styleId);
			m_hasData=true;
		}
		return styleId;
	}

	@Override
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle) {
		addText(txt, textStyle, fontStyle, true);
	}
	
	@Override
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle, boolean createParagraphOnLinebreak) {
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");

		Memory txtMem = NotesStringUtils.toLMBCS(txt, false);
		Memory lineDelimMem = new Memory(3);
		lineDelimMem.setByte(0, (byte) '\r'); 
		lineDelimMem.setByte(1, (byte) '\n'); 
		lineDelimMem.setByte(2, (byte) 0);

		int fontId;
		if (fontStyle==null) {
			fontId = getDefaultFontId();
		}
		else {
			FontId fontIdObj = fontStyle.getAdapter(FontId.class);
			if (fontIdObj==null)
				throw new NotesError(0, "Unable to get FontId from FontStyle");
			fontId = fontIdObj.getFontId();
		}
		
		int dwStyleID = textStyle==null ? NotesConstants.STYLE_ID_SAMEASPREV : getStyleId(textStyle);

		Pointer nlsInfoPtr = NotesNativeAPI.get().OSGetLMBCSCLS();
		short result;
		int dwFlags = NotesConstants.COMP_PRESERVE_LINES | NotesConstants.COMP_PARA_BLANK_LINE;
		if (createParagraphOnLinebreak) {
			dwFlags = dwFlags | NotesConstants.COMP_PARA_LINE;
		}
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAddTextExt(m_handle64, dwStyleID, fontId, txtMem, txtMem==null ? 0 : (int) txtMem.size(),
					lineDelimMem, dwFlags, nlsInfoPtr);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAddTextExt(m_handle32, dwStyleID, fontId, txtMem, txtMem==null ? 0 : (int) txtMem.size(),
					lineDelimMem, dwFlags, nlsInfoPtr);
		}
		NotesErrorUtils.checkResult(result);
		m_hasData=true;
	}

	@Override
	public void addRichTextItem(NotesNote otherNote, String itemName) {
		if (otherNote.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAssimilateItem(m_handle64, otherNote.getHandle64(), itemNameMem, 0);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAssimilateItem(m_handle32, otherNote.getHandle32(), itemNameMem, 0);
		}
		NotesErrorUtils.checkResult(result);
		m_hasData=true;
	}

	public void addCDRecords(Memory cdRecordMem) {
		addCDRecords(cdRecordMem, (int) cdRecordMem.size());
	}
	
	/**
	 * Method to add raw CD records to the compound text
	 * 
	 * @param cdRecordPtr pointer to CD records
	 * @param recordLength the length of the buffer
	 */
	public void addCDRecords(Pointer cdRecordPtr, int recordLength) {
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, cdRecordPtr, recordLength);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, cdRecordPtr, recordLength);
		}
		NotesErrorUtils.checkResult(result);
		m_hasData=true;
	}
	
	@Override
	public void addClosedStandaloneRichText(StandaloneRichText rt) {
		CompoundTextWriter ctWriter = rt.getAdapter(CompoundTextWriter.class);
		if (ctWriter==null)
			throw new NotesError(0, "Could not get "+CompoundTextWriter.class.getSimpleName()+" from "+StandaloneRichText.class.getSimpleName());
		
		addClosedStandaloneCompoundText(ctWriter);
	}
	
	/**
	 * Closes the specified standalone compound text and appends its content to this compound text
	 * 
	 * @param ct standalone compound text
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addClosedStandaloneCompoundText(CompoundTextWriter ct) {
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");
		
		if (!ct.isStandalone())
			throw new NotesError(0, "Provided compound text is not of type standalone");
		
		short result;

		CloseResult closeResult = ct.closeStandaloneContext();
		if (closeResult.getType()==CloseResultType.Buffer) {
			final CompoundTextStandaloneBuffer buffer = closeResult.getBuffer();
			final int bufferLength = buffer.getSize();
			
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction() {

					@Override
					public Object run() throws Exception {
						File tmpFile = File.createTempFile("comptext", ".tmp");
						FileOutputStream fOut = null;
						Pointer ptr;
						if (PlatformUtils.is64Bit()) {
							ptr = Mem64.OSLockObject(buffer.getHandle64());
						}
						else {
							ptr = Mem32.OSLockObject(buffer.getHandle32());
						}
						try {
							byte[] bufferData = ptr.getByteArray(0, bufferLength);
							fOut = new FileOutputStream(tmpFile);
							fOut.write(bufferData, 0, bufferData.length);
							fOut.flush();
						}
						finally {
							if (PlatformUtils.is64Bit()) {
								Mem64.OSUnlockObject(buffer.getHandle64());
							}
							else {
								Mem32.OSUnlockObject(buffer.getHandle32());
							}
							
							if (fOut!=null)
								fOut.close();
						}
						
						try {
							String filePath = tmpFile.getAbsolutePath();
							Memory filePathMem = NotesStringUtils.toLMBCS(filePath, true);

							short result;
							if (PlatformUtils.is64Bit()) {
								result = NotesNativeAPI64.get().CompoundTextAssimilateFile(m_handle64, filePathMem, 0);
							}
							else {
								result = NotesNativeAPI32.get().CompoundTextAssimilateFile(m_handle32, filePathMem, 0);
							}
							NotesErrorUtils.checkResult(result);
						}
						finally {
							if (!tmpFile.delete())
								tmpFile.deleteOnExit();
						}
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getCause() instanceof NotesError) {
					throw (NotesError) e.getCause();
				}
				else
					throw new NotesError(0, "Error appending standalone compound text content", e);
			}
		}
		else {
			String filePath = closeResult.getFilePath();
			Memory filePathMem = NotesStringUtils.toLMBCS(filePath, true);
			
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().CompoundTextAssimilateFile(m_handle64, filePathMem, 0);
			}
			else {
				result = NotesNativeAPI32.get().CompoundTextAssimilateFile(m_handle32, filePathMem, 0);
			}
			NotesErrorUtils.checkResult(result);
		}
		m_hasData = m_hasData | ct.hasData();
	}
	
	/**
	 * This routine assimilates the contents of a CD file (a file containing rich text) into a CompoundText context.<br>
	 * The contents are assimilated in that PABIDs and styles are fixed up (renumbered/renamed as needed) before they are
	 * appended to the CompoundText context.<br>
	 * <br>
	 * A CD file is a file containing data in Domino rich text format. CompoundTextClose() creates a CD file when closing
	 * a stand alone context containing more than 64K bytes of rich text.<br>
	 * A CD file consists of a datatype word (usually TYPE_COMPOSITE) followed by any number of CD records.<br>
	 * <br>
	 * The data type word is always in Host (machine-specific) format.<br>
	 * The remainder of the data in a CD file is in Domino Canonical format.
	 * 
	 * @param filePath path to file
	 */
	public void addCompoundTextFromFile(final String filePath) {
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");

		long fileSize;
		try {
			fileSize = AccessController.doPrivileged(new PrivilegedExceptionAction<Long>() {

				@Override
				public Long run() throws Exception {
					File file = new File(filePath);
					if (!file.exists())
						throw new FileNotFoundException("File does not exist: "+filePath);
					return file.length();
				}
			});
		} catch (PrivilegedActionException e) {
			throw new NotesError(0, "Error reading file size of file "+filePath, e);
		}
		
		if (fileSize==0) {
			//nothing to do
			return;
		}
		
		Memory filePathMem = NotesStringUtils.toLMBCS(filePath, true);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAssimilateFile(m_handle64, filePathMem, 0);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAssimilateFile(m_handle32, filePathMem, 0);
		}
		NotesErrorUtils.checkResult(result);
		m_hasData=true;
	}
	
	@Override
	public void addImage(File f) throws IOException {
		addImage(-1, -1, f);
	}

	@Override
	public void addImage(int resizeToWidth, int resizeToHeight, File f) throws IOException {
		FileInputStream fIn = new FileInputStream(f);
		try {
			addImage(resizeToWidth, resizeToHeight, (int) f.length(), fIn);
		}
		finally {
			fIn.close();
		}
	}
	
	@Override
	public void addImage(int fileSize, InputStream imageData) throws IOException {
		addImage(-1, -1, fileSize, imageData);
	}
	
	@Override
	public void addImage(int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException {
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");

		int bufSize = fileSize; // must be fileSize, otherwise PNG image width/height cannot be extracted
		
		BufferedInputStream innerBufIn = new BufferedInputStream(imageData, bufSize);
		innerBufIn.mark(bufSize);
		BufferedInputStream outerBufIn = new BufferedInputStream(innerBufIn, bufSize);
		
		FileType fileType = FileTypeDetector.detectFileType(outerBufIn);
		if (fileType == FileType.Unknown)
			throw new NotesError(0, "Unable to detect filetype of image");
		innerBufIn.reset();
		
		boolean isSupported = false;
		if (fileType == FileType.Gif || fileType == FileType.Jpeg || fileType == FileType.Bmp || fileType == FileType.Png) {
			isSupported = true;
		}
		
		if (!isSupported)
			throw new NotesError(0, "Unsupported filetype "+fileType+". Only GIF, PNG, JPEG and BMP are supported");
		
		int imgWidth = -1;
		int imgHeight = -1;

		outerBufIn.mark(bufSize);
		Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(outerBufIn);
		} catch (ImageProcessingException e) {
			throw new NotesError(0, "Unable to read image metadata", e);
		}
		innerBufIn.reset();
		
		switch (fileType) {
		case Gif:
			Collection<GifHeaderDirectory> gifHeaderDirs = metadata.getDirectoriesOfType(GifHeaderDirectory.class);
			if (gifHeaderDirs!=null && !gifHeaderDirs.isEmpty()) {
				GifHeaderDirectory header = gifHeaderDirs.iterator().next();
				try {
					imgWidth = header.getInt(GifHeaderDirectory.TAG_IMAGE_WIDTH);
					imgHeight = header.getInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT);
				} catch (MetadataException e) {
					throw new NotesError(0, "Error reading GIF image size", e);
				}
			}
			break;
		case Jpeg:
			Collection<JpegDirectory> jpegHeaderDirs = metadata.getDirectoriesOfType(JpegDirectory.class);
			if (jpegHeaderDirs!=null && !jpegHeaderDirs.isEmpty()) {
				JpegDirectory jpegDir = jpegHeaderDirs.iterator().next();
				try {
					imgWidth = jpegDir.getImageWidth();
					imgHeight = jpegDir.getImageHeight();
				} catch (MetadataException e) {
					throw new NotesError(0, "Error reading JPEG image size", e);
				}
			}
			break;
		case Bmp:
			Collection<BmpHeaderDirectory> bmpHeaderDirs = metadata.getDirectoriesOfType(BmpHeaderDirectory.class);
			if (bmpHeaderDirs!=null && !bmpHeaderDirs.isEmpty()) {
				BmpHeaderDirectory bmpHeader = bmpHeaderDirs.iterator().next();
				try {
					imgWidth = bmpHeader.getInt(BmpHeaderDirectory.TAG_IMAGE_WIDTH);
					imgHeight = bmpHeader.getInt(BmpHeaderDirectory.TAG_IMAGE_HEIGHT);
				} catch (MetadataException e) {
					throw new NotesError(0, "Error reading BMP image size", e);
				}
			}
			break;
		case Png:
			Collection<PngDirectory> pngHeaderDirs = metadata.getDirectoriesOfType(PngDirectory.class);
			if (pngHeaderDirs!=null && !pngHeaderDirs.isEmpty()) {
				PngDirectory pngHeader = pngHeaderDirs.iterator().next();
				try {
					imgWidth = pngHeader.getInt(PngDirectory.TAG_IMAGE_WIDTH);
					imgHeight = pngHeader.getInt(PngDirectory.TAG_IMAGE_HEIGHT);
				} catch (MetadataException e) {
					throw new NotesError(0, "Error reading BMP image size", e);
				}
			}
			break;
		default:
			//
		}
		
		if (imgWidth<=0 || imgHeight<=0) {
			throw new IllegalArgumentException("Width/Height cannot be extracted from the image data");
		}
		
		short result;
		
//		write graphic header:
//		typedef struct {
//			   LSIG     Header;     /* Signature and Length */
//			   RECTSIZE DestSize;   /* Destination Display size in TWIPS
//			                           (1/1440 inch) */
//			   RECTSIZE CropSize;   /* Reserved */
//			   CROPRECT CropOffset; /* Reserved */
//			   WORD     fResize;    /* True if user resized object */
//			   BYTE     Version;    /* CDGRAPHIC_VERSIONxxx */
//			   BYTE     bFlags;     /* Ignored before CDGRAPHIC_VERSION3 */
//			      WORD     wReserved;
//			} CDGRAPHIC;
				
		Memory graphicMem = new Memory(
				6 +				//LSIG
				2 + 2 +			// RECTSIZE
				2 + 2 +			// RECTSIZE
				2 + 2 + 2 + 2 + // CROPRECT
				2 +				// fResize
				1 + 				// Verson
				1 + 				// Flags
				2 				// Reserved
				);
		graphicMem.clear();
		graphicMem.setShort(0, NotesConstants.SIG_CD_GRAPHIC);
		graphicMem.share(2).setInt(0, (int) graphicMem.size());
		
		boolean isResized = resizeToWidth!=-1 && resizeToHeight!=-1;
		
		// DestSize : RECTSIZE (Word/Word)
		if (isResized) {
			graphicMem.share(6).setShort(0, (short) (resizeToWidth & 0xffff));
			graphicMem.share(6 + 2).setShort(0, (short) (resizeToHeight & 0xffff));
		}
		else {
			graphicMem.share(6).setShort(0, (short) 0);
			graphicMem.share(6 + 2).setShort(0, (short) 0);
		}
		
		// CropSize : RECTSIZE (Word/Word)
		graphicMem.share(6 + 4).setShort(0, (short) 0);
		graphicMem.share(6 + 6).setShort(0, (short) 0);
		// CropOffset : CROPRECT
		graphicMem.share(6 + 8).setShort(0, (short) 0);
		graphicMem.share(6 + 10).setShort(0, (short) 0);
		graphicMem.share(6 + 12).setShort(0, (short) 0);
		graphicMem.share(6 + 14).setShort(0, (short) 0);
		// fResize : WORD
		if (isResized) {
			graphicMem.share(6 + 16).setShort(0, (short) 1);
		}
		else {
			graphicMem.share(6 + 16).setShort(0, (short) 0);
		}
		//Version: BYTE
		graphicMem.share(6 + 18).setByte(0, NotesConstants.CDGRAPHIC_VERSION3);
		
		//Flags:
		graphicMem.share(6 + 19).setByte(0, (byte) NotesConstants.CDGRAPHIC_FLAG_DESTSIZE_IS_PIXELS);
		
		//Reserved:
		graphicMem.share(6 + 20).setShort(0, (short) 0);
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, graphicMem, (int) graphicMem.size());
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, graphicMem, (int) graphicMem.size());
			NotesErrorUtils.checkResult(result);
		}
		
		m_hasData=true;

		//write image header
//		typedef struct {
//		   LSIG  Header;        /* Signature and Length */
//		   WORD  ImageType;     /* Type of image (e.g., GIF, JPEG) */
//		   WORD  Width;         /* Width of the image (in pixels) */
//		   WORD  Height;        /* Height of the image (in pixels) */
//		   DWORD ImageDataSize; /* Size (in bytes) of the image data */
//		   DWORD SegCount;      /* Number of CDIMAGESEGMENT records
//		                           expected to follow */
//		   DWORD Flags;         /* Flags (currently unused) */
//		   DWORD Reserved;      /* Reserved for future use */
//		} CDIMAGEHEADER;

		Memory imageHeaderMem = new Memory(
				2+4 + //LSIG
				2 + //ImageType = 0x0003
				2 + //Width = 48
				2 + //Height = 48
				4 + //ImageDataSize = 0
				4 + //SegCount = 0
				4 + //Flags = 0
				4  //Reserved
				);
		imageHeaderMem.clear();
		
		imageHeaderMem.setShort(0, NotesConstants.SIG_CD_IMAGEHEADER);
		imageHeaderMem.share(2).setInt(0, (int) imageHeaderMem.size());
		short imageTypeShort;
		switch (fileType) {
		case Gif:
			imageTypeShort = NotesConstants.CDIMAGETYPE_GIF;
			break;
		case Jpeg:
			imageTypeShort = NotesConstants.CDIMAGETYPE_JPEG;
			break;
		case Png:
		case Bmp:
			//R9 introduced PNG rendering support, but for the type they use BMP
			//and added a new (undocumented) CD record type right after CDIMAGEHEADER that
			//probably contains the actual image type (0x0004 for PNG) and the image size as DWORD
			imageTypeShort = NotesConstants.CDIMAGETYPE_BMP;
			break;
		default:
			throw new IllegalArgumentException("Unknown image type: "+fileType);
		}
		
		final int MAX_SEGMENT_SIZE = 10240;
		
		imageHeaderMem.share(6).setShort(0, imageTypeShort);
		
		short imgWidthShort = (short) (imgWidth & 0xffff);
		imageHeaderMem.share(8).setShort(0, imgWidthShort);
		short imgHeightShort = (short) (imgHeight & 0xffff);
		imageHeaderMem.share(10).setShort(0, imgHeightShort);
		
		if (fileType == FileType.Png) {
			//for PNG, set filesize 0x00000000, probably to prevent older clients from reading the data
			imageHeaderMem.share(12).setInt(0, 0);
		}
		else {
			imageHeaderMem.share(12).setInt(0, fileSize);
		}
		
		int fullSegments = (int) (fileSize / MAX_SEGMENT_SIZE);
		int segments = fullSegments;
		int dataBytesInLastSegment = fileSize - fullSegments * MAX_SEGMENT_SIZE;
		if (dataBytesInLastSegment>0) {
			segments++;
		}
		
		if (fileType == FileType.Png) {
			//for PNG, set segments to 0x00000000, probably to prevent older clients from reading the data
			imageHeaderMem.share(16).setInt(0, 0);
		}
		else {
			imageHeaderMem.share(16).setInt(0, segments & 0xffff);
		}
		
		imageHeaderMem.share(20).setInt(0, 0); //flags
		imageHeaderMem.share(24).setInt(0, 0); //reserved

		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, imageHeaderMem, (int) imageHeaderMem.size());
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, imageHeaderMem, (int) imageHeaderMem.size());
			NotesErrorUtils.checkResult(result);
		}

		if (fileType==FileType.Png) {
			//for PNG we add an undocumented CD record type to define the image type,
			//number of chunks and filesize

//			typedef struct
//			  {
//			  BSIG  Header;       /* Signature and Length */
//			  WORD  ImageType;    /* Type of image (e.g., PNG, etc.) */
//			  DWORD ImageDataSize;    /* Size (in bytes) of the image data */
//			  DWORD SegCount;     /* Number of CDIMAGESEGMENT records expected to follow */
//			  DWORD Flags;        /* Flags (currently unused) */
//			  DWORD Reserved;     /* Reserved for future use */
//			  } CDIMAGEHEADER2;
			  
			Memory imageHeader2Mem = new Memory(20);
			imageHeader2Mem.clear();
			
			//BSIG Header with type and size
			imageHeader2Mem.setByte(0, (byte) (NotesConstants.SIG_CD_IMAGEHEADER2 & 0xff));
			imageHeader2Mem.share(1).setByte(0, (byte) (imageHeader2Mem.size() & 0xff));
			
			imageHeader2Mem.share(2).setShort(0, NotesConstants.CDIMAGETYPE_PNG);
			imageHeader2Mem.share(4).setInt(0, fileSize);
			imageHeader2Mem.share(8).setShort(0, (short) (segments & 0xffff));
			
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, imageHeader2Mem, (int) imageHeader2Mem.size());
				NotesErrorUtils.checkResult(result);
			}
			else {
				result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, imageHeader2Mem, (int) imageHeader2Mem.size());
				NotesErrorUtils.checkResult(result);
			}
		}

		byte[] buf = new byte[MAX_SEGMENT_SIZE];
		int len;
		int bytesRead = 0;
		
		for (int i=0; i<segments; i++) {
			Arrays.fill(buf, (byte) 0);
			
			len = innerBufIn.read(buf);
			if (i<(segments-1)) {
				if (len<MAX_SEGMENT_SIZE)
					throw new IllegalStateException("The InputStream returned "+(bytesRead+len)+" instead of "+fileSize);
			}
			else {
				//last segment
				if (len < 0) {
					throw new IllegalStateException("The InputStream returned "+bytesRead+" instead of "+fileSize);
				}
			}
			bytesRead += len;

			//write image segment
//			typedef struct {
//			   LSIG Header;   /* Signature and Length */
//			   WORD DataSize; /* Actual Size of image bits in bytes, ignoring
//			                     any filler */
//			   WORD SegSize;  /* Size of segment, is equal to or larger than
//			                     DataSize if filler byte added to maintain word
//			                     boundary */
//			} CDIMAGESEGMENT;
			
//			Read record IMAGESEGMENT (124) with 10244 data bytes, cdrecord length: 10250
//			Data:
//			[00 28 00 28 47 49 46 38]   [.(.(GIF8]
//			[39 61 6e 01 2c 01 f7 00]   [9an.,...]
//			[00 06 06 06 2b 2b 2b 4a]   [....+++J]
//			[4a 4a 6b 6b 6b 8c 8c 8c]   [JJkkk...]
//			[ac ac ac cb cb cb ff ff]   [........]
//			[ff 00 00 00 00 00 00 00]   [........]
//			[00 00 00 00 00 00 00 00]   [........]
//			[00 00 00 00 00 00 00 00]   [........]
//			[00 00 00 00 00 00 00 00]   [........]
//			[00 00 00 00 00 00 00 00]   [........]
//			[00 00 00 00 00 00 00 00]   [........]
//			[00 00 00 00 00 00 00 00]   [........]
					
			int segSize = len;
			if ((segSize & 1L)==1) {
				segSize++;
			}

			Memory imageSegMem = new Memory(
					6 + //LSIG
					2 + //DataSize
					2 + //SegSize
					segSize
					);
			imageSegMem.clear();
			
			imageSegMem.setShort(0, NotesConstants.SIG_CD_IMAGESEGMENT); // LSIG
			imageSegMem.share(2).setInt(0, (int) imageSegMem.size()); // LSIG
			imageSegMem.share(6).setShort(0, (short) (len & 0xffff)); // DataSize
			imageSegMem.share(8).setShort(0, (short) (segSize & 0xffff)); // SegSize
			imageSegMem.share(10).write(0, buf, 0, len); // Data
			
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, imageSegMem, (int) imageSegMem.size());
				NotesErrorUtils.checkResult(result);
			}
			else {
				result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, imageSegMem, (int) imageSegMem.size());
				NotesErrorUtils.checkResult(result);
			}
		}
	}

	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay) {
		addFileHotspot(attachment.getFileName(), filenameToDisplay);
	}
	
	@Override
	public void addFileHotspot(String attachmentProgrammaticName, String filenameToDisplay) {
		InputStream in = getClass().getResourceAsStream("file-icon.gif");
		if (in==null)
			throw new IllegalStateException("Default icon file not found");
		try {
			ByteArrayOutputStream bOut = new ByteArrayOutputStream(1024);
			byte[] buf = new byte[1024];
			int len;
			
			while ((len=in.read(buf))>0) {
				bOut.write(buf,0, len);
			}
			
			ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
			addFileHotspot(attachmentProgrammaticName, filenameToDisplay, filenameToDisplay, new FontStyle(), CaptionPosition.BELOWCENTER, 0, 0, 0, -1, -1, bOut.size(), bIn);
		} catch (IOException e) {
			throw new IllegalStateException("Unexpected problems reading the default icon", e);
		}
		finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, File image) throws IOException {
		addFileHotspot(attachment.getFileName(), filenameToDisplay, captionText, image);
	}
	
	@Override
	public void addFileHotspot(String attachmentProgrammaticName, String filenameToDisplay, String captionText, File image) throws IOException {
		FileInputStream fIn = new FileInputStream(image);
		try {
			addFileHotspot(attachmentProgrammaticName, filenameToDisplay, captionText, new FontStyle(), CaptionPosition.BELOWCENTER,
					0, 0, 0, -1, -1, (int) image.length(), fIn);
		}
		finally {
			fIn.close();
		}
	}

	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, FontStyle captionStyle,
			CaptionPosition captionPos, int captionColorRed, int captionColorGreen, int captionColorBlue,
			int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException {
		
		addFileHotspot(attachment.getFileName(), filenameToDisplay, captionText, captionStyle, captionPos,
				captionColorRed, captionColorGreen, captionColorBlue, resizeToWidth, resizeToHeight, fileSize,
				imageData);
	}
	
	@Override
	public void addFileHotspot(String attachmentProgrammaticName, String filenameToDisplay, String captionText, FontStyle captionStyle,
			CaptionPosition captionPos, int captionColorRed, int captionColorGreen, int captionColorBlue,
			int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException {
		
		checkHandle();
		if (isClosed())
			throw new NotesError(0, "CompoundText already closed");

	if (captionColorRed<0 || captionColorRed>255)
		throw new IllegalArgumentException("Red value of color can only be between 0 and 255");
	if (captionColorGreen<0 || captionColorGreen>255)
		throw new IllegalArgumentException("Green value of color can only be between 0 and 255");
	if (captionColorBlue<0 || captionColorBlue>255)
		throw new IllegalArgumentException("Blue value of color can only be between 0 and 255");
	
	Memory beginMem = new Memory(
			2 + //BSIG
			2 + //Version
			2 //Signature
			);
	beginMem.clear();
	
//	Read record BEGIN (221) with 4 data bytes, cdrecord length: 6
//	Data:
//	[00 00 ad ff            ]   [....    ]
	
	beginMem.setShort(0, NotesConstants.SIG_CD_BEGIN);
	beginMem.share(1).setByte(0, (byte) (beginMem.size() & 0xff));
	beginMem.share(2).setShort(0, (short) 0);
	beginMem.share(4).setShort(0, (short) (NotesConstants.SIG_CD_V4HOTSPOTBEGIN & 0xffff));
	
	short result;
	if (PlatformUtils.is64Bit()) {
		result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, beginMem, (int) beginMem.size());
		NotesErrorUtils.checkResult(result);
	}
	else {
		result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, beginMem, (int) beginMem.size());
		NotesErrorUtils.checkResult(result);
	}
	m_hasData=true;
	
//
//		Read record HOTSPOTBEGIN (-87) with 24 data bytes, cdrecord length: 28
//		Data:
//		[04 00 08 00 00 00 10 00]   [........]
//		[6d 61 69 6e 2e 6a 73 00]   [main.js.]
//		[6d 61 69 6e 2e 6a 73 00]   [main.js.]
	
//	typedef struct {
//		   WSIG  Header; /* Signature and length of this record */	
//		   WORD  Type;
//		   DWORD Flags;
//		   WORD  DataLength;
//		/*	Data follows... */
//			/*  if HOTSPOTREC_RUNFLAG_SIGNED, WORD SigLen then SigData follows. */
//		} CDHOTSPOTBEGIN;
	
	Memory uniqueFileNameAttachment = NotesStringUtils.toLMBCS(attachmentProgrammaticName, true);
	Memory fileNameToDisplayMem = NotesStringUtils.toLMBCS(filenameToDisplay, true);
	
	Memory hotspotBeginMem = new Memory(
			2 + 2 + //WSIG
			2 + //Type
			4 + //Flags
			2 + //DataLength
			uniqueFileNameAttachment.size() + 
			fileNameToDisplayMem.size()
			);
	hotspotBeginMem.clear();
	
	hotspotBeginMem.setShort(0, NotesConstants.SIG_CD_HOTSPOTBEGIN);
	hotspotBeginMem.share(2).setShort(0, (short) (hotspotBeginMem.size() & 0xffff));
	hotspotBeginMem.share(4).setShort(0, NotesConstants.HOTSPOTREC_TYPE_FILE);
	
	int flags = NotesConstants.HOTSPOTREC_RUNFLAG_NOBORDER;
	hotspotBeginMem.share(6).setInt(0, flags);
	hotspotBeginMem.share(10).setShort(0, (short) ((uniqueFileNameAttachment.size() + fileNameToDisplayMem.size()) & 0xffff));
	
	hotspotBeginMem.share(12).write(0, uniqueFileNameAttachment.getByteArray(0, (int) uniqueFileNameAttachment.size()), 0, (int) uniqueFileNameAttachment.size());
	hotspotBeginMem.share(12 + uniqueFileNameAttachment.size()).write(0, fileNameToDisplayMem.getByteArray(0, (int) fileNameToDisplayMem.size()), 0, (int) fileNameToDisplayMem.size());
	
	if (PlatformUtils.is64Bit()) {
		result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, hotspotBeginMem, (int) hotspotBeginMem.size());
		NotesErrorUtils.checkResult(result);
	}
	else {
		result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, hotspotBeginMem, (int) hotspotBeginMem.size());
		NotesErrorUtils.checkResult(result);
	}
	
	addImage(resizeToWidth, resizeToHeight, fileSize, imageData);
	
//	typedef struct {
//		   WSIG        Header;       /* Tag and length */
//		   WORD        wLength;      /* Text length */
//		   BYTE        Position;     /* One of the position flags above */
//		   FONTID      FontID;       /* Font to use for the text */
//		   COLOR_VALUE FontColor;    /* RGB font color info */
//		   BYTE        Reserved[11]; /* Reserved for future use */
//		/* The 8-bit text string follows... */
//		} CDCAPTION;
		
	Memory captionTextMem = NotesStringUtils.toLMBCS(captionText, false);
	
	Memory captionMem = new Memory(
			2 + 2 + //WSIG
			2 + // Text Length
			1 + // Position
			4 + // FontID
			6 + // FontColor
			11 + //Reserved
			captionTextMem.size()
			);
	captionMem.clear();
	
	captionMem.setShort(0, NotesConstants.SIG_CD_CAPTION);
	captionMem.share(2).setShort(0, (short) (captionMem.size() & 0xffff));
	captionMem.share(4).setShort(0, (short) (captionTextMem.size() & 0xffff));
	
	byte captionPosition;
	if (captionPos==CaptionPosition.BELOWCENTER) {
		captionPosition = NotesConstants.CAPTION_POSITION_BELOW_CENTER;
	}
	else if (captionPos==CaptionPosition.MIDDLECENTER) {
		captionPosition = NotesConstants.CAPTION_POSITION_MIDDLE_CENTER;
	}
	else {
		captionPosition = 0;
	}
	captionMem.share(6).setByte(0, captionPosition);
	
	FontId fontIdObj = captionStyle.getAdapter(FontId.class);
	if (fontIdObj==null)
		throw new NotesError(0, "Unable to get FontId from FontStyle");
	int fontId = fontIdObj.getFontId();
	captionMem.share(7).setInt(0, fontId);
	
	byte captionColorRedByte = (byte) (captionColorRed & 0xff);
	byte captionColorGreenByte = (byte) (captionColorGreen & 0xff);
	byte captionColorBlueByte = (byte) (captionColorBlue & 0xff);
	
	NotesColorValueStruct colorStruct = NotesColorValueStruct.newInstance();
	colorStruct.Component1 = captionColorRedByte;
	colorStruct.Component2 = captionColorGreenByte;
	colorStruct.Component3 = captionColorBlueByte;
	colorStruct.Flags = NotesConstants.COLOR_VALUE_FLAGS_ISRGB;
	colorStruct.write();
	captionMem.share(11).write(0, colorStruct.getPointer().getByteArray(0, 6), 0, 6);
	captionMem.share(17).write(0, new byte[11], 0, 11);
	captionMem.share(28).write(0, captionTextMem.getByteArray(0, (int) captionTextMem.size()), 0, (int) captionTextMem.size());
	
	if (PlatformUtils.is64Bit()) {
		result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, captionMem, (int) captionMem.size());
		NotesErrorUtils.checkResult(result);
	}
	else {
		result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, captionMem, (int) captionMem.size());
		NotesErrorUtils.checkResult(result);
	}
	
//
//		Read record HOTSPOTEND (170) with 0 data bytes, cdrecord length: 2
//		Data:
	
	Memory hotspotEndMem = new Memory(2);
	hotspotEndMem.clear();
	
	hotspotEndMem.setShort(0, NotesConstants.SIG_CD_HOTSPOTEND);
	hotspotEndMem.share(1).setByte(0, (byte) (hotspotEndMem.size() & 0xff));
	
	if (PlatformUtils.is64Bit()) {
		result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, hotspotEndMem, (int) hotspotEndMem.size());
		NotesErrorUtils.checkResult(result);
	}
	else {
		result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, hotspotEndMem, (int) hotspotEndMem.size());
		NotesErrorUtils.checkResult(result);
	}
	
	Memory endMem = new Memory(6);
	endMem.clear();
	
	endMem.setShort(0, NotesConstants.SIG_CD_END);
	endMem.share(1).setByte(0, (byte) (endMem.size() & 0xff));
	endMem.share(2).setShort(0, (short) 0);
	endMem.share(4).setShort(0, (short) (NotesConstants.SIG_CD_V4HOTSPOTEND & 0xffff));

	if (PlatformUtils.is64Bit()) {
		result = NotesNativeAPI64.get().CompoundTextAddCDRecords(m_handle64, endMem, (int) endMem.size());
		NotesErrorUtils.checkResult(result);
	}
	else {
		result = NotesNativeAPI32.get().CompoundTextAddCDRecords(m_handle32, endMem, (int) endMem.size());
		NotesErrorUtils.checkResult(result);
	}
	
	//
//		Read record END (222) with 4 data bytes, cdrecord length: 6
//		Data:
//		[00 00 ae 00            ]   [....    ]

	}
	
	public boolean hasData() {
		return m_hasData;
	}
	
	public boolean isStandalone() {
		return m_isStandalone;
	}
	
	/**
	 * This routine closes the build process. Use {@link NotesNote#update()} 
	 * after {@link #closeItemContext()} to update and save the document.
	 */
	public void closeItemContext() {
		if (isStandalone())
			throw new UnsupportedOperationException("This is a standalone compound text");

		checkHandle();
		if (isClosed())
			return;
		
		short result;
		if (PlatformUtils.is64Bit()) {
			NotesGC.__objectBeeingBeRecycled(CompoundTextWriter.class, this);
			result = NotesNativeAPI64.get().CompoundTextClose(m_handle64, null, null, null, (short) 0);
			m_handle64 = 0;
			m_closed = true;
		}
		else {
			NotesGC.__objectBeeingBeRecycled(CompoundTextWriter.class, this);
			result = NotesNativeAPI32.get().CompoundTextClose(m_handle32, null, null, null, (short) 0);
			m_handle32 = 0;
			m_closed = true;
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This routine closes the standalone CompoundText. The result is either an in-memory buffer or
	 * a temporary file on disk, depending on the memory size of the CD records
	 * 
	 * @return close result
	 */
	public CloseResult closeStandaloneContext() {
		if (!isStandalone())
			throw new UnsupportedOperationException("This is not a standalone compound text");
		
		if (isClosed())
			return m_closeResult;
		
		checkHandle();
		
		Memory returnFileMem = new Memory(NotesConstants.MAXPATH);
		returnFileMem.clear();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethBuffer = new LongByReference();
			rethBuffer.setValue(0);
			IntByReference retBufSize = new IntByReference();
			retBufSize.setValue(0);
			NotesGC.__objectBeeingBeRecycled(CompoundTextWriter.class, this);
			result = NotesNativeAPI64.get().CompoundTextClose(m_handle64, rethBuffer, retBufSize, returnFileMem, (short) (NotesConstants.MAXPATH & 0xffff));
			NotesErrorUtils.checkResult(result);
			m_handle64 = 0;
			m_closed = true;

			long hBuffer = rethBuffer.getValue();
			if (hBuffer!=0) {
				//content was small enough to fit into an in-memory buffer
				int bufSize = retBufSize.getValue();
				CompoundTextStandaloneBuffer buf = new CompoundTextStandaloneBuffer(hBuffer, bufSize);
				//register buffer instance to auto-gc it
				NotesGC.__memoryAllocated(buf);
				
				m_closeResult = new CloseResult();
				m_closeResult.setType(CloseResultType.Buffer);
				m_closeResult.setBuffer(buf);
				return m_closeResult;
			}
			else {
				//content had to be written to a temp file
				String fileName = NotesStringUtils.fromLMBCS(returnFileMem, -1);
				m_closeResult = new CloseResult();
				m_closeResult.setType(CloseResultType.File);
				m_closeResult.setFilePath(fileName);
				return m_closeResult;
			}
		}
		else {
			IntByReference rethBuffer = new IntByReference();
			rethBuffer.setValue(0);
			IntByReference retBufSize = new IntByReference();
			retBufSize.setValue(0);
			NotesGC.__objectBeeingBeRecycled(CompoundTextWriter.class, this);
			result = NotesNativeAPI32.get().CompoundTextClose(m_handle32, rethBuffer, retBufSize, returnFileMem, (short) (NotesConstants.MAXPATH & 0xffff));
			NotesErrorUtils.checkResult(result);
			m_handle32 = 0;
			m_closed = true;
			
			int hBuffer = rethBuffer.getValue();
			if (hBuffer!=0) {
				//content was small enough to fit into an in-memory buffer
				int bufSize = retBufSize.getValue();
				CompoundTextStandaloneBuffer buf = new CompoundTextStandaloneBuffer(hBuffer, bufSize);
				//register buffer instance to auto-gc it
				NotesGC.__memoryAllocated(buf);
				
				m_closeResult = new CloseResult();
				m_closeResult.setType(CloseResultType.Buffer);
				m_closeResult.setBuffer(buf);
				return m_closeResult;
			}
			else {
				//content had to be written to a temp file
				String fileName = NotesStringUtils.fromLMBCS(returnFileMem, -1);
				m_closeResult = new CloseResult();
				m_closeResult.setType(CloseResultType.File);
				m_closeResult.setFilePath(fileName);
				return m_closeResult;
			}
		}
	}
	
	public static enum CloseResultType {Buffer, File}
	
	public static class CloseResult {
		
		private CloseResultType m_type;
		private CompoundTextStandaloneBuffer m_buffer;
		private String m_filePath;
		
		public CloseResultType getType() {
			return m_type;
		}
		
		private void setType(CloseResultType type) {
			this.m_type = type;
		}
		
		public CompoundTextStandaloneBuffer getBuffer() {
			return m_buffer;
		}
		
		private void setBuffer(CompoundTextStandaloneBuffer buf) {
			m_buffer = buf;
		}
		
		public String getFilePath() {
			return m_filePath;
		}

		private void setFilePath(String fileName) {
			this.m_filePath = fileName;
		}
	}
	
	@Override
	public void recycle() {
		if (isRecycled())
			return;

		if (PlatformUtils.is64Bit()) {
			if (m_handle64!=0) {
				NotesNativeAPI64.get().CompoundTextDiscard(m_handle64);
				NotesGC.__objectBeeingBeRecycled(CompoundTextWriter.class, this);
				m_handle64=0;
			}
		}
		else {
			if (m_handle32!=0) {
				NotesNativeAPI32.get().CompoundTextDiscard(m_handle32);
				NotesGC.__objectBeeingBeRecycled(CompoundTextWriter.class, this);
				m_handle32=0;
			}
		}
	}

	@Override
	public boolean isRecycled() {
		if (PlatformUtils.is64Bit()) {
			return m_handle64==0;
		}
		else {
			return m_handle32==0;
		}
	}

	@Override
	public boolean isNoRecycle() {
		return false;
	}

	@Override
	public int getHandle32() {
		return m_handle32;
	}

	@Override
	public long getHandle64() {
		return m_handle64;
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "CompoundTextWriter [recycled]";
		}
		else {
			return "CompoundTextWriter [handle="+(PlatformUtils.is64Bit() ? m_handle64 : m_handle32)+", standalone="+isStandalone()+", closed="+isClosed()+"]";
		}
	}

	/**
	 * Container to store and track the in-memory buffer that Domino creates when a
	 * compound text is closed and its content is small enough to fit into a memory segment.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class CompoundTextStandaloneBuffer implements IAllocatedMemory {
		private long m_handle64;
		private int m_handle32;
		private int m_size;
		private List<FileInputStream> m_createdTempFileStreams;
		private File m_file;
		
		public CompoundTextStandaloneBuffer(long handle, int size) {
			if (!PlatformUtils.is64Bit())
				throw new NotesError(0, "Constructor is 64 bit only");
			m_handle64 = handle;
			m_size = size;
		}
		
		public CompoundTextStandaloneBuffer(int handle, int size) {
			if (PlatformUtils.is64Bit())
				throw new NotesError(0, "Constructor is 32 bit only");
			m_handle32 = handle;
			m_size = size;
		}

		public static class FileInfo {
			private String m_filePath;
			private long m_fileSize;
			private FileInputStream m_fileIn;
			
			public FileInfo(String filePath, long fileSize, FileInputStream fileIn) {
				m_filePath = filePath;
				m_fileSize = fileSize;
				m_fileIn = fileIn;
			}
			
			public String getFilePath() {
				return m_filePath;
			}
			
			public long getFileSize() {
				return m_fileSize;
			}
			
			public FileInputStream getStream() {
				return m_fileIn;
			}
		}
		
		/**
		 * Writes the memory buffer content to a temporary file and returns a {@link FileInputStream}
		 * to read the data. The stream will be auto-closed and the file deleted when this buffer is freed.
		 * 
		 * @return stream to temporary file on disk
		 * @throws FileNotFoundException  in case of I/O errors
		 */
		public FileInfo asFileOnDisk() throws FileNotFoundException {
			if (m_file==null || !m_file.exists()) {
				try {
					m_file = AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {

						@Override
						public File run() throws Exception {
							File tmpFile = File.createTempFile("comptext", ".tmp");
							FileOutputStream fOut = null;
							Pointer ptr;
							if (PlatformUtils.is64Bit()) {
								ptr = Mem64.OSLockObject(m_handle64);
							}
							else {
								ptr = Mem32.OSLockObject(m_handle32);
							}
							try {
								byte[] bufferData = ptr.getByteArray(0, m_size);
								fOut = new FileOutputStream(tmpFile);
								fOut.write(bufferData, 0, bufferData.length);
								fOut.flush();
							}
							finally {
								if (PlatformUtils.is64Bit()) {
									Mem64.OSUnlockObject(m_handle64);
								}
								else {
									Mem32.OSUnlockObject(m_handle32);
								}
								
								if (fOut!=null)
									fOut.close();
							}
							return tmpFile;
						}
					});
				} catch (PrivilegedActionException e) {
					if (e.getCause() instanceof NotesError) {
						throw (NotesError) e.getCause();
					}
					else
						throw new NotesError(0, "Could not write content of memory buffer to disk", e);
				}
			}
			FileInputStream in = new FileInputStream(m_file);
			//keep track of stream to auto-close when memory is freed
			if (m_createdTempFileStreams==null)
				m_createdTempFileStreams = new ArrayList<FileInputStream>();
			m_createdTempFileStreams.add(in);
			
			return new FileInfo(m_file.getAbsolutePath(), m_file.length(), in);
		}
		
		public int getSize() {
			return m_size;
		}
		
		@Override
		public void free() {
			if (isFreed())
				return;
			
			if (PlatformUtils.is64Bit()) {
				NotesGC.__memoryBeeingFreed(this);
				short result = Mem64.OSMemFree(m_handle64);
				NotesErrorUtils.checkResult(result);
				m_handle64=0;
			}
			else {
				NotesGC.__memoryBeeingFreed(this);
				short result = Mem32.OSMemFree(m_handle32);
				NotesErrorUtils.checkResult(result);
				m_handle32=0;
			}
			
			if (m_createdTempFileStreams!=null && !m_createdTempFileStreams.isEmpty()) {
				for (FileInputStream currIn : m_createdTempFileStreams) {
					try {
						currIn.close();
					} catch (IOException e) {
						//just write to stderr, go on and try to delete the file
						e.printStackTrace();
					}
				}
			}
			
			if (m_file!=null && m_file.exists()) {
				if (!m_file.delete())
					m_file.deleteOnExit();
			}
		}

		@Override
		public boolean isFreed() {
			if (PlatformUtils.is64Bit()) {
				return m_handle64==0;
			}
			else {
				return m_handle32==0;
			}
		}

		@Override
		public int getHandle32() {
			return m_handle32;
		}

		@Override
		public long getHandle64() {
			return m_handle64;
		}
		
		@Override
		public String toString() {
			if (isFreed()) {
				return "CompoundTextStandaloneBuffer [freed]";
			}
			else {
				return "CompoundTextStandaloneBuffer [handle="+(PlatformUtils.is64Bit() ? m_handle64 : m_handle32)+", size="+getSize()+"]";
			}
		}
	}
}
