package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.NotesCollateDescriptor;
import com.mindoo.domino.jna.NotesCollationInfo;
import com.mindoo.domino.jna.constants.CollateType;
import com.mindoo.domino.jna.structs.collation.NotesCollateDescriptorStruct;
import com.mindoo.domino.jna.structs.collation.NotesCollationStruct;
import com.mindoo.domino.jna.utils.DumpUtil;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Pointer;

/**
 * Utility class to decode the COLLATION and COLLATE_DESCRIPTOR data structures from
 * view note items of type TYPE_COLLATION (e.g. $Collation, $Collation1, $Collation2 etc.).
 * 
 * @author Karsten Lehmann
 */
public class CollationDecoder {

	/**
	 * Decodes the item value. Extracted data is returned as {@link NotesCollationInfo} object
	 * 
	 * @param dataPtr item value pointer
	 * @return collation info
	 */
	public static NotesCollationInfo decodeCollation(Pointer dataPtr) {
		NotesCollationStruct collationStruct = NotesCollationStruct.newInstance(dataPtr);
		collationStruct.read();

		//sanity check that the signature byte is at the right position
		if (NotesCAPI.COLLATION_SIGNATURE != collationStruct.signature)
			throw new AssertionError("Collation signature byte is not correct.\nMem dump:\n"+DumpUtil.dumpAsAscii(dataPtr, NotesCAPI.notesCollationSize));
		
		List<NotesCollateDescriptor> collateDescriptors = new ArrayList<NotesCollateDescriptor>();
		int items = (int) (collationStruct.Items & 0xffff);
		
		long baseOffsetDescriptors = NotesCAPI.notesCollationSize;
		long baseOffsetTextBuffer = baseOffsetDescriptors + (items * NotesCAPI.notesCollateDescriptorSize);
		
		NotesCollationInfo collationInfo = new NotesCollationInfo(collationStruct.Flags, collateDescriptors);

		for (int i=0; i<items; i++) {
			NotesCollateDescriptorStruct descStruct = NotesCollateDescriptorStruct.newInstance(dataPtr.share(baseOffsetDescriptors + i*NotesCAPI.notesCollateDescriptorSize));
			descStruct.read();
			
			byte currDescFlags = descStruct.Flags;
			CollateType currDescType;
			try {
				currDescType = CollateType.toType(descStruct.keytype);
			}
			catch (IllegalArgumentException e) {
				throw new AssertionError("Collation structure invalid, collate type unknown for column #"+i+".\nMem dump:\n"+DumpUtil.dumpAsAscii(dataPtr, NotesCAPI.notesCollationSize + (items * NotesCAPI.notesCollateDescriptorSize)), e);
			}
			
			//sanity check that the signature byte is at the right position
			if (NotesCAPI.COLLATE_DESCRIPTOR_SIGNATURE != descStruct.signature) {
				throw new AssertionError("Descriptor signature byte is not correct.\nMem dump:\n"+DumpUtil.dumpAsAscii(dataPtr, NotesCAPI.notesCollationSize + (items * NotesCAPI.notesCollateDescriptorSize)));
			}
			
			int currTextBufferOffset = (int) (descStruct.NameOffset & 0xffff);
			int currTextBufferLength = (int) (descStruct.NameLength & 0xffff);
			Pointer currTextPtr = dataPtr.share(baseOffsetTextBuffer + currTextBufferOffset);
			String currName = NotesStringUtils.fromLMBCS(currTextPtr, currTextBufferLength);
			
			NotesCollateDescriptor newDesc = new NotesCollateDescriptor(collationInfo, currName, currDescType, currDescFlags);
			collateDescriptors.add(newDesc);
		}
		
		return collationInfo;
	}
}
