package com.mindoo.domino.jna.utils;

/**
 * Container class for a generic pair of two values
 * 
 * @author Karsten Lehmann
 *
 * @param <V1> type of value 1
 * @param <V2> type of value 2
 */
public class Pair<V1,V2> {
	private V1 value1;
	private V2 value2;
	
	public Pair(V1 key, V2 value)  {
		this.value1 = key;
		this.value2 = value;
	}
	
	public V1 getValue1() {
		return value1;
	}
	
	public void setValue1(V1 value) {
		this.value1 = value;
	}
	
	public V2 getValue2() {
		return value2;
	}
	
	public void setValue2(V2 value) {
		this.value2 = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
		result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (value1 == null) {
			if (other.value1 != null)
				return false;
		} else if (!value1.equals(other.value1))
			return false;
		if (value2 == null) {
			if (other.value2 != null)
				return false;
		} else if (!value2.equals(other.value2))
			return false;
		return true;
	}
	
	
}
