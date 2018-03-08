package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.structs.NotesNumberPairStruct;
import com.mindoo.domino.jna.internal.structs.NotesRangeStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.mindoo.domino.jna.utils.LMBCSString;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

public class ItemDecoder {

	public static double decodeNumber(Pointer ptr, int valueLength) {
		double numVal = ptr.getDouble(0);
		return numVal;
	}
	
	public static Object decodeTextValue(Pointer ptr, int valueLength, boolean convertStringsLazily) {
		if (valueLength<=0) {
			return "";
		}
		
		if (convertStringsLazily) {
			byte[] stringDataArr = new byte[valueLength];
			ptr.read(0, stringDataArr, 0, valueLength);

			LMBCSString lmbcsString = new LMBCSString(stringDataArr);
			return lmbcsString;
		}
		else {
			String txtVal = NotesStringUtils.fromLMBCS(ptr, valueLength);
			return txtVal;
		}
	}
	
	public static List<Object> decodeTextListValue(Pointer ptr, boolean convertStringsLazily) {
		//read a text list item value
		int listCountAsInt = ptr.getShort(0) & 0xffff;
		
		List<Object> listValues = new ArrayList<Object>(listCountAsInt);
		
		Memory retTextPointer = new Memory(Pointer.SIZE);
		ShortByReference retTextLength = new ShortByReference();
		
		for (short l=0; l<listCountAsInt; l++) {
			short result = NotesNativeAPI.get().ListGetText(ptr, false, l, retTextPointer, retTextLength);
			NotesErrorUtils.checkResult(result);
			
			//retTextPointer[0] points to the list entry text
			Pointer pointerToTextInMem = retTextPointer.getPointer(0);
			int retTextLengthAsInt = retTextLength.getValue() & 0xffff;
			
			if (retTextLengthAsInt==0) {
				listValues.add("");
			}
			else {
				if (convertStringsLazily) {
					byte[] stringDataArr = new byte[retTextLengthAsInt];
					pointerToTextInMem.read(0, stringDataArr, 0, retTextLengthAsInt);

					LMBCSString lmbcsString = new LMBCSString(stringDataArr);
					listValues.add(lmbcsString);
				}
				else {
					String currListEntry = NotesStringUtils.fromLMBCS(pointerToTextInMem, (short) retTextLengthAsInt);
					listValues.add(currListEntry);
				}
			}
		}
		
		return listValues;
	}
	
	public static NotesTimeDate decodeTimeDateAsNotesTimeDate(final Pointer ptr, int valueLength) {
		int[] innards = ptr.getIntArray(0, 2);
		return new NotesTimeDate(innards);
	}
	
	public static Calendar decodeTimeDate(final Pointer ptr, int valueLength) {
		int[] innards = ptr.getIntArray(0, 2);
		Calendar calDate = NotesDateTimeUtils.innardsToCalendar(innards);
		return calDate;
	}

	public static List<Object> decodeNumberList(Pointer ptr, int valueLength) {
		NotesRangeStruct range = NotesRangeStruct.newInstance(ptr);
		range.read();
		
		//read number of list and range entries in range
		int listEntriesAsInt = range.ListEntries & 0xffff;
		int rangeEntriesAsInt = range.RangeEntries & 0xffff;
		
		//skip range header
		Pointer ptrAfterRange = ptr.share(NotesConstants.rangeSize);
		
		//we create an object list, because number ranges contain double[] array
		//(not sure whether number ranges exist in real life)
		List<Object> numberValues = new ArrayList<Object>(listEntriesAsInt + rangeEntriesAsInt);
		for (int t=0; t<listEntriesAsInt; t++) {
			double numVal = ptrAfterRange.getDouble(t * 8);
			numberValues.add(numVal);
		}
		//skip list entries part of the buffer
		Pointer ptrAfterListEntries = ptrAfterRange.share(8 * listEntriesAsInt);
		
		for (int t=0; t<rangeEntriesAsInt; t++) {
			Pointer ptrListEntry = ptrAfterListEntries.share(t * NotesConstants.numberPairSize);
			NotesNumberPairStruct numPair = NotesNumberPairStruct.newInstance(ptrListEntry);
			numPair.read();
			double lower = numPair.Lower;
			double upper = numPair.Upper;
			
			numberValues.add(new double[] {lower, upper});
		}
		
		return numberValues;
	}
	public static List<Object> decodeTimeDateListAsNotesTimeDate(Pointer ptr) {
		NotesRangeStruct range = NotesRangeStruct.newInstance(ptr);
		range.read();
		
		//read number of list and range entries in range
		int listEntriesAsInt = range.ListEntries & 0xffff;
		int rangeEntriesAsInt = range.RangeEntries & 0xffff;
		
		//skip range header
		Pointer ptrAfterRange = ptr.share(NotesConstants.rangeSize);
		
		List<Object> calendarValues = new ArrayList<Object>(listEntriesAsInt + rangeEntriesAsInt);
		
		for (int t=0; t<listEntriesAsInt; t++) {
			Pointer ptrListEntry = ptrAfterRange.share(t * NotesConstants.timeDateSize);
			int[] innards = ptrListEntry.getIntArray(0, 2);
			calendarValues.add(new NotesTimeDate(innards));
		}
		
		//move position to the range data
		Pointer ptrAfterListEntries = ptrAfterRange.share(listEntriesAsInt * NotesConstants.timeDateSize);
		
		for (int t=0; t<rangeEntriesAsInt; t++) {
			Pointer ptrRangeEntry = ptrAfterListEntries.share(t * NotesConstants.timeDatePairSize);
			NotesTimeDatePairStruct timeDatePair = NotesTimeDatePairStruct.newInstance(ptrRangeEntry);
			timeDatePair.read();
			
			NotesTimeDateStruct lowerTimeDateStruct = timeDatePair.Lower;
			NotesTimeDateStruct upperTimeDateStruct = timeDatePair.Upper;
			
			int[] lowerTimeDateInnards = lowerTimeDateStruct.Innards;
			int[] upperTimeDateInnards = upperTimeDateStruct.Innards;
			
			NotesTimeDate lowerTimeDate = new NotesTimeDate(lowerTimeDateInnards);
			NotesTimeDate upperTimeDate = new NotesTimeDate(upperTimeDateInnards);
			
			calendarValues.add(new NotesTimeDate[] {lowerTimeDate, upperTimeDate});
		}
		
		return calendarValues;
	
	}
	
	public static List<Object> decodeTimeDateList(Pointer ptr, boolean useDayLight, int gmtOffset) {
		NotesRangeStruct range = NotesRangeStruct.newInstance(ptr);
		range.read();
		
		//read number of list and range entries in range
		int listEntriesAsInt = range.ListEntries & 0xffff;
		int rangeEntriesAsInt = range.RangeEntries & 0xffff;
		
		//skip range header
		Pointer ptrAfterRange = ptr.share(NotesConstants.rangeSize);
		
		List<Object> calendarValues = new ArrayList<Object>(listEntriesAsInt + rangeEntriesAsInt);
		
		for (int t=0; t<listEntriesAsInt; t++) {
			Pointer ptrListEntry = ptrAfterRange.share(t * NotesConstants.timeDateSize);
			int[] innards = ptrListEntry.getIntArray(0, 2);
			Calendar calDate = NotesDateTimeUtils.innardsToCalendar(innards);
			if (calDate!=null) {
				calendarValues.add(calDate);
			}
			else {
				//invalid TimeDate detected; we produce a "null" value to be able to detect this error
				Calendar nullCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				nullCal.set(Calendar.YEAR, 1);
				nullCal.set(Calendar.MONTH, 1);
				nullCal.set(Calendar.DAY_OF_MONTH, 1);
				nullCal.set(Calendar.HOUR, 0);
				nullCal.set(Calendar.MINUTE, 0);
				nullCal.set(Calendar.SECOND, 0);
				nullCal.set(Calendar.MILLISECOND, 0);
				calendarValues.add(nullCal);
			}
		}
		
		//move position to the range data
		Pointer ptrAfterListEntries = ptrAfterRange.share(listEntriesAsInt * NotesConstants.timeDateSize);
		
		for (int t=0; t<rangeEntriesAsInt; t++) {
			Pointer ptrRangeEntry = ptrAfterListEntries.share(t * NotesConstants.timeDatePairSize);
			NotesTimeDatePairStruct timeDatePair = NotesTimeDatePairStruct.newInstance(ptrRangeEntry);
			timeDatePair.read();
			
			NotesTimeDateStruct lowerTimeDate = timeDatePair.Lower;
			NotesTimeDateStruct upperTimeDate = timeDatePair.Upper;
			
			int[] lowerTimeDateInnards = lowerTimeDate.Innards;
			int[] upperTimeDateInnards = upperTimeDate.Innards;
			
			Calendar lowerCalDate = NotesDateTimeUtils.innardsToCalendar(lowerTimeDateInnards);
			if (lowerCalDate==null) {
				//invalid TimeDate detected; we produce a "null" value to be able to detect this error
				lowerCalDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				lowerCalDate.set(Calendar.YEAR, 1);
				lowerCalDate.set(Calendar.MONTH, 1);
				lowerCalDate.set(Calendar.DAY_OF_MONTH, 1);
				lowerCalDate.set(Calendar.HOUR, 0);
				lowerCalDate.set(Calendar.MINUTE, 0);
				lowerCalDate.set(Calendar.SECOND, 0);
				lowerCalDate.set(Calendar.MILLISECOND, 0);
			}
			Calendar upperCalDate = NotesDateTimeUtils.innardsToCalendar(upperTimeDateInnards);
			if (upperCalDate==null) {
				//invalid TimeDate detected; we produce a "null" value to be able to detect this error
				upperCalDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				upperCalDate.set(Calendar.YEAR, 0);
				upperCalDate.set(Calendar.MONTH, 0);
				upperCalDate.set(Calendar.DAY_OF_MONTH, 0);
				upperCalDate.set(Calendar.HOUR, 0);
				upperCalDate.set(Calendar.MINUTE, 0);
				upperCalDate.set(Calendar.SECOND, 0);
				upperCalDate.set(Calendar.MILLISECOND, 0);
			}
			
			calendarValues.add(new Calendar[] {lowerCalDate, upperCalDate});
		}
		
		return calendarValues;
	}
	
}
