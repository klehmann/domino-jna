package com.mindoo.domino.jna.test.directorycache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesSearch.SearchCallback;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.StringUtil;

/**
 * Cache to store persons of a NAB database in an in-memory map, uses incremental NSF searches
 * to keep the cache up to date and provides very fast name lookups.
 * 
 * @author Karsten Lehmann
 */
public class DirectoryCache {
	public static final String ITEMS_USER_LASTNAME = "Lastname";
	public static final String ITEMS_USER_FIRSTNAME = "Firstname";
	public static final String ITEMS_USER_EMAIL = "InternetAddress";
	public static final String ITEMS_USER_CELLPHONENUMBER = "CellPhoneNumber";
	public static final String ITEMS_USER_PHONENUMBER = "PhoneNumber";
	public static final String ITEMS_USER_CITY = "City";
	public static final String ITEMS_USER_ZIP = "Zip";
	public static final String ITEMS_USER_STREET = "StreetAddress";
	public static final String ITEMS_USER_TITLE = "Title";
	public static final String ITEMS_USER_FULLNAME = "Fullname";

	private String m_serverAbbr;
	private String m_filePath;

	private LinkedHashMap<String,String> m_columns;

	//our cache, currently only hashed by UNID; could be enhanced with additional
	//lookup names that get updated in the addtoCache / removeFromCache methods;
	//for simplicity we scan the whole cache map in this sample when doing name lookups
	private Map<String,DirectoryUser> m_cachedDirectoryUsersByUNID;
	// R/W lock to coordinate cache access between readers and writers
	private ReadWriteLock m_cachedDirectoryUsersByUNIDLock;

	private NotesTimeDate m_lastSyncTimeDate;

	public DirectoryCache(String serverAbbr, String filePath) {
		m_serverAbbr = serverAbbr;
		m_filePath = filePath;

		m_cachedDirectoryUsersByUNID = new HashMap<String,DirectoryUser>();
		m_cachedDirectoryUsersByUNIDLock = new ReentrantReadWriteLock();
	}

	/**
	 * Returns the data we want to read for each document during our NSF search
	 * 
	 * @return lookup values
	 */
	private LinkedHashMap<String,String> getDirectoryUserColumns() {
		if (m_columns==null) {
			m_columns = new LinkedHashMap<String,String>();
			//value="" for normal document fields:
			m_columns.put(ITEMS_USER_EMAIL, "");
			m_columns.put(ITEMS_USER_CELLPHONENUMBER, "");
			m_columns.put(ITEMS_USER_PHONENUMBER, "");
			m_columns.put(ITEMS_USER_CITY, "");
			m_columns.put(ITEMS_USER_ZIP, "");
			m_columns.put(ITEMS_USER_STREET, "");
			m_columns.put(ITEMS_USER_LASTNAME, "");
			m_columns.put(ITEMS_USER_FIRSTNAME, "");
			m_columns.put(ITEMS_USER_TITLE, "");
			m_columns.put(ITEMS_USER_FULLNAME, "");

			//computed value used in our find methods to search for fullname and email field content
			m_columns.put("lookupnames", "@Trim(@Unique("
					+ "@Name([Abbreviate]; FullName):FullName" + 
					":AltFullName:@Name([Abbreviate]; AltFullName):@Name([CN];AltFullName)" + 
					":InternetAddress"
					+ "))");

		}

		return m_columns;
	}

	/**
	 * Returns the Domino database to read and write config data
	 * 
	 * @return database
	 */
	public NotesDatabase getDatabase() {
		String cacheKey = DirectoryCache.class.getName()+".db."+m_serverAbbr+"!!"+m_filePath;

		//don't reopen the NSF instance on each call, keep it in our NotesGC custom values
		//for the duration of the current NotesGC.runWithAutoGC block, e.g. for the duration of
		//the HTTP request processing
		NotesDatabase db = (NotesDatabase) NotesGC.getCustomValue(cacheKey);
		if (db==null || db.isRecycled()) {
			try {
				//open database as the ID user
				db = new NotesDatabase(m_serverAbbr, m_filePath, (String) null);
			}
			catch (NotesError e) {
				throw new RuntimeException("Error opening config database at "+m_serverAbbr+"!!"+m_filePath+"'", e);
			}
			NotesGC.setCustomValue(cacheKey, db);
		}
		return db;
	}

	/**
	 * Searches for a {@link DirectoryUser} by its email address or username
	 * 
	 * @param emailOrUserName email address or username
	 * @return user or null if not found
	 * @throws Exception
	 */
	public DirectoryUser findUserByEmailOrUserName(String emailOrUserName) {
		hashDirectoryUsers(false);

		m_cachedDirectoryUsersByUNIDLock.readLock().lock();
		try {
			for (Entry<String,DirectoryUser> currEntry : m_cachedDirectoryUsersByUNID.entrySet()) {
				DirectoryUser currUser = currEntry.getValue();
				List<String> lookupNames = currUser.getLookupNames();
				if (lookupNames!=null) {
					for (int i=0; i<lookupNames.size(); i++) {
						String currLookupName = lookupNames.get(i);
						if (emailOrUserName.equalsIgnoreCase(currLookupName)) {
							return currUser;
						}
					}
				}
			}
		}
		finally {
			m_cachedDirectoryUsersByUNIDLock.readLock().unlock();
		}
		return null;
	}

	public void hashDirectoryUsers(boolean enforceRefresh) {
		String cacheKey = null;

		if (!enforceRefresh) {
			cacheKey = DirectoryCache.class.getName()+".db."+m_serverAbbr+"!!"+m_filePath+".synced";
			if (Boolean.TRUE.equals(NotesGC.getCustomValue(cacheKey))) {
				//already synced
				return;
			}
		}

		NotesDatabase db = getDatabase();

		//search all person documents
		final String formula = "Type = \"Person\"";

		long t0=System.currentTimeMillis();
		final AtomicInteger added = new AtomicInteger();
		final AtomicInteger removed = new AtomicInteger();

		boolean isFirstSync = m_lastSyncTimeDate == null;

		//make this this search does not run in parallel; NSFSearch returns very quickly
		//when there is nothing to do
		synchronized (this) {
			LinkedHashMap<String,String> lookupColumns = getDirectoryUserColumns();

			NotesTimeDate newSyncTimeDate = NotesSearch.search(db, null, formula, lookupColumns,
					"-", EnumSet.of(Search.SUMMARY, Search.ALL_VERSIONS, Search.NOTIFYDELETIONS), EnumSet.of(NoteClass.DATA, NoteClass.NOTIFYDELETION),
					m_lastSyncTimeDate, new SearchCallback() {

				@Override
				public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
						IItemTableData summaryBufferData) {

					added.incrementAndGet();
					//overwrites any previously entries for this UNID in the cache:
					addToCache(parentDb, searchMatch, summaryBufferData);
					return Action.Continue;
				}

				@Override
				public Action deletionStubFound(NotesDatabase parentDb, ISearchMatch searchMatch,
						IItemTableData summaryBufferData) {

					//note has been deleted in the database; can be any document, but make
					//sure to have it removed from the cache if it's in there
					DirectoryUser oldUser = removeFromCache(searchMatch, summaryBufferData);
					if (oldUser!=null) {
						removed.incrementAndGet();
					}
					return Action.Continue;
				}

				@Override
				public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, ISearchMatch searchMatch,
						IItemTableData summaryBufferData) {

					//note may have changed so that it does not match the search formula anymore;
					//make sure to have it removed from the cache
					DirectoryUser oldUser = removeFromCache(searchMatch, summaryBufferData);
					if (oldUser!=null) {
						removed.incrementAndGet();
					}
					return Action.Continue;
				}

			});
			m_lastSyncTimeDate = newSyncTimeDate;

			if (cacheKey==null) {
				cacheKey = DirectoryCache.class.getName()+".db."+m_serverAbbr+"!!"+m_filePath+".synced";
			}
			NotesGC.setCustomValue(cacheKey, Boolean.TRUE);
		}
		long t1=System.currentTimeMillis();

		if (isFirstSync) {
			System.out.println("Mindoo DirectoryCache: Finished initial directory fetch of "+m_serverAbbr+"!!"+m_filePath+" after "+(t1-t0)+"ms, "+added.get()+" users added");
		}
	}

	private void addToCache(NotesDatabase parentDb, ISearchMatch searchMatch,
			IItemTableData summaryBufferData) {

		//make sure we have exclusive access to the cache (locks our readers)
		m_cachedDirectoryUsersByUNIDLock.writeLock().lock();
		try {
			String unid = searchMatch.getUNID();

			String title = summaryBufferData.getAsString(ITEMS_USER_TITLE, "");
			String firstname = summaryBufferData.getAsString(ITEMS_USER_FIRSTNAME, "");
			String lastname = summaryBufferData.getAsString(ITEMS_USER_LASTNAME, "");
			String fullname = summaryBufferData.getAsString(ITEMS_USER_FULLNAME, "");
			String email = summaryBufferData.getAsString(ITEMS_USER_EMAIL, "");
			String mobile = summaryBufferData.getAsString(ITEMS_USER_CELLPHONENUMBER, "");
			String phone = summaryBufferData.getAsString(ITEMS_USER_PHONENUMBER, "");
			String street = summaryBufferData.getAsString(ITEMS_USER_STREET, "");
			String zip = summaryBufferData.getAsString(ITEMS_USER_ZIP, "");
			String city = summaryBufferData.getAsString(ITEMS_USER_CITY, "");

			List<String> lookupNames = summaryBufferData.getAsStringList("lookupnames", null);
			if (lookupNames==null) {
				lookupNames = Collections.emptyList();
			}

			DirectoryUser dirUser = new DirectoryUser()
					.setId(unid)
					.setSalutation(title)
					.setFirstName(firstname)
					.setLastName(lastname)
					.setFullName(fullname)
					.setEmail(email)
					.setMobile(mobile)
					.setPhone(phone)
					.setStreet(street)
					.setZip(zip)
					.setCity(city)
					.setLookupNames(lookupNames);

			m_cachedDirectoryUsersByUNID.put(unid, dirUser);
		}
		finally {
			m_cachedDirectoryUsersByUNIDLock.writeLock().unlock();
		}
	}

	private DirectoryUser removeFromCache(ISearchMatch searchMatch,
			IItemTableData summaryBufferData) {

		m_cachedDirectoryUsersByUNIDLock.writeLock().lock();
		try {
			return m_cachedDirectoryUsersByUNID.remove(searchMatch.getUNID());
		}
		finally {
			m_cachedDirectoryUsersByUNIDLock.writeLock().unlock();
		}
	}

	/**
	 * Method to look up directory users by Notes name or email address
	 * 
	 * @param lookupKeys lookup keys; we do a lookup for each key
	 * @param start start index
	 * @param count number of elements to return
	 * @return elements
	 */
	public List<DirectoryUser> lookupNames(final List<String> lookupKeys, final int start, final int count) {
		TreeMap<String,DirectoryUser> lookupResultByFullName = new TreeMap<String,DirectoryUser>(String.CASE_INSENSITIVE_ORDER);

		for (String currLkKey : lookupKeys) {
			DirectoryUser dirUser = findUserByEmailOrUserName(currLkKey);
			if (dirUser!=null) {
				lookupResultByFullName.put(dirUser.getFullName(), dirUser);
			}
		}

		List<DirectoryUser> matchingUsers = new ArrayList<DirectoryUser>(lookupResultByFullName.values());
		sortDirectoryUsers(matchingUsers);

		List<DirectoryUser> matchingUsersOnPage = subListChecked(matchingUsers, start, count);
		return matchingUsersOnPage;
	}

	/**
	 * Method to return a sublist from a list based on the start index <code>start</code> and
	 * the number of entries to return <code>count</code>. In constrast to {@link List#subList(int, int)},
	 * this implementation is forgiving in case <code>start</code> or <code>start+count</code> is higher than
	 * the actual number of list entries.
	 * 
	 * @param <T> list type
	 * @param list list
	 * @param start start index
	 * @param count number of entries to return
	 * @return sublist, backed by the original list; see {@link List#subList(int, int)} for details
	 */
	public static <T> List<T> subListChecked(List<T> list, int start, int count) {
		if (start > list.size())
			return Collections.emptyList();
		else {
			long startLong = (long) start;
			long countLong = (long) count;
			//make sure we do not exceed Integer.MAX_VALUE
			long sum = Math.min(Integer.MAX_VALUE, startLong + countLong);

			return list.subList(start, Math.min(list.size(), (int) sum));
		}
	}

	/**
	 * Method to do a typeahead lookup
	 * 
	 * @param namePattern prefix to search for
	 * @param start start index
	 * @param count max number of elements to return
	 * @return lookup result
	 */
	public NameLookupResult lookupNames(final String namePattern, final int start, final int count) {
		hashDirectoryUsers(false);

		TreeMap<String,DirectoryUser> lookupResultByFullName = new TreeMap<String,DirectoryUser>(String.CASE_INSENSITIVE_ORDER);

		//multiple readers can read concurrently
		m_cachedDirectoryUsersByUNIDLock.readLock().lock();
		try {
			for (Entry<String,DirectoryUser> currEntry : m_cachedDirectoryUsersByUNID.entrySet()) {
				DirectoryUser currUser = currEntry.getValue();
				List<String> lookupNames = currUser.getLookupNames();
				if (lookupNames!=null) {
					for (int i=0; i<lookupNames.size(); i++) {
						String currLookupName = lookupNames.get(i);
						if (StringUtil.startsWithIgnoreCase(currLookupName, namePattern)) {
							lookupResultByFullName.put(currUser.getFullName(), currUser);
							break;
						}
					}
				}
			}
		}
		finally {
			m_cachedDirectoryUsersByUNIDLock.readLock().unlock();
		}

		List<DirectoryUser> matchingUsers = new ArrayList<DirectoryUser>(lookupResultByFullName.values());
		sortDirectoryUsers(matchingUsers);

		int total = matchingUsers.size();

		List<DirectoryUser> matchingUsersOnPage = subListChecked(matchingUsers, start, count);

		NameLookupResult result = new NameLookupResult();
		result.setMatches(matchingUsersOnPage);
		result.setHasMore(total > (start + count));
		result.setNumRows(total);

		return result;
	}

	public static class NameLookupResult {
		private boolean m_hasMore;
		private List<DirectoryUser> m_matches;
		private int m_numRows;

		public boolean hasMore() {
			return m_hasMore;
		}

		public void setHasMore(boolean m_hasMore) {
			this.m_hasMore = m_hasMore;
		}

		public List<DirectoryUser> getMatches() {
			return m_matches;
		}

		public void setMatches(List<DirectoryUser> m_matches) {
			this.m_matches = m_matches;
		}

		public int getNumRows() {
			return m_numRows;
		}

		public void setNumRows(int m_numRows) {
			this.m_numRows = m_numRows;
		}

	}

	/**
	 * Sorts users by lastname / firstname / email
	 * 
	 * @param users users to sort
	 */
	private void sortDirectoryUsers(List<DirectoryUser> users) {
		Collections.sort(users, new Comparator<DirectoryUser>() {

			@Override
			public int compare(DirectoryUser obj1, DirectoryUser obj2) {
				String lastName1 = obj1.getLastName();
				String lastName2 = obj2.getLastName();

				int c = lastName1.compareToIgnoreCase(lastName2);
				if (c!=0)
					return c;

				String firstName1 = obj1.getFirstName();
				String firstName2 = obj2.getFirstName();

				c = firstName1.compareToIgnoreCase(firstName2);

				if (c!=0)
					return c;

				String email1 = obj1.getEmail();
				String email2 = obj2.getEmail();

				c = email1.compareToIgnoreCase(email2);

				return c;
			}
		});
	}
}
