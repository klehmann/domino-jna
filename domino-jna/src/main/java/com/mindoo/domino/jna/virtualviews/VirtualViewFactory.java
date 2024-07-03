package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mindoo.domino.jna.virtualviews.dataprovider.FolderVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.IVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.NoteIdsVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.dataprovider.NotesSearchVirtualViewDataProvider;

public class VirtualViewFactory {

	public VirtualViewBuilder createView(VirtualViewColumn... columnsParam) {
		return createView(Arrays.asList(columnsParam));
	}
	
	public VirtualViewBuilder createView(List<VirtualViewColumn> columnsParam) {
		return new VirtualViewBuilder(columnsParam);
	}
	
	public static class VirtualViewBuilder {
		private VirtualView m_view;

		private VirtualViewBuilder(List<VirtualViewColumn> columns) {
			this.m_view = new VirtualView(columns);
		}

		public VirtualView build() {
			m_view.update();
			return m_view;
		}

		public VirtualViewBuilder withDbSearch(String id, String dbServer, String dbFilePath, String searchFormula,
				Map<String,String> overrideColumnFormulas,
				Set<Integer> noteIdFilter) {

			NotesSearchVirtualViewDataProvider dataProvider = new NotesSearchVirtualViewDataProvider(
					id,
					dbServer,
					dbFilePath,
					searchFormula,
					null,
					noteIdFilter);
			
            dataProvider.init(m_view);            
            m_view.addDataProvider(dataProvider);
            
			return this;
		}
		
		public VirtualViewBuilder withFolderEntries(String id, String dbServer, String dbFilePath, String folderName,
				Map<String,String> overrideColumnFormulas) {
			FolderVirtualViewDataProvider dataProvider = new FolderVirtualViewDataProvider (id, dbServer, dbFilePath, folderName, overrideColumnFormulas);
			dataProvider.init(m_view);            
			m_view.addDataProvider(dataProvider);

			return this;
		}
		
		public VirtualViewBuilder withManualNoteIds(String id, String dbServer, String dbFilePath, Set<Integer> noteIds,
				Map<String,String> overrideColumnFormulas) {
			NoteIdsVirtualViewDataProvider dataProvider = new NoteIdsVirtualViewDataProvider (id, dbServer, dbFilePath, overrideColumnFormulas);
			dataProvider.init(m_view);
			m_view.addDataProvider(dataProvider);
			dataProvider.addNoteIds(noteIds);
			
            return this;
			
		}
		
		public VirtualViewBuilder withCustomDataProvider(IVirtualViewDataProvider customDataProvider) {
			customDataProvider.init(m_view);
			m_view.addDataProvider(customDataProvider);

			return this;
		}
	}
	
	protected Optional<VirtualView> findInVirtualViewCache(String key) {
		return Optional.empty();
	}
	
	protected void addToVirtualViewCache(String key, VirtualView view) {
	}
}
