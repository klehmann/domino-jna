package com.mindoo.domino.jna.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.NotesScheduleContainer;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.ScheduleOptions;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
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
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		NotesUniversalNoteIdStruct unidStruct = apptUnid==null ? null : NotesUniversalNoteIdStruct.fromString(apptUnid);
		NotesTimeDateStruct apptOrigDateStruct = apptOrigDate==null ? null : apptOrigDate.getAdapter(NotesTimeDateStruct.class);
		
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDateStruct fromStruct = from.getAdapter(NotesTimeDateStruct.class);
		NotesTimeDateStruct untilStruct = until.getAdapter(NotesTimeDateStruct.class);
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = fromStruct;
		intervalPair.Upper = untilStruct;
		intervalPair.write();
		
		if (duration > 65535) {
			throw new IllegalArgumentException("Duration can only have a short value ("+duration+">65535)");
		}
		
		List<String> namesCanonical = new ArrayList<String>();
		for (String currName : names) {
			namesCanonical.add(NotesNamingUtils.toCanonicalName(currName));
		}
		
		short result;
		
		List<Object> decodedTimeListAsObj = null;
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethList = new LongByReference();
			ShortByReference retListSize = new ShortByReference();

			result = notesAPI.b64_ListAllocate((short) 0, 
					(short) 0,
					0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			long hList = rethList.getValue();
			notesAPI.b64_OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = notesAPI.b64_ListAddEntry(hList, 0, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = notesAPI.b64_OSLockObject(hList);
			LongByReference rethRange = new LongByReference();
			try {
				result = notesAPI.b64_SchFreeTimeSearch(unidStruct, apptOrigDateStruct, (short) (findFirstFit ? 1 : 0), 0, intervalPair,
						(short) (duration & 0xffff), valuePtr, rethRange);

				NotesErrorUtils.checkResult(result);
				
				long hRange = rethRange.getValue();
				if (hRange!=0) {
					Pointer rangePtr = notesAPI.b64_OSLockObject(hRange);
					try {
						boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
						int gmtOffset = NotesDateTimeUtils.getGMTOffset();
						decodedTimeListAsObj = ItemDecoder.decodeTimeDateList(notesAPI, rangePtr, useDayLight, gmtOffset);
					}
					finally {
						notesAPI.b64_OSUnlockObject(rethRange.getValue());
						notesAPI.b64_OSMemFree(rethRange.getValue());
					}
				}
			}
			finally {
				notesAPI.b64_OSUnlockObject(hList);
				notesAPI.b64_OSMemFree(hList);
			}
		}
		else {
			IntByReference rethList = new IntByReference();
			ShortByReference retListSize = new ShortByReference();

			result = notesAPI.b32_ListAllocate((short) 0, 
					(short) 0,
					1, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			int hList = rethList.getValue();
			notesAPI.b32_OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = notesAPI.b32_ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = notesAPI.b32_OSLockObject(hList);
			IntByReference rethRange = new IntByReference();
			try {
				result = notesAPI.b32_SchFreeTimeSearch(unidStruct, apptOrigDateStruct, (short) (findFirstFit ? 1 : 0), 0, intervalPair,
						(short) (duration & 0xffff), valuePtr, rethRange);
				NotesErrorUtils.checkResult(result);
				
				int hRange = rethRange.getValue();
				if (hRange!=0) {
					Pointer rangePtr = notesAPI.b32_OSLockObject(hRange);
					try {
						boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
						int gmtOffset = NotesDateTimeUtils.getGMTOffset();
						decodedTimeListAsObj = ItemDecoder.decodeTimeDateList(notesAPI, rangePtr, useDayLight, gmtOffset);
					}
					finally {
						notesAPI.b32_OSUnlockObject(rethRange.getValue());
						notesAPI.b32_OSMemFree(rethRange.getValue());
					}
				}
			}
			finally {
				notesAPI.b32_OSUnlockObject(hList);
				notesAPI.b32_OSMemFree(hList);
			}
		}
		
		if (decodedTimeListAsObj==null)
			return Collections.emptyList();
		
		List<Calendar[]> decodedTimeListWithRanges = new ArrayList<Calendar[]>(decodedTimeListAsObj.size());
		for (Object currObj : decodedTimeListAsObj) {
			if (currObj instanceof Calendar[]) {
				decodedTimeListWithRanges.add((Calendar[]) currObj);
			}
		}
		return decodedTimeListWithRanges;
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
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesUniversalNoteIdStruct unidStruct = apptUnid==null ? null : NotesUniversalNoteIdStruct.fromString(apptUnid);
		
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDateStruct fromStruct = from.getAdapter(NotesTimeDateStruct.class);
		NotesTimeDateStruct untilStruct = until.getAdapter(NotesTimeDateStruct.class);
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = fromStruct;
		intervalPair.Upper = untilStruct;
		intervalPair.write();

		List<String> namesCanonical = new ArrayList<String>();
		for (String currName : names) {
			namesCanonical.add(NotesNamingUtils.toCanonicalName(currName));
		}
		
		//make sure we always get the extended schedule container
		final int SCHRQST_EXTFORMAT = 0x0020;
		
		short result;

		int optionsAsInt = ScheduleOptions.toBitMaskInt(options) | SCHRQST_EXTFORMAT;

		if (NotesJNAContext.is64Bit()) {
			LongByReference rethList = new LongByReference();
			ShortByReference retListSize = new ShortByReference();

			result = notesAPI.b64_ListAllocate((short) 0, 
					(short) 0,
					0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			long hList = rethList.getValue();
			notesAPI.b64_OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = notesAPI.b64_ListAddEntry(hList, 0, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = notesAPI.b64_OSLockObject(hList);
			LongByReference rethCntnr = new LongByReference();
			try {
				result = notesAPI.b64_SchRetrieve(unidStruct, null, optionsAsInt, intervalPair, valuePtr, rethCntnr,
						null, null,null);
				NotesErrorUtils.checkResult(result);
				
				long hCntnr = rethCntnr.getValue();
				
				NotesScheduleContainer scheduleContainer = new NotesScheduleContainer(hCntnr);
				NotesGC.__objectCreated(NotesScheduleContainer.class, scheduleContainer);
				return scheduleContainer;
			}
			finally {
				notesAPI.b64_OSUnlockObject(hList);
				notesAPI.b64_OSMemFree(hList);
			}
		}
		else {
			IntByReference rethList = new IntByReference();
			ShortByReference retListSize = new ShortByReference();

			result = notesAPI.b32_ListAllocate((short) 0, 
					(short) 0,
					1, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			int hList = rethList.getValue();
			notesAPI.b32_OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = notesAPI.b32_ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = notesAPI.b32_OSLockObject(hList);
			IntByReference rethCntnr = new IntByReference();
			try {
				result = notesAPI.b32_SchRetrieve(unidStruct, null, optionsAsInt, intervalPair, valuePtr, rethCntnr, null, null,null);
				NotesErrorUtils.checkResult(result);
				
				long hCntnr = rethCntnr.getValue();
				
				NotesScheduleContainer scheduleContainer = new NotesScheduleContainer(hCntnr);
				NotesGC.__objectCreated(NotesScheduleContainer.class, scheduleContainer);
				return scheduleContainer;
			}
			finally {
				notesAPI.b32_OSUnlockObject(hList);
				notesAPI.b32_OSMemFree(hList);
			}
		}
	}
}
