package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesViewColumn;
import com.mindoo.domino.jna.NotesViewFormat;
import com.mindoo.domino.jna.structs.viewformat.NotesColorValueStruct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewColumnFormat2Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewColumnFormat3Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewColumnFormat4Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewColumnFormat5Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewColumnFormatStruct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewFormatHeaderStruct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormat2Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormat4Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormat5Struct;
import com.mindoo.domino.jna.structs.viewformat.NotesViewTableFormatStruct;
import com.mindoo.domino.jna.utils.DumpUtil;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Pointer;

public class ViewFormatDecoder {

	public static NotesViewFormat decodeViewFormat(Pointer dataPtr, int valueLength) {
		int valueLengthRemaining = valueLength;
		
		Pointer currPtr = dataPtr;
		NotesViewTableFormatStruct tableFormat1 = NotesViewTableFormatStruct.newInstance(currPtr);
		tableFormat1.read();
		
		NotesViewTableFormat2Struct tableFormat2 = null;
		
		//START: view table format 3 fields (not using struct because COLOR_VALUE has variable length)
//		short format3Length;
//		int format3Flags;
//		NotesColorValueStruct format3BackgroundColor;
//		NotesColorValueStruct format3BackgroundColor_Gradient;
//		NotesColorValueStruct format3AlternateBackgroundColor;
//		NotesColorValueStruct format3AlternateBackgroundColor_Gradient;
//		NotesColorValueStruct format3GridColor;
//		NotesColorValueStruct format3GridColor_Gradient;
//		short format3wViewMarginTop;
//		short format3wViewMarginLeft;
//		short format3wViewMarginRight;
//		short format3wViewMarginBottom;
//		NotesColorValueStruct format3MarginBackgroundColor;
//		NotesColorValueStruct format3MarginBackgroundColor_Gradient;
//		NotesColorValueStruct format3HeaderBackgroundColor;
//		NotesColorValueStruct format3HeaderBackgroundColor_Gradient;
//		short format3wViewMarginTopUnder;
//		NotesColorValueStruct format3UnreadColor;
//		NotesColorValueStruct format3UnreadColor_Gradient;
//		NotesColorValueStruct format3TotalsColor;
//		NotesColorValueStruct format3TotalsColor_Gradient;
//		short format3wMaxRows;
//		short format3wReserved;
//		int format3dwReserved;
		//END: view table format 3 fields
		
		NotesViewTableFormat4Struct tableFormat4 = null;
		NotesViewTableFormat5Struct tableFormat5 = null;
		
		Map<Integer,NotesViewColumnFormatStruct> columnsFormat1 = new HashMap<Integer,NotesViewColumnFormatStruct>();
		Map<Integer,NotesViewColumnFormat2Struct> columnsFormat2 = new HashMap<Integer,NotesViewColumnFormat2Struct>();
		Map<Integer,NotesViewColumnFormat3Struct> columnsFormat3 = new HashMap<Integer,NotesViewColumnFormat3Struct>();
		Map<Integer,NotesViewColumnFormat4Struct> columnsFormat4 = new HashMap<Integer,NotesViewColumnFormat4Struct>();
		Map<Integer,NotesViewColumnFormat5Struct> columnsFormat5 = new HashMap<Integer,NotesViewColumnFormat5Struct>();

		//read view format v1
		NotesViewFormatHeaderStruct header = tableFormat1.Header;
		
		int colCount = (int) (tableFormat1.Columns & 0xffff);
		
		currPtr = currPtr.share(NotesCAPI.notesViewTableFormatSize);
		valueLengthRemaining -= NotesCAPI.notesViewTableFormatSize;
		

		//read view column data v1
		for (int i=0; i<colCount; i++) {
			NotesViewColumnFormatStruct colFormat = NotesViewColumnFormatStruct.newInstance(currPtr);
			colFormat.read();
			
			if (NotesCAPI.VIEW_COLUMN_FORMAT_SIGNATURE != colFormat.Signature)
				throw new AssertionError("Signature of column #"+i+" with format v1 is not correct.\nMem dump:\n"+DumpUtil.dumpAsAscii(dataPtr, NotesCAPI.notesViewTableFormatSize + (colCount * NotesCAPI.notesViewColumnFormatSize)));
			
			columnsFormat1.put(i, colFormat);
			
			currPtr = currPtr.share(NotesCAPI.notesViewColumnFormatSize);
			valueLengthRemaining -= NotesCAPI.notesViewColumnFormatSize;
		}
		
		Map<Integer,String> columnItemNames = new HashMap<Integer, String>();
		Map<Integer,String> columnTitles = new HashMap<Integer, String>();
		Map<Integer,byte[]> columnFormula = new HashMap<Integer, byte[]>();
		Map<Integer,byte[]> columnConstantValue = new HashMap<Integer, byte[]>();

		//read item names v1
		for (int i=0; i<colCount; i++) {
			NotesViewColumnFormatStruct currCol = columnsFormat1.get(i);
			
			int currItemNameSize = (int) (currCol.ItemNameSize & 0xffff);
			int currTitleSize = (int) (currCol.TitleSize & 0xffff);
			int currFormulaSize = (int) (currCol.FormulaSize & 0xffff);
			int currConstantValueSize = (int) (currCol.ConstantValueSize & 0xffff);
			
			String currItemName = NotesStringUtils.fromLMBCS(currPtr, currItemNameSize);
			columnItemNames.put(i,  currItemName);
			currPtr = currPtr.share(currItemNameSize);
			valueLengthRemaining -= currItemNameSize;
			
			String currTitle = NotesStringUtils.fromLMBCS(currPtr, currTitleSize);
			columnTitles.put(i, currTitle);
			currPtr = currPtr.share(currTitleSize);
			valueLengthRemaining -= currTitleSize;
			
			byte[] currFormulaCompiled = currPtr.getByteArray(0, currFormulaSize);
			columnFormula.put(i, currFormulaCompiled);
			currPtr = currPtr.share(currFormulaSize);
			valueLengthRemaining -= currFormulaSize;
			
			byte[] currConstantValue = currPtr.getByteArray(0, currConstantValueSize);
			columnConstantValue.put(i, currConstantValue);
			currPtr = currPtr.share(currConstantValueSize);
			valueLengthRemaining -= currConstantValueSize;
		}
		
		if (valueLengthRemaining >= NotesCAPI.notesViewTableFormat2Size) {
			//read view format v2
			tableFormat2 = NotesViewTableFormat2Struct.newInstance(currPtr);
			tableFormat2.read();
			
			if (NotesCAPI.VALID_VIEW_FORMAT_SIG != tableFormat2.wSig)
				throw new AssertionError("Signature of view table format v2 is not correct.\nMem dump:\n"+DumpUtil.dumpAsAscii(dataPtr, NotesCAPI.notesViewTableFormatSize + (colCount * NotesCAPI.notesViewColumnFormatSize) + NotesCAPI.notesViewTableFormat2Size));
			
			int tableFormat2Size = (int) (tableFormat2.Length & 0xffff);
			currPtr = currPtr.share(tableFormat2Size);
			valueLengthRemaining -= tableFormat2Size;
			
			//commented out until we find out the right structure sizes for the rest;
			//looks like the C API documentation is not correct. In our test, we found 4
			//additional bytes in VIEW_COLUMN_FORMAT3 (34 instead of 30 bytes) and
			//the following 2 columns had a v3 signature, while the following column
			//had a v4 signature
			
			/*
			if (valueLengthRemaining >= (colCount * NotesCAPI.notesViewColumnFormat2Size)) {
				//read view column data v2
				for (int i=0; i<colCount; i++) {
					NotesViewColumnFormat2Struct colFormat = NotesViewColumnFormat2Struct.newInstance(currPtr);
					colFormat.read();
					
					Assert.assertEquals("Signature of column #"+i+" with format v2 is correct", NotesCAPI.VIEW_COLUMN_FORMAT_SIGNATURE2, colFormat.Signature);
					
					columnsFormat2.put(i, colFormat);
					
					currPtr = currPtr.share(NotesCAPI.notesViewColumnFormat2Size);
					valueLengthRemaining -= NotesCAPI.notesViewColumnFormat2Size;
				}
				
				if (valueLengthRemaining >= 2) {
					Pointer ptrBeforeFormat3 = currPtr;
					
					System.out.println("Reading v3 view table format");
					System.out.println(DumpUtil.dumpAsAscii(currPtr, 90));
					
					//read parts of format v3
					format3Length = currPtr.getShort(0);
					currPtr = currPtr.share(2);
					
					format3BackgroundColor = NotesColorValueStruct.newInstance(currPtr);
					format3BackgroundColor.read();
					currPtr = currPtr.share(format3BackgroundColor.size());
					
					if (hasGradient(format3BackgroundColor)) {
						format3BackgroundColor_Gradient = NotesColorValueStruct.newInstance(currPtr);
						format3BackgroundColor_Gradient.read();
						currPtr = currPtr.share(format3BackgroundColor_Gradient.size());
					}
					else {
						format3BackgroundColor_Gradient = null;
					}
					
					format3AlternateBackgroundColor = NotesColorValueStruct.newInstance(currPtr);
					format3AlternateBackgroundColor.read();
					currPtr = currPtr.share(format3AlternateBackgroundColor.size());
					
					if (hasGradient(format3AlternateBackgroundColor)) {
						format3AlternateBackgroundColor_Gradient = NotesColorValueStruct.newInstance(currPtr);
						format3AlternateBackgroundColor_Gradient.read();
						currPtr = currPtr.share(format3AlternateBackgroundColor_Gradient.size());
					}
					else {
						format3AlternateBackgroundColor_Gradient = null;
					}
					
					format3GridColor = NotesColorValueStruct.newInstance(currPtr);
					format3GridColor.read();
					currPtr = currPtr.share(format3GridColor.size());
					
					if (hasGradient(format3GridColor)) {
						format3GridColor_Gradient = NotesColorValueStruct.newInstance(currPtr);
						format3GridColor_Gradient.read();
						currPtr = currPtr.share(format3GridColor_Gradient.size());
					}
					else {
						format3GridColor_Gradient = null;
					}
					
					format3wViewMarginTop = currPtr.getShort(0);
					currPtr = currPtr.share(2);

					format3wViewMarginLeft = currPtr.getShort(0);
					currPtr = currPtr.share(2);

					format3wViewMarginRight = currPtr.getShort(0);
					currPtr = currPtr.share(2);

					format3wViewMarginBottom = currPtr.getShort(0);
					currPtr = currPtr.share(2);
					
					format3MarginBackgroundColor = NotesColorValueStruct.newInstance(currPtr);
					format3MarginBackgroundColor.read();
					currPtr = currPtr.share(format3MarginBackgroundColor.size());
					
					if (hasGradient(format3MarginBackgroundColor)) {
						format3MarginBackgroundColor_Gradient = NotesColorValueStruct.newInstance(currPtr);
						format3MarginBackgroundColor_Gradient.read();
						currPtr = currPtr.share(format3MarginBackgroundColor_Gradient.size());
					}
					else {
						format3MarginBackgroundColor_Gradient = null;
					}
					
					format3HeaderBackgroundColor = NotesColorValueStruct.newInstance(currPtr);
					format3HeaderBackgroundColor.read();
					currPtr = currPtr.share(format3HeaderBackgroundColor.size());
					
					if (hasGradient(format3HeaderBackgroundColor)) {
						format3HeaderBackgroundColor_Gradient = NotesColorValueStruct.newInstance(currPtr);
						format3HeaderBackgroundColor_Gradient.read();
						currPtr = currPtr.share(format3HeaderBackgroundColor_Gradient.size());
					}
					else {
						format3HeaderBackgroundColor_Gradient = null;
					}
					
					format3wViewMarginTopUnder = currPtr.getShort(0);
					currPtr = currPtr.share(2);
					
					format3UnreadColor = NotesColorValueStruct.newInstance(currPtr);
					format3UnreadColor.read();
					currPtr = currPtr.share(format3UnreadColor.size());
					
					if (hasGradient(format3UnreadColor)) {
						format3UnreadColor_Gradient = NotesColorValueStruct.newInstance(currPtr);
						format3UnreadColor_Gradient.read();
						currPtr = currPtr.share(format3UnreadColor_Gradient.size());
					}
					else {
						format3UnreadColor_Gradient = null;
					}
					
					format3TotalsColor = NotesColorValueStruct.newInstance();
					format3TotalsColor.read();
					currPtr = currPtr.share(format3TotalsColor.size());
					
					if (hasGradient(format3TotalsColor)) {
						format3TotalsColor_Gradient = NotesColorValueStruct.newInstance();
						format3TotalsColor_Gradient.read();
						currPtr = currPtr.share(format3TotalsColor_Gradient.size());
					}
					else {
						format3TotalsColor_Gradient = null;
					}
					
					format3wMaxRows = currPtr.getShort(0);
					currPtr = currPtr.share(2);
					
					format3wReserved = currPtr.getShort(0);
					currPtr = currPtr.share(2);
					
					format3dwReserved = currPtr.getInt(0);
					currPtr = currPtr.share(4);
					
					valueLengthRemaining -= (Pointer.nativeValue(currPtr) - Pointer.nativeValue(ptrBeforeFormat3));
					
					if (valueLengthRemaining >= (colCount*NotesCAPI.notesViewColumnFormat3Size)) {
						//read view column data v3
						for (int i=0; i<colCount; i++) {
							System.out.println("Reading c3 column "+i);
							System.out.println(DumpUtil.dumpAsAscii(currPtr, NotesCAPI.notesViewColumnFormat3Size+10));
							NotesViewColumnFormat3Struct colFormat = NotesViewColumnFormat3Struct.newInstance(currPtr);
							colFormat.read();
							
							Assert.assertEquals("Signature of column #"+i+" with format v3 is correct", NotesCAPI.VIEW_COLUMN_FORMAT_SIGNATURE3, colFormat.Signature);
							
							columnsFormat3.put(i, colFormat);
							
							//TODO find out where these 4 bytes come from
							currPtr = currPtr.share(NotesCAPI.notesViewColumnFormat3Size + 4);
							valueLengthRemaining -= (NotesCAPI.notesViewColumnFormat3Size + 4);
						}

						if (valueLengthRemaining >= NotesCAPI.notesViewTableFormat4Size) {
							//read view format v4
							tableFormat4 = NotesViewTableFormat4Struct.newInstance(currPtr);
							tableFormat4.read();

							int tableFormat4Size = (int) (tableFormat4.Length & 0xffff);
							currPtr = currPtr.share(tableFormat4Size);
							valueLengthRemaining -= tableFormat4Size;
							
							if (valueLengthRemaining >= (colCount*NotesCAPI.notesViewColumnFormat4Size)) {
								//read view column data v4
								for (int i=0; i<colCount; i++) {
									NotesViewColumnFormat4Struct colFormat = NotesViewColumnFormat4Struct.newInstance(currPtr);
									colFormat.read();
									
									Assert.assertEquals("Signature of column #"+i+" with format v4 is correct", NotesCAPI.VIEW_COLUMN_FORMAT_SIGNATURE4, colFormat.Signature);
									
									columnsFormat4.put(i, colFormat);
									
									currPtr = currPtr.share(NotesCAPI.notesViewColumnFormat4Size);
									valueLengthRemaining -= NotesCAPI.notesViewColumnFormat4Size;
								}

								if (valueLengthRemaining >= NotesCAPI.notesViewTableFormat5Size) {
									//read view format v5
									tableFormat5 = NotesViewTableFormat5Struct.newInstance(currPtr);
									tableFormat5.read();

									int tableFormat5Size = (int) (tableFormat5.Length & 0xffff);
									currPtr = currPtr.share(tableFormat5Size);
									valueLengthRemaining -= tableFormat5Size;
									
									if (valueLengthRemaining >= (colCount*NotesCAPI.notesViewColumnFormat5Size)) {
										//read view column data v5
										for (int i=0; i<colCount; i++) {
											NotesViewColumnFormat5Struct colFormat = NotesViewColumnFormat5Struct.newInstance(currPtr);
											colFormat.read();
											
											Assert.assertEquals("Signature of column #"+i+" with format v5 is correct", NotesCAPI.VIEW_COLUMN_FORMAT_SIGNATURE5, colFormat.Signature);
											
											columnsFormat5.put(i, colFormat);
											
											currPtr = currPtr.share(NotesCAPI.notesViewColumnFormat5Size);
											valueLengthRemaining -= NotesCAPI.notesViewColumnFormat5Size;
										}		
									}
								}
							}
						}
					}
				}
			}
			*/
		}
		
		List<NotesViewColumn> columns = new ArrayList<NotesViewColumn>();
		NotesViewFormat viewFormat = new NotesViewFormat(new ViewFormatAdaptable(tableFormat1,
				tableFormat2,
				tableFormat4, tableFormat5), columns);
		
		int currColValuesIndex = 0;
		
		for (int i=0; i<colCount; i++) {
			NotesViewColumnFormatStruct currFormat1 = columnsFormat1.get(i);
			NotesViewColumnFormat2Struct currFormat2 = columnsFormat2.get(i);
			NotesViewColumnFormat3Struct currFormat3 = columnsFormat3.get(i);
			NotesViewColumnFormat4Struct currFormat4 = columnsFormat4.get(i);
			NotesViewColumnFormat5Struct currFormat5 = columnsFormat5.get(i);

			String currItemName = columnItemNames.get(i);
			String currTitle = columnTitles.get(i);
			byte[] currFormulaCompiled = columnFormula.get(i);
			byte[] currConstantValue = columnConstantValue.get(i);
			
			int colValuesIndex = (currConstantValue!=null && currConstantValue.length>0) ? 65535 : (currColValuesIndex++);
			
			NotesViewColumn newColumn = new NotesViewColumn(viewFormat, 
					currItemName,
					currTitle,
					currFormulaCompiled,
					currConstantValue,
					i+1,
					colValuesIndex,
					new ViewColumnDataAdaptable(
					currFormat1, currFormat2,
					currFormat3, currFormat4,
					currFormat5));
			
			columns.add(newColumn);
		}
		return viewFormat;
	}

	private static boolean hasGradient(NotesColorValueStruct color) {
		return (color.Flags & NotesCAPI.COLOR_VALUE_FLAGS_HASGRADIENT) == NotesCAPI.COLOR_VALUE_FLAGS_HASGRADIENT;
	}

	private static class ViewFormatAdaptable implements IAdaptable {
		private NotesViewTableFormatStruct m_format;
		private NotesViewTableFormat2Struct m_format2;
		private NotesViewTableFormat4Struct m_format4;
		private NotesViewTableFormat5Struct m_format5;
		
		public ViewFormatAdaptable(NotesViewTableFormatStruct format, NotesViewTableFormat2Struct format2,
				NotesViewTableFormat4Struct format4, NotesViewTableFormat5Struct format5) {
			m_format = format;
			m_format2 = format2;
			m_format4 = format4;
			m_format5 = format5;
		}

		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (clazz == NotesViewTableFormatStruct.class) {
				return (T) m_format;
			}
			else if (clazz == NotesViewTableFormat2Struct.class) {
				return (T) m_format2;
			}
			else if (clazz == NotesViewTableFormat4Struct.class) {
				return (T) m_format4;
			}
			else if (clazz == NotesViewTableFormat5Struct.class) {
				return (T) m_format5;
			}
			else {
				return null;
			}
		}
	}
	
	private static class ViewColumnDataAdaptable implements IAdaptable {
		private NotesViewColumnFormatStruct m_format;
		private NotesViewColumnFormat2Struct m_format2;
		private NotesViewColumnFormat3Struct m_format3;
		private NotesViewColumnFormat4Struct m_format4;
		private NotesViewColumnFormat5Struct m_format5;
		
		public ViewColumnDataAdaptable(NotesViewColumnFormatStruct format, NotesViewColumnFormat2Struct format2,
				NotesViewColumnFormat3Struct format3, NotesViewColumnFormat4Struct format4,
				NotesViewColumnFormat5Struct format5) {

			m_format = format;
			m_format2 = format2;
			m_format3 = format3;
			m_format4 = format4;
			m_format5 = format5;
		}
		
		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (clazz == NotesViewColumnFormatStruct.class) {
				return (T) m_format;
			}
			else if (clazz == NotesViewColumnFormat2Struct.class) {
				return (T) m_format2;
			}
			else if (clazz == NotesViewColumnFormat3Struct.class) {
				return (T) m_format3;
			}
			else if (clazz == NotesViewColumnFormat4Struct.class) {
				return (T) m_format4;
			}
			else if (clazz == NotesViewColumnFormat5Struct.class) {
				return (T) m_format5;
			}
			else {
				return null;
			}
		}
		
	}
}
