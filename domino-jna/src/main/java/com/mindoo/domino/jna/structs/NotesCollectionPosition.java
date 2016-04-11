package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the current collection position (COLLECTIONPOSITION type)
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionPosition extends Structure {
	/** # levels -1 in tumbler */
	public short Level;

	/**
	 * MINIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the NAVIGATE_MINLEVEL flag is enabled (for backward
	 * compatibility)
	 */
	public byte MinLevel;

	/**
	 * MAXIMUM level that this position is allowed to be nagivated to. This is
	 * useful to navigate a subtree using all navigator codes. This field is
	 * IGNORED unless the NAVIGATE_MAXLEVEL flag is enabled (for backward
	 * compatibility)
	 */
	public byte MaxLevel;
	
	
	/**
	 * Current tumbler (1.2.3, etc)<br>
	 * C type : DWORD[32]
	 */
	public int[] Tumbler = new int[32];
	public NotesCollectionPosition() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("Level", "MinLevel", "MaxLevel", "Tumbler");
	}
	/**
	 * @param Level # levels -1 in tumbler<br>
	 * @param MinLevel MINIMUM level that this position<br>
	 * @param MaxLevel MAXIMUM level that this position<br>
	 * @param Tumbler Current tumbler (1.2.3, etc)<br>
	 * C type : DWORD[32]
	 */
	public NotesCollectionPosition(short Level, byte MinLevel, byte MaxLevel, int Tumbler[]) {
		super();
		this.Level = Level;
		this.MinLevel = MinLevel;
		this.MaxLevel = MaxLevel;
		if ((Tumbler.length != this.Tumbler.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.Tumbler = Tumbler;
	}
	public NotesCollectionPosition(Pointer peer) {
		super(peer);
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
	public String toPosString() {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<=this.Level; i++) {
			if (sb.length() > 0) {
				sb.append('.');
			}
			sb.append(this.Tumbler[i]);
		}
		
		if (MinLevel!=0 || MaxLevel!=0) {
			sb.append("|").append(MinLevel).append("-").append(MaxLevel);
		}
		return sb.toString();
	}
	
	/**
	 * Converts a position string like "1.2.3" to a {@link NotesCollectionPosition} object.<br>
	 * <br>
	 * Please note that we also support an advanced syntax in contrast to IBM's API in order
	 * to specify the min/max level parameters: "1.2.3|0-2" for minlevel=0, maxlevel=2. These
	 * levels can be used to limit reading entries in a categorized view to specified depths.
	 * 
	 * @param posStr position string
	 * @return position object
	 */
	public static NotesCollectionPosition toPosition(String posStr) {
		short level;
		int[] tumbler;
		byte minLevel = 0;
		byte maxLevel = 0;
		
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
		
		if (posStr==null || posStr.length()==0 || "0".equals(posStr)) {
			level = 0;
			tumbler = new int[32];
			tumbler[0] = 0;
		}
		else {
			String[] parts = posStr.split("\\.");
			level = (short) (parts.length-1);
			tumbler = new int[32];
			for (int i=0; i<parts.length; i++) {
				tumbler[i] = Integer.parseInt(parts[i]);
			}
		}
		
		NotesCollectionPosition pos = new NotesCollectionPosition();
		pos.Level = level;
		pos.MinLevel = minLevel;
		pos.MaxLevel = maxLevel;
		for (int i=0; i<pos.Tumbler.length; i++) {
			pos.Tumbler[i] = 0;
		}
		for (int i=0; i<tumbler.length; i++) {
			pos.Tumbler[i] = tumbler[i];
		}
		return pos;
	}

	public static class ByReference extends NotesCollectionPosition implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesCollectionPosition implements Structure.ByValue {
		
	};
}
