package com.mindoo.domino.jna.virtualviews.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.virtualviews.VirtualViewEntryData;

public class ViewEntryAccessCheck {
	private String userName;
	private Map<String,Set<String>> userNamesListByOrigin;
	
	public static ViewEntryAccessCheck forUser(String userName) {
		return new ViewEntryAccessCheck(userName);
	}
	
	private ViewEntryAccessCheck(String userName) {
		this.userName = userName;
		this.userNamesListByOrigin = new HashMap<>();
	}
	
	public String getUserName() {
		return userName;
	}
	
	/**
	 * Checks if the user has read access to the provided entry
	 * 
	 * @param entry entry to check
	 * @return true if the user has read access
	 */
	public boolean isVisible(VirtualViewEntryData entry) {
		if (!entry.isDocument()) {
			//categories are always visible
			return true;
		}
		
		List<String> readersList = entry.getReadersList();
		if (readersList == null || readersList.contains("*")) {
			return true;
		}
		
		String origin = entry.getOrigin();
		Set<String> userNamesList = userNamesListByOrigin.get(origin);
		if (userNamesList == null) {
			return false;
		}

		for (String currReader : readersList) {
			if (userNamesList.contains(currReader)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Computes the name variants, groups and roles of the user in the provided database
	 * and stores them as access rights for the origins
	 * 
	 * @param db database to check access against
	 * @param origins origins for which the user should have access
	 * @return this instance
	 */
	public ViewEntryAccessCheck addDbUserNamesListForOrigins(NotesDatabase db, String... origins) {
		TreeSet<String> userNamesList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		List<String> nameVariants = NotesNamingUtils.getUserNamesList(db.getServer(), userName);
		userNamesList.addAll(nameVariants);
		
		List<String> roles = db.queryAccessRoles(userName);
		userNamesList.addAll(roles);
		
		for (String currOrigin : origins) {
			userNamesListByOrigin.put(currOrigin, userNamesList);			
		}
		return this;
	}
	
	/**
	 * Adds a manual list of name variants, groupd and roles for the user for the provided origins
	 * 
	 * @param userNamesList list of name variants, groups and roles (will be converted to a case-insensitive set)
	 * @param origins origins for which the user should have access
	 * @return this instance
	 */
	public ViewEntryAccessCheck addDbUserNamesListForOrigins(List<String> userNamesList, String... origins) {
		TreeSet<String> userNamesListIgnoreCase = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		userNamesListIgnoreCase.addAll(userNamesListIgnoreCase);
		
		for (String currOrigin : origins) {
			userNamesListByOrigin.put(currOrigin, userNamesListIgnoreCase);
		}
		return this;
	}
}
