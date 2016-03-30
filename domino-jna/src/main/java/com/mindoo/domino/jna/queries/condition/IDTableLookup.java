package com.mindoo.domino.jna.queries.condition;



public class IDTableLookup extends Criteria {
	private String[] m_noteIdsStr;
	private int[] m_noteIdsInt;
	
	private IDTableLookup(String... values) {
		m_noteIdsStr = values;
	}
	
	private IDTableLookup(int... values) {
		m_noteIdsInt = values;
	}

	public static Criteria noteIdsContain(String... values) {
		return new IDTableLookup(values);
	}

	public static Criteria noteIdsContain(int... values) {
		return new IDTableLookup(values);
	}

}
