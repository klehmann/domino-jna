package com.mindoo.domino.jna.constants;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * NIFReadEntries() returns these flags, as output, in the WORD specified by the <code>retSignalFlags</code> parameter.<br>
 * NIFReadEntries() may set multiple signal flags in the SignalFlags word by bitwise Or-ing the individual flag
 * values together.<br>
 * <br>
 * If NIFReadEntries() sets one of the flags represented by {@link #ANY_CONFLICT}, the collection has
 * changed since it was last read. Your program may respond to this signal by updating the collection
 * with {@link NotesCollection#update()}, resetting the COLLECTIONPOSITION, and calling NIFReadEntries() again.<br>
 * <br>
 * If NIFReadEntries() sets the {@link #MORE_TO_DO} flag, then the end of the collection has not
 * been reached and there exists more information than could fit in the return buffer.<br>
 * Your program may respond to this signal by calling NIFReadEntries again to retrieve an
 * additional buffer of information.
 * 
 * @author Karsten Lehmann
 */
public enum NIFSignal {
	/** At least one of the "definition" view items ($FORMULA, $COLLATION,
	 * or $FORMULACLASS) has been modified by another user since last ReadEntries.
	 * Upon receipt, you may wish to re-read the view note if up-to-date
	 * copies of these items are needed.<br>
	 * Upon receipt, you may also wish to re-synchronize your index position
	 * and re-read the rebuilt index.<br>
	 * <br>
	 * Signal returned only ONCE per detection */
	DEFN_ITEM_MODIFIED(NotesConstants.SIGNAL_DEFN_ITEM_MODIFIED),

	/** At least one of the non-"definition" view items ($TITLE,etc) has been
	 * modified since last ReadEntries.<br>
	 * Upon receipt, you may wish to re-read the view note if up-to-date
	 * copies of these items are needed.<br>
	 * <br>
	 * Signal returned only ONCE per detection */
	VIEW_ITEM_MODIFIED(NotesConstants.SIGNAL_VIEW_ITEM_MODIFIED),
	
	/** Collection index has been modified by another user since last ReadEntries.
	 * Upon receipt, you may wish to re-synchronize your index position
	 * and re-read the modified index.<br>
	 * <br>
	 * Signal returned only ONCE per detection */
	INDEX_MODIFIED(NotesConstants.SIGNAL_INDEX_MODIFIED),
	
	/** Unread list has been modified by another window using the same
	 * hCollection context<br>
	 * Upon receipt, you may wish to repaint the window if the window
	 * contains the state of unread flags (This signal is never generated
	 *  by NIF - only unread list users) */
	UNREADLIST_MODIFIED(NotesConstants.SIGNAL_UNREADLIST_MODIFIED),
	
	/** Collection is not up to date */
	DATABASE_MODIFIED(NotesConstants.SIGNAL_DATABASE_MODIFIED),
	
	/** End of collection has not been reached due to buffer being too full.
	 * The ReadEntries should be repeated to continue reading the desired entries. */
	MORE_TO_DO(NotesConstants.SIGNAL_MORE_TO_DO),
	
	/** The view contains a time-relative formula (e.g., @Now).  Use this flag to tell if the
	 * collection will EVER be up-to-date since time-relative views, by definition, are NEVER
	 * up-to-date. */
	VIEW_TIME_RELATIVE(NotesConstants.SIGNAL_VIEW_TIME_RELATIVE),
	
	/** Returned if signal flags are not supported<br>
	 * This is used by NIFFindByKeyExtended when it is talking to a pre-V4 server that does not
	 * support signal flags for FindByKey */
	NOT_SUPPORTED(NotesConstants.SIGNAL_NOT_SUPPORTED),
	
    /** The view contains documents with readers fields */
	VIEW_HASPRIVS (NotesConstants.SIGNAL_VIEW_HASPRIVS),

	/** Differential view read was requested but could not be done */
	DIFF_READ_NOT_DONE(NotesConstants.SIGNAL_DIFF_READ_NOT_DONE),
	
	/** Used to optimize NRPC transactions for a single doc unread state modification */
	SINGLENOTE_UNREAD_MODIFIED(NotesConstants.SIGNAL_SINGLENOTE_UNREAD_MODIFIED);

	private final short m_val;
	
	private NIFSignal(int val) {
		m_val = (short) (val & 0xffff);
	}
	
	public short getValue() {
		return m_val;
	}
	
	public static Set<NIFSignal> valuesOf(int bitMask) {
		EnumSet<NIFSignal> retFlags = EnumSet.noneOf(NIFSignal.class);
		for (NIFSignal currEnumVal : NIFSignal.values()) {
			if ((bitMask & currEnumVal.getValue()) == currEnumVal.getValue()) {
				retFlags.add(currEnumVal);
			}
		}
		return retFlags;
	}

	/**	Mask that defines all "sharing conflicts", which are cases when
	the database or collection has changed out from under the user. */
	public static final Set<NIFSignal> ANY_CONFLICT = Arrays.asList(
			DEFN_ITEM_MODIFIED,
			VIEW_ITEM_MODIFIED,
			INDEX_MODIFIED,
			UNREADLIST_MODIFIED,
			DATABASE_MODIFIED
			)
			.stream()
			.collect(Collectors.toSet());
	
	/**	Mask that defines all "sharing conflicts" except for SIGNAL_DATABASE_MODIFIED.
	This can be used in combination with SIGNAL_VIEW_TIME_RELATIVE to tell if
	the database or collection has truly changed out from under the user or if the
	view is a time-relative view which will NEVER be up-to-date.  SIGNAL_DATABASE_MODIFIED
	is always returned for a time-relative view to indicate that it is never up-to-date. */
	public static final Set<NIFSignal> ANY_NONDATA_CONFLICT = Arrays.asList(
			DEFN_ITEM_MODIFIED,
			VIEW_ITEM_MODIFIED,
			INDEX_MODIFIED,
			UNREADLIST_MODIFIED
			)
			.stream()
			.collect(Collectors.toSet());
	
}
