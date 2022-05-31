package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.IItemValueTableData;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollectionStats;
import com.mindoo.domino.jna.NotesDateRange;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.NotesViewLookupResultData;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.structs.NotesCollectionStatsStruct;
import com.mindoo.domino.jna.internal.structs.NotesItemTableStruct;
import com.mindoo.domino.jna.utils.LMBCSString;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;

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
	 * @param parentCollection parent collection
	 * @param bufferHandle buffer handle
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param pos position of first match, if returned by find method
	 * @param indexModifiedSequenceNo index modified sequence no
	 * @param retDiffTime only set in {@link NotesCollection#readEntriesExt(com.mindoo.domino.jna.NotesCollectionPosition, EnumSet, int, EnumSet, int, EnumSet, NotesTimeDate, NotesIDTable, Integer)}
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param singleColumnLookupName for single column lookups, programmatic name of lookup column
	 * @return collection data
	 */
	public static NotesViewLookupResultData b32_decodeCollectionLookupResultBuffer(NotesCollection parentCollection, int bufferHandle, int numEntriesSkipped, int numEntriesReturned,
			EnumSet<ReadMask> returnMask, short signalFlags, String pos,
			int indexModifiedSequenceNo, NotesTimeDate retDiffTime, boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar,
			String singleColumnLookupName) {
		return b64_decodeCollectionLookupResultBuffer(parentCollection, bufferHandle, numEntriesSkipped, numEntriesReturned,
				returnMask, signalFlags, pos, indexModifiedSequenceNo, retDiffTime, convertStringsLazily, convertNotesTimeDateToCalendar,
				singleColumnLookupName);
	}

	/**
	 * Decodes the buffer, 32 bit mode
	 * 
	 * @param parentCollection parent collection
	 * @param ptrBuffer buffer pointer
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param pos position of first match, if returned by find method
	 * @param indexModifiedSequenceNo index modified sequence no
	 * @param retDiffTime only set in {@link NotesCollection#readEntriesExt(com.mindoo.domino.jna.NotesCollectionPosition, EnumSet, int, EnumSet, int, EnumSet, NotesTimeDate, NotesIDTable, Integer)}
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param singleColumnLookupName for single column lookups, programmatic name of lookup column
	 * @return collection data
	 */
	public static NotesViewLookupResultData b32_decodeCollectionLookupResultBuffer(NotesCollection parentCollection, Pointer ptrBuffer,
			int numEntriesSkipped, int numEntriesReturned,
			EnumSet<ReadMask> returnMask, short signalFlags, String pos,
			int indexModifiedSequenceNo, NotesTimeDate retDiffTime, boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar,
			String singleColumnLookupName) {
		return b64_decodeCollectionLookupResultBuffer(parentCollection, ptrBuffer, numEntriesSkipped, numEntriesReturned,
				returnMask, signalFlags, pos, indexModifiedSequenceNo, retDiffTime, convertStringsLazily, convertNotesTimeDateToCalendar,
				singleColumnLookupName);
	}
	
	/**
	 * Decodes the buffer, 64 bit mode
	 * 
	 * @param parentCollection parent collection
	 * @param bufferHandle buffer handle
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param pos position to add to NotesViewLookupResultData object in case view data is read via {@link NotesCollection#findByKeyExtended2(EnumSet, EnumSet, Object...)}
	 * @param indexModifiedSequenceNo index modified sequence no
	 * @param retDiffTime only set in {@link NotesCollection#readEntriesExt(com.mindoo.domino.jna.NotesCollectionPosition, EnumSet, int, EnumSet, int, EnumSet, NotesTimeDate, NotesIDTable, Integer)}
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param singleColumnLookupName for single column lookups, programmatic name of lookup column
	 * @return collection data
	 */
	public static NotesViewLookupResultData b64_decodeCollectionLookupResultBuffer(NotesCollection parentCollection, long bufferHandle, int numEntriesSkipped, int numEntriesReturned,
			EnumSet<ReadMask> returnMask, short signalFlags, String pos, int indexModifiedSequenceNo, NotesTimeDate retDiffTime,
			boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar, String singleColumnLookupName) {
		
		Pointer bufferPtr;
		if (PlatformUtils.is64Bit()) {
			bufferPtr = Mem64.OSLockObject(bufferHandle);
		}
		else {
			bufferPtr = Mem32.OSLockObject((int) bufferHandle);
		}
		
		try {
			return b64_decodeCollectionLookupResultBuffer(parentCollection, bufferPtr, numEntriesSkipped,
					numEntriesReturned, returnMask, signalFlags, pos, indexModifiedSequenceNo, retDiffTime,
					convertStringsLazily, convertNotesTimeDateToCalendar, singleColumnLookupName);
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				Mem64.OSUnlockObject(bufferHandle);
				short result = Mem64.OSMemFree(bufferHandle);
				NotesErrorUtils.checkResult(result);
			}
			else {
				Mem32.OSUnlockObject((int)bufferHandle);
				short result = Mem32.OSMemFree((int)bufferHandle);
				NotesErrorUtils.checkResult(result);
			}
		}
	}
	
	/**
	 * Decodes the buffer, 64 bit mode
	 * 
	 * @param parentCollection parent collection
	 * @param bufferPtr buffer pointer
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param pos position to add to NotesViewLookupResultData object in case view data is read via {@link NotesCollection#findByKeyExtended2(EnumSet, EnumSet, Object...)}
	 * @param indexModifiedSequenceNo index modified sequence no
	 * @param retDiffTime only set in {@link NotesCollection#readEntriesExt(com.mindoo.domino.jna.NotesCollectionPosition, EnumSet, int, EnumSet, int, EnumSet, NotesTimeDate, NotesIDTable, Integer)}
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param singleColumnLookupName for single column lookups, programmatic name of lookup column
	 * @return collection data
	 */
	public static NotesViewLookupResultData b64_decodeCollectionLookupResultBuffer(NotesCollection parentCollection,
			Pointer bufferPtr, int numEntriesSkipped, int numEntriesReturned,
			EnumSet<ReadMask> returnMask, short signalFlags, String pos, int indexModifiedSequenceNo, NotesTimeDate retDiffTime,
			boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar, String singleColumnLookupName) {

		int bufferPos = 0;
		
		NotesCollectionStats collectionStats = null;

		if (returnMask.contains(ReadMask.COLLECTIONSTATS)) {
			NotesCollectionStatsStruct tmpStats = NotesCollectionStatsStruct.newInstance(bufferPtr);
			tmpStats.read();
			
			collectionStats = new NotesCollectionStats(tmpStats.TopLevelEntries, tmpStats.LastModifiedTime);
					
			bufferPos += tmpStats.size();
		}

		List<NotesViewEntryData> viewEntries = new ArrayList<NotesViewEntryData>();
		
		final boolean decodeAllValues = true;

		if (returnMask.size()==1 && returnMask.contains(ReadMask.NOTEID)) {
			//special optimized case for reading only note ids
			int[] noteIds = new int[numEntriesReturned];
			bufferPtr.read(0, noteIds, 0, numEntriesReturned);
			
			for (int i=0; i<noteIds.length; i++) {
				NotesViewEntryData newData = new NotesViewEntryData(parentCollection);
				viewEntries.add(newData);
				newData.setNoteId(noteIds[i]);
			}
			
		}
		else {
			for (int i=0; i<numEntriesReturned; i++) {
				NotesViewEntryData newData = new NotesViewEntryData(parentCollection);
				viewEntries.add(newData);

				if (returnMask.contains(ReadMask.NOTEID)) {
					int entryNoteId = bufferPtr.getInt(bufferPos);
					newData.setNoteId(entryNoteId);

					bufferPos+=4;
				}

				if (returnMask.contains(ReadMask.NOTEUNID)) {
					long[] unidLongs = bufferPtr.getLongArray(bufferPos, 2);
					newData.setUNID(unidLongs);

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
					int[] posArr = new int[level+1];
					bufferPtr.read(bufferPos + 2 /* level */  + 2 /* MinLevel+MaxLevel */, posArr, 0, level+1);

					newData.setPosition(posArr);

					bufferPos += 4 * (level + 2);
				}
				if (returnMask.contains(ReadMask.SUMMARYVALUES)) {
					//				The information in a view summary of values is as follows:
					//
					//					ITEM_VALUE_TABLE containing header information (total length of summary, number of items in summary)
					//					WORD containing the length of item #1 (including data type)
					//					WORD containing the length of item #2 (including data type)
					//					WORD containing the length of item #3 (including data type)
					//					...
					//					USHORT containing the data type of item #1
					//					value of item #1
					//					USHORT containing the data type of item #2
					//					value of item #2
					//					USHORT containing the data type of item #3
					//					value of item #3
					//					....

					int startBufferPosOfSummaryValues = bufferPos;

					Pointer itemValueTablePtr = bufferPtr.share(bufferPos);
					ItemValueTableDataImpl itemTableData = (ItemValueTableDataImpl) decodeItemValueTable(itemValueTablePtr,
							convertStringsLazily, convertNotesTimeDateToCalendar, decodeAllValues);

					//move to the end of the buffer
					bufferPos = startBufferPosOfSummaryValues + itemTableData.getTotalBufferLength();

					Object[] decodedItemValues = new Object[itemTableData.getItemsCount()];
					for (int c=0; c<itemTableData.getItemsCount(); c++) {
						decodedItemValues[c] = itemTableData.getItemValue(c);
					}
					newData.setColumnValues(decodedItemValues);
					//add some statistical information to the data object to be able to see which columns "pollute" the summary buffer
					newData.setColumnValueSizesInBytes(itemTableData.getItemValueLengthsInBytes());
				}
				if (returnMask.contains(ReadMask.SUMMARY)) {
					int startBufferPosOfSummaryValues = bufferPos;

					Pointer itemTablePtr = bufferPtr.share(bufferPos);
					ItemTableDataImpl itemTableData = (ItemTableDataImpl) decodeItemTable(itemTablePtr, convertStringsLazily,
							convertNotesTimeDateToCalendar, decodeAllValues);

					//move to the end of the buffer
					bufferPos = startBufferPosOfSummaryValues + itemTableData.getTotalBufferLength();

					Map<String,Object> itemValues = itemTableData.asMap(false);
					newData.setSummaryData(itemValues);
				}
				if (singleColumnLookupName!=null) {
					newData.setSingleColumnLookupName(singleColumnLookupName);
				}
			}
		}
		
		return new NotesViewLookupResultData(collectionStats, viewEntries, numEntriesSkipped, numEntriesReturned, signalFlags, pos, indexModifiedSequenceNo, retDiffTime);
	}

	/**
	 * Produces an ITEM_TABLE by decoding an ITEM_VALUE_TABLE structure, which contains an ordered list of item values,
	 * and adding an array of column names
	 * 
	 * @param columnItemNames column item names
	 * @param bufferPtr pointer to a buffer
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param decodeAllValues true to decode all values in the buffer
	 * @return item value table data
	 */
	public static IItemTableData decodeItemValueTableWithColumnNames(String[] columnItemNames,
			Pointer bufferPtr, boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar, boolean decodeAllValues) {
		ItemValueTableDataImpl valueTable = (ItemValueTableDataImpl) decodeItemValueTable(bufferPtr, convertStringsLazily, convertNotesTimeDateToCalendar, decodeAllValues);
		IItemTableData itemTableData = new ItemTableDataImpl(columnItemNames, valueTable);
		return itemTableData;
	}
	
	/**
	 * Decodes an ITEM_VALUE_TABLE structure, which contains an ordered list of item values
	 * 
	 * @param bufferPtr pointer to a buffer
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param decodeAllValues true to decode all values in the buffer
	 * @return item value table data
	 */
	public static IItemValueTableData decodeItemValueTable(Pointer bufferPtr,
			boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar, boolean decodeAllValues) {
		int bufferPos = 0;
		
		//skip item value table header
		bufferPos += NotesConstants.itemValueTableSize;
		
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
		
		int totalBufferLength = bufferPtr.getShort(0) & 0xffff;
		int itemsCount = bufferPtr.getShort(2) & 0xffff;
		
		int[] itemValueLengths = new int[itemsCount];
		//we don't have any item names:
		int[] itemNameLengths = null;
		
		//read all item lengths
		for (int j=0; j<itemsCount; j++) {
			//convert USHORT to int without sign
			itemValueLengths[j] = bufferPtr.getShort(bufferPos) & 0xffff;
			bufferPos += 2;
		}

		ItemValueTableDataImpl data = new ItemValueTableDataImpl(convertStringsLazily);
		data.setPreferNotesTimeDates(!convertNotesTimeDateToCalendar);
		data.m_totalBufferLength = totalBufferLength;
		data.m_itemsCount = itemsCount;

		Pointer itemValuePtr = bufferPtr.share(bufferPos);
		populateItemValueTableData(itemValuePtr, itemsCount, itemNameLengths, itemValueLengths, data,
				convertStringsLazily, convertNotesTimeDateToCalendar, decodeAllValues);

		return data;
	}

	/**
	 * This utility method extracts the item values from the buffer
	 * 
	 * @param bufferPtr buffer pointer
	 * @param itemsCount number of items in the buffer
	 * @param itemValueLengths lengths of the item values
	 * @param retData data object to populate
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param decodeAllValues true to decode all values in the buffer
	 */
	private static void populateItemValueTableData(Pointer bufferPtr, int itemsCount,
			int[] itemNameLengths, int[] itemValueLengths, ItemValueTableDataImpl retData, boolean convertStringsLazily,
			boolean convertNotesTimeDateToCalendar, boolean decodeAllValues) {
		int bufferPos = 0;
		String[] itemNames = new String[itemsCount];
		int[] itemDataTypes = new int[itemsCount];
		Pointer[] itemValueBufferPointers = new Pointer[itemsCount];
		int[] itemValueBufferSizes = new int[itemsCount];
		Object[] decodedItemValues = new Object[itemsCount];
		
		for (int j=0; j<itemsCount; j++) {
			if (itemNameLengths!=null && itemNameLengths[j]>0) {
				itemNames[j] = NotesStringUtils.fromLMBCS(bufferPtr.share(bufferPos), itemNameLengths[j]);
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
				itemDataTypes[j] = (int) (bufferPtr.getShort(bufferPos) & 0xffff);
				
				//add data type size to position
				bufferPos += 2;
				
				//read item values
				itemValueBufferPointers[j] = bufferPtr.share(bufferPos);
				itemValueBufferSizes[j] = itemValueLengths[j] - 2;
				
				//skip item value
				bufferPos += (itemValueLengths[j] - 2);

				if (decodeAllValues) {
					if (itemDataTypes[j] == NotesItem.TYPE_TEXT) {
						Object strVal = ItemDecoder.decodeTextValue(itemValueBufferPointers[j], (int) (itemValueBufferSizes[j] & 0xffff), convertStringsLazily);
						decodedItemValues[j] = strVal;
					}
					else if (itemDataTypes[j] == NotesItem.TYPE_TEXT_LIST) {
						//read a text list item value
						int valueLength = (int) (itemValueBufferSizes[j] & 0xffff);
						List<Object> listValues = valueLength==0 ? Collections.emptyList() : ItemDecoder.decodeTextListValue(itemValueBufferPointers[j], convertStringsLazily);
						decodedItemValues[j]  = listValues;
					}
					else if (itemDataTypes[j] == NotesItem.TYPE_NUMBER) {
						double numVal = ItemDecoder.decodeNumber(itemValueBufferPointers[j], (int) (itemValueBufferSizes[j] & 0xffff));
						decodedItemValues[j] = numVal;
					}
					else if (itemDataTypes[j] == NotesItem.TYPE_TIME) {
						if (convertNotesTimeDateToCalendar) {
							Calendar cal = ItemDecoder.decodeTimeDate(itemValueBufferPointers[j], (int) (itemValueBufferSizes[j] & 0xffff));
							decodedItemValues[j]  = cal;
						}
						else {
							NotesTimeDate td = ItemDecoder.decodeTimeDateAsNotesTimeDate(itemValueBufferPointers[j], (int) (itemValueBufferSizes[j] & 0xffff));
							decodedItemValues[j]  = td;
						}
					}
					else if (itemDataTypes[j] == NotesItem.TYPE_NUMBER_RANGE) {
						List<Object> numberList = ItemDecoder.decodeNumberList(itemValueBufferPointers[j], (int) (itemValueBufferSizes[j] & 0xffff));
						decodedItemValues[j]  = numberList;
					}
					else if (itemDataTypes[j] == NotesItem.TYPE_TIME_RANGE) {
						List<Object> calendarValues;
						if (convertNotesTimeDateToCalendar) {
							calendarValues = ItemDecoder.decodeTimeDateList(itemValueBufferPointers[j]);
						}
						else {
							calendarValues = ItemDecoder.decodeTimeDateListAsNotesTimeDate(itemValueBufferPointers[j]);
						}
						decodedItemValues[j] = calendarValues;
					}
				}
			}
		}
		
		retData.m_itemValueBufferPointers = itemValueBufferPointers;
		retData.m_itemValueBufferSizes = itemValueBufferSizes;
		retData.m_itemValues = decodedItemValues;
		retData.m_itemDataTypes = itemDataTypes;
		retData.m_itemValueLengthsInBytes = itemValueLengths;
		
		if (retData instanceof ItemTableDataImpl) {
			((ItemTableDataImpl)retData).m_itemNames = itemNames;
		}
	}

	/**
	 * Decodes an ITEM_TABLE structure with item names and item values
	 * 
	 * @param bufferPtr pointer to a buffer
	 * @param convertStringsLazily true to delay string conversion until the first use
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @param decodeAllValues true to decode all values in the buffer
	 * @return data
	 */
	public static IItemTableData decodeItemTable(Pointer bufferPtr,
			boolean convertStringsLazily, boolean convertNotesTimeDateToCalendar, boolean decodeAllValues) {
		int bufferPos = 0;
		NotesItemTableStruct itemTable = NotesItemTableStruct.newInstance(bufferPtr);
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
		int[] itemValueLengths = new int[itemsCount];
		int[] itemNameLengths = new int[itemsCount];
		
		//read ITEM structures for each item
		for (int j=0; j<itemsCount; j++) {
			Pointer itemPtr = bufferPtr.share(bufferPos);
			itemNameLengths[j] = (int) (itemPtr.getShort(0) & 0xffff);
			itemValueLengths[j] = (int) (itemPtr.share(2).getShort(0) & 0xffff);
			
			bufferPos += NotesConstants.tableItemSize;
		}
		
		ItemTableDataImpl data = new ItemTableDataImpl(convertStringsLazily);
		data.setPreferNotesTimeDates(!convertNotesTimeDateToCalendar);
		data.m_totalBufferLength = itemTable.getLengthAsInt();
		data.m_itemsCount = itemsCount;
		
		Pointer itemValuePtr = bufferPtr.share(bufferPos);
		populateItemValueTableData(itemValuePtr, itemsCount, itemNameLengths, itemValueLengths,
				data, convertStringsLazily, convertNotesTimeDateToCalendar, decodeAllValues);
		
		return data;
	}

	/**
	 * Container class for the data parsed from an ITEM_VALUE_TABLE structure
	 * 
	 * @author Karsten Lehmann
	 */
	private static class ItemValueTableDataImpl implements IItemValueTableData {
		protected Pointer[] m_itemValueBufferPointers;
		protected int[] m_itemValueBufferSizes;
		protected Object[] m_itemValues;
		protected int[] m_itemDataTypes;
		protected int m_totalBufferLength;
		protected int m_itemsCount;
		protected int[] m_itemValueLengthsInBytes;
		protected boolean m_convertStringsLazily;
		protected boolean m_freed;
		private Boolean m_preferNotesTimeDates;
		
		public ItemValueTableDataImpl(boolean convertStringsLazily) {
			m_convertStringsLazily = convertStringsLazily;
		}
		
		public void free() {
			m_freed = true;
		}
		
		public boolean isFreed() {
			return m_freed;
		}
		
		@Override
		public void setPreferNotesTimeDates(boolean b) {
			m_preferNotesTimeDates = b;
		}
		
		@Override
		public boolean isPreferNotesTimeDates() {
			if (m_preferNotesTimeDates==null) {
				return NotesGC.isPreferNotesTimeDate();
			}
			return m_preferNotesTimeDates;
		}

		@Override
		public Object getItemValue(int index) {
			int type = getItemDataType(index);
			
			if (m_itemValues[index] == null) {
				if (isFreed())
					throw new NotesError(0, "Buffer already freed");
				
				if (type == NotesItem.TYPE_TEXT) {
					m_itemValues[index] = ItemDecoder.decodeTextValue(m_itemValueBufferPointers[index], (int) (m_itemValueBufferSizes[index] & 0xffff), m_convertStringsLazily);
				}
				else if (type == NotesItem.TYPE_TEXT_LIST) {
					//read a text list item value
					int valueLength = (int) (m_itemValueBufferSizes[index] & 0xffff);
					m_itemValues[index] = valueLength==0 ? Collections.emptyList() : ItemDecoder.decodeTextListValue(m_itemValueBufferPointers[index], m_convertStringsLazily);
				}
				else if (type == NotesItem.TYPE_NUMBER) {
					m_itemValues[index] = ItemDecoder.decodeNumber(m_itemValueBufferPointers[index], (int) (m_itemValueBufferSizes[index] & 0xffff));
				}
				else if (type == NotesItem.TYPE_TIME) {
					//we always store NotesTimeDate and convert to Calendar if requested by caller
					m_itemValues[index] = ItemDecoder.decodeTimeDateAsNotesTimeDate(m_itemValueBufferPointers[index], (int) (m_itemValueBufferSizes[index] & 0xffff));
				}
				else if (type == NotesItem.TYPE_NUMBER_RANGE) {
					m_itemValues[index] = ItemDecoder.decodeNumberList(m_itemValueBufferPointers[index], (int) (m_itemValueBufferSizes[index] & 0xffff));
				}
				else if (type == NotesItem.TYPE_TIME_RANGE) {
					//we always store a List of NotesTimeDate and convert to Calendar if requested by caller
					m_itemValues[index] = ItemDecoder.decodeTimeDateListAsNotesTimeDate(m_itemValueBufferPointers[index]);
				}
			}
			
			if (type == NotesItem.TYPE_TIME && !isPreferNotesTimeDates()) {
				if (m_itemValues[index] instanceof NotesTimeDate) {
					return ((NotesTimeDate)m_itemValues[index]).toCalendar();
				}
				else if (m_itemValues[index] instanceof NotesTimeDate[]) {
					NotesTimeDate[] range = (NotesTimeDate[]) m_itemValues[index];
					Calendar[] convertedRange = new Calendar[] {range[0].toCalendar(), range[1].toCalendar()};
					return convertedRange;
				}
				else {
					//should not happen
					return m_itemValues[index];
				}
			}
			else if (type == NotesItem.TYPE_TIME_RANGE && m_itemValues[index] instanceof List && !isPreferNotesTimeDates()) {
				List<Object> tdList = (List<Object>) m_itemValues[index];
				
				List<Object> calList = new ArrayList<Object>(tdList.size());
				
				for (int i=0; i<tdList.size(); i++) {
					if (tdList.get(i) instanceof NotesTimeDate) {
						calList.add(((NotesTimeDate) tdList.get(i)).toCalendar());
					}
					else if (tdList.get(i) instanceof NotesDateRange) {
						NotesDateRange range = (NotesDateRange) tdList.get(i);
						Calendar[] convertedRange = new Calendar[] {range.getStartDateTime().toCalendar(), range.getEndDateTime().toCalendar()};
						calList.add(convertedRange);
					}
					else if (tdList.get(i) instanceof NotesTimeDate[]) {
						NotesTimeDate[] range = (NotesTimeDate[]) tdList.get(i);
						Calendar[] convertedRange = new Calendar[] {range[0].toCalendar(), range[1].toCalendar()};
						calList.add(convertedRange);
					}
					else {
						//should not happen
						calList.add(tdList.get(i));
					}
				}
				
				return calList;
			}
			else
				return m_itemValues[index];
		}
		
		@Override
		public int getItemDataType(int index) {
			return m_itemDataTypes[index];
		}
		
		/**
		 * Returns the total length of the summary buffer
		 * 
		 * @return length
		 */
		public int getTotalBufferLength() {
			return m_totalBufferLength;
		}
		
		@Override
		public int getItemsCount() {
			return m_itemsCount;
		}
		
		/**
		 * Returns the lengths of the encoded item values in bytes, e.g. for of each column
		 * in a collection (for {@link ReadMask#SUMMARYVALUES}) or for the summary buffer items
		 * returned for {@link ReadMask#SUMMARY}.
		 * 
		 * @return lengths
		 */
		public int[] getItemValueLengthsInBytes() {
			return m_itemValueLengthsInBytes;
		}
	}
	
	/**
	 * Container class for the data parsed from an ITEM_VALUE structure
	 * 
	 * @author Karsten Lehmann
	 */
	private static class ItemTableDataImpl extends ItemValueTableDataImpl implements IItemTableData {
		protected String[] m_itemNames;
		private ItemValueTableDataImpl m_wrappedValueTable;
		private Map<String,Boolean> m_itemExistence;
		private TypedItemAccess m_typedItems;
		
		public ItemTableDataImpl(String[] itemNames, ItemValueTableDataImpl valueTable) {
			super(valueTable.m_convertStringsLazily);
			
			m_itemNames = itemNames;
			
			m_wrappedValueTable = valueTable;
			m_itemValueBufferPointers = valueTable.m_itemValueBufferPointers;
			m_itemValueBufferSizes = valueTable.m_itemValueBufferSizes;
			m_itemValues = valueTable.m_itemValues;
			m_itemDataTypes = valueTable.m_itemDataTypes;
			m_totalBufferLength = valueTable.m_totalBufferLength;
			m_itemsCount = valueTable.m_itemsCount;
			m_itemValueLengthsInBytes = valueTable.m_itemValueLengthsInBytes;
			
			m_typedItems = new TypedItemAccess() {
				
				@Override
				public Object get(String itemName) {
					return ItemTableDataImpl.this.get(itemName);
				}
			};
		}
		
		public ItemTableDataImpl(boolean convertStringsLazily) {
			super(convertStringsLazily);
		}
		
		@Override
		public boolean has(String itemName) {
			String itemNameLC = itemName.toLowerCase(Locale.ENGLISH);
			
			Boolean exists = null;
			if (m_itemExistence!=null) {
				exists = m_itemExistence.get(itemNameLC);
			}
			if (exists==null) {
				//hash the result in case we have some really frequent calls for the same item
				for (String currItem : m_itemNames) {
					if (currItem.equalsIgnoreCase(itemName)) {
						exists = Boolean.TRUE;
						break;
					}
				}
				if (exists==null)
					exists = Boolean.FALSE;
				
				if (m_itemExistence==null)
					m_itemExistence = new HashMap<String, Boolean>();
				m_itemExistence.put(itemNameLC, exists);
			}
			return exists;
		}
		
		@Override
		public String[] getItemNames() {
			return m_itemNames;
		}
		
		@Override
		public Object get(String itemName) {
			if (m_wrappedValueTable!=null && m_wrappedValueTable.isFreed()) {
				throw new NotesError(0, "Buffer already freed");
			}
			
			for (int i=0; i<m_itemNames.length; i++) {
				if (m_itemNames[i].equalsIgnoreCase(itemName)) {
					Object val = getItemValue(i);
					if (val instanceof LMBCSString) {
						return ((LMBCSString)val).getValue();
					}
					else if (val instanceof List) {
						List<Object> valAsList = (List<Object>) val;
						for (int j=0; j<valAsList.size(); j++) {
							Object currListValue = valAsList.get(j);
							
							if (currListValue instanceof LMBCSString) {
								valAsList.set(j, ((LMBCSString)currListValue).getValue());
							}
						}
						return valAsList;
					}
					else {
						return val;
					}
				}
			}
			return null;
		}
		
		@Override
		public String getAsString(String itemName, String defaultValue) {
			return m_typedItems.getAsString(itemName, defaultValue);
		}

		@Override
		public String getAsNameAbbreviated(String itemName) {
			return m_typedItems.getAsNameAbbreviated(itemName);
		}
		
		@Override
		public String getAsNameAbbreviated(String itemName, String defaultValue) {
			return m_typedItems.getAsNameAbbreviated(itemName, defaultValue);
		}

		@Override
		public List<String> getAsNamesListAbbreviated(String itemName) {
			return m_typedItems.getAsNamesListAbbreviated(itemName);
		}
		
		@Override
		public List<String> getAsNamesListAbbreviated(String itemName, List<String> defaultValue) {
			return m_typedItems.getAsNamesListAbbreviated(itemName, defaultValue);
		}
		
		@Override
		public List<String> getAsStringList(String itemName, List<String> defaultValue) {
			return m_typedItems.getAsStringList(itemName, defaultValue);
		}
		
		@Override
		public NotesTimeDate getAsTimeDate(String itemName, NotesTimeDate defaultValue) {
			boolean oldPrefTimeDate = isPreferNotesTimeDates();
			//prevent automatic conversion to Calendar
			setPreferNotesTimeDates(true);
			Object val;
			try {
				val = get(itemName);
			}
			finally {
				setPreferNotesTimeDates(oldPrefTimeDate);
			}
			if (val instanceof NotesTimeDate) {
				return (NotesTimeDate) val;
			}
			else if (val instanceof List) {
				List<?> valAsList = (List<?>) val;
				if (!valAsList.isEmpty()) {
					Object firstVal = valAsList.get(0);
					if (firstVal instanceof NotesTimeDate) {
						return (NotesTimeDate) firstVal;
					}
				}
			}
			return defaultValue;
		}
		
		@Override
		public List<NotesTimeDate> getAsTimeDateList(String itemName, List<NotesTimeDate> defaultValue) {
			boolean oldPrefTimeDate = isPreferNotesTimeDates();
			//prevent automatic conversion to Calendar
			setPreferNotesTimeDates(true);
			Object val;
			try {
				val = get(itemName);
			}
			finally {
				setPreferNotesTimeDates(oldPrefTimeDate);
			}
			if (val instanceof NotesTimeDate) {
				return Arrays.asList((NotesTimeDate) val);
			}
			else if (val instanceof List) {
				List<?> valAsList = (List<?>) val;
				boolean supportedType=true;
				
				for (int i=0; i<valAsList.size(); i++) {
					if (valAsList.get(i) instanceof NotesTimeDate) {
						//ok
					}
					else {
						supportedType = false;
						break;
					}
				}
				
				if (supportedType) {
					return (List<NotesTimeDate>) valAsList;
				}
				else {
					return defaultValue;
				}
			}
			return defaultValue;
		}
		
		@Override
		public Calendar getAsCalendar(String itemName, Calendar defaultValue) {
			Object val = get(itemName);
			if (val instanceof Calendar) {
				return (Calendar) val;
			}
			else if (val instanceof NotesTimeDate) {
				return ((NotesTimeDate) val).toCalendar();
			}
			else if (val instanceof List) {
				List<?> valAsList = (List<?>) val;
				if (!valAsList.isEmpty()) {
					Object firstVal = valAsList.get(0);
					if (firstVal instanceof Calendar) {
						return (Calendar) firstVal;
					}
					else if (firstVal instanceof NotesTimeDate) {
						return ((NotesTimeDate) firstVal).toCalendar();
					}
				}
			}
			return defaultValue;
		}

		@Override
		public List<Calendar> getAsCalendarList(String itemName, List<Calendar> defaultValue) {
			Object val = get(itemName);
			if (val instanceof Calendar) {
				return Arrays.asList((Calendar) val);
			}
			else if (val instanceof NotesTimeDate) {
				return Arrays.asList(((NotesTimeDate) val).toCalendar());
			}
			else if (val instanceof List) {
				List<?> valAsList = (List<?>) val;
				boolean supportedType=true;
				boolean conversionRequired=false;
				
				for (int i=0; i<valAsList.size(); i++) {
					if (valAsList.get(i) instanceof Calendar) {
						//ok
					}
					else if (valAsList.get(i) instanceof NotesTimeDate) {
						conversionRequired = true;
					}
					else {
						supportedType = false;
						break;
					}
				}
				
				if (supportedType) {
					if (conversionRequired) {
						List<Calendar> calList = new ArrayList<Calendar>(valAsList.size());
						for (int i=0; i<valAsList.size(); i++) {
							if (valAsList.get(i) instanceof Calendar) {
								calList.add((Calendar) valAsList.get(i));
							}
							else if (valAsList.get(i) instanceof NotesTimeDate) {
								calList.add(((NotesTimeDate) valAsList.get(i)).toCalendar());
							}
						}
						return calList;
					}
					else {
						return (List<Calendar>) valAsList;
					}
				}
				else {
					return defaultValue;
				}
			}
			return defaultValue;
		}
		
		@Override
		public Double getAsDouble(String itemName, Double defaultValue) {
			return m_typedItems.getAsDouble(itemName, defaultValue);
		}

		@Override
		public Integer getAsInteger(String itemName, Integer defaultValue) {
			return m_typedItems.getAsInteger(itemName, defaultValue);
		}

		@Override
		public List<Double> getAsDoubleList(String itemName, List<Double> defaultValue) {
			return m_typedItems.getAsDoubleList(itemName, defaultValue);
		}

		@Override
		public List<Integer> getAsIntegerList(String itemName, List<Integer> defaultValue) {
			return m_typedItems.getAsIntegerList(itemName, defaultValue);
		}

		@Override
		public Map<String,Object> asMap() {
			return asMap(true);
		}
		
		@Override
		public Map<String,Object> asMap(boolean decodeLMBCS) {
			Map<String,Object> data = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
			int itemCount = getItemsCount();
			for (int i=0; i<itemCount; i++) {
				Object val = getItemValue(i);
				
				if (val instanceof LMBCSString) {
					if (decodeLMBCS) {
						data.put(m_itemNames[i], ((LMBCSString)val).getValue());
					}
					else {
						data.put(m_itemNames[i], val);
					}
				}
				else if(!isPreferNotesTimeDates() && val instanceof NotesTimeDate) {
					data.put(m_itemNames[i], ((NotesTimeDate)val).toCalendar());
				}
				else if (val instanceof List) {
					if (decodeLMBCS) {
						//check for LMBCS strings and NotesTimeDate
						List valAsList = (List) val;
						boolean hasLMBCS = false;
						boolean hasTimeDate = false;
						
						for (int j=0; j<valAsList.size(); j++) {
							if (valAsList.get(j) instanceof LMBCSString) {
								hasLMBCS = true;
								break;
							}
							else if (!isPreferNotesTimeDates() && valAsList.get(j) instanceof NotesTimeDate) {
								hasTimeDate = true;
								break;
							}
						}
						
						if (hasLMBCS || hasTimeDate) {
							List<Object> convList = new ArrayList<Object>(valAsList.size());
							for (int j=0; j<valAsList.size(); j++) {
								Object currObj = valAsList.get(j);
								if (currObj instanceof LMBCSString) {
									convList.add(((LMBCSString)currObj).getValue());
								}
								else if (!isPreferNotesTimeDates() && currObj instanceof NotesTimeDate) {
									convList.add(((NotesTimeDate)currObj).toCalendar());
								}
								else {
									convList.add(currObj);
								}
							}
							data.put(m_itemNames[i], convList);
						}
						else {
							data.put(m_itemNames[i], val);
						}
					}
					else {
						data.put(m_itemNames[i], val);
					}
				}
				else {
					data.put(m_itemNames[i], val);
				}
			}
			return data;
		}
	}
	
}
