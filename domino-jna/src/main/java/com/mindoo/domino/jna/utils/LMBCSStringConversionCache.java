package com.mindoo.domino.jna.utils;

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
	private static final int MAX_STRINGCACHE_SIZE = 200;
	
	/**
	 * Converts an LMBCS string to a Java String. If already cached, no native call is made.
	 * 
	 * @param lmbcsString LMBCS string
	 * @return converted string
	 */
	public static String get(LMBCSString lmbcsString) {
		@SuppressWarnings("unchecked")
		Map<LMBCSString,String> cache = (Map<LMBCSString, String>) NotesGC.getCustomValue(CACHE_KEY);
		String convertedString;
		
		if (cache==null) {
			cache = new LinkedHashMap<LMBCSString, String>(16,0.75f, true) {
				private static final long serialVersionUID = -5818239831757810895L;

				@Override
				protected boolean removeEldestEntry (Map.Entry<LMBCSString,String> eldest) {
					if (size() > MAX_STRINGCACHE_SIZE) {
						return true;
					}
					else {
						return false;
					}
				}
			};
			NotesGC.setCustomValue(CACHE_KEY, cache);
			convertedString = null;
		}
		else {
			convertedString = cache.get(lmbcsString);
		}

		if (convertedString==null) {
			byte[] dataArr = lmbcsString.getData();
			Memory dataMem = new Memory(dataArr.length);
			dataMem.write(0, dataArr, 0, dataArr.length);
			
			convertedString = NotesStringUtils.fromLMBCS(dataMem, dataArr.length);
			cache.put(lmbcsString, convertedString);
		}
		return convertedString;
	}
	
}
