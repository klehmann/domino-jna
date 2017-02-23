package com.mindoo.domino.jna;

import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.structs.NotesCollectionPositionStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * This structure is used to specify the hierarchical, index position of an item (or category)
 * within a View(collection).<br>
 * <br>
 * Level = (number of levels in tumbler - 1)<br>
 * <br>
 * Tumbler is an array of ordinal ranks within the view; with the first (0) entry referring to the top level.<br>
 * <br>
 * For example, consider the following non-Domino Outline Scheme :<br>
 * <br>
 * I.  First Main Category<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;A.  First sub-category under I<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;B.  Second sub-category under I<br>
 * II.  Second Main Category<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;A.  First sub-category under II<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.  First item under  II.A<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.  Second item under II.A<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;B.  Second sub-category under II<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;C.  Third sub-category under II<br>
 * III.  Third Main Category<br>
 * <br>
 * With this example, [2; II.A.2] refers to the "Second item under II.A."<br>
 * Similarly, [0; III] refers to the "Third Main Category."<br>
 * <br>
 * Finally, it should be noted that [2; I.B.1], [1; I.C], and [3; II.A.1] are all NOT valid positions.<br>
 * <br>
 * [2; I.B.1] because the "Second sub-category under I" has no items.<br>
 * [1; I.C] because there is no "Third sub-category under I", and<br>
 * [3; II.A.1] because the value of Level (3) shows four levels should be represented
 * in the Tumbler and there are only three.
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionPosition implements IAdaptable {
	private NotesCollectionPositionStruct m_struct;
	
	public NotesCollectionPosition(NotesCollectionPositionStruct struct) {
		m_struct = struct;
	}
	
	public NotesCollectionPosition(Pointer p) {
		this(NotesCollectionPositionStruct.newInstance(p));
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param Level level in the view, use 0 for first level
	 * @param MinLevel min level, see {@link #setMinLevel(int)}
	 * @param MaxLevel max level, see {@link #setMaxLevel(int)}
	 * @param Tumbler array with position information [index level0, index level1, ...], e.g. [1,2,3], up to 32 entries
	 */
	public NotesCollectionPosition(int Level, int MinLevel, int MaxLevel, final int Tumbler[]) {
		this(NotesCollectionPositionStruct.newInstance());
		
		m_struct.Level = (short) (Level & 0xffff);
		m_struct.MinLevel = (byte) (MinLevel & 0xff);
		m_struct.MaxLevel = (byte) (MaxLevel & 0xff);
		for (int i=0; i<m_struct.Tumbler.length; i++) {
			if (i < Tumbler.length) {
				m_struct.Tumbler[i] = Tumbler[i];
			}
			else {
				m_struct.Tumbler[i] = 0;
			}
		}
		m_struct.write();
	}
	
	/**
	 * Converts a position string like "1.2.3" to a {@link NotesCollectionPosition} object.<br>
	 * <br>
	 * Please note that we also support an advanced syntax in contrast to IBM's API in order
	 * to specify the min/max level parameters: "1.2.3|0-2" for minlevel=0, maxlevel=2. These
	 * levels can be used to limit reading entries in a categorized view to specified depths.
	 * 
	 * @param posStr position string
	 */
	public NotesCollectionPosition(String posStr) {
		this(NotesCollectionPositionStruct.toPosition(posStr));
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCollectionPositionStruct.class || clazz == Structure.class) {
			return (T) m_struct;
		}
		return null;
	}
	
	/**
	 * # levels -1 in tumbler
	 * 
	 * @return levels
	 */
	public int getLevel() {
		return (int) (m_struct.Level & 0xffff);
	}
	
	/**
	 * MINIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the NAVIGATE_MINLEVEL flag is enabled (for backward
	 * compatibility)
	 * 
	 * @return min level
	 */
	public int getMinLevel() {
		return (int) (m_struct.MinLevel & 0xffff);
	}
	
	/**
	 * Sets the MINIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the NAVIGATE_MINLEVEL flag is enabled (for backward
	 * compatibility)
	 * 
	 * @param level min level
	 */
	public void setMinLevel(int level) {
		m_struct.MinLevel = (byte) (level & 0xff);
		m_struct.write();
	}
	
	/**
	 * MAXIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the {@link Navigate#MAXLEVEL} flag is enabled (for backward
	 * compatibility)
	 * 
	 * @return max level
	 */
	public int getMaxLevel() {
		return (int) (m_struct.MaxLevel & 0xffff);
	}
	
	/**
	 * Sets the MAXIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the {@link Navigate#MAXLEVEL} flag is enabled (for backward
	 * compatibility)
	 * 
	 * @param level max level
	 */
	public void setMaxLevel(int level) {
		m_struct.MaxLevel = (byte) (level & 0xff);
		m_struct.write();
	}
	/**
	 * Returns the index position at each view level
	 * 
	 * @param level 0 for first level
	 * @return position starting with 1 if not restricted by reader fields
	 */
	public int getTumbler(int level) {
		return m_struct.Tumbler[level];
	}
}
