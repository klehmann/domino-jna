package com.mindoo.domino.jna.virtualviews.security;

import java.util.Collection;
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
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator;
import com.mindoo.domino.jna.virtualviews.dataprovider.AbstractNSFVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.IVirtualViewDataProvider;

/**
 * Class that checks if a user has read access to a view entry by comparing the user's name variants, groups and roles
 * with the computed readers list of the entry.<br>
 * <br>
 * During view indexing. we collect all reader lists of documents and accumulate them for each origin database
 * in the parent categories (with their count) up until the root entry. We also count how many docs there are that have no reader
 * items at all. This allows us to quickly check if a user has access to a category by checking if there are any
 * descendants without reader items.<br>
 * 
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
	 */
	public ViewEntryAccessCheck(VirtualView view, String effectiveUserName) {
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
	
	protected VirtualView getView() {
		return view;
	}
	
	public String getUserName() {
		return effectiveUserName;
	}
	
	@Override
	public boolean isVisible(VirtualViewNavigator nav, VirtualViewEntryData entry) {
		if (entry.isDocument()) {
			String origin = entry.getOrigin();
			
			//check general DB access level of the user
			AclLevel aclLevel = dbAccessLevelsByOrigin.get(origin);
			if (aclLevel == null || aclLevel.getValue() < AclLevel.READER.getValue()) {
				return false;
			}
			
			Collection<String> readersList = entry.getDocReadersList();
			if (readersList == null || readersList.contains("*")) {
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
		else if (entry.isCategory()) {
			if (!nav.isDontShowEmptyCategories()) {
				//show all categories
				return true;
			}
			
			//fast check if there are any descendants without reader items
			Set<String> origins = entry.getCategoryReadersListOrigins();
			
			if (entry.getDescendantCountWithoutReaders() > 0) {
				//check if the user has read access to all origins
				boolean accessToAllDbs = true;
				
				for (String currOrigin : origins) {
					AclLevel aclLevel = dbAccessLevelsByOrigin.get(currOrigin);
					if (aclLevel != null && aclLevel.getValue() < AclLevel.READER.getValue()) {
						accessToAllDbs = false;
						break;
					}
				}
				
				if (accessToAllDbs) {
					return true;
				}
				
				//go on with slower check
			}
			
			//slower check: for all origins, check if the user can see any entry
			
			for (String currOrigin : origins) {
				// check general DB access level of the user
				AclLevel aclLevel = dbAccessLevelsByOrigin.get(currOrigin);
				if (aclLevel != null && aclLevel.getValue() > AclLevel.DEPOSITOR.getValue()) {
					Set<String> readersForOrigin = entry.getCategoryReadersList(currOrigin);
					if (readersForOrigin != null) {
						if (readersForOrigin.contains("*")) {
							return true;
						}
						
						Set<String> userNamesList = userNamesListByOrigin.get(currOrigin);
						if (userNamesList != null) {
							for (String currReader : readersForOrigin) {
								if (userNamesList.contains(currReader)) {
									return true;
								}
							}
						}
					}					
				}
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
