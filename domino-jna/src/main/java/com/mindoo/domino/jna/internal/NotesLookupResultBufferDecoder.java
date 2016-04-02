package com.mindoo.domino.jna.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesViewLookupResultData;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesCollectionStats;
import com.mindoo.domino.jna.structs.NotesItem;
import com.mindoo.domino.jna.structs.NotesItemTable;
import com.mindoo.domino.jna.structs.NotesItemValueTable;
import com.mindoo.domino.jna.structs.NotesNumberPair;
import com.mindoo.domino.jna.structs.NotesRange;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesTimeDatePair;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to decode the buffer returned by data lookups, e.g. in {@link NotesCollection}'s
 * and for database searches.
 * 
 * @author Karsten Lehmann
 */
public class NotesLookupResultBufferDecoder {
	
	/**
	 * Decodes the buffer, 32 bit mode
	 * 
	 * @param bufferHandle buffer handle
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param columnsToDecode optional array of columns to decode or null
	 * @param indexModifiedSequenceNo index modified sequence no
	 * @return collection data
	 */
	public static NotesViewLookupResultData b32_decodeCollectionLookupResultBuffer(int bufferHandle, int numEntriesSkipped, int numEntriesReturned,
			EnumSet<ReadMask> returnMask, short signalFlags, boolean[] columnsToDecode, String pos,
			int indexModifiedSequenceNo) {
		return b64_decodeCollectionLookupResultBuffer(bufferHandle, numEntriesSkipped, numEntriesReturned, returnMask, signalFlags, columnsToDecode, pos, indexModifiedSequenceNo);
	}

	/**
	 * Decodes the buffer, 64 bit mode
	 * 
	 * @param bufferHandle buffer handle
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param columnsToDecode optional array of columns to decode or null
	 * @param pos position to add to NotesViewData object in case view data is read via {@link NotesCollection#findByKeyAndReadData(EnumSet, EnumSet, EnumSet, boolean[], Object...)}
	 * @param indexModifiedSequenceNo index modified sequence no
	 * @return collection data
	 */
	public static NotesViewLookupResultData b64_decodeCollectionLookupResultBuffer(long bufferHandle, int numEntriesSkipped, int numEntriesReturned,
			EnumSet<ReadMask> returnMask, short signalFlags, boolean[] columnsToDecode, String pos, int indexModifiedSequenceNo) {
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Pointer bufferPtr;
		if (NotesJNAContext.is64Bit()) {
			bufferPtr = notesAPI.b64_OSLockObject(bufferHandle);
		}
		else {
			bufferPtr = notesAPI.b32_OSLockObject((int) bufferHandle);
		}

		int bufferPos = 0;
		
		NotesCollectionStats collectionStats = null;
		
		//compute structure sizes
		
		try {
			if (returnMask.contains(ReadMask.COLLECTIONSTATS)) {
				NotesCollectionStats tmpStats = new NotesCollectionStats(bufferPtr);
				tmpStats.read();
				
				collectionStats = new NotesCollectionStats();
				collectionStats.TopLevelEntries = tmpStats.TopLevelEntries;
				collectionStats.LastModifiedTime = tmpStats.LastModifiedTime;
						
				bufferPos += tmpStats.size();
			}

			List<NotesViewEntryData> viewEntries = new ArrayList<NotesViewEntryData>();
			
            int gmtOffset = NotesDateTimeUtils.getGMTOffset();
            boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
            
            Memory sharedCollectionPositionMem = null;
			NotesCollectionPosition sharedPosition = null;
			if (returnMask.contains(ReadMask.INDEXPOSITION)) {
				//allocate memory for a position
				sharedCollectionPositionMem = new Memory(NotesCAPI.collectionPositionSize);
				sharedPosition = new NotesCollectionPosition(sharedCollectionPositionMem);
			}
			
			for (int i=0; i<numEntriesReturned; i++) {
				NotesViewEntryData newData = new NotesViewEntryData();
				viewEntries.add(newData);
				
				if (returnMask.contains(ReadMask.NOTEID)) {
					ByteBuffer noteIdBuf = bufferPtr.getByteBuffer(bufferPos, 4);
					IntBuffer noteIdBufAsInt = noteIdBuf.asIntBuffer();

					int entryNoteId = noteIdBufAsInt.get(0);
					newData.setNoteId(entryNoteId);
					
					bufferPos+=4;
				}
				
				if (returnMask.contains(ReadMask.NOTEUNID)) {
					ByteBuffer unidBytes = bufferPtr.getByteBuffer(bufferPos, 16);
					String unid = NotesDatabase.toUNID(unidBytes);
					newData.setUNID(unid);
					
					bufferPos+=16;
				}
				if (returnMask.contains(ReadMask.NOTECLASS)) {
					short noteClass = bufferPtr.getShort(bufferPos);
					newData.setNoteClass(noteClass);
					
					bufferPos+=2;
				}
				if (returnMask.contains(ReadMask.INDEXSIBLINGS)) {
					int siblingCount = bufferPtr.getInt(bufferPos);
					newData.setSiblingCount(siblingCount);
					
					bufferPos+=4;
				}
				if (returnMask.contains(ReadMask.INDEXCHILDREN)) {
					int childCount = bufferPtr.getInt(bufferPos);
					newData.setChildCount(childCount);
					
					bufferPos+=4;
				}
				if (returnMask.contains(ReadMask.INDEXDESCENDANTS)) {
					int descendantCount = bufferPtr.getInt(bufferPos);
					newData.setDescendantCount(descendantCount);
					
					bufferPos+=4;
				}
				if (returnMask.contains(ReadMask.INDEXANYUNREAD)) {
					boolean isAnyUnread = bufferPtr.getShort(bufferPos) == 1;
					newData.setAnyUnread(isAnyUnread);
					
					bufferPos+=2;
				}
				if (returnMask.contains(ReadMask.INDENTLEVELS)) {
					short indentLevels = bufferPtr.getShort(bufferPos);
					newData.setIndentLevels(indentLevels);
					
					bufferPos += 2;
				}
				if (returnMask.contains(ReadMask.SCORE)) {
					short score = bufferPtr.getShort(bufferPos);
					newData.setFTScore(score);
					
					bufferPos += 2;
				}
				if (returnMask.contains(ReadMask.INDEXUNREAD)) {
					boolean isUnread = bufferPtr.getShort(bufferPos) == 1;
					newData.setUnread(isUnread);
					
					bufferPos+=2;
				}
				if (returnMask.contains(ReadMask.INDEXPOSITION)) {
					short level = bufferPtr.getShort(bufferPos);
					int truncatedCollectionPositionSize = 4 * (level + 2);
					sharedCollectionPositionMem.clear();

					for (int j=0; j<truncatedCollectionPositionSize; j++) {
						sharedCollectionPositionMem.setByte(j, bufferPtr.getByte(bufferPos + j));
					}
					sharedPosition.read();
					int[] posArr = new int[sharedPosition.Level+1];
					for (int j=0; j<posArr.length; j++) {
						posArr[j] = sharedPosition.Tumbler[j];
					}
					newData.setPosition(posArr);
					
					bufferPos += truncatedCollectionPositionSize;
				}
				if (returnMask.contains(ReadMask.SUMMARYVALUES)) {
//					The information in a view summary of values is as follows:
//
//						ITEM_VALUE_TABLE containing header information (total length of summary, number of items in summary)
//						WORD containing the length of item #1 (including data type)
//						WORD containing the length of item #2 (including data type)
//						WORD containing the length of item #3 (including data type)
//						...
//						USHORT containing the data type of item #1
//						value of item #1
//						USHORT containing the data type of item #2
//						value of item #2
//						USHORT containing the data type of item #3
//						value of item #3
//						....
					
					int startBufferPosOfSummaryValues = bufferPos;

					Pointer itemValueTablePtr = bufferPtr.share(bufferPos);
					ItemValueTableData itemTableData = decodeItemValueTable(itemValueTablePtr, gmtOffset, useDayLight, columnsToDecode);
					
					//move to the end of the buffer
					bufferPos = startBufferPosOfSummaryValues + itemTableData.getTotalBufferLength();

					Object[] decodedItemValues = itemTableData.getItemValues();
					newData.setColumnValues(decodedItemValues);
				}
				if (returnMask.contains(ReadMask.SUMMARY)) {
					int startBufferPosOfSummaryValues = bufferPos;

					Pointer itemTablePtr = bufferPtr.share(bufferPos);
					ItemTableData itemTableData = decodeItemTable(itemTablePtr, gmtOffset, useDayLight);
					
					//move to the end of the buffer
					bufferPos = startBufferPosOfSummaryValues + itemTableData.getTotalBufferLength();

					Map<String,Object> itemValues = itemTableData.asMap();
					newData.setSummaryData(itemValues);
				}
			}
			
			return new NotesViewLookupResultData(collectionStats, viewEntries, numEntriesSkipped, numEntriesReturned, signalFlags, pos, indexModifiedSequenceNo);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(bufferHandle);
				notesAPI.b64_OSMemFree(bufferHandle);
			}
			else {
				notesAPI.b32_OSUnlockObject((int)bufferHandle);
				notesAPI.b32_OSMemFree((int)bufferHandle);
			}
		}
		
	}

	/**
	 * Decodes an ITEM_VALUE_TABLE structure, which contains an ordered list of item values
	 * 
	 * @param bufferPtr pointer to a buffer
	 * @param gmtOffset GMT offset ({@link NotesDateTimeUtils#getGMTOffset()}) to parse datetime values
	 * @param useDayLight DST ({@link NotesDateTimeUtils#isDaylightTime()}) to parse datetime values
	 * @param columnsToDecode optional array of columns to decode or null
	 * @return
	 */
	public static ItemValueTableData decodeItemValueTable(Pointer bufferPtr, int gmtOffset, boolean useDayLight, boolean[] columnsToDecode) {
		int bufferPos = 0;
		NotesItemValueTable itemValueTable = new NotesItemValueTable(bufferPtr);
		itemValueTable.read();
		
		//skip item value table header
		bufferPos += itemValueTable.size();
		
//		The information in a view summary of values is as follows:
//
//			ITEM_VALUE_TABLE containing header information (total length of summary, number of items in summary)
//			WORD containing the length of item #1 (including data type)
//			WORD containing the length of item #2 (including data type)
//			WORD containing the length of item #3 (including data type)
//			...
//			USHORT containing the data type of item #1
//			value of item #1
//			USHORT containing the data type of item #2
//			value of item #2
//			USHORT containing the data type of item #3
//			value of item #3
//			....
		
		int itemsCount = itemValueTable.getItemsAsInt();
		int[] itemValueLengths = new int[itemsCount];
		//we don't have any item names:
		int[] itemNameLengths = null;
		
		//read all item lengths
		for (int j=0; j<itemsCount; j++) {
			//convert USHORT to int without sign
			itemValueLengths[j] = bufferPtr.getShort(bufferPos) & 0xffff;
			bufferPos += 2;
		}

		ItemValueTableData data = new ItemValueTableData();
		data.m_totalBufferLength = itemValueTable.getLengthAsInt();
		data.m_itemsCount = itemsCount;

		Pointer itemValuePtr = bufferPtr.share(bufferPos);
		populateItemValueTableData(itemValuePtr, gmtOffset, useDayLight, itemsCount, itemNameLengths, itemValueLengths, columnsToDecode, data);

		return data;
	}

	/**
	 * This utility method extracts the item values from the buffer
	 * 
	 * @param bufferPtr buffer pointer
	 * @param gmtOffset GMT offset ({@link NotesDateTimeUtils#getGMTOffset()}) to parse datetime values
	 * @param useDayLight DST ({@link NotesDateTimeUtils#isDaylightTime()}) to parse datetime values
	 * @param itemsCount number of items in the buffer
	 * @param itemValueLengths lengths of the item values
	 * @param columnsToDecode optional array of columns to decode or null
	 * @param retData data object to populate
	 */
	private static void populateItemValueTableData(Pointer bufferPtr, int gmtOffset, boolean useDayLight, int itemsCount, int[] itemNameLengths, int[] itemValueLengths, boolean[] decodeColumns, ItemValueTableData retData) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		int bufferPos = 0;
		String[] itemNames = new String[itemsCount];
		int[] itemDataTypes = new int[itemsCount];
		Pointer[] itemValueBufferPointers = new Pointer[itemsCount];
		int[] itemValueBufferSizes = new int[itemsCount];
		Object[] decodedItemValues = new Object[itemsCount];
		
		for (int j=0; j<itemsCount; j++) {
			if (itemNameLengths!=null && itemNameLengths[j]>0) {
				//casting to short should be safe for item names
				itemNames[j] = NotesStringUtils.fromLMBCS(bufferPtr.share(bufferPos), (short) itemNameLengths[j]);
				bufferPos += itemNameLengths[j];
			}
			
			
			//read data type
			if (itemValueLengths[j] == 0) {
				/* If an item has zero length it indicates an "empty" item in the
				summary. This might occur in a lower-level category and stand for a
				higher-level category that has already appeared. Or an empty item might
				be a field that is missing in a response doc. Just print * as a place
				holder and go on to the next item in the pSummary. */
				continue;
			}
			else {
				itemDataTypes[j] = bufferPtr.getShort(bufferPos) & 0xffff;
				
				//add data type size to position
				bufferPos += 2;
				
				//read item values
				itemValueBufferPointers[j] = bufferPtr.share(bufferPos);
				itemValueBufferSizes[j] = itemValueLengths[j] - 2;
				
				//skip item value
				bufferPos += (itemValueLengths[j] - 2);

				if (itemDataTypes[j] == NotesCAPI.TYPE_TEXT) {
					//read a text item value
					String txtVal = NotesStringUtils.fromLMBCS(itemValueBufferPointers[j], (short) itemValueBufferSizes[j]);
					decodedItemValues[j] = txtVal;
				}
				else if (itemDataTypes[j] == NotesCAPI.TYPE_TEXT_LIST) {
					//read a text list item value
					int listCountAsInt = itemValueBufferPointers[j].getShort(0) & 0xffff;
					
					List<String> listValues = new ArrayList<String>(listCountAsInt);
					
					Memory retTextPointer = new Memory(Pointer.SIZE);
					ShortByReference retTextLength = new ShortByReference();
					
					for (short l=0; l<listCountAsInt; l++) {
						short result = notesAPI.ListGetText(itemValueBufferPointers[j], false, l, retTextPointer, retTextLength);
						NotesErrorUtils.checkResult(result);
						
						//retTextPointer[0] points to the list entry text
						Pointer pointerToTextInMem = retTextPointer.getPointer(0);
						int retTextLengthAsInt = retTextLength.getValue() & 0xffff;
						
						String currListEntry = NotesStringUtils.fromLMBCS(pointerToTextInMem, (short) retTextLengthAsInt);
						listValues.add(currListEntry);
					}
					
					decodedItemValues[j] = listValues;
				}
				else if (itemDataTypes[j] == NotesCAPI.TYPE_NUMBER) {
					double numVal = itemValueBufferPointers[j].getDouble(0);
					decodedItemValues[j] = numVal;
				}
				else if (itemDataTypes[j] == NotesCAPI.TYPE_TIME) {
					NotesTimeDate timeDate = new NotesTimeDate(itemValueBufferPointers[j]);
					timeDate.read();
					
					Calendar calDate = NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, timeDate);
					decodedItemValues[j] = calDate;
				}
				else if (itemDataTypes[j] == NotesCAPI.TYPE_NUMBER_RANGE) {
					NotesRange range = new NotesRange(itemValueBufferPointers[j]);
					range.read();
					//read number of list and range entries in range
					int listEntriesAsInt = range.ListEntries & 0xffff;
					int rangeEntriesAsInt = range.RangeEntries & 0xffff;
					
					//skip range header
					Pointer ptrAfterRange = itemValueBufferPointers[j].share(NotesCAPI.rangeSize);
					
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
					
					decodedItemValues[j] = numberValues;
				}
				else if (itemDataTypes[j] == NotesCAPI.TYPE_TIME_RANGE) {
					NotesRange range = new NotesRange(itemValueBufferPointers[j]);
					range.read();
					
					//read number of list and range entries in range
					int listEntriesAsInt = range.ListEntries & 0xffff;
					int rangeEntriesAsInt = range.RangeEntries & 0xffff;
					
					//skip range header
					Pointer ptrAfterRange = itemValueBufferPointers[j].share(NotesCAPI.rangeSize);
					
					//we create an object list, because number ranges contain double[] array
					//(not sure whether number ranges exist in real life)
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
					
					decodedItemValues[j] = calendarValues;
				}
			}
		}
		
		retData.m_itemValues = decodedItemValues;
		retData.m_itemDataTypes = itemDataTypes;
		if (retData instanceof ItemTableData) {
			((ItemTableData)retData).m_itemNames = itemNames;
		}
	}

	/**
	 * Decodes an ITEM_TABLE structure with item names and item values
	 * 
	 * @param bufferPtr pointer to a buffer
	 * @param gmtOffset GMT offset ({@link NotesDateTimeUtils#getGMTOffset()}) to parse datetime values
	 * @param useDayLight DST ({@link NotesDateTimeUtils#isDaylightTime()}) to parse datetime values
	 * @return data
	 */
	public static ItemTableData decodeItemTable(Pointer bufferPtr, int gmtOffset, boolean useDayLight) {
		int bufferPos = 0;
		NotesItemTable itemTable = new NotesItemTable(bufferPtr);
		itemTable.read();
		
		//skip item table header
		bufferPos += itemTable.size();

//		typedef struct {
//			   USHORT Length; /*  total length of this buffer */
//			   USHORT Items;  /* number of items in the table */
//			/* now come an array of ITEMs */
//			/* now comes the packed text containing the item names. */
//			} ITEM_TABLE;					
		
		int itemsCount = itemTable.getItemsAsInt();
		NotesItem[] items = new NotesItem[itemsCount];
		int[] itemValueLengths = new int[itemsCount];
		int[] itemNameLengths = new int[itemsCount];
		
		//read ITEM structures for each item
		for (int j=0; j<itemsCount; j++) {
			Pointer itemPtr = bufferPtr.share(bufferPos);
			items[j] = new NotesItem(itemPtr);
			items[j].read();
			
			itemValueLengths[j] = items[j].getValueLengthAsInt();
			itemNameLengths[j] = items[j].getNameLengthAsInt();
			
			bufferPos += items[j].size();
		}

		ItemTableData data = new ItemTableData();
		data.m_totalBufferLength = itemTable.getLengthAsInt();
		data.m_itemsCount = itemsCount;
		
		Pointer itemValuePtr = bufferPtr.share(bufferPos);
		populateItemValueTableData(itemValuePtr, gmtOffset, useDayLight, itemsCount, itemNameLengths, itemValueLengths, (boolean[]) null, data);
		
		return data;
	}

	/**
	 * Container class for the data parsed from an ITEM_VALUE_TABLE structure
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ItemValueTableData {
		protected Object[] m_itemValues;
		protected int[] m_itemDataTypes;
		protected int m_totalBufferLength;
		protected int m_itemsCount;
		
		public Object[] getItemValues() {
			return m_itemValues;
		}
		
		public int[] getItemDataTypes() {
			return m_itemDataTypes;
		}
		
		public int getTotalBufferLength() {
			return m_totalBufferLength;
		}
		
		public int getItemsCount() {
			return m_itemsCount;
		}
	}
	
	/**
	 * Container class for the data parsed from an ITEM_VALUE structure
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ItemTableData extends ItemValueTableData {
		protected String[] m_itemNames;
		
		public String[] getItemNames() {
			return m_itemNames;
		}
		
		/**
		 * Converts the values to a Java {@link Map}
		 * 
		 * @return data as map
		 */
		public Map<String,Object> asMap() {
			Map<String,Object> data = new CaseInsensitiveMap<String, Object>();
			int itemCount = getItemsCount();
			for (int i=0; i<itemCount; i++) {
				data.put(m_itemNames[i], m_itemValues[i]);
			}
			return data;
		}
	}
	
}
