package com.mindoo.domino.jna.dql;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDbQueryResult;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesTimeDate;

/**
 * Utility class to programmatically compose syntactically correct
 * DQL queries and prevent malicous content injection (we hopefully
 * catch all possible threats like using quote characters in search
 * values).<br>
 * <br>
 * For best code quality, use a static import like this<br>
 * <br>
 * <code>
 * import static com.mindoo.domino.jna.dql.DQL.*;
 * </code>
 * <br><br>
 * to import all static methods of this class.<br>
 * Then you can use methods like {@link #item(String)}, {@link #in(String...)},
 * {@link #inAll(String...)} or {@link #view(String)}
 * directly without the prefix <code>DQL."</code>:<br>
 * <br>
 * <code>
 * DQLTerm dqlQuery = and(<br>
 * &nbsp;&nbsp;item("Lastname").isEqualTo("Abbott"),<br>
 * &nbsp;&nbsp;item("Firstname").isGreaterThan("B")<br>
 * );<br>
 * </code>
 * The returned {@link DQLTerm} object can be passed to one of the query
 * methods in {@link NotesDatabase}, e.g. {@link NotesDatabase#query(DQLTerm, java.util.EnumSet)}
 * to receive a {@link NotesDbQueryResult} containing a {@link NotesIDTable}
 * with note ids of matching documents (and some statics like how long
 * it took to compute the result).<br>
 * See the provided sample code to find out how to project this result
 * onto a view to dynamically sort the documents.<br>
 * <br>
 * To see the resulting DQL query string, simple call {@link DQLTerm#toString()}.
 * 
 * @author Karsten Lehmann
 */
public class DQL {
	/**
	 * Use this method to filter for documents with a specific item
	 * value
	 * 
	 * @param itemName item name
	 * @return object to define the item value and relation (e.g. isEqual to)
	 */
	public static NamedItem item(String itemName) {
		return new NamedItem(itemName);
	}
	
	/**
	 * Use this method to filter for documents with a specific view
	 * column value
	 * 
	 * @param viewName view name
	 * @return object to define which column to filter
	 */
	public static NamedView view(String viewName) {
		return new NamedView(viewName);
	}
	
	/**
	 * Creates a search term to find documents that exist in at least
	 * one of the specified views.
	 * 
	 * @param views view names (V10.0 does not support alias names)
	 * @return DQL term
	 */
	public static InViewsOrFoldersTerm in(String...views) {
		return new InViewsOrFoldersTerm(views, false);
	}
	
	/**
	 * Creates a search term to find documents that exist in multiple
	 * views
	 * 
	 * @param views view names (V10.0 does not support alias names)
	 * @return DQL term
	 */
	public static InViewsOrFoldersTerm inAll(String... views) {
		return new InViewsOrFoldersTerm(views, true);
	}
	
	/**
	 * Base class for a varietly of 
	 * @author Karsten Lehmann
	 */
	public static abstract class DQLTerm {
		
		/**
		 * Returns the term content as DQL query
		 * 
		 * @return DQL
		 */
		public abstract String toString();
	}
	
	public static class InViewsOrFoldersTerm extends DQLTerm {
		private String[] m_viewNames;
		private boolean m_matchAll;
		
		private String m_toString;
		
		private InViewsOrFoldersTerm(String[] viewNames, boolean matchAll) {
			m_viewNames = viewNames;
			m_matchAll = matchAll;
		}
		
		@Override
		public String toString() {
			if (m_toString==null) {
				StringBuilder sb = new StringBuilder();
				sb.append("in ");
				if (m_matchAll) {
					sb.append("all ");
				}
				sb.append("(");
				
				for (int i=0; i<m_viewNames.length; i++) {
					if (i>0) {
						sb.append(", ");
					}
					sb.append("'");
					sb.append(escapeViewName(m_viewNames[i]));
					sb.append("'");
				}
				sb.append(")");
				m_toString = sb.toString();
			}
			return m_toString;
		}
	}
	
	public static class NamedView {
		private String m_viewName;
		
		private NamedView(String viewName) {
			m_viewName = viewName;
		}
		
		public String getViewName() {
			return m_viewName;
		}
		
		/**
		 * Method to define the column for which we want to
		 * filter the value
		 * 
		 * @param columnName view column name
		 * @return object to define the column value and relation (e.g. isEqualTo)
		 */
		public NamedViewColumn column(String columnName) {
			return new NamedViewColumn(this, columnName);
		}
		
		@Override
		public String toString() {
			return m_viewName;
		}
		
	}
	
	public static class NamedViewColumn extends Subject {
		private NamedView m_view;
		private String m_columnName;
		
		private String m_toString;
		
		private NamedViewColumn(NamedView view, String columnName) {
			m_view = view;
			m_columnName = columnName;
		}
		
		public String getColumnName() {
			return m_columnName;
		}
		
		@Override
		public String toString() {
			if (m_toString==null) {
				m_toString = "'" + escapeViewName(m_view.getViewName())+"'."+escapeColumnName(m_columnName);
			}
			return m_toString;
		}
	}
	
	private enum TermRelation {
		EQUAL("="),
		LESSTHAN("<"),
		LESSTHANOREQUAL("<="),
		GREATERTHAN(">"),
		GREATERTHANOREQUAL(">="),
		IN("in"),
		INALL("in all");
		
		private String m_val;
		
		private TermRelation(String val) {
			m_val = val;
		}
		
		public String getValue() {
			return m_val;
		}
	};
		
	private enum SpecialValueType {
		MODIFIEDINTHISFLE("@ModifiedInThisFile"),
		DOCUMENTUNIQUEID("@DocumentUniqueID"),
		CREATED("@Created");
	
		private String m_value;
		
		private SpecialValueType(String value) {
			m_value = value;
		}
		
		public String getValue() {
			return m_value;
		}
	};
	
	/**
	 * Returns a DQL term that matches all documents
	 * 
	 * @return term
	 */
	public static DQLTerm all() {
		return new AllTerm();
	}
	
	/**
	 * Returns a DQL term to do an AND operation on multiple other terms
	 * 
	 * @param terms terms for AND operation
	 * @return AND term
	 */
	public static DQLTerm and(DQLTerm... terms) {
		return new AndTerm(terms);
	}
	
	/**
	 * Returns a DQL term to do an OR operation on multiple other terms
	 * 
	 * @param terms terms for OR operation
	 * @return OR term
	 */
	public static DQLTerm or(DQLTerm... terms) {
		return new OrTerm(terms);
	}
	
	/**
	 * Returns a DQL term to negate the specified term
	 * 
	 * @param term term to negate
	 * @return negated term
	 */
	public static DQLTerm not(DQLTerm term) {
		return new NotTerm(term);
	}
	
	public static class AllTerm extends DQLTerm {

		@Override
		public String toString() {
			return "@all";
		}
	}
	
	public static class NotTerm extends DQLTerm {
		private DQLTerm m_term;
		
		private String m_toString;
		
		private NotTerm(DQLTerm term) {
			m_term = term;
		}
		
		@Override
		public String toString() {
			if (m_toString==null) {
				if (m_term instanceof AndTerm || m_term instanceof OrTerm) {
					m_toString = "not ("+m_term.toString()+")";
				}
				else {
					m_toString = "not "+m_term.toString();
				}
			}
			return m_toString;
		}
	}
	
	public static class AndTerm extends DQLTerm {
		private DQLTerm[] m_terms;
		private String m_toString;
		
		private AndTerm(DQLTerm[] terms) {
			if (terms==null) {
				throw new IllegalArgumentException("And arguments value is null");
			}
			if (terms.length==0) {
				throw new IllegalArgumentException("And arguments value is empty");
			}
			if (terms.length==1) {
				throw new IllegalArgumentException("And arguments value contains one value and must contain at least two");
			}
			m_terms = terms;
		}
		
		@Override
		public String toString() {
			if (m_toString==null) {
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<m_terms.length; i++) {
					if (i>0) {
						sb.append(" and ");
					}
					
					if (m_terms[i] instanceof OrTerm) {
						sb.append("(");
						sb.append(m_terms[i].toString());
						sb.append(")");
					}
					else if (m_terms[i] instanceof AndTerm) {
						sb.append(m_terms[i].toString());
					}
					else {
						sb.append(m_terms[i]);
					}
				}
				m_toString = sb.toString();
			}
			return m_toString;
		}
	}
	
	public static class OrTerm extends DQLTerm {
		private DQLTerm[] m_terms;
		private String m_toString;
		
		private OrTerm(DQLTerm[] terms) {
			if (terms==null) {
				throw new IllegalArgumentException("Or arguments value is null");
			}
			if (terms.length==0) {
				throw new IllegalArgumentException("Or arguments value is empty");
			}
			if (terms.length==1) {
				throw new IllegalArgumentException("Or arguments value contains one value and must contain at least two");
			}
			m_terms = terms;
		}
		
		@Override
		public String toString() {
			if (m_toString==null) {
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<m_terms.length; i++) {
					if (i>0) {
						sb.append(" or ");
					}
					
					if (m_terms[i] instanceof OrTerm) {
						sb.append(m_terms[i].toString());
					}
					else if (m_terms[i] instanceof AndTerm) {
//						sb.append("(");
						sb.append(m_terms[i].toString());
//						sb.append(")");
					}
					else {
						sb.append(m_terms[i]);
					}
				}
				m_toString = sb.toString();
			}
			return m_toString;
		}

	}
	
	/**
	 * Use this method to filter by @ModifiedInThisFile value
	 * 
	 * @return object to define a date value and relation (e.g. isGreaterThan / isLess)
	 */
	public static SpecialValue modifiedInThisFile() {
		return new SpecialValue(SpecialValueType.MODIFIEDINTHISFLE);
	}

	/**
	 * Use this method to filter by @DocumentUniqueId value
	 * 
	 * @return object to define the UNID and isEqual relation
	 */
	public static SpecialValue documentUniqueId() {
		return new SpecialValue(SpecialValueType.DOCUMENTUNIQUEID);
	}

	/**
	 * Use this method to filter by @Created value
	 * 
	 * @return object to define the creation date to compare with and relation (e.g. isGreaterThan)
	 */
	public static SpecialValue created() {
		return new SpecialValue(SpecialValueType.CREATED);
	}

	public static class SpecialValue extends NamedItem {
		private SpecialValueType m_type;
		
		private SpecialValue(SpecialValueType type) {
			super(type.getValue());
			m_type = type;
		}
		
		public SpecialValueType getType() {
			return m_type;
		}
		
		@Override
		public String toString() {
			return m_type.getValue();
		}
	}
	
	private static class Subject {
		public ValueComparisonTerm isEqualTo(String strVal) {
			return new ValueComparisonTerm(this, TermRelation.EQUAL, strVal);
		}
		
		public ValueComparisonTerm isLessThan(String strVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHAN, strVal);
		}

		public ValueComparisonTerm isLessThanOrEqual(String strVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHANOREQUAL, strVal);
		}

		public ValueComparisonTerm isGreaterThan(String strVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHAN, strVal);
		}

		public ValueComparisonTerm isGreaterThanOrEqual(String strVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHANOREQUAL, strVal);
		}

		public ValueComparisonTerm isEqualTo(int numVal) {
			return new ValueComparisonTerm(this, TermRelation.EQUAL, Integer.valueOf(numVal));
		}

		public ValueComparisonTerm isLessThan(int numVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHAN, Integer.valueOf(numVal));
		}

		public ValueComparisonTerm isLessThanOrEqual(int numVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHANOREQUAL, Integer.valueOf(numVal));
		}

		public ValueComparisonTerm isGreaterThan(int numVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHAN, Integer.valueOf(numVal));
		}

		public ValueComparisonTerm isGreaterThanOrEqual(int numVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHANOREQUAL, Integer.valueOf(numVal));
		}

		public ValueComparisonTerm isEqualTo(double numVal) {
			return new ValueComparisonTerm(this, TermRelation.EQUAL, Double.valueOf(numVal));
		}

		public ValueComparisonTerm isLessThan(double numVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHAN, Double.valueOf(numVal));
		}

		public ValueComparisonTerm isLessThanOrEqual(double numVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHANOREQUAL, Double.valueOf(numVal));
		}

		public ValueComparisonTerm isGreaterThan(double numVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHAN, Double.valueOf(numVal));
		}

		public ValueComparisonTerm isGreaterThanOrEqual(double numVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHANOREQUAL, Double.valueOf(numVal));
		}

		public ValueComparisonTerm in(String... strValues) {
			if (strValues==null)
				throw new IllegalArgumentException("Values ist cannot be null");
			if (strValues.length==0)
				throw new IllegalArgumentException("Values ist cannot be empty");
			
			return new ValueComparisonTerm(this, TermRelation.IN, strValues);
		}
		
		public ValueComparisonTerm in(int... intValues) {
			if (intValues==null)
				throw new IllegalArgumentException("Values ist cannot be null");
			if (intValues.length==0)
				throw new IllegalArgumentException("Values ist cannot be empty");
			
			return new ValueComparisonTerm(this, TermRelation.IN, intValues);
		}
		
		public ValueComparisonTerm in(double... dblValues) {
			if (dblValues==null)
				throw new IllegalArgumentException("Values ist cannot be null");
			if (dblValues.length==0)
				throw new IllegalArgumentException("Values ist cannot be empty");
			
			return new ValueComparisonTerm(this, TermRelation.IN, dblValues);
		}
		
		public ValueComparisonTerm isEqualTo(Date dtVal) {
			return new ValueComparisonTerm(this, TermRelation.EQUAL, dtVal);
		}
		
		public ValueComparisonTerm isLessThan(Date dtVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHAN, dtVal);
		}

		public ValueComparisonTerm isLessThanOrEqual(Date dtVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHANOREQUAL, dtVal);
		}

		public ValueComparisonTerm isGreaterThanOrEqual(Date dtVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHANOREQUAL, dtVal);
		}

		public ValueComparisonTerm isGreaterThan(Date dtVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHAN, dtVal);
		}

		public ValueComparisonTerm isEqualTo(NotesTimeDate tdVal) {
			return new ValueComparisonTerm(this, TermRelation.EQUAL, tdVal);
		}
		
		public ValueComparisonTerm isLessThan(NotesTimeDate tdVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHAN, tdVal);
		}

		public ValueComparisonTerm isLessThanOrEqual(NotesTimeDate tdVal) {
			return new ValueComparisonTerm(this, TermRelation.LESSTHANOREQUAL, tdVal);
		}

		public ValueComparisonTerm isGreaterThanOrEqual(NotesTimeDate tdVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHANOREQUAL, tdVal);
		}

		public ValueComparisonTerm isGreaterThan(NotesTimeDate tdVal) {
			return new ValueComparisonTerm(this, TermRelation.GREATERTHAN, tdVal);
		}

	}
	
	public static class NamedItem extends Subject {
		private String m_itemName;
		
		private NamedItem(String itemName) {
			m_itemName = itemName;
		}
		
		public String getName() {
			return m_itemName;
		}

		@Override
		public String toString() {
			return escapeItemName(m_itemName);
		}
	}
	
	public static class ValueComparisonTerm extends DQLTerm {
		private Subject m_subject;
		private TermRelation m_relation;
		private Object m_value;
		
		private String m_toString;
		
		private ValueComparisonTerm(Subject item, TermRelation relation, Object value) {
			m_subject = item;
			m_relation = relation;
			m_value = value;
		}
		
		@Override
		public String toString() {
			if (m_toString==null) {
				StringBuilder sb = new StringBuilder();
				
				sb.append(m_subject.toString());
				
				sb.append(" ");
				sb.append(m_relation.getValue());
				sb.append(" ");
				
				if (m_relation == TermRelation.IN) {
					sb.append("(");
				}

				if (m_value instanceof String[]) {
					String[] strValues = (String[]) m_value;
					for (int i=0; i<strValues.length; i++) {
						if (i>0) {
							sb.append(", ");
						}
						sb.append("'");
						sb.append(escapeStringValue(strValues[i]));
						sb.append("'");
					}
				}
				else if (m_value instanceof int[]) {
					int[] intValues = (int[]) m_value;
					
					for (int i=0; i<intValues.length; i++) {
						if (i>0) {
							sb.append(", ");
						}
						sb.append(Integer.toString(intValues[i]));
					}
				}
				else if (m_value instanceof double[]) {
					double[] dblValues = (double[]) m_value;
					
					for (int i=0; i<dblValues.length; i++) {
						if (i>0) {
							sb.append(", ");
						}
						sb.append(formatDoubleValue(dblValues[i]));
					}
				}
				else if (m_value instanceof Date[]) {
					Date[] dateValues = (Date[]) m_value;
					
					for (int i=0; i<dateValues.length; i++) {
						if (i>0) {
							sb.append(", ");
						}
						sb.append(formatDateValue(dateValues[i]));
					}
				}
				else if (m_value instanceof NotesTimeDate[]) {
					NotesTimeDate[] tdValues = (NotesTimeDate[]) m_value;
					
					for (int i=0; i<tdValues.length; i++) {
						if (i>0) {
							sb.append(", ");
						}
						sb.append(formatNotesTimeDateValue(tdValues[i]));
					}
				}
				else if (m_value instanceof String) {
					sb
					.append("'")
					.append(escapeStringValue((String) m_value))
					.append("'");
				}
				else if (m_value instanceof Integer) {
					sb.append(m_value);
				}
				else if (m_value instanceof Double) {
					sb.append(formatDoubleValue(((Double)m_value).doubleValue()));
				}
				else if (m_value instanceof Date) {
					sb.append(formatDateValue((Date) m_value));
				}
				else if (m_value instanceof NotesTimeDate) {
					sb.append(formatNotesTimeDateValue((NotesTimeDate) m_value));
				}
				else {
					throw new IllegalArgumentException("Unknown value found: "+m_value+" (type="+(m_value==null ? "null" : m_value.getClass().getName()+")"));
				}
				
				if (m_relation == TermRelation.IN) {
					sb.append(")");
				}

				m_toString = sb.toString();
			}
			return m_toString;
		}
	}
	
	private static String escapeItemName(String itemName) {
		if (itemName.contains(" ")) {
			throw new IllegalArgumentException("Unexpected whitespace found in item name: "+itemName);
		}
		if (itemName.contains("'")) {
			throw new IllegalArgumentException("Unexpected quote character in item name: "+itemName);
		}
		if (itemName.contains("\"")) {
			throw new IllegalArgumentException("Unexpected quote character in item name: "+itemName);
		}
		
		return itemName;
	}
	
	private static String escapeViewName(String viewName) {
		if (viewName.contains("'")) {
			throw new IllegalArgumentException("Unexpected quote character in view name: "+viewName);
		}
		if (viewName.contains("\"")) {
			throw new IllegalArgumentException("Unexpected quote character in view name: "+viewName);
		}
		return viewName.replace("\\", "\\\\");
	}
	
	private static String escapeColumnName(String columnName) {
		if (columnName.contains(" ")) {
			throw new IllegalArgumentException("Unexpected whitespace found in view name: "+columnName);
		}
		if (columnName.contains("'")) {
			throw new IllegalArgumentException("Unexpected quote character in view name: "+columnName);
		}
		if (columnName.contains("\"")) {
			throw new IllegalArgumentException("Unexpected quote character in view name: "+columnName);
		}
		return columnName.replace("\\", "\\\\");
	}
	
	private static String escapeStringValue(String strVal) {
		if (strVal.contains("\n")) {
			throw new IllegalArgumentException("Unexpected newline character in string value: "+strVal);
		}
		
		return strVal
				.replace("'", "''");
	}
	
	private static String formatDoubleValue(double dblValue) {
		return Double.toString(dblValue);
	}
	
    private static DateFormat RFC3339_PATTERN_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static DateFormat RFC3339_PATTERN_DATE = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat RFC3339_PATTERN_TIME = new SimpleDateFormat("HH:mm:ss");
    
    private static String toRFC3339(Date dt) {
    	StringBuilder sb = new StringBuilder();
    	sb.append(RFC3339_PATTERN_DATETIME.format(dt));
    	
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt);
        cal.setTimeZone(TimeZone.getDefault());
        
        int offsetMillis = cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET);
        int offsetHours = Math.abs(offsetMillis/(1000*60*60));
        int offsetMinutes = Math.abs((offsetMillis/(1000*60))%60);

        if (offsetMillis==0) {
            sb.append("Z");
        }
        else {
            sb.append((offsetMillis>0) ? "+" : "-");
            if (offsetHours<10) {
            	sb.append("0");
            }
            sb.append(offsetHours);
            sb.append(":");
            if (offsetMinutes<10) {
            	sb.append("0");
            }
            sb.append(offsetMinutes);
        } 
 
        return sb.toString(); 
    }
    
	private static String formatDateValue(Date dateValue) {
		return "@dt('" + toRFC3339(dateValue) + "')";
	}
	
	private static String formatNotesTimeDateValue(NotesTimeDate tdValue) {
		if (tdValue.hasDate()) {
			if (tdValue.hasTime()) {
				return formatDateValue(tdValue.toDate());
			}
			else {
				return RFC3339_PATTERN_DATE.format(tdValue.toDate());
			}
		}
		else {
			if (tdValue.hasTime()) {
				return RFC3339_PATTERN_TIME.format(tdValue.toDate());
			}
			else {
				throw new IllegalArgumentException("NotesTimeDate has no date and no time");
			}
		}
	}

}
