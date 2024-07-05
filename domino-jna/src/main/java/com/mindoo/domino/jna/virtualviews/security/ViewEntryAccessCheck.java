package com.mindoo.domino.jna.virtualviews.security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mindoo.domino.jna.NotesACL;
import com.mindoo.domino.jna.NotesACL.NotesACLAccess;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewEntryData;
import com.mindoo.domino.jna.virtualviews.dataprovider.AbstractNSFVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.IVirtualViewDataProvider;

/**
 * Class that checks if a user has read access to a view entry by comparing the user's name variants, groups and roles
 * with the computed readers list of the entry
 */
public class ViewEntryAccessCheck implements IViewEntryAccessCheck {
	private VirtualView view;
	private String effectiveUserName;
	private Map<String,Set<String>> userNamesListByOrigin;
	private Map<String,AclLevel> dbAccessLevelsByOrigin;
	
	/**
	 * Creates a new instance
	 * 
	 * @param view virtual view
	 * @param effectiveUserName name of the user to check access for
	 * @return access check instance
	 */
	public static ViewEntryAccessCheck forUser(VirtualView view, String effectiveUserName) {
		return new ViewEntryAccessCheck(view, effectiveUserName);
	}
	
	private ViewEntryAccessCheck(VirtualView view, String effectiveUserName) {
		this.view = view;
		this.effectiveUserName = effectiveUserName;
		this.userNamesListByOrigin = new HashMap<>();
		this.dbAccessLevelsByOrigin = new HashMap<>();
		
		//collect the usernames lists for the user in all databases
		Iterator<IVirtualViewDataProvider> dataProvidersIt = view.getDataProviders();
		while (dataProvidersIt.hasNext()) {
			IVirtualViewDataProvider currProvider = dataProvidersIt.next();
			if (currProvider instanceof AbstractNSFVirtualViewDataProvider) {
				NotesDatabase db = ((AbstractNSFVirtualViewDataProvider) currProvider).getDatabase();
				NotesACL acl = db.getACL();
				NotesACLAccess aclAccess = acl.lookupAccess(effectiveUserName);
				dbAccessLevelsByOrigin.put(currProvider.getOrigin(), aclAccess.getAclLevel());
				
				addDbUserNamesListForOrigins(db, currProvider.getOrigin());
			}
		}
	}
	
	public String getUserName() {
		return effectiveUserName;
	}
	
	@Override
	public boolean isVisible(VirtualViewEntryData entry) {
		if (!entry.isDocument()) {
			//categories are always visible
			return true;
		}
		
		String origin = entry.getOrigin();

		//check general DB access level of the user
		AclLevel aclLevel = dbAccessLevelsByOrigin.get(origin);
		if (aclLevel == null || aclLevel == AclLevel.NOACCESS || aclLevel == AclLevel.DEPOSITOR) {
			return false;
		}
		
		List<String> readersList = entry.getReadersList();
		if (readersList == null || readersList.contains("*")) {
			return true;
		}
		if (readersList != null && readersList.size() == 1 && readersList.get(0).equals("$P")) {
			//we had this value when searching through profile docs with NSFSearchExtended3
			return true;
		}
		
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
		List<String> nameVariants = NotesNamingUtils.getUserNamesList(db.getServer(), effectiveUserName);
		userNamesList.addAll(nameVariants);
		
		List<String> roles = db.queryAccessRoles(effectiveUserName);
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
