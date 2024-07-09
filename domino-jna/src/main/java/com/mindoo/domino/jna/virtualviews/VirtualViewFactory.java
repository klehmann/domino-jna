package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.IViewColumn.ColumnSort;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.utils.StringUtil;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Category;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Hidden;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Total;
import com.mindoo.domino.jna.virtualviews.dataprovider.FolderVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.IVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.NoteIdsVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.NotesSearchVirtualViewDataProvider;

/**
 * Factory class to create {@link VirtualView} objects and cache them for reuse
 */
public enum VirtualViewFactory {
	INSTANCE;
	
	private ConcurrentHashMap<String,VirtualViewWithVersion> viewsById;
	
	private VirtualViewFactory() {
		viewsById = new ConcurrentHashMap<>();
	}
	
	/**
	 * Creates a new {@link VirtualView} object with the specified columns
	 * 
	 * @param columnsParam columns to display in the view
	 * @return builder object to add data providers
	 */
	public static VirtualViewBuilder createView(VirtualViewColumn... columnsParam) {
		return createView(Arrays.asList(columnsParam));
	}
	
	/**
	 * Creates a new {@link VirtualView} object with the specified columns
	 * 
	 * @param columnsParam columns to display in the view
	 * @return builder object to add data providers
	 */
	public static VirtualViewBuilder createView(List<VirtualViewColumn> columnsParam) {
		return new VirtualViewBuilder(columnsParam);
	}

	/**
	 * Creates a new {@link VirtualView} object with the columns from the specified NotesCollection
	 * 
	 * @param col NotesCollection to use as template
	 * @return builder object to add data providers
	 */
	public static VirtualViewBuilder createViewFromTemplate(NotesCollection col) {
		List<VirtualViewColumn> virtualViewColumns = col
				.getColumns()
				.stream()
				.map((currCol) -> {
					if (currCol.isResponse()) {
						//skip response columns, unsupported
						return null;
					}
					String title = currCol.getTitle();
					String itemName = currCol.getItemName();
					String formula = currCol.getFormula();
					boolean isCategory = currCol.isCategory();
					boolean isHidden = currCol.isHidden();
					boolean sorted = currCol.isSorted();
					boolean sortedDescending = currCol.isSortedDescending();
					
					ColumnSort sort = ColumnSort.NONE;
					if (sorted) {
						if (sortedDescending) {
							sort = ColumnSort.DESCENDING;
						}
						else {
							sort = ColumnSort.ASCENDING;
						}
					}
					
					//currently unsupported, because we don't extract that info from the NotesCollection design
					Total totalMode = Total.NONE;
					return new VirtualViewColumn(title, itemName, isCategory ? Category.YES : Category.NO, isHidden ? Hidden.YES : Hidden.NO, sort,
							totalMode, formula);
                })
				.filter((currCol) -> currCol!=null)
				.collect(Collectors.toList());
		return new VirtualViewBuilder(virtualViewColumns);
	}

	/**
	 * Builder class to create a new {@link VirtualView} object
	 */
	public static class VirtualViewBuilder {
		private VirtualView m_view;

		private VirtualViewBuilder(List<VirtualViewColumn> columns) {
			this.m_view = new VirtualView(columns);
		}

		public VirtualView build() {
			m_view.update();
			return m_view;
		}

		/**
		 * Adds a data provider to the view that runs a formula search in a Notes database and for all matching data documents
		 * it computes the view column values.
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param searchFormula The search formula to use
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withDbSearch(String origin, String dbServer, String dbFilePath, String searchFormula) {			
			return withDbSearch(origin, dbServer, dbFilePath, searchFormula, EnumSet.of(NoteClass.DATA),
					null, null, null, null);
		}
		
		/**
		 * Adds a data provider to the view that runs a formula search in a Notes database and for all matching data or design documents
		 * it computes the view column values.
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param searchFormula The search formula to use
		 * @param noteClasses Optional set with note classes to pre-filter the search results or null to use {@link NoteClass#DATA}; to search for all design notes, use {@link NoteClass#ALLNONDATA} or use specific note classes like {@link NoteClass#VIEW}
		 * @param optFTQuery Optional full text query to post process the found notes or null
		 * @param optFTOptions Optional full text search options or null for default FT options
		 * @param overrideColumnFormulas Optional map with column formulas to override the original formulas derived from the view columns or null
		 * @param noteIdFilter Optional set with note ids to pre-filter the search results or null
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withDbSearch(String origin, String dbServer, String dbFilePath, String searchFormula, Set<NoteClass> noteClasses,
				String optFTQuery, Set<FTSearch> optFTOptions,
				Map<String,String> overrideColumnFormulas,
				Set<Integer> noteIdFilter) {

			NotesSearchVirtualViewDataProvider dataProvider = new NotesSearchVirtualViewDataProvider(
					origin,
					dbServer,
					dbFilePath,
					searchFormula,
					noteClasses,
					EnumSet.of(Search.SESSION_USERNAME),
					optFTQuery,
					optFTOptions,
					null,
					noteIdFilter);
			
            dataProvider.init(m_view);            
            m_view.addDataProvider(dataProvider);
            
			return this;
		}
		
		/**
		 * Adds a data provider to the view that runs a formula search in a Notes database on all profile documents
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param searchFormula The search formula to use
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withProfileDocs(String origin, String dbServer, String dbFilePath, String searchFormula) {
			return withProfileDocs(origin, dbServer, dbFilePath, searchFormula, null, null);
		}
		
		/**
		 * Adds a data provider to the view that runs a formula search in a Notes database on all profile documents
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param searchFormula The search formula to use
		 * @param overrideColumnFormulas Optional map with column formulas to override the original formulas derived from the view columns or null
		 * @param noteIdFilter Optional set with note ids to pre-filter the search results or null
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withProfileDocs(String origin, String dbServer, String dbFilePath, String searchFormula, 
				Map<String,String> overrideColumnFormulas,
				Set<Integer> noteIdFilter) {

			String combinedSearchFormula = "@Begins($Name; \"$profile_\")";
			if (StringUtil.isNotEmpty(searchFormula)) {
				combinedSearchFormula += " & (" + searchFormula + ")";
			}
			
			NotesSearchVirtualViewDataProvider dataProvider = new NotesSearchVirtualViewDataProvider(
					origin,
					dbServer,
					dbFilePath,
					combinedSearchFormula,
					EnumSet.of(NoteClass.DATA),
					EnumSet.of(Search.NAMED_GHOSTS, Search.SELECT_NAMED_GHOSTS, Search.PROFILE_DOCS),
					null,
					null,
					null,
					noteIdFilter) {
			
				@Override
				protected boolean isAccepted(ISearchMatch searchMatch, IItemTableData summaryBufferData) {
					String name = summaryBufferData.getAsString("$Name", "");
					if (name.startsWith("$profile_")) {
						return true;
					}
					else {
						return false;
					}
				}				
			};
			
            dataProvider.init(m_view);            
            m_view.addDataProvider(dataProvider);
            
			return this;
		}
		
		/**
		 * Adds a data provider to the view that takes the documents of an existing folder and computes the view column values for them.<br>
		 * When updating the data provider via {@link VirtualView#update(String)}, we incrementally fetch folder changes (additions/removals).
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param folderName name of the folder
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withFolderEntries(String origin, String dbServer, String dbFilePath, String folderName) {
			return withFolderEntries(origin, dbServer, dbFilePath, folderName, null);
		}
		
		/**
		 * Adds a data provider to the view that takes the documents of an existing folder and computes the view column values for them.<br>
		 * When updating the data provider via {@link VirtualView#update(String)}, we incrementally fetch folder changes (additions/removals).
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param folderName name of the folder
		 * @param overrideColumnFormulas Optional map with column formulas to override the original formulas derived from the view columns or null
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withFolderEntries(String origin, String dbServer, String dbFilePath, String folderName,
				Map<String,String> overrideColumnFormulas) {
			FolderVirtualViewDataProvider dataProvider = new FolderVirtualViewDataProvider (origin, dbServer, dbFilePath, folderName, overrideColumnFormulas);
			dataProvider.init(m_view);            
			m_view.addDataProvider(dataProvider);

			return this;
		}

		/**
		 * Adds a data provider to the view that computes the view column values for a manual list of note ids.<br>
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param noteIds set with note ids to include in the view
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withManualNoteIds(String origin, String dbServer, String dbFilePath, Set<Integer> noteIds) {
			return withManualNoteIds(origin, dbServer, dbFilePath, noteIds, null);
		}
		
		/**
		 * Adds a data provider to the view that computes the view column values for a manual list of note ids.<br>
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param noteIds set with note ids to include in the view
		 * @param overrideColumnFormulas Optional map with column formulas to override the original formulas derived from the view columns or null
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withManualNoteIds(String origin, String dbServer, String dbFilePath, Set<Integer> noteIds,
				Map<String,String> overrideColumnFormulas) {
			NoteIdsVirtualViewDataProvider dataProvider = new NoteIdsVirtualViewDataProvider (origin, dbServer, dbFilePath, overrideColumnFormulas);
			dataProvider.init(m_view);
			m_view.addDataProvider(dataProvider);
			dataProvider.addNoteIds(noteIds);
			
            return this;
			
		}
		
		/**
		 * Adds a custom data provider to the view that computes the view column values for a custom data source.
		 * 
		 * @param customDataProvider custom data provider
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withCustomDataProvider(IVirtualViewDataProvider customDataProvider) {
			customDataProvider.init(m_view);
			m_view.addDataProvider(customDataProvider);

			return this;
		}
	}
	
	/**
	 * Reuses a {@link VirtualView} if it already exists, otherwise creates a new one
	 * 
	 * @param viewId unique id of the view
	 * @param version version of the view, increase this number to force a rebuilt of the view
	 * @param discardUnusedAfter if a view was not accessed for this time (by calling this method again), it will be removed from the cache
	 * @param discardUnusedUnit unit of the discardUnusedAfter value (e.g. TimeUnit.MINUTES), in the Domino JNA OSGi plugin, we run our cleanup job every minute
	 * @param fct function to create the view
	 * @return view
	 */
	public VirtualView createViewOnce(String viewId, int version, int discardUnusedAfter, TimeUnit discardUnusedUnit,
			Function<String, VirtualView> fct) {
		VirtualViewWithVersion viewWithVersion = viewsById.get(viewId);
		if (viewWithVersion != null && viewWithVersion.getVersion() == version) {
			viewWithVersion.updateLastAccess();
			return viewWithVersion.getView();
		} else {
			VirtualView view = fct.apply(viewId);
			if (view == null) {
				throw new IllegalArgumentException("Function must not return null");
			}
			long discardUnusedAfterMinutes = discardUnusedUnit.toMinutes(discardUnusedAfter);
			viewsById.put(viewId, new VirtualViewWithVersion(view, version, discardUnusedAfterMinutes));
			return view;
		}
	}
	
	/**
	 * Disposes a view by removing it from the cache
	 * 
	 * @param viewId unique id of the view
	 */
	public void disposeView(String viewId) {
		viewsById.remove(viewId);
	}

	/**
	 * Returns the view ids of all views that are currently stored in the cache
	 * 
	 * @return view ids
	 */
	public Iterator<String> getStoredViewIds() {
		return viewsById.keySet().iterator();
	}
	
	/**
	 * Go through the list of cached views and removes views that were not accessed for the
	 * configured maximum time
	 */
	public void cleanupExpiredViews() {
		boolean changed = false;
		
		long now = System.currentTimeMillis();
		for (Iterator<Map.Entry<String, VirtualViewWithVersion>> it = viewsById.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, VirtualViewWithVersion> currEntry = it.next();
			VirtualViewWithVersion viewWithVersion = currEntry.getValue();
			long timeDiffLastAccess = now - viewWithVersion.getLastAccess();
			if (timeDiffLastAccess > viewWithVersion.getDiscardUnusedAfterMinutes() * 60 * 1000) {
				it.remove();
				changed = true;
			}
		}
		if (changed) {
			System.gc();
		}
	}
	
	/**
	 * Returns the time when a view was last accessed via {@link #createViewOnce(String, int, int, TimeUnit, Function)}
	 * 
	 * @param viewId view id
	 * @return last access time or -1 if the view is not in the cache
	 */
	public long getLastViewAccess(String viewId) {
		VirtualViewWithVersion viewWithVersion = viewsById.get(viewId);
		if (viewWithVersion != null) {
			return viewWithVersion.getLastAccess();
		} else {
			return -1;
		}
	}
	
	/**
	 * Returns the time when a view was created via {@link #createViewOnce(String, int, int, TimeUnit, Function)}
	 * 
	 * @param viewId view id
	 * @return creation time or -1 if the view is not in the cache
	 */
	public long getViewCreationDate(String viewId) {
		VirtualViewWithVersion viewWithVersion = viewsById.get(viewId);
		if (viewWithVersion != null) {
			return viewWithVersion.getCreated();
		} else {
			return -1;
		}
	}
	
	/**
	 * Returns the time after which a view is discarded if it was not accessed via
	 * {@link #createViewOnce(String, int, int, TimeUnit, Function)}
	 * 
	 * @param viewId view id
	 * @return time in minutes or -1 if the view is not in the cache
	 */
	public long getDiscardUnusedAfterMinutes(String viewId) {
		VirtualViewWithVersion viewWithVersion = viewsById.get(viewId);
		if (viewWithVersion != null) {
			return viewWithVersion.getDiscardUnusedAfterMinutes();
		} else {
			return -1;
		}
	}
	
	/**
	 * Returns the stored version of a view
	 * 
	 * @param viewId view id
	 * @return version or -1 if the view is not in the cache
	 */
	public int getViewVersion(String viewId) {
		VirtualViewWithVersion viewWithVersion = viewsById.get(viewId);
		if (viewWithVersion != null) {
			return viewWithVersion.getVersion();
		} else {
			return -1;
		}
	}
	
	private static class VirtualViewWithVersion {
		private VirtualView view;
		private int version;
		private long created;
		private long lastAccess;
		private long discardUnusedAfterMinutes;
		
		public VirtualViewWithVersion(VirtualView view, int version, long discardUnusedAfterMinutes) {
			this.view = view;
			this.version = version;
			this.created = System.currentTimeMillis();
			this.lastAccess = created;
			this.discardUnusedAfterMinutes = discardUnusedAfterMinutes;
		}
		
		public VirtualView getView() {
            return view;
		}
		
		public int getVersion() {
			return version;
		}
		
		public long getCreated() {
			return created;
		}
		
		public long getLastAccess() {
			return lastAccess;
		}
		
		public void updateLastAccess() {
			this.lastAccess = System.currentTimeMillis();
		}
		
		public long getDiscardUnusedAfterMinutes() {
			return discardUnusedAfterMinutes;
		}
	}
}
