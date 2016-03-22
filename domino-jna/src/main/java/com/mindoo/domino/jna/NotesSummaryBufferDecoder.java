package com.mindoo.domino.jna;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesCollectionStats;
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
 * Utility class to decode the summery buffer of collection entries and retrieve column data
 * and additional information like the row's type and note id
 * 
 * @author Karsten Lehmann
 */
public class NotesSummaryBufferDecoder {
	
	
	/**
	 * Decodes the buffer, 32 bit mode
	 * 
	 * @param bufferHandle buffer handle
	 * @param numEntriesSkipped entries skipped during collection scan
	 * @param numEntriesReturned entries read during collection scan
	 * @param returnMask bitmask used to fill the buffer with data
	 * @param signalFlags signal flags returned by NIFReadEntries, e.g. whether we have more data to read
	 * @param columnsToDecode optional array of columns to decode or null
	 * @return collection data
	 */
	public static NotesViewData b32_decodeBuffer(int bufferHandle, int numEntriesSkipped, int numEntriesReturned,
			int returnMask, short signalFlags, boolean[] columnsToDecode) {
		return b64_decodeBuffer(bufferHandle, numEntriesSkipped, numEntriesReturned, returnMask, signalFlags, columnsToDecode);
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
	 * @return collection data
	 */
	public static NotesViewData b64_decodeBuffer(long bufferHandle, int numEntriesSkipped, int numEntriesReturned,
			int returnMask, short signalFlags, boolean[] columnsToDecode) {
		if ((returnMask & NotesCAPI.READ_MASK_SUMMARY) == NotesCAPI.READ_MASK_SUMMARY) {
			throw new UnsupportedOperationException("Mode READ_MASK_SUMMARY is not supported yet");
		}
		
		NotesCAPI notesAPI = NotesContext.getNotesAPI();

		Pointer bufferPtr;
		if (NotesContext.is64Bit()) {
			bufferPtr = notesAPI.b64_OSLockObject(bufferHandle);
		}
		else {
			bufferPtr = notesAPI.b32_OSLockObject((int) bufferHandle);
		}

		int bufferPos = 0;
		
		NotesCollectionStats collectionStats = null;
		
		//compute structure sizes
		
		try {
			if ((returnMask & NotesCAPI.READ_MASK_COLLECTIONSTATS)==NotesCAPI.READ_MASK_COLLECTIONSTATS) {
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
			if ((returnMask & NotesCAPI.READ_MASK_INDEXPOSITION) == NotesCAPI.READ_MASK_INDEXPOSITION) {
				//allocate memory for a position
				sharedCollectionPositionMem = new Memory(NotesCAPI.collectionPositionSize);
				sharedPosition = new NotesCollectionPosition(sharedCollectionPositionMem);
			}
			
			for (int i=0; i<numEntriesReturned; i++) {
				NotesViewEntryData newData = new NotesViewEntryData();
				viewEntries.add(newData);
				
				if ((returnMask & NotesCAPI.READ_MASK_NOTEID) == NotesCAPI.READ_MASK_NOTEID) {
					ByteBuffer noteIdBuf = bufferPtr.getByteBuffer(bufferPos, 4);
					IntBuffer noteIdBufAsInt = noteIdBuf.asIntBuffer();

					int entryNoteId = noteIdBufAsInt.get(0);
					newData.setNoteId(entryNoteId);
					
					bufferPos+=4;
				}
				
				if ((returnMask & NotesCAPI.READ_MASK_NOTEUNID) == NotesCAPI.READ_MASK_NOTEUNID) {
					ByteBuffer unidBytes = bufferPtr.getByteBuffer(bufferPos, 16);
					String unid = NotesDatabase.toUNID(unidBytes);
					newData.setUNID(unid);
					
					bufferPos+=16;
				}
				if ((returnMask & NotesCAPI.READ_MASK_NOTECLASS) == NotesCAPI.READ_MASK_NOTECLASS) {
					short noteClass = bufferPtr.getShort(bufferPos);
					newData.setNoteClass(noteClass);
					
					bufferPos+=2;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDEXSIBLINGS) == NotesCAPI.READ_MASK_INDEXSIBLINGS) {
					int siblingCount = bufferPtr.getInt(bufferPos);
					newData.setSiblingCount(siblingCount);
					
					bufferPos+=4;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDEXCHILDREN) == NotesCAPI.READ_MASK_INDEXCHILDREN) {
					int childCount = bufferPtr.getInt(bufferPos);
					newData.setChildCount(childCount);
					
					bufferPos+=4;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDEXDESCENDANTS) == NotesCAPI.READ_MASK_INDEXDESCENDANTS) {
					int descendantCount = bufferPtr.getInt(bufferPos);
					newData.setDescendantCount(descendantCount);
					
					bufferPos+=4;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDEXANYUNREAD) == NotesCAPI.READ_MASK_INDEXANYUNREAD) {
					boolean isAnyUnread = bufferPtr.getShort(bufferPos) == 1;
					newData.setAnyUnread(isAnyUnread);
					
					bufferPos+=2;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDENTLEVELS) == NotesCAPI.READ_MASK_INDENTLEVELS) {
					short indentLevels = bufferPtr.getShort(bufferPos);
					newData.setIndentLevels(indentLevels);
					
					bufferPos += 2;
				}
				if ((returnMask & NotesCAPI.READ_MASK_SCORE) == NotesCAPI.READ_MASK_SCORE) {
					short score = bufferPtr.getShort(bufferPos);
					newData.setFTScore(score);
					
					bufferPos += 2;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDEXUNREAD) == NotesCAPI.READ_MASK_INDEXUNREAD) {
					boolean isUnread = bufferPtr.getShort(bufferPos) == 1;
					newData.setUnread(isUnread);
					
					bufferPos+=2;
				}
				if ((returnMask & NotesCAPI.READ_MASK_INDEXPOSITION) == NotesCAPI.READ_MASK_INDEXPOSITION) {
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
				if ((returnMask & NotesCAPI.READ_MASK_SUMMARYVALUES) == NotesCAPI.READ_MASK_SUMMARYVALUES) {
					Pointer itemValuePtr = bufferPtr.share(bufferPos);
					
					int startBufferPosOfSummaryValues = bufferPos;
					NotesItemValueTable itemValueTable = new NotesItemValueTable(itemValuePtr);
					itemValueTable.read();
					
					//skip item value table header
					bufferPos += itemValueTable.size();
					
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
					
					int itemsCount = itemValueTable.getItemsAsInt();
					int[] itemLengths = new int[itemsCount];
					
					//read all item lengths
					for (int j=0; j<itemsCount; j++) {
						//convert USHORT to int without sign
						itemLengths[j] = bufferPtr.getShort(bufferPos) & 0xffff;
						bufferPos += 2;
					}
					
					int[] itemDataTypes = new int[itemsCount];
					Pointer[] itemValueBufferPointers = new Pointer[itemsCount];
					int[] itemValueBufferSizes = new int[itemsCount];
					Object[] decodedItemValues = new Object[itemsCount];
					
					for (int j=0; j<itemsCount; j++) {
						//check if this column should be decoded; can be used to only read specific view columns for performance reasons
						boolean decodeThisColumn = true;
						if (columnsToDecode!=null) {
							if (j < columnsToDecode.length) {
								decodeThisColumn = columnsToDecode[j];
							}
							else {
								decodeThisColumn = false;
							}
						}
						
						//read data type
						if (itemLengths[j] == 0) {
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
							itemValueBufferSizes[j] = itemLengths[j] - 2;
							
							//skip item value
							bufferPos += (itemLengths[j] - 2);
						
							if (decodeThisColumn) {
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
								}
							}
						}
					}
					
					if (bufferPos!=(startBufferPosOfSummaryValues + itemValueTable.getLengthAsInt())) {
//						System.err.println("Warning: Unexpected pointer value mismatch while reading the summary value table.");
					}
					
					//move to the end of the buffer
					bufferPos = startBufferPosOfSummaryValues + itemValueTable.getLengthAsInt();
					
					newData.setColumnValues(decodedItemValues);
				}
				if ((returnMask & NotesCAPI.READ_MASK_SUMMARY) == NotesCAPI.READ_MASK_SUMMARY) {
					//TODO implement decoding summary with item names if required
				}
			}
			
			return new NotesViewData(collectionStats, viewEntries, numEntriesSkipped, numEntriesReturned, signalFlags);
		}
		finally {
			if (NotesContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(bufferHandle);
				notesAPI.b64_OSMemFree(bufferHandle);
			}
			else {
				notesAPI.b32_OSUnlockObject((int)bufferHandle);
				notesAPI.b32_OSMemFree((int)bufferHandle);
			}
		}
		
	}

}
