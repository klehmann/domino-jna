package com.mindoo.domino.jna.utils;

import java.nio.charset.Charset;

import com.mindoo.domino.jna.gc.NotesGC;
import com.sun.jna.Memory;

/**
 * Cache to optimize performance of LMBCS String conversion to Java Strings.
 * 
 * @author Karsten Lehmann
 */
public class LMBCSStringConversionCache {
	private static final String CACHE_KEY = "LMBCSStringCache";
	
	private static final int MAX_STRINGCACHE_SIZE_SHARED = 100000;
	private static final int MAX_STRINGCACHE_SIZE_PERTHREAD = 1000;
	
	private static final int MAX_STRINGCACHE_SIZE_SHARED_BYTES = 750000;
	private static final int MAX_STRINGCACHE_SIZE_PERTHREAD_BYTES = 40000;

	//switch to change cache scope for performance testing
	private static final boolean USE_SHARED_CACHE = true;
	private static LRULMBCCache SHAREDSTRINGCONVERSIONCACHE = new LRULMBCCache(MAX_STRINGCACHE_SIZE_SHARED_BYTES);

	public static int getCacheSize() {
		return getCache().getCurrentCacheSizeInUnits();
	}

	private static LRULMBCCache getCache() {
		LRULMBCCache cache;
		if (USE_SHARED_CACHE) {
			cache = SHAREDSTRINGCONVERSIONCACHE;
		}
		else {
			cache = (LRULMBCCache) NotesGC.getCustomValue(CACHE_KEY);
			if (cache==null) {
				cache = new LRULMBCCache(MAX_STRINGCACHE_SIZE_PERTHREAD_BYTES);
				NotesGC.setCustomValue(CACHE_KEY, cache);
			}
		}
		return cache;
	}

	/**
	 * Converts an LMBCS string to a Java String. If already cached, no native call is made.
	 * 
	 * @param lmbcsString LMBCS string
	 * @return converted string
	 */
	public static String get(LMBCSString lmbcsString) {
		LRULMBCCache cache = getCache();
		
		String stringFromCache = cache.get(lmbcsString);
		String convertedString;
		
		if (stringFromCache==null) {
			byte[] dataArr = lmbcsString.getData();
			
			boolean isPureAscii = true;
			for (int i=0; i < dataArr.length; i++) {
				byte b = dataArr[i];
				if (b <= 0x1f || b >= 0x80) {
					isPureAscii = false;
					break;
				}
			}
			
			if (isPureAscii) {
				String asciiStr = new String(dataArr, Charset.forName("ASCII"));
				cache.put(lmbcsString, asciiStr);
				return asciiStr;
			}
			
			Memory dataMem = new Memory(dataArr.length);
			dataMem.write(0, dataArr, 0, dataArr.length);
			
			boolean skipAsciiCheck = true;
			convertedString = NotesStringUtils.fromLMBCS(dataMem, dataArr.length, skipAsciiCheck);
			cache.put(lmbcsString, convertedString);
		}
		else {
			convertedString = stringFromCache;
		}
		return convertedString;
	}
	
	public static class LRULMBCCache extends SizeLimitedLRUCache<LMBCSString,String> {

		public LRULMBCCache(int maxSizeUnits) {
			super(maxSizeUnits);
		}

		@Override
		protected int computeSize(CacheEntry<LMBCSString,String> entry) {
			LMBCSString key = entry.getKey();
			String value = entry.getValue();
			
			return key.size() + value.length()*2;
		}

		@Override
		protected void entryAdded(
				com.mindoo.domino.jna.utils.SizeLimitedLRUCache.CacheEntry<LMBCSString, String> entry) {

//			System.out.println("Added to cache: "+entry.getValue());
		}
		
		@Override
		protected void entryRemoved(com.mindoo.domino.jna.utils.SizeLimitedLRUCache.CacheEntry<LMBCSString, String> entry) {
//			System.out.println("Removed from cache: "+entry.getValue());
		}
		
	}
}
