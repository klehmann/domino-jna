package com.mindoo.domino.jna.internal;

import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

/**
 * Abstract cache class that implements an LRU algorithm
 * 
 * @author Karsten Lehmann
 *
 * @param <K> key type
 * @param <V> value type
 */
public abstract class SizeLimitedLRUCache<K,V> {
	private Map<K, CacheEntry<K,V>> m_cache;
	private CacheEntry<K,V> m_head;
	private CacheEntry<K,V> m_tail;
	
	private int m_maxSizeUnits;
	private int m_cacheSizeInUnits;

	public SizeLimitedLRUCache(int maxSizeUnits) {
		m_cache = new HashedMap<K, CacheEntry<K,V>>();
		m_maxSizeUnits = maxSizeUnits;
	}
	
	public CacheEntry<K,V> getHead() {
		return m_head;
	}
	
	public CacheEntry<K,V> getTail() {
		return m_tail;
	}
	
	private enum Type {Added,Moved,NoChange}
	
	public final int getMaxSizeInUnits() {
		return m_maxSizeUnits;
	}
	
	public final int getCurrentCacheSizeInUnits() {
		return m_cacheSizeInUnits;
	}
	
	private Type addOrMoveToHead(CacheEntry<K,V> entry) {
		int entrySizeUnits = computeSize(entry);
		
		if (m_head==null) {
			//first entry, so head and tail
			m_head = entry;
			m_tail = entry;
			entry.setPrev(null);
			entry.setNext(null);
			m_cacheSizeInUnits = entrySizeUnits;
			entryAdded(entry);
			return Type.Added;
		}
		else {
			CacheEntry<K,V> oldHead = m_head;
			CacheEntry<K,V> oldEntryPrev = entry.getPrev();
			CacheEntry<K,V> oldEntryNext = entry.getNext();
			
			if (!m_head.equals(entry)) {
				if (oldEntryPrev==null && oldEntryNext==null) {
					//new entry, insert at head
					entry.setNext(oldHead);
					oldHead.setPrev(entry);
					m_head = entry;
					m_cacheSizeInUnits += entrySizeUnits;
					entryAdded(entry);
					return Type.Added;
				}
				else {
					//entry already in the list, not at head
					
					//remove entry from chain, connect neighbors
					if (oldEntryPrev!=null) {
						oldEntryPrev.setNext(oldEntryNext);
					}
					if (oldEntryNext!=null) {
						oldEntryNext.setPrev(oldEntryPrev);
					}
					//move to head,
					//before old head
					entry.setPrev(null);
					entry.setNext(oldHead);
					oldHead.setPrev(entry);
					m_head = entry;
					
					if (m_tail!=null && m_tail.equals(entry)) {
						//entry was tail
						m_tail = oldEntryPrev;
					}
					return Type.Moved;
				}
			}
			else {
				//element was already in the list at head position
				return Type.NoChange;
			}
		}
	}
	
	public boolean remove(K key) {
		CacheEntry<K,V> entry = m_cache.get(key);
		if (entry!=null) {
			CacheEntry<K,V> oldEntryPrev = entry.getPrev();
			CacheEntry<K,V> oldEntryNext = entry.getNext();
			
			int entrySize = computeSize(entry);
			
			if (m_head.equals(entry)) {
				//entry is head
				entry.setNext(null);
				oldEntryNext.setPrev(null);
				m_head = oldEntryNext;
			}
			else {
				if (oldEntryPrev!=null) {
					oldEntryPrev.setNext(oldEntryNext);
				}
				
				if (oldEntryNext!=null) {
					oldEntryNext.setPrev(oldEntryPrev);
				}
				entry.setPrev(null);
				entry.setNext(null);
			}
			
			if (m_tail!=null && m_tail.equals(entry)) {
				//entry was tail
				m_tail = oldEntryPrev;
			}
			
			m_cacheSizeInUnits -= entrySize;
			m_cache.remove(key);
			return true;
		}
		else {
			return false;
		}
	}
	
	public V get(K key) {
		CacheEntry<K,V> entry = m_cache.get(key);
		if (entry!=null) {
			//mark entry as recently used
			addOrMoveToHead(entry);
			
			return entry.getValue();
		}
		return null;
	}
	
	private void removeTail() {
		if (m_tail!=null) {
			CacheEntry<K,V> oldTail = m_tail;
			int tailSizeUnits = computeSize(m_tail);
			
			CacheEntry<K,V> tailPredecessor = m_tail.getPrev();
			m_tail = tailPredecessor;
			tailPredecessor.setNext(null);
			oldTail.setPrev(null);
			m_cacheSizeInUnits -= tailSizeUnits;
			
			entryRemoved(oldTail);
			
			if (oldTail.equals(m_head)) {
				//entry was the only chain element (also head)
				m_head = null;
			}
		}
	}

	protected void entryAdded(CacheEntry<K,V> entry) {
		
	}

	protected void entryRemoved(CacheEntry<K,V> entry) {
		
	}

	public void put(K key, V value) {
		CacheEntry<K,V> oldEntry = m_cache.get(key);
		if (oldEntry!=null) {
			int oldEntrySize = computeSize(oldEntry);
			m_cacheSizeInUnits -= oldEntrySize;
			oldEntry.setValue(value);
			int newEntrySize = computeSize(oldEntry);
			m_cacheSizeInUnits += newEntrySize;
			
			addOrMoveToHead(oldEntry);
		}
		else {
			CacheEntry<K,V> entry = new CacheEntry(key, value);
			m_cache.put(key, entry);
			addOrMoveToHead(entry);
		}
		
		//remove cache elements that make the cache too big
		while ((m_cacheSizeInUnits > m_maxSizeUnits) && m_cache.size()>1 && m_tail!=null) {
			removeTail();
		}
	}
	
	/**
	 * Implement this method to compute a size for the cache entry
	 * 
	 * @param entry entry
	 * @return size in units
	 */
	protected abstract int computeSize(CacheEntry<K,V> entry);
	
	public static class CacheEntry<K,V> {
		private K m_key;
		private V m_value;
		private CacheEntry<K,V> m_prev;
		private CacheEntry<K,V> m_next;
		
		public CacheEntry(K key, V value) {
			m_key = key;
			m_value = value;
		}
		
		public K getKey() {
			return m_key;
		}
		
		public V getValue() {
			return m_value;
		}
		
		public void setValue(V value) {
			m_value = value;
		}
		
		public CacheEntry<K,V> getPrev() {
			return m_prev;
		}
		
		public CacheEntry<K,V> getNext() {
			return m_next;
		}
		
		public void setPrev(CacheEntry<K,V> prev) {
			m_prev = prev;
		}
		
		public void setNext(CacheEntry<K,V> next) {
			m_next = next;
		}
	}
}
