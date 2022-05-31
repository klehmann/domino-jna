package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.utils.LMBCSString;
import com.mindoo.domino.jna.utils.NotesStringUtils;

/**
 * Cache to optimize performance of LMBCS String conversion to Java Strings.
 * 
 * @author Karsten Lehmann
 */
public class LMBCSStringConversionCache {
	//use simple cache for lmbcs-string conversion of short string
	private static final boolean USE_LMBCS2STRING_CACHE = true;
	//max length of each lmbcs-string cache entry in bytes
	private static final int MAX_LMBCS2STRING_KEY_LENGTH = 1000;

	private static final int MAX_LMBCS2STRING_SIZE_BYTES = 1000000;

	private static SizeLimitedLRUCache<LMBCSString,String> LMBCS2STRINGCACHE = new SizeLimitedLRUCache<LMBCSString,String>(MAX_LMBCS2STRING_SIZE_BYTES) {
		@Override
		protected int computeSize(LMBCSString key, String value) {
			return key.size() + value.length()*2;
		}
	};

	public static long getCacheSize() {
		return LMBCS2STRINGCACHE.getCurrentCacheSizeInUnits();
	}

	/**
	 * Converts an LMBCS string to a Java String. If already cached, no native call is made.
	 * 
	 * @param lmbcsString LMBCS string
	 * @return converted string
	 */
	public static String get(LMBCSString lmbcsString) {
		return get(lmbcsString, false);
	}
	
	/**
	 * Converts an LMBCS string to a Java String. If already cached, no native call is made.
	 * 
	 * @param lmbcsString LMBCS string
	 * @param onlyIfCached returns null if value is not already in the cache
	 * @return converted string
	 */
	public static String get(LMBCSString lmbcsString, boolean onlyIfCached) {
		String stringFromCache = LMBCS2STRINGCACHE.get(lmbcsString);
		if (onlyIfCached) {
			return stringFromCache;
		}
		String convertedString;
		
		if (stringFromCache==null) {
			byte[] dataArr = lmbcsString.getData();
			
			convertedString = NotesStringUtils.fromLMBCS(dataArr);
			if (USE_LMBCS2STRING_CACHE && lmbcsString.size()<=MAX_LMBCS2STRING_KEY_LENGTH) {
				LMBCS2STRINGCACHE.put(lmbcsString, convertedString);
			}
		}
		else {
			convertedString = stringFromCache;
		}
		return convertedString;
	}
}
