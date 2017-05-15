package com.mindoo.domino.jna.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

	//switch to change cache scope for performance testing
	private static final boolean USE_SHARED_CACHE = true;
	//cache instance is shared across threads to improve reuse of strings
	private static final Map<LMBCSString,String> SHAREDSTRINGCONVERSIONCACHE = !USE_SHARED_CACHE ? null : Collections.synchronizedMap(new LinkedHashMap<LMBCSString, String>(16,0.75f, true) {
		private static final long serialVersionUID = -5818239831757810895L;

		@Override
		protected boolean removeEldestEntry (Map.Entry<LMBCSString,String> eldest) {
			if (size() > MAX_STRINGCACHE_SIZE_SHARED) {
				return true;
			}
			else {
				return false;
			}
		}
	});

	public static int getCacheSize() {
		return getCache().size();
	}
	
	private static Map<LMBCSString,String> getCache() {
		Map<LMBCSString,String> cache;
		if (USE_SHARED_CACHE) {
			cache = SHAREDSTRINGCONVERSIONCACHE;
		}
		else {
			cache = (Map<LMBCSString, String>) NotesGC.getCustomValue(CACHE_KEY);
			if (cache==null) {
				cache = new LinkedHashMap<LMBCSString, String>(16,0.75f, true) {
					private static final long serialVersionUID = -5818239831757810895L;

					@Override
					protected boolean removeEldestEntry (Map.Entry<LMBCSString,String> eldest) {
						if (size() > MAX_STRINGCACHE_SIZE_PERTHREAD) {
							return true;
						}
						else {
							return false;
						}
					}
				};
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
		Map<LMBCSString,String> cache = getCache();
		
		String stringFromCache = cache.get(lmbcsString);
		String convertedString;
		
		if (stringFromCache==null) {
			byte[] dataArr = lmbcsString.getData();
			Memory dataMem = new Memory(dataArr.length);
			dataMem.write(0, dataArr, 0, dataArr.length);
			
			convertedString = NotesStringUtils.fromLMBCS(dataMem, dataArr.length);
			cache.put(lmbcsString, convertedString);
		}
		else {
			convertedString = stringFromCache;
		}
		return convertedString;
	}
	
}
