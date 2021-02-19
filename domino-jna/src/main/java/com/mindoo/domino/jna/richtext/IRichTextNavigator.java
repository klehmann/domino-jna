package com.mindoo.domino.jna.richtext;

import java.util.Set;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.sun.jna.Memory;

/**
 * Navigator class to traverse the CD record structure of a richtext item
 * 
 * @author Karsten Lehmann
 */
public interface IRichTextNavigator {
		
	/**
	 * Checks if there is any data to read
	 * 
	 * @return true if we have no data for the item
	 */
	public boolean isEmpty();
	
	/**
	 * Moves the position to the first CD record
	 * 
	 * @return true if there is data to read
	 */
	public boolean gotoFirst();
	
	/**
	 * Moves the position to the last CD record
	 * 
	 * @return true if there is data to read
	 */
	public boolean gotoLast();
	
	/**
	 * Moves the position to the next CD record
	 * 
	 * @return true if there is data to read
	 */
	public boolean gotoNext();
	
	/**
	 * Moves the position to the previous CD record
	 * 
	 * @return true if there is data to read
	 */
	public boolean gotoPrev();
	
	/**
	 * Method to check if there is a next CD record
	 * 
	 * @return true if there is data to read
	 */
	public boolean hasNext();
	
	/**
	 * Method to check if there is a previous CD record
	 * 
	 * @return true if there is data to read
	 */
	public boolean hasPrev();
	
	/**
	 * Returns a read-only buffer to access the CD record data (CD record header BSIG/WSIG/LSIG is not part of
	 * the returned data)
	 * 
	 * @return data buffer with length {@link #getCurrentRecordDataLength()}
	 */
	public Memory getCurrentRecordData();
	
	/**
	 * Returns a read-only buffer to access the CD record data (CD record header BSIG/WSIG/LSIG is included
	 * in the returned data)
	 * 
	 * @return data buffer with length {@link #getCurrentRecordDataLength()} + {@link #getCurrentRecordHeaderLength()}
	 */
	public Memory getCurrentRecordDataWithHeader();

	/**
	 * Returns the length of the BSIG/WSIG/LSIG header contained in {@link #getCurrentRecordDataWithHeader()}
	 * 
	 * @return header size
	 */
	public int getCurrentRecordHeaderLength();
	
	/**
	 * Returns the type of the current CD record as short constant. Use
	 * {@link CDRecordType#getRecordTypeForConstant(short, com.mindoo.domino.jna.constants.CDRecordType.Area)}
	 * to get an enum value for the type of data you are processing.
	 * 
	 * @return type as short
	 */
	public short getCurrentRecordTypeAsShort();
	
	/**
	 * Returns the type of the current CD record.
	 * 
	 * @return a set of {@link CDRecordType} values that have the value {@link CDRecordType#getConstant()} (there may be duplicates like PABHIDE/VMTEXTBOX or ACTION/VMPOLYRGN)
	 */
	public Set<CDRecordType> getCurrentRecordType();
	
	/**
	 * Returns the length of the actual data stored in the CD record (CD record lengths minus header bytes)
	 * 
	 * @return length without header
	 */
	public int getCurrentRecordDataLength();
	
	/**
	 * Returns the length of the current CD record including BSIG/WSIG/LSIG header that contains the record
	 * type and data length
	 * 
	 * @return length with header
	 */
	public int getCurrentRecordTotalLength();
	
	/**
	 * Returns the current position in the CD record stream. Use {@link #restoreCurrentRecordPosition(RichTextNavPosition)}
	 * to change the position to a stored value. Can be used to search for complex structures in the CD record
	 * stream (like file hotspots for a specific file, which consist of multiple CD records) and jump back if
	 * the structure could not be found.
	 * 
	 * @return position
	 */
	public RichTextNavPosition getCurrentRecordPosition();
	
	/**
	 * Resets the position to a stored one. Use {@link #getCurrentRecordPosition()} first to read a position
	 * 
	 * @param pos position
	 */
	public void restoreCurrentRecordPosition(RichTextNavPosition pos);
	
	/**
	 * Adds the current CD record to a {@link ICompoundText}
	 * 
	 * @param target compound text
	 */
	public void copyCurrentRecordTo(ICompoundText<?> target);
	
	/**
	 * Position in a richtext item's CD record stream. The class implementing this interface
	 * has valid implementations for {@link #equals(Object)} and {@link #hashCode()} so that
	 * it can be compared with other positions retrieved earlier from the same navigator instance.
	 * 
	 * @author Karsten Lehmann
	 */
	public interface RichTextNavPosition {
		@Override
		public boolean equals(Object obj);
		
		@Override
		public int hashCode();
	}

	/**
	 * Extracts the text content stored in this richtext item, ignoring any formatting
	 * or other elements like images.
	 * 
	 * @return extracted text
	 */
	public String getText();
}
