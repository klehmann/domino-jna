package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.mindoo.domino.jna.INoteSummary;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

public abstract class TypedItemAccess implements INoteSummary {

	public abstract Object get(String itemName);
	
	@Override
	public String getAsString(String itemName, String defaultValue) {
		Object val = get(itemName);
		if (val instanceof String) {
			return (String) val;
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (valAsList.isEmpty()) {
				return defaultValue;
			}
			else {
				return valAsList.get(0).toString();
			}
		}
		return defaultValue;
	}
	
	@Override
	public String getAsNameAbbreviated(String itemName) {
		return getAsNameAbbreviated(itemName, null);
	}
	
	@Override
	public String getAsNameAbbreviated(String itemName, String defaultValue) {
		String nameStr = getAsString(itemName, null);
		return nameStr==null ? defaultValue : NotesNamingUtils.toAbbreviatedName(nameStr);
	}
	
	@Override
	public List<String> getAsNamesListAbbreviated(String itemName) {
		return getAsNamesListAbbreviated(itemName, null);
	}
	
	@Override
	public List<String> getAsNamesListAbbreviated(String itemName, List<String> defaultValue) {
		List<String> strList = getAsStringList(itemName, null);
		if (strList!=null) {
			List<String> namesAbbr = new ArrayList<String>(strList.size());
			for (int i=0; i<strList.size(); i++) {
				namesAbbr.add(NotesNamingUtils.toAbbreviatedName(strList.get(i)));
			}
			return namesAbbr;
		}
		else
			return defaultValue;
	}
	
	@Override
	public List<String> getAsStringList(String itemName, List<String> defaultValue) {
		Object val = get(itemName);
		if (val instanceof String) {
			return Arrays.asList((String) val);
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			for (int i=0; i<valAsList.size(); i++) {
				if (!(valAsList.get(i) instanceof String)) {
					correctType=false;
					break;
				}
			}
			
			if (correctType) {
				return (List<String>) valAsList;
			}
			else {
				List<String> strList = new ArrayList<String>();
				for (int i=0; i<valAsList.size(); i++) {
					strList.add(valAsList.get(i).toString());
				}
				return strList;
			}
		}
		else if (val!=null) {
			return Arrays.asList(val.toString());
		}
		return defaultValue;
	}
	
	@Override
	public Calendar getAsCalendar(String itemName, Calendar defaultValue) {
		Object val = get(itemName);
		if (val instanceof Calendar) {
			return (Calendar) val;
		}
		else if (val instanceof NotesTimeDate) {
			return ((NotesTimeDate) val).toCalendar();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Calendar) {
					return (Calendar) firstVal;
				}
				else if (firstVal instanceof NotesTimeDate) {
					return ((NotesTimeDate) firstVal).toCalendar();
				}
			}
		}
		return defaultValue;
	}
	
	@Override
	public NotesTimeDate getAsTimeDate(String itemName, NotesTimeDate defaultValue) {
		Object val = get(itemName);

		if (val instanceof NotesTimeDate) {
			return (NotesTimeDate) val;
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof NotesTimeDate) {
					return (NotesTimeDate) firstVal;
				}
			}
		}
		return defaultValue;
	}
	
	@Override
	public List<Calendar> getAsCalendarList(String itemName, List<Calendar> defaultValue) {
		Object val = get(itemName);
		if (val instanceof Calendar) {
			return Arrays.asList((Calendar) val);
		}
		else if (val instanceof NotesTimeDate) {
			return Arrays.asList(((NotesTimeDate) val).toCalendar());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean supportedType=true;
			boolean conversionRequired=false;
			
			for (int i=0; i<valAsList.size(); i++) {
				if (valAsList.get(i) instanceof Calendar) {
					//ok
				}
				else if (valAsList.get(i) instanceof NotesTimeDate) {
					conversionRequired = true;
				}
				else {
					supportedType = false;
					break;
				}
			}
			
			if (supportedType) {
				if (conversionRequired) {
					List<Calendar> calList = new ArrayList<Calendar>(valAsList.size());
					for (int i=0; i<valAsList.size(); i++) {
						if (valAsList.get(i) instanceof Calendar) {
							calList.add((Calendar) valAsList.get(i));
						}
						else if (valAsList.get(i) instanceof NotesTimeDate) {
							calList.add(((NotesTimeDate) valAsList.get(i)).toCalendar());
						}
					}
					return calList;
				}
				else {
					return (List<Calendar>) valAsList;
				}
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}
	
	@Override
	public List<NotesTimeDate> getAsTimeDateList(String itemName, List<NotesTimeDate> defaultValue) {
		Object val = get(itemName);

		if (val instanceof NotesTimeDate) {
			return Arrays.asList((NotesTimeDate) val);
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean supportedType=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				if (valAsList.get(i) instanceof NotesTimeDate) {
					//ok
				}
				else {
					supportedType = false;
					break;
				}
			}
			
			if (supportedType) {
				return (List<NotesTimeDate>) valAsList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}
	
	@Override
	public Double getAsDouble(String itemName, Double defaultValue) {
		Object val = get(itemName);
		if (val instanceof Number) {
			return ((Number) val).doubleValue();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Number) {
					return ((Number) firstVal).doubleValue();
				}
			}
		}
		return defaultValue;
	}
	
	@Override
	public Integer getAsInteger(String itemName, Integer defaultValue) {
		Object val = get(itemName);
		if (val instanceof Number) {
			return ((Number) val).intValue();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Number) {
					return ((Number) firstVal).intValue();
				}
			}
		}
		return defaultValue;
	}
	
	@Override
	public List<Double> getAsDoubleList(String itemName, List<Double> defaultValue) {
		Object val = get(itemName);
		if (val instanceof Number) {
			return Arrays.asList(((Number) val).doubleValue());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			boolean numberList=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currObj = valAsList.get(i);
				
				if (currObj instanceof Double) {
					//ok
				}
				else if (currObj instanceof Number) {
					correctType=false;
					numberList=true;
				}
				else {
					correctType=false;
					numberList=false;
				}
			}
			
			if (correctType) {
				return (List<Double>) valAsList;
			}
			else if (numberList) {
				List<Double> dblList = new ArrayList<Double>(valAsList.size());
				for (int i=0; i<valAsList.size(); i++) {
					dblList.add(((Number)valAsList.get(i)).doubleValue());
				}
				return dblList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}
	
	@Override
	public List<Integer> getAsIntegerList(String itemName, List<Integer> defaultValue) {
		Object val = get(itemName);
		if (val instanceof Number) {
			return Arrays.asList(((Number) val).intValue());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			boolean numberList=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currObj = valAsList.get(i);
				
				if (currObj instanceof Integer) {
					//ok
				}
				else if (currObj instanceof Number) {
					correctType=false;
					numberList=true;
				}
				else {
					correctType=false;
					numberList=false;
				}
			}
			
			if (correctType) {
				return (List<Integer>) valAsList;
			}
			else if (numberList) {
				List<Integer> intList = new ArrayList<Integer>(valAsList.size());
				for (int i=0; i<valAsList.size(); i++) {
					intList.add(((Number)valAsList.get(i)).intValue());
				}
				return intList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	@Override
	public Long getAsLong(String itemName, Long defaultValue) {
		Object val = get(itemName);
		if (val instanceof Number) {
			return ((Number) val).longValue();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Number) {
					return ((Number) firstVal).longValue();
				}
			}
		}
		return defaultValue;
	}
	
	@Override
	public List<Long> getAsLongList(String itemName, List<Long> defaultValue) {
		Object val = get(itemName);
		if (val instanceof Number) {
			return Arrays.asList(((Number) val).longValue());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			boolean numberList=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currObj = valAsList.get(i);
				
				if (currObj instanceof Long) {
					//ok
				}
				else if (currObj instanceof Number) {
					correctType=false;
					numberList=true;
				}
				else {
					correctType=false;
					numberList=false;
				}
			}
			
			if (correctType) {
				return (List<Long>) valAsList;
			}
			else if (numberList) {
				List<Long> longList = new ArrayList<Long>(valAsList.size());
				for (int i=0; i<valAsList.size(); i++) {
					longList.add(((Number)valAsList.get(i)).longValue());
				}
				return longList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}	
}
