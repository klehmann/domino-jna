package com.mindoo.domino.jna.virtualviews;

import com.mindoo.domino.jna.INoteSummary;

/**
 * Interface for a function that computes the value of a column in a {@link VirtualView}
 * 
 * @param <T> column value type
 */
public interface VirtualViewColumnValueFunction<T> {

	/**
	 * Computes the value of a column in a {@link VirtualView}
	 * 
	 * @param origin origin of the data
	 * @param itemName column item name
	 * @param columnValues note summary data to read other column values (e.g. from formula execution or previous function calls)
	 * @return value or null (String, Number, NotesTimeDate, List&lt;String&gt;, List&lt;Number&gt; or List&lt;NotesTimeDate&gt;)
	 */
	T getValue(String origin, String itemName, INoteSummary columnValues);
	
}
