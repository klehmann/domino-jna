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
	/** MINIMUM level that this position */
	public byte MinLevel;
	/** MAXIMUM level that this position */
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
	
	public String toPosString() {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<=this.Level; i++) {
			if (sb.length() > 0) {
				sb.append('.');
			}
			sb.append(this.Tumbler[i]);
		}
		return sb.toString();
	}
	
	public static NotesCollectionPosition toPosition(String posStr) {
		short level;
		int[] tumbler;
		byte minLevel = 0;
		byte maxLevel = 0;
		
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
//		return new NotesCollectionPosition(level, (byte) 0, (byte) 0, tumbler);
	}

//	public static NotesCollectionPosition.ByReference toPositionByRef(String posStr) {
//		NotesCollectionPosition.ByReference posByRef = new NotesCollectionPosition.ByReference();
//		
//		if (posStr==null || posStr.length()==0 || "0".equals(posStr)) {
//			posByRef.Level = 0;
//			posByRef.Tumbler = new int[32];
//			posByRef.Tumbler[0] = 0;
//		}
//		else {
//			String[] parts = posStr.split("\\.");
//			short level = (short) (parts.length-1);
//			int[] tumbler = new int[32];
//			for (int i=0; i<parts.length; i++) {
//				tumbler[i] = Integer.parseInt(parts[i]);
//			}
//			posByRef.Level = level;
//			posByRef.MinLevel = 0;
//			posByRef.MaxLevel = 0;
//			posByRef.Tumbler = tumbler;
//		}
//		return posByRef;
//	}

	public static class ByReference extends NotesCollectionPosition implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesCollectionPosition implements Structure.ByValue {
		
	};
}
