package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mindoo.domino.jna.IViewColumn.ColumnSort;
import com.mindoo.domino.jna.NotesCollection;
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
		 * Adds a data provider to the view that runs a formula search in a Notes database and for all matching documents
		 * it computes the view column values.
		 * 
		 * @param origin The origin id of the data provider, used to identify the data provider in the view
		 * @param dbServer The server name of the database
		 * @param dbFilePath The file path of the database
		 * @param searchFormula The search formula to use
		 * @param overrideColumnFormulas Optional map with column formulas to override the original formulas derived from the view columns or null
		 * @param noteIdFilter Optional set with note ids to pre-filter the search results or null
		 * @return builder object to add more data providers
		 */
		public VirtualViewBuilder withDbSearch(String origin, String dbServer, String dbFilePath, String searchFormula,
				Map<String,String> overrideColumnFormulas,
				Set<Integer> noteIdFilter) {

			NotesSearchVirtualViewDataProvider dataProvider = new NotesSearchVirtualViewDataProvider(
					origin,
					dbServer,
					dbFilePath,
					searchFormula,
					null,
					noteIdFilter);
			
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
	 * @param fct function to create the view
	 * @return view
	 */
	public VirtualView createViewOnce(String viewId, int version, Function<String, VirtualView> fct) {
		VirtualViewWithVersion viewWithVersion = viewsById.get(viewId);
		if (viewWithVersion != null && viewWithVersion.getVersion() == version) {
			return viewWithVersion.getView();
		} else {
			VirtualView view = fct.apply(viewId);
			if (view == null) {
				throw new IllegalArgumentException("Function must not return null");
			}
			viewsById.put(viewId, new VirtualViewWithVersion(view, version));
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
	
	private static class VirtualViewWithVersion {
		private VirtualView view;
		private int version;

		public VirtualViewWithVersion(VirtualView view, int version) {
			this.view = view;
			this.version = version;
		}
		
		public VirtualView getView() {
            return view;
		}
		
		public int getVersion() {
			return version;
		}		
	}
}
