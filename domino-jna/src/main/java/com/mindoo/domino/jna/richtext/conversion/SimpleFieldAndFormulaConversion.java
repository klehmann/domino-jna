package com.mindoo.domino.jna.richtext.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple subclass of {@link AbstractFieldAndFormulaConversion} that uses a from/to
 * map to find/replace matches
 * 
 * @author Karsten Lehmann
 */
public class SimpleFieldAndFormulaConversion extends AbstractFieldAndFormulaConversion {
	private Map<Pattern,String> m_fromPatternToString;
	private Map<String,String> m_dvFormulasByFieldName;
	private Map<String,String> m_itFormulasByFieldName;
	private Map<String,String> m_ivFormulasByFieldName;
	private Map<String,String> m_keywordFormulasByFieldName;
	
	/**
	 * Creates a new instance
	 * 
	 * @param fromTo from/to map of search string and replacement
	 * @param ignoreCase true to ignore the case when searching
	 */
	public SimpleFieldAndFormulaConversion(Map<String,String> fromTo, boolean ignoreCase) {
		m_fromPatternToString = new HashMap<Pattern,String>();
		for (Entry<String,String> currEntry : fromTo.entrySet()) {
			String currFrom = currEntry.getKey();
			String currTo = currEntry.getValue();
			
			String currFromPattern = Pattern.quote(currFrom);
			Pattern pattern = ignoreCase ? Pattern.compile(currFromPattern, Pattern.CASE_INSENSITIVE) : Pattern.compile(currFromPattern);
			m_fromPatternToString.put(pattern, currTo);
		}
	}

	/**
	 * Use this method to override the default value formulas of fields
	 * 
	 * @param formulasByFieldName map with fieldname as key and new formula as value
	 */
	public void setDefaultValueFormulas(Map<String,String> formulasByFieldName) {
		m_dvFormulasByFieldName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		m_dvFormulasByFieldName.putAll(formulasByFieldName);
	}
	
	/**
	 * Use this method to override the input translation formulas of fields
	 * 
	 * @param formulasByFieldName map with fieldname as key and new formula as value
	 */
	public void setInputTranslationFormulas(Map<String,String> formulasByFieldName) {
		m_itFormulasByFieldName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		m_itFormulasByFieldName.putAll(formulasByFieldName);
	}

	/**
	 * Use this method to override the input validation formulas of fields
	 * 
	 * @param formulasByFieldName map with fieldname as key and new formula as value
	 */
	public void setInputValidationFormulas(Map<String,String> formulasByFieldName) {
		m_ivFormulasByFieldName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		m_ivFormulasByFieldName.putAll(formulasByFieldName);
	}

	/**
	 * Use this method to override the keyword formulas of fields (formula computing the list values of a combobox)
	 * 
	 * @param formulasByFieldName map with fieldname as key and new formula as value
	 */
	public void setKeywordFormulas(Map<String,String> formulasByFieldName) {
		m_keywordFormulasByFieldName = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		m_keywordFormulasByFieldName.putAll(formulasByFieldName);
	}

	private boolean containsMatch(String txt) {
		for (Pattern currPattern : m_fromPatternToString.keySet()) {
			if (currPattern.matcher(txt).find()) {
				return true;
			}
		}
		return false;
	}

	private String replaceAllMatches(String txt) {
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

	@Override
	protected boolean fieldFormulaContainsMatch(String fieldName, FormulaType type, String formula) {
		//check of formula should be overridden
		switch (type) {
		case DEFAULTVALUE:
			if(m_dvFormulasByFieldName!=null && m_dvFormulasByFieldName.containsKey(fieldName)) {
				return true;
			}
			break;
		case INPUTTRANSLATION:
			if(m_itFormulasByFieldName!=null && m_itFormulasByFieldName.containsKey(fieldName)) {
				return true;
			}
			break;
		case INPUTVALIDITYCHECK:
			if (m_ivFormulasByFieldName!=null && m_ivFormulasByFieldName.containsKey(fieldName)) {
				return true;
			}
			break;
		case KEYWORDFORMULA:
			if(m_keywordFormulasByFieldName!=null && m_keywordFormulasByFieldName.containsKey(fieldName)) {
				return true;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown formula type: "+type);
		}

		return containsMatch(formula);
	}

	@Override
	protected String replaceAllMatchesInFieldFormula(String fieldName, FormulaType type, String formula) {
		//check if whole formula should be replaced
		switch (type) {
		case DEFAULTVALUE:
		{
			if (m_dvFormulasByFieldName!=null) {
				String newFormula = m_dvFormulasByFieldName.get(fieldName);
				if (newFormula!=null) {
					//override formula
					return newFormula;
				}
			}
		}
		break;
		case INPUTTRANSLATION:
		{
			if (m_itFormulasByFieldName!=null) {
				String newFormula = m_itFormulasByFieldName.get(fieldName);
				if (newFormula!=null) {
					//override formula
					return newFormula;
				}
			}
		}
		break;
		case INPUTVALIDITYCHECK:
		{
			if (m_ivFormulasByFieldName!=null) {
				String newFormula = m_ivFormulasByFieldName.get(fieldName);
				if (newFormula!=null) {
					//override formula
					return newFormula;
				}
			}
		}
		break;
		case KEYWORDFORMULA:
		{
			if (m_keywordFormulasByFieldName!=null) {
				String newFormula = m_keywordFormulasByFieldName.get(fieldName);
				if (newFormula!=null) {
					//override formula
					return newFormula;
				}
			}
		}
		break;
		}
		
		return replaceAllMatches(formula);
	}

	@Override
	protected boolean hideWhenFormulaContainsMatch(String formula) {
		return containsMatch(formula);
	}
	
	@Override
	protected String replaceAllMatchesInHideWhenFormula(String formula) {
		return replaceAllMatches(formula);
	}
	
	@Override
	protected boolean hotspotFormulaContainsMatch(String formula) {
		return containsMatch(formula);
	}

	@Override
	protected String replaceAllMatchesInHotspotFormula(String formula) {
		return replaceAllMatches(formula);
	}
	
	@Override
	protected boolean fieldNameContainsMatch(String fieldName) {
		return containsMatch(fieldName);
	}

	@Override
	protected String replaceAllMatchesInFieldName(String fieldName) {
		return replaceAllMatches(fieldName);
	}

	@Override
	protected boolean fieldDescriptionContainsMatch(String fieldName, String fieldDesc) {
		return containsMatch(fieldDesc);
	}

	@Override
	protected String replaceAllMatchesInFieldDescription(String fieldName, String fieldDesc) {
		return replaceAllMatches(fieldDesc);
	}

}
