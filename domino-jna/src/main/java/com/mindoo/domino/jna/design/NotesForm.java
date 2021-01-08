package com.mindoo.domino.jna.design;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.ItemType;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.richtext.FieldInfo;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.RichTextUtils;
import com.mindoo.domino.jna.utils.ListUtil;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

/**
 * Represents a form or subform in a database
 * 
 * @author Karsten Lehmann
 */
public class NotesForm {
	private int m_formNoteId;
	private NotesNote m_formNote;
	private String m_formName;
	private List<String> m_formAliases;
	
	/**
	 * Creates a new instance
	 * 
	 * @param formNote form note
	 */
	public NotesForm(NotesNote formNote) {
		m_formNote = formNote;
		m_formNoteId = formNote.getNoteId();
	}
	
	/**
	 * Returns the underlying form note
	 * 
	 * @return note
	 */
	public NotesNote getNote() {
		return m_formNote;
	}
	
	public int getNoteId() {
		return m_formNoteId;
	}
	
	private void parseNameAndAlias() {
		if (m_formName==null || m_formAliases==null) {
			List<String> formNameAndAliases = m_formNote.getItemValueStringList(NotesConstants.FIELD_TITLE);
			if (formNameAndAliases==null || formNameAndAliases.isEmpty()) {
				m_formName = "";
				m_formAliases = Collections.emptyList();
			}
			else if (formNameAndAliases.size()==1) {
				m_formName = formNameAndAliases.get(0);
				m_formAliases = Collections.emptyList();
			}
			else {
				m_formName = formNameAndAliases.get(0);
				m_formAliases = new ArrayList<>();
				for (int i=1; i<formNameAndAliases.size(); i++) {
					m_formAliases.add(formNameAndAliases.get(i));
				}
			}
		}
	}
	
	/**
	 * Returns the name of the form
	 * 
	 * @return name
	 */
	public String getName() {
		parseNameAndAlias();
		return m_formName;
	}
	
	/**
	 * Returns the aliases of the form
	 * 
	 * @return aliases, not null
	 */
	public List<String> getAliases() {
		parseNameAndAlias();
		return m_formAliases;
	}
	
	/**
	 * Indicates whether this is a form or a subform
	 * 
	 * @return true if subform
	 */
	public boolean isSubForm() {
		String flags = m_formNote.getItemValueString(NotesConstants.DESIGN_FLAGS);
		return flags.contains(NotesConstants.DESIGN_FLAG_SUBFORM);
	}

	/**
	 * Returns detail information about the form fields
	 * 
	 * @return field infos
	 */
	public List<FieldInfo> getFields() {
		IRichTextNavigator rtNav = m_formNote.getRichtextNavigator(NotesConstants.ITEM_NAME_TEMPLATE);
		return RichTextUtils.collectFields(rtNav);
	}
	
	/**
	 * Returns the contents of the $FormUsers item
	 * 
	 * @return form users, not null
	 */
	public List<String> getFormUsers() {
		return m_formNote.getItemValueStringList(NotesConstants.ITEM_NAME_FORMUSERS);
	}
	
	/**
	 * Changes the $FormUsers field. Call {@link #update()} afterwards to
	 * save your changes.
	 * 
	 * @param users new values or null to remove the item
	 */
	public void setFormUsers(List<String> users) {
		m_formNote.replaceItemValue(NotesConstants.ITEM_NAME_FORMUSERS,
				EnumSet.of(ItemType.SUMMARY, ItemType.NAMES), NotesNamingUtils.toCanonicalNames(users));
	}
	
	/**
	 * Returns the contents of the $Readers item
	 * 
	 * @return readers, not null
	 */
	public List<String> getReaders() {
		return m_formNote.getItemValueStringList(NotesConstants.DESIGN_READERS);
	}
	
	/**
	 * Changes the $Readers field. Call {@link #update()} afterwards to
	 * save your changes.
	 * 
	 * @param readers readers
	 */
	public void setReaders(List<String> readers) {
		m_formNote.replaceItemValue(NotesConstants.DESIGN_READERS,
				EnumSet.of(ItemType.SUMMARY, ItemType.NAMES), NotesNamingUtils.toCanonicalNames(readers));
	}
	
	/**
	 * Test if the item should be retained in a design refresh
	 * 
	 * @param itemName item name
	 * @return true to protect
	 */
	private boolean isProtected(String itemName) {
		List<String> retainFields = m_formNote.getItemValueStringList(NotesConstants.DESIGN_RETAIN_FIELDS);
		return ListUtil.containsIgnoreCase(retainFields, itemName);
	}
	
	/**
	 * Helper method tp protect an item against a redesign refresh
	 * 
	 * @param itemName item name
	 * @param protect true to protect
	 */
	private void setProtected(String itemName, boolean protect) {
		List<String> retainFields = m_formNote.getItemValueStringList(NotesConstants.DESIGN_RETAIN_FIELDS);
		boolean isProtected = ListUtil.containsIgnoreCase(retainFields, itemName);
		
		if (isProtected==protect) {
			return;
		}
		
		List<String> newRetainFields = new ArrayList<>();
		
		if (protect) {
			newRetainFields.addAll(retainFields);
			newRetainFields.add(itemName);
		}
		else {
			for (String currItemName : retainFields) {
				if (!currItemName.equalsIgnoreCase(itemName)) {
					newRetainFields.add(currItemName);
				}
			}
		}
		m_formNote.replaceItemValue(NotesConstants.DESIGN_RETAIN_FIELDS, newRetainFields);
	}
	
	/**
	 * Checks if the $Readers item is protected against design refresh
	 * 
	 * @return true if protected
	 */
	public boolean isProtectReaders() {
		return isProtected(NotesConstants.DESIGN_READERS);
	}

	/**
	 * Protects/unprotects the $Readers item against design refresh.
	 * Call {@link #update()} afterwards to save your changes.
	 * 
	 * @param protect true to protect
	 */
	public void setProtectReaders(boolean protect) {
		setProtected(NotesConstants.DESIGN_READERS, protect);
	}

	/**
	 * Checks if the $FormUsers item is protected against design refresh
	 * 
	 * @return true if protected
	 */
	public boolean isProtectUsers() {
		return isProtected(NotesConstants.ITEM_NAME_FORMUSERS);
	}

	/**
	 * Protects/unprotects the $FormUsers item against design refresh.
	 * Call {@link #update()} afterwards to save your changes.
	 * 
	 * @param protect true to protect
	 */
	public void setProtectUsers(boolean protect) {
		setProtected(NotesConstants.ITEM_NAME_FORMUSERS, protect);
	}
	
	/**
	 * Saves the underlying note (calls {@link NotesNote#update(java.util.Set)}
	 * with {@link UpdateNote#FORCE}).
	 */
	public void update() {
		m_formNote.update(EnumSet.of(UpdateNote.FORCE));
	}
	
	/**
	 * Recycles the underlying form note
	 */
	public void recycle() {
		m_formNote.recycle();
	}
	
	public boolean isRecycled() {
		return m_formNote.isRecycled();
	}

	@Override
	public String toString() {
		if (m_formNote.isRecycled()) {
			return "NotesForm [recycled, noteid="+getNoteId()+"]";
		}
		else {
			return "NotesForm [name=" + getName() + ", aliases=" + getAliases() +
					", issubform="+isSubForm()+", noteid="+getNoteId()
					+ "]";
		}
	}
	
	
}
