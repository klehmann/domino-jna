package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.mindoo.domino.jna.structs.NotesItem;
import com.mindoo.domino.jna.structs.NotesItemValueTable;
import com.mindoo.domino.jna.structs.NotesNumberPair;
import com.mindoo.domino.jna.structs.NotesRange;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesTimeDatePair;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public class NotesSearchKeyEncoder {

	public static Memory b32_encodeKeys(Object[] keys) throws Exception {
		return b64_encodeKeys(keys);
	}
	
	/**
	 * Produces the keybuffer for NIFFindByKey
	 * 
	 * @param keys array of String, Double, Integer, Calendar, Date, Calendar[] (with two elements lower/upper), Date[] (with two elements lower/upper), {@link Interval}
	 * @return
	 * @throws Exception
	 */
	public static Memory b64_encodeKeys(Object[] keys) throws Exception {
		ByteArrayOutputStream metaDataByteOut = new ByteArrayOutputStream();

		ByteArrayOutputStream valueDataByteOut = new ByteArrayOutputStream();
		
		for (int i=0; i<keys.length; i++) {
			if (keys[i] == null) {
				throw new NullPointerException("Keys cannot be null. keys="+new ArrayList<Object>(Arrays.asList(keys)));
			}
		}
		
		//write placeholder for the total buffer size and the number of items, will be filled later
		metaDataByteOut.write(0);
		metaDataByteOut.write(0);
		
		metaDataByteOut.write(0);
		metaDataByteOut.write(0);
		

		for (int i=0; i<keys.length; i++) {
			Object currKey = keys[i];
			
			if (currKey instanceof String) {
				addStringKey(metaDataByteOut, valueDataByteOut, (String) currKey);
			}
			else if (currKey instanceof Double) {
				addNumberKey(metaDataByteOut, valueDataByteOut, ((Double) currKey).doubleValue());
			}
			else if (currKey instanceof Integer) {
				addNumberKey(metaDataByteOut, valueDataByteOut, ((Integer) currKey).doubleValue());
			}
			else if (currKey instanceof Date) {
				Calendar cal = Calendar.getInstance();
				cal.setTime((Date) currKey);
				addCalendarKey(metaDataByteOut, valueDataByteOut, cal);
			}
			else if (currKey instanceof Calendar) {
				addCalendarKey(metaDataByteOut, valueDataByteOut, (Calendar) currKey);
			}
			else if (currKey instanceof Date[]) {
				Date[] dateArr = (Date[]) currKey;
				Calendar[] calArr = new Calendar[dateArr.length];
				for (int j=0; i<dateArr.length; j++) {
					calArr[j] = Calendar.getInstance();
					calArr[j].setTime(dateArr[j]);
				}
				
				addCalendarRangeKey(metaDataByteOut, valueDataByteOut, calArr);
			}
			else if (currKey instanceof Calendar[]) {
				//date range
				addCalendarRangeKey(metaDataByteOut, valueDataByteOut, (Calendar[]) currKey);
			}
			else if (currKey instanceof Interval) {
				Interval interval = (Interval) currKey;
				DateTime startDateTime = interval.getStart();
				DateTime endDateTime = interval.getEnd();
				
				addCalendarRangeKey(metaDataByteOut, valueDataByteOut, new Calendar[] {startDateTime.toCalendar(Locale.getDefault()), endDateTime.toCalendar(Locale.getDefault())});
			}
			else if (currKey instanceof double[]) {
				//looks like this does not work (the C API documentation says it does not work either)
				addNumberRangeKey(metaDataByteOut, valueDataByteOut, (double[]) currKey);
			}
			else if (currKey instanceof Double[]) {
				Double[] objArr = (Double[]) currKey;
				double[] doubleArr = new double[objArr.length];
				for (int j=0; j<objArr.length; j++) {
					if (objArr[j] != null) {
						doubleArr[j] = objArr[j].doubleValue();
					}
				}
				//looks like this does not work (the C API documentation says it does not work either)
				addNumberRangeKey(metaDataByteOut, valueDataByteOut, doubleArr);
			}
			else {
				throw new IllegalArgumentException("Unknown key type: "+currKey+", class="+(currKey==null ? "null" : currKey.getClass().getName()));
			}
		}		
		
		byte[] metaDataByteArr = metaDataByteOut.toByteArray();
		byte[] valueDataByteArr = valueDataByteOut.toByteArray();
		
		Memory mem = new Memory(metaDataByteArr.length + valueDataByteArr.length);
		
		int offset = 0;
		for (int i=0; i<metaDataByteArr.length; i++) {
			mem.setByte(offset, metaDataByteArr[i]);
			offset++;
		}
		
		//update length value containing the total buffer size
		short totalSize = (short) ((4 + (keys.length * 4) + valueDataByteArr.length)  & 0xffff);
		
		NotesItemValueTable itemTable = new NotesItemValueTable(mem);
		itemTable.Length = totalSize;
		itemTable.Items = (short) keys.length;
		itemTable.write();
		
		for (int i=0; i<valueDataByteArr.length; i++) {
			mem.setByte(offset, valueDataByteArr[i]);
			offset++;
		}
		
//		System.out.println("Dumping meta data:");
//		StringBuilder sb = new StringBuilder();
//		for (int i=0; i<metaDataByteArr.length; i++) {
//			if (sb.length()>0)
//				sb.append(" ");
//			sb.append(Integer.toString(mem.getByte(i), 16) + " ");
//		}
//		System.out.println(sb.toString());
//		
//		
//		System.out.println("Dumping value data:");
//		sb.setLength(0);
//		for (int i=0; i<valueDataByteArr.length; i++) {
//			if (sb.length()>0)
//				sb.append(" ");
//			sb.append(Integer.toString(mem.getByte(metaDataByteArr.length + i), 16) + " ");
//		}
//		System.out.println(sb.toString());
//
//		System.out.println("Dumping mem:");
//		sb.setLength(0);
//		for (int i=0; i<mem.size(); i++) {
//			if (sb.length()>0)
//				sb.append(" ");
//			sb.append(Integer.toString(mem.getByte(i), 16) + " ");
//		}
//		System.out.println(sb.toString());

		return mem;
	}

	/**
	 * Writes data for a time search key
	 * 
	 * @param itemOut output stream for ITEM structure
	 * @param valueDataOut output stream for search key value
	 * @param currKey search key
	 * @throws Exception
	 */
	private static void addCalendarKey(OutputStream itemOut, OutputStream valueDataOut, Calendar currKey) throws Exception {
		Memory itemMem = new Memory(NotesCAPI.itemSize);
		NotesItem item = new NotesItem(itemMem);
		item.NameLength = 0;
		item.ValueLength = (short) (NotesCAPI.timeSize + 2);
		item.write();
		
		for (int i=0; i<NotesCAPI.itemSize; i++) {
			itemOut.write(itemMem.getByte(i));
		}
		
		//write data type
		Memory valueMem = new Memory(2 + 8);
		valueMem.setShort(0, (short) NotesCAPI.TYPE_TIME);
		
		boolean hasDate = NotesDateTimeUtils.hasDate(currKey);
		boolean hasTime = NotesDateTimeUtils.hasTime(currKey);
		
		int[] innards = NotesDateTimeUtils.calendarToInnards(currKey, hasDate, hasTime);
		Pointer timeDatePtr = valueMem.share(2);
		NotesTimeDate timeDate = new NotesTimeDate(timeDatePtr);
		timeDate.Innards[0] = innards[0];
		timeDate.Innards[1] = innards[1];
		timeDate.write();
		
		for (int i=0; i<valueMem.size(); i++) {
			valueDataOut.write(valueMem.getByte(i));
		}
	}
	
	/**
	 * Searching with number range keys is not supported yet (R9), as the 
	 * <a href="http://www-12.lotus.com/ldd/doc/domino_notes/9.0/api90ref.nsf/70cfe734675fd140852561ce00718042/35abe18f9580ca2d8525622e0062c48d?OpenDocument">documentation</a> says.
	 * 
	 * @param itemOut output stream for ITEM structure
	 * @param valueDataOut output stream for search key value
	 * @param currKey search key
	 * @throws Exception
	 */
	private static void addNumberRangeKey(OutputStream itemOut, OutputStream valueDataOut, double[] currKey) throws Exception {
		if (currKey.length!=2)
			throw new IllegalArgumentException("Double search key array must have exactly 2 elements. We found "+currKey.length);
		
		Memory itemMem = new Memory(NotesCAPI.itemSize);
		NotesItem item = new NotesItem(itemMem);
		item.NameLength = 0;
		item.ValueLength = (short) ((NotesCAPI.rangeSize + NotesCAPI.numberPairSize + 2) & 0xffff);
		item.write();

		for (int i=0; i<NotesCAPI.itemSize; i++) {
			itemOut.write(itemMem.getByte(i));
		}

		Memory valueMem = new Memory(NotesCAPI.rangeSize + NotesCAPI.numberPairSize + 2);
		valueMem.setShort(0, (short) NotesCAPI.TYPE_NUMBER_RANGE);

		Pointer rangePtr = valueMem.share(2);
		NotesRange range = new NotesRange(rangePtr);
		range.ListEntries = 0;
		range.RangeEntries = 1;
		range.write();

		Pointer pairPtr = rangePtr.share(NotesCAPI.rangeSize);
		NotesNumberPair pair = new NotesNumberPair(pairPtr);
		pair.Lower = currKey[0];
		pair.Upper = currKey[1];
		pair.write();
		
		for (int i=0; i<valueMem.size(); i++) {
			valueDataOut.write(valueMem.getByte(i));
		}
	}
	
	/**
	 * Writes data for a time range search key
	 * 
	 * @param itemOut output stream for ITEM structure
	 * @param valueDataOut output stream for search key value
	 * @param currKey search key, array with two values
	 * @throws Exception
	 */
	private static void addCalendarRangeKey(OutputStream itemOut, OutputStream valueDataOut, Calendar[] currKey) throws Exception {
		if (currKey.length!=2)
			throw new IllegalArgumentException("Calendar search key array must have exactly 2 elements. We found "+currKey.length);
		
		Memory itemMem = new Memory(NotesCAPI.itemSize);
		NotesItem item = new NotesItem(itemMem);
		item.NameLength = 0;
		item.ValueLength = (short) ((NotesCAPI.rangeSize + NotesCAPI.timeDatePairSize + 2) & 0xffff);
		item.write();

		for (int i=0; i<NotesCAPI.itemSize; i++) {
			itemOut.write(itemMem.getByte(i));
		}

		Memory valueMem = new Memory(NotesCAPI.rangeSize + NotesCAPI.timeDatePairSize + 2);
		valueMem.setShort(0, (short) NotesCAPI.TYPE_TIME_RANGE);
		
		Pointer rangePtr = valueMem.share(2);
		NotesRange range = new NotesRange(rangePtr);
		range.ListEntries = 0;
		range.RangeEntries = 1;
		range.write();
		
		Pointer pairPtr = rangePtr.share(NotesCAPI.rangeSize);
		NotesTimeDatePair pair = new NotesTimeDatePair(pairPtr);
		Calendar lowerBound = currKey[0];
		Calendar upperBound = currKey[1];
		NotesTimeDate lower = NotesDateTimeUtils.calendarToTimeDate(lowerBound);
		NotesTimeDate upper = NotesDateTimeUtils.calendarToTimeDate(upperBound);
		
		pair.Lower = lower;
		pair.Upper = upper;
		pair.write();
		
		for (int i=0; i<valueMem.size(); i++) {
			valueDataOut.write(valueMem.getByte(i));
		}
	}

	/**
	 * Writes data for a string search key
	 * 
	 * @param itemOut output stream for ITEM structure
	 * @param valueDataOut output stream for search key value
	 * @param currKey search key
	 * @throws Exception
	 */
	private static void addStringKey(OutputStream itemOut, OutputStream valueDataOut, String currKey) throws Exception {
		Memory strValueMem = NotesStringUtils.toLMBCS(currKey);
		
		Memory itemMem = new Memory(NotesCAPI.itemSize);
		NotesItem item = new NotesItem(itemMem);
		item.NameLength = 0;
		//-1 => remove 0 byte at the end
		item.ValueLength = (short) ((strValueMem.size()-1 + 2) & 0xffff);
		item.write();

		for (int i=0; i<NotesCAPI.itemSize; i++) {
			itemOut.write(itemMem.getByte(i));
		}

		Memory valueMem = new Memory(strValueMem.size()-1 + 2);
		short txtType = (short) NotesCAPI.TYPE_TEXT;
		valueMem.setShort(0, txtType);

		Pointer strValuePtr = valueMem.share(2);
		
		for (int i=0; i<strValueMem.size()-1; i++) {
			strValuePtr.setByte(i, strValueMem.getByte(i));
		}
		
		for (int i=0; i<valueMem.size(); i++) {
			valueDataOut.write(valueMem.getByte(i));
		}
	}

	/**
	 * Writes data for a number search key
	 * 
	 * @param itemOut output stream for ITEM structure
	 * @param valueDataOut output stream for search key value
	 * @param doubleValue
	 * @throws Exception
	 */
	private static void addNumberKey(OutputStream itemOut, OutputStream valueDataOut, double doubleValue) throws Exception {
		Memory itemMem = new Memory(NotesCAPI.itemSize);
		NotesItem item = new NotesItem(itemMem);
		item.NameLength = 0;
		item.ValueLength = (short) (8 + 2);
		item.write();

		for (int i=0; i<NotesCAPI.itemSize; i++) {
			itemOut.write(itemMem.getByte(i));
		}

		Memory valueMem = new Memory(8 + 2);
		valueMem.setShort(0, (short) NotesCAPI.TYPE_NUMBER);
		
		Pointer doubleValPtr = valueMem.share(2);
		doubleValPtr.setDouble(0, doubleValue);
		
		for (int i=0; i<valueMem.size(); i++) {
			valueDataOut.write(valueMem.getByte(i));
		}
	}


}
