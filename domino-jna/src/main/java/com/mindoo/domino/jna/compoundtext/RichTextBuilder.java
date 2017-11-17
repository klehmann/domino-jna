package com.mindoo.domino.jna.compoundtext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.structs.compoundtext.NotesCompoundStyleStruct;
import com.mindoo.domino.jna.structs.compoundtext.NotesFontIDFieldsStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * This class can be used to create basic richtext content. It uses C API methods to create
 * "CompoundText", which provide some high-level methods for richtext creation, e.g.
 * to add text, doclinks, render notes as richtext or append other richtext items.<br>
 * <br>
 * <b>After calling the available methods in the returned {@link RichTextBuilder}, you must
 * call {@link RichTextBuilder#close()} to write your changes into the note. Otherwise
 * it is discarded.</b>
 * 
 * @author Karsten Lehmann
 */
public class RichTextBuilder implements IRecyclableNotesObject {
	private NotesNote m_parentNote;
	private long m_handle64;
	private int m_handle32;
	private Map<Integer,Integer> m_definedStyleId;

	public RichTextBuilder(NotesNote parentNote, long handle) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_parentNote = parentNote;
		m_handle64 = handle;
		m_definedStyleId = new HashMap<Integer, Integer>();
	}

	public RichTextBuilder(NotesNote parentNote, int handle) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_parentNote = parentNote;
		m_handle32 = handle;
		m_definedStyleId = new HashMap<Integer, Integer>();
	}

	void checkHandle() {
		if (m_parentNote.isRecycled())
			throw new NotesError(0, "Parent note already recycled");

		if (NotesJNAContext.is64Bit()) {
			if (m_handle64==0)
				throw new NotesError(0, "Compound text already recycled");
			NotesGC.__b64_checkValidObjectHandle(RichTextBuilder.class, m_handle64);
		}
		else {
			if (m_handle32==0)
				throw new NotesError(0, "Compound text already recycled");
			NotesGC.__b32_checkValidObjectHandle(RichTextBuilder.class, m_handle32);
		}
	}

	private int getDefaultFontId() {
		return getFontId(NotesCAPI.FONT_FACE_SWISS, (byte) 0, (byte) 0, (byte) 10);
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

	/**
	 * This function inserts a DocLink for the specified {@link NotesNote}.
	 * 
	 * @param note note to create the link
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public void addDocLink(NotesNote note, String comment) {
		if (note.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		NotesDatabase parentDb = note.getParent();
		String replicaId = parentDb.getReplicaID();
		String noteUnid = note.getUNID();
		NotesCollection defaultCollection = parentDb.openDefaultCollection();
		addDocLink(replicaId, defaultCollection.getUNID(), noteUnid, comment);
	}

	/**
	 * This function inserts a DocLink using manual values.
	 * 
	 * @param dbReplicaId Replica ID of the database that contains the note (document) pointed to by the DocLink.
	 * @param viewUnid UNID of the view that contains the note (document) pointed to by the DocLink.
	 * @param noteUNID UNID of the note (document) pointed to by the DocLink.
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public void addDocLink(String dbReplicaId, String viewUnid, String noteUNID, String comment) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

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

		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddDocLink(m_handle64, dbReplicaIdStructByVal, viewUNIDStructByVal, noteUNIDStructByVal, commentMem, 0);
		}
		else {
			result = notesAPI.b32_CompoundTextAddDocLink(m_handle32, dbReplicaIdStructByVal, viewUNIDStructByVal, noteUNIDStructByVal, commentMem, 0);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * The function will render the specified note in Composite Data format (i.e., richtext) and add the rendered
	 * note to the data created by this builder.<br>
	 * This allows an editable copy of an entire note to be embedded in another note.
	 * 
	 * @param note note to render
	 */
	public void addRenderedNote(NotesNote note) {
		String formName = note.getItemValueString("Form");

		addRenderedNote(note, formName);
	}

	/**
	 * The function will render the specified note in Composite Data format (i.e., richtext) and add the rendered
	 * note to the data created by this builder.<br>
	 * This allows an editable copy of an entire note to be embedded in another note.
	 * 
	 * @param note note to render
	 * @param form name of form used to render the note, null for default form
	 */
	public void addRenderedNote(NotesNote note, String form) {
		if (note.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesDatabase db = note.getParent();
		NotesNote formNote = StringUtil.isEmpty(form) ? null : db.findDesignNote(form, NoteClass.FORM);

		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddRenderedNote(m_handle64, note.getHandle64(), formNote==null ? 0 : formNote.getHandle64(), 0);
		}
		else {
			result = notesAPI.b64_CompoundTextAddRenderedNote(m_handle32, note.getHandle32(), formNote==null ? 0 : formNote.getHandle32(), 0);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Adds text with default text and font style
	 * 
	 * @param txt text to add
	 */
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
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
			IntByReference retStyleId = new IntByReference();

			NotesCompoundStyleStruct styleStruct = NotesCompoundStyleStruct.newInstance();
			notesAPI.CompoundTextInitStyle(styleStruct);
			styleStruct.read();
			styleStruct.JustifyMode = style.m_justifyMode;
			styleStruct.LineSpacing = style.m_lineSpacing;
			styleStruct.ParagraphSpacingBefore = style.m_paragraphSpacingBefore;
			styleStruct.ParagraphSpacingAfter = style.m_paragraphSpacingAfter;
			styleStruct.LeftMargin = style.m_leftMargin;
			styleStruct.RightMargin = style.m_rightMargin;
			styleStruct.FirstLineLeftMargin = style.m_firstLineLeftMargin;
			styleStruct.Tabs = style.m_tabs;
			styleStruct.Tab = style.m_tab==null ? null : style.m_tab.clone();
			styleStruct.Flags = style.m_flags;
			styleStruct.write();

			Memory styleNameMem = NotesStringUtils.toLMBCS(style.m_styleName, true);

			short result;
			if (NotesJNAContext.is64Bit()) {
				result = notesAPI.b64_CompoundTextDefineStyle(m_handle64, styleNameMem, styleStruct, retStyleId);
			}
			else {
				result = notesAPI.b32_CompoundTextDefineStyle(m_handle32, styleNameMem, styleStruct, retStyleId);

			}
			NotesErrorUtils.checkResult(result);
			styleId = retStyleId.getValue();

			m_definedStyleId.put(styleHash, styleId);
		}
		return styleId;
	}

	/**
	 * Adds text with the specified text and font style
	 * 
	 * @param txt text to add
	 * @param textStyle text style
	 * @param fontStyle font style
	 */
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory txtMem = NotesStringUtils.toLMBCS(txt, false);
		Memory lineDelimMem = new Memory(3);
		lineDelimMem.setByte(0, (byte) '\r'); 
		lineDelimMem.setByte(1, (byte) '\n'); 
		lineDelimMem.setByte(2, (byte) 0);

		int fontId = fontStyle==null ? getDefaultFontId() : fontStyle.getFontId();
		int dwStyleID = textStyle==null ? NotesCAPI.STYLE_ID_SAMEASPREV : getStyleId(textStyle);

		Pointer nlsInfoPtr = notesAPI.OSGetLMBCSCLS();
		short result;
		int dwFlags = NotesCAPI.COMP_PRESERVE_LINES | NotesCAPI.COMP_PARA_LINE | NotesCAPI.COMP_PARA_BLANK_LINE;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddTextExt(m_handle64, dwStyleID, fontId, txtMem, (int) txtMem.size(),
					lineDelimMem, dwFlags, nlsInfoPtr);
		}
		else {
			result = notesAPI.b32_CompoundTextAddTextExt(m_handle32, dwStyleID, fontId, txtMem, (int) txtMem.size(),
					lineDelimMem, dwFlags, nlsInfoPtr);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This routine assimilates the contents of one or more named richtext items (type TYPE_COMPOSITE) from a document
	 * into the item to be created by this class.<br>
	 * <br>
	 * The contents are assimilated in that PABIDs and styles are fixed up (renumbered/renamed as needed)
	 * before they are appended to the CompoundText context.<br>
	 * <br>
	 * Note: This function does not handle features of rich text that depend on special items that reside outside
	 * the rich text field specified by the "itemName" parameter.<br>
	 * Two such features of rich text include doc links that depend on the special $Links item, and font faces
	 * that depend on the special $Fonts item. These special items in the note reside outside the
	 * specified rich text field.<br>
	 * Therefore, when this method merges a rich text field containing doc links consisting of CDLINK2 records
	 * into a Compound Text context, the doc link in the resulting compound text does not function.
	 * A work-around to this problem is demonstrated by sample program HISTORY in the RICHTEXT directory.<br>
	 * <br>
	 * Also, when this method merges a rich text field containing font faces that require a font
	 * table (a $Fonts item in the note)  the resulting compound text does not reflect the font face
	 * used in the original field.
	 * 
	 * @param otherNote note containing the item to append
	 * @param itemName name of item to append
	 */
	public void addCompoundText(NotesNote otherNote, String itemName) {
		if (otherNote.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAssimilateItem(m_handle64, otherNote.getHandle64(), itemNameMem, 0);
		}
		else {
			result = notesAPI.b32_CompoundTextAssimilateItem(m_handle32, otherNote.getHandle32(), itemNameMem, 0);
		}
		NotesErrorUtils.checkResult(result);
	}

	public enum ImageType {GIF, JPEG, BMP}
	
	/**
	 * Adds an image to the richtext item
	 * 
	 * @param f image file
	 * @throws IOException
	 */
	public void addImage(File f) throws IOException {
		addImage(-1, -1, f);
	}
	
	/**
	 * Adds an image to the richtext item. We support GIF, JPEG and BMP files.
	 * 
	 * @param resizeToWidth if not -1, resize the image to this width
	 * @param resizeToHeight if not -1, resize the image to this width
	 * @param f image file
	 * @throws IOException
	 */
	public void addImage(int resizeToWidth, int resizeToHeight, File f) throws IOException {
		FileInputStream fIn = new FileInputStream(f);
		try {
			addImage(resizeToWidth, resizeToHeight, (int) f.length(), fIn);
		}
		finally {
			fIn.close();
		}
	}
	
	/**
	 * Adds an image to the richtext item
	 * 
	 * @param fileSize total size of image data
	 * @param imageData image data as bytestream
	 * @throws IOException
	 */
	public void addImage(int fileSize, InputStream imageData) throws IOException {
		addImage(-1, -1, fileSize, imageData);
	}
	
	/**
	 * Adds an image to the richtext item
	 * 
	 * @param resizeToWidth if not -1, resize the image to this width
	 * @param resizeToHeight if not -1, resize the image to this width
	 * @param fileSize total size of image data
	 * @param imageData image data as bytestream
	 * @throws IOException
	 */
	public void addImage(int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException {
		checkHandle();
		
		BufferedInputStream innerBufIn = new BufferedInputStream(imageData, 3000);
		innerBufIn.mark(3000);
		BufferedInputStream outerBufIn = new BufferedInputStream(innerBufIn, 3000);
		
		FileType fileType = FileTypeDetector.detectFileType(outerBufIn);
		if (fileType == FileType.Unknown)
			throw new NotesError(0, "Unable to detect filetype of image");
		innerBufIn.reset();
		
		boolean isSupported = false;
		if (fileType == FileType.Gif || fileType == FileType.Jpeg || fileType == FileType.Bmp) {
			isSupported = true;
		}
		
		if (!isSupported)
			throw new NotesError(0, "Unsupported filetype "+fileType+". Only GIF, JPEG and BMP are supported");
		
		outerBufIn.mark(3000);
		Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(outerBufIn);
		} catch (ImageProcessingException e) {
			throw new NotesError(0, "Unable to read image metadata", e);
		}
		innerBufIn.reset();

		int imgWidth = -1;
		int imgHeight = -1;
		
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
					imgWidth = bmpHeader.getInt(bmpHeader.TAG_IMAGE_WIDTH);
					imgHeight = bmpHeader.getInt(bmpHeader.TAG_IMAGE_HEIGHT);
				} catch (MetadataException e) {
					throw new NotesError(0, "Error reading BMP image size", e);
				}
			}
			break;
		default:
			//
		}
		
		if (imgWidth<=0 || imgHeight<=0) {
			throw new IllegalArgumentException("Width/Height must be specified");
		}
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		//write graphic header
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
		
//		Read record GRAPHIC (153) with 22 data bytes, cdrecord length: 28
//		Data:
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 01 00 00 00      ]
				
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
		graphicMem.setShort(0, NotesCAPI.SIG_CD_GRAPHIC);
		graphicMem.share(2).setInt(0, (int) graphicMem.size());
		
		boolean isResized = resizeToWidth!=-1 && resizeToWidth!=-1;
		
		// DestSize : RECTSIZE (Word/Word)
		if (isResized) {
			graphicMem.share(6).setShort(0, (short) (resizeToWidth & 0xffff));
			graphicMem.share(6 + 2).setShort(0, (short) (resizeToWidth & 0xffff));
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
		graphicMem.share(6 + 18).setByte(0, NotesCAPI.CDGRAPHIC_VERSION3);
		
		//Flags:
		graphicMem.share(6 + 19).setByte(0, (byte) NotesCAPI.CDGRAPHIC_FLAG_DESTSIZE_IS_PIXELS);
		//Reserved:
		graphicMem.share(6 + 20).setShort(0, (short) 0);
		
		
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddCDRecords(m_handle64, graphicMem, (int) graphicMem.size());
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = notesAPI.b32_CompoundTextAddCDRecords(m_handle32, graphicMem, (int) graphicMem.size());
			NotesErrorUtils.checkResult(result);
		}

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

//		[01 00 6e 01 2c 01 1f 41]   [..n.,..A]
//		[00 00 02 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00      ]   [......  ]
						
		Memory imageHeaderMem = new Memory(
				2+4 + //LSIG
				2 + //ImageType
				2 + //Width
				2 + //Height
				4 + //ImageDataSize
				4 + //SegCount
				4 + //Flags
				4  //Reserved
				);
		imageHeaderMem.setShort(0, NotesCAPI.SIG_CD_IMAGEHEADER);
		imageHeaderMem.share(2).setInt(0, (int) imageHeaderMem.size());
		short imageTypeShort;
		switch (fileType) {
		case Gif:
			imageTypeShort = NotesCAPI.CDIMAGETYPE_GIF;
			break;
		case Jpeg:
			imageTypeShort = NotesCAPI.CDIMAGETYPE_JPEG;
			break;
		case Bmp:
			imageTypeShort = NotesCAPI.CDIMAGETYPE_BMP;
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
		
		imageHeaderMem.share(12).setInt(0, fileSize);
		
		int fullSegments = (int) (fileSize / MAX_SEGMENT_SIZE);
		int segments = fullSegments;
		int dataBytesInLastSegment = fileSize - fullSegments * MAX_SEGMENT_SIZE;
		if (dataBytesInLastSegment>0) {
			segments++;
		}
		
		imageHeaderMem.share(16).setInt(0, segments & 0xffff);
		
		imageHeaderMem.share(20).setInt(0, 0); //flags
		imageHeaderMem.share(24).setInt(0, 0); //reserved

		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddCDRecords(m_handle64, imageHeaderMem, (int) imageHeaderMem.size());
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = notesAPI.b32_CompoundTextAddCDRecords(m_handle32, imageHeaderMem, (int) imageHeaderMem.size());
			NotesErrorUtils.checkResult(result);
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
			
			imageSegMem.setShort(0, NotesCAPI.SIG_CD_IMAGESEGMENT); // LSIG
			imageSegMem.share(2).setInt(0, (int) imageSegMem.size()); // LSIG
			imageSegMem.share(6).setShort(0, (short) (len & 0xffff)); // DataSize
			imageSegMem.share(8).setShort(0, (short) (segSize & 0xffff)); // SegSize
			imageSegMem.share(10).write(0, buf, 0, len); // Data
			
			if (NotesJNAContext.is64Bit()) {
				result = notesAPI.b64_CompoundTextAddCDRecords(m_handle64, imageSegMem, (int) imageSegMem.size());
				NotesErrorUtils.checkResult(result);
			}
			else {
				result = notesAPI.b32_CompoundTextAddCDRecords(m_handle32, imageSegMem, (int) imageSegMem.size());
				NotesErrorUtils.checkResult(result);
			}
		}
	}
	
	/**
	 * This routine closes the build process. Use {@link NotesNote#update()} 
	 * after {@link #close()} to update and save the document.
	 */
	public void close() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		short result;
		if (NotesJNAContext.is64Bit()) {
			NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
			result = notesAPI.b64_CompoundTextClose(m_handle64, null, null, null, (short) 0);
			m_handle64 = 0;
		}
		else {
			NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
			result = notesAPI.b32_CompoundTextClose(m_handle32, null, null, null, (short) 0);
			m_handle32 = 0;
		}
		NotesErrorUtils.checkResult(result);
	}

	@Override
	public void recycle() {
		if (isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			if (m_handle64!=0) {
				notesAPI.b64_CompoundTextDiscard(m_handle64);
				NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
				m_handle64=0;
			}
		}
		else {
			if (m_handle32!=0) {
				notesAPI.b32_CompoundTextDiscard(m_handle32);
				NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
				m_handle32=0;
			}
		}
	}

	@Override
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
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
}
