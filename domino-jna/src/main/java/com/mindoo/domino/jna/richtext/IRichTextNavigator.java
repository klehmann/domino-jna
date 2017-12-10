package com.mindoo.domino.jna.richtext;

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
	 * Returns the type of the current CD record
	 * 
	 * @return type
	 */
	public CDRecordType getCurrentRecordType();
	
	/**
	 * Returns a read-only buffer to access the CD record data
	 * 
	 * @return data buffer
	 */
	public Memory getCurrentRecordData();
	
	/**
	 * Returns the type of the current CD record as short
	 * 
	 * @return type as short
	 */
	public short getCurrentRecordTypeAsShort();
	
	/**
	 * Returns the length of the actual data stored in the CD record (CD record lengths minus header bytes)
	 * 
	 * @return length without header
	 */
	public int getCurrentRecordDataSize();
	
	/**
	 * Returns the length of the current CD record including header
	 * 
	 * @return length with header
	 */
	public int getCurrentRecordTotalLength();
	
	/**
	 * Returns the current position in the CD record stream. Use {@link #restoreCurrentRecordPosition(RichTextNavPosition)}
	 * to change the position to a stored value. Can be used to search for complex structures in the CD record
	 * stream (like file hotspots for a specific file, which consist of multiple CD records) and jump back if
	 * the structure could not be found.
	 */
	public RichTextNavPosition getCurrentRecordPosition();
	
	/**
	 * Resets the position to a stored one. Use {@link #getCurrentRecordPosition()} first to read a position
	 */
	public void restoreCurrentRecordPosition(RichTextNavPosition pos);
	
	/**
	 * Adds the current CD record to a {@link ICompoundText}
	 * 
	 * @param target compound text
	 */
	public void copyCurrentRecordTo(ICompoundText target);
	
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
}
