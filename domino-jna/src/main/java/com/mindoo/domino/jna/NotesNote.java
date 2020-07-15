package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.mindoo.domino.jna.NotesItem.ICompositeCallbackDirect;
import com.mindoo.domino.jna.NotesMIMEPart.PartType;
import com.mindoo.domino.jna.NotesNote.IItemCallback.Action;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.constants.Compression;
import com.mindoo.domino.jna.constants.ItemType;
import com.mindoo.domino.jna.constants.MimePartOptions;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.LotusScriptCompilationError;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.errors.UnsupportedItemValueError;
import com.mindoo.domino.jna.formula.FormulaExecution;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.html.CommandId;
import com.mindoo.domino.jna.html.IHtmlApiReference;
import com.mindoo.domino.jna.html.IHtmlApiUrlTargetComponent;
import com.mindoo.domino.jna.html.IHtmlConversionResult;
import com.mindoo.domino.jna.html.IHtmlImageRef;
import com.mindoo.domino.jna.html.ReferenceType;
import com.mindoo.domino.jna.html.TargetType;
import com.mindoo.domino.jna.internal.CalNoteOpenData32;
import com.mindoo.domino.jna.internal.CalNoteOpenData64;
import com.mindoo.domino.jna.internal.CollationDecoder;
import com.mindoo.domino.jna.internal.CompoundTextWriter;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.ReadOnlyMemory;
import com.mindoo.domino.jna.internal.ViewFormatDecoder;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.structs.NoteIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesFileObjectStruct;
import com.mindoo.domino.jna.internal.structs.NotesLSCompileErrorInfoStruct;
import com.mindoo.domino.jna.internal.structs.NotesMIMEPartStruct;
import com.mindoo.domino.jna.internal.structs.NotesNumberPairStruct;
import com.mindoo.domino.jna.internal.structs.NotesObjectDescriptorStruct;
import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesRangeStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.mindoo.domino.jna.internal.structs.html.HTMLAPIReference32Struct;
import com.mindoo.domino.jna.internal.structs.html.HTMLAPIReference64Struct;
import com.mindoo.domino.jna.internal.structs.html.HtmlApi_UrlTargetComponentStruct;
import com.mindoo.domino.jna.mime.MimeConversionControl;
import com.mindoo.domino.jna.richtext.FieldInfo;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.IRichTextNavigator.RichTextNavPosition;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.richtext.StandaloneRichText;
import com.mindoo.domino.jna.richtext.conversion.IRichTextConversion;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.ListUtil;
import com.mindoo.domino.jna.utils.Loop;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.Ref;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * Object wrapping a Notes document / note
 * 
 * @author Karsten Lehmann
 */
public class NotesNote implements IRecyclableNotesObject {
	private int m_hNote32;
	private long m_hNote64;
	private boolean m_noRecycle;
	private NotesDatabase m_parentDb;
	private Document m_legacyDocRef;
	private EnumSet<NoteClass> m_noteClass;
	private boolean m_preferNotesTimeDates;
	
	/**
	 * Creates a new instance
	 * 
	 * @param parentDb parent database
	 * @param hNote handle
	 */
	NotesNote(NotesDatabase parentDb, int hNote) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_parentDb = parentDb;
		m_hNote32 = hNote;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param parentDb parent database
	 * @param hNote handle
	 */
	NotesNote(NotesDatabase parentDb, long hNote) {
		if (!PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_parentDb = parentDb;
		m_hNote64 = hNote;
	}

	/**
	 * Creates a new NotesNote
	 * 
	 * @param adaptable adaptable providing enough information to create the database
	 */
	public NotesNote(IAdaptable adaptable) {
		Document legacyDoc = adaptable.getAdapter(Document.class);
		if (legacyDoc!=null) {
			if (isRecycled(legacyDoc))
				throw new NotesError(0, "Legacy database already recycled");
			
			long docHandle = LegacyAPIUtils.getDocHandle(legacyDoc);
			if (docHandle==0)
				throw new NotesError(0, "Could not read db handle");
			
			if (PlatformUtils.is64Bit()) {
				m_hNote64 = docHandle;
			}
			else {
				m_hNote32 = (int) docHandle;
			}
			setNoRecycle();
			m_legacyDocRef = legacyDoc;
			
			Database legacyDb;
			try {
				legacyDb = legacyDoc.getParentDatabase();
			} catch (NotesException e1) {
				throw new NotesError(0, "Could not read parent legacy db from document", e1);
			}
			long dbHandle = LegacyAPIUtils.getDBHandle(legacyDb);
			try {
				if (PlatformUtils.is64Bit()) {
					m_parentDb = (NotesDatabase) NotesGC.__b64_checkValidObjectHandle(NotesDatabase.class, dbHandle);
				}
				else {
					m_parentDb = (NotesDatabase) NotesGC.__b32_checkValidObjectHandle(NotesDatabase.class, (int) dbHandle);
				}
			} catch (NotesError e) {
				m_parentDb = LegacyAPIUtils.toNotesDatabase(legacyDb);
			}
			return;
		}
		if (PlatformUtils.is64Bit()) {
			CalNoteOpenData64 calOpenNote = adaptable.getAdapter(CalNoteOpenData64.class);
			if (calOpenNote!=null) {
				m_parentDb = calOpenNote.getDb();
				m_hNote64 = calOpenNote.getNoteHandle();
				return;
			}
		}
		else {
			CalNoteOpenData32 calOpenNote = adaptable.getAdapter(CalNoteOpenData32.class);
			if (calOpenNote!=null) {
				m_parentDb = calOpenNote.getDb();
				m_hNote32 = calOpenNote.getNoteHandle();
				return;
			}
		}
		throw new NotesError(0, "Unsupported adaptable parameter");
	}

	private boolean isRecycled(Document doc) {
		try {
			//call any method to check recycled state
			doc.hasItem("~-~-~-~-~-~");
		}
		catch (NotesException e) {
			if (e.id==4376 || e.id==4466)
				return true;
		}
		return false;
	}

	/**
	 * Converts a legacy {@link lotus.domino.Document} to a
	 * {@link NotesNote}.
	 * 
	 * @param doc document to convert
	 * @return note
	 */
	public static NotesNote toNote(final Document doc) {
		return LegacyAPIUtils.toNotesNote(doc);
	}
	
	/**
	 * Converts this note to a legacy {@link Document}
	 * 
	 * @param db parent database
	 * @return document
	 */
	public Document toDocument(Database db) {
		if (PlatformUtils.is64Bit()) {
			return LegacyAPIUtils.createDocument(db, m_hNote64);
		}
		else {
			return LegacyAPIUtils.createDocument(db, m_hNote32);
		}
	}
	
	/**
	 * Returns the parent database
	 * 
	 * @return database
	 */
	public NotesDatabase getParent() {
		return m_parentDb;
	}
	
	/**
	 * Returns the note id of the note
	 * 
	 * @return note id
	 */
	public int getNoteId() {
		checkHandle();
		
		Memory retNoteId = new Memory(4);
		retNoteId.clear();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_ID, retNoteId);
		}
		else {
			NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_ID, retNoteId);
		}
		return retNoteId.getInt(0);
	}
	
	/**
	 * Method to check whether a note has already been saved
	 * 
	 * @return true if yet unsaved
	 */
	public boolean isNewNote() {
		return getNoteId() == 0;
	}
	
	/**
	 * Returns the note class of the note
	 * 
	 * @return note class
	 */
	public EnumSet<NoteClass> getNoteClass() {
		if (m_noteClass==null) {
			checkHandle();
			
			Memory retNoteClass = new Memory(2);
			retNoteClass.clear();
			
			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_CLASS, retNoteClass);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_CLASS, retNoteClass);
			}
			int noteClassMask = retNoteClass.getShort(0);
			m_noteClass = NoteClass.toNoteClasses(noteClassMask);
		}
		return m_noteClass;
	}
	
	/**
	 * Changes the note class for this note
	 * 
	 * @param noteClass new note class
	 */
	public void setNoteClass(EnumSet<NoteClass> noteClass) {
		checkHandle();

		EnumSet<NoteClass> noteClassToWrite = noteClass.clone();
		noteClassToWrite.remove(NoteClass.ALL);
		noteClassToWrite.remove(NoteClass.ALLNONDATA);
		
		short noteClassToWriteAsShort = NoteClass.toBitMask(noteClassToWrite);
		
		DisposableMemory noteClassMem = new DisposableMemory(2);
		noteClassMem.setShort(0, noteClassToWriteAsShort);
		
		try {
			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteSetInfo(m_hNote64, NotesConstants._NOTE_CLASS, noteClassMem);
			}
			else {
				NotesNativeAPI32.get().NSFNoteSetInfo(m_hNote32, NotesConstants._NOTE_CLASS, noteClassMem);
			}
		}
		finally {
			noteClassMem.dispose();
		}
	}
	
	/**
	 * Converts the value of {@link #getNoteId()} to a hex string in uppercase
	 * format
	 * 
	 * @return note id as hex string
	 */
	public String getNoteIdAsString() {
		return Integer.toString(getNoteId(), 16).toUpperCase();
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesNote [recycled]";
		}
		else {
			return "NotesNote [handle="+(PlatformUtils.is64Bit() ? m_hNote64 : m_hNote32)+", noteid="+getNoteId()+"]";
		}
	}
	
	/**
	 * Returns the UNID of the note
	 * 
	 * @return UNID
	 */
	public String getUNID() {
		NotesOriginatorIdStruct oid = getOIDStruct();
		String unid = oid.getUNIDAsString();
		return unid;
	}

	/**
	 * Internal method to get the populated {@link NotesOriginatorIdStruct} object
	 * for this note
	 * 
	 * @return oid structure
	 */
	private NotesOriginatorIdStruct getOIDStruct() {
		checkHandle();
		
		Memory retOid = new Memory(NotesConstants.oidSize);
		retOid.clear();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_OID, retOid);
		}
		else {
			NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_OID, retOid);
		}
		NotesOriginatorIdStruct oidStruct = NotesOriginatorIdStruct.newInstance(retOid);
		oidStruct.read();
		
		return oidStruct;
	}
	
	/**
	 * Returns the {@link NotesOriginatorId} for this note
	 * 
	 * @return oid
	 */
	public NotesOriginatorId getOID() {
		NotesOriginatorIdStruct oidStruct = getOIDStruct();
		NotesOriginatorId oid = new NotesOriginatorId(oidStruct);
		return oid;
	}
	
	/**
	 * Sets a new UNID in the {@link NotesOriginatorId} for this note
	 * 
	 * @param newUnid new universal id
	 */
	public void setUNID(String newUnid) {
		checkHandle();

		DisposableMemory retOid = new DisposableMemory(NotesConstants.oidSize);
		try {
			retOid.clear();

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_OID, retOid);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_OID, retOid);
			}
			NotesOriginatorIdStruct oidStruct = NotesOriginatorIdStruct.newInstance(retOid);
			oidStruct.read();
			oidStruct.setUNID(newUnid);
			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteSetInfo(m_hNote64, NotesConstants._NOTE_OID, retOid);
			}
			else {
				NotesNativeAPI32.get().NSFNoteSetInfo(m_hNote32, NotesConstants._NOTE_OID, retOid);
			}
		}
		finally {
			retOid.dispose();
		}
	}
	
	/**
	 * Returns the last modified date of the note
	 * 
	 * @return last modified date
	 */
	public Calendar getLastModified() {
		NotesTimeDate td = getLastModifiedAsTimeDate();
		return td==null ? null : td.toCalendar();
	}

	/**
	 * Returns the last modified date of the note
	 * 
	 * @return last modified date as {@link NotesTimeDate}
	 */
	public NotesTimeDate getLastModifiedAsTimeDate() {
		checkHandle();

		DisposableMemory retModified = new DisposableMemory(NotesConstants.timeDateSize);
		try {
			retModified.clear();

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_MODIFIED, retModified);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_MODIFIED, retModified);
			}
			NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retModified);
			td.read();
			return new NotesTimeDate(td.Innards);
		}
		finally {
			retModified.dispose();
		}
	}
	
	/**
	 * Returns the creation date of this note
	 * 
	 * @return creation date
	 */
	public Calendar getCreationDate() {
		checkHandle();
		
		Calendar creationDate = getItemValueDateTime("$CREATED");
		if (creationDate!=null) {
			return creationDate;
		}
		
		NotesOriginatorIdStruct oidStruct = getOIDStruct();
		NotesTimeDateStruct creationDateStruct = oidStruct.Note;
		return creationDateStruct.toCalendar();
	}
	
	/**
	 * Returns the creation date of this note
	 * 
	 * @return creation date as {@link NotesTimeDate}
	 */
	public NotesTimeDate getCreationDateAsTimeDate() {
		checkHandle();
		
		NotesTimeDate creationDate = getItemValueAsTimeDate("$CREATED");
		if (creationDate!=null) {
			return creationDate;
		}
		
		NotesOriginatorIdStruct oidStruct = getOIDStruct();
		NotesTimeDateStruct creationDateStruct = oidStruct.Note;
		return new NotesTimeDate(creationDateStruct.Innards);
	}
	
	/**
	 * Returns the last access date of the note
	 * 
	 * @return last access date
	 */
	public Calendar getLastAccessed() {
		checkHandle();
		
		DisposableMemory retAccessed = new DisposableMemory(NotesConstants.timeDateSize);
		try {
			retAccessed.clear();

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_ACCESSED, retAccessed);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_ACCESSED, retAccessed);
			}
			NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retAccessed);
			td.read();
			Calendar cal = td.toCalendar();
			return cal;
		}
		finally {
			retAccessed.dispose();
		}
	}

	/**
	 * Returns the last access date of the note
	 * 
	 * @return last access date as {@link NotesTimeDate}
	 */
	public NotesTimeDate getLastAccessedAsTimeDate() {
		checkHandle();
		
		DisposableMemory retAccessed = new DisposableMemory(NotesConstants.timeDateSize);
		try {
			retAccessed.clear();

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_ACCESSED, retAccessed);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_ACCESSED, retAccessed);
			}
			NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retAccessed);
			td.read();
			return new NotesTimeDate(td.Innards);
		}
		finally {
			retAccessed.dispose();
		}
	}
	
	/**
	 * Returns the date/time when the note got added to the NSF instance
	 * 
	 * @return added to file time
	 */
	public Calendar getAddedToFileTime() {
		checkHandle();
		
		Memory retAddedToFile = new Memory(NotesConstants.timeDateSize);
		retAddedToFile.clear();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_ADDED_TO_FILE, retAddedToFile);
		}
		else {
			NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_ADDED_TO_FILE, retAddedToFile);
		}
		NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retAddedToFile);
		td.read();
		Calendar cal = td.toCalendar();
		return cal;
	}

	/**
	 * Returns the date/time when the note got added to the NSF instance
	 * 
	 * @return added to file time as {@link NotesTimeDate}
	 */
	public NotesTimeDate getAddedToFileTimeAsTimeDate() {
		checkHandle();
		
		DisposableMemory retAddedToFile = new DisposableMemory(NotesConstants.timeDateSize);
		try {
			retAddedToFile.clear();

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_ADDED_TO_FILE, retAddedToFile);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_ADDED_TO_FILE, retAddedToFile);
			}
			NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retAddedToFile);
			td.read();
			return new NotesTimeDate(td.Innards);
		}
		finally {
			retAddedToFile.dispose();
		}
	}
	
	/**
	 * The NOTE_FLAG_READONLY bit indicates that the note is Read-Only for the current user.<br>
	 * If a note contains an author names field, and the name of the user opening the
	 * document is not included in the author names field list, then  the NOTE_FLAG_READONLY
	 * bit is set in the note header data when that user opens the note.
	 * 
	 * @return TRUE if document cannot be updated
	 */
	public boolean isReadOnly() {
		int flags = getFlags();
		return (flags & NotesConstants.NOTE_FLAG_READONLY) == NotesConstants.NOTE_FLAG_READONLY;
	}
	
	/**
	 * The NOTE_FLAG_ABSTRACTED bit indicates that the note has been abstracted (truncated).<br>
	 * This bit may be set if the database containing the note has replication settings set to
	 * "Truncate large documents and remove attachments".
	 * 
	 * @return true if truncated
	 */
	public boolean isTruncated() {
		int flags = getFlags();
		return (flags & NotesConstants.NOTE_FLAG_ABSTRACTED) == NotesConstants.NOTE_FLAG_ABSTRACTED;
	}

	/**
	 * Method to check if this is ghost note. Ghost notes do not appear in any view or search.
	 * 
	 * @return true if ghost
	 */
	public boolean isGhost() {
		int flags = getFlags();
		return (flags & NotesConstants.NOTE_FLAG_GHOST) == NotesConstants.NOTE_FLAG_GHOST;
	}
	
	/**
	 * Changes the note's ghost flag. Ghost notes do not appear in any view or search.
	 * 
	 * @param b true if ghost
	 */
	void setGhost(boolean b) {
		short flags = getFlags();
		short newFlags;
		
		if (b) {
			if ((flags & NotesConstants.NOTE_FLAG_GHOST) == NotesConstants.NOTE_FLAG_GHOST) {
				return;
			}
			
			newFlags = (short) ((flags | NotesConstants.NOTE_FLAG_GHOST) & 0xffff);
		}
		else {
			if ((flags & NotesConstants.NOTE_FLAG_GHOST) == 0) {
				return;
			}
			
			newFlags = (short) ((flags & ~NotesConstants.NOTE_FLAG_GHOST) & 0xffff);
		}
		
		setFlags(newFlags);
	}
	
	/**
	 * Examines the items in the note and determines if they are correctly formed.
	 * 
	 * @throws NotesError if items contain errors like the overall lengths does not match the data type ({@link INotesErrorConstants#ERR_INVALID_ITEMLEN}) or an item's type is not recognized ({@link INotesErrorConstants#ERR_INVALID_ITEMTYPE})
	 */
	public void check() {
		checkHandle();

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteCheck(getHandle64());
		} else {
			result = NotesNativeAPI32.get().NSFNoteCheck(getHandle32());
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Reads the note flags (e.g. {@link NotesConstants#NOTE_FLAG_READONLY})
	 * 
	 * @return flags
	 */
	private short getFlags() {
		checkHandle();

		DisposableMemory retFlags = new DisposableMemory(2);
		try {
			retFlags.clear();

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteGetInfo(m_hNote64, NotesConstants._NOTE_FLAGS, retFlags);
			}
			else {
				NotesNativeAPI32.get().NSFNoteGetInfo(m_hNote32, NotesConstants._NOTE_FLAGS, retFlags);
			}
			short flags = retFlags.getShort(0);
			return flags;
		}
		finally {
			retFlags.dispose();
		}
	}
	
	private void setFlags(short flags) {
		checkHandle();

		DisposableMemory flagsMem = new DisposableMemory(2);
		try {
			flagsMem.setShort(0, flags);

			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFNoteSetInfo(m_hNote64, NotesConstants._NOTE_FLAGS, flagsMem);
			}
			else {
				NotesNativeAPI32.get().NSFNoteSetInfo(m_hNote32, NotesConstants._NOTE_FLAGS, flagsMem);
			}
		}
		finally {
			flagsMem.dispose();
		}
	}
	
	public void setNoRecycle() {
		m_noRecycle=true;
	}
	
	@Override
	public boolean isNoRecycle() {
		return m_noRecycle;
	}
	
	@Override
	public void recycle() {
		if (m_noRecycle || isRecycled())
			return;

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteClose(m_hNote64);
			NotesErrorUtils.checkResult(result);
			NotesGC.__objectBeeingBeRecycled(NotesNote.class, this);
			m_hNote64=0;
			
			DisposableMemory retStrBufMem = stringretBuffer.get();
			if (retStrBufMem!=null) {
				if (!retStrBufMem.isDisposed()) {
					retStrBufMem.dispose();
				}
				stringretBuffer.set(null);
			}
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteClose(m_hNote32);
			NotesErrorUtils.checkResult(result);
			NotesGC.__objectBeeingBeRecycled(NotesNote.class, this);
			m_hNote32=0;
			
			DisposableMemory retStrBufMem = stringretBuffer.get();
			if (retStrBufMem!=null) {
				if (!retStrBufMem.isDisposed()) {
					retStrBufMem.dispose();
				}
				stringretBuffer.set(null);
			}
		}
	}

	@Override
	public boolean isRecycled() {
		if (m_legacyDocRef!=null && isRecycled(m_legacyDocRef)) {
			return true;
		}

		if (PlatformUtils.is64Bit()) {
			return m_hNote64==0;
		}
		else {
			return m_hNote32==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hNote32;
	}

	@Override
	public long getHandle64() {
		return m_hNote64;
	}

	void checkHandle() {
		if (m_legacyDocRef!=null) {
			if (isRecycled(m_legacyDocRef)) {
				throw new NotesError(0, "Wrapped legacy document already recycled");
			}
			else {
				//note not registered in our GC, so skip the following handle check
				return;
			}
		}
		
		if (PlatformUtils.is64Bit()) {
			if (m_hNote64==0)
				throw new NotesError(0, "Note already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesNote.class, m_hNote64);
		}
		else {
			if (m_hNote32==0)
				throw new NotesError(0, "Note already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesNote.class, m_hNote32);
		}
	}

	/**
	 * Unsigns the note. This function removes the $Signature item from the note.
	 */
	public void unsign() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFNoteUnsign(m_hNote64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFNoteUnsign(m_hNote32);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function writes the in-memory version of a note to its database.<br>
	 * <br>
	 * Prior to issuing this call, a new note (or changes to a note) are not a part
	 * of the on-disk database.<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described
	 * in the entry {@link UpdateNote}.<br>
	 * <br>
	 * You should also consider updating the collections associated with other Views
	 * in a database via the function {@link NotesCollection#update()},
	 * if you have added and/or deleted a substantial number of documents.<br>
	 * <br>
	 * If the Server's Indexer Task does not rebuild the collections associated with the database's Views,
	 * the first user to access a View in the modified database might experience an inordinant
	 * delay, while the collection is rebuilt by the Notes Workstation (locally) or
	 * Server Application (remotely).

	 * @param updateFlags flags
	 */
	public void update(EnumSet<UpdateNote> updateFlags) {
		if (checkForProfileAndUpdate()) {
			return;
		}
		checkHandle();
		
		int updateFlagsBitmask = UpdateNote.toBitMaskForUpdateExt(updateFlags);
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFNoteUpdateExtended(m_hNote64, updateFlagsBitmask);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFNoteUpdateExtended(m_hNote32, updateFlagsBitmask);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Checks if this note is a profile. If it is, we use a different C method
	 * to update it in the database and also update the profile cache.
	 * 
	 * @return true if note is a profile note that has been saved, false otherwise
	 */
	private boolean checkForProfileAndUpdate() {
		checkHandle();
		
		if (!isGhost()) {
			return false;
		}
		
		String[] profileNameAndUsername = parseProfileAndUserName();
		
		if (profileNameAndUsername==null) {
			return false;
		}
		String profileName = profileNameAndUsername[0];
		String profileUsername = profileNameAndUsername[1];
		
		Memory profileNameMem = NotesStringUtils.toLMBCS(profileName, false);
		Memory userNameMem = StringUtil.isEmpty(profileUsername) ? null : NotesStringUtils.toLMBCS(profileUsername, false);

		//NSFProfileUpdate updates the profile cache
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFProfileUpdate(getHandle64(),
					profileNameMem, (short) (profileNameMem.size() & 0xffff), userNameMem,
					(short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)));
		}
		else {
			result = NotesNativeAPI32.get().NSFProfileUpdate(getHandle32(),
					profileNameMem, (short) (profileNameMem.size() & 0xffff), userNameMem,
					(short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)));
		}
		NotesErrorUtils.checkResult(result);

		return true;
	}
	
	/**
	 * This function writes the in-memory version of a note to its database.<br>
	 * Prior to issuing this call, a new note (or changes to a note) are not a part of
	 * the on-disk database.<br>
	 * <br>
	 * You should also consider updating the collections associated with other Views in a database
	 * via the function
	 * {@link NotesCollection#update()}, if you have added and/or deleted a substantial number of documents.<br>
	 * If the Server's Indexer Task does not rebuild the collections associated with
	 * the database's Views, the first user to access a View in the modified database
	 * might experience an inordinant delay, while the collection is rebuilt by the
	 * Notes Workstation (locally) or Server Application (remotely).<br>
	 * <br>
	 * Do not update notes to disk that contain invalid items.<br>
	 * An example of an invalid item is a view design note that has a $Collation item
	 * whose BufferSize member is set to zero.<br>
	 * This update method may return an error for an invalid item that was not caught
	 * in a previous release of Domino or Notes.<br>
	 * Note: if you have enabled IMAP on a database, in the case of the
	 * special NoteID "NOTEID_ADD_OR_REPLACE", a new note is always created.
	 * 
	 */
	public void update() {
		update(EnumSet.noneOf(UpdateNote.class));
	}

	/**
	 * The method checks whether an item exists
	 * 
	 * @param itemName item name
	 * @return true if the item exists
	 */
	public boolean hasItem(String itemName) {
		checkHandle();
		
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFItemInfo(m_hNote64, itemNameMem, (short) (itemNameMem.size() & 0xffff), null, null, null, null);
		}
		else {
			result = NotesNativeAPI32.get().NSFItemInfo(m_hNote32, itemNameMem, (short) (itemNameMem.size() & 0xffff), null, null, null, null);
		}
		return result == 0;	
	}
	
	/**
	 * The function NSFNoteHasComposite returns TRUE if the given note contains any TYPE_COMPOSITE items.
	 * 
	 * @return true if composite
	 */
	public boolean hasComposite() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			return NotesNativeAPI64.get().NSFNoteHasComposite(m_hNote64) == 1;
		}
		else {
			return NotesNativeAPI32.get().NSFNoteHasComposite(m_hNote32) == 1;
		}
	}
	
	/**
	 * The function NSFNoteHasMIME returns TRUE if the given note contains either {@link NotesItem#TYPE_RFC822_TEXT}
	 * items or {@link NotesItem#TYPE_MIME_PART} items.
	 * 
	 * @return true if mime
	 */
	public boolean hasMIME() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			return NotesNativeAPI64.get().NSFNoteHasMIME(m_hNote64) == 1;
		}
		else {
			return NotesNativeAPI32.get().NSFNoteHasMIME(m_hNote32) == 1;
		}
	}
	
	/**
	 * The function returns TRUE if the given note contains any {@link NotesItem#TYPE_MIME_PART} items.
	 * 
	 * @return true if mime part
	 */
	public boolean hasMIMEPart() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			return NotesNativeAPI64.get().NSFNoteHasMIMEPart(m_hNote64) == 1;
		}
		else {
			return NotesNativeAPI32.get().NSFNoteHasMIMEPart(m_hNote32) == 1;
		}
	}
	
	/**
	 * The function returns TRUE if the given note contains any items with reader access flag
	 * 
	 * @return true if readers field
	 */
	public boolean hasReadersField() {
		checkHandle();
		
		NotesBlockIdStruct blockId = NotesBlockIdStruct.newInstance();
		boolean hasReaders;
		
		//use an optimized call to search for reader fields
		if (PlatformUtils.is64Bit()) {
			hasReaders = NotesNativeAPI64.get().NSFNoteHasReadersField(m_hNote64, blockId) == 1;
		}
		else {
			hasReaders = NotesNativeAPI32.get().NSFNoteHasReadersField(m_hNote32, blockId) == 1;
		}
		
		return hasReaders;
	}
	
	/**
	 * The function returns all the readers items of the note
	 * 
	 * @return array with readers fields
	 */
	public List<NotesItem> getReadersFields() {
		checkHandle();
		
		NotesBlockIdStruct blockId = NotesBlockIdStruct.newInstance();
		boolean hasReaders;
		
		//use an optimized call to find the first readers field
		if (PlatformUtils.is64Bit()) {
			hasReaders = NotesNativeAPI64.get().NSFNoteHasReadersField(m_hNote64, blockId) == 1;
		}
		else {
			hasReaders = NotesNativeAPI32.get().NSFNoteHasReadersField(m_hNote32, blockId) == 1;
		}
		
		if (!hasReaders)
			return Collections.emptyList();
		
		List<NotesItem> readerFields = new ArrayList<NotesItem>();
		
		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = blockId.pool;
		itemBlockIdByVal.block = blockId.block;
		
		ByteByReference retSeqByte = new ByteByReference();
		ByteByReference retDupItemID = new ByteByReference();

		Memory item_name = new Memory(NotesConstants.MAXUSERNAME);
		ShortByReference retName_len = new ShortByReference();
		ShortByReference retItem_flags = new ShortByReference();
		ShortByReference retDataType = new ShortByReference();
		IntByReference retValueLen = new IntByReference();

		NotesBlockIdStruct retValueBid = NotesBlockIdStruct.newInstance();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			
			NotesNativeAPI64.get().NSFItemQueryEx(m_hNote64,
					itemBlockIdByVal, item_name, (short) (item_name.size() & 0xffff), retName_len,
					retItem_flags, retDataType, retValueBid, retValueLen, retSeqByte, retDupItemID);
		}
		else {
			NotesNativeAPI32.get().NSFItemQueryEx(m_hNote32,
					itemBlockIdByVal, item_name, (short) (item_name.size() & 0xffff), retName_len,
					retItem_flags, retDataType, retValueBid, retValueLen, retSeqByte, retDupItemID);
		}

		NotesBlockIdStruct itemBlockIdForItemCreation = NotesBlockIdStruct.newInstance();
		itemBlockIdForItemCreation.pool = itemBlockIdByVal.pool;
		itemBlockIdForItemCreation.block = itemBlockIdByVal.block;
		itemBlockIdForItemCreation.write();
		
		if ((retItem_flags.getValue() & NotesConstants.ITEM_READERS) == NotesConstants.ITEM_READERS) {
			NotesItem firstItem = new NotesItem(this, itemBlockIdForItemCreation, (int) (retDataType.getValue() & 0xffff),
					retValueBid);
			readerFields.add(firstItem);
		}

		//now search for more items with readers flag
		while (true) {
			IntByReference retNextValueLen = new IntByReference();
			
			NotesBlockIdStruct retItemBlockId = NotesBlockIdStruct.newInstance();
			
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFItemInfoNext(m_hNote64, itemBlockIdByVal,
						null, (short) 0, retItemBlockId, retDataType,
						retValueBid, retNextValueLen);
			}
			else {
				result = NotesNativeAPI32.get().NSFItemInfoNext(m_hNote32, itemBlockIdByVal,
						null, (short) 0, retItemBlockId, retDataType,
						retValueBid, retNextValueLen);
			}

			if (result == INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
				return readerFields;
			}

			NotesErrorUtils.checkResult(result);

			itemBlockIdForItemCreation = NotesBlockIdStruct.newInstance();
			itemBlockIdForItemCreation.pool = retItemBlockId.pool;
			itemBlockIdForItemCreation.block = retItemBlockId.block;
			itemBlockIdForItemCreation.write();
			
			NotesBlockIdStruct valueBlockIdClone = NotesBlockIdStruct.newInstance();
			valueBlockIdClone.pool = retValueBid.pool;
			valueBlockIdClone.block = retValueBid.block;
			valueBlockIdClone.write();
			
			short dataType = retDataType.getValue();

			NotesItem newItem = new NotesItem(this, itemBlockIdForItemCreation, dataType,
					valueBlockIdClone);
			if (newItem.isReaders()) {
				readerFields.add(newItem);
			}
			
			itemBlockIdByVal.pool = retItemBlockId.pool;
			itemBlockIdByVal.block = retItemBlockId.block;
			itemBlockIdByVal.write();
		}
	}
	
	/**
	 * This function deletes this note from the specified database with default flags (0).<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described in the entry {@link UpdateNote}.<br>
	 * <br>
	 * It deletes the specified note by updating it with a nil body, and marking the note as a deletion stub.<br>
	 * The deletion stub identifies the deleted note to other replica copies of the database.<br>
	 * This allows the replicator to delete copies of the note from replica databases.
	 * <br>
	 * The deleted note may be of any NOTE_CLASS_xxx.  The active user ID must have sufficient user access
	 * in the databases's Access Control List (ACL) to carry out a deletion on the note or the function
	 * will return an error code.
	 */
	public void delete() {
		delete(EnumSet.noneOf(UpdateNote.class));
	}

	/**
	 * This function deletes this note from the specified database.<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described in the entry {@link UpdateNote}.<br>
	 * <br>
	 * It deletes the specified note by updating it with a nil body, and marking the note as a deletion stub.<br>
	 * The deletion stub identifies the deleted note to other replica copies of the database.<br>
	 * This allows the replicator to delete copies of the note from replica databases.
	 * <br>
	 * The deleted note may be of any NOTE_CLASS_xxx.  The active user ID must have sufficient user access
	 * in the databases's Access Control List (ACL) to carry out a deletion on the note or the function
	 * will return an error code.
	 * 
	 * @param flags flags
	 */
	public void delete(EnumSet<UpdateNote> flags) {
		if (checkForProfileAndDelete()) {
			return;
		}
		
		checkHandle();
		
		if (m_parentDb.isRecycled())
			throw new NotesError(0, "Parent database already recycled");
		
		int flagsAsInt = UpdateNote.toBitMaskForUpdateExt(flags);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteDeleteExtended(m_parentDb.getHandle64(), getNoteId(), flagsAsInt);
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteDeleteExtended(m_parentDb.getHandle32(), getNoteId(), flagsAsInt);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Checks if this note is a profile. If it is, we use a different C method
	 * to delete it in the database and also delete the profile cache entry.
	 * 
	 * @return true if note is a profile note that has been saved, false otherwise
	 */
	private boolean checkForProfileAndDelete() {
		checkHandle();
		
		if (!isGhost()) {
			return false;
		}
		
		String[] profileNameAndUsername = parseProfileAndUserName();
		
		if (profileNameAndUsername==null) {
			return false;
		}
		
		String profileName = profileNameAndUsername[0];
		String profileUsername = profileNameAndUsername[1];
		
		Memory profileNameMem = NotesStringUtils.toLMBCS(profileName, false);
		Memory profileUsernameMem = StringUtil.isEmpty(profileUsername) ? null : NotesStringUtils.toLMBCS(profileUsername, false);
		
		NotesDatabase parentDb = getParent();
		if (parentDb.isRecycled()) {
			throw new NotesError("Parent database is disposed");
		}
		
		//delete note and remove it from the profile cache
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFProfileDelete(parentDb.getHandle64(),
					profileNameMem, (short) (profileNameMem.size() & 0xffff),
					profileUsernameMem, (short) ((profileUsernameMem==null ? 0 : profileUsernameMem.size()) & 0xffff));
		}
		else {
			result = NotesNativeAPI32.get().NSFProfileDelete(parentDb.getHandle32(),
					profileNameMem, (short) (profileNameMem.size() & 0xffff),
					profileUsernameMem, (short) ((profileUsernameMem==null ? 0 : profileUsernameMem.size()) & 0xffff));
		}
		NotesErrorUtils.checkResult(result);
		
		return true;
	}

	/**
	 * Method to remove an item from a note
	 * 
	 * @param itemName item name
	 */
	public void removeItem(String itemName) {
		checkHandle();
		
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFItemDelete(m_hNote64, itemNameMem, (short) (itemNameMem.size() & 0xffff));
		}
		else {
			result = NotesNativeAPI32.get().NSFItemDelete(m_hNote32, itemNameMem, (short) (itemNameMem.size() & 0xffff));
		}
		if (result==INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
			return;
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/** default size of return buffer for operations returning strings like NSFItemGetText  */
	private final int DEFAULT_STRINGRETVALUE_LENGTH = 16384;
	/** max size of return buffer for operations returning strings like NSFItemGetText  */
	private final int MAX_STRINGRETVALUE_LENGTH = 65535;

	private ThreadLocal<DisposableMemory> stringretBuffer = new ThreadLocal<DisposableMemory>();
	private boolean m_saveMessageOnSend;
	
	/**
	 * Use this function to read the value of a text item.<br>
	 * <br>
	 * If the item does not exist, the method returns an empty string. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return text value
	 */
	public String getItemValueString(String itemName) {
		List<String> strList = getItemValueStringList(itemName);
		if (strList!=null && !strList.isEmpty()) {
			return strList.get(0);
		}
		else {
			return "";
		}
	}
	
	/**
	 * This function writes an item of type TEXT to the note.<br>
	 * If an item of that name already exists, it deletes the existing item first,
	 * then appends the new item.<br>
	 * <br>
	 * Note 1: Use \n as line break in your string. The method internally converts these line breaks
	 * to null characters ('\0'), because that's what the Notes API expects as line break delimiter.<br>
	 * <br>
	 * Note 2: If the Summary parameter of is set to TRUE, the ITEM_SUMMARY flag in the item will be set.
	 * Items with the ITEM_SUMMARY flag set are stored in the note's summary buffer. These items may
	 * be used in view columns,  selection formulas, and @-functions. The maximum size of the summary
	 * buffer is 32K per note.<br>
	 * If you append more than 32K bytes of data in items that have the ITEM_SUMMARY flag set,
	 * this method call will succeed, but {@link #update(EnumSet)} will return ERR_SUMMARY_TOO_BIG (ERR 561).
	 * To avoid this, decide which fields are not used in view columns, selection formulas,
	 * or @-functions. For these "non-computed" fields, use this method with the Summary parameter set to FALSE.<br>
	 * <br>
	 * API program may read, modify, and write items that do not have the summary flag set, but they
	 * must open the note first. Items that do not have the summary flag set can not be accessed
	 * in the summary buffer.<br>
	 * 
	 * @param itemName item name
	 * @param itemValue item value
	 * @param isSummary true to set summary flag
	 */
	public void setItemValueString(String itemName, String itemValue, boolean isSummary) {
		checkHandle();
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		Memory itemValueMem = NotesStringUtils.toLMBCS(itemValue, false);
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemSetTextSummary(m_hNote64, itemNameMem, itemValueMem, itemValueMem==null ? 0 : ((short) (itemValueMem.size() & 0xffff)), isSummary);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemSetTextSummary(m_hNote32, itemNameMem, itemValueMem, itemValueMem==null ? 0 : ((short) (itemValueMem.size() & 0xffff)), isSummary);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * This function writes a number to an item in the note.
	 * 
	 * @param itemName item name
	 * @param value new value
	 */
	public void setItemValueDouble(String itemName, double value) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		Memory doubleMem = new Memory(Native.getNativeSize(Double.TYPE));
		doubleMem.setDouble(0, value);
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemSetNumber(m_hNote64, itemNameMem, doubleMem);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemSetNumber(m_hNote32, itemNameMem, doubleMem);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Writes a {@link Calendar} value in an item
	 * 
	 * @param itemName item name
	 * @param cal new value
	 */
	public void setItemValueDateTime(String itemName, Calendar cal) {
		if (cal==null) {
			removeItem(itemName);
			return;
		}
		
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

		NotesTimeDate timeDate = NotesDateTimeUtils.calendarToTimeDate(cal);
		NotesTimeDateStruct timeDateStruct = timeDate==null ? null : NotesTimeDateStruct.newInstance(timeDate.getInnards());
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemSetTime(m_hNote64, itemNameMem, timeDateStruct);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemSetTime(m_hNote32, itemNameMem, timeDateStruct);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Writes a {@link Date} value in an item
	 * 
	 * @param itemName item name
	 * @param dt new value
	 * @param hasDate true to save the date of the specified {@link Date} object
	 * @param hasTime true to save the time of the specified {@link Date} object
	 */
	public void setItemValueDateTime(String itemName, Date dt, boolean hasDate, boolean hasTime) {
		if (dt==null) {
			removeItem(itemName);
			return;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		if (!hasDate) {
			NotesDateTimeUtils.setAnyDate(cal);
		}
		if (!hasTime) {
			NotesDateTimeUtils.setAnyTime(cal);
		}
		setItemValueDateTime(itemName, cal);
	}

	/**
	 * Reads the value of a text list item
	 * 
	 * @param itemName item name
	 * @return list of strings; empty if item does not exist
	 */
	public List<String> getItemValueStringList(String itemName) {
		List<?> docValues = getItemValue(itemName);
		if (docValues != null) {
			List<String> strList = new ArrayList<String>(docValues.size());
			for (int i = 0; i < docValues.size(); i++) {
				String currStr = docValues.get(i).toString();
				if (!"".equals(currStr)) {
					strList.add(currStr);
				}
			}
			return strList;
		}
		return Collections.emptyList();
	}
	
	/**
	 * This is a very powerful function that converts many kinds of Domino fields (items) into
	 * text strings.<br>
	 * 
	 * If there is more than one item with the same name, this function will always return the first of these.
	 * This function, therefore, is not useful if you want to retrieve multiple instances of the same
	 * field name. For these situations, use {@link NotesItem#getValueAsText(char)}.<br>
	 * <br>
	 * The item value may be any one of these supported Domino data types:<br>
	 * <ul>
	 * <li>{@link NotesItem#TYPE_TEXT} - Text is returned unmodified.</li>
	 * <li>{@link NotesItem#TYPE_TEXT_LIST} - A text list items will merged into a single text string, with the separator between them.</li>
	 * <li>{@link NotesItem#TYPE_NUMBER} - the FLOAT number will be converted to text.</li>
	 * <li>{@link NotesItem#TYPE_NUMBER_RANGE} - the FLOAT numbers will be converted to text, with the separator between them.</li>
	 * <li>{@link NotesItem#TYPE_TIME} - the binary Time/Date will be converted to text.</li>
	 * <li>{@link NotesItem#TYPE_TIME_RANGE} - the binary Time/Date values will be converted to text, with the separator between them.</li>
	 * <li>{@link NotesItem#TYPE_COMPOSITE} - The text portion of the rich text field will be returned.</li>
	 * <li>{@link NotesItem#TYPE_USERID} - The user name portion will be converted to text.</li>
	 * <li>{@link NotesItem#TYPE_ERROR} - the binary Error value will be converted to "ERROR: ".</li>
	 * <li>{@link NotesItem#TYPE_UNAVAILABLE} - the binary Unavailable value will be converted to "UNAVAILABLE: ".</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param multiValueDelimiter delimiter character for value lists; should be an ASCII character, since no encoding is done
	 * @return string value
	 */
	public String getItemValueAsText(String itemName, char multiValueDelimiter) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		DisposableMemory retItemValueMem = stringretBuffer.get();
		if (retItemValueMem==null || retItemValueMem.isDisposed()) {
			retItemValueMem = new DisposableMemory(DEFAULT_STRINGRETVALUE_LENGTH);
			stringretBuffer.set(retItemValueMem);
		}

		short length;
		if (PlatformUtils.is64Bit()) {
			length = NotesNativeAPI64.get().NSFItemConvertToText(m_hNote64, itemNameMem, retItemValueMem, (short) (retItemValueMem.size() & 0xffff), multiValueDelimiter);
			if (length == (retItemValueMem.size()-1)) {
				retItemValueMem.dispose();
				retItemValueMem = new DisposableMemory(MAX_STRINGRETVALUE_LENGTH);
				stringretBuffer.set(retItemValueMem);
				length = NotesNativeAPI64.get().NSFItemConvertToText(m_hNote64, itemNameMem, retItemValueMem, (short) (retItemValueMem.size() & 0xffff), multiValueDelimiter);
			}
		}
		else {
			length = NotesNativeAPI32.get().NSFItemConvertToText(m_hNote32, itemNameMem, retItemValueMem, (short) (retItemValueMem.size() & 0xffff), multiValueDelimiter);
			if (length == (retItemValueMem.size()-1)) {
				retItemValueMem.dispose();
				retItemValueMem = new DisposableMemory(MAX_STRINGRETVALUE_LENGTH);
				stringretBuffer.set(retItemValueMem);
				length = NotesNativeAPI32.get().NSFItemConvertToText(m_hNote32, itemNameMem, retItemValueMem, (short) (retItemValueMem.size() & 0xffff), multiValueDelimiter);
			}
		}
		int lengthAsInt = (int) length & 0xffff;
		if (lengthAsInt==0) {
			return "";
		}
		
		String strVal = NotesStringUtils.fromLMBCS(retItemValueMem, lengthAsInt);
		return strVal;
	}
	
	/**
	 * Use this function to read the value of a number item as double.<br>
	 * <br>
	 * If the item does not exist, the method returns 0. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return double value
	 */
	public double getItemValueDouble(String itemName) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		DoubleByReference retNumber = new DoubleByReference();
		
		if (PlatformUtils.is64Bit()) {
			boolean exists = NotesNativeAPI64.get().NSFItemGetNumber(m_hNote64, itemNameMem, retNumber);
			if (!exists) {
				return 0;
			}
		}
		else {
			boolean exists = NotesNativeAPI32.get().NSFItemGetNumber(m_hNote32, itemNameMem, retNumber);
			if (!exists) {
				return 0;
			}
		}
		
		return retNumber.getValue();
	}

	/**
	 * Use this function to read the value of a number item as long.<br>
	 * <br>
	 * If the item does not exist, the method returns 0. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return long value
	 */
	public long getItemValueLong(String itemName) {
		List<?> values = getItemValue(itemName);
		if (values.size()==0)
			return 0;
		Object firstVal = values.get(0);
		if (firstVal instanceof Number) {
			return ((Number)firstVal).longValue();
		}
		return 0;
	}

	/**
	 * Use this function to read the value of a number item as integer.<br>
	 * <br>
	 * If the item does not exist, the method returns 0. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return int value
	 */
	public int getItemValueInteger(String itemName) {
		List<?> values = getItemValue(itemName);
		if (values.size()==0)
			return 0;
		Object firstVal = values.get(0);
		if (firstVal instanceof Number) {
			return ((Number)firstVal).intValue();
		}
		return 0;
	}
	
	/**
	 * Use this function to read the value of a timedate item as {@link Calendar}.<br>
	 * <br>
	 * If the item does not exist, the method returns null.
	 * 
	 * @param itemName item name
	 * @return time date value or null
	 */
	public Calendar getItemValueDateTime(String itemName) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		NotesTimeDateStruct td_item_value = NotesTimeDateStruct.newInstance();
		
		if (PlatformUtils.is64Bit()) {
			boolean exists = NotesNativeAPI64.get().NSFItemGetTime(m_hNote64, itemNameMem, td_item_value);
			if (!exists) {
				return null;
			}
		}
		else {
			boolean exists = NotesNativeAPI32.get().NSFItemGetTime(m_hNote32, itemNameMem, td_item_value);
			if (!exists) {
				return null;
			}
		}
		return td_item_value.toCalendar();
	}

	/**
	 * Use this function to read the value of a timedate item as {@link NotesTimeDate}.<br>
	 * <br>
	 * If the item does not exist, the method returns null.
	 * 
	 * @param itemName item name
	 * @return time date value or null if not found
	 */
	public NotesTimeDate getItemValueAsTimeDate(String itemName) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		NotesTimeDateStruct td_item_value = NotesTimeDateStruct.newInstance();
		
		if (PlatformUtils.is64Bit()) {
			boolean exists = NotesNativeAPI64.get().NSFItemGetTime(m_hNote64, itemNameMem, td_item_value);
			if (!exists) {
				return null;
			}
		}
		else {
			boolean exists = NotesNativeAPI32.get().NSFItemGetTime(m_hNote32, itemNameMem, td_item_value);
			if (!exists) {
				return null;
			}
		}
		td_item_value.read();
		int[] innards = td_item_value.Innards;
		return new NotesTimeDate(innards);
	}
	
	/**
	 * Decodes an item value
	 * 
	 * @param itemName item name (for logging purpose)
	 * @param valueBlockId value block id
	 * @param valueLength item value length plus 2 bytes for the data type WORD
	 * @return item value as list
	 */
	List<Object> getItemValue(String itemName, NotesBlockIdStruct itemBlockId, NotesBlockIdStruct valueBlockId, int valueLength) {
		Pointer poolPtr;
		if (PlatformUtils.is64Bit()) {
			poolPtr = Mem64.OSLockObject((long) valueBlockId.pool);
		}
		else {
			poolPtr = Mem32.OSLockObject(valueBlockId.pool);
		}
		
		int block = (valueBlockId.block & 0xffff);
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		Pointer valuePtr = new Pointer(poolPtrLong);
		
		try {
			List<Object> values = getItemValue(itemName, itemBlockId, valueBlockId, valuePtr, valueLength);
			return values;
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				Mem64.OSUnlockObject((long) valueBlockId.pool);
			}
			else {
				Mem32.OSUnlockObject(valueBlockId.pool);
			}
		}
	}
	
	/**
	 * Sets whether methods like {@link #getItemValue(String)} should return {@link NotesTimeDate}
	 * instead of {@link Calendar}.
	 * 
	 * @param b true to prefer NotesTimeDate (false by default)
	 */
	public void setPreferNotesTimeDates(boolean b) {
		m_preferNotesTimeDates = b;
	}
	
	/**
	 * Returns whether methods like {@link #getItemValue(String)} should return {@link NotesTimeDate}
	 * instead of {@link Calendar}.
	 * 
	 * @return true to prefer NotesTimeDate
	 */
	public boolean isPreferNotesTimeDates() {
		return m_preferNotesTimeDates;
	}
	
	/**
	 * Decodes an item value
	 * 
	 * @param notesAPI Notes API
	 * @param itemName item name (for logging purpose)
	 * @param itemBlockId item block id
	 * @param valueBlockId value block id
	 * @param valuePtr pointer to the item value
	 * @param valueLength item value length plus 2 bytes for the data type WORD
	 * @return item value as list
	 */
	List<Object> getItemValue(String itemName, NotesBlockIdStruct itemBlockId, NotesBlockIdStruct valueBlockId, Pointer valuePtr, int valueLength) {
		short dataType = valuePtr.getShort(0);
		int dataTypeAsInt = (int) (dataType & 0xffff);
		
		boolean supportedType = false;
		if (dataTypeAsInt == NotesItem.TYPE_TEXT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_OBJECT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NOTEREF_LIST) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_COLLATION) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_VIEW_FORMAT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_FORMULA) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_MIME_PART) {
			supportedType = true;
		}
		
		if (!supportedType) {
			if (dataTypeAsInt == NotesItem.TYPE_COMPOSITE) {
				throw new UnsupportedItemValueError("Use NotesNote.getRichtextNavigator() to read richtext item data");
			}
			else {
				throw new UnsupportedItemValueError("Data type for value of item "+itemName+" is currently unsupported: "+dataTypeAsInt);
			}
		}

		int checkDataType = valuePtr.getShort(0) & 0xffff;
		Pointer valueDataPtr = valuePtr.share(2);
		int valueDataLength = valueLength - 2;
		
		if (checkDataType!=dataTypeAsInt) {
			throw new IllegalStateException("Value data type does not meet expected date type: found "+checkDataType+", expected "+dataTypeAsInt);
		}
		if (dataTypeAsInt == NotesItem.TYPE_TEXT) {
			String txtVal = (String) ItemDecoder.decodeTextValue(valueDataPtr, valueDataLength, false);
			return txtVal==null ? Collections.emptyList() : Arrays.asList((Object) txtVal);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			List<Object> textList = valueDataLength==0 ? Collections.emptyList() : ItemDecoder.decodeTextListValue(valueDataPtr, false);
			return textList==null ? Collections.emptyList() : textList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			double numVal = ItemDecoder.decodeNumber(valueDataPtr, valueDataLength);
			return Arrays.asList((Object) Double.valueOf(numVal));
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			List<Object> numberList = ItemDecoder.decodeNumberList(valueDataPtr, valueDataLength);
			return numberList==null ? Collections.emptyList() : numberList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			if (isPreferNotesTimeDates()) {
				NotesTimeDate td = ItemDecoder.decodeTimeDateAsNotesTimeDate(valueDataPtr, valueDataLength);
				return Arrays.asList((Object) td);
			}
			else {
				Calendar cal = ItemDecoder.decodeTimeDate(valueDataPtr, valueDataLength);
				if (cal==null) {
					Calendar nullCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					nullCal.set(Calendar.YEAR, 1);
					nullCal.set(Calendar.MONTH, 1);
					nullCal.set(Calendar.DAY_OF_MONTH, 1);
					nullCal.set(Calendar.HOUR, 0);
					nullCal.set(Calendar.MINUTE, 0);
					nullCal.set(Calendar.SECOND, 0);
					nullCal.set(Calendar.MILLISECOND, 0);
					return Arrays.asList((Object) nullCal);
				}
				else {
					return Arrays.asList((Object) cal);
				}
			}
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			if (isPreferNotesTimeDates()) {
				List<Object> tdValues = ItemDecoder.decodeTimeDateListAsNotesTimeDate(valueDataPtr);
				return tdValues==null ? Collections.emptyList() : tdValues;
			}
			else {
				List<Object> calendarValues = ItemDecoder.decodeTimeDateList(valueDataPtr);
				return calendarValues==null ? Collections.emptyList() : calendarValues;
			}
		}
		else if (dataTypeAsInt == NotesItem.TYPE_OBJECT) {
			NotesObjectDescriptorStruct objDescriptor = NotesObjectDescriptorStruct.newInstance(valueDataPtr);
			objDescriptor.read();
			
			int rrv = objDescriptor.RRV;
			
			if (objDescriptor.ObjectType == NotesConstants.OBJECT_FILE) {
				Pointer fileObjectPtr = valueDataPtr;
				
				NotesFileObjectStruct fileObject = NotesFileObjectStruct.newInstance(fileObjectPtr);
				fileObject.read();
				
				short compressionType = fileObject.CompressionType;
				NotesTimeDateStruct fileCreated = fileObject.FileCreated;
				NotesTimeDateStruct fileModified = fileObject.FileModified;
				NotesTimeDate fileCreatedWrap = fileCreated==null ? null : new NotesTimeDate(fileCreated.Innards);
				NotesTimeDate fileModifiedWrap = fileModified==null ? null : new NotesTimeDate(fileModified.Innards);
				
				short fileNameLength = fileObject.FileNameLength;
				int fileSize = fileObject.FileSize;
				short flags = fileObject.Flags;
				
				Compression compression = null;
				for (Compression currComp : Compression.values()) {
					if (compressionType == currComp.getValue()) {
						compression = currComp;
						break;
					}
				}
				
				Pointer fileNamePtr = fileObjectPtr.share(NotesConstants.fileObjectSize);
				String fileName = NotesStringUtils.fromLMBCS(fileNamePtr, fileNameLength);
				
				NotesAttachment attInfo = new NotesAttachment(fileName, compression, flags, fileSize,
						fileCreatedWrap, fileModifiedWrap, this,
						itemBlockId, rrv);
				
				return Arrays.asList((Object) attInfo);
			}
			//TODO add support for other object types
			
			//clone values because value data gets unlocked, preventing invalid memory access
			NotesObjectDescriptorStruct clonedObjDescriptor = NotesObjectDescriptorStruct.newInstance();
			clonedObjDescriptor.ObjectType = objDescriptor.ObjectType;
			clonedObjDescriptor.RRV = objDescriptor.RRV;
			return Arrays.asList((Object) clonedObjDescriptor);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NOTEREF_LIST) {
			int numEntries = (int) (valueDataPtr.getShort(0) & 0xffff);
			
			//skip LIST structure, clone data to prevent invalid memory access when buffer gets disposed
			valueDataPtr = valueDataPtr.share(2);
			
			List<Object> unids = new ArrayList<>();
			
			for (int i=0; i<numEntries; i++) {
				byte[] unidBytes = valueDataPtr.getByteArray(0, NotesConstants.notesUniversalNoteIdSize);
				Memory unidMem = new Memory(NotesConstants.notesUniversalNoteIdSize);
				unidMem.write(0, unidBytes, 0, unidBytes.length);
				NotesUniversalNoteIdStruct unidStruct = NotesUniversalNoteIdStruct.newInstance(unidMem);
				unidStruct.read();
				NotesUniversalNoteId unid = new NotesUniversalNoteId(unidStruct);
				unids.add(unid);
				
				valueDataPtr = valueDataPtr.share(NotesConstants.notesUniversalNoteIdSize);
			}
			return unids;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_COLLATION) {
			NotesCollationInfo colInfo = CollationDecoder.decodeCollation(valueDataPtr);
			return Arrays.asList((Object) colInfo);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_VIEW_FORMAT) {
			NotesViewFormat viewFormatInfo = ViewFormatDecoder.decodeViewFormat(valueDataPtr,  valueDataLength);
			return Arrays.asList((Object) viewFormatInfo);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_FORMULA) {
			boolean isSelectionFormula = "$FORMULA".equalsIgnoreCase(itemName) && getNoteClass().contains(NoteClass.VIEW);
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethFormulaText = new LongByReference();
				ShortByReference retFormulaTextLength = new ShortByReference();
				short result = NotesNativeAPI64.get().NSFFormulaDecompile(valueDataPtr, isSelectionFormula, rethFormulaText, retFormulaTextLength);
				NotesErrorUtils.checkResult(result);

				Pointer formulaPtr = Mem64.OSLockObject(rethFormulaText.getValue());
				try {
					int textLen = (int) (retFormulaTextLength.getValue() & 0xffff);
					String formula = NotesStringUtils.fromLMBCS(formulaPtr, textLen);
					return Arrays.asList((Object) formula);
				}
				finally {
					Mem64.OSUnlockObject(rethFormulaText.getValue());
					result = Mem64.OSMemFree(rethFormulaText.getValue());
					NotesErrorUtils.checkResult(result);
				}
			}
			else {
				IntByReference rethFormulaText = new IntByReference();
				ShortByReference retFormulaTextLength = new ShortByReference();
				short result = NotesNativeAPI32.get().NSFFormulaDecompile(valueDataPtr, isSelectionFormula, rethFormulaText, retFormulaTextLength);
				NotesErrorUtils.checkResult(result);
				
				Pointer formulaPtr = Mem32.OSLockObject(rethFormulaText.getValue());
				try {
					int textLen = (int) (retFormulaTextLength.getValue() & 0xffff);
					String formula = NotesStringUtils.fromLMBCS(formulaPtr, textLen);
					return Arrays.asList((Object) formula);
				}
				finally {
					Mem32.OSUnlockObject(rethFormulaText.getValue());
					result = Mem32.OSMemFree(rethFormulaText.getValue());
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			return Collections.emptyList();
		}
		else if (dataTypeAsInt == NotesItem.TYPE_MIME_PART) {
			NotesMIMEPartStruct mimePartStruct = NotesMIMEPartStruct.newInstance(valueDataPtr);
			mimePartStruct.read();
			
			int iByteCount = (int) (mimePartStruct.wByteCount & 0xffff);
			int iBoundaryLen = (int) (mimePartStruct.wBoundaryLen & 0xffff);
			int iHeadersLen = (int) (mimePartStruct.wHeadersLen & 0xffff);
			
			Pointer mimeBoundaryStrPtr = valueDataPtr.share(NotesConstants.mimePartSize);
			String boundaryStr = NotesStringUtils.fromLMBCS(mimeBoundaryStrPtr, iBoundaryLen);
			while (boundaryStr.startsWith("\r\n")) {
				boundaryStr = boundaryStr.substring(2);
			}
			while (boundaryStr.endsWith("\r\n")) {
				boundaryStr = boundaryStr.substring(0, boundaryStr.length()-2);
			}

			Pointer mimeHeadersPtr = mimeBoundaryStrPtr.share((int) (mimePartStruct.wBoundaryLen & 0xffff));
			String headers = NotesStringUtils.fromLMBCS(mimeHeadersPtr, iHeadersLen);

			Pointer mimeDataPtr = mimeHeadersPtr.share((int) (mimePartStruct.wHeadersLen & 0xffff));
			byte[] data = mimeDataPtr.getByteArray(0, iByteCount - iBoundaryLen - iHeadersLen);
			
			EnumSet<MimePartOptions> options = EnumSet.noneOf(MimePartOptions.class);
			
			for (MimePartOptions currOpt : MimePartOptions.values()) {
				if ((mimePartStruct.dwFlags & currOpt.getValue()) == currOpt.getValue()) {
					options.add(currOpt);
				}
			}
			
			byte cPartType = mimePartStruct.cPartType;
			PartType partType;
			if (cPartType==NotesConstants.MIME_PART_PROLOG) {
				partType = PartType.PROLOG;
			}
			else if (cPartType==NotesConstants.MIME_PART_BODY) {
				partType = PartType.BODY;
			}
			else if (cPartType==NotesConstants.MIME_PART_EPILOG) {
				partType = PartType.EPILOG;
			}
			else if (cPartType==NotesConstants.MIME_PART_RETRIEVE_INFO) {
				partType = PartType.RETRIEVE_INFO;
			}
			else if (cPartType==NotesConstants.MIME_PART_MESSAGE) {
				partType = PartType.MESSAGE;
			}
			else {
				partType = null;
			}
			NotesMIMEPart mimePart = new NotesMIMEPart(this, options, partType, boundaryStr, headers, data);
			return Arrays.asList((Object) mimePart);
		}
		else {
			throw new UnsupportedItemValueError("Data type for value of item "+itemName+" is currently unsupported: "+dataTypeAsInt);
		}
	}

	/**
	 * Interface to create document attachments in-memory without 
	 * the need to write files to disk.
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IAttachmentProducer {
		
		/**
		 * This method is called before creating the binary object in
		 * the database to get a size estimation. The final size is adjusted
		 * to the produced amount of data. Return an estimated file size
		 * here to improve efficiency, otherwise we will do the first NSF object
		 * allocation with a default size (1000 bytes) and auto-grow / -shrink
		 * it based on the produced file data.
		 * 
		 * @return size estimation or -1 if unknown
		 */
		public int getSizeEstimation();
		
		/**
		 * Implement this method to produce the file attachment data
		 * 
		 * @param out output stream
		 * @throws IOException in case of I/O errors
		 */
		public void produceAttachment(OutputStream out) throws IOException;
		
	}
	
	/**
	 * Creates a new attachment with streamed data. This method does not require the
	 * file to be written to disk first like {@link #attachFile(String, String, Compression)},
	 * but creates and auto-resizes an NSF binary object based on the data written in
	 * {@link IAttachmentProducer#produceAttachment(OutputStream)}.
	 * 
	 * @param producer attachment producer
	 * @param uniqueFileNameInNote filename that will be stored internally with the attachment, displayed when the attachment is not part of any richtext item (called "V2 attachment" in the Domino help), and subsequently used when selecting which attachment to extract or detach.  Note that these operations may be carried out both from the workstation application Attachments dialog box and programmatically, so try to choose meaningful filenames as opposed to attach.001, attach002, etc., whenever possible. This function will be sure that the filename is really unique by appending _2, _3 etc. to the base filename, followed by the extension; use the returned NotesAttachment to get the filename we picked
	 * @param fileCreated file creation date
	 * @param fileModified file modified date
	 * @return attachment object just created, e.g. to pass into {@link RichTextBuilder#addFileHotspot(NotesAttachment, String)}
	 */
	public NotesAttachment attachFile(IAttachmentProducer producer, String uniqueFileNameInNote, 
			Date fileCreated, Date fileModified) {
		checkHandle();

		//currently we do not support compression, because we could not find a Java OutputStream
		//implementation for Huffman that produced compatible result and no implementation at all
		//for LZ1 (tried LZW, but that did not work either)
		final Compression compression = Compression.NONE;
		
		//make sure that the unique filename is really unique, since it will be used to return the NotesAttachment object
		List<Object> existingFileItems = FormulaExecution.evaluate("@AttachmentNames", this);
		String reallyUniqueFileName = uniqueFileNameInNote;
		if (existingFileItems.contains(reallyUniqueFileName)) {
			String newFileName=reallyUniqueFileName;
			int idx = 1;
			while (existingFileItems.contains(reallyUniqueFileName)) {
				idx++;

				int iPos = reallyUniqueFileName.lastIndexOf('.');
				if (iPos==-1) {
					newFileName = reallyUniqueFileName+"_"+idx;
				}
				else {
					newFileName = reallyUniqueFileName.substring(0, iPos)+"_"+idx+reallyUniqueFileName.substring(iPos);
				}
				reallyUniqueFileName = newFileName;
			}
		}
		
		//use a default initial object size of 1000 bytes if nothing is specified
		final int estimatedSize = producer.getSizeEstimation()<1 ? 1000 : producer.getSizeEstimation();
		
		final int bufferSize = 30000;
		final byte[] buffer = new byte[bufferSize];
		final AtomicInteger currBufferOffset = new AtomicInteger(0);

		final AtomicInteger currFileSize = new AtomicInteger(0);

		Memory fileItemNameMem = NotesStringUtils.toLMBCS("$FILE", false);
		Memory reallyUniqueFileNameMem = NotesStringUtils.toLMBCS(reallyUniqueFileName, false);

		short result;
		if (PlatformUtils.is64Bit()) {
			//allocate memory buffer used to transfer written data to the NSF binary object
			final LongByReference retBufferHandle = new LongByReference();
			result = Mem64.OSMemAlloc((short) 0, bufferSize, retBufferHandle);
			NotesErrorUtils.checkResult(result);
			final IntByReference rtnRRV = new IntByReference();
			try {
				short type = 0; // 0 = attachment, store in DAOS if available

				//allocate binary object with initial size
				result = NotesNativeAPI64.get().NSFDbAllocObjectExtended2(getParent().getHandle64(), estimatedSize,
						NotesConstants.NOTE_CLASS_DOCUMENT, (short) 0, type, rtnRRV);
				NotesErrorUtils.checkResult(result);

				try {
					//call producer to write file data
					OutputStream nsfObjectOutputStream = new OutputStream() {

						@Override
						public void write(int b) throws IOException {
							//write byte value at current buffer array position
							int iCurrBufferOffset = currBufferOffset.get();
							buffer[iCurrBufferOffset] = (byte) (b & 0xff);

							//check if buffer full
							if ((iCurrBufferOffset+1) == bufferSize) {
								//check if we need to grow the NSF object
								int newObjectSize = currFileSize.get() + bufferSize;
								if (newObjectSize > estimatedSize) {
									short result = NotesNativeAPI64.get().NSFDbReallocObject(getParent().getHandle64(),
											rtnRRV.getValue(), newObjectSize);
									NotesErrorUtils.checkResult(result);
								}

								//copy buffer array data into memory buffer
								Pointer ptrBuffer = Mem64.OSLockObject(retBufferHandle.getValue());
								try {
									ptrBuffer.write(0, buffer, 0, bufferSize);
								}
								finally {
									Mem64.OSUnlockObject(retBufferHandle.getValue());
								}

								//write memory buffer to NSF object
								short result = NotesNativeAPI64.get().NSFDbWriteObject(
										getParent().getHandle64(),
										rtnRRV.getValue(),
										retBufferHandle.getValue(),
										currFileSize.get(), bufferSize);
								NotesErrorUtils.checkResult(result);

								//increment NSF object offset by bufferSize
								currFileSize.addAndGet(bufferSize);
								//reset currBufferOffset
								currBufferOffset.set(0);
							}
							else {
								//buffer not full yet
								
								//increment buffer offset
								currBufferOffset.incrementAndGet();
							}
						}

					};

					try {
						if (compression == Compression.NONE) {
							producer.produceAttachment(nsfObjectOutputStream);
						}
						else {
							throw new IllegalArgumentException("Unsupported compression: "+compression);
						}
					}
					finally {
						nsfObjectOutputStream.close();
					}
					
					int iCurrBufferOffset = currBufferOffset.get();
					if (iCurrBufferOffset>0) {
						//we need to write the remaining buffer data to the NSF object
						
						//set the correct total filesize
						int finalFileSize = currFileSize.get() + iCurrBufferOffset;
						result = NotesNativeAPI64.get().NSFDbReallocObject(getParent().getHandle64(),
								rtnRRV.getValue(), finalFileSize);
						NotesErrorUtils.checkResult(result);

						//copy buffer array data into memory buffer
						Pointer ptrBuffer = Mem64.OSLockObject(retBufferHandle.getValue());
						try {
							ptrBuffer.write(0, buffer, 0, iCurrBufferOffset);
						}
						finally {
							Mem64.OSUnlockObject(retBufferHandle.getValue());
						}
						
						//write memory buffer to NSF object
						result = NotesNativeAPI64.get().NSFDbWriteObject(
								getParent().getHandle64(),
								rtnRRV.getValue(),
								retBufferHandle.getValue(),
								currFileSize.get(), iCurrBufferOffset);
						NotesErrorUtils.checkResult(result);

						currFileSize.set(finalFileSize);
					}
					else if (estimatedSize != currFileSize.get()) {
						//make sure the object has the right size
						result = NotesNativeAPI64.get().NSFDbReallocObject(getParent().getHandle64(),
								rtnRRV.getValue(), currFileSize.get());
						NotesErrorUtils.checkResult(result);
					}
				}
				catch (Exception e) {
					//delete the object in case of errors
					result = NotesNativeAPI64.get().NSFDbFreeObject(getParent().getHandle64(), rtnRRV.getValue());
					NotesErrorUtils.checkResult(result);
					throw new NotesError(0, "Error writing binary NSF DB object for file "+reallyUniqueFileName, e);
				}
			}
			finally {
				Mem64.OSMemFree(retBufferHandle.getValue());
			}

			//allocate memory for the $FILE item value:
			//datatype WORD + FILEOBJECT structure + unique filename
			int sizeOfFileObjectWithFileName = (int) (2 + NotesConstants.fileObjectSize + reallyUniqueFileNameMem.size());
			LongByReference retFileObjectWithFileNameHandle = new LongByReference();
			result = Mem64.OSMemAlloc((short) 0, sizeOfFileObjectWithFileName, retFileObjectWithFileNameHandle);
			NotesErrorUtils.checkResult(result);
			
			//produce FILEOBJECT data structure
			Pointer ptrFileObjectWithDatatype = Mem64.OSLockObject(retFileObjectWithFileNameHandle.getValue());
			try {
				//write datatype WORD
				ptrFileObjectWithDatatype.setShort(0, (short) (NotesItem.TYPE_OBJECT & 0xffff));
				NotesFileObjectStruct fileObjectStruct = NotesFileObjectStruct.newInstance(ptrFileObjectWithDatatype.share(2));
				fileObjectStruct.CompressionType = (short) (compression.getValue() & 0xffff);
				fileObjectStruct.FileAttributes = 0;
				fileObjectStruct.FileCreated = NotesTimeDateStruct.newInstance(fileCreated);
				fileObjectStruct.FileModified = NotesTimeDateStruct.newInstance(fileModified);
				fileObjectStruct.FileNameLength = (short) (reallyUniqueFileNameMem.size() & 0xffff);
				fileObjectStruct.FileSize = currFileSize.get();
				fileObjectStruct.Flags = 0;
				fileObjectStruct.Header.RRV = rtnRRV.getValue();
				fileObjectStruct.Header.ObjectType = NotesConstants.OBJECT_FILE;
				
				fileObjectStruct.write();
				
				//append unique filename
				ptrFileObjectWithDatatype.share(2 + NotesConstants.fileObjectSize).write(0, reallyUniqueFileNameMem.getByteArray(0, (int) reallyUniqueFileNameMem.size()), 0, (int) reallyUniqueFileNameMem.size());
			}
			finally {
				Mem64.OSUnlockObject(retFileObjectWithFileNameHandle.getValue());
			}
			
			NotesBlockIdStruct.ByValue bhValue = NotesBlockIdStruct.ByValue.newInstance();
			bhValue.pool = (int) retFileObjectWithFileNameHandle.getValue();

			int fDealloc = 1;
			//transfers ownership if the item value buffer to the note
			result = NotesNativeAPI64.get().NSFItemAppendObject(m_hNote64,
					NotesConstants.ITEM_SUMMARY,
					fileItemNameMem,
					(short) (fileItemNameMem.size() & 0xffff),
					bhValue,
					sizeOfFileObjectWithFileName,
					fDealloc);
			NotesErrorUtils.checkResult(result);
		}
		else {
			//allocate memory buffer used to transfer written data to the NSF binary object
			final IntByReference retBufferHandle = new IntByReference();
			result = Mem32.OSMemAlloc((short) 0, bufferSize, retBufferHandle);
			NotesErrorUtils.checkResult(result);
			final IntByReference rtnRRV = new IntByReference();
			try {
				short type = 0; // 0 = attachment, store in DAOS if available

				//allocate binary object with initial size
				result = NotesNativeAPI32.get().NSFDbAllocObjectExtended2(getParent().getHandle32(), estimatedSize,
						NotesConstants.NOTE_CLASS_DOCUMENT, (short) 0, type, rtnRRV);
				NotesErrorUtils.checkResult(result);

				try {
					//call producer to write file data
					OutputStream nsfObjectOutputStream = new OutputStream() {


						@Override
						public void write(int b) throws IOException {
							//write byte value at current buffer array position
							int iCurrBufferOffset = currBufferOffset.get();
							buffer[iCurrBufferOffset] = (byte) (b & 0xff);

							//check if buffer full
							if ((iCurrBufferOffset+1) == bufferSize) {
								//check if we need to grow the NSF object
								int newObjectSize = currFileSize.get() + bufferSize;
								if (newObjectSize > estimatedSize) {
									short result = NotesNativeAPI32.get().NSFDbReallocObject(getParent().getHandle32(),
											rtnRRV.getValue(), newObjectSize);
									NotesErrorUtils.checkResult(result);
								}

								//copy buffer array data into memory buffer
								Pointer ptrBuffer = Mem32.OSLockObject(retBufferHandle.getValue());
								try {
									ptrBuffer.write(0, buffer, 0, bufferSize);
								}
								finally {
									Mem32.OSUnlockObject(retBufferHandle.getValue());
								}

								//write memory buffer to NSF object
								short result = NotesNativeAPI32.get().NSFDbWriteObject(
										getParent().getHandle32(),
										rtnRRV.getValue(),
										retBufferHandle.getValue(),
										currFileSize.get(), bufferSize);
								NotesErrorUtils.checkResult(result);

								//increment NSF object offset by bufferSize
								currFileSize.addAndGet(bufferSize);
								//reset currBufferOffset
								currBufferOffset.set(0);
							}
							else {
								//buffer not full yet
								
								//increment buffer offset
								currBufferOffset.incrementAndGet();
							}
						}
					};
					
					try {
						if (compression == Compression.NONE) {
							producer.produceAttachment(nsfObjectOutputStream);
						}
						else {
							throw new IllegalArgumentException("Unsupported compression: "+compression);
						}
					}
					finally {
						nsfObjectOutputStream.close();
					}

					int iCurrBufferOffset = currBufferOffset.get();
					if (iCurrBufferOffset>0) {
						//we need to write the remaining buffer data to the NSF object
						
						//set the correct total filesize
						int finalFileSize = currFileSize.get() + iCurrBufferOffset;
						result = NotesNativeAPI32.get().NSFDbReallocObject(getParent().getHandle32(),
								rtnRRV.getValue(), finalFileSize);
						NotesErrorUtils.checkResult(result);

						//copy buffer array data into memory buffer
						Pointer ptrBuffer = Mem32.OSLockObject(retBufferHandle.getValue());
						try {
							ptrBuffer.write(0, buffer, 0, iCurrBufferOffset);
						}
						finally {
							Mem32.OSUnlockObject(retBufferHandle.getValue());
						}
						
						//write memory buffer to NSF object
						result = NotesNativeAPI32.get().NSFDbWriteObject(
								getParent().getHandle32(),
								rtnRRV.getValue(),
								retBufferHandle.getValue(),
								currFileSize.get(), iCurrBufferOffset);
						NotesErrorUtils.checkResult(result);

						currFileSize.set(finalFileSize);
					}
					else if (estimatedSize != currFileSize.get()) {
						//make sure the object has the right size
						result = NotesNativeAPI32.get().NSFDbReallocObject(getParent().getHandle32(),
								rtnRRV.getValue(), currFileSize.get());
						NotesErrorUtils.checkResult(result);
					}
				}
				catch (Exception e) {
					//delete the object in case of errors
					result = NotesNativeAPI32.get().NSFDbFreeObject(getParent().getHandle32(), rtnRRV.getValue());
					NotesErrorUtils.checkResult(result);
					throw new NotesError(0, "Error writing binary NSF DB object for file "+reallyUniqueFileName, e);
				}
			}
			finally {
				Mem32.OSMemFree(retBufferHandle.getValue());
			}

			//allocate memory for the $FILE item value:
			//datatype WORD + FILEOBJECT structure + unique filename
			int sizeOfFileObjectWithFileName = (int) (2 + NotesConstants.fileObjectSize + reallyUniqueFileNameMem.size());
			IntByReference retFileObjectWithFileNameHandle = new IntByReference();
			result = Mem32.OSMemAlloc((short) 0, sizeOfFileObjectWithFileName, retFileObjectWithFileNameHandle);
			NotesErrorUtils.checkResult(result);
			
			//produce FILEOBJECT data structure
			Pointer ptrFileObjectWithDatatype = Mem32.OSLockObject(retFileObjectWithFileNameHandle.getValue());
			try {
				//write datatype WORD
				ptrFileObjectWithDatatype.setShort(0, (short) (NotesItem.TYPE_OBJECT & 0xffff));
				NotesFileObjectStruct fileObjectStruct = NotesFileObjectStruct.newInstance(ptrFileObjectWithDatatype.share(2));
				fileObjectStruct.CompressionType = (short) (compression.getValue() & 0xffff);
				fileObjectStruct.FileAttributes = 0;
				fileObjectStruct.FileCreated = NotesTimeDateStruct.newInstance(fileCreated);
				fileObjectStruct.FileModified = NotesTimeDateStruct.newInstance(fileModified);
				fileObjectStruct.FileNameLength = (short) (reallyUniqueFileNameMem.size() & 0xffff);
				fileObjectStruct.FileSize = currFileSize.get();
				fileObjectStruct.Flags = 0;
				fileObjectStruct.Header.RRV = rtnRRV.getValue();
				fileObjectStruct.Header.ObjectType = NotesConstants.OBJECT_FILE;
				
				fileObjectStruct.write();
				
				//append unique filename
				ptrFileObjectWithDatatype.share(2 + NotesConstants.fileObjectSize).write(0, reallyUniqueFileNameMem.getByteArray(0, (int) reallyUniqueFileNameMem.size()), 0, (int) reallyUniqueFileNameMem.size());
			}
			finally {
				Mem32.OSUnlockObject(retFileObjectWithFileNameHandle.getValue());
			}
			
			NotesBlockIdStruct.ByValue bhValue = NotesBlockIdStruct.ByValue.newInstance();
			bhValue.pool = (int) retFileObjectWithFileNameHandle.getValue();

			int fDealloc = 1;
			//transfers ownership if the item value buffer to the note
			result = NotesNativeAPI32.get().NSFItemAppendObject(m_hNote32,
					NotesConstants.ITEM_SUMMARY,
					fileItemNameMem,
					(short) (fileItemNameMem.size() & 0xffff),
					bhValue,
					sizeOfFileObjectWithFileName,
					fDealloc);
			NotesErrorUtils.checkResult(result);
		}
		
		//load and return created attachment
		NotesAttachment att = getAttachment(reallyUniqueFileName);
		return att;
	}
	
	/**
	 * Attaches a disk file to a note.<br>
	 * <br>
	 * To accomplish this, the function creates an item of TYPE_OBJECT, sub-category OBJECT_FILE,
	 * whose ITEM_xxx flag(s) are set to ITEM_SIGN | ITEM_SEAL.<br>
	 * The item that is built by NSFNoteAttachFile contains all relevant file information and
	 * the compressed file itself.<br>
	 * Since the Item APIs offer no means of dealing with signed, sealed, or compressed item values,
	 * the File Attachment API NSFNoteDetachFile must be used exclusively to access these items.
	 * 
	 * @param filePathOnDisk fully qualified file path specification for file being attached
	 * @param uniqueFileNameInNote filename that will be stored internally with the attachment, displayed when the attachment is not part of any richtext item (called "V2 attachment" in the Domino help), and subsequently used when selecting which attachment to extract or detach.  Note that these operations may be carried out both from the workstation application Attachments dialog box and programmatically, so try to choose meaningful filenames as opposed to attach.001, attach002, etc., whenever possible. This function will be sure that the filename is really unique by appending _2, _3 etc. to the base filename, followed by the extension; use the returned NotesAttachment to get the filename we picked
	 * @param compression compression to use
	 * @return attachment object just created, e.g. to pass into {@link RichTextBuilder#addFileHotspot(NotesAttachment, String)}
	 */
	public NotesAttachment attachFile(String filePathOnDisk, String uniqueFileNameInNote, Compression compression) {
		checkHandle();

		//make sure that the unique filename is really unique, since it will be used to return the NotesAttachment object
		List<Object> existingFileItems = FormulaExecution.evaluate("@AttachmentNames", this);
		String reallyUniqueFileName = uniqueFileNameInNote;
		if (existingFileItems.contains(reallyUniqueFileName)) {
			String newFileName=reallyUniqueFileName;
			int idx = 1;
			while (existingFileItems.contains(reallyUniqueFileName)) {
				idx++;
				
				int iPos = reallyUniqueFileName.lastIndexOf('.');
				if (iPos==-1) {
					newFileName = reallyUniqueFileName+"_"+idx;
				}
				else {
					newFileName = reallyUniqueFileName.substring(0, iPos)+"_"+idx+reallyUniqueFileName.substring(iPos);
				}
				reallyUniqueFileName = newFileName;
			}
		}
		
		Memory $fileItemName = NotesStringUtils.toLMBCS("$FILE", true);
		Memory filePathOnDiskMem = NotesStringUtils.toLMBCS(filePathOnDisk, true);
		Memory uniqueFileNameInNoteMem = NotesStringUtils.toLMBCS(reallyUniqueFileName, true);
		short compressionAsShort = (short) (compression.getValue() & 0xffff);
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFNoteAttachFile(m_hNote64, $fileItemName, (short) (($fileItemName.size()-1) & 0xffff), filePathOnDiskMem, uniqueFileNameInNoteMem, compressionAsShort);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFNoteAttachFile(m_hNote32, $fileItemName, (short) (($fileItemName.size()-1) & 0xffff), filePathOnDiskMem, uniqueFileNameInNoteMem, compressionAsShort);
			NotesErrorUtils.checkResult(result);
		}
		return getAttachment(reallyUniqueFileName);
	}
	
	/**
	 * The method searches for a note attachment with the specified filename
	 * 
	 * @param fileName filename to search for (case-insensitive)
	 * @return attachment or null if not found
	 */
	public NotesAttachment getAttachment(final String fileName) {
		final NotesAttachment[] foundAttInfo = new NotesAttachment[1];
		
		getItems("$file", new IItemCallback() {
			
			@Override
			public void itemNotFound() {
			}
			
			@Override
			public Action itemFound(NotesItem item) {
				List<Object> values = item.getValues();
				if (values!=null && !values.isEmpty() && values.get(0) instanceof NotesAttachment) {
					NotesAttachment attInfo = (NotesAttachment) values.get(0);
					if (attInfo.getFileName().equalsIgnoreCase(fileName)) {
						foundAttInfo[0] = attInfo;
						return Action.Stop;
					}
				}
				return Action.Continue;
			}
		});
		return foundAttInfo[0];
	}
	
	/**
	 * Decodes the value(s) of the first item with the specified item name<br>
	 * <br>
	 * The supported data types are documented here: {@link NotesItem#getValues()}
	 * 
	 * @param itemName item name
	 * @return value(s) as list, not null
	 * @throws UnsupportedItemValueError if item type is not supported yet
	 */
	public List<Object> getItemValue(String itemName) {
		checkHandle();

		NotesItem item = getFirstItem(itemName);
		if (item==null) {
			return Collections.emptyList();
		}
		
		int valueLength = item.getValueLength();

		Pointer valuePtr;
		
		//lock and decode value
		NotesBlockIdStruct valueBlockId = item.getValueBlockId();
		
		Pointer poolPtr;
		if (PlatformUtils.is64Bit()) {
			poolPtr = Mem64.OSLockObject((long) valueBlockId.pool);
		}
		else {
			poolPtr = Mem32.OSLockObject(valueBlockId.pool);
		}
		
		int block = (int) (valueBlockId.block & 0xffff);
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		valuePtr = new Pointer(poolPtrLong);
		
		try {
			List<Object> values = getItemValue(itemName, item.getItemBlockId(), valueBlockId, valuePtr, valueLength);
			return values;
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				Mem64.OSUnlockObject((long) valueBlockId.pool);
				
			}
			else {
				Mem32.OSUnlockObject(valueBlockId.pool);
			}
		}
	}
	
	/**
	 * Callback interface for {@link NotesNote#getItems(IItemCallback)}
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IItemCallback {
		public static enum Action {Continue, Stop};
		
		/**
		 * Method is called when an item could not be found in the note
		 */
		public default void itemNotFound() {};
		
		/**
		 * Method is called for each item in the note. A note may contain the same item name
		 * multiple times. In this case, the method is called for each item instance
		 * 
		 * @param item item object with meta data and access method to decode item value
		 * @return next action, either continue or stop scan
		 */
		public Action itemFound(NotesItem item);
	}
	
	/**
	 * Utility class to decode the item flag bitmask
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ItemFlags {
		private int m_itemFlags;
		
		public ItemFlags(int itemFlags) {
			m_itemFlags = itemFlags;
		}
		
		public int getBitMask() {
			return m_itemFlags;
		}
		
		public boolean isSigned() {
			return (m_itemFlags & NotesConstants.ITEM_SIGN) == NotesConstants.ITEM_SIGN;
		}
		
		public boolean isSealed() {
			return (m_itemFlags & NotesConstants.ITEM_SEAL) == NotesConstants.ITEM_SEAL;
		}
		
		public boolean isSummary() {
			return (m_itemFlags & NotesConstants.ITEM_SUMMARY) == NotesConstants.ITEM_SUMMARY;
		}
		
		public boolean isReadWriters() {
			return (m_itemFlags & NotesConstants.ITEM_READWRITERS) == NotesConstants.ITEM_READWRITERS;
		}
		
		public boolean isNames() {
			return (m_itemFlags & NotesConstants.ITEM_NAMES) == NotesConstants.ITEM_NAMES;
		}
		
		public boolean isPlaceholder() {
			return (m_itemFlags & NotesConstants.ITEM_PLACEHOLDER) == NotesConstants.ITEM_PLACEHOLDER;
		}
		
		public boolean isProtected() {
			return (m_itemFlags & NotesConstants.ITEM_PROTECTED) == NotesConstants.ITEM_PROTECTED;
		}
		
		public boolean isReaders() {
			return (m_itemFlags & NotesConstants.ITEM_READERS) == NotesConstants.ITEM_READERS;
		}
		
		public boolean isUnchanged() {
			return (m_itemFlags & NotesConstants.ITEM_UNCHANGED) == NotesConstants.ITEM_UNCHANGED;
		}

	}
	
	/**
	 * Scans through all items of this note
	 * 
	 * @param callback callback is called for each item found
	 */
	public void getItems(final IItemCallback callback) {
		getItems((String) null, callback);
	}
	
	private static class LoopImpl extends Loop {
		
		@Override
		public void setIndex(int index) {
			super.setIndex(index);
		}
		
		@Override
		public void setIsLast() {
			super.setIsLast();
		}
	}
	
	/**
	 * Scans through all items of this note
	 * 
	 * @param consumer lambda expression to receive items
	 */
	public void getItems(BiConsumer<NotesItem, Loop> consumer) {
		getItems((String)null, consumer);
	}
	
	/**
	 * Scans through all items of this note
	 * 
	 * @param searchForItemName item name to search for or null to scan through all items
	 * @param consumer lambda expression to receive items
	 */
	public void getItems(final String searchForItemName, BiConsumer<NotesItem, Loop> consumer) {
		LoopImpl loop = new LoopImpl();
		
		AtomicInteger itemIdx = new AtomicInteger(-1);
		//to be able to report that the item is the last one, we need to prefetch one
		AtomicReference<NotesItem> lastReadItem = new AtomicReference<>();
		
		getItems(searchForItemName, new IItemCallback() {
			
			@Override
			public Action itemFound(NotesItem item) {
				if (itemIdx.get() == -1) {
					//first match
					lastReadItem.set(item);
				}
				else {
					//report last read
					consumer.accept(lastReadItem.get(), loop);
					//store item for next loop run
					lastReadItem.set(item);
				}
				
				loop.setIndex(itemIdx.incrementAndGet());
				
				if (loop.isStopped()) {
					return Action.Stop;
				}
				else {
					return Action.Continue;
				}
			}
		});
		
		//report last item
		NotesItem lastItem = lastReadItem.get();
		if (lastItem != null && !loop.isStopped()) {
			loop.setIsLast();
			
			consumer.accept(lastItem, loop);
		}
	}
	
	/**
	 * Scans through all items of this note that have the specified name
	 * 
	 * @param searchForItemName item name to search for or null to scan through all items
	 * @param callback callback is called for each scan result
	 */
	public void getItems(final String searchForItemName, final IItemCallback callback) {
		checkHandle();
		
		Memory itemNameMem = StringUtil.isEmpty(searchForItemName) ? null : NotesStringUtils.toLMBCS(searchForItemName, false);
		
		NotesBlockIdStruct.ByReference itemBlockId = NotesBlockIdStruct.ByReference.newInstance();
		NotesBlockIdStruct.ByReference valueBlockId = NotesBlockIdStruct.ByReference.newInstance();
		ShortByReference retDataType = new ShortByReference();
		IntByReference retValueLen = new IntByReference();
		
		short result;
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFItemInfo(m_hNote64, itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff),
					itemBlockId, retDataType, valueBlockId, retValueLen);
		}
		else {
			result = NotesNativeAPI32.get().NSFItemInfo(m_hNote32, itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff),
					itemBlockId, retDataType, valueBlockId, retValueLen);
		}
		
		if (result == INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
			callback.itemNotFound();
			return;
		}

		NotesErrorUtils.checkResult(result);
		
		NotesBlockIdStruct itemBlockIdClone = NotesBlockIdStruct.newInstance();
		itemBlockIdClone.pool = itemBlockId.pool;
		itemBlockIdClone.block = itemBlockId.block;
		itemBlockIdClone.write();
		
		NotesBlockIdStruct valueBlockIdClone = NotesBlockIdStruct.newInstance();
		valueBlockIdClone.pool = valueBlockId.pool;
		valueBlockIdClone.block = valueBlockId.block;
		valueBlockIdClone.write();
		
		int dataType = retDataType.getValue();
		
		NotesItem itemInfo = new NotesItem(this, itemBlockIdClone, dataType,
				valueBlockIdClone);
		
		Action action = callback.itemFound(itemInfo);
		if (action != Action.Continue) {
			return;
		}
		
		while (true) {
			IntByReference retNextValueLen = new IntByReference();
			
			NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
			itemBlockIdByVal.pool = itemBlockId.pool;
			itemBlockIdByVal.block = itemBlockId.block;
			
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFItemInfoNext(m_hNote64, itemBlockIdByVal,
						itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff), itemBlockId, retDataType,
						valueBlockId, retNextValueLen);
			}
			else {
				result = NotesNativeAPI32.get().NSFItemInfoNext(m_hNote32, itemBlockIdByVal,
						itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff), itemBlockId, retDataType,
						valueBlockId, retNextValueLen);
			}

			if (result == INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
				return;
			}

			NotesErrorUtils.checkResult(result);

			itemBlockIdClone = NotesBlockIdStruct.newInstance();
			itemBlockIdClone.pool = itemBlockId.pool;
			itemBlockIdClone.block = itemBlockId.block;
			itemBlockIdClone.write();
			
			valueBlockIdClone = NotesBlockIdStruct.newInstance();
			valueBlockIdClone.pool = valueBlockId.pool;
			valueBlockIdClone.block = valueBlockId.block;
			valueBlockIdClone.write();
			
			dataType = retDataType.getValue();

			itemInfo = new NotesItem(this, itemBlockIdClone, dataType,
					valueBlockIdClone);
			
			action = callback.itemFound(itemInfo);
			if (action != Action.Continue) {
				return;
			}
		}
	}

	/**
	 * Returns a {@link IRichTextNavigator} to traverse the CD record structure of a richtext item
	 * back and forth
	 * 
	 * @param richTextItemName richtext item name
	 * @return navigator with position already set to the first CD record
	 */
	public IRichTextNavigator getRichtextNavigator(String richTextItemName) {
		return new MultiItemRichTextNavigator(richTextItemName);
	}
	
	/**
	 * Extracts all text from a richtext item
	 * 
	 * @param itemName item name
	 * @return text content
	 */
	public String getRichtextContentAsText(String itemName) {
		final StringWriter sWriter = new StringWriter();
		
		//find all items with this name in case the content got to big to fit into one item alone
		getItems(itemName, new IItemCallback() {

			@Override
			public void itemNotFound() {
			}

			@Override
			public Action itemFound(NotesItem item) {
				if (item.getType()==NotesItem.TYPE_COMPOSITE) {
					item.getAllCompositeTextContent(sWriter);
				}
				return Action.Continue;
			}
		});
		
		return sWriter.toString();
	}
	
	
	/**
	 * Returns the first item with the specified name from the note
	 * 
	 * @param itemName item name
	 * @return item data or null if not found
	 */
	public NotesItem getFirstItem(String itemName) {
		if (itemName==null) {
			throw new IllegalArgumentException("Item name cannot be null. Use getItems() instead.");
		}
		
		final NotesItem[] retItem = new NotesItem[1];
		
		getItems(itemName, new IItemCallback() {

			@Override
			public void itemNotFound() {
				retItem[0]=null;
			}

			@Override
			public Action itemFound(NotesItem itemInfo) {
				retItem[0] = itemInfo;
				return Action.Stop;
			}
		});
		
		return retItem[0];
	}
	
	/**
	 * Checks whether this note is signed
	 * 
	 * @return true if signed
	 */
	public boolean isSigned() {
		checkHandle();
		
		ByteByReference signed_flag_ptr = new ByteByReference();
		ByteByReference sealed_flag_ptr = new ByteByReference();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFNoteIsSignedOrSealed(m_hNote64, signed_flag_ptr, sealed_flag_ptr);
			byte signed = signed_flag_ptr.getValue();
			return signed == 1;
		}
		else {
			NotesNativeAPI32.get().NSFNoteIsSignedOrSealed(m_hNote32, signed_flag_ptr, sealed_flag_ptr);
			byte signed = signed_flag_ptr.getValue();
			return signed == 1;
		}
	}
	
	/**
	 * Checks whether this note is sealed
	 * 
	 * @return true if sealed
	 */
	public boolean isSealed() {
		checkHandle();
		
		ByteByReference signed_flag_ptr = new ByteByReference();
		ByteByReference sealed_flag_ptr = new ByteByReference();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFNoteIsSignedOrSealed(m_hNote64, signed_flag_ptr, sealed_flag_ptr);
			byte sealed = sealed_flag_ptr.getValue();
			return sealed == 1;
		}
		else {
			NotesNativeAPI32.get().NSFNoteIsSignedOrSealed(m_hNote32, signed_flag_ptr, sealed_flag_ptr);
			byte sealed = sealed_flag_ptr.getValue();
			return sealed == 1;
		}
	}

	/** Possible validation phases for {@link NotesNote#computeWithForm(boolean, ComputeWithFormCallback)}  */
	public static enum ValidationPhase {
		/** Error occurred when processing the Default Value formula. */
		CWF_DV_FORMULA,
		/** Error occurred when processing the Translation formula. */
		CWF_IT_FORMULA,
		/**  Error occurred when processing the Validation formula. */
		CWF_IV_FORMULA,
		/** Error occurred when processing the computed field Value formula. */
		CWF_COMPUTED_FORMULA,
		/** Error occurred when verifying the data type for the field. */
		CWF_DATATYPE_CONVERSION,
		/** Error occurred when processing the computed field Value formula, during the "load" pass. */
		CWF_COMPUTED_FORMULA_LOAD,
		/** Error occurred when processing the computed field Value formula, during the "save" pass. */
		CWF_COMPUTED_FORMULA_SAVE
	};

	/* 	Possible return values from the callback routine specified in
	NSFNoteComputeWithForm() */
	public static enum CWF_Action {
		/** End all processing by NSFNoteComputeWithForm() and return the error status to the caller. */
		CWF_ABORT((short) 1),
		/** End validation of the current field and go on to the next. */
		CWF_NEXT_FIELD((short) 2),
		/** Begin the validation process for this field over again. */
		CWF_RECHECK_FIELD((short) 3);
		
		short actionVal;
		
		CWF_Action(short val) {
			this.actionVal = val;
		}
		
		public short getShortVal() {
			return actionVal;
		}

	}
	
	private ValidationPhase decodeValidationPhase(short phase) {
		ValidationPhase phaseEnum = null;
		if (phase == NotesConstants.CWF_DV_FORMULA) {
			phaseEnum = ValidationPhase.CWF_DV_FORMULA;
		}
		else if (phase == NotesConstants.CWF_IT_FORMULA) {
			phaseEnum = ValidationPhase.CWF_IT_FORMULA;
		}
		else if (phase == NotesConstants.CWF_IV_FORMULA) {
			phaseEnum = ValidationPhase.CWF_IV_FORMULA;
		}
		else if (phase == NotesConstants.CWF_COMPUTED_FORMULA) {
			phaseEnum = ValidationPhase.CWF_COMPUTED_FORMULA;
		}
		else if (phase == NotesConstants.CWF_DATATYPE_CONVERSION) {
			phaseEnum = ValidationPhase.CWF_DATATYPE_CONVERSION;
		}
		else if (phase == NotesConstants.CWF_COMPUTED_FORMULA_LOAD) {
			phaseEnum = ValidationPhase.CWF_COMPUTED_FORMULA_LOAD;
		}
		else if (phase == NotesConstants.CWF_COMPUTED_FORMULA_SAVE) {
			phaseEnum = ValidationPhase.CWF_COMPUTED_FORMULA_SAVE;
		}

		return phaseEnum;
	}
	
	/**
	 * Compiles all LotusScript code in this design note.
	 * 
	 * @throws LotusScriptCompilationError when encountering descriptive compilation problem
	 * @throws NotesError for other errors or compilation problems without further description
	 */
	public void compileLotusScript() {
		checkHandle();
		
		final Ref<NotesError> exception = new Ref<NotesError>();
		final NotesCallbacks.LSCompilerErrorProc errorProc;
		if (PlatformUtils.isWin32()) {
			errorProc = new Win32NotesCallbacks.LSCompilerErrorProcWin32() {
				@Override public short invoke(Pointer pInfo, Pointer pCtx) {
					NotesLSCompileErrorInfoStruct errorInfo = NotesLSCompileErrorInfoStruct.newInstance(pInfo);
					errorInfo.read();
					
					int errTextLen = NotesStringUtils.getNullTerminatedLength(errorInfo.pErrText);
					int errFileLen = NotesStringUtils.getNullTerminatedLength(errorInfo.pErrFile);
					
					String errText = NotesStringUtils.fromLMBCS(errorInfo.pErrText, errTextLen);
					String errFile = NotesStringUtils.fromLMBCS(errorInfo.pErrFile, errFileLen);
					
					exception.set(new LotusScriptCompilationError(12051, errorInfo.getLineAsInt(), errText, errFile));
					return 0;
				}
			};
		}
		else {
			errorProc = new NotesCallbacks.LSCompilerErrorProc() {
				@Override public short invoke(Pointer pInfo, Pointer pCtx) {
					NotesLSCompileErrorInfoStruct errorInfo = NotesLSCompileErrorInfoStruct.newInstance(pInfo);
					errorInfo.read();
					
					int errTextLen = NotesStringUtils.getNullTerminatedLength(errorInfo.pErrText);
					int errFileLen = NotesStringUtils.getNullTerminatedLength(errorInfo.pErrFile);
					
					String errText = NotesStringUtils.fromLMBCS(errorInfo.pErrText, errTextLen);
					String errFile = NotesStringUtils.fromLMBCS(errorInfo.pErrFile, errFileLen);
					
					exception.set(new LotusScriptCompilationError(12051, errorInfo.getLineAsInt(), errText, errFile));
					return 0;
				}
			};
		}

		short result;
		try {
			if (PlatformUtils.is64Bit()) {
				//AccessController call required to prevent SecurityException when running in XPages
				result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

					@Override
					public Short run() throws Exception {
						return NotesNativeAPI64.get().NSFNoteLSCompileExt(NotesNote.this.getParent().getHandle64(), m_hNote64, 0, errorProc, null);
					}
				});
			} else {
				result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

					@Override
					public Short run() throws Exception {
						return NotesNativeAPI32.get().NSFNoteLSCompileExt(NotesNote.this.getParent().getHandle32(), m_hNote32, 0, errorProc, null);
					}
				});
			}
		} catch (PrivilegedActionException e) {
			if (e.getCause() instanceof RuntimeException) 
				throw (RuntimeException) e.getCause();
			else
				throw new NotesError(0, "Error getting notes from database", e);
		}
		if(exception.get() != null) {
			throw exception.get();
		}
		NotesErrorUtils.checkResult(result);
	}
	
	public void computeWithForm(boolean continueOnError, final ComputeWithFormCallback callback) {
		checkHandle();

		int dwFlags = 0;
		if (continueOnError) {
			dwFlags = NotesConstants.CWF_CONTINUE_ON_ERROR;
		}
		
		if (PlatformUtils.is64Bit()) {
			NotesCallbacks.b64_CWFErrorProc errorProc = new NotesCallbacks.b64_CWFErrorProc() {

				@Override
				public short invoke(Pointer pCDField, short phase, short error, long hErrorText,
						short wErrorTextSize, Pointer ctx) {
					
					String errorTxt;
					if (hErrorText==0) {
						errorTxt = "";
					}
					else {
						Pointer errorTextPtr = Mem64.OSLockObject(hErrorText);
						try {
							//TODO find out where this offset 6 comes from
							errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
						}
						finally {
							Mem64.OSUnlockObject(hErrorText);
						}
					}

					NotesCDFieldStruct cdFieldStruct = NotesCDFieldStruct.newInstance(pCDField);
					cdFieldStruct.read();
					FieldInfo fieldInfo = new FieldInfo(cdFieldStruct);
					ValidationPhase phaseEnum = decodeValidationPhase(phase);

					CWF_Action action;
					if (callback==null) {
						action = CWF_Action.CWF_ABORT;
					}
					else {
						action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, error);
					}
					return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
				}
				
			};
		
			short result = NotesNativeAPI64.get().NSFNoteComputeWithForm(m_hNote64, 0, dwFlags, errorProc, null);
			NotesErrorUtils.checkResult(result);
		}
		else {
			NotesCallbacks.b32_CWFErrorProc errorProc;
			if (PlatformUtils.isWin32()) {
				errorProc = new Win32NotesCallbacks.CWFErrorProcWin32() {

					@Override
					public short invoke(Pointer pCDField, short phase, short error, int hErrorText,
							short wErrorTextSize, Pointer ctx) {
						
						String errorTxt;
						if (hErrorText==0) {
							errorTxt = "";
						}
						else {
							Pointer errorTextPtr = Mem32.OSLockObject(hErrorText);
							try {
								//TODO find out where this offset 6 comes from
								errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
							}
							finally {
								Mem32.OSUnlockObject(hErrorText);
							}
						}

						NotesCDFieldStruct cdFieldStruct = NotesCDFieldStruct.newInstance(pCDField);
						cdFieldStruct.read();
						FieldInfo fieldInfo = new FieldInfo(cdFieldStruct);
						ValidationPhase phaseEnum = decodeValidationPhase(phase);

						CWF_Action action;
						if (callback==null) {
							action = CWF_Action.CWF_ABORT;
						}
						else {
							action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, error);
						}
						return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
					}

				};
			}
			else {
				errorProc = new NotesCallbacks.b32_CWFErrorProc() {

					@Override
					public short invoke(Pointer pCDField, short phase, short error, int hErrorText,
							short wErrorTextSize, Pointer ctx) {
						
						String errorTxt;
						if (hErrorText==0) {
							errorTxt = "";
						}
						else {
							Pointer errorTextPtr = Mem32.OSLockObject(hErrorText);
							try {
								//TODO find out where this offset 6 comes from
								errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
							}
							finally {
								Mem32.OSUnlockObject(hErrorText);
							}
						}

						NotesCDFieldStruct cdFieldStruct = NotesCDFieldStruct.newInstance(pCDField);
						cdFieldStruct.read();
						FieldInfo fieldInfo = new FieldInfo(cdFieldStruct);
						ValidationPhase phaseEnum = decodeValidationPhase(phase);

						CWF_Action action;
						if (callback==null) {
							action = CWF_Action.CWF_ABORT;
						}
						else {
							action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, error);
						}
						return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
					}

				};
			}
			short result = NotesNativeAPI32.get().NSFNoteComputeWithForm(m_hNote32, 0, dwFlags, errorProc, null);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	public static interface ComputeWithFormCallback {
		
		public CWF_Action errorRaised(FieldInfo fieldInfo, ValidationPhase phase, String errorTxt, long errCode);
	}
	
	public static enum EncryptionMode {
		/** Encrypt the message with the key in the user's ID. This flag is not for outgoing mail
		 * messages because recipients, other than the sender, will not be able to decrypt
		 * the message. This flag can be useful to encrypt documents in a local database to
		 * keep them secure or to encrypt documents that can only be decrypted by the same user. */
		ENCRYPT_WITH_USER_PUBLIC_KEY (NotesConstants.ENCRYPT_WITH_USER_PUBLIC_KEY),
		
		/**
		 * Encrypt SMIME if MIME present
		 */
		ENCRYPT_SMIME_IF_MIME_PRESENT (NotesConstants.ENCRYPT_SMIME_IF_MIME_PRESENT),
		
		/**
		 * Encrypt SMIME no sender.
		 */
		ENCRYPT_SMIME_NO_SENDER (NotesConstants.ENCRYPT_SMIME_NO_SENDER),
		
		/**
		 * Encrypt SMIME trusting all certificates.
		 */
		ENCRYPT_SMIME_TRUST_ALL_CERTS(NotesConstants.ENCRYPT_SMIME_TRUST_ALL_CERTS);
		
		private int m_mode;
		
		private EncryptionMode(int mode) {
			m_mode = mode;
		}
		
		public int getMode() {
			return m_mode;
		}
	};

	/**
	 * This function copies and encrypts (seals) the encryption enabled fields in a note
	 * (including the note's file objects), using the current ID file.<br>
	 * <br>
	 * It can encrypt a note in several ways -- by using the Domino public key of the caller,
	 * by using specified secret encryption keys stored in the caller's ID, or by using the
	 * Domino public keys of specified users, if the note does not have any mime parts.<br>
	 * <br>
	 * The method decides which type of encryption to do based upon the setting of the flag
	 * passed to it in its <code>encryptionMode</code> argument.<br>
	 * <br>
	 * If the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is set, it uses the
	 * caller's public ID to encrypt the note.<br>
	 * In this case, only the user who encodes the note can decrypt it.<br>
	 * This feature allows an individual to protect information from anyone else.<br>
	 * <br>
	 * If, instead, the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is not set,
	 * then the function expects the note to contain  a field named "SecretEncryptionKeys"
	 * a field named "PublicEncryptionKeys", or both.<br>
	 * Each field is either a TYPE_TEXT or TYPE_TEXT_LIST field.<br>
	 * <br>
	 * "SecretEncryptionKeys" contains the name(s) of the secret encryption keys in the
	 * calling user's ID to be used to encrypt the note.<br>
	 * This feature is intended to allow a group to encrypt some of the notes in a single
	 * database in a way that only they can decrypt them -- they must share the secret encryption
	 * keys among themselves first for this to work.<br>
	 * <br>
	 * "PublicEncryptionKeys" contains the name(s) of  users, in canonical format.<br>
	 * The note will be encrypted with each user's Domino public key.<br>
	 * The user can then decrypt the note with the private key in the user's ID.<br>
	 * This feature provides a way to encrypt documents, such as mail documents, for another user.<br>
	 * <br>
	 * The note must contain at least one encryption enabled item (an item with the ITEM_SEAL flag set)
	 * in order to be encrypted.<br>
	 * If the note has mime parts and flag {@link EncryptionMode#ENCRYPT_SMIME_IF_MIME_PRESENT}
	 * is set, then it is SMIME encrypted.<br>
	 * <br>
	 * If the document is to be signed as well as encrypted, you must sign the document
	 * before using this method.

	 * @param encryptionMode encryption mode
	 * @return encrypted note copy
	 */
	public NotesNote copyAndEncrypt(EnumSet<EncryptionMode> encryptionMode) {
		return copyAndEncrypt(null, encryptionMode);
	}
		
	/**
	 * This function copies and encrypts (seals) the encryption enabled fields in a note
	 * (including the note's file objects), using a handle to an ID file.<br>
	 * <br>
	 * It can encrypt a note in several ways -- by using the Domino public key of the caller,
	 * by using specified secret encryption keys stored in the caller's ID, or by using the
	 * Domino public keys of specified users, if the note does not have any mime parts.<br>
	 * <br>
	 * The method decides which type of encryption to do based upon the setting of the flag
	 * passed to it in its <code>encryptionMode</code> argument.<br>
	 * <br>
	 * If the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is set, it uses the
	 * caller's public ID to encrypt the note.<br>
	 * In this case, only the user who encodes the note can decrypt it.<br>
	 * This feature allows an individual to protect information from anyone else.<br>
	 * <br>
	 * If, instead, the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is not set,
	 * then the function expects the note to contain  a field named "SecretEncryptionKeys"
	 * a field named "PublicEncryptionKeys", or both.<br>
	 * Each field is either a TYPE_TEXT or TYPE_TEXT_LIST field.<br>
	 * <br>
	 * "SecretEncryptionKeys" contains the name(s) of the secret encryption keys in the
	 * calling user's ID to be used to encrypt the note.<br>
	 * This feature is intended to allow a group to encrypt some of the notes in a single
	 * database in a way that only they can decrypt them -- they must share the secret encryption
	 * keys among themselves first for this to work.<br>
	 * <br>
	 * "PublicEncryptionKeys" contains the name(s) of  users, in canonical format.<br>
	 * The note will be encrypted with each user's Domino public key.<br>
	 * The user can then decrypt the note with the private key in the user's ID.<br>
	 * This feature provides a way to encrypt documents, such as mail documents, for another user.<br>
	 * <br>
	 * The note must contain at least one encryption enabled item (an item with the ITEM_SEAL flag set)
	 * in order to be encrypted.<br>
	 * If the note has mime parts and flag {@link EncryptionMode#ENCRYPT_SMIME_IF_MIME_PRESENT}
	 * is set, then it is SMIME encrypted.<br>
	 * <br>
	 * If the document is to be signed as well as encrypted, you must sign the document
	 * before using this method.

	 * @param id user id to be used for encryption, use null for current id
	 * @param encryptionMode encryption mode
	 * @return encrypted note copy
	 */
	public NotesNote copyAndEncrypt(NotesUserId id, EnumSet<EncryptionMode> encryptionMode) {
		checkHandle();
		
		int flags = 0;
		for (EncryptionMode currMode : encryptionMode) {
			flags = flags | currMode.getMode();
		}
		
		short flagsShort = (short) (flags & 0xffff);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethDstNote = new LongByReference();
			result = NotesNativeAPI64.get().NSFNoteCopyAndEncryptExt2(m_hNote64, id==null ? 0 : id.getHandle64(), flagsShort, rethDstNote, 0, null);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(m_parentDb, rethDstNote.getValue());
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
		else {
			IntByReference rethDstNote = new IntByReference();
			result = NotesNativeAPI32.get().NSFNoteCopyAndEncryptExt2(m_hNote32, id==null ? 0 : id.getHandle32(), flagsShort, rethDstNote, 0, null);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(m_parentDb, rethDstNote.getValue());
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
	}
	
	public NotesNote copyToDatabase(NotesDatabase targetDb) {
		checkHandle();

		if (targetDb.isRecycled()) {
			throw new NotesError(0, "Target database already recycled");
		}
		
		if (PlatformUtils.is64Bit()) {
			LongByReference note_handle_dst = new LongByReference();
			short result = NotesNativeAPI64.get().NSFNoteCopy(m_hNote64, note_handle_dst);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(targetDb, note_handle_dst.getValue());
			
			NotesOriginatorId newOid = targetDb.generateOID();
			NotesOriginatorIdStruct newOidStruct = newOid.getAdapter(NotesOriginatorIdStruct.class);
			
			NotesNativeAPI64.get().NSFNoteSetInfo(copyNote.getHandle64(), NotesConstants._NOTE_ID, null);
			NotesNativeAPI64.get().NSFNoteSetInfo(copyNote.getHandle64(), NotesConstants._NOTE_OID, newOidStruct.getPointer());
			
			LongByReference targetDbHandle = new LongByReference();
			targetDbHandle.setValue(targetDb.getHandle64());
			NotesNativeAPI64.get().NSFNoteSetInfo(copyNote.getHandle64(), NotesConstants._NOTE_DB, targetDbHandle.getPointer());
			
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
		else {
			IntByReference note_handle_dst = new IntByReference();
			short result = NotesNativeAPI32.get().NSFNoteCopy(m_hNote32, note_handle_dst);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(targetDb, note_handle_dst.getValue());
			
			NotesOriginatorId newOid = targetDb.generateOID();
			NotesOriginatorIdStruct newOidStruct = newOid.getAdapter(NotesOriginatorIdStruct.class);
			
			NotesNativeAPI32.get().NSFNoteSetInfo(copyNote.getHandle32(), NotesConstants._NOTE_ID, null);
			NotesNativeAPI32.get().NSFNoteSetInfo(copyNote.getHandle32(), NotesConstants._NOTE_OID, newOidStruct.getPointer());
			
			IntByReference targetDbHandle = new IntByReference();
			targetDbHandle.setValue(targetDb.getHandle32());
			NotesNativeAPI32.get().NSFNoteSetInfo(copyNote.getHandle32(), NotesConstants._NOTE_DB, targetDbHandle.getPointer());
			
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
	}
	
	/**
	 * This function decrypts an encrypted note, using the current user's ID file.<br>
	 * If the user does not have the appropriate encryption key to decrypt the note, an error is returned.<br>
	 * <br>
	 * This function supports new cryptographic keys and algorithms introduced in Release 8.0.1 as
	 * well as any from releases prior to 8.0.1.
	 * <br>
	 * The current implementation of this function automatically decrypts attachments as well.
	 */
	public void decrypt() {
		decrypt(null);
	}
	
	/**
	 * This function decrypts an encrypted note, using the appropriate encryption key stored
	 * in the user's ID file.<br>
	 * If the user does not have the appropriate encryption key to decrypt the note, an error is returned.<br>
	 * <br>
	 * This function supports new cryptographic keys and algorithms introduced in Release 8.0.1 as
	 * well as any from releases prior to 8.0.1.
	 * <br>
	 * The current implementation of this function automatically decrypts attachments as well.
	 * 
	 * @param id user id used for decryption, use null for current id
	 */
	public void decrypt(NotesUserId id) {
		checkHandle();
		
		short decryptFlags = NotesConstants.DECRYPT_ATTACHMENTS_IN_PLACE;
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteCipherDecrypt(m_hNote64, id==null ? 0 : id.getHandle64(), decryptFlags,
					null, 0, null);
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteCipherDecrypt(m_hNote32, id==null ? 0 : id.getHandle32(), decryptFlags,
					null, 0, null);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Removes any existing field with the specified name and creates a new one with the specified value, setting the {@link ItemType#SUMMARY}<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem replaceItemValue(String itemName, Object value) {
		return replaceItemValue(itemName, EnumSet.of(ItemType.SUMMARY), value);
	}

	private static String dumpValueType(Object value) {
		if (value instanceof List) {
			List valueList = (List) value;
			StringBuilder sb = new StringBuilder();
			sb.append(value.getClass().getName()).append(" [");
			for (int i=0; i<valueList.size(); i++) {
				if (i>0) {
					sb.append(", ");
				}
				sb.append(dumpValueType(valueList.get(i)));
			}
			sb.append("]");
			return sb.toString();
		}
		else if (value!=null) {
			return value.getClass().getName();
		}
		else {
			return "null";
		}
	}

	/**
	 * Removes any existing field with the specified name and creates a new one with the specified value.<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>{@link NotesDateRange}, Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * <li>{@link FormulaExecution} to write a compiled formula (e.g. for the window title in design elements)</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param flags item flags, e.g. {@link ItemType#SUMMARY}
	 * @param value item value, see method comment for allowed types; use null to just remove the old item value
	 * @return created item or null if value was null
	 */
	public NotesItem replaceItemValue(String itemName, EnumSet<ItemType> flags, Object value) {
		if (!hasSupportedItemObjectType(value)) {
			throw new IllegalArgumentException("Unsupported value type: "+dumpValueType(value));
		}
		
		while (hasItem(itemName)) {
			removeItem(itemName);
		}
		if (value!=null)
			return appendItemValue(itemName, flags, value);
		else
			return null;
	}
	
	@SuppressWarnings("rawtypes")
	private boolean hasSupportedItemObjectType(Object value) {
		if (value==null) {
			return true;
		}
		else if (value instanceof String) {
			return true;
		}
		else if (value instanceof Number) {
			return true;
		}
		else if (value instanceof Calendar || value instanceof NotesTimeDate || value instanceof Date) {
			return true;
		}
		else if (value instanceof List && ((List)value).isEmpty()) {
			return true;
		}
		else if (value instanceof List && isStringList((List) value)) {
			return true;
		}
		else if (value instanceof List && isNumberOrNumberArrayList((List) value)) {
			return true;
		}
		else if (value instanceof List && isCalendarOrCalendarArrayList((List) value)) {
			return true;
		}
		else if (value instanceof Calendar[] && ((Calendar[])value).length==2) {
			return true;
		}
		else if (value instanceof NotesTimeDate[] && ((NotesTimeDate[])value).length==2) {
			return true;
		}
		else if (value instanceof Date[] && ((Date[])value).length==2) {
			return true;
		}
		else if (value instanceof Number[] && ((Number[])value).length==2) {
			return true;
		}
		else if (value instanceof Double[] && ((Double[])value).length==2) {
			return true;
		}
		else if (value instanceof Integer[] && ((Integer[])value).length==2) {
			return true;
		}
		else if (value instanceof Long[] && ((Long[])value).length==2) {
			return true;
		}
		else if (value instanceof Float[] && ((Float[])value).length==2) {
			return true;
		}
		else if (value instanceof double[] && ((double[])value).length==2) {
			return true;
		}
		else if (value instanceof int[] && ((int[])value).length==2) {
			return true;
		}
		else if (value instanceof long[] && ((long[])value).length==2) {
			return true;
		}
		else if (value instanceof float[] && ((float[])value).length==2) {
			return true;
		}
		else if (value instanceof NotesUniversalNoteId) {
			return true;
		}
		else if (value instanceof FormulaExecution) {
			return true;
		}
		return false;
	}
	
	/**
	 * Creates a new item with the specified value, setting the {@link ItemType#SUMMARY} (does not overwrite items with the same name)<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem appendItemValue(String itemName, Object value) {
		return appendItemValue(itemName, EnumSet.of(ItemType.SUMMARY), value);
	}
	
	/**
	 * Creates a new item with the specified item flags and value<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * <li>{@link NotesUniversalNoteId} for $REF like items</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param flagsOrig item flags
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem appendItemValue(String itemName, EnumSet<ItemType> flagsOrig, Object value) {
		checkHandle();

		//remove our own pseudo flags:
		boolean keepLineBreaks = flagsOrig.contains(ItemType.KEEPLINEBREAKS);
		EnumSet<ItemType> flags = flagsOrig.clone();
		flags.remove(ItemType.KEEPLINEBREAKS);

		if (value instanceof FormulaExecution) {
			//formulas as stored in compiled binary format
			flags.remove(ItemType.SUMMARY);
		}
		
		if (value instanceof String) {
			Memory strValueMem;
			if (keepLineBreaks) {
				strValueMem = NotesStringUtils.toLMBCS((String)value, false, false);
			}
			else {
				strValueMem = NotesStringUtils.toLMBCS((String)value, false);
			}

			int valueSize = (int) (2 + (strValueMem==null ? 0 : strValueMem.size()));
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TEXT);
					valuePtr = valuePtr.share(2);
					if (strValueMem!=null) {
						valuePtr.write(0, strValueMem.getByteArray(0, (int) strValueMem.size()), 0, (int) strValueMem.size());
					}
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TEXT);
					valuePtr = valuePtr.share(2);
					if (strValueMem!=null) {
						valuePtr.write(0, strValueMem.getByteArray(0, (int) strValueMem.size()), 0, (int) strValueMem.size());
					}
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT, rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		
		}
		else if (value instanceof Number) {
			int valueSize = 2 + 8;
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER);
					valuePtr = valuePtr.share(2);
					valuePtr.setDouble(0, ((Number)value).doubleValue());
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER);
					valuePtr = valuePtr.share(2);
					valuePtr.setDouble(0, ((Number)value).doubleValue());
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER, rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof Calendar || value instanceof NotesTimeDate || value instanceof Date) {
			int[] innards;
			boolean hasDate;
			boolean hasTime;
			
			if (value instanceof NotesTimeDate) {
				//no date conversion to innards needing, we already have them
				innards = ((NotesTimeDate)value).getInnards();
				hasTime = innards[0] != NotesConstants.ALLDAY;
				hasDate = innards[1] != NotesConstants.ANYDAY;
			}
			else {
				Calendar calValue;
				if (value instanceof Calendar) {
					calValue = (Calendar) value;
				}
				else if (value instanceof NotesTimeDate) {
					calValue = ((NotesTimeDate)value).toCalendar();
				}
				else if (value instanceof Date) {
					calValue = Calendar.getInstance();
					calValue.setTime((Date) value);
				}
				else {
					throw new IllegalArgumentException("Unsupported date value type: "+(value==null ? "null" : value.getClass().getName()));
				}
				
				hasDate = NotesDateTimeUtils.hasDate(calValue);
				hasTime = NotesDateTimeUtils.hasTime(calValue);
				innards = NotesDateTimeUtils.calendarToInnards(calValue, hasDate, hasTime);
			}

			int valueSize = 2 + 8;
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME);
					valuePtr = valuePtr.share(2);

					NotesTimeDateStruct timeDate = NotesTimeDateStruct.newInstance(valuePtr);
					timeDate.Innards[0] = innards[0];
					timeDate.Innards[1] = innards[1];
					timeDate.write();

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME);
					valuePtr = valuePtr.share(2);

					NotesTimeDateStruct timeDate = NotesTimeDateStruct.newInstance(valuePtr);
					timeDate.Innards[0] = innards[0];
					timeDate.Innards[1] = innards[1];
					timeDate.write();

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME, rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof List && (((List)value).isEmpty() || isStringList((List) value))) {
			List<String> strList = (List<String>) value;
			
			if (strList.size()> 65535) {
				throw new IllegalArgumentException("String list size must fit in a WORD ("+strList.size()+">65535)");
			}
			
			short result;
			if (PlatformUtils.is64Bit()) {
				LongByReference rethList = new LongByReference();
				ShortByReference retListSize = new ShortByReference();

				result = NotesNativeAPI64.get().ListAllocate((short) 0, 
						(short) 0,
						1, rethList, null, retListSize);
				
				NotesErrorUtils.checkResult(result);

				long hList = rethList.getValue();
				Mem64.OSUnlockObject(hList);
				
				for (int i=0; i<strList.size(); i++) {
					String currStr = strList.get(i);
					Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);

					result = NotesNativeAPI64.get().ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currStrMem,
							(short) (currStrMem==null ? 0 : (currStrMem.size() & 0xffff)));
					NotesErrorUtils.checkResult(result);
				}
				
				int listSize = retListSize.getValue() & 0xffff;
				
				@SuppressWarnings("unused")
				Pointer valuePtr = Mem64.OSLockObject(hList);
				try {
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT_LIST, (int) hList, listSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(hList);
				}
			}
			else {
				IntByReference rethList = new IntByReference();
				ShortByReference retListSize = new ShortByReference();

				result = NotesNativeAPI32.get().ListAllocate((short) 0, 
						(short) 0,
						1, rethList, null, retListSize);
				
				NotesErrorUtils.checkResult(result);

				int hList = rethList.getValue();
				Mem32.OSUnlockObject(hList);
				
				for (int i=0; i<strList.size(); i++) {
					String currStr = strList.get(i);
					Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);

					result = NotesNativeAPI32.get().ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currStrMem,
							(short) (currStrMem==null ? 0 : (currStrMem.size() & 0xffff)));
					NotesErrorUtils.checkResult(result);
				}
				
				int listSize = retListSize.getValue() & 0xffff;
				
				@SuppressWarnings("unused")
				Pointer valuePtr = Mem32.OSLockObject(hList);
				try {
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT_LIST, (int) hList, listSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(hList);
				}
			}
		}
		else if (value instanceof List && isNumberOrNumberArrayList((List) value)) {
			List<?> numberOrNumberArrList = toNumberOrNumberArrayList((List<?>) value);
			
			List<Number> numberList = new ArrayList<Number>();
			List<double[]> numberArrList = new ArrayList<double[]>();
			
			for (int i=0; i<numberOrNumberArrList.size(); i++) {
				Object currObj = numberOrNumberArrList.get(i);
				if (currObj instanceof Double) {
					numberList.add((Number) currObj);
				}
				else if (currObj instanceof double[]) {
					numberArrList.add((double[])currObj);
				}
			}
			
			if (numberList.size()> 65535) {
				throw new IllegalArgumentException("Number list size must fit in a WORD ("+numberList.size()+">65535)");
			}

			if (numberArrList.size()> 65535) {
				throw new IllegalArgumentException("Number range list size must fit in a WORD ("+numberList.size()+">65535)");
			}

			int valueSize = 2 + NotesConstants.rangeSize + 
					8 * numberList.size() +
					NotesConstants.numberPairSize * numberArrList.size();

			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (numberList.size() & 0xffff);
					range.RangeEntries = (short) (numberArrList.size() & 0xffff);
					range.write();

					Pointer doubleListPtr = rangePtr.share(NotesConstants.rangeSize);
					
					for (int i=0; i<numberList.size(); i++) {
						doubleListPtr.setDouble(0, numberList.get(i).doubleValue());
						doubleListPtr = doubleListPtr.share(8);
					}

					Pointer doubleArrListPtr = doubleListPtr;
					
					for (int i=0; i<numberArrList.size(); i++) {
						double[] currNumberArr = numberArrList.get(i);
						
						NotesNumberPairStruct numberPair = NotesNumberPairStruct.newInstance(doubleArrListPtr);
						numberPair.Lower = currNumberArr[0];
						numberPair.Upper = currNumberArr[1];
						numberPair.write();

						doubleArrListPtr = doubleArrListPtr.share(NotesConstants.numberPairSize);
					}
					
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER_RANGE, (int) rethItem.getValue(),
							valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (numberList.size() & 0xffff);
					range.RangeEntries = (short) (numberArrList.size() & 0xffff);
					range.write();

					Pointer doubleListPtr = rangePtr.share(NotesConstants.rangeSize);
					
					for (int i=0; i<numberList.size(); i++) {
						doubleListPtr.setDouble(0, numberList.get(i).doubleValue());
						doubleListPtr = doubleListPtr.share(8);
					}

					Pointer doubleArrListPtr = doubleListPtr;
					
					for (int i=0; i<numberArrList.size(); i++) {
						double[] currNumberArr = numberArrList.get(i);
						
						NotesNumberPairStruct numberPair = NotesNumberPairStruct.newInstance(doubleArrListPtr);
						numberPair.Lower = currNumberArr[0];
						numberPair.Upper = currNumberArr[1];
						numberPair.write();

						doubleArrListPtr = doubleArrListPtr.share(NotesConstants.numberPairSize);
					}
					
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER_RANGE, rethItem.getValue(),
							valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof Calendar[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Date[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof NotesTimeDate[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof List && isCalendarOrCalendarArrayList((List) value)) {
			List<?> calendarOrCalendarArrList = toCalendarOrCalendarArrayList((List<?>) value);
			
			List<Calendar> calendarList = new ArrayList<Calendar>();
			List<Calendar[]> calendarArrList = new ArrayList<Calendar[]>();
			
			for (int i=0; i<calendarOrCalendarArrList.size(); i++) {
				Object currObj = calendarOrCalendarArrList.get(i);
				if (currObj instanceof Calendar) {
					calendarList.add((Calendar) currObj);
				}
				else if (currObj instanceof Calendar[]) {
					calendarArrList.add((Calendar[]) currObj);
				}
			}
			
			if (calendarList.size() > 65535) {
				throw new IllegalArgumentException("Date list size must fit in a WORD ("+calendarList.size()+">65535)");
			}
			if (calendarArrList.size() > 65535) {
				throw new IllegalArgumentException("Date range list size must fit in a WORD ("+calendarArrList.size()+">65535)");
			}

			int valueSize = 2 + NotesConstants.rangeSize + 
					8 * calendarList.size() +
					NotesConstants.timeDatePairSize * calendarArrList.size();
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (calendarList.size() & 0xffff);
					range.RangeEntries = (short) (calendarArrList.size() & 0xffff);
					range.write();

					Pointer dateListPtr = rangePtr.share(NotesConstants.rangeSize);
					
					for (Calendar currCal : calendarList) {
						boolean hasDate = NotesDateTimeUtils.hasDate(currCal);
						boolean hasTime = NotesDateTimeUtils.hasTime(currCal);
						int[] innards = NotesDateTimeUtils.calendarToInnards(currCal, hasDate, hasTime);

						dateListPtr.setInt(0, innards[0]);
						dateListPtr = dateListPtr.share(4);
						dateListPtr.setInt(0, innards[1]);
						dateListPtr = dateListPtr.share(4);
					}
					
					Pointer rangeListPtr = dateListPtr;
					
					for (int i=0; i<calendarArrList.size(); i++) {
						Calendar[] currRangeVal = calendarArrList.get(i);
						
						boolean hasDateStart = NotesDateTimeUtils.hasDate(currRangeVal[0]);
						boolean hasTimeStart = NotesDateTimeUtils.hasTime(currRangeVal[0]);
						int[] innardsStart = NotesDateTimeUtils.calendarToInnards(currRangeVal[0], hasDateStart, hasTimeStart);

						boolean hasDateEnd = NotesDateTimeUtils.hasDate(currRangeVal[1]);
						boolean hasTimeEnd = NotesDateTimeUtils.hasTime(currRangeVal[1]);
						int[] innardsEnd = NotesDateTimeUtils.calendarToInnards(currRangeVal[1], hasDateEnd, hasTimeEnd);

						NotesTimeDateStruct timeDateStart = NotesTimeDateStruct.newInstance(innardsStart);
						NotesTimeDateStruct timeDateEnd = NotesTimeDateStruct.newInstance(innardsEnd);
						
						NotesTimeDatePairStruct timeDatePair = NotesTimeDatePairStruct.newInstance(rangeListPtr);
						timeDatePair.Lower = timeDateStart;
						timeDatePair.Upper = timeDateEnd;
						timeDatePair.write();

						rangeListPtr = rangeListPtr.share(NotesConstants.timeDatePairSize);
					}

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME_RANGE, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (calendarList.size() & 0xffff);
					range.RangeEntries = (short) (calendarArrList.size() & 0xffff);
					range.write();

					Pointer dateListPtr = rangePtr.share(NotesConstants.rangeSize);
					
					for (Calendar currCal : calendarList) {
						boolean hasDate = NotesDateTimeUtils.hasDate(currCal);
						boolean hasTime = NotesDateTimeUtils.hasTime(currCal);
						int[] innards = NotesDateTimeUtils.calendarToInnards(currCal, hasDate, hasTime);

						dateListPtr.setInt(0, innards[0]);
						dateListPtr = dateListPtr.share(4);
						dateListPtr.setInt(0, innards[1]);
						dateListPtr = dateListPtr.share(4);
					}
					
					Pointer rangeListPtr = dateListPtr;
					
					for (int i=0; i<calendarArrList.size(); i++) {
						Calendar[] currRangeVal = calendarArrList.get(i);
						
						boolean hasDateStart = NotesDateTimeUtils.hasDate(currRangeVal[0]);
						boolean hasTimeStart = NotesDateTimeUtils.hasTime(currRangeVal[0]);
						int[] innardsStart = NotesDateTimeUtils.calendarToInnards(currRangeVal[0], hasDateStart, hasTimeStart);

						boolean hasDateEnd = NotesDateTimeUtils.hasDate(currRangeVal[1]);
						boolean hasTimeEnd = NotesDateTimeUtils.hasTime(currRangeVal[1]);
						int[] innardsEnd = NotesDateTimeUtils.calendarToInnards(currRangeVal[1], hasDateEnd, hasTimeEnd);

						NotesTimeDateStruct timeDateStart = NotesTimeDateStruct.newInstance(innardsStart);
						NotesTimeDateStruct timeDateEnd = NotesTimeDateStruct.newInstance(innardsEnd);
						
						NotesTimeDatePairStruct timeDatePair = NotesTimeDatePairStruct.newInstance(rangeListPtr);
						timeDatePair.Lower = timeDateStart;
						timeDatePair.Upper = timeDateEnd;
						timeDatePair.write();

						rangeListPtr = rangeListPtr.share(NotesConstants.timeDatePairSize);
					}

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME_RANGE, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof double[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof int[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof float[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof long[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Number[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Double[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Integer[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Float[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Long[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof NotesUniversalNoteId) {
			NotesUniversalNoteIdStruct struct = ((NotesUniversalNoteId)value).getAdapter(NotesUniversalNoteIdStruct.class);

			//date type + LIST structure + UNIVERSALNOTEID
			int valueSize = 2 + 2 + 2 * NotesConstants.timeDateSize;
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NOTEREF_LIST);
					valuePtr = valuePtr.share(2);
					
					//LIST structure
					valuePtr.setShort(0, (short) 1);
					valuePtr = valuePtr.share(2);
					
					struct.write();
					valuePtr.write(0, struct.getAdapter(Pointer.class).getByteArray(0, 2*NotesConstants.timeDateSize), 0, 2*NotesConstants.timeDateSize);

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NOTEREF_LIST, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NOTEREF_LIST);
					valuePtr = valuePtr.share(2);
					
					//LIST structure
					valuePtr.setShort(0, (short) 1);
					valuePtr = valuePtr.share(2);
					
					struct.write();
					valuePtr.write(0, struct.getAdapter(Pointer.class).getByteArray(0, 2*NotesConstants.timeDateSize), 0, 2*NotesConstants.timeDateSize);

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NOTEREF_LIST, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof FormulaExecution) {
			byte[] compiledFormula = ((FormulaExecution)value).getAdapter(byte[].class);
			if (compiledFormula==null) {
				throw new IllegalArgumentException("Unable to read the data of the compiled formula: "+((FormulaExecution)value).getFormula());
			}
			
			//date type + compiled formula
			int valueSize = 2 + compiledFormula.length;
			
			if (PlatformUtils.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = Mem64.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem64.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_FORMULA);
					valuePtr = valuePtr.share(2);
					
					valuePtr.write(0, compiledFormula, 0, compiledFormula.length);

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_FORMULA, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem64.OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = Mem32.OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = Mem32.OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_FORMULA);
					valuePtr = valuePtr.share(2);
					
					valuePtr.write(0, compiledFormula, 0, compiledFormula.length);

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NOTEREF_LIST, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					Mem32.OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported value type: "+(value==null ? "null" : value.getClass().getName()));
		}
	}

	private List<?> toNumberOrNumberArrayList(List<?> list) {
		boolean allNumbers = true;
		for (int i=0; i<list.size(); i++) {
			if (!(list.get(i) instanceof double[]) && !(list.get(i) instanceof Double)) {
				allNumbers = false;
				break;
			}
		}
		
		if (allNumbers)
			return (List<?>) list;
		
		List convertedList = new ArrayList();
		for (int i=0; i<list.size(); i++) {
			if (list.get(i) instanceof Number) {
				//ok
				convertedList.add(((Number)list.get(i)).doubleValue());
			}
			else if (list.get(i) instanceof double[]) {
				if (((double[])list.get(i)).length!=2) {
					throw new IllegalArgumentException("Length of double array entry must be 2 for number ranges");
				}
				//ok
				convertedList.add((double[]) list.get(i));
			}
			else if (list.get(i) instanceof Number[]) {
				Number[] numberArr = (Number[]) list.get(i);
				if (numberArr.length!=2) {
					throw new IllegalArgumentException("Length of Number array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						numberArr[0].doubleValue(),
						numberArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof Double[]) {
				Double[] doubleArr = (Double[]) list.get(i);
				if (doubleArr.length!=2) {
					throw new IllegalArgumentException("Length of Number array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						doubleArr[0],
						doubleArr[1]
				});
			}
			else if (list.get(i) instanceof Integer[]) {
				Integer[] integerArr = (Integer[]) list.get(i);
				if (integerArr.length!=2) {
					throw new IllegalArgumentException("Length of Integer array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						integerArr[0].doubleValue(),
						integerArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof Long[]) {
				Long[] longArr = (Long[]) list.get(i);
				if (longArr.length!=2) {
					throw new IllegalArgumentException("Length of Long array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						longArr[0].doubleValue(),
						longArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof Float[]) {
				Float[] floatArr = (Float[]) list.get(i);
				if (floatArr.length!=2) {
					throw new IllegalArgumentException("Length of Float array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						floatArr[0].doubleValue(),
						floatArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof int[]) {
				int[] intArr = (int[]) list.get(i);
				if (intArr.length!=2) {
					throw new IllegalArgumentException("Length of int array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						intArr[0],
						intArr[1]
				});
			}
			else if (list.get(i) instanceof long[]) {
				long[] longArr = (long[]) list.get(i);
				if (longArr.length!=2) {
					throw new IllegalArgumentException("Length of long array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						longArr[0],
						longArr[1]
				});
			}
			else if (list.get(i) instanceof float[]) {
				float[] floatArr = (float[]) list.get(i);
				if (floatArr.length!=2) {
					throw new IllegalArgumentException("Length of float array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						floatArr[0],
						floatArr[1]
				});
			}
			else {
				throw new IllegalArgumentException("Unsupported date format found in list: "+(list.get(i)==null ? "null" : list.get(i).getClass().getName()));
			}
		}
		return convertedList;
	}
	private List<?> toCalendarOrCalendarArrayList(List<?> list) {
		boolean allCalendar = true;
		for (int i=0; i<list.size(); i++) {
			if (!(list.get(i) instanceof Calendar[]) && !(list.get(i) instanceof Calendar)) {
				allCalendar = false;
				break;
			}
		}
		
		if (allCalendar)
			return (List<?>) list;
		
		List convertedList = new ArrayList();
		for (int i=0; i<list.size(); i++) {
			if (list.get(i) instanceof Calendar) {
				//ok
				convertedList.add(list.get(i));
			}
			else if (list.get(i) instanceof Calendar[]) {
				//ok
				convertedList.add((Calendar[]) list.get(i));
			}
			else if (list.get(i) instanceof Date) {
				Calendar cal = Calendar.getInstance();
				cal.setTime((Date) list.get(i));
				convertedList.add(cal);
			}
			else if (list.get(i) instanceof NotesTimeDate) {
				Calendar cal = ((NotesTimeDate)list.get(i)).toCalendar();
				convertedList.add(cal);
			}
			else if (list.get(i) instanceof Date[]) {
				Date[] dateArr = (Date[]) list.get(i);
				if (dateArr.length!=2) {
					throw new IllegalArgumentException("Length of Date array entry must be 2 for date ranges");
				}
				Calendar val1 = Calendar.getInstance();
				val1.setTime(dateArr[0]);

				Calendar val2 = Calendar.getInstance();
				val2.setTime(dateArr[1]);

				convertedList.add(new Calendar[] {val1, val2});
			}
			else if (list.get(i) instanceof NotesTimeDate[]) {
				NotesTimeDate[] ntdArr = (NotesTimeDate[]) list.get(i);
				if (ntdArr.length!=2) {
					throw new IllegalArgumentException("Length of NotesTimeDate array entry must be 2 for date ranges");
				}
				Calendar val1 = ntdArr[0].toCalendar();
				Calendar val2 = ntdArr[1].toCalendar();
				
				convertedList.add(new Calendar[] {val1, val2});
			}
			else {
				throw new IllegalArgumentException("Unsupported date format found in list: "+(list.get(i)==null ? "null" : list.get(i).getClass().getName()));
			}
		}
		return convertedList;
	}
	
	private boolean isStringList(List<?> list) {
		if (list==null || list.isEmpty()) {
			return false;
		}
		for (int i=0; i<list.size(); i++) {
			if (!(list.get(i) instanceof String)) {
				return false;
			}
		}
		return true;
	}

	private boolean isCalendarOrCalendarArrayList(List<?> list) {
		if (list==null || list.isEmpty()) {
			return false;
		}
		for (int i=0; i<list.size(); i++) {
			boolean isAccepted=false;
			
			Object currObj = list.get(i);
			
			if (currObj instanceof Calendar[]) {
				Calendar[] calArr = (Calendar[]) currObj;
				if (calArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Date[]) {
				Date[] dateArr = (Date[]) currObj;
				if (dateArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof NotesTimeDate[]) {
				NotesTimeDate[] ndtArr = (NotesTimeDate[]) currObj;
				if (ndtArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Calendar) {
				isAccepted = true;
			}
			else if (currObj instanceof Date) {
				isAccepted = true;
			}
			else if (currObj instanceof NotesTimeDate) {
				isAccepted = true;
			}
			
			if (!isAccepted) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isNumberOrNumberArrayList(List<?> list) {
		if (list==null || list.isEmpty()) {
			return false;
		}
		for (int i=0; i<list.size(); i++) {
			boolean isAccepted=false;
			
			Object currObj = list.get(i);
			
			if (currObj instanceof double[]) {
				double[] valArr = (double[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			if (currObj instanceof int[]) {
				int[] valArr = (int[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			if (currObj instanceof long[]) {
				long[] valArr = (long[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			if (currObj instanceof float[]) {
				float[] valArr = (float[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Number[]) {
				Number[] valArr = (Number[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Integer[]) {
				Integer[] valArr = (Integer[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Long[]) {
				Long[] valArr = (Long[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Double[]) {
				Double[] valArr = (Double[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Float[]) {
				Float[] valArr = (Float[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Number) {
				isAccepted = true;
			}
			
			if (!isAccepted) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Internal method that calls the C API method to write the item
	 * 
	 * @param itemName item name
	 * @param flags item flags
	 * @param itemType item type
	 * @param hItemValue handle to memory block with item value
	 * @param valueLength length of binary item value (without data type short)
	 */
	private NotesItem appendItemValue(String itemName, EnumSet<ItemType> flags, int itemType, int hItemValue, int valueLength) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
		
		short flagsShort = ItemType.toBitMask(flags);
		
		NotesBlockIdStruct.ByValue valueBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		valueBlockIdByVal.pool = hItemValue;
		valueBlockIdByVal.block = 0;
		valueBlockIdByVal.write();
		
		NotesBlockIdStruct retItemBlockId = NotesBlockIdStruct.newInstance();
		retItemBlockId.pool = 0;
		retItemBlockId.block = 0;
		retItemBlockId.write();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFItemAppendByBLOCKID(m_hNote64, flagsShort, itemNameMem,
					(short) (itemNameMem==null ? 0 : itemNameMem.size()), valueBlockIdByVal,
					valueLength, retItemBlockId);
		}
		else {
			result = NotesNativeAPI32.get().NSFItemAppendByBLOCKID(m_hNote32, flagsShort, itemNameMem,
					(short) (itemNameMem==null ? 0 : itemNameMem.size()), valueBlockIdByVal,
					valueLength, retItemBlockId);
		}
		NotesErrorUtils.checkResult(result);
		
		NotesItem item = new NotesItem(this, retItemBlockId, itemType, valueBlockIdByVal);
		return item;
	}
	
	/**
	 * This function signs a document by creating a unique electronic signature and appending this
	 * signature to the note.<br>
	 * <br>
	 * A signature constitutes proof of the user's identity and serves to assure the reader that
	 * the user was the real author of the document.<br>
	 * <br>
	 * The signature is derived from the User ID. A signature item has data type {@link NotesItem#TYPE_SIGNATURE}
	 * and item flags {@link NotesConstants#ITEM_SEAL}. The data value of the signature item is a digest
	 * of the data stored in items in the note, signed with the user's private key.<br>
	 * <br>
	 * This method signs entire document. It creates a digest of all the items in the note, and
	 * appends a signature item with field name $Signature (ITEM_NAME_NOTE_SIGNATURE).<br>
	 * <br>
	 * If the document to be signed is encrypted, this function will attempt to decrypt the
	 * document in order to generate a valid signature.<br>
	 * If you want the document to be signed and encrypted, you must sign the document
	 * {@link #copyAndEncrypt(NotesUserId, EnumSet)}.<br>
	 * <br>
	 * Note:  When the Notes user interface opens a note, it always uses the {@link OpenNote#EXPAND}
	 * option to promote items of type number to number list, and items of type text to text list.<br>
	 */
	public void sign() {
		checkHandle();

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteExpand(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI64.get().NSFNoteSign(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI64.get().NSFNoteContract(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteExpand(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI32.get().NSFNoteSign(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI32.get().NSFNoteContract(m_hNote32);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function signs the note using the specified ID.
	 * It allows you to pass a flag to determine how MIME parts will be signed.<br>
	 * <br>
	 * The used C method NSFNoteSignExt3 generates a signature item with the specified name and
	 * appends this signature item to the note. A signature item has data type
	 * TYPE_SIGNATURE and item flags ITEM_SEAL. The data value of the signature item is a
	 * digest of the data stored in items in the note, signed with the user's private key.<br>
	 * <br>
	 * If the note has MIME parts and the flag <code>signNotesIfMimePresent</code> is true,
	 * it will be SMIME signed, if not set it will be Notes signed.
	 * <br>
	 * If the document to be signed is encrypted, this function will attempt to decrypt the
	 * document in order to generate a valid signature.<br>
	 * If you want the document to be signed and encrypted, you must sign the document
	 * before using {@link #copyAndEncrypt(NotesUserId, EnumSet)}.<br>
	 * <br>
	 * <b>Please note:<br>
	 * As described <a href="http://www.eknori.de/2015-01-31/serious-issue-when-signing-a-database-in-the-background-on-the-server/">here</a>,
	 * it is not possible to use this method to sign XPages design elements on a server with
	 * an ID other than the server id.</b>
	 * 
	 * @param id user id to use for signing or null for current id
	 * @param signNotesIfMimePresent If the note has MIME parts and this flag is true it will be SMIME signed, if not set it will be Notes signed.
	 */
	public void sign(NotesUserId id, boolean signNotesIfMimePresent) {
		checkHandle();

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteExpand(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI64.get().NSFNoteSignExt3(m_hNote64, id==null ? 0 : id.getHandle64(), null, NotesConstants.MAXWORD, 0, signNotesIfMimePresent ? NotesConstants.SIGN_NOTES_IF_MIME_PRESENT : 0, 0, null);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI64.get().NSFNoteContract(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			//verify signature
			NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
			Memory retSigner = new Memory(NotesConstants.MAXUSERNAME);
			Memory retCertifier = new Memory(NotesConstants.MAXUSERNAME);

			result = NotesNativeAPI64.get().NSFNoteVerifySignature (m_hNote64, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteExpand(m_hNote32);
			NotesErrorUtils.checkResult(result);

			result = NotesNativeAPI32.get().NSFNoteSignExt3(m_hNote32, id==null ? 0 : id.getHandle32(), null, NotesConstants.MAXWORD, 0, signNotesIfMimePresent ? NotesConstants.SIGN_NOTES_IF_MIME_PRESENT : 0, 0, null);
			NotesErrorUtils.checkResult(result);

			result = NotesNativeAPI32.get().NSFNoteContract(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			//verify signature
			NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
			Memory retSigner = new Memory(NotesConstants.MAXUSERNAME);
			Memory retCertifier = new Memory(NotesConstants.MAXUSERNAME);

			result = NotesNativeAPI32.get().NSFNoteVerifySignature (m_hNote32, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function will sign all hotspots in a document that contain object code.<br>
	 * <br>
	 * Using the current ID, signature data will be added to any signature containing CD
	 * records for hotspots having code within TYPE_COMPOSITE items.<br>
	 * <br>
	 * This routine only operates on documents.<br>
	 * If the note is a design element, the routine returns without signing anything.
	 * 
	 * @return true if any hotspots are signed
	 */
	public boolean signHotSpots() {
		checkHandle();
		
		IntByReference retfSigned = new IntByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteSignHotspots(m_hNote64, 0, retfSigned);
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteSignHotspots(m_hNote32, 0, retfSigned);
		}
		NotesErrorUtils.checkResult(result);
		
		return retfSigned.getValue() == 1;
	}
	
	/**
	 * Container for note signature data
	 * 
	 * @author Karsten Lehmann
	 */
	public static class SignatureData {
		private NotesTimeDate m_whenSigned;
		private String m_signer;
		private String m_certifier;
		
		private SignatureData(NotesTimeDate whenSigned, String signer, String certifier) {
			m_whenSigned = whenSigned;
			m_signer = signer;
			m_certifier = certifier;
		}
		
		public NotesTimeDate getWhenSigned() {
			return m_whenSigned;
		}
		
		public String getSigner() {
			return m_signer;
		}
		
		public String getCertifier() {
			return m_certifier;
		}
	}
	
	/**
	 * Returns the signer of a note
	 * 
	 * @return signer or empty string if not signed
	 */
	public String getSigner() {
		try {
			SignatureData signatureData = verifySignature();
			
			return signatureData.getSigner();
		}
		catch (NotesError e) {
			if (e.getId()==INotesErrorConstants.ERR_NOTE_NOT_SIGNED) {
				return "";
			}
			else {
				throw e;
			}
		}
	}
	
	/**
	 * This function verifies a signature on a note or section(s) within a note.<br>
	 * It returns an error if a signature did not verify.<br>
	 * <br>

	 * @return signer data
	 */
	public SignatureData verifySignature() {
		checkHandle();
		
		NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
		Memory retSigner = new Memory(NotesConstants.MAXUSERNAME);
		Memory retCertifier = new Memory(NotesConstants.MAXUSERNAME);
		short result;
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteExpand(m_hNote64);
			NotesErrorUtils.checkResult(result);

			result = NotesNativeAPI64.get().NSFNoteVerifySignature (m_hNote64, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI64.get().NSFNoteContract(m_hNote64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteExpand(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI32.get().NSFNoteVerifySignature (m_hNote32, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
			
			result = NotesNativeAPI32.get().NSFNoteContract(m_hNote32);
			NotesErrorUtils.checkResult(result);
		}

		String signer = NotesStringUtils.fromLMBCS(retSigner, NotesStringUtils.getNullTerminatedLength(retSigner));
		String certifier = NotesStringUtils.fromLMBCS(retCertifier, NotesStringUtils.getNullTerminatedLength(retCertifier));
		SignatureData data = new SignatureData(new NotesTimeDate(retWhenSigned), signer, certifier);
		return data;
	}
	
	/**
	 * Creates/overwrites a $REF item pointing to another {@link NotesNote}
	 * 
	 * @param note $REF target note
	 */
	public void makeResponse(NotesNote note) {
		makeResponse(note.getUNID());
	}
	
	/**
	 * Creates/overwrites a $REF item pointing to a UNID
	 * 
	 * @param targetUnid $REF target UNID
	 */
	public void makeResponse(String targetUnid) {
		replaceItemValue("$REF", EnumSet.of(ItemType.SUMMARY), new NotesUniversalNoteId(targetUnid));
	}

	/**
	 * Callback interface that receives data of images embedded in a HTML conversion result
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IHtmlItemImageConversionCallback {
		public static enum Action {Continue, Stop};
		
		/**
		 * Reports the size of the image
		 * 
		 * @param size size
		 * @return return how many bytes to skip before reading
		 */
		public int setSize(int size);
		
		/**
		 * Implement this method to receive element data
		 * 
		 * @param data data
		 * @return action, either Continue or Stop
		 */
		public Action read(byte[] data);
	}
	
	public enum HtmlConvertOption {
		/** Forces all sections to be expanded, regardless of their expansion in the Notes® rich text fields. */
		ForceSectionExpand,
		/** Forces alternate formatting of tables with tabbed sections.<br>
		 * All of the tabs are displayed at the same time, one below the other, with the tab labels included as headers. */
		RowAtATimeTableAlt,
		/** Forces all outlines to be expanded, regardless of their expansion in the Notes rich text. */
		ForceOutlineExpand,
		/** Disables passthru HTML, treating the HTML as plain text. */
		DisablePassThruHTML,
		/** Preserves Notes intraline whitespace (spaces between characters). */
		TextExactSpacing,
		/** use styles instead of &lt;FONT&gt; tags */
		FontConversion,
		/** enable new code for better representation of indented lists */
		ListFidelity;
		
		public static String[] toStringArray(EnumSet<HtmlConvertOption> options) {
			List<String> optionsAsStrList = new ArrayList<String>(options.size());
			for (HtmlConvertOption currOption : options) {
				optionsAsStrList.add(currOption.toString()+"=1");
			}
			return optionsAsStrList.toArray(new String[optionsAsStrList.size()]);
		}
	}
	
	/**
	 * Convenience method to read the binary data of a {@link IHtmlImageRef}
	 * 
	 * @param image image reference
	 * @param callback callback to receive the data
	 */
	public void convertHtmlElement(IHtmlImageRef image, IHtmlItemImageConversionCallback callback) {
		String itemName = image.getItemName();
		int itemIndex = image.getItemIndex();
		int itemOffset = image.getItemOffset();
		EnumSet<HtmlConvertOption> options = image.getOptions();
		
		convertHtmlElement(itemName, options, itemIndex, itemOffset, callback);
	}
	
	/**
	 * Method to access images embedded in HTML conversion result. Compute index and offset parameters
	 * from the img tag path like this: 1.3E =&gt; index=1, offset=63
	 * 
	 * @param itemName  rich text field which is being converted
	 * @param options conversion options
	 * @param itemIndex the relative item index -- if there is more than one, Item with the same pszItemName, then this indicates which one (zero relative)
	 * @param itemOffset byte offset in the Item where the element starts
	 * @param callback callback to receive the data
	 */
	public void convertHtmlElement(String itemName, EnumSet<HtmlConvertOption> options, int itemIndex, int itemOffset, IHtmlItemImageConversionCallback callback) {
		checkHandle();
		
		LongByReference phHTML64 = new LongByReference();
		IntByReference phHTML32 = new IntByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().HTMLCreateConverter(phHTML64);
		}
		else {
			result = NotesNativeAPI32.get().HTMLCreateConverter(phHTML32);
		}
		NotesErrorUtils.checkResult(result);
		
		long hHTML64 = phHTML64.getValue();
		int hHTML32 = phHTML32.getValue();
		
		try {
			if (!options.isEmpty()) {
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().HTMLSetHTMLOptions(hHTML64, new StringArray(HtmlConvertOption.toStringArray(options)));
				}
				else {
					result = NotesNativeAPI32.get().HTMLSetHTMLOptions(hHTML32, new StringArray(HtmlConvertOption.toStringArray(options)));
				}
				NotesErrorUtils.checkResult(result);
			}

			Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

			int totalLen;
			
			int skip;
			
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().HTMLConvertElement(hHTML64, getParent().getHandle64(), m_hNote64, itemNameMem, itemIndex, itemOffset);
				NotesErrorUtils.checkResult(result);
				
				Memory tLenMem = new Memory(4);
				result = NotesNativeAPI64.get().HTMLGetProperty(hHTML64, (long) NotesConstants.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);
				skip = callback.setSize(totalLen);
			}
			else {
				result = NotesNativeAPI32.get().HTMLConvertElement(hHTML32, getParent().getHandle32(), m_hNote32, itemNameMem, itemIndex, itemOffset);
				NotesErrorUtils.checkResult(result);
				
				Memory tLenMem = new Memory(4);
				result = NotesNativeAPI32.get().HTMLGetProperty(hHTML32, (int) NotesConstants.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);
				skip = callback.setSize(totalLen);
			}

			if (skip > totalLen)
				throw new IllegalArgumentException("Skip value cannot be greater than size: "+skip+" > "+totalLen);
			
			IntByReference len = new IntByReference();
			len.setValue(NotesConstants.MAXPATH);
			int startOffset=skip;
			Memory bufMem = new Memory(NotesConstants.MAXPATH+1);
			
			while (result==0 && len.getValue()>0 && startOffset<totalLen) {
				len.setValue(NotesConstants.MAXPATH);
				
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().HTMLGetText(hHTML64, startOffset, len, bufMem);
				}
				else {
					result = NotesNativeAPI32.get().HTMLGetText(hHTML32, startOffset, len, bufMem);
				}
				NotesErrorUtils.checkResult(result);
				
				byte[] data = bufMem.getByteArray(0, len.getValue());
				IHtmlItemImageConversionCallback.Action action = callback.read(data);
				if (action == IHtmlItemImageConversionCallback.Action.Stop)
					break;
				
				startOffset += len.getValue();
			}
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().HTMLDestroyConverter(hHTML64);
			}
			else {
				result = NotesNativeAPI32.get().HTMLDestroyConverter(hHTML32);
			}
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Method to convert the whole note to HTML
	 * 
	 * @param options conversion options
	 * @return conversion result
	 */
	public IHtmlConversionResult convertNoteToHtml(EnumSet<HtmlConvertOption> options) {
		return convertNoteToHtml(options, (EnumSet<ReferenceType>) null, (Map<ReferenceType,EnumSet<TargetType>>) null);
	}

	/**
	 * Method to convert the whole note to HTML with additional filters for the
	 * data returned in the conversion result
	 * 
	 * @param options conversion options
	 * @param refTypeFilter optional filter for ref types to be returned or null for no filter
	 * @param targetTypeFilter optional filter for target types to be returned or null for no filter
	 * @return conversion result
	 */
	public IHtmlConversionResult convertNoteToHtml(EnumSet<HtmlConvertOption> options,
			EnumSet<ReferenceType> refTypeFilter,
			Map<ReferenceType,EnumSet<TargetType>> targetTypeFilter) {
		return internalConvertItemToHtml(null, options, refTypeFilter, targetTypeFilter);
	}
	
	/**
	 * Method to convert a single item of this note to HTML
	 * 
	 * @param itemName item name
	 * @param options conversion options
	 * @return conversion result
	 */
	public IHtmlConversionResult convertItemToHtml(String itemName, EnumSet<HtmlConvertOption> options) {
		return convertItemToHtml(itemName, options, (EnumSet<ReferenceType>) null, (Map<ReferenceType,EnumSet<TargetType>>) null);
	}
	
	/**
	 * Method to convert a single item of this note to HTML with additional filters for the
	 * data returned in the conversion result
	 * 
	 * @param itemName item name
	 * @param options conversion options
	 * @param refTypeFilter optional filter for ref types to be returned or null for no filter
	 * @param targetTypeFilter optional filter for target types to be returned or null for no filter
	 * @return conversion result
	 */
	public IHtmlConversionResult convertItemToHtml(String itemName, EnumSet<HtmlConvertOption> options,
			EnumSet<ReferenceType> refTypeFilter,
			Map<ReferenceType,EnumSet<TargetType>> targetTypeFilter) {
		if (StringUtil.isEmpty(itemName))
			throw new NullPointerException("Item name cannot be null");
		
		return internalConvertItemToHtml(itemName, options, refTypeFilter, targetTypeFilter);
	}

	/**
	 * Implementation of {@link IHtmlConversionResult} that contains the HTML conversion result
	 * 
	 * @author Karsten Lehmann
	 */
	private class HtmlConversionResult implements IHtmlConversionResult {
		private String m_html;
		private List<IHtmlApiReference> m_references;
		private EnumSet<HtmlConvertOption> m_options;
		
		private HtmlConversionResult(String html, List<IHtmlApiReference> references, EnumSet<HtmlConvertOption> options) {
			m_html = html;
			m_references = references;
			m_options = options;
		}
		
		@Override
		public String getText() {
			return m_html;
		}

		@Override
		public List<IHtmlApiReference> getReferences() {
			return m_references;
		}
		
		private IHtmlImageRef createImageRef(final String refText, final String fieldName, final int itemIndex,
				final int itemOffset, final String format) {
			return new IHtmlImageRef() {
				
				@Override
				public void readImage(IHtmlItemImageConversionCallback callback) {
					NotesNote.this.convertHtmlElement(this, callback);
				}
				
				@Override
				public void writeImage(File f) throws IOException {
					if (f.exists() && !f.delete())
						throw new IOException("Cannot delete existing file "+f.getAbsolutePath());
						
					final FileOutputStream fOut = new FileOutputStream(f);
					final IOException[] ex = new IOException[1];
					try {
						NotesNote.this.convertHtmlElement(this, new IHtmlItemImageConversionCallback() {

							@Override
							public int setSize(int size) {
								return 0;
							}

							@Override
							public Action read(byte[] data) {
								try {
									fOut.write(data);
									return Action.Continue;
								} catch (IOException e) {
									ex[0] = e;
									return Action.Stop;
								}
							}
						});
						
						if (ex[0]!=null)
							throw ex[0];
					}
					finally {
						fOut.close();
					}
				}
				
				@Override
				public void writeImage(final OutputStream out) throws IOException {
					final IOException[] ex = new IOException[1];

					NotesNote.this.convertHtmlElement(this, new IHtmlItemImageConversionCallback() {

						@Override
						public int setSize(int size) {
							return 0;
						}

						@Override
						public Action read(byte[] data) {
							try {
								out.write(data);
								return Action.Continue;
							} catch (IOException e) {
								ex[0] = e;
								return Action.Stop;
							}
						}
					});
					
					if (ex[0]!=null)
						throw ex[0];
					
					out.flush();
				}
				
				@Override
				public String getReferenceText() {
					return refText;
				}
				
				@Override
				public EnumSet<HtmlConvertOption> getOptions() {
					return m_options;
				}
				
				@Override
				public int getItemOffset() {
					return itemOffset;
				}
				
				@Override
				public String getItemName() {
					return fieldName;
				}
				
				@Override
				public int getItemIndex() {
					return itemIndex;
				}
				
				@Override
				public String getFormat() {
					return format;
				}
			};
		}
		
		public java.util.List<com.mindoo.domino.jna.html.IHtmlImageRef> getImages() {
			List<IHtmlImageRef> imageRefs = new ArrayList<IHtmlImageRef>();
			
			for (IHtmlApiReference currRef : m_references) {
				if (currRef.getType() == ReferenceType.IMG) {
					String refText = currRef.getReferenceText();
					String format = "gif";
					int iFormatPos = refText.indexOf("FieldElemFormat=");
					if (iFormatPos!=-1) {
						String remainder = refText.substring(iFormatPos + "FieldElemFormat=".length());
						int iNextDelim = remainder.indexOf('&');
						if (iNextDelim==-1) {
							format = remainder;
						}
						else {
							format = remainder.substring(0, iNextDelim);
						}
					}
					
					IHtmlApiUrlTargetComponent<?> fieldOffsetTarget = currRef.getTargetByType(TargetType.FIELDOFFSET);
					if (fieldOffsetTarget!=null) {
						Object fieldOffsetObj = fieldOffsetTarget.getValue();
						if (fieldOffsetObj instanceof String) {
							String fieldOffset = (String) fieldOffsetObj;
							// 1.3E -> index=1, offset=63
							int iPos = fieldOffset.indexOf('.');
							if (iPos!=-1) {
								String indexStr = fieldOffset.substring(0, iPos);
								String offsetStr = fieldOffset.substring(iPos+1);
								
								int itemIndex = Integer.parseInt(indexStr, 16);
								int itemOffset = Integer.parseInt(offsetStr, 16);
								
								IHtmlApiUrlTargetComponent<?> fieldTarget = currRef.getTargetByType(TargetType.FIELD);
								if (fieldTarget!=null) {
									Object fieldNameObj = fieldTarget.getValue();
									String fieldName = (fieldNameObj instanceof String) ? (String) fieldNameObj : null;
									
									IHtmlImageRef newImgRef = createImageRef(refText, fieldName, itemIndex, itemOffset, format);
									imageRefs.add(newImgRef);
								}
							}
							
						}
					}
				}
			}
			
			return imageRefs;
		};
		
	}

	private static class HTMLApiReference implements IHtmlApiReference {
		private ReferenceType m_type;
		private String m_refText;
		private String m_fragment;
		private CommandId m_commandId;
		private List<IHtmlApiUrlTargetComponent<?>> m_targets;
		private Map<TargetType, IHtmlApiUrlTargetComponent<?>> m_targetByType;
		
		private HTMLApiReference(ReferenceType type, String refText, String fragment, CommandId commandId,
				List<IHtmlApiUrlTargetComponent<?>> targets) {
			m_type = type;
			m_refText = refText;
			m_fragment = fragment;
			m_commandId = commandId;
			m_targets = targets;
		}
		
		@Override
		public ReferenceType getType() {
			return m_type;
		}

		@Override
		public String getReferenceText() {
			return m_refText;
		}

		@Override
		public String getFragment() {
			return m_fragment;
		}

		@Override
		public CommandId getCommandId() {
			return m_commandId;
		}

		@Override
		public List<IHtmlApiUrlTargetComponent<?>> getTargets() {
			return m_targets;
		}

		@Override
		public IHtmlApiUrlTargetComponent<?> getTargetByType(TargetType type) {
			if (m_targetByType==null) {
				m_targetByType = new HashMap<TargetType, IHtmlApiUrlTargetComponent<?>>();
				if (m_targets!=null && !m_targets.isEmpty()) {
					for (IHtmlApiUrlTargetComponent<?> currTarget : m_targets) {
						m_targetByType.put(currTarget.getType(), currTarget);
					}
				}
			}
			return m_targetByType.get(type);
		}
	}
	
	private static class HtmlApiUrlTargetComponent<T> implements IHtmlApiUrlTargetComponent<T> {
		private TargetType m_type;
		private Class<T> m_valueClazz;
		private T m_value;
		
		private HtmlApiUrlTargetComponent(TargetType type, Class<T> valueClazz, T value) {
			m_type = type;
			m_valueClazz = valueClazz;
			m_value = value;
		}
		
		@Override
		public TargetType getType() {
			return m_type;
		}

		@Override
		public Class<T> getValueClass() {
			return m_valueClazz;
		}

		@Override
		public T getValue() {
			return m_value;
		}
	}

	/**
	 * Internal method doing the HTML conversion work
	 * 
	 * @param itemName item name to be converted or null for whole note
	 * @param options conversion options
	 * @param refTypeFilter optional filter for ref types to be returned or null for no filter
	 * @param targetTypeFilter optional filter for target types to be returned or null for no filter
	 * @return conversion result
	 */
	private IHtmlConversionResult internalConvertItemToHtml(String itemName,
			EnumSet<HtmlConvertOption> options, EnumSet<ReferenceType> refTypeFilter,
			Map<ReferenceType,EnumSet<TargetType>> targetTypeFilter) {
		checkHandle();
		
		LongByReference phHTML64 = new LongByReference();
		phHTML64.setValue(0);
		IntByReference phHTML32 = new IntByReference();
		phHTML32.setValue(0);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().HTMLCreateConverter(phHTML64);
		}
		else {
			result = NotesNativeAPI32.get().HTMLCreateConverter(phHTML32);
		}
		NotesErrorUtils.checkResult(result);
		
		long hHTML64 = phHTML64.getValue();
		int hHTML32 = phHTML32.getValue();
		
		try {
			if (!options.isEmpty()) {
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().HTMLSetHTMLOptions(hHTML64, new StringArray(HtmlConvertOption.toStringArray(options)));
				}
				else {
					result = NotesNativeAPI32.get().HTMLSetHTMLOptions(hHTML32, new StringArray(HtmlConvertOption.toStringArray(options)));
				}
				NotesErrorUtils.checkResult(result);
			}

			Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
			
			int totalLen;
			
			if (PlatformUtils.is64Bit()) {
				if (itemName==null) {
					result = NotesNativeAPI64.get().HTMLConvertNote(hHTML64, getParent().getHandle64(), m_hNote64, 0, null);
					NotesErrorUtils.checkResult(result);
				}
				else {
					result = NotesNativeAPI64.get().HTMLConvertItem(hHTML64, getParent().getHandle64(), m_hNote64, itemNameMem);
					NotesErrorUtils.checkResult(result);
				}
				
				Memory tLenMem = new Memory(4);
				result = NotesNativeAPI64.get().HTMLGetProperty(hHTML64, (long) NotesConstants.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);
			}
			else {
				if (itemName==null) {
					result = NotesNativeAPI32.get().HTMLConvertNote(hHTML32, getParent().getHandle32(), m_hNote32, 0, null);
					NotesErrorUtils.checkResult(result);
				}
				else {
					result = NotesNativeAPI32.get().HTMLConvertItem(hHTML32, getParent().getHandle32(), m_hNote32, itemNameMem);
					NotesErrorUtils.checkResult(result);
					
				}

				Memory tLenMem = new Memory(4);
				result = NotesNativeAPI32.get().HTMLGetProperty(hHTML32, NotesConstants.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);

			}
			
			IntByReference len = new IntByReference();
			int startOffset=0;
			int bufSize = 4000;
			int iLen = bufSize;
			
			byte[] bufArr = new byte[bufSize];
			
			ByteArrayOutputStream htmlTextLMBCSOut = new ByteArrayOutputStream();
			
			DisposableMemory textMem = new DisposableMemory(bufSize+1);
			try {
				while (result==0 && iLen>0 && startOffset<totalLen) {
					len.setValue(bufSize);
					textMem.setByte(0, (byte) 0);

					if (PlatformUtils.is64Bit()) {
						result = NotesNativeAPI64.get().HTMLGetText(hHTML64, startOffset, len, textMem);
					}
					else {
						result = NotesNativeAPI32.get().HTMLGetText(hHTML32, startOffset, len, textMem);
					}
					NotesErrorUtils.checkResult(result);

					iLen = len.getValue();

					if (result==0 && iLen > 0) {
						textMem.read(0, bufArr, 0, iLen);
						htmlTextLMBCSOut.write(bufArr, 0, iLen);

						startOffset += iLen;
					}
				}
			}
			finally {
				textMem.dispose();
			}

			String htmlText = NotesStringUtils.fromLMBCS(htmlTextLMBCSOut.toByteArray());
			
			Memory refCount = new Memory(4);
			
			if (PlatformUtils.is64Bit()) {
				result=NotesNativeAPI64.get().HTMLGetProperty(hHTML64, NotesConstants.HTMLAPI_PROP_NUMREFS, refCount);
			}
			else {
				result=NotesNativeAPI32.get().HTMLGetProperty(hHTML32, NotesConstants.HTMLAPI_PROP_NUMREFS, refCount);
			}
			NotesErrorUtils.checkResult(result);
			
			int iRefCount = refCount.getInt(0);

			List<IHtmlApiReference> references = new ArrayList<IHtmlApiReference>();
			
			for (int i=0; i<iRefCount; i++) {
				LongByReference phRef64 = new LongByReference();
				phRef64.setValue(0);
				IntByReference phRef32 = new IntByReference();
				phRef32.setValue(0);
				
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().HTMLGetReference(hHTML64, i, phRef64);
				}
				else {
					result = NotesNativeAPI32.get().HTMLGetReference(hHTML32, i, phRef32);
				}
				NotesErrorUtils.checkResult(result);
				
				Memory ppRef = new Memory(Native.POINTER_SIZE);
				
				long hRef64 = phRef64.getValue();
				int hRef32 = phRef32.getValue();
				
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().HTMLLockAndFixupReference(hRef64, ppRef);
				}
				else {
					result = NotesNativeAPI32.get().HTMLLockAndFixupReference(hRef32, ppRef);
				}
				NotesErrorUtils.checkResult(result);
				try {
					int iRefType;
					Pointer pRefText;
					Pointer pFragment;
					int iCmdId;
					int nTargets;
					Pointer pTargets;
					
					//use separate structs for 64/32, because RefType uses 8 bytes on 64 and 4 bytes on 32 bit
					if (PlatformUtils.is64Bit()) {
						HTMLAPIReference64Struct htmlApiRef = HTMLAPIReference64Struct.newInstance(ppRef.getPointer(0));
						htmlApiRef.read();
						iRefType = (int) htmlApiRef.RefType;
						pRefText = htmlApiRef.pRefText;
						pFragment = htmlApiRef.pFragment;
						iCmdId = (int) htmlApiRef.CommandId;
						nTargets = htmlApiRef.NumTargets;
						pTargets = htmlApiRef.pTargets;
					}
					else {
						HTMLAPIReference32Struct htmlApiRef = HTMLAPIReference32Struct.newInstance(ppRef.getPointer(0));
						htmlApiRef.read();
						iRefType = htmlApiRef.RefType;
						pRefText = htmlApiRef.pRefText;
						pFragment = htmlApiRef.pFragment;
						iCmdId = htmlApiRef.CommandId;
						nTargets = htmlApiRef.NumTargets;
						pTargets = htmlApiRef.pTargets;
					}

					ReferenceType refType = ReferenceType.getType((int) iRefType);
					
					if (refTypeFilter==null || refTypeFilter.contains(refType)) {
						String refText = NotesStringUtils.fromLMBCS(pRefText, -1);
						String fragment = NotesStringUtils.fromLMBCS(pFragment, -1);
						
						CommandId cmdId = CommandId.getCommandId(iCmdId);
						
						List<IHtmlApiUrlTargetComponent<?>> targets = new ArrayList<IHtmlApiUrlTargetComponent<?>>(nTargets);
						
						for (int t=0; t<nTargets; t++) {
							Pointer pCurrTarget = pTargets.share(t * NotesConstants.htmlApiUrlComponentSize);
							HtmlApi_UrlTargetComponentStruct currTarget = HtmlApi_UrlTargetComponentStruct.newInstance(pCurrTarget);
							currTarget.read();
							
							int iTargetType = currTarget.AddressableType;
							TargetType targetType = TargetType.getType(iTargetType);
							
							EnumSet<TargetType> targetTypeFilterForRefType = targetTypeFilter==null ? null : targetTypeFilter.get(refType);
							
							if (targetTypeFilterForRefType==null || targetTypeFilterForRefType.contains(targetType)) {
								switch (currTarget.ReferenceType) {
								case NotesConstants.URT_Name:
									currTarget.Value.setType(Pointer.class);
									currTarget.Value.read();
									String name = NotesStringUtils.fromLMBCS(currTarget.Value.name, -1);
									targets.add(new HtmlApiUrlTargetComponent(targetType, String.class, name));
									break;
								case NotesConstants.URT_NoteId:
									currTarget.Value.setType(NoteIdStruct.class);
									currTarget.Value.read();
									NoteIdStruct noteIdStruct = currTarget.Value.nid;
									int iNoteId = noteIdStruct.nid;
									targets.add(new HtmlApiUrlTargetComponent(targetType, Integer.class, iNoteId));
									break;
								case NotesConstants.URT_Unid:
									currTarget.Value.setType(NotesUniversalNoteIdStruct.class);
									currTarget.Value.read();
									NotesUniversalNoteIdStruct unidStruct = currTarget.Value.unid;
									unidStruct.read();
									String unid = unidStruct.toString();
									targets.add(new HtmlApiUrlTargetComponent(targetType, String.class, unid));
									break;
								case NotesConstants.URT_None:
									targets.add(new HtmlApiUrlTargetComponent(targetType, Object.class, null));
									break;
								case NotesConstants.URT_RepId:
									//TODO find out how to decode this one
									break;
								case NotesConstants.URT_Special:
									//TODO find out how to decode this one
									break;
								}
							}
						}
						
						IHtmlApiReference newRef = new HTMLApiReference(refType, refText, fragment,
								cmdId, targets);
						references.add(newRef);
					}
				}
				finally {
					if (PlatformUtils.is64Bit()) {
						if (hRef64!=0) {
							Mem64.OSMemoryUnlock(hRef64);
							Mem64.OSMemoryFree(hRef64);
						}
					}
					else {
						if (hRef32!=0) {
							Mem32.OSMemoryUnlock(hRef32);
							Mem32.OSMemoryFree(hRef32);
						}
					}
				}
			}
			
			return new HtmlConversionResult(htmlText, references, options);
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				if (hHTML64!=0) {
					result = NotesNativeAPI64.get().HTMLDestroyConverter(hHTML64);
				}
				
			}
			else {
				if (hHTML32!=0) {
					result = NotesNativeAPI32.get().HTMLDestroyConverter(hHTML32);
				}
			}
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Applies one of multiple conversions to a richtext item
	 * 
	 * @param itemName richtext item name
	 * @param conversions conversions, processed from left to right
	 * @return true if richtext has been updated, false if all conversion classes returned {@link IRichTextConversion#isMatch(IRichTextNavigator)} as false
	 */
	public boolean convertRichTextItem(String itemName, IRichTextConversion... conversions) {
		return convertRichTextItem(itemName, this, itemName, conversions);
	}
	
	/**
	 * Applies one of multiple conversions to a richtext item
	 * 
	 * @param itemName richtext item name
	 * @param targetNote note to copy to conversion result to
	 * @param targetItemName item name in target note where we should save the conversion result
	 * @param conversions conversions, processed from left to right
	 * @return true if richtext has been updated, false if all conversion classes returned {@link IRichTextConversion#isMatch(IRichTextNavigator)} as false
	 */
	public boolean convertRichTextItem(String itemName, NotesNote targetNote, String targetItemName, IRichTextConversion... conversions) {
		checkHandle();
		
		if (conversions==null || conversions.length==0)
			return false;
		
		IRichTextNavigator navFromNote = getRichtextNavigator(itemName);
		IRichTextNavigator currNav = navFromNote;
		
		StandaloneRichText tmpRichText = null;
		for (int i=0; i<conversions.length; i++) {
			IRichTextConversion currConversion = conversions[i];
			if (currConversion.isMatch(currNav)) {
				tmpRichText = new StandaloneRichText();
				currConversion.convert(currNav, tmpRichText);
				
				IRichTextNavigator nextNav = tmpRichText.closeAndGetRichTextNavigator();
				currNav = nextNav;
			}
		}
		
		if (tmpRichText!=null) {
			tmpRichText.closeAndCopyToNote(targetNote, targetItemName);
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * This function can be used to create basic richtext content. It uses C API methods to create
	 * "CompoundText", which provide some high-level methods for richtext creation, e.g.
	 * to add text, doclinks, render notes as richtext or append other richtext items.<br>
	 * <br>
	 * <b>After calling the available methods in the returned {@link RichTextBuilder}, you must
	 * call {@link RichTextBuilder#close()} to write your changes into the note. Otherwise
	 * it is discarded.</b>
	 * 
	 * @param itemName item name
	 * @return richtext builder
	 */
	public RichTextBuilder createRichTextItem(String itemName) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethCompound = new LongByReference();
			result = NotesNativeAPI64.get().CompoundTextCreate(m_hNote64, itemNameMem, rethCompound);
			NotesErrorUtils.checkResult(result);
			long hCompound = rethCompound.getValue();
			CompoundTextWriter ct = new CompoundTextWriter(hCompound, false);
			NotesGC.__objectCreated(CompoundTextWriter.class, ct);
			RichTextBuilder rt = new RichTextBuilder(this, ct);
			return rt;
		}
		else {
			IntByReference rethCompound = new IntByReference();
			result = NotesNativeAPI32.get().CompoundTextCreate(m_hNote32, itemNameMem, rethCompound);
			NotesErrorUtils.checkResult(result);
			int hCompound = rethCompound.getValue();
			CompoundTextWriter ct = new CompoundTextWriter(hCompound, false);
			NotesGC.__objectCreated(CompoundTextWriter.class, ct);
			RichTextBuilder rt = new RichTextBuilder(this, ct);
			return rt;
		}
	}
	
	private class RichTextNavPositionImpl implements RichTextNavPosition {
		private MultiItemRichTextNavigator m_parentNav;
		private int m_itemIndex;
		private int m_recordIndex;
		
		public RichTextNavPositionImpl(MultiItemRichTextNavigator parentNav, int itemIndex, int recordIndex) {
			m_parentNav = parentNav;
			m_itemIndex = itemIndex;
			m_recordIndex = recordIndex;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + m_itemIndex;
			result = prime * result + ((m_parentNav == null) ? 0 : m_parentNav.hashCode());
			result = prime * result + m_recordIndex;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RichTextNavPositionImpl other = (RichTextNavPositionImpl) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (m_itemIndex != other.m_itemIndex)
				return false;
			if (m_parentNav == null) {
				if (other.m_parentNav != null)
					return false;
			} else if (!m_parentNav.equals(other.m_parentNav))
				return false;
			if (m_recordIndex != other.m_recordIndex)
				return false;
			return true;
		}

		private NotesNote getOuterType() {
			return NotesNote.this;
		}
		
	}
	
	/**
	 * Implementation of {@link IRichTextNavigator} to traverse the CD record structure
	 * of a richtext item stored in a {@link NotesNote} and split up into multiple
	 * items of type {@link NotesItem#TYPE_COMPOSITE}.
	 * 
	 * @author Karsten Lehmann
	 */
	private class MultiItemRichTextNavigator implements IRichTextNavigator {
		private String m_richTextItemName;
		
		private LinkedList<NotesItem> m_items;
		private int m_currentItemIndex = -1;
		
		private LinkedList<CDRecordMemory> m_currentItemRecords;
		private int m_currentItemRecordsIndex = -1;
		
		/**
		 * Creates a new navigator and moves to the first CD record
		 * 
		 * @param richTextItemName richtext item name
		 */
		public MultiItemRichTextNavigator(String richTextItemName) {
			m_richTextItemName = richTextItemName;
			//read items
			m_items = new LinkedList<NotesItem>();
			
			getItems(richTextItemName, new IItemCallback() {

				@Override
				public void itemNotFound() {
				}

				@Override
				public Action itemFound(NotesItem item) {
					if (item.getType()==NotesItem.TYPE_COMPOSITE) {
						m_items.add(item);
					}
					return Action.Continue;
				}
			});
		}
		
		@Override
		public RichTextNavPosition getCurrentRecordPosition() {
			RichTextNavPositionImpl posImpl = new RichTextNavPositionImpl(this, m_currentItemIndex, m_currentItemRecordsIndex);
			return posImpl;
		}
		
		@Override
		public void restoreCurrentRecordPosition(RichTextNavPosition pos) {
			checkHandle();
			if (!(pos instanceof RichTextNavPositionImpl))
				throw new IllegalArgumentException("Invalid position, not generated by this navigator");

			RichTextNavPositionImpl posImpl = (RichTextNavPositionImpl) pos;
			if (posImpl.m_parentNav!=this)
				throw new IllegalArgumentException("Invalid position, not generated by this navigator");
			
			int oldItemIndex = m_currentItemIndex;
			m_currentItemIndex = posImpl.m_itemIndex;
			m_currentItemRecordsIndex = posImpl.m_recordIndex;
			
			if (posImpl.m_itemIndex==-1) {
				m_currentItemRecords = null;
			}
			else if (oldItemIndex!=posImpl.m_itemIndex) {
				//current item changed, so we need to reload the records
				NotesItem currItem = m_items.get(m_currentItemIndex);
				m_currentItemRecords = readCDRecords(currItem);
			}
		}
		
		@Override
		public boolean isEmpty() {
			checkHandle();
			if (m_items.isEmpty()) {
				return true;
			}
			else {
				boolean hasRecords = hasCDRecords(m_items.getFirst());
				return !hasRecords;
			}
		}
		
		@Override
		public boolean gotoFirst() {
			checkHandle();
			
			if (isEmpty()) {
				m_currentItemIndex = -1;
				m_currentItemRecords = null;
				m_currentItemRecordsIndex = -1;
				return false;
			}
			else if (m_currentItemRecords==null || m_currentItemIndex!=0) {
				//move to first item
				NotesItem firstItem = m_items.getFirst();
				m_currentItemRecords = readCDRecords(firstItem);
				m_currentItemIndex = 0;
			}
			
			//move to first record
			if (m_currentItemRecords.isEmpty()) {
				m_currentItemRecordsIndex = -1;
				return false;
			}
			else {
				m_currentItemRecordsIndex = 0;
				return true;
			}
		}
		
		private boolean hasCDRecords(NotesItem item) {
			final boolean[] hasRecords = new boolean[1];
			
			item.enumerateCDRecords(new ICompositeCallbackDirect() {
				
				@Override
				public ICompositeCallbackDirect.Action recordVisited(Pointer dataPtr,
						short signature, int dataLength, Pointer cdRecordPtr, int cdRecordLength) {
					hasRecords[0] = true;
					return ICompositeCallbackDirect.Action.Stop;
				}
			});
		
			return hasRecords[0];
		}
		
		/**
		 * Copies all CD records from the specified item
		 * 
		 * @param item item
		 * @return list with CD record data
		 */
		private LinkedList<CDRecordMemory> readCDRecords(NotesItem item) {
			final LinkedList<CDRecordMemory> itemRecords = new LinkedList<CDRecordMemory>();

			item.enumerateCDRecords(new ICompositeCallbackDirect() {
				
				@Override
				public ICompositeCallbackDirect.Action recordVisited(Pointer dataPtr,
						short signature, int dataLength, Pointer cdRecordPtr, int cdRecordLength) {
					
					byte[] cdRecordDataArr = cdRecordPtr.getByteArray(0, cdRecordLength);
					ReadOnlyMemory cdRecordDataCopied = new ReadOnlyMemory(cdRecordLength);
					cdRecordDataCopied.write(0, cdRecordDataArr, 0, cdRecordLength);
					cdRecordDataCopied.seal();
					
					CDRecordMemory cdRecordMem = new CDRecordMemory(cdRecordDataCopied, signature,
							dataLength, cdRecordLength);
					itemRecords.add(cdRecordMem);
					return ICompositeCallbackDirect.Action.Continue;
				}
			});
		
			return itemRecords;
		}
		
		@Override
		public boolean gotoLast() {
			checkHandle();
			
			if (isEmpty()) {
				m_currentItemIndex = -1;
				m_currentItemRecords = null;
				m_currentItemRecordsIndex = -1;
				return false;
			}
			else if (m_currentItemIndex!=(m_items.size()-1)) {
				//move to last item
				NotesItem lastItem = m_items.getLast();
				m_currentItemRecords = readCDRecords(lastItem);
				m_currentItemIndex = m_items.size()-1;
			}
			
			//move to last record
			if (m_currentItemRecords.isEmpty()) {
				m_currentItemRecordsIndex = -1;
				return false;
			}
			else {
				m_currentItemRecordsIndex = m_currentItemRecords.size()-1;
				return true;
			}
		}
		
		@Override
		public boolean gotoNext() {
			checkHandle();
			
			if (isEmpty()) {
				m_currentItemIndex = -1;
				m_currentItemRecords = null;
				m_currentItemRecordsIndex = -1;
				return false;
			}
			else {
				if (m_currentItemRecordsIndex==-1) {
					//offroad?
					return false;
				}
				else if (m_currentItemRecordsIndex<(m_currentItemRecords.size()-1)) {
					//more records available for current item
					m_currentItemRecordsIndex++;
					return true;
				}
				else {
					//move to next item
					
					if (m_currentItemIndex==-1) {
						//offroad?
						return false;
					}
					else if (m_currentItemIndex<(m_items.size()-1)) {
						//move items available
						m_currentItemIndex++;
						NotesItem currItem = m_items.get(m_currentItemIndex);
						m_currentItemRecords = readCDRecords(currItem);
						if (m_currentItemRecords.isEmpty()) {
							m_currentItemRecordsIndex = -1;
							return false;
						}
						else {
							//more to first record of that item
							m_currentItemRecordsIndex = 0;
							return true;
						}
					}
					else {
						//no more items available
						return false;
					}
				}
			}
		}
		
		@Override
		public boolean gotoPrev() {
			checkHandle();
			
			if (isEmpty()) {
				m_currentItemIndex = -1;
				m_currentItemRecords = null;
				m_currentItemRecordsIndex = -1;
				return false;
			}
			else {
				if (m_currentItemRecordsIndex==-1) {
					//offroad?
					return false;
				}
				else if (m_currentItemRecordsIndex>0) {
					//more records available for current item
					m_currentItemRecordsIndex--;
					return true;
				}
				else {
					//move to prev item
					
					if (m_currentItemIndex==-1) {
						//offroad?
						return false;
					}
					else if (m_currentItemIndex>0) {
						//move items available
						m_currentItemIndex--;
						NotesItem currItem = m_items.get(m_currentItemIndex);
						m_currentItemRecords = readCDRecords(currItem);
						if (m_currentItemRecords.isEmpty()) {
							m_currentItemRecordsIndex = -1;
							return false;
						}
						else {
							//more to last record of that item
							m_currentItemRecordsIndex = m_currentItemRecords.size()-1;
							return true;
						}
					}
					else {
						//no more items available
						return false;
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			checkHandle();
			
			if (m_items.isEmpty()) {
				return false;
			}
			else {
				if (m_currentItemRecordsIndex==-1) {
					//offroad?
					return false;
				}
				else if (m_currentItemRecordsIndex<(m_currentItemRecords.size()-1)) {
					//more records available for current item
					return true;
				}
				else {
					if (m_currentItemIndex==-1) {
						//offroad?
						return false;
					}
					else if (m_currentItemIndex<(m_items.size()-1)) {
						//more items available
						NotesItem currItem = m_items.get(m_currentItemIndex+1);
						boolean hasRecords = hasCDRecords(currItem);
						return hasRecords;
					}
					else {
						//no more items available
						return false;
					}
				}
			}
		}
		
		@Override
		public boolean hasPrev() {
			checkHandle();
			
			if (m_items.isEmpty()) {
				return false;
			}
			else {
				if (m_currentItemRecordsIndex==-1) {
					//offroad?
					return false;
				}
				else if (m_currentItemRecordsIndex>0) {
					//more records available for current item
					return true;
				}
				else {
					//move to prev item
					
					if (m_currentItemIndex==-1) {
						//offroad?
						return false;
					}
					else if (m_currentItemIndex>0) {
						//move items available
						NotesItem currItem = m_items.get(m_currentItemIndex-1);
						boolean hasRecords = hasCDRecords(currItem);
						return hasRecords;
					}
					else {
						//no more items available
						return false;
					}
				}
			}
		}
		
		private CDRecordMemory getCurrentRecord() {
			if (m_currentItemRecordsIndex==-1) {
				return null;
			}
			else {
				CDRecordMemory record = m_currentItemRecords.get(m_currentItemRecordsIndex);
				return record;
			}
		}
		
		@Override
		public Memory getCurrentRecordData() {
			CDRecordMemory record = getCurrentRecord();
			if (record==null) {
				return null;
			}
			else {
				return record.getRecordDataWithoutHeader();
			}
		}
		
		@Override
		public Memory getCurrentRecordDataWithHeader() {
			CDRecordMemory record = getCurrentRecord();
			if (record==null) {
				return null;
			}
			else {
				return record.getRecordDataWithHeader();
			}
		}
		
		@Override
		public int getCurrentRecordHeaderLength() {
			CDRecordMemory record = getCurrentRecord();
			if (record==null) {
				return 0;
			}
			else {
				return record.getRecordHeaderLength();
			}
		}
		
		@Override
		public short getCurrentRecordTypeAsShort() {
			CDRecordMemory record = getCurrentRecord();
			return record==null ? 0 : record.getTypeAsShort();
		}
		
		@Override
		public Set<CDRecordType> getCurrentRecordType() {
			CDRecordMemory record = getCurrentRecord();
			if (record==null) {
				return null;
			}
			else {
				return record.getType();
			}
		}
		
		@Override
		public int getCurrentRecordDataLength() {
			CDRecordMemory record = getCurrentRecord();
			return record==null ? 0 : record.getDataSize();
		}
		
		@Override
		public int getCurrentRecordTotalLength() {
			CDRecordMemory record = getCurrentRecord();
			return record==null ? 0 : record.getCDRecordLength();
		}
		
		@Override
		public void copyCurrentRecordTo(ICompoundText ct) {
			if (ct.isRecycled())
				throw new NotesError(0, "ICompoundText already recycled");
			
			CompoundTextWriter writer = ct.getAdapter(CompoundTextWriter.class);
			if (writer==null)
				throw new NotesError(0, "Unable to get CompoundTextWriter from RichTextBuilder");

			addCurrentRecordToCompoundTextWriter(writer);	
		}
		
		private void addCurrentRecordToCompoundTextWriter(CompoundTextWriter writer) {
			CDRecordMemory record = getCurrentRecord();
			if (record==null)
				throw new IllegalStateException("Current record is null");

			Pointer recordPtr = record.getRecordDataWithHeader();
			int recordLength = record.getCDRecordLength();
			
			if (recordPtr==null || recordLength<=0) {
				throw new NotesError(0, "The current record does not contain any data");
			}
			writer.addCDRecords(recordPtr, recordLength);
		}
			
		/**
		 * Data container for a single CD record
		 * 
		 * @author Karsten Lehmann
		 */
		private class CDRecordMemory {
			private ReadOnlyMemory m_cdRecordMemory;
			private short m_typeAsShort;
			private int m_dataSize;
			private int m_cdRecordLength;
			
			public CDRecordMemory(ReadOnlyMemory cdRecordMem, short typeAsShort, int dataSize, int cdRecordLength) {
				m_cdRecordMemory = cdRecordMem;
				m_typeAsShort = typeAsShort;
				m_dataSize = dataSize;
				m_cdRecordLength = cdRecordLength;
			}
			
			public Memory getRecordDataWithHeader() {
				return m_cdRecordMemory;
			}
			
			public Memory getRecordDataWithoutHeader() {
				return (Memory) m_cdRecordMemory.share(m_cdRecordLength - m_dataSize);
			}
			
			public short getTypeAsShort() {
				return m_typeAsShort;
			}

			public Set<CDRecordType> getType() {
				return CDRecordType.getRecordTypesForConstant(m_typeAsShort);
			}
			
			public int getDataSize() {
				return m_dataSize;
			}
			
			public int getCDRecordLength() {
				return m_cdRecordLength;
			}
			
			public int getRecordHeaderLength() {
				return m_cdRecordLength - m_dataSize;
			}
		}
	}

	/**
	 * if the document is locked, the method returns a list of current lock holders. If the
	 * document is not locked, we return an empty list.
	 * 
	 * @return lock holders or empty list
	 */
	public List<String> getLockHolders() {
		checkHandle();

		if (isNewNote()) {
			return Collections.emptyList();
		}
		
		int lockFlags = NotesConstants.NOTE_LOCK_STATUS;
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethLockers = new LongByReference();
			IntByReference retLength = new IntByReference();
			
			result = NotesNativeAPI64.get().NSFDbNoteLock(getParent().getHandle64(), getNoteId(), lockFlags,
					null, rethLockers, retLength);
			NotesErrorUtils.checkResult(result);

			long rethLockersAsLong = rethLockers.getValue();
			if (rethLockersAsLong==0 || retLength.getValue()==0)
				return Collections.emptyList();
			
			Pointer retLockersPtr = Mem64.OSLockObject(rethLockersAsLong);
			try {
				String retLockHoldersConc = NotesStringUtils.fromLMBCS(retLockersPtr, retLength.getValue());
				if (StringUtil.isEmpty(retLockHoldersConc))
					return Collections.emptyList();
				
				String[] retLockHoldersArr = retLockHoldersConc.split(";");
				return Arrays.asList(retLockHoldersArr);
			}
			finally {
				Mem64.OSUnlockObject(rethLockersAsLong);
			}
		}
		else {
			IntByReference rethLockers = new IntByReference();
			IntByReference retLength = new IntByReference();
			
			result = NotesNativeAPI32.get().NSFDbNoteLock(getParent().getHandle32(), getNoteId(), lockFlags,
					null, rethLockers, retLength);
			NotesErrorUtils.checkResult(result);

			int rethLockersAsInt = rethLockers.getValue();
			if (rethLockersAsInt==0 || retLength.getValue()==0)
				return Collections.emptyList();
			
			Pointer retLockersPtr = Mem32.OSLockObject(rethLockersAsInt);
			try {
				String retLockHoldersConc = NotesStringUtils.fromLMBCS(retLockersPtr, retLength.getValue());
				if (StringUtil.isEmpty(retLockHoldersConc))
					return Collections.emptyList();
				
				String[] retLockHoldersArr = retLockHoldersConc.split(";");
				return Arrays.asList(retLockHoldersArr);
			}
			finally {
				Mem32.OSUnlockObject(rethLockersAsInt);
			}
		}
	}

	/** Document locking mode */
	public static enum LockMode {
		/** Hard lock can only be set if Master Locking Server is available */
		Hard,
		/** Try to create a hard lock; if Master Locking Server is not available, use a provisional lock */
		HardOrProvisional,
		/** Provisional lock can be set if Master Locking Server is not available */
		Provisional
	}

	/**
	 * This function adds an "$Writers" field to a note which contains a list of "writers"
	 * who will be able to update the note.<br>
	 * <br>
	 * Any user will be able to open the note, but only the members contained in the "$Writers"
	 * field are allowed to update the note.<br>
	 * <br>
	 * This function will only succeed if the database option "Allow document locking" is set.<br>
	 * <br>
	 * Please refer to the Domino documentation for a full description of document locking.
	 * 
	 * @param lockHolder new lock holder
	 * @param mode lock mode
	 * @return true if successful, false if already locked
	 */
	public boolean lock(String lockHolder, LockMode mode) {
		return lock(Arrays.asList(lockHolder), mode);
	}
	
	/**
	 * This function adds an "$Writers" field to a note which contains a list of "writers"
	 * who will be able to update the note.<br>
	 * <br>
	 * Any user will be able to open the note, but only the members contained in the "$Writers"
	 * field are allowed to update the note.<br>
	 * <br>
	 * This function will only succeed if the database option "Allow document locking" is set.<br>
	 * <br>
	 * Please refer to the Domino documentation for a full description of document locking.
	 * 
	 * @param lockHolders new lock holders
	 * @param mode lock mode
	 * @return true if successful, false if already locked
	 */
	public boolean lock(List<String> lockHolders, LockMode mode) {
		checkHandle();

		if (isNewNote())
			throw new NotesError(0, "Note must be saved before locking");
		
		int lockFlags = 0;
		if (mode==LockMode.Hard || mode==LockMode.HardOrProvisional) {
			lockFlags = NotesConstants.NOTE_LOCK_HARD;
		}
		else if (mode==LockMode.Provisional) {
			lockFlags = NotesConstants.NOTE_LOCK_PROVISIONAL;
		}
		else
			throw new IllegalArgumentException("Missing lock mode");
		
		String lockHoldersConc = StringUtil.join(lockHolders, ";");
		Memory lockHoldersMem = NotesStringUtils.toLMBCS(lockHoldersConc, true);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethLockers = new LongByReference();
			IntByReference retLength = new IntByReference();
			
			result = NotesNativeAPI64.get().NSFDbNoteLock(getParent().getHandle64(), getNoteId(), lockFlags,
					lockHoldersMem, rethLockers, retLength);
			
			if (result==1463) { //Unable to connect to Master Lock Database
				if (mode==LockMode.HardOrProvisional) {
					result = NotesNativeAPI64.get().NSFDbNoteLock(getParent().getHandle64(), getNoteId(), NotesConstants.NOTE_LOCK_PROVISIONAL,
							lockHoldersMem, rethLockers, retLength);
				}
			}
		}
		else {
			IntByReference rethLockers = new IntByReference();
			IntByReference retLength = new IntByReference();
			
			result = NotesNativeAPI32.get().NSFDbNoteLock(getParent().getHandle32(), getNoteId(), lockFlags,
					lockHoldersMem, rethLockers, retLength);
			
			if (result==1463) { //Unable to connect to Master Lock Database
				if (mode==LockMode.HardOrProvisional) {
					result = NotesNativeAPI32.get().NSFDbNoteLock(getParent().getHandle32(), getNoteId(), NotesConstants.NOTE_LOCK_PROVISIONAL,
							lockHoldersMem, rethLockers, retLength);
				}
			}
		}
		if (result == INotesErrorConstants.ERR_NOTE_LOCKED) {
			return false;
		}
		NotesErrorUtils.checkResult(result);

		return true;
	}
	
	/**
	 * This function removes the lock on a note.<br>
	 * <br>
	 * Only the members contained in the "writers" list are allowed to remove a lock,
	 * with the exception of person(s) designated as capable of removing locks.<br>
	 * <br>
	 * Please refer to the Domino documentation for a full description of document locking.#
	 * 
	 * @param mode lock mode
	 */
	public void unlock(LockMode mode) {
		checkHandle();
		
		if (isNewNote())
			return;
		
		int lockFlags = 0;
		if (mode==LockMode.Hard || mode==LockMode.HardOrProvisional) {
			lockFlags = NotesConstants.NOTE_LOCK_HARD;
		}
		else if (mode==LockMode.Provisional) {
			lockFlags = NotesConstants.NOTE_LOCK_PROVISIONAL;
		}
		else
			throw new IllegalArgumentException("Missing lock mode");
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbNoteUnlock(getParent().getHandle64(), getNoteId(), lockFlags);
			
			if (result==1463) { //Unable to connect to Master Lock Database
				if (mode==LockMode.HardOrProvisional) {
					result = NotesNativeAPI64.get().NSFDbNoteUnlock(getParent().getHandle64(), getNoteId(), NotesConstants.NOTE_LOCK_PROVISIONAL);
				}
			}
		}
		else {
			result = NotesNativeAPI32.get().NSFDbNoteUnlock(getParent().getHandle32(), getNoteId(), lockFlags);
			
			if (result==1463) { //Unable to connect to Master Lock Database
				if (mode==LockMode.HardOrProvisional) {
					result = NotesNativeAPI32.get().NSFDbNoteUnlock(getParent().getHandle32(), getNoteId(), NotesConstants.NOTE_LOCK_PROVISIONAL);
				}
			}
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function converts the all {@link NotesItem#TYPE_COMPOSITE} (richtext) items in an open note
	 * to {@link NotesItem#TYPE_MIME_PART} items.<br>
	 * It does not update the Domino database; to update the database, call {@link #update()}.
	 * 
	 * @param concCtrl  If non-NULL, the handle to the Conversion Controls settings. If NULL, the default settings are used.
	 */
	public void convertToMime(MimeConversionControl concCtrl) {
		checkHandle();
		
		if (concCtrl!=null && concCtrl.isRecycled()) {
			throw new NotesError(0, "The conversion control object is recycled");
		}
		
		boolean isCanonical = (getFlags() & NotesConstants.NOTE_FLAG_CANONICAL) == NotesConstants.NOTE_FLAG_CANONICAL;
		boolean isMime = hasMIMEPart();
		
		Pointer ccPtr = concCtrl==null ? null : concCtrl.getAdapter(Pointer.class);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().MIMEConvertCDParts(getHandle64(), isCanonical, isMime, ccPtr);
		}
		else {
			result = NotesNativeAPI32.get().MIMEConvertCDParts(getHandle32(), isCanonical, isMime, ccPtr);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	public String getProfileName() {
		if (isGhost()) {
			String[] profileAndUsername = parseProfileAndUserName();
			if (profileAndUsername!=null) {
				return profileAndUsername[0];
			}
		}
		return "";
	}
	
	private String[] parseProfileAndUserName() {
		String name = getItemValueString("$name"); //$profile_015calendarprofile_<username>
		if (!name.startsWith("$profile_")) {
			return null;
		}
		
		String remainder = name.substring(9); //"$profile_".length()
		if (remainder.length()<3) {
			return null;
		}
		
		String profileNameLengthStr = remainder.substring(0, 3);
		int profileNameLength = Integer.parseInt(profileNameLengthStr);
		
		remainder = remainder.substring(3);
		String profileName = remainder.substring(0, profileNameLength);
		
		remainder = remainder.substring(profileNameLength+1);
		
		String userName = remainder;
		
		return new String[] {profileName, userName};
	}
	
	public String getProfileUserName() {
		if (isGhost()) {
			String[] profileAndUsername = parseProfileAndUserName();
			if (profileAndUsername!=null) {
				return profileAndUsername[1];
			}
		}
		return "";
	}
	
	/**
	 * Writes a primary key information to the note. This primary key can be used for
	 * efficient note retrieval without any lookup views.<br>
	 * <br>
	 * Both <code>category</code> and <code>objectKey</code> are combined
	 * to a string that is expected to be unique within the database.
	 * 
	 * @param category category part of primary key
	 * @param objectId object id part of primary key
	 */
	public void setPrimaryKey(String category, String objectId) {
		String name = NotesDatabase.getApplicationNoteName(category, objectId);
		replaceItemValue("$name", name);
	}
	
	/**
	 * Returns the category part of the note primary key
	 * 
	 * @return category or empty string if no primary key has been assigned
	 */
	public String getPrimaryKeyCategory() {
		String name = getItemValueString("$name");
		if (!StringUtil.isEmpty(name)) {
			String[] parsedParts = NotesDatabase.parseApplicationNamedNoteName(name);
			if (parsedParts!=null) {
				return parsedParts[0];
			}
		}
		
		return "";
	}
	
	/**
	 * Returns the object id part of the note primary key
	 * 
	 * @return object id or empty string if no primary key has been assigned
	 */
	public String getPrimaryKeyObjectId() {
		String name = getItemValueString("$name");
		if (!StringUtil.isEmpty(name)) {
			String[] parsedParts = NotesDatabase.parseApplicationNamedNoteName(name);
			if (parsedParts!=null) {
				return parsedParts[1];
			}
		}
		
		return "";
	}

	private NotesNote cloneNote() {
		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethDstNote = new LongByReference();
			result = NotesNativeAPI64.get().NSFNoteCreateClone(m_hNote64, rethDstNote);
			NotesErrorUtils.checkResult(result);
			
			NotesNote clone = new NotesNote(m_parentDb, rethDstNote.getValue());
			NotesGC.__objectCreated(NotesNote.class, clone);
			return clone;
		}
		else {
			IntByReference rethDstNote = new IntByReference();
			result = NotesNativeAPI32.get().NSFNoteCreateClone(m_hNote32, rethDstNote);
			NotesErrorUtils.checkResult(result);
			
			NotesNote clone = new NotesNote(m_parentDb, rethDstNote.getValue());
			NotesGC.__objectCreated(NotesNote.class, clone);
			return clone;
		}
	}
	
	/**
	 * Mails the note<br>
	 * Convenience function that calls {@link #send(boolean, Collection)}.
	 */
	public void send() {
		send(false, (Collection<String>) null);
	}

	/**
	 * Mails the note<br>
	 * Convenience function that calls {@link #send(boolean, Collection)}.
	 *  
	 * @param recipient The recipient of the document, may include people, groups, or mail-in databases.
	 */
	public void send(String recipient) {
		send(false, Arrays.asList(recipient));
	}

	/**
	 * Mails the note<br>
	 * Convenience function that calls {@link #send(boolean, Collection)}.
	 * 
	 * @param recipients The recipients of the document, may include people, groups, or mail-in databases.
	 */
	public void send(Collection<String> recipients) {
		send(false, recipients);
	}

	/**
	 * Mails the note<br>
	 * Convenience function that calls {@link #send(boolean, Collection)}.
	 * 
	 * @param attachform If true, the form is stored and sent along with the document. If false, it isn't. Do not attach a form that uses computed subforms.
	 */
	public void send(boolean attachform) {
		send(attachform, (Collection<String>) null);
	}

	/**
	 * Mails the note<br>
	 * Convenience function that calls {@link #send(boolean, Collection)}.
	 * 
	 * @param attachform If true, the form is stored and sent along with the document. If false, it isn't. Do not attach a form that uses computed subforms.
	 * @param recipient The recipient of the document, may include people, groups, or mail-in databases.
	 */
	public void send(boolean attachform, String recipient) {
		send(attachform, Arrays.asList(recipient));
	}

	/**
	 * Indicates whether a document is saved to a database when mailed.
	 * 
	 * @return true to save on send
	 */
	public boolean isSaveMessageOnSend() {
		return m_saveMessageOnSend;
	}

	/**
	 * Indicates whether a document is saved to a database when mailed.
	 * 
	 * @param b true to save on send
	 */
	public void setSaveMessageOnSend(boolean b) {
		m_saveMessageOnSend = b;
	}

	/**
	 * Mails the note<br>
	 * <br>
	 * Two kinds of items can affect the mailing of the document when you use send:<br>
	 * <ul>
	 * <li>If the document contains additional recipient items, such as CopyTo or BlindCopyTo, the documents mailed to those recipients.</li>
	 * <li>If the document contains items to control the routing of mail, such as DeliveryPriority, DeliveryReport, or ReturnReceipt, they are used when sending the document.</li>
	 * </ul>
	 * The {@link #isSaveMessageOnSend()} property controls whether the sent document is saved
	 * in the database. If {@link #isSaveMessageOnSend()} is true and you attach the form to the document,
	 * the form is saved with the document.<br>
	 * Sending the form increases the size of the document, but ensures that the recipient can see
	 * all of the items on the document.
	 * 
	 * @param attachform If true, the form is stored and sent along with the document. If false, it isn't. Do not attach a form that uses computed subforms.
	 * @param recipients The recipients of the document, may include people, groups, or mail-in databases.
	 */
	public void send(boolean attachform, Collection<String> recipients) {
		checkHandle();
		
		if (m_parentDb.isRecycled()) {
			throw new NotesError("Parent database is recycled");
		}
		
		short flags = 0;
		
		if (this.isSigned() || attachform) {
			flags |= NotesConstants.MSN_SIGN;
		}

		if (this.isSealed()) {
			flags |= NotesConstants.MSN_SEAL;
		}
		
		boolean cancelsend = false;
		if (cancelsend) {
			flags |= NotesConstants.MSN_PUBKEY_ONLY;
		}

		Ref<Boolean> foundNonBodyMIME = new Ref<>();
		Ref<Boolean> foundBodyMIME = new Ref<>();
		this.searchForNonBodyMIME(foundNonBodyMIME, foundBodyMIME);
		
		if (Boolean.TRUE.equals(foundNonBodyMIME.get())) {
			throw new NotesError("Found items of type MIME_PART that are not named 'Body'. This is currently unsupported.");
		}
		
		short wMailNoteFlags = NotesConstants.MAILNOTE_ANYRECIPIENT;
		
		if (Boolean.TRUE.equals(foundBodyMIME.get())) {
			wMailNoteFlags |= NotesConstants.MAILNOTE_MIMEBODY;
			wMailNoteFlags |= NotesConstants.MAILNOTE_NOTES_ENCRYPT_MIME;
		}
		
		/* SPR ajrs39vm4l: We're about to modify the user's note.
		 * If they do something stupid, like send it a second time,
		 * we're going to modify it again, ending up with all sorts
		 * of duplicate items that will cause problems for the recipient.
		 * So, we fix that by cloning the hnote here, making all
		 * modifications on the copy.
		 * 
		 * Note that we use NULL for the hdb when creating the note
		 * to prevent a network round trip. Then we have to set the
		 * hdb explicitly into the note. Later, after sending the thing,
		 * we have to check to see if the original hnote needs updating
		 * (for example, if the current note is not on disk, but the
		 * user specified "save on send", then we have to update the
		 * note id).
		 */

		//create a clone to not modify this note instance and prevent issues when sending a second time
		NotesNote tmpNote = cloneNote();
		
		// now copy all items to the new note
		if (PlatformUtils.is64Bit()) {
			short copyItemsResult = NotesNativeAPI64.get().NSFNoteReplaceItems(getHandle64(), tmpNote.getHandle64(),
					null, true);
			NotesErrorUtils.checkResult(copyItemsResult);
		}
		else {
			short copyItemsResult = NotesNativeAPI32.get().NSFNoteReplaceItems(getHandle32(), tmpNote.getHandle32(),
					null, true);
			NotesErrorUtils.checkResult(copyItemsResult);
		}

		// did caller provide a recipients list?
		if (recipients!=null) {
			// if there's already a SendTo item, delete it
			NotesItem sendto = tmpNote.getFirstItem(NotesConstants.MAIL_SENDTO_ITEM);
			if (sendto!=null) {
				sendto.remove();
			}

			recipients = NotesNamingUtils.toCanonicalNames(recipients);
			tmpNote.replaceItemValue(NotesConstants.MAIL_SENDTO_ITEM, recipients);
		}

		if (tmpNote.hasItem(NotesConstants.MAIL_COPYTO_ITEM)) {
			List<String> copyToNames = tmpNote.getItemValueStringList(NotesConstants.MAIL_COPYTO_ITEM);
			copyToNames = NotesNamingUtils.toCanonicalNames(copyToNames);
			tmpNote.replaceItemValue(NotesConstants.MAIL_COPYTO_ITEM, copyToNames);
		}
		
		if (tmpNote.hasItem(NotesConstants.MAIL_BLINDCOPYTO_ITEM)) {
			List<String> blindCopyToNames = tmpNote.getItemValueStringList(NotesConstants.MAIL_COPYTO_ITEM);
			blindCopyToNames = NotesNamingUtils.toCanonicalNames(blindCopyToNames);
			tmpNote.replaceItemValue(NotesConstants.MAIL_BLINDCOPYTO_ITEM, blindCopyToNames);
		}

		if (!tmpNote.hasItem(NotesConstants.MAIL_SENDTO_ITEM) && !tmpNote.hasItem(NotesConstants.MAIL_COPYTO_ITEM) &&
				!tmpNote.hasItem(NotesConstants.MAIL_BLINDCOPYTO_ITEM)) {
			throw new NotesError(0, "Missing mail recipient items");
		}

		/* To attach the form, find it in the database, get the all
		 * items from the form note beginning with '$' (with some
		 * exceptions), and copy them to the mail note. Then,
		 * we have to delete the Form item, which points to the original
		 * form name. By deleting it, we're telling the editor to look
		 * for the stored form instead.
		 * 
		 * If the form contains a SUBFORM_ITEM (textlist of the
		 * names of subforms used in the form), then we have
		 * to copy a bunch of sub-form stuff too
		 */

		if (attachform) {
			// Remove old stored form items before adding items from another form
			short removeStoredFormResult;
			if (PlatformUtils.is64Bit()) {
				removeStoredFormResult = NotesNativeAPI64.get().StoredFormRemoveItems(tmpNote.getHandle64(), 0);
			}
			else {
				removeStoredFormResult = NotesNativeAPI32.get().StoredFormRemoveItems(tmpNote.getHandle32(), 0);
			}
			NotesErrorUtils.checkResult(removeStoredFormResult);
			
			NotesItem formname = tmpNote.getFirstItem(NotesConstants.FIELD_FORM);
			NotesItem body = tmpNote.getFirstItem(NotesConstants.ITEM_NAME_TEMPLATE);
			
			IntByReference fnid = new IntByReference();
			fnid.setValue(0);
			
			if (formname!=null) {
				String formnameStr = tmpNote.getItemValueString(NotesConstants.FIELD_FORM);
				/* Delete the form item from the note, we copied the name.
				 * We don't want a Form item on the note when we attach
				 * the form itself, otherwise the editor gets confused.
				 * Doing the Remove also deletes the object. */
				formname.remove();
				
				if (!StringUtil.isEmpty(formnameStr)) {
					Memory formnameStrMem = NotesStringUtils.toLMBCS(formnameStr, true);
					PointerByReference pName = new PointerByReference();
					ShortByReference wNameLen = new ShortByReference();
					PointerByReference pAlias = new PointerByReference();
					ShortByReference wAliasLen = new ShortByReference();
					
					NotesNativeAPI.get().DesignGetNameAndAlias(formnameStrMem, pName, wNameLen, pAlias, wAliasLen);
					
					DisposableMemory szBuffer = new DisposableMemory(NotesConstants.DESIGN_ALL_NAMES_MAX);
					szBuffer.clear();
					
					if (wAliasLen.getValue()>0) {
						byte[] aliasArr = pAlias.getValue().getByteArray(0, (int) (wAliasLen.getValue() & 0xffff));
						szBuffer.write(0, aliasArr, 0, aliasArr.length);
					}
					else {
						byte[] wNameArr = pName.getValue().getByteArray(0, (int) (wNameLen.getValue() & 0xffff));
						szBuffer.write(0, wNameArr, 0, wNameArr.length);
					}
					
					short lkFormResult;
					Memory szFlagsPatternMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWFORM_ALL_VERSIONS, true);
					
					if (PlatformUtils.is64Bit()) {
						lkFormResult = NotesNativeAPI64.get().DesignLookupNameFE(m_parentDb.getHandle64(),
								NotesConstants.NOTE_CLASS_FORM, szFlagsPatternMem,
								szBuffer, wAliasLen.getValue()>0 ? wAliasLen.getValue() : wNameLen.getValue(),
										NotesConstants.DGN_ONLYSHARED, fnid, (IntByReference) null, 
										(NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC) null, null);
					}
					else {
						lkFormResult = NotesNativeAPI32.get().DesignLookupNameFE(m_parentDb.getHandle32(),
								NotesConstants.NOTE_CLASS_FORM, szFlagsPatternMem,
								szBuffer, wAliasLen.getValue()>0 ? wAliasLen.getValue() : wNameLen.getValue(),
										NotesConstants.DGN_ONLYSHARED, fnid, (IntByReference) null, 
										(NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC) null, null);
						
					}
					
					if (lkFormResult!=0) {
						/* Try private. */
						if (PlatformUtils.is64Bit()) {
							lkFormResult = NotesNativeAPI64.get().DesignLookupNameFE(m_parentDb.getHandle64(),
									NotesConstants.NOTE_CLASS_FORM, szFlagsPatternMem,
									szBuffer, wAliasLen.getValue()>0 ? wAliasLen.getValue() : wNameLen.getValue(),
											NotesConstants.DGN_ONLYPRIVATE, fnid, (IntByReference) null, 
											(NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC) null, null);
						}
						else {
							lkFormResult = NotesNativeAPI32.get().DesignLookupNameFE(m_parentDb.getHandle32(),
									NotesConstants.NOTE_CLASS_FORM, szFlagsPatternMem,
									szBuffer, wAliasLen.getValue()>0 ? wAliasLen.getValue() : wNameLen.getValue(),
											NotesConstants.DGN_ONLYPRIVATE, fnid, (IntByReference) null, 
											(NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC) null, null);
						}
					}

					if (lkFormResult==0) {
						// delete existing $body
						while (tmpNote.hasItem(NotesConstants.ITEM_NAME_TEMPLATE)) {
							tmpNote.removeItem(NotesConstants.ITEM_NAME_TEMPLATE);
						}

						NotesNote formNote = getParent().openNoteById(fnid.getValue(), EnumSet.of(OpenNote.CACHE));
						if (formNote!=null) {
							List<String> excludeList = Arrays.asList(
									NotesConstants.DESIGN_CLASS,
									NotesConstants.DESIGN_FLAGS,
									NotesConstants.FIELD_UPDATED_BY);
							
							// do the $ items
							copyFormItems(formNote, tmpNote, excludeList);
							
							// check for subforms
							copySubformItems(formNote, tmpNote);
							
							// this will add the new, more secure style of stored form and subform items
							// to the target doc
							// Note: This has to be done after $Body item(s) have been added to the note
							// by copyFormItems

							short addStoredFormResult;
							if (PlatformUtils.is64Bit()) {
								addStoredFormResult = NotesNativeAPI64.get().StoredFormAddItems(m_parentDb.getHandle64(),
										formNote.getHandle64(),
										tmpNote.getHandle64(), true, 0);
							}
							else {
								addStoredFormResult = NotesNativeAPI32.get().StoredFormAddItems(m_parentDb.getHandle32(),
										formNote.getHandle32(),
										tmpNote.getHandle32(), true, 0);
								
							}
							NotesErrorUtils.checkResult(addStoredFormResult);
							
							formNote.recycle();
						}
					}
				}
			}
			else {
				/* If have body and blank form, use existing body */
				if (body==null) {
					throw new NotesError("Found no form name in the note to look up the form to attach");
				}
			}
		}
		
		final boolean isMsgComingFromAgent = false;
		if (isMsgComingFromAgent) {
			if (!tmpNote.hasItem(NotesConstants.ASSIST_MAIL_ITEM)) {
				tmpNote.replaceItemValue(NotesConstants.ASSIST_MAIL_ITEM, "1");
			}
		}
		
		/* IETF standard auto flag  */
		tmpNote.replaceItemValue(NotesConstants.MAIL_ITEM_AUTOSUBMITTED, NotesConstants.MAIL_AUTOGENERATED);
		
		/* If this is happening on a server, then we want to tag the
		 * message as being from the effective user, not from the server.
		 * Always check for the special "from" item, though, in case the
		 * user is getting tricky with us */

		// if there's already a From item, delete it
		NotesItem from = tmpNote.getFirstItem(NotesConstants.MAIL_FROM_ITEM);
		if (from!=null) {
			from.remove();
		}
		
		if (IDUtils.isOnServer() || Boolean.TRUE.equals(NotesGC.getCustomValue("notesnote.sendasotheruser"))) { // we added a flag here to test this in the client
			String effUserName = IDUtils.getEffectiveUsername();
			if (!StringUtil.isEmpty(effUserName)) {
				effUserName = NotesNamingUtils.toCanonicalName(effUserName);
				
				tmpNote.replaceItemValue(NotesConstants.MAIL_FROM_ITEM, effUserName);
			}
		}
		
		// Contract before sending to be editor-compatible
		short contractResult;
		if (PlatformUtils.is64Bit()) {
			contractResult = NotesNativeAPI64.get().NSFNoteContract(tmpNote.getHandle64());
		}
		else {
			contractResult = NotesNativeAPI32.get().NSFNoteContract(tmpNote.getHandle32());
		}
		NotesErrorUtils.checkResult(contractResult);

		/*spr bban3kzhk9 -- allow sendto AND/OR copyto AND/OR blindcopyto */
		/* snis6z2taf et al. -- reinstate ability to MIME encrypt, 
		   by flagging any MIME body for the mailer */
		short mailNoteResult;
		if (PlatformUtils.is64Bit()) {
			mailNoteResult = NotesNativeAPI64.get().MailNoteJitEx2(null, tmpNote.getHandle64(), flags, null,
					NotesConstants.MAIL_NO_JIT, wMailNoteFlags, null, null);
		}
		else {
			mailNoteResult = NotesNativeAPI32.get().MailNoteJitEx2(null, tmpNote.getHandle32(), flags, null,
					NotesConstants.MAIL_NO_JIT, wMailNoteFlags, null, null);
		}
		NotesErrorUtils.checkResult(mailNoteResult);

		/* must replace certain critical item(s) on original note with their new after
		 * mailing values so that the note will appear in 'Sent' view, etc.
		 * Note: wholesale replacement of all items with new values will result in a
		 * regression problem with spr ajrs39vm4l
		 */

		/* here we query the new copy for posted date */
		NotesTimeDate postedDate = tmpNote.getItemValueAsTimeDate(NotesConstants.MAIL_POSTEDDATE_ITEM);

		removeItem(NotesConstants.MAIL_POSTEDDATE_ITEM);
		if (postedDate!=null) {
			replaceItemValue(NotesConstants.MAIL_POSTEDDATE_ITEM, postedDate);
		}

		// save the msg?
		if (isSaveMessageOnSend()) {
			// Message recall does not work for mails sent from a DIIOP program
			// need to generate message id for the recall feature to work. 
			// Make sure deleting the existing message id if it is present and re-create new one to match with 
			// the one in local mail.box for the recall feature to work.

			if (hasItem(NotesConstants.MAIL_ID_ITEM)) {
				removeItem(NotesConstants.MAIL_ID_ITEM);
			}
			
			DisposableMemory messageId = new DisposableMemory(NotesConstants.MAXPATH+1);
			short setMsgIdResult;
			if (PlatformUtils.is64Bit()) {
				setMsgIdResult = NotesNativeAPI64.get().MailSetSMTPMessageID(m_hNote64, null, messageId, (short) (NotesConstants.MAXPATH & 0xffff));
			}
			else {
				setMsgIdResult = NotesNativeAPI32.get().MailSetSMTPMessageID(m_hNote32, null, messageId, (short) (NotesConstants.MAXPATH & 0xffff));
			}
			NotesErrorUtils.checkResult(setMsgIdResult);
			
			String messageIdStr = NotesStringUtils.fromLMBCS(messageId, -1);
			replaceItemValue(NotesConstants.MAIL_ID_ITEM, messageIdStr);
			
			// we save the original note, not the new one
			update();
		}
	
		// now we can kill the temp note and reset
		tmpNote.recycle();
	}
	
	/**
	 * copy relevant subform items
	 * 
	 * @param formNote source note
	 * @param tmpNote target note
	 */
	private void copySubformItems(NotesNote formNote, NotesNote tmpNote) {
		checkHandle();
		if (m_parentDb.isRecycled()) {
			throw new NotesError("Parent DB is recycled");
		}
		
		List<String> exclude_list = Arrays.asList(
				NotesConstants.DESIGN_CLASS,
				NotesConstants.DESIGN_FLAGS,
				NotesConstants.FIELD_UPDATED_BY,
				NotesConstants.ITEM_NAME_TEMPLATE,
				NotesConstants.FIELD_TITLE,
				NotesConstants.ITEM_NAME_DOCUMENT
				);
		
		/* If the form doesn't contain a subform name list, then there's nothing to do */
		if (!formNote.hasItem(NotesConstants.SUBFORM_ITEM_NAME)) {
			return;
		}
		
		// get the list, iterate over the subform names
		List<String> subformNames = formNote.getItemValueStringList(NotesConstants.SUBFORM_ITEM_NAME);
		for (int i = 0; i < subformNames.size(); i++) {
			String subformname = subformNames.get(i);
			Memory subformnameMem = NotesStringUtils.toLMBCS(subformname, true);
			
			if (NotesNativeAPI.get().StoredFormHasSubformToken(subformnameMem)) {
				continue;
			}
			
			PointerByReference ppName = new PointerByReference();
			ShortByReference pNameLen = new ShortByReference();
			PointerByReference ppAlias = new PointerByReference();
			ShortByReference pAliasLen = new ShortByReference();
			
			NotesNativeAPI.get().DesignGetNameAndAlias(subformnameMem,
					ppName, pNameLen, ppAlias, pAliasLen);
			
			Pointer subformaliasMem;
			short subformaliasLen;
			
			if (Short.toUnsignedInt(pAliasLen.getValue()) > 0) {
				subformaliasMem = ppAlias.getValue();
				subformaliasLen = pAliasLen.getValue();
			}
			else {
				subformaliasMem = ppName.getValue();
				subformaliasLen = pNameLen.getValue();
			}
			String subformalias = NotesStringUtils.fromLMBCS(subformaliasMem, Short.toUnsignedInt(subformaliasLen));
			
			Memory designPatternMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SUBFORM_ALL_VERSIONS, true);
			
			IntByReference fnid = new IntByReference();
			
			short lkDesignResult;
			if (PlatformUtils.is64Bit()) {
				lkDesignResult = NotesNativeAPI64.get().DesignLookupNameFE(m_parentDb.getHandle64(), NotesConstants.NOTE_CLASS_FORM,
						designPatternMem, subformaliasMem, subformaliasLen, NotesConstants.DGN_ONLYSHARED,
						fnid, null, null, null);
			}
			else {
				lkDesignResult = NotesNativeAPI32.get().DesignLookupNameFE(m_parentDb.getHandle32(), NotesConstants.NOTE_CLASS_FORM,
						designPatternMem, subformaliasMem, subformaliasLen, NotesConstants.DGN_ONLYSHARED,
						fnid, null, null, null);
			}
			
			if (lkDesignResult!=0) {
				/* Try private. */
				if (PlatformUtils.is64Bit()) {
					lkDesignResult = NotesNativeAPI64.get().DesignLookupNameFE(m_parentDb.getHandle64(), NotesConstants.NOTE_CLASS_FORM,
							designPatternMem, subformaliasMem, subformaliasLen, NotesConstants.DGN_ONLYPRIVATE,
							fnid, null, null, null);
				}
				else {
					lkDesignResult = NotesNativeAPI32.get().DesignLookupNameFE(m_parentDb.getHandle32(), NotesConstants.NOTE_CLASS_FORM,
							designPatternMem, subformaliasMem, subformaliasLen, NotesConstants.DGN_ONLYPRIVATE,
							fnid, null, null, null);
				}
			}
			
			if (lkDesignResult!=0) {
				continue;
			}

			try {
				NotesNote subformNote = m_parentDb.openNoteById(fnid.getValue(), EnumSet.of(OpenNote.CACHE));
				if (subformNote!=null) {
					copyFormItems(subformNote, tmpNote, exclude_list, (short) ((i+2) & 0xffff), subformname);
				}
			}
			catch (NotesError e) {
				throw new NotesError(e.getId(), "Error opening subform "+subformalias, e);
			}
		}
	}

	/**
	 * copy specified items from form when mailing
	 * 
	 * @param formNote form note
	 * @param tmpNote target note
	 * @param excludeList exclude list of item names
	 */
	private void copyFormItems(NotesNote formNote, NotesNote tmpNote, List<String> excludeList) {
		copyFormItems(formNote, tmpNote, excludeList, (short) 0, null);
	}
	
	/**
	 * copy specified items from form when mailing
	 * 
	 * @param formNote form note
	 * @param tmpNote target note
	 * @param excludeList exclude list of item names
	 * @param namemod counter
	 * @param subformname subform name or null
	 */
	private void copyFormItems(NotesNote formNote, NotesNote tmpNote, List<String> excludeList,
			short namemod, String subformname) {
		checkHandle();
		
		// table of items which get renamed, if this is a subform copy
		List<String> rename_list = Arrays.asList(
				NotesConstants.FORM_SCRIPT_ITEM_NAME,
				NotesConstants.DOC_SCRIPT_ITEM,
				NotesConstants.DOC_SCRIPT_NAME,
				NotesConstants.DOC_ACTION_ITEM
				
				);
		
		/* These are the object-code item names, derived
		from the ones above (prepended '$', appended "_O") */
		List<String> rename_list_special = Arrays.asList(
				"$" + NotesConstants.FORM_SCRIPT_ITEM_NAME + "_O",
				"$" + NotesConstants.DOC_SCRIPT_ITEM + "_O"
				);
		
		/* Copy all items beginning with '$' from the form note to 
		 * the current note. Certain item names are skipped for the
		 * form ($FLAGS, $CLASS, $UPDATEDBY) and for subforms (indicated by
		 * namemod > 0) ($TITLE, $BODY).
		 * 
		 * If the "namemod" argument is > 0, then we also have to make
		 * the new item name different: append the integer to the name
		 * again, for selected item names.
		 */
		formNote.getItems((item, loop) -> {
			String itemName = item.getName();
			if (itemName.startsWith("$")) {
				boolean exclude = false;
				
				if (ListUtil.containsIgnoreCase(excludeList, itemName)) {
					exclude = true;
				}
				else if (NotesConstants.ITEM_NAME_NOTE_SIGNATURE.equals(itemName)) {
					exclude = true;
				}
				
				if (!exclude) {
					/* If the item's name is not being modified, then use the
					easy ItemCopy call. If it is, though, we have to do a
					bit more work. If this is a subform, check the rename
					list to see if it really is getting renamed.

					Note that there are script object-code items with the
					same name as the source-code items, but with an appended
					"_O". If we find one of those, then we have to specially
					rename it, the number goes before the _O, not after
				*/
					
					boolean rename = false;
					boolean rename_special = false;
					boolean rename_signature = false;

					String itemname2 = "";
					
					if (NotesConstants.ITEM_NAME_NOTE_SIGNATURE.equalsIgnoreCase(itemName)) {
						rename = true;
						rename_signature = true;
						
						if (namemod == 0) {
							itemname2 = NotesConstants.ITEM_NAME_NOTE_STOREDFORM_SIG;
						}
						else {
							itemname2 = NotesConstants.ITEM_NAME_NOTE_STOREDFORM_SIG_PREFIX + subformname;
							if (itemname2.length() > NotesConstants.MAXPATH) {
								itemname2 = itemname2.substring(0, NotesConstants.MAXPATH);
							}
						}
						
					}
					
					if (namemod>0 && !rename_signature) {
						if (ListUtil.containsIgnoreCase(rename_list, itemName)) {
							rename = true;
						}
						else if (ListUtil.containsIgnoreCase(rename_list_special, itemName)) { // check for special
							rename = true;
							rename_special = true;
						}
					}
					
					// "special" rename?
					if (rename_special) {
						itemname2 = itemName.substring(0, itemName.length()-2); // trim _O
						itemname2 += Short.toString(namemod);
					}
					else if (!rename_signature) {
						itemname2 = itemName + Short.toString(namemod);
					}
					
					if (rename) {
						//we need to check if this call does the same as the C code below
						item.copyToNote(tmpNote, itemname2, false);
					}
					else {
						item.copyToNote(tmpNote, false);
					}
				}
			}
		});
	}

	/**
	 * detect any MIME_PART items not named "Body"; if not found, detect any TYPE_MIME_PART item named "Body"
	 * 
	 * @param foundNonBodyMIME returns {@link Boolean#TRUE} if there are TYPE_MIME_PART items with a different name than "body"
	 * @param foundBodyMIME returns {@link Boolean#TRUE} if there are TYPE_MIME_PART items with the name than "body"
	 */
	private void searchForNonBodyMIME(Ref<Boolean> foundNonBodyMIME, Ref<Boolean> foundBodyMIME) {
		foundNonBodyMIME.set(Boolean.FALSE);
		foundBodyMIME.set(Boolean.FALSE);
		
		/* Optimization: assume by this point that if there is no $NoteHasNativeMIME item, then
		 * there aren't any MIME_PART items at all.
		 */	
		if (!hasItem(NotesConstants.ITEM_IS_NATIVE_MIME)) {
			return;
		}
		
		/* Finally, scan all the items, looking for non-"Body" item of TYPE_MIME_PART */
		getItems((item, loop) -> {
			if (item.getType() == NotesItem.TYPE_MIME_PART) {
				String itemName = item.getName();
				if (NotesConstants.MAIL_BODY_ITEM.equalsIgnoreCase(itemName)) {
					// don't stop here; finding MIME "Body" is secondary to finding MIME non-"Body"s
					foundBodyMIME.set(Boolean.TRUE);
				}
				else {
					foundNonBodyMIME.set(Boolean.TRUE);
					loop.stop();
				}
			}
		});
	}
}
