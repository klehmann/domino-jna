package com.mindoo.domino.jna.virtualviews;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to encapsulate changes in a {@link VirtualView} data set by one data provider
 */
public class VirtualViewDataChange {
	private String origin;
	private Set<Integer> removals;
	private Map<Integer, EntryData> additions;
	
	public VirtualViewDataChange(String origin) {
		this.origin = origin;
		this.removals = new HashSet<>();
		this.additions = new HashMap<>();
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public void removeEntry(int noteId) {
		removals.add(noteId);
	}
	
	public void addEntry(int noteId, String unid, Map<String,Object> values) {
		EntryData entry = new EntryData(unid, values);
		additions.put(noteId, entry);
	}
	
	public static class EntryData {
		private String unid;
		private Map<String, Object> values;

		public EntryData(String unid, Map<String, Object> values) {
			this.unid = unid;
			this.values = values;
		}
		
		public String getUnid() {
			return unid;
		}
		
		public Map<String, Object> getValues() {
			return values;
		}
	}
	
	public Set<Integer> getRemovals() {
		return removals;
	}
	
	public Map<Integer, EntryData> getAdditions() {
		return additions;
	}
}
