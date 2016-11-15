package com.mindoo.domino.jna;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mindoo.domino.jna.NotesCollection.ViewLookupCallback;
import com.mindoo.domino.jna.NotesViewEntryData.CacheableViewEntryData;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.structs.NotesTimeDate;

/**
 * LRU cache class to be returned in {@link ViewLookupCallback#createDataCache()} in order to let NIF
 * improve lookup performance by skipping already known collection data.
 * 
 * @author Karsten Lehmann
 */
public class CollectionDataCache implements Serializable {
	private static final long serialVersionUID = 522152090817358117L;
	
	private int m_maxSize;
	private Map<Integer,CacheableViewEntryData> m_cacheEntries;
	private NotesTimeDate m_diffTime;
	private ReentrantReadWriteLock m_rwLock = new ReentrantReadWriteLock();
	private EnumSet<ReadMask> m_readMask;
	private long m_cacheUseCounter;
	
	/**
	 * Creates a new instance of an unbounded cache
	 */
	public CollectionDataCache() {
		this(Integer.MAX_VALUE);
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param maxSize maximum number of entries in the LRU cache
	 */
	public CollectionDataCache(final int maxSize) {
		if (maxSize <= 0)
			throw new IllegalArgumentException("Max size must be greater than 0: "+maxSize);
		
		m_maxSize = maxSize;
		
		//set up LRU cache map
		m_cacheEntries = Collections.synchronizedMap(new LinkedHashMap<Integer, CacheableViewEntryData>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<Integer, CacheableViewEntryData> eldest) {
				if (size() > maxSize) {
					return true;
				}
				else {
					return false;
				}
			}
		});
	}
	
	/**
	 * Returns the maximum number of entries in the cache
	 * 
	 * @return maximum number
	 */
	public int getMaxCacheSize() {
		return m_maxSize;
	}
	
	/**
	 * Returns the current number of entries in the cache
	 * 
	 * @return size
	 */
	public int size() {
		return m_cacheEntries.size();
	}
	
	/**
	 * Returns a statistic value with the number of view entries where we could use the cache data
	 * 
	 * @return count
	 */
	public long getCacheUsageStats() {
		return m_cacheUseCounter;
	}
	
	/**
	 * Removes all data from the cache
	 */
	public void flush() {
		m_rwLock.writeLock().lock();
		try {
			m_diffTime = null;
			m_readMask = null;
			m_cacheEntries.clear();
		}
		finally {
			m_rwLock.writeLock().unlock();
		}
	}
	
	/**
	 * Method to fill the cache with data read from the collection
	 * 
	 * @param diffTime diff time returned from the read operation
	 * @param entries collection entries read
	 */
	void addCacheValues(EnumSet<ReadMask> readMask, NotesTimeDate diffTime, List<NotesViewEntryData> entries) {
		m_rwLock.writeLock().lock();
		try {
			boolean flush = false;
			
			if (m_diffTime!=null && !m_diffTime.equals(diffTime)) {
				flush = true;
			}
			else if (m_readMask!=null && !m_readMask.equals(readMask)) {
				flush = true;
			}
			if (flush) {
				m_cacheEntries.clear();
			}
			
			m_readMask = readMask;
			m_diffTime = diffTime;
			
			for (NotesViewEntryData currEntry : entries) {
				if (currEntry.hasAnyColumnValues()) {
					CacheableViewEntryData cacheableData = currEntry.getCacheableData();
					m_cacheEntries.put(currEntry.getNoteId(), cacheableData);
				}
			}
		}
		finally {
			m_rwLock.writeLock().unlock();
		}
	}
	
	/**
	 * For every {@link NotesViewEntryData} in the specified list, this method checks whether
	 * NIF returned any column data. If not, the entry was skipped by NIF, because it already exists
	 * in the cache. We can then copy the data of our current cache object.
	 * 
	 * @param entries entries to scan
	 */
	void populateEntryStubsWithData(List<NotesViewEntryData> entries) {
		boolean hasAnyMissingData = false;
		for (NotesViewEntryData currEntry : entries) {
			if (!currEntry.hasAnyColumnValues()) {
				hasAnyMissingData = true;
				break;
			}
		}
		
		if (hasAnyMissingData) {
			m_rwLock.readLock().lock();
			try {
				for (NotesViewEntryData currEntry : entries) {
					if (!currEntry.hasAnyColumnValues()) {
						CacheableViewEntryData cacheData = m_cacheEntries.get(currEntry.getNoteId());
						if (cacheData!=null) {
							//updating data of stub entry from cache
							currEntry.updateFromCache(cacheData);
							m_cacheUseCounter++;
						}
					}
				}
			}
			finally {
				m_rwLock.readLock().unlock();
			}
		}
	}
	
	/**
	 * Copies the current state of the cache
	 * 
	 * @return state
	 */
	CacheState getCacheState() {
		m_rwLock.readLock().lock();
		try {
			return new CacheState(m_readMask, m_diffTime, new HashMap<Integer, NotesViewEntryData.CacheableViewEntryData>(m_cacheEntries));
		}
		finally {
			m_rwLock.readLock().unlock();
		}
	}
	
	/**
	 * Data object with cache state values
	 * 
	 * @author Karsten Lehmann
	 */
	static class CacheState {
		private NotesTimeDate m_diffTime;
		private Map<Integer,CacheableViewEntryData> m_cacheEntries;
		private EnumSet<ReadMask> m_readMask;
		
		private CacheState(EnumSet<ReadMask> readMask, NotesTimeDate diffTime, Map<Integer,CacheableViewEntryData> cacheEntries) {
			m_readMask = readMask;
			m_diffTime = diffTime;
			m_cacheEntries = cacheEntries;
		}
		
		public EnumSet<ReadMask> getReadMask() {
			return m_readMask;
		}
		
		public NotesTimeDate getDiffTime() {
			return m_diffTime;
		}
		
		public Map<Integer,CacheableViewEntryData> getCacheEntries() {
			return m_cacheEntries;
		}
	}
	

}
