/*
 * ==========================================================================
 * Copyright (C) 2019-2020 HCL ( http://www.hcl.com/ )
 *                            All rights reserved.
 * ==========================================================================
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may
 * not use this file except in compliance with the License.  You may obtain a
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the  specific language  governing permissions  and limitations
 * under the License.
 * ==========================================================================
 */
package com.mindoo.domino.jna;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;

/**
 * Utility class that acts like the NoteCollection in the legacy API.
 * 
 * @author Karsten Lehmann
 */
public class NotesNoteCollection extends NotesIDTable {
	/**
	 * The available note types that can be selected via
	 * {@link NotesNoteCollection#select(Collection)} and {@link NotesNoteCollection#select(SelectionType...)}.
	 */
	public enum SelectionType {
		/** Indicates whether the collection contains the data documents. */
		DOCUMENTS,
		/** Indicates whether the collection contains profile documents */
		PROFILES,
		/** Indicates whether the collection contains notes for forms */
		FORMS,
		/**Indicates whether the collection contains notes for subforms */
		SUBFORMS,
		/** Indicates whether the collection contains notes for actions */
		ACTIONS,
		/** Indicates whether the collection contains notes for frame sets */
		FRAMESETS,
		/** Indicates whether the collection contains notes for pages */
		PAGES,
		/** Indicates whether the collection contains notes for image resources */
		IMAGE_RESOURCES,
		/** Indicates whether the collection contains notes for style sheet resources */
		STYLESHEETS,
		/** Indicates whether the collection contains notes for Java™ resources */
		JAVA_RESOURCES,
		/** Indicates whether the collection contains notes for miscellaneous format elements */
		MISC_FORMAT_ELEMENTS,
		/** Indicates whether the collection contains notes for views */
		VIEWS,
		/** Indicates whether the collection contains notes for folders */
		FOLDERS,
		/** Indicates whether the collection contains notes for navigators */
		NAVIGATORS,
		/** Indicates whether the collection contains notes for miscellaneous index elements */
		MISC_INDEX_ELEMENTS,
		/** Indicates whether the collection contains an icon note */
		ICON,
		/**Indicates whether the collection contains notes for agents */
		AGENTS,
		/** Indicates whether the collection contains notes for outlines */
		OUTLINES,
		/** Indicates whether the collection contains a database script note */
		DATASCRIPT_SCRIPT,
		/** Indicates whether the collection contains notes for script libraries */
		SCRIPT_LIBRARIES,
		/** Indicates whether the collection contains a data connection note */
		DATA_CONNECTIONS,
		/** Indicates whether the collection contains notes for miscellaneous code elements */
		MISC_CODE_ELEMENTS,
		/** Indicates whether the collection contains notes for shared fields */
		SHARED_FIELDS,
		/** Indicates whether the collection contains an "About Database" note */
		HELP_ABOUT,
		/** Indicates whether the collection contains a "Using Database" note */
		HELP_USING,
		/** Indicates whether the collection contains a help index note */
		HELP_INDEX,
		/** Indicates whether the collection contains replication formulas */
		REPLICATION_FORMULAS,
		/** Indicates whether the collection contains an ACL note */
		ACL
	}
	
	private NotesDatabase m_db;
	private Set<SelectionType> m_selection;
	private String m_formula;
	private NotesTimeDate m_sinceTime;
	private NotesTimeDate m_untilTime;
	private NotesTimeDate m_lastBuildTime;
	private NotesIDTable m_preselectedNoteIds;

	// LMBCS conversion of design flags
	
	private Memory m_DFLAGPAT_VIEWFORM_ALL_VERSIONS;
	private Memory m_DFLAGPAT_SUBFORM_ALL_VERSIONS;
	private Memory m_DFLAGPAT_SACTIONS_DESIGN;
	private Memory m_DFLAGPAT_FRAMESET;
	private Memory m_DFLAGPAT_WEBPAGE;
	private Memory m_DFLAGPAT_IMAGE_RESOURCE;
	private Memory m_DFLAGPAT_STYLE_SHEET_RESOURCE;
	private Memory m_DFLAGPAT_JAVA_RESOURCE;
	private Memory m_DFLAGPAT_VIEW_DESIGN;
	private Memory m_DFLAGPAT_FOLDER_ALL_VERSIONS;
	private Memory m_DFLAGPAT_VIEWMAP_DESIGN;
	private Memory m_DFLAGPAT_AGENTSLIST;
	private Memory m_DFLAGPAT_SITEMAP;
	private Memory m_DFLAGPAT_DATABASESCRIPT;
	private Memory m_DFLAGPAT_SCRIPTLIB;
	private Memory m_DFLAGPAT_DATA_CONNECTION_RESOURCE;

	public NotesNoteCollection(NotesDatabase db) {
		super();
		m_db = db;
		m_selection = new HashSet<>();
	}

	/**
	 * Returns the parent database
	 * 
	 * @return database
	 */
	public NotesDatabase getParent() {
		return m_db;
	}

	/**
	 * Adds content types to the selection
	 * 
	 * @param selectionTypes content types
	 * @return note collection instance
	 */
	public NotesNoteCollection select(SelectionType... selectionTypes) {
		if (selectionTypes!=null) {
			for (SelectionType currType : selectionTypes) {
				m_selection.add(currType);
			}
		}
		return this;
	}

	/**
	 * Adds content types to the selection
	 * 
	 * @param selectionTypes content types
	 * @return note collection instance
	 */
	public NotesNoteCollection select(Collection<SelectionType> selectionTypes) {
		m_selection.addAll(selectionTypes);
		return this;
	}

	/**
	 * Selects data, admin and design notes
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllNotes() {
		selectAllDataNotes();
		selectAllAdminNotes();
		selectAllDesignElements();

		return this;
	}
	
	/**
	 * Selects normal data notes and profiles
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllDataNotes() {
		select(SelectionType.DOCUMENTS, SelectionType.PROFILES);

		return this;
	}
	
	/**
	 * Selects replication formulas and ACLs
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllAdminNotes() {
		select(SelectionType.REPLICATION_FORMULAS, SelectionType.ACL);
		
		return this;
	}

	/**
	 * Selects format, index and code elements, the icon, shared fields,
	 * help about, help using and help imdex.
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllDesignElements() {
		selectAllFormatElements();
		selectAllIndexElements();
		selectAllCodeElements();
		
		select(SelectionType.ICON, SelectionType.SHARED_FIELDS, SelectionType.HELP_ABOUT,
				SelectionType.HELP_USING, SelectionType.HELP_INDEX);
		
		return this;
	}

	/**
	 * Selects forms, subforms, actions, framesets, pages, image resources,
	 * stylesheets, Java resources and misc format elements.
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllFormatElements() {
		select(SelectionType.FORMS, SelectionType.SUBFORMS, SelectionType.ACTIONS,
				SelectionType.FRAMESETS, SelectionType.PAGES, SelectionType.IMAGE_RESOURCES,
				SelectionType.STYLESHEETS, SelectionType.JAVA_RESOURCES, SelectionType.MISC_FORMAT_ELEMENTS);
		
		return this;
	}
	
	/**
	 * Selects views, folders, navigators and misc index elements
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllIndexElements() {
		select(SelectionType.VIEWS, SelectionType.FOLDERS, SelectionType.NAVIGATORS, SelectionType.MISC_INDEX_ELEMENTS);
		
		return this;
	}

	/**
	 * Selects agents, outlines, database script, script libraries,
	 * data connections and misc code elements.
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection selectAllCodeElements() {
		select(SelectionType.AGENTS, SelectionType.OUTLINES, SelectionType.DATASCRIPT_SCRIPT,
				SelectionType.SCRIPT_LIBRARIES, SelectionType.DATA_CONNECTIONS, SelectionType.MISC_CODE_ELEMENTS);
		
		return this;
	}
	
	/**
	 * Removes content types from the selection
	 * 
	 * @param selectionTypes content types
	 * @return note collection instance
	 */
	public NotesNoteCollection deselect(SelectionType... selectionTypes) {
		if (selectionTypes!=null) {
			for (SelectionType currType : selectionTypes) {
				m_selection.remove(currType);
			}
		}
		return this;
	}

	/**
	 * Removes content types from the selection
	 * 
	 * @param selectionTypes content types
	 * @return note collection instance
	 */
	public NotesNoteCollection deselect(Collection<SelectionType> selectionTypes) {
		m_selection.removeAll(selectionTypes);
		return this;
	}

	/**
	 * Checks if a content type is selected
	 * 
	 * @param selectionType content type
	 * @return true if selected
	 */
	public boolean isSelected(SelectionType selectionType) {
		return m_selection.contains(selectionType);
	}

	/**
	 * Returns the currently selected content types
	 * 
	 * @return selection
	 */
	public Set<SelectionType> getSelection() {
		return m_selection;
	}
	
	/**
	 * Sets a formula that selects notes for inclusion in the collection.
	 * 
	 * @param formula formula
	 * @return note collection instance
	 */
	public NotesNoteCollection withSelectionFormula(String formula) {
		m_formula = formula;
		return this;
	}

	/**
	 * Returns the selection formula to selects notes for inclusion in the collection.
	 * @return formula or null
	 */
	public String getSelectionFormula() {
		return m_formula;
	}

	/**
	 * Adds a since time to the search so that it only contains notes
	 * with a "Modified in this file" date after the given date.
	 * 
	 * @param dt since time
	 * @return note collection instance
	 */
	public NotesNoteCollection withSinceTime(NotesTimeDate dt) {
		m_sinceTime = dt;
		return this;
	}

	/**
	 * Returns the since time so that the search only contains notes
	 * with a "Modified in this file" date after the given date.
	 * @return since time
	 */
	public NotesTimeDate getSinceTime() {
		return m_sinceTime;
	}

	/**
	 * Returns a date that can be passed as "since time" to the next search
	 * for incremental searches.
	 * 
	 * @return until time; is set after running {@link #build()}
	 */
	public NotesTimeDate getUntilTime() {
		return m_untilTime;
	}
	
	/**
	 * Returns the last time, the {@link #build()} has been invoked.
	 * 
	 * @return last build time or null if not built yet
	 */
	public NotesTimeDate getLastBuildTime() {
		return m_lastBuildTime;
	}

	/**
	 * Use this method to pre-filter the documents to be scanned by the search
	 * 
	 * @param noteIds note ids
	 * @return note collection instance
	 */
	public NotesNoteCollection withPreselection(Collection<Integer> noteIds) {
		m_preselectedNoteIds = new NotesIDTable(noteIds);
		return this;
	}
	
	/**
	 * Returns an {@link NotesIDTable} with note ids to pre-filter the documents to
	 * be scanned by the search
	 * 
	 * @return id table or null if not set
	 */
	public NotesIDTable getPreselection() {
		return m_preselectedNoteIds;
	}
	
	/**
	 * Runs a database search, adding note ids of search matches to this
	 * object (subclass of {@link NotesIDTable}).
	 * 
	 * @return note collection instance
	 */
	public NotesNoteCollection build() {
		Map<String,String> columnFormulas = new HashMap<>();
		columnFormulas.put("$Name", "");
		columnFormulas.put("$Flags", "");
		
		EnumSet<NoteClass> docClass = EnumSet.of(NoteClass.PRIVATE);

		if (isSelected(SelectionType.DOCUMENTS) || isSelected(SelectionType.PROFILES)) {
			docClass.add(NoteClass.DOCUMENT);
		}

		if (isSelected(SelectionType.HELP_ABOUT)) {
			docClass.add(NoteClass.INFO);
		}
		
		if (isSelected(SelectionType.FORMS) || isSelected(SelectionType.SUBFORMS) ||
				isSelected(SelectionType.ACTIONS) || isSelected(SelectionType.FRAMESETS) ||
				isSelected(SelectionType.PAGES) || isSelected(SelectionType.IMAGE_RESOURCES) ||
				isSelected(SelectionType.STYLESHEETS) || isSelected(SelectionType.JAVA_RESOURCES) ||
				isSelected(SelectionType.MISC_FORMAT_ELEMENTS)) {
			docClass.add(NoteClass.FORM);
		}

		if (isSelected(SelectionType.VIEWS) || isSelected(SelectionType.FOLDERS) ||
				isSelected(SelectionType.NAVIGATORS) || isSelected(SelectionType.MISC_INDEX_ELEMENTS)) {
			docClass.add(NoteClass.VIEW);
		}
		
		if (isSelected(SelectionType.ICON)) {
			docClass.add(NoteClass.ICON);
		}

		if (isSelected(SelectionType.ACL)) {
			docClass.add(NoteClass.ACL);
		}

		if (isSelected(SelectionType.HELP_INDEX)) {
			docClass.add(NoteClass.HELP_INDEX);
		}

		if (isSelected(SelectionType.HELP_USING)) {
			docClass.add(NoteClass.HELP);
		}
		
		if (isSelected(SelectionType.AGENTS) || isSelected(SelectionType.OUTLINES) ||
				isSelected(SelectionType.DATASCRIPT_SCRIPT) || isSelected(SelectionType.SCRIPT_LIBRARIES) ||
				isSelected(SelectionType.DATA_CONNECTIONS) || isSelected(SelectionType.MISC_CODE_ELEMENTS)) {
			docClass.add(NoteClass.FILTER);
		}
		
		if (isSelected(SelectionType.SHARED_FIELDS)) {
			docClass.add(NoteClass.FIELD);
		}

		if (isSelected(SelectionType.REPLICATION_FORMULAS)) {
			docClass.add(NoteClass.REPLFORMULA);
		}

		EnumSet<Search> searchFlags = EnumSet.of(Search.NOTIFYDELETIONS, Search.SUMMARY);
		
		if (isSelected(SelectionType.PROFILES)) {
			//add profile docs to search results
			searchFlags.add(Search.PROFILE_DOCS);
			//and make sure the selection formula is run on named ghosts (profiles and more)
			searchFlags.add(Search.NAMED_GHOSTS);
		}
		
		clear();
		
		m_untilTime = NotesSearch.search(m_db, m_preselectedNoteIds,
				StringUtil.isEmpty(m_formula) ? "@All" : m_formula,
				columnFormulas,
				"-", searchFlags, docClass, m_sinceTime, new NotesSearch.SearchCallback() {
					
					@Override
					public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {

						//skip deletions and notes not matching the formula
						Set<NoteClass> docClass = searchMatch.getNoteClass();
						if (docClass.contains(NoteClass.NOTIFYDELETION)) {
							return Action.Continue;
						}
						if (!searchMatch.matchesFormula()) {
							return Action.Continue;
						}

						if (docClass.contains(NoteClass.DOCUMENT)) {
							//check if this is a profile document ($Name field with "$Profile")
							String nameVal = summaryBufferData.getAsString("$Name", "");

							if ("$Profile".equalsIgnoreCase(nameVal)) {
								if (isSelected(SelectionType.PROFILES)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							//just a normal document
							if (isSelected(SelectionType.DOCUMENTS)) {
								addNote(searchMatch.getNoteId());
							}
							return Action.Continue;
						}
						else if (NoteClass.isDesignElement(docClass)) {
							//check $Flags value to see which kind of design element this is
							String flags = null;

							if (docClass.contains(NoteClass.FORM) || 
									docClass.contains(NoteClass.VIEW) ||
									docClass.contains(NoteClass.FILTER)) {
								flags = summaryBufferData.getAsString("$Flags", "");
							}

							if (docClass.contains(NoteClass.INFO)) {
								if (isSelected(SelectionType.HELP_ABOUT)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							Memory flagsMem = null;
							short flagsLength = 0;
							if (flags!=null) {
								flagsMem = NotesStringUtils.toLMBCS(flags, true);
								flagsLength = (short) ((flagsMem.size()-1) & 0xffff);
							}

							if (docClass.contains(NoteClass.FORM)) {
								if (flags!=null) {
									if (m_DFLAGPAT_VIEWFORM_ALL_VERSIONS==null) {
										m_DFLAGPAT_VIEWFORM_ALL_VERSIONS = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWFORM_ALL_VERSIONS, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_VIEWFORM_ALL_VERSIONS)) {
										if (isSelected(SelectionType.FORMS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_SUBFORM_ALL_VERSIONS==null) {
										m_DFLAGPAT_SUBFORM_ALL_VERSIONS = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SUBFORM_ALL_VERSIONS, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_SUBFORM_ALL_VERSIONS)) {
										if (isSelected(SelectionType.SUBFORMS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_SACTIONS_DESIGN==null) {
										m_DFLAGPAT_SACTIONS_DESIGN = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SACTIONS_DESIGN, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_SACTIONS_DESIGN)) {
										if (isSelected(SelectionType.ACTIONS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_FRAMESET==null) {
										m_DFLAGPAT_FRAMESET = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_FRAMESET, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_FRAMESET)) {
										if (isSelected(SelectionType.FRAMESETS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_WEBPAGE==null) {
										m_DFLAGPAT_WEBPAGE = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_WEBPAGE, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_WEBPAGE)) {
										if (isSelected(SelectionType.PAGES)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_IMAGE_RESOURCE==null) {
										m_DFLAGPAT_IMAGE_RESOURCE = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_IMAGE_RESOURCE, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_IMAGE_RESOURCE)) {
										if (isSelected(SelectionType.IMAGE_RESOURCES)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_STYLE_SHEET_RESOURCE==null) {
										m_DFLAGPAT_STYLE_SHEET_RESOURCE = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_STYLE_SHEET_RESOURCE, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_STYLE_SHEET_RESOURCE)) {
										if (isSelected(SelectionType.STYLESHEETS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_JAVA_RESOURCE==null) {
										m_DFLAGPAT_JAVA_RESOURCE = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_JAVA_RESOURCE, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_JAVA_RESOURCE)) {
										if (isSelected(SelectionType.JAVA_RESOURCES)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}
									return Action.Continue;
								}

								if (isSelected(SelectionType.FORMS)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							if (docClass.contains(NoteClass.VIEW)) {
								if (flagsMem!=null) {
									if (m_DFLAGPAT_VIEW_DESIGN==null) {
										m_DFLAGPAT_VIEW_DESIGN = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEW_DESIGN, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_VIEW_DESIGN)) {
										if (isSelected(SelectionType.VIEWS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_FOLDER_ALL_VERSIONS==null) {
										m_DFLAGPAT_FOLDER_ALL_VERSIONS = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_FOLDER_ALL_VERSIONS, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_FOLDER_ALL_VERSIONS)) {
										if (isSelected(SelectionType.FOLDERS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_VIEWMAP_DESIGN==null) {
										m_DFLAGPAT_VIEWMAP_DESIGN = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWMAP_DESIGN, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_VIEWMAP_DESIGN)) {
										if (isSelected(SelectionType.NAVIGATORS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									return Action.Continue;
								}

								if (isSelected(SelectionType.VIEWS)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							if (docClass.contains(NoteClass.ICON)) {
								if (isSelected(SelectionType.ICON)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							if (docClass.contains(NoteClass.HELP_INDEX)) {
								if (isSelected(SelectionType.HELP_INDEX)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							if (docClass.contains(NoteClass.HELP)) {
								if (isSelected(SelectionType.HELP_USING)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							if (docClass.contains(NoteClass.FILTER)) {
								if (flagsMem!=null) {
									if (m_DFLAGPAT_AGENTSLIST==null) {
										m_DFLAGPAT_AGENTSLIST = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_AGENTSLIST, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_AGENTSLIST)) {
										if (isSelected(SelectionType.AGENTS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_SITEMAP==null) {
										m_DFLAGPAT_SITEMAP = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SITEMAP, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_SITEMAP)) {
										if (isSelected(SelectionType.OUTLINES)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_DATABASESCRIPT==null) {
										m_DFLAGPAT_DATABASESCRIPT = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_DATABASESCRIPT, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_DATABASESCRIPT)) {
										if (isSelected(SelectionType.DATASCRIPT_SCRIPT)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_SCRIPTLIB==null) {
										m_DFLAGPAT_SCRIPTLIB = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SCRIPTLIB, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_SCRIPTLIB)) {
										if (isSelected(SelectionType.SCRIPT_LIBRARIES)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}

									if (m_DFLAGPAT_DATA_CONNECTION_RESOURCE==null) {
										m_DFLAGPAT_DATA_CONNECTION_RESOURCE = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_DATA_CONNECTION_RESOURCE, true);
									}
									if (NotesNativeAPI.get().CmemflagTestMultiple(flagsMem, flagsLength,
											m_DFLAGPAT_DATA_CONNECTION_RESOURCE)) {
										if (isSelected(SelectionType.DATA_CONNECTIONS)) {
											addNote(searchMatch.getNoteId());
										}
										return Action.Continue;
									}
								}
							}

							if (docClass.contains(NoteClass.FIELD)) {
								if (isSelected(SelectionType.SHARED_FIELDS)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}
						}
						else {
							if (docClass.contains(NoteClass.ACL)) {
								if (isSelected(SelectionType.ACL)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							if (docClass.contains(NoteClass.REPLFORMULA)) {
								if (isSelected(SelectionType.REPLICATION_FORMULAS)) {
									addNote(searchMatch.getNoteId());
								}
								return Action.Continue;
							}

							return Action.Continue;
						}

						return Action.Continue;
					}
				});
		m_lastBuildTime = new NotesTimeDate();
		
		return this;
	}
	
}
