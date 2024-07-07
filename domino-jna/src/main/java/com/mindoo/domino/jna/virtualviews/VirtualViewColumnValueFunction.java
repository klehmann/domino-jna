package com.mindoo.domino.jna.virtualviews;

import com.mindoo.domino.jna.INoteSummary;

/**
 * Interface for a function that computes the value of a column in a {@link VirtualView}
 * 
 * @param <T> column value type
 */
public abstract class VirtualViewColumnValueFunction<T> {
	private int m_version;
	
	public VirtualViewColumnValueFunction(int version) {
		m_version = version;
	}
	
	/**
	 * Returns the version of this function. If the function implementation changes, the version
	 * should be increased to make caching and reusing of the {@link VirtualView} instances work correctly
	 * 
	 * @return version
	 */
	public final int getVersion() {
		return m_version;
	}
	
	/**
	 * Computes the value of a column in a {@link VirtualView}
	 * 
	 * @param origin origin of the data
	 * @param itemName column item name
	 * @param columnValues note summary data to read other column values (e.g. from formula execution or previous function calls)
	 * @return value or null (String, Number, NotesTimeDate, List&lt;String&gt;, List&lt;Number&gt; or List&lt;NotesTimeDate&gt;)
	 */
	public abstract T getValue(String origin, String itemName, INoteSummary columnValues);
	
}
