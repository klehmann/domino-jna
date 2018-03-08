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
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
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
		
		List<String> namesCanonical = new ArrayList<String>();
		for (String currName : names) {
			namesCanonical.add(NotesNamingUtils.toCanonicalName(currName));
		}
		
		short result;
		
		List<Object> decodedTimeListAsObj = null;
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethList = new LongByReference();
			ShortByReference retListSize = new ShortByReference();

			result = NotesNativeAPI64.get().ListAllocate((short) 0, 
					(short) 0,
					0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			long hList = rethList.getValue();
			Mem64.OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = NotesNativeAPI64.get().ListAddEntry(hList, 0, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = Mem64.OSLockObject(hList);
			LongByReference rethRange = new LongByReference();
			try {
				result = NotesNativeAPI64.get().SchFreeTimeSearch(unidStruct, apptOrigDateStruct, (short) (findFirstFit ? 1 : 0), 0, intervalPair,
						(short) (duration & 0xffff), valuePtr, rethRange);

				NotesErrorUtils.checkResult(result);
				
				long hRange = rethRange.getValue();
				if (hRange!=0) {
					Pointer rangePtr = Mem64.OSLockObject(hRange);
					try {
						boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
						int gmtOffset = NotesDateTimeUtils.getGMTOffset();
						decodedTimeListAsObj = ItemDecoder.decodeTimeDateList(rangePtr, useDayLight, gmtOffset);
					}
					finally {
						Mem64.OSUnlockObject(rethRange.getValue());
						result = Mem64.OSMemFree(rethRange.getValue());
						NotesErrorUtils.checkResult(result);
					}
				}
			}
			finally {
				Mem64.OSUnlockObject(hList);
				result = Mem64.OSMemFree(hList);
				NotesErrorUtils.checkResult(result);
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
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = NotesNativeAPI32.get().ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = Mem32.OSLockObject(hList);
			IntByReference rethRange = new IntByReference();
			try {
				result = NotesNativeAPI32.get().SchFreeTimeSearch(unidStruct, apptOrigDateStruct, (short) (findFirstFit ? 1 : 0), 0, intervalPair,
						(short) (duration & 0xffff), valuePtr, rethRange);
				NotesErrorUtils.checkResult(result);
				
				int hRange = rethRange.getValue();
				if (hRange!=0) {
					Pointer rangePtr = Mem32.OSLockObject(hRange);
					try {
						boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
						int gmtOffset = NotesDateTimeUtils.getGMTOffset();
						decodedTimeListAsObj = ItemDecoder.decodeTimeDateList(rangePtr, useDayLight, gmtOffset);
					}
					finally {
						Mem32.OSUnlockObject(rethRange.getValue());
						result = Mem32.OSMemFree(rethRange.getValue());
						NotesErrorUtils.checkResult(result);
					}
				}
			}
			finally {
				Mem32.OSUnlockObject(hList);
				result = Mem32.OSMemFree(hList);
				NotesErrorUtils.checkResult(result);
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

		NotesUniversalNoteIdStruct unidStruct = apptUnid==null ? null : NotesUniversalNoteIdStruct.fromString(apptUnid);
		
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = NotesTimeDateStruct.newInstance(from.getInnards());
		intervalPair.Upper = NotesTimeDateStruct.newInstance(until.getInnards());
		intervalPair.write();

		List<String> namesCanonical = new ArrayList<String>();
		for (String currName : names) {
			namesCanonical.add(NotesNamingUtils.toCanonicalName(currName));
		}
		
		//make sure we always get the extended schedule container
		final int SCHRQST_EXTFORMAT = 0x0020;
		
		short result;

		int optionsAsInt = ScheduleOptions.toBitMaskInt(options) | SCHRQST_EXTFORMAT;

		if (PlatformUtils.is64Bit()) {
			LongByReference rethList = new LongByReference();
			ShortByReference retListSize = new ShortByReference();

			result = NotesNativeAPI64.get().ListAllocate((short) 0, 
					(short) 0,
					0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			long hList = rethList.getValue();
			Mem64.OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = NotesNativeAPI64.get().ListAddEntry(hList, 0, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = Mem64.OSLockObject(hList);
			LongByReference rethCntnr = new LongByReference();
			try {
				result = NotesNativeAPI64.get().SchRetrieve(unidStruct, null, optionsAsInt, intervalPair, valuePtr, rethCntnr,
						null, null,null);
				NotesErrorUtils.checkResult(result);
				
				long hCntnr = rethCntnr.getValue();
				
				NotesScheduleContainer scheduleContainer = new NotesScheduleContainer(hCntnr);
				NotesGC.__objectCreated(NotesScheduleContainer.class, scheduleContainer);
				return scheduleContainer;
			}
			finally {
				Mem64.OSUnlockObject(hList);
				result = Mem64.OSMemFree(hList);
				NotesErrorUtils.checkResult(result);
			}
		}
		else {
			IntByReference rethList = new IntByReference();
			ShortByReference retListSize = new ShortByReference();

			result = NotesNativeAPI32.get().ListAllocate((short) 0, 
					(short) 0,
					0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			int hList = rethList.getValue();
			Mem32.OSUnlockObject(hList);
			
			for (int i=0; i<namesCanonical.size(); i++) {
				String currName = namesCanonical.get(i);
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, false);

				result = NotesNativeAPI32.get().ListAddEntry(hList, 0, retListSize, (short) (i & 0xffff), currNameMem,
						(short) (currNameMem==null ? 0 : (currNameMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			int listSize = retListSize.getValue() & 0xffff;
			
			Pointer valuePtr = Mem32.OSLockObject(hList);
			IntByReference rethCntnr = new IntByReference();
			try {
				result = NotesNativeAPI32.get().SchRetrieve(unidStruct, null, optionsAsInt, intervalPair, valuePtr, rethCntnr, null, null,null);
				NotesErrorUtils.checkResult(result);
				
				int hCntnr = rethCntnr.getValue();
				
				NotesScheduleContainer scheduleContainer = new NotesScheduleContainer(hCntnr);
				NotesGC.__objectCreated(NotesScheduleContainer.class, scheduleContainer);
				return scheduleContainer;
			}
			finally {
				Mem32.OSUnlockObject(hList);
				result = Mem32.OSMemFree(hList);
				NotesErrorUtils.checkResult(result);
			}
		}
	}
}
