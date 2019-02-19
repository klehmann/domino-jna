package com.mindoo.domino.jna;

import java.util.Arrays;

import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.internal.structs.NotesCollectionPositionStruct;
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
	/** # levels -1 in tumbler */
	private int level;

	/**
	 * MINIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the NAVIGATE_MINLEVEL flag is enabled (for backward
	 * compatibility)
	 */
	private int minLevel;

	/**
	 * MAXIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the NAVIGATE_MAXLEVEL flag is enabled (for backward
	 * compatibility)
	 */
	private int maxLevel;
	
	/**
	 * Current tumbler (1.2.3, etc)<br>
	 * C type : DWORD[32]
	 */
	private int[] tumbler = new int[32];

	private String toString;
	
	private NotesCollectionPositionStruct struct;
	
	public NotesCollectionPosition(IAdaptable adaptable) {
		NotesCollectionPositionStruct struct = adaptable.getAdapter(NotesCollectionPositionStruct.class);
		if (struct!=null) {
			this.struct = struct;
			this.level = struct.Level;
			this.minLevel = struct.MinLevel;
			this.maxLevel = struct.MaxLevel;
			this.tumbler = struct.Tumbler;
			return;
		}
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			this.struct = NotesCollectionPositionStruct.newInstance(p);
			this.level = this.struct.Level;
			this.minLevel = this.struct.MinLevel;
			this.maxLevel = this.struct.MaxLevel;
			this.tumbler = this.struct.Tumbler;
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param tumbler array with position information [index level0, index level1, ...], e.g. [1,2,3], up to 32 entries
	 */
	public NotesCollectionPosition(int[] tumbler) {
		this(computeLevel(tumbler), 0, 0, tumbler);
	}
	
	private static int computeLevel(int[] tumbler) {
		if (tumbler[0]==0) {
			return 0;
		}
		
		for (int i=1; i<tumbler.length; i++) {
			if (tumbler[i]==0) {
				return i-1;
			}
		}
		return tumbler.length-1;
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param level level in the view, use 0 for first level
	 * @param minLevel min level, see {@link #setMinLevel(int)}
	 * @param maxLevel max level, see {@link #setMaxLevel(int)}
	 * @param tumbler array with position information [index level0, index level1, ...], e.g. [1,2,3], up to 32 entries
	 */
	public NotesCollectionPosition(int level, int minLevel, int maxLevel, final int tumbler[]) {
		this.level = level;
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		if (tumbler.length>32) {
			throw new IllegalArgumentException("Tumbler array exceeds the maximum size ("+tumbler.length+" > 32)");
		}
		this.tumbler = new int[32];
		for (int i=0; i<this.tumbler.length; i++) {
			if (i < tumbler.length) {
				this.tumbler[i] = tumbler[i];
			}
			else {
				this.tumbler[i] = 0;
			}
		}
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
		int iPos = posStr.indexOf("|");
		if (iPos!=-1) {
			//optional addition to the classic position string: |minlevel-maxlevel
			String minMaxStr = posStr.substring(iPos+1);
			posStr = posStr.substring(0, iPos);
			
			iPos = minMaxStr.indexOf("-");
			if (iPos!=-1) {
				minLevel = Byte.parseByte(minMaxStr.substring(0, iPos));
				maxLevel = Byte.parseByte(minMaxStr.substring(iPos+1));
			}
		}
		
		tumbler = new int[32];

		if (posStr==null || posStr.length()==0 || "0".equals(posStr)) {
			level = 0;
			tumbler[0] = 0;
			this.toString = "0";
		}
		else {
			String[] parts = posStr.split("\\.");
			level = (short) (parts.length-1);
			for (int i=0; i<parts.length; i++) {
				tumbler[i] = Integer.parseInt(parts[i]);
			}
			this.toString = posStr;
		}
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesCollectionPositionStruct.class || clazz == Structure.class) {
			if (this.struct==null) {
				this.struct = NotesCollectionPositionStruct.newInstance();
				this.struct.Level = (short) (this.level & 0xffff);
				this.struct.MinLevel = (byte) (this.minLevel & 0xff);
				this.struct.MaxLevel = (byte) (this.maxLevel & 0xff);
				this.struct.Tumbler = this.tumbler.clone();
				this.struct.write();
			}
			return (T) this.struct;
		}
		return null;
	}
	
	/**
	 * # levels -1 in tumbler
	 * 
	 * @return levels
	 */
	public int getLevel() {
		if (this.struct!=null) {
			//get current struct value, is changed by NIFReadEntries
			this.level = (int) (this.struct.Level & 0xffff);
		}
		return this.level;
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
		if (this.struct!=null) {
			//get current struct value, is changed by NIFReadEntries
			this.minLevel = (this.struct.MinLevel & 0xffff);
		}
		return this.minLevel;
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
		this.minLevel = level;
		if (this.struct!=null) {
			this.struct.MinLevel = (byte) (level & 0xff);
			this.struct.write();
		}
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
		if (this.struct!=null) {
			//get current struct value, is changed by NIFReadEntries
			this.maxLevel = (this.struct.MaxLevel & 0xffff);
		}
		return this.maxLevel;
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
		this.maxLevel = level;
		if (this.struct!=null) {
			this.struct.MaxLevel = (byte) (level & 0xff);
			this.struct.write();
		}
	}
	
	/**
	 * Returns the index position at each view level
	 * 
	 * @param level 0 for first level
	 * @return position starting with 1 if not restricted by reader fields
	 */
	public int getTumbler(int level) {
		if (this.struct!=null) {
			//get current struct value, is changed by NIFReadEntries
			this.tumbler = this.struct.Tumbler;
		}
		return this.tumbler[level];
	}

	/**
	 * Converts the position object to a position string like "1.2.3".<br>
	 * <br>
	 * Please note that we also support an advanced syntax in contrast to IBM's API in order
	 * to specify the min/max level parameters: "1.2.3|0-2" for minlevel=0, maxlevel=2. These
	 * levels can be used to limit reading entries in a categorized view to specified depths.<br>
	 * <br>
	 * This method will returns a string with the advanced syntax if MinLevel or MaxLevel is not 0.
	 * 
	 * @return position string
	 */
	@Override
	public String toString() {
		boolean recalc;
		//cache if cached value needs to be recalculated
		if (this.toString==null) {
			recalc = true;
		}
		else if (this.struct!=null && (this.level != this.struct.Level ||
				this.minLevel != this.struct.MinLevel ||
				this.maxLevel != this.struct.MaxLevel ||
				!Arrays.equals(this.tumbler, this.struct.Tumbler))) {
			recalc = true;
		}
		else {
			recalc = false;
		}
		
		if (recalc) {
			int level = this.getLevel();
			int minLevel = this.getMinLevel();
			int maxLevel = this.getMaxLevel();
			
			StringBuilder sb = new StringBuilder();
			
			for (int i=0; i<=level; i++) {
				if (sb.length() > 0) {
					sb.append('.');
				}
				sb.append(this.getTumbler(i));
			}
			
			if (minLevel!=0 || maxLevel!=0) {
				sb.append("|").append(minLevel).append("-").append(maxLevel);
			}
			toString = sb.toString();
		}
		return toString;
	}

}
