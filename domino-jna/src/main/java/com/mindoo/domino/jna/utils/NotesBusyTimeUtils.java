package com.mindoo.domino.jna.utils;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.mindoo.domino.jna.NotesDateRange;
import com.mindoo.domino.jna.NotesScheduleContainer;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.ScheduleOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to access the busytime information collected by the Domino server
 * 
 * @author Karsten Lehmann
 */
public class NotesBusyTimeUtils {

	/**
	 * This routine searches the schedule database (locally or on a specified server) for free time periods
	 * common to a specified list of people.
	 * 
	 * @param apptUnid This is the UNID of an appointment to ignore for the purposes of calculating free time. This is useful when you need to move an appointment to a time which overlaps it. Can be null
	 * @param apptOrigDate This is the date of the original date of the appointment to ignore for free time calculations. Note that the only reason that this is here is for compatibility with Organizer 2.x gateway.
	 * @param findFirstFit If this value is equal to TRUE then this routine will return just the first free time interval that fits the duration. The size of this interval will equal to duration.
	 * @param from specifies the start of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param until specifies the end of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param duration How much free time you are looking for, in minutes (max 65535).
	 * @param names list of distinguished names whose schedule should be searched, either in abbreviated or canonical format
	 * @return timedate pairs indicating runs of free time
	 */
	public static List<Calendar[]> freeTimeSearch(String apptUnid, NotesTimeDate apptOrigDate,
			boolean findFirstFit, NotesTimeDate from, NotesTimeDate until, int duration, List<String> names) {
		
		return freeTimeSearchAsTimeDate(apptUnid, apptOrigDate, findFirstFit, from, until, duration, names)
				.stream()
				.map((entry) -> {
					return new Calendar[] {entry.getStartDateTime().toCalendar(), entry.getEndDateTime().toCalendar()};
				})
				.collect(Collectors.toList());
	}
	
	/**
	 * This routine searches the schedule database (locally or on a specified server) for free time periods
	 * common to a specified list of people.
	 * 
	 * @param apptUnid This is the UNID of an appointment to ignore for the purposes of calculating free time. This is useful when you need to move an appointment to a time which overlaps it. Can be null
	 * @param apptOrigDate This is the date of the original date of the appointment to ignore for free time calculations. Note that the only reason that this is here is for compatibility with Organizer 2.x gateway.
	 * @param findFirstFit If this value is equal to TRUE then this routine will return just the first free time interval that fits the duration. The size of this interval will equal to duration.
	 * @param from specifies the start of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param until specifies the end of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param duration How much free time you are looking for, in minutes (max 65535).
	 * @param names list of distinguished names whose schedule should be searched, either in abbreviated or canonical format
	 * @return timedate pairs indicating runs of free time
	 */
	public static List<NotesDateRange> freeTimeSearchAsTimeDate(String apptUnid, NotesTimeDate apptOrigDate,
			boolean findFirstFit, NotesTimeDate from, NotesTimeDate until, int duration, List<String> names) {
		
		NotesUniversalNoteIdStruct unidStruct = apptUnid==null ? null : NotesUniversalNoteIdStruct.fromString(apptUnid);
		NotesTimeDateStruct apptOrigDateStruct = apptOrigDate==null ? null : NotesTimeDateStruct.newInstance(apptOrigDate.getInnards());
		
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = NotesTimeDateStruct.newInstance(from.getInnards());
		intervalPair.Upper = NotesTimeDateStruct.newInstance(until.getInnards());
		intervalPair.write();
		
		if (duration > 65535) {
			throw new IllegalArgumentException("Duration can only have a short value ("+duration+">65535)");
		}

		List<String> namesCanonical = names
				.stream()
				.filter(StringUtil::isNotEmpty)
				.map(NotesNamingUtils::toCanonicalName)
				.collect(Collectors.toList());

		if (namesCanonical.isEmpty()) {
			throw new IllegalArgumentException("No usernames specified to retrieve schedules.");
		}

		List<Object> decodedTimeListAsObj = null;
		
		DHANDLE.ByReference rethList = DHANDLE.newInstanceByReference();
		ShortByReference retListSize = new ShortByReference();

		short result = NotesNativeAPI.get().ListAllocate((short) 0, 
				(short) 0,
				0, rethList, null, retListSize);
		
		NotesErrorUtils.checkResult(result);

		Mem.OSUnlockObject(rethList);
		
		for (int i=0; i<namesCanonical.size(); i++) {
			String currName = namesCanonical.get(i);
			Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);
			if (currNameMem!=null && currNameMem.size() > 65535) {
				throw new NotesError(MessageFormat.format("List item at position {0} exceeds max lengths of 65535 bytes", i));
			}

			char textSize = currNameMem==null ? 0 : (char) currNameMem.size();

			result = NotesNativeAPI.get().ListAddEntry(rethList.getByValue(), 0, retListSize, (char) i, currNameMem,
					textSize);
			NotesErrorUtils.checkResult(result);
		}
		
		Pointer valuePtr = Mem.OSLockObject(rethList);
		try {
			DHANDLE.ByReference rethRange = DHANDLE.newInstanceByReference();
			
			result = NotesNativeAPI.get().SchFreeTimeSearch(unidStruct, apptOrigDateStruct, (short) (findFirstFit ? 1 : 0), 0, intervalPair,
					(short) (duration & 0xffff), valuePtr, rethRange);
			NotesErrorUtils.checkResult(result);

			if (!rethRange.isNull()) {
				Pointer rangePtr = Mem.OSLockObject(rethRange.getByValue());
				try {
					decodedTimeListAsObj = ItemDecoder.decodeTimeDateListAsNotesTimeDate(rangePtr);
				}
				finally {
					Mem.OSUnlockObject(rethRange);
					result = Mem.OSMemFree(rethRange.getByValue());
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		finally {
			Mem.OSUnlockObject(rethList);
			result = Mem.OSMemFree(rethList.getByValue());
			NotesErrorUtils.checkResult(result);
		}
		
		if (decodedTimeListAsObj==null)
			return Collections.emptyList();

		return decodedTimeListAsObj
				.stream()
				.filter(NotesDateRange.class::isInstance)
				.map(NotesDateRange.class::cast)
				.collect(Collectors.toList());
	}
	
	/**
	 * Synchronously retrieves a local or remote schedule by asking the caller's home server for the schedule.<br>
	 * <br>
	 * The ONLY time that local busy time is used is when the client is in the Disconnected mode
	 * which is specified through the location document.<br>
	 * <br>
	 * Otherwise, the API will route ALL lookup requests to the users home server for processing.
	 * 
	 * @param apptUnid  Ignore this UNID in computations
	 * @param options option flags
	 * @param from specifies the start of the range over which the schedule lookup should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param until specifies the end of the range over which the schedule lookup should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param names list of distinguished names whose schedule should be searched, either in abbreviated or canonical format
	 * @return schedule container
	 */
	public static NotesScheduleContainer retrieveSchedules(String apptUnid, EnumSet<ScheduleOptions> options,
			NotesTimeDate from, NotesTimeDate until, List<String> names) {

		NotesUniversalNoteIdStruct unidStruct = apptUnid==null ? null : NotesUniversalNoteIdStruct.fromString(apptUnid);
		
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = NotesTimeDateStruct.newInstance(from.getInnards());
		intervalPair.Upper = NotesTimeDateStruct.newInstance(until.getInnards());
		intervalPair.write();

		List<String> namesCanonical = names
				.stream()
				.filter(StringUtil::isNotEmpty)
				.map(NotesNamingUtils::toCanonicalName)
				.collect(Collectors.toList());

		if (namesCanonical.isEmpty()) {
			throw new IllegalArgumentException("No usernames specified to retrieve schedules.");
		}
		
		//make sure we always get the extended schedule container
		final int SCHRQST_EXTFORMAT = 0x0020;
		
		short result;

		int optionsAsInt = ScheduleOptions.toBitMaskInt(options) | SCHRQST_EXTFORMAT;

		DHANDLE.ByReference rethList = DHANDLE.newInstanceByReference();
		ShortByReference retListSize = new ShortByReference();

		result = NotesNativeAPI.get().ListAllocate((short) 0, 
				(short) 0,
				0, rethList, null, retListSize);
		
		NotesErrorUtils.checkResult(result);

		Mem.OSUnlockObject(rethList);
		
		for (int i=0; i<namesCanonical.size(); i++) {
			String currName = namesCanonical.get(i);
			Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);
			if (currNameMem!=null && currNameMem.size() > 65535) {
				throw new NotesError(MessageFormat.format("List item at position {0} exceeds max lengths of 65535 bytes", i));
			}

			char textSize = currNameMem==null ? 0 : (char) currNameMem.size();

			result = NotesNativeAPI.get().ListAddEntry(rethList.getByValue(), 0, retListSize, (char) i, currNameMem,
					textSize);
			NotesErrorUtils.checkResult(result);
		}
		
		Pointer valuePtr = Mem.OSLockObject(rethList);
		DHANDLE.ByReference rethCntnr = DHANDLE.newInstanceByReference();
		try {
			result = NotesNativeAPI.get().SchRetrieve(unidStruct, null, optionsAsInt, intervalPair, valuePtr, rethCntnr,
					null, null,null);
			NotesErrorUtils.checkResult(result);
			
			NotesScheduleContainer scheduleContainer = new NotesScheduleContainer(rethCntnr);
			NotesGC.__objectCreated(NotesScheduleContainer.class, scheduleContainer);
			return scheduleContainer;
		}
		finally {
			Mem.OSUnlockObject(rethList);
			result = Mem.OSMemFree(rethList.getByValue());
			NotesErrorUtils.checkResult(result);
		}
	
	}
}
