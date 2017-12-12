package com.mindoo.domino.jna.richtext.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple subclass of {@link AbstractMailMergeConversion} that uses a from/to
 * map to find/replace matches
 * 
 * @author Karsten Lehmann
 */
public class SimpleMailMergeConversion extends AbstractMailMergeConversion {
	private Map<Pattern,String> m_fromPatternToString;
	
	/**
	 * Creates a new instance
	 * 
	 * @param fromTo from/to map of search string and replacement
	 * @param ignoreCase true to ignore the case when searching
	 */
	public SimpleMailMergeConversion(Map<String,String> fromTo, boolean ignoreCase) {
		m_fromPatternToString = new HashMap<Pattern,String>();
		for (Entry<String,String> currEntry : fromTo.entrySet()) {
			String currFrom = currEntry.getKey();
			String currTo = currEntry.getValue();
			
			String currFromPattern = Pattern.quote(currFrom);
			Pattern pattern = ignoreCase ? Pattern.compile(currFromPattern, Pattern.CASE_INSENSITIVE) : Pattern.compile(currFromPattern);
			m_fromPatternToString.put(pattern, currTo);
		}
	}
	
	@Override
	protected boolean containsMatch(String txt) {
		for (Pattern currPattern : m_fromPatternToString.keySet()) {
			if (currPattern.matcher(txt).find()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected String replaceAllMatches(String txt) {
		String currTxt = txt;
		
		StringBuffer sb = new StringBuffer();
		
		for (Entry<Pattern,String> currEntry : m_fromPatternToString.entrySet()) {
			Pattern currPattern = currEntry.getKey();
			String currTo = currEntry.getValue();
			
			Matcher m = currPattern.matcher(currTxt);
			while (m.find()) {
				m.appendReplacement(sb, currTo);
			}
			m.appendTail(sb);
			currTxt = sb.toString();
			sb.setLength(0);
		}
		return currTxt;
	}

}
