package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.structs.NotesNumberPair;
import com.mindoo.domino.jna.structs.NotesRange;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesTimeDatePair;
import com.mindoo.domino.jna.utils.LMBCSString;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

public class ItemDecoder {

	public static double decodeNumber(NotesCAPI notesAPI, Pointer ptr, int valueLength) {
		double numVal = ptr.getDouble(0);
		return numVal;
	}
	
	public static Object decodeTextValue(NotesCAPI notesAPI, Pointer ptr, int valueLength, boolean convertStringsLazily) {
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
	
	public static List<Object> decodeTextListValue(NotesCAPI notesAPI, Pointer ptr, int valueLength, boolean convertStringsLazily) {
		if (valueLength==0) {
			return Collections.emptyList();
		}
		
		//read a text list item value
		int listCountAsInt = ptr.getShort(0) & 0xffff;
		
		List<Object> listValues = new ArrayList<Object>(listCountAsInt);
		
		Memory retTextPointer = new Memory(Pointer.SIZE);
		ShortByReference retTextLength = new ShortByReference();
		
		for (short l=0; l<listCountAsInt; l++) {
			short result = notesAPI.ListGetText(ptr, false, l, retTextPointer, retTextLength);
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
	
	public static Calendar decodeTimeDate(NotesCAPI notesAPI, Pointer ptr, int valueLength, boolean useDayLight, int gmtOffset) {
		NotesTimeDate timeDate = new NotesTimeDate(ptr);
		timeDate.read();
		
		Calendar calDate = NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, timeDate);
		return calDate;
	}
	
	public static List<Object> decodeNumberList(NotesCAPI notesAPI, Pointer ptr, int valueLength) {
		NotesRange range = new NotesRange(ptr);
		range.read();
		
		//read number of list and range entries in range
		int listEntriesAsInt = range.ListEntries & 0xffff;
		int rangeEntriesAsInt = range.RangeEntries & 0xffff;
		
		//skip range header
		Pointer ptrAfterRange = ptr.share(NotesCAPI.rangeSize);
		
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
			Pointer ptrListEntry = ptrAfterListEntries.share(t * NotesCAPI.numberPairSize);
			NotesNumberPair numPair = new NotesNumberPair(ptrListEntry);
			numPair.read();
			double lower = numPair.Lower;
			double upper = numPair.Upper;
			
			numberValues.add(new double[] {lower, upper});
		}
		
		return numberValues;
	}
	
	public static List<Object> decodeTimeDateList(NotesCAPI notesAPI, Pointer ptr, int valueLength, boolean useDayLight, int gmtOffset) {
		NotesRange range = new NotesRange(ptr);
		range.read();
		
		//read number of list and range entries in range
		int listEntriesAsInt = range.ListEntries & 0xffff;
		int rangeEntriesAsInt = range.RangeEntries & 0xffff;
		
		//skip range header
		Pointer ptrAfterRange = ptr.share(NotesCAPI.rangeSize);
		
		List<Object> calendarValues = new ArrayList<Object>(listEntriesAsInt + rangeEntriesAsInt);
		
		for (int t=0; t<listEntriesAsInt; t++) {
			Pointer ptrListEntry = ptrAfterRange.share(t * NotesCAPI.timeDateSize);
			NotesTimeDate timeDate = new NotesTimeDate(ptrListEntry);
			timeDate.read();
			
			Calendar calDate = NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, timeDate);
			if (calDate!=null) {
				calendarValues.add(calDate);
			}
		}
		
		//move position to the range data
		Pointer ptrAfterListEntries = ptrAfterRange.share(listEntriesAsInt * NotesCAPI.timeDateSize);
		
		for (int t=0; t<rangeEntriesAsInt; t++) {
			Pointer ptrRangeEntry = ptrAfterListEntries.share(t * NotesCAPI.timeDatePairSize);
			NotesTimeDatePair timeDatePair = new NotesTimeDatePair(ptrRangeEntry);
			timeDatePair.read();
			
			NotesTimeDate lowerTimeDate = timeDatePair.Lower;
			NotesTimeDate upperTimeDate = timeDatePair.Upper;
			
			Calendar lowerCalDate = NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, lowerTimeDate);
			Calendar upperCalDate = NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, upperTimeDate);
			
			calendarValues.add(new Calendar[] {lowerCalDate, upperCalDate});
		}
		
		return calendarValues;
	}
	
}
