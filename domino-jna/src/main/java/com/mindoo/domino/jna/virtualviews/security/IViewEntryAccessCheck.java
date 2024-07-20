package com.mindoo.domino.jna.virtualviews.security;

import com.mindoo.domino.jna.virtualviews.VirtualViewEntryData;
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator;

/**
 * Interface for a class that checks if a user has read access to a view entry
 */
public interface IViewEntryAccessCheck {

	/**
	 * Checks if the user has read access to the provided entry
	 * 
	 * @param nav navigator to access the view data
	 * @param entry entry to check
	 * @return true if the user has read access
	 */
	boolean isVisible(VirtualViewNavigator nav, VirtualViewEntryData entry);
	
}
