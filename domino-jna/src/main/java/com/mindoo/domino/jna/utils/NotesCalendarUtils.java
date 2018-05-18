package com.mindoo.domino.jna.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.NotesUniversalNoteId;
import com.mindoo.domino.jna.constants.CalendarNoteOpen;
import com.mindoo.domino.jna.constants.CalendarRead;
import com.mindoo.domino.jna.constants.CalendarReadRange;
import com.mindoo.domino.jna.constants.CalendarWrite;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.CalNoteOpenData32;
import com.mindoo.domino.jna.internal.CalNoteOpenData64;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to access the Calendaring and Scheduling APIs of Domino.<br>
 * <br>
 * <i>Please note that this class is not feature complete. We currently support
 * creating, updating and reading appointments, but we do not yet support Domino's
 * meeting workflow (accept/decline etc.)</i>
 * 
 * @author Karsten Lehmann
 */
public class NotesCalendarUtils {

	/**
	 * Creates a calendar entry.<br>
	 * <br>
	 * This supports either a single entry, or a recurring entry which may contain multiple
	 * VEVENTS represenging both the series and exception data. The iCalendar input must only
	 * contain data for a single UID.For meetings, ATTENDEE PARTSTAT data is ignored.<br>
	 * If the mailfile owner is the meeting organizer, invitations will be sent out to meeting
	 * participants (unless {@link CalendarWrite#DISABLE_IMPLICIT_SCHEDULING} is specified)
	 * 
	 * The following errors will be thrown by this method:
	 * <ul>
	 * <li>NOERROR - on success</li>
	 * <li>{@link INotesErrorConstants#ERR_NULL_DBHANDLE} - The database handle is NULL</li>
	 * <li>ERR_MISC_INVALID_ARGS - Unexpected arguments provided</li>
	 * <li>ERR_NO_CALENDAR_FOUND - Unable to find the entry because the required view does not exist in this database</li>
	 * <li>ERR_EXISTS				An entry already exists</li>
	 * <li>ERR_CS_PROFILE_NOOWNER - Calendar Profile does not specify owner</li>
	 * <li>ERR_UNEXPECTED_METHOD - Provided iCalendar contains a method (no method expected here)</li>
	 * <li>ERR_MissingVEventComponents - Missing required VEvent components</li>
	 * <li>ERR_InvalidVEventPropertyFound - Invalid VEvent property found</li>
	 * <li>ERR_ICAL2NOTE_CONVERT - Error interpereting iCalendar input</li>
	 * <li>ERR_MISC_UNEXPECTED_ERROR - Unexpected internal error</li>
	 * <li>ERR_IMPLICIT_SCHED_FAILED - Entry was updated, but errors were encountered sending notices to meeting participants</li>
	 * </ul>	
	 * 
	 * @param dbMail The database where the entry will be created.
	 * @param iCal The iCalendar data representing the entry to create
	 * @param flags {@link CalendarWrite} flags to control non-default behavior
	 * @return the UID of the created iCalendar
	 */
	public static String createCalendarEntry(NotesDatabase dbMail, String iCal, EnumSet<CalendarWrite> flags) {
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");
		
		Memory icalMem = NotesStringUtils.toLMBCS(iCal, true);
		
		int dwFlags = flags==null ? 0 : CalendarWrite.toBitMask(flags);
		
		String retUID=null;
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference hRetUID = new LongByReference();
			result = NotesNativeAPI64.get().CalCreateEntry(dbMail.getHandle64(), icalMem, dwFlags, hRetUID, null);
			NotesErrorUtils.checkResult(result);

			long hRetUIDLong = hRetUID.getValue();
			if (hRetUIDLong!=0) {
				Pointer retUIDPtr = Mem64.OSMemoryLock(hRetUIDLong);
				try {
					retUID = NotesStringUtils.fromLMBCS(retUIDPtr, -1);
				}
				finally {
					Mem64.OSMemoryUnlock(hRetUIDLong);
					Mem64.OSMemoryFree(hRetUIDLong);
				}
			}
		}
		else {
			IntByReference hRetUID = new IntByReference();
			result = NotesNativeAPI32.get().CalCreateEntry(dbMail.getHandle32(), icalMem, dwFlags, hRetUID, null);
			NotesErrorUtils.checkResult(result);

			int hRetUIDInt = hRetUID.getValue();
			if (hRetUIDInt!=0) {
				Pointer retUIDPtr = Mem32.OSMemoryLock(hRetUIDInt);
				try {
					retUID = NotesStringUtils.fromLMBCS(retUIDPtr, -1);
				}
				finally {
					Mem32.OSMemoryUnlock(hRetUIDInt);
					Mem32.OSMemoryFree(hRetUIDInt);
				}
			}
		}
		return retUID;
	}
	
	/**
	 * This will modify an existing calendar entry.<br>
	 * <br>
	 * This supports either single entries or recurring entries, but recurring entries will only
	 * support updates for a single instance specified via RECURRENCE-ID that may not include a
	 * RANGE (This may be permitted in the future but for now will return an error).<br>
	 * <br>
	 * The iCalendar input may only contain a single VEVENT and must contain a UID.<br>
	 * By default, attachments and custom data (for fields contained in $CSCopyItems) will be
	 * maintained from the existing calendar entry.  Similarly, description will also be maintained
	 * if it is not specified in the iCalendar content that is updating.<br>
	 * <br>
	 * Both of these behaviors can be canceled via the CAL_WRITE_COMPLETE_REPLACE flag.<br>
	 * If the mailfile owner is the meeting organizer, appropriate notices will be sent out
	 * to meeting participants (unless {@link CalendarWrite#DISABLE_IMPLICIT_SCHEDULING} is specified).<br>
	 * <br>
	 * The following errors will be thrown by this method:
	 * <ul>
	 * <li>NOERROR - on success</li>
	 * <li>{@link INotesErrorConstants#ERR_NULL_DBHANDLE} - The database handle is NULL</li>
	 * <li>ERR_MISC_INVALID_ARGS - Unexpected arguments provided</li>
	 * <li>ERR_CALACTION_INVALID - This calendar entry is not in a state where updating it is supported.</li>
	 * <li>ERR_NO_CALENDAR_FOUND - Unable to find the entry because the required view does not exist in this database</li>
	 * <li>ERR_NOT_FOUND - There are no entries that match the specified UID or UID/recurid in the database</li>
	 * <li>ERR_NOT_YET_IMPLEMENTED - This update is not yet supported (update range or multiple VEVENTs?)</li>
	 * <li>ERR_CS_PROFILE_NOOWNER - Calendar Profile does not specify owner</li>
	 * <li>ERR_UNEXPECTED_METHOD - Provided iCalendar contains a method (no method expected here)</li>
	 * <li>ERR_ICAL2NOTE_OUTOFDATE - iCalendar input is out of date in regards to sequence information.</li>
	 * <li>ERR_MissingVEventComponents - Missing required VEvent components</li>
	 * <li>ERR_InvalidVEventPropertyFound - Invalid VEvent property found</li>
	 * <li>ERR_ICAL2NOTE_CONVERT - Error interpereting iCalendar input</li>
	 * <li>ERR_MISC_UNEXPECTED_ERROR - Unexpected internal error</li>
	 * <li>ERR_IMPLICIT_SCHED_FAILED - Entry was updated, but errors were encountered sending notices to meeting participants</li>
	 * </ul>
	 * 
	 * @param dbMail The database containing the entry to update
	 * @param iCal The iCalendar data representing the updated entry
	 * @param uid If non-NULL, this value MUST match the UID value in the iCalendar input. If present,or else this returns ERR_InvalidVEventPropertyFound. If the iCalendar input has no UID this value will be used.
	 * @param recurId If non-NULL, this value MUST match the RECURRENCE-ID value in the iCalendar input if present, or else this returns ERR_InvalidVEventPropertyFound. If the iCalendar input has no RECURRENCE-ID this value will be used.
	 * @param comments If non-NULL, this text will be sent as comments on any notices sent to meeting participants as a result of this call. that will be included on the notices. Can be NULL.
	 * @param flags {@link CalendarWrite} flags to control non-default behavior. Supported: CAL_WRITE_MODIFY_LITERAL, {@link CalendarWrite#DISABLE_IMPLICIT_SCHEDULING}, {@link CalendarWrite#IGNORE_VERIFY_DB}.
	 */
	public static void updateCalendarEntry(NotesDatabase dbMail, String iCal, String uid, String recurId, String comments,
			EnumSet<CalendarWrite> flags) {
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");

		Memory icalMem = NotesStringUtils.toLMBCS(iCal, true);
		Memory uidMem = NotesStringUtils.toLMBCS(uid, true);
		Memory recurIdMem = NotesStringUtils.toLMBCS(recurId, true);
		Memory commentsMem = NotesStringUtils.toLMBCS(comments, true);
		
		int dwFlags = flags==null ? 0 : CalendarWrite.toBitMask(flags);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().CalUpdateEntry(dbMail.getHandle64(), icalMem, uidMem, recurIdMem, commentsMem, dwFlags, null);
		}
		else {
			result = NotesNativeAPI32.get().CalUpdateEntry(dbMail.getHandle32(), icalMem, uidMem, recurIdMem, commentsMem, dwFlags, null);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This is a convinience method that returns a UID from a NOTEID.<br>
	 * NOTEID->UID is a many to one mapping since one or several notes may represent
	 * a calendar entry (especially if it repeats) and its related notices.
	 * <br>
	 * As such, the UID output will be the same for all notes that refer to the same calendar entry.<br>
	 * This method may incur a note open, so there could be performance impact.
	 * <br>
	 * The following errors will be thrown by this method:
	 * <ul>
	 * <li>NOERROR - on success</li>
	 * <li>{@link INotesErrorConstants#ERR_NULL_DBHANDLE} - The database handle is NULL</li>
	 * <li>ERR_INVALID_NOTE - Note is not valid or is not a calendar note</li>
	 * <li>ERR_MISC_INVALID_ARGS - Unexpected arguments provided</li>
	 * <li>ERR_VALUE_LENGTH - The value is too long for the allocated buffer</li>
	 * </ul>
	 * 
	 * @param note note
	 * @return UID
	 */
	public static String getUIDfromNote(NotesNote note) {
		return getUIDfromNoteID(note.getParent(), note.getNoteId());
	}
	
	/**
	 * This is a convinience method that returns a UID from a NOTEID.<br>
	 * NOTEID->UID is a many to one mapping since one or several notes may represent
	 * a calendar entry (especially if it repeats) and its related notices.
	 * <br>
	 * As such, the UID output will be the same for all notes that refer to the same calendar entry.<br>
	 * This method may incur a note open, so there could be performance impact.
	 * <br>
	 * The following errors will be thrown by this method:
	 * <ul>
	 * <li>NOERROR - on success</li>
	 * <li>{@link INotesErrorConstants#ERR_NULL_DBHANDLE} - The database handle is NULL</li>
	 * <li>ERR_INVALID_NOTE - Note is not valid or is not a calendar note</li>
	 * <li>ERR_MISC_INVALID_ARGS - Unexpected arguments provided</li>
	 * <li>ERR_VALUE_LENGTH - The value is too long for the allocated buffer</li>
	 * </ul>
	 * 
	 * @param dbMail The database containing the note referenced by noteid.
	 * @param noteId note id
	 * @return UID
	 */
	public static String getUIDfromNoteID(NotesDatabase dbMail, int noteId) {
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");
		
		DisposableMemory retUID = new DisposableMemory(NotesConstants.MAXPATH);
		try {
			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().CalGetUIDfromNOTEID(dbMail.getHandle64(), noteId, retUID, (short) (NotesConstants.MAXPATH & 0xffff),
						null, 0, null);
			}
			else {
				result = NotesNativeAPI32.get().CalGetUIDfromNOTEID(dbMail.getHandle32(), noteId, retUID, (short) (NotesConstants.MAXPATH & 0xffff),
						null, 0, null);
			}
			NotesErrorUtils.checkResult(result);
			
			String uid = NotesStringUtils.fromLMBCS(retUID, 1);
			return uid;
		}
		finally {
			retUID.dispose();
		}
	}
	
	/**
	 * This is a convinience method that returns a UID from a UNID.<br>
	 * <br>
	 * UNID->UID is a many to one mapping since one or several notes may represent a
	 * calendar entry (especially if it repeats) and its related notices.<br>
	 * As such, the UID output will be the same for all notes that refer to the same
	 * calendar entry.<br>
	 * This method may incur a note open, so there could be performance impact.
	 * <br>
	 * The following errors will be thrown by this method:
	 * <ul>
	 * <li>NOERROR - on success</li>
	 * <li>{@link INotesErrorConstants#ERR_NULL_DBHANDLE} - The database handle is NULL</li>
	 * <li>ERR_INVALID_NOTE - Note is not valid or is not a calendar note</li>
	 * <li>ERR_MISC_INVALID_ARGS - Unexpected arguments provided</li>
	 * <li>ERR_VALUE_LENGTH - The value is too long for the allocated buffer</li>
	 * </ul>
	 * 
	 * @param dbMail The database containing the note referenced by unid.
	 * @param unid UNID of a calendar note
	 * @return UID
	 */
	public static String getUIDFromUNID(NotesDatabase dbMail, String unid) {
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");
		
		NotesUniversalNoteId unidObj = new NotesUniversalNoteId(unid);
		NotesUniversalNoteIdStruct unidStruct = unidObj.getAdapter(NotesUniversalNoteIdStruct.class);
		
		DisposableMemory retUID = new DisposableMemory(NotesConstants.MAXPATH);
		try {
			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().CalGetUIDfromUNID(dbMail.getHandle64(), unidStruct, retUID,
						(short) (NotesConstants.MAXPATH & 0xffff), null, 0, null);
			}
			else {
				result = NotesNativeAPI32.get().CalGetUIDfromUNID(dbMail.getHandle32(), unidStruct, retUID,
						(short) (NotesConstants.MAXPATH & 0xffff), null, 0, null);
			}
			NotesErrorUtils.checkResult(result);
			
			String uid = NotesStringUtils.fromLMBCS(retUID, 1);
			return uid;
		}
		finally {
			retUID.dispose();
		}
	}
	
	/**
	 * This is a convenience method that returns an Apptunid value that corresponds to a UID.<br>
	 * 
	 * @param uid UID of the icalendar entry
	 * @return ApptUnid
	 */
	public static String getApptUnidFromUID(String uid) {
		Memory uidMem = NotesStringUtils.toLMBCS(uid, true);
		DisposableMemory retApptUnidMem = new DisposableMemory(NotesConstants.MAXPATH);
		try {
			short result = NotesNativeAPI.get().CalGetApptunidFromUID(uidMem, retApptUnidMem, 0, null);
			NotesErrorUtils.checkResult(result);
			
			String apptUnid = NotesStringUtils.fromLMBCS(retApptUnidMem, -1);
			return apptUnid;
		}
		finally {
			retApptUnidMem.dispose();
		}
	}
	
	/**
	 * This is a method to get a note handle for an entry on the calendar.<br>
	 * <br>
	 * The intent is that the note handle can be used to get information about an
	 * entry or instance or to write additional information to the entry or
	 * instance (beyond what is defined in iCalendar and/or available in this API).<br>
	 * It is the callers responsibility to close the note via {@link NotesNote#recycle()} or
	 * let it be auto-GC'ed.<br>
	 * When opening a recurring entry, a valid recurrence ID MUST also be provided.<br>
	 * <br>
	 * A note representing the single instance will be returned. This might cause notes to be created or modified as a side effect.
	 * <br>
	 * The following errors will be thrown by this method:
	 * <ul>
	 * <li>NOERROR - on success</li>
	 * <li>{@link INotesErrorConstants#ERR_NULL_DBHANDLE} - The database handle is NULL</li>
	 * <li>ERR_NO_CALENDAR_FOUND - Unable to find the entry because the required view does not exist in this database</li>
	 * <li>ERR_NOT_FOUND - There are no entries that match the specified UID or UID/recurid in the database</li>
	 * <li>ERR_MISC_INVALID_ARGS - Unexpected arguments provided</li>
	 * <li>ERR_TDI_CONV - The recurrence ID specified cannot be interpreted</li>
	 * <li>ERR_MISC_UNEXPECTED_ERROR - Unexpected internal error</li>
	 * </ul>
	 *  
	 * @param dbMail The database containing the entry to open.
	 * @param uid The UID of the entry to get a note handle for.
	 * @param recurId The RECURRENCE-ID of the instance to get a note handle for. Timezones not permitted (time values must be in UTC time). NULL for single entries.  Must be present for recurring entries.
	 * @param flags {@link CalendarNoteOpen} flags to control non-default behavior. Supported: {@link CalendarNoteOpen#HANDLE_NOSPLIT}.
	 * @return note
	 */
	public static NotesNote openCalendarEntryNote(NotesDatabase dbMail, String uid, String recurId,
			EnumSet<CalendarNoteOpen> flags) {
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");
		
		Memory uidMem = NotesStringUtils.toLMBCS(uid, true);
		Memory recurIdMem = NotesStringUtils.toLMBCS(recurId, true);
		
		int dwFlags = flags==null ? 0 : CalendarNoteOpen.toBitMask(flags);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			result = NotesNativeAPI64.get().CalOpenNoteHandle(dbMail.getHandle64(), uidMem, recurIdMem, rethNote, dwFlags, null);
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			if (hNote==0)
				return null;
			
			CalNoteOpenData64 calNoteOpen = new CalNoteOpenData64(dbMail, hNote);
			NotesNote note = new NotesNote(calNoteOpen);
			NotesGC.__objectCreated(NotesNote.class, note);
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			result = NotesNativeAPI32.get().CalOpenNoteHandle(dbMail.getHandle32(), uidMem, recurIdMem, rethNote, dwFlags, null);
			NotesErrorUtils.checkResult(result);
			
			int hNote = rethNote.getValue();
			if (hNote==0)
				return null;
			
			CalNoteOpenData32 calNoteOpen = new CalNoteOpenData32(dbMail, hNote);
			NotesNote note = new NotesNote(calNoteOpen);
			NotesGC.__objectCreated(NotesNote.class, note);
			return note;
		}
	}
	
	/**
	 * This will return complete iCalendar data for the specified entry.<br>
	 * <br>
	 * For recurring entries, this may result in multiple VEVENTs in the returned
	 * iCalendar data.<br>
	 * In this case, the first VEVENT represents the recurrence set and additional
	 * entries represent exceptions to the recurrence set.<br>
	 * <br>
	 * All instances that differ from the recurrence set will be returned as additional
	 * VEVENTs containing the exceptional data. There is no concept of 'runs' of
	 * instances or RANGE of instances.<br>
	 * Alternatively, a specific instance may be requested using <code>recurId</code>
	 * and only the data for that instance will be returned.<br>
	 * Returned data will not include rich text description.<br>
	 * All participants of a meeting will be returned as PARTSTAT=NEEDS-ACTION even if they have responded.
	 * 
	 * @param dbMail The database from which entries are returned.
	 * @param uid The UID of the entry to be returned.
	 * @param recurId NULL for single entries or to read data for an entire recurring series. If populated, this is the RECURRENCE-ID of the specific instance to read.
	 * @param flags {@link CalendarRead} flags to control non-default behavior
	 * @return iCalendar data
	 */
	public static String readCalendarEntry(NotesDatabase dbMail, String uid, String recurId, EnumSet<CalendarRead> flags) {
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");
		
		Memory uidMem = NotesStringUtils.toLMBCS(uid, true);
		Memory recurIdMem = NotesStringUtils.toLMBCS(recurId, true);
		
		int dwFlags = flags==null ? 0 : CalendarRead.toBitMask(flags);
		
		String retIcal = null;
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference hRetCalData = new LongByReference();
			
			result = NotesNativeAPI64.get().CalReadEntry(dbMail.getHandle64(), uidMem, recurIdMem, hRetCalData, null, dwFlags, null);
			NotesErrorUtils.checkResult(result);
			
			long hRetCalDataLong = hRetCalData.getValue();
			if (hRetCalDataLong!=0) {
				Pointer retUIDPtr = Mem64.OSMemoryLock(hRetCalDataLong);
				try {
					retIcal = NotesStringUtils.fromLMBCS(retUIDPtr, -1);
				}
				finally {
					Mem64.OSMemoryUnlock(hRetCalDataLong);
					Mem64.OSMemoryFree(hRetCalDataLong);
				}
			}
		}
		else {
			IntByReference hRetCalData = new IntByReference();
			
			result = NotesNativeAPI32.get().CalReadEntry(dbMail.getHandle32(), uidMem, recurIdMem, hRetCalData, null, dwFlags, null);
			NotesErrorUtils.checkResult(result);
			
			int hRetCalDataInt = hRetCalData.getValue();
			if (hRetCalDataInt!=0) {
				Pointer retUIDPtr = Mem32.OSMemoryLock(hRetCalDataInt);
				try {
					retIcal = NotesStringUtils.fromLMBCS(retUIDPtr, -1);
				}
				finally {
					Mem32.OSMemoryUnlock(hRetCalDataInt);
					Mem32.OSMemoryFree(hRetCalDataInt);
				}
			}
		}
		
		return retIcal;
	}
	
	/**
	 * Gets a summary of calendar entries for a range of times
	 * 
	 * @param dbMail The database from which entries are returned.
	 * @param start the start time of the range
	 * @param end the end time of the range. An exception occurs if the end time is not greater than the start time
	 * @param retICal if not null, we return a summary in iCalendar format of the entries from the start date to the end date, inclusive. An exception occurs if the range contains no entries.
	 * @param retUIDs if not null, we return a list of UIDs found within the range
	 * @throws IOException if writing iCalendar data fails
	 */
	public static void readRange(NotesDatabase dbMail, NotesTimeDate start, NotesTimeDate end,
			Appendable retICal, List<String> retUIDs) throws IOException {
		readRange(dbMail, start, end, 0, Integer.MAX_VALUE, null, retICal, retUIDs);
	}
	
	/**
	 * Gets a summary of calendar entries for a range of times
	 * 
	 * @param dbMail The database from which entries are returned.
	 * @param start the start time of the range
	 * @param end the end time of the range. An exception occurs if the end time is not greater than the start time
	 * @param skipCount the number of entries to skip from the beginning of the range. This parameter can be used in conjunction with <i>entriesprocessed</i> to read the entries in a series of calls
	 * @param maxRead the maximum number of entries to read
	 * @param retICal if not null, we return a summary in iCalendar format of the entries from the start date to the end date, inclusive. An exception occurs if the range contains no entries.
	 * @param retUIDs if not null, we return a list of UIDs found within the range
	 * @throws IOException if writing iCalendar data fails
	 */
	public static void readRange(NotesDatabase dbMail, NotesTimeDate start, NotesTimeDate end, int skipCount, int maxRead,
			Appendable retICal, List<String> retUIDs) throws IOException {
		readRange(dbMail, start, end, skipCount, maxRead, null, retICal, retUIDs);
	}
	
	/**
	 * Gets a summary of calendar entries for a range of times
	 * 
	 * @param dbMail The database from which entries are returned.
	 * @param start the start time of the range
	 * @param end the end time of the range. An exception occurs if the end time is not greater than the start time
	 * @param skipCount the number of entries to skip from the beginning of the range. This parameter can be used in conjunction with <i>entriesprocessed</i> to read the entries in a series of calls
	 * @param maxRead the maximum number of entries to read
	 * @param readMask flags that control what properties about the calendar entries will be returned
	 * @param retICal if not null, we return a summary in iCalendar format of the entries from the start date to the end date, inclusive. An exception occurs if the range contains no entries.
	 * @param retUIDs if not null, we return a list of UIDs found within the range
	 * @throws IOException if writing iCalendar data fails
	 */
	public static void readRange(NotesDatabase dbMail, NotesTimeDate start, NotesTimeDate end, int skipCount, int maxRead,
			EnumSet<CalendarReadRange> readMask, Appendable retICal, List<String> retUIDs) throws IOException {
		
		if (dbMail.isRecycled())
			throw new NotesError(0, "Target database already recycled");
		
		NotesTimeDateStruct.ByValue startStruct = start==null ? null : NotesTimeDateStruct.ByValue.newInstance(start.getInnards());
		NotesTimeDateStruct.ByValue endStruct = end==null ? null : NotesTimeDateStruct.ByValue.newInstance(end.getInnards());

		int dwReturnMask = CalendarReadRange.toBitMask(readMask);
		int dwReturnMaskExt = CalendarReadRange.toBitMask2(readMask);

		//variables to collect the whole lookup result
		StringBuilder sbIcalAllData = retICal==null ? null : new StringBuilder();
		List<String> uidAllData = retUIDs==null ? null : new ArrayList<String>();
		
		while (true) {
			int currSkipCount = skipCount;
			int remainingToRead = maxRead;
			short result;
			boolean hasMoreToDo;
			boolean hasConflict;
			
			//clear current lookup result, while(true) loop may be run multiple times
			//if lookup view changes
			if (sbIcalAllData!=null) {
				sbIcalAllData.setLength(0);
			}
			if (uidAllData!=null) {
				uidAllData.clear();
			}
			
			do {
				if (PlatformUtils.is64Bit()) {
					LongByReference hRetCalData = retICal==null ? null : new LongByReference();
					LongByReference hRetUIDData = retUIDs==null ? null : new LongByReference();
					ShortByReference retCalBufferLength = new ShortByReference();
					ShortByReference retSignalFlags = new ShortByReference();
					IntByReference retNumEntriesProcessed = new IntByReference();
					
					result = NotesNativeAPI64.get().CalReadRange(dbMail.getHandle64(), startStruct, endStruct, currSkipCount,
							remainingToRead, dwReturnMask, dwReturnMaskExt, null, hRetCalData,
							retCalBufferLength, hRetUIDData, retNumEntriesProcessed, retSignalFlags, 0, null);
					NotesErrorUtils.checkResult(result);
					
					int numEntriesProcessed = retNumEntriesProcessed.getValue();
					currSkipCount += numEntriesProcessed;
					remainingToRead -= numEntriesProcessed;
					
					if (hRetCalData!=null && retICal!=null && sbIcalAllData!=null) {
						//decode iCalendar
						int iCalBufLength = (int) (retCalBufferLength.getValue() & 0xffff);
						if (iCalBufLength>0) {
							long hRetCalDataLong = hRetCalData.getValue();
							if (hRetCalDataLong!=0) {
								Pointer retUIDPtr = Mem64.OSMemoryLock(hRetCalDataLong);
								try {
									String currICal = NotesStringUtils.fromLMBCS(retUIDPtr, iCalBufLength);
									sbIcalAllData.append(currICal);
								}
								finally {
									Mem64.OSMemoryUnlock(hRetCalDataLong);
									Mem64.OSMemoryFree(hRetCalDataLong);
								}
							}
						}
					}
					
					if (hRetUIDData!=null && retUIDs!=null && uidAllData!=null) {
						//decode UID list
						long hRetUIDDataLong = hRetUIDData.getValue();
						if (hRetUIDDataLong!=0) {
							Pointer pUIDData = Mem64.OSMemoryLock(hRetUIDDataLong);
							ShortByReference retTextLength = new ShortByReference();
							Memory retTextPointer = new Memory(Pointer.SIZE);
							try {
								int numEntriesAsInt = (int) (NotesNativeAPI.get().ListGetNumEntries(pUIDData, 0) & 0xffff);
								for (int i=0; i<numEntriesAsInt; i++) {
									result = NotesNativeAPI.get().ListGetText(pUIDData, false, (short) (i & 0xffff), retTextPointer, retTextLength);
									NotesErrorUtils.checkResult(result);
									
									String currUID = NotesStringUtils.fromLMBCS(retTextPointer.getPointer(0), retTextLength.getValue() & 0xffff);
									uidAllData.add(currUID);
								}
							}
							finally {
								Mem64.OSMemoryUnlock(hRetUIDDataLong);
								Mem64.OSMemoryFree(hRetUIDDataLong);
							}
						}
					}
					
					short signalFlags = retSignalFlags.getValue();
					hasMoreToDo = (signalFlags & NotesConstants.SIGNAL_MORE_TO_DO) == NotesConstants.SIGNAL_MORE_TO_DO;
					hasConflict = (signalFlags & NotesConstants.SIGNAL_ANY_CONFLICT) == NotesConstants.SIGNAL_ANY_CONFLICT;
				}
				else {
					IntByReference hRetCalData = retICal==null ? null : new IntByReference();
					IntByReference hRetUIDData = retUIDs==null ? null : new IntByReference();
					ShortByReference retCalBufferLength = new ShortByReference();
					ShortByReference retSignalFlags = new ShortByReference();
					IntByReference retNumEntriesProcessed = new IntByReference();
					
					result = NotesNativeAPI32.get().CalReadRange(dbMail.getHandle32(), startStruct, endStruct, currSkipCount,
							remainingToRead, dwReturnMask, dwReturnMaskExt, null, hRetCalData,
							retCalBufferLength, hRetUIDData, retNumEntriesProcessed, retSignalFlags, 0, null);
					NotesErrorUtils.checkResult(result);

					int numEntriesProcessed = retNumEntriesProcessed.getValue();
					currSkipCount += numEntriesProcessed;
					remainingToRead -= numEntriesProcessed;
					
					if (hRetCalData!=null && retICal!=null && sbIcalAllData!=null) {
						//decode iCalendar
						int iCalBufLength = (int) (retCalBufferLength.getValue() & 0xffff);
						if (iCalBufLength>0) {
							int hRetCalDataInt = hRetCalData.getValue();
							if (hRetCalDataInt!=0) {
								Pointer retUIDPtr = Mem32.OSMemoryLock(hRetCalDataInt);
								try {
									String currICal = NotesStringUtils.fromLMBCS(retUIDPtr, iCalBufLength);
									sbIcalAllData.append(currICal);
								}
								finally {
									Mem32.OSMemoryUnlock(hRetCalDataInt);
									Mem32.OSMemoryFree(hRetCalDataInt);
								}
							}
						}
					}

					if (hRetUIDData!=null && retUIDs!=null && uidAllData!=null) {
						//decode UID list
						int hRetUIDDataInt = hRetUIDData.getValue();
						if (hRetUIDDataInt!=0) {
							Pointer pUIDData = Mem32.OSMemoryLock(hRetUIDDataInt);
							ShortByReference retTextLength = new ShortByReference();
							Memory retTextPointer = new Memory(Pointer.SIZE);
							try {
								int numEntriesAsInt = (int) (NotesNativeAPI.get().ListGetNumEntries(pUIDData, 0) & 0xffff);
								for (int i=0; i<numEntriesAsInt; i++) {
									result = NotesNativeAPI.get().ListGetText(pUIDData, false, (short) (i & 0xffff), retTextPointer, retTextLength);
									NotesErrorUtils.checkResult(result);
									
									String currUID = NotesStringUtils.fromLMBCS(retTextPointer.getPointer(0), retTextLength.getValue() & 0xffff);
									uidAllData.add(currUID);
								}
							}
							finally {
								Mem32.OSMemoryUnlock(hRetUIDDataInt);
								Mem32.OSMemoryFree(hRetUIDDataInt);
							}
						}
					}
					
					short signalFlags = retSignalFlags.getValue();
					hasMoreToDo = (signalFlags & NotesConstants.SIGNAL_MORE_TO_DO) == NotesConstants.SIGNAL_MORE_TO_DO;
					hasConflict = (signalFlags & NotesConstants.SIGNAL_ANY_CONFLICT) == NotesConstants.SIGNAL_ANY_CONFLICT;
				}
			}
			while (hasMoreToDo && remainingToRead>0);
			
			if (!hasConflict) {
				//no read conflict in view, we are done
				break;
			}
			else {
				//retry the whole lookup
				continue;
			}
		}
		
		//return what we have read
		if (retICal!=null && sbIcalAllData!=null) {
			retICal.append(sbIcalAllData.toString());
		}
		if (retUIDs!=null && uidAllData!=null) {
			retUIDs.addAll(uidAllData);
		}
	}
}
