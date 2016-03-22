package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesCollection;

/**
 * These flags control how
 * {@link NotesCollection#readEntries(com.weilgut.xlayer.domino.jna.structs.NotesCollectionPosition, short, int, short, int, int, boolean[])}
 * steps through a collection.<br>
 * <br>
 * The flags are used to control both the order in which NIFReadEntries:<br>
 * skips notes in a collection before reading any notes, and navigates the collection while it is being read.
 * 
 * @author Karsten Lehmann
 */
public interface INavigateConstants {

	/** Remain at current position (reset position & return data). */
	public static final short NAVIGATE_CURRENT	= 0;	
	/** Up 1 level */
	public static final short NAVIGATE_PARENT = 3;	
	/** Down 1 level to first child */
	public static final short NAVIGATE_CHILD = 4;
	/** First node at our level */
	public static final short NAVIGATE_FIRST_PEER = 7;
	/** Last node at our level */
	public static final short NAVIGATE_LAST_PEER = 8;
	
	/** Next node at our level */
	public static final short NAVIGATE_NEXT_PEER = 5;
	/** Prev node at our level */
	public static final short NAVIGATE_PREV_PEER = 6;
	/** Highest level non-category entry */
	public static final short NAVIGATE_CURRENT_MAIN	= 11;
	/** CURRENT_MAIN, then NEXT_PEER */
	public static final short NAVIGATE_NEXT_MAIN = 12;
	/** CURRENT_MAIN, then PREV_PEER only if already there */
	public static final short NAVIGATE_PREV_MAIN = 13;
	/** PARENT, then NEXT_PEER */
	public static final short NAVIGATE_NEXT_PARENT = 19;
	/** PARENT, then PREV_PEER */
	public static final short NAVIGATE_PREV_PARENT = 20;

	/** /* Next entry over entire tree (parent first, then children,...) */
	public static final short NAVIGATE_NEXT	= 1;
	
	/** Previous entry over entire tree (opposite order of PREORDER) */
	public static final short NAVIGATE_PREV = 9;
	
	/**  NEXT, but only descendants below NIFReadEntries() - StartPos. */
	public static final short NAVIGATE_ALL_DESCENDANTS = 17;
	
	/** NEXT, but only "unread" entries */
	public static final short NAVIGATE_NEXT_UNREAD = 10;
	/** NEXT_UNREAD, but stop at main note also */
	public static final short NAVIGATE_NEXT_UNREAD_MAIN = 18;
	/** Previous unread main. */
	public static final short NAVIGATE_PREV_UNREAD_MAIN = 34;
	/** PREV, but only "unread" entries */
	public static final short NAVIGATE_PREV_UNREAD = 21;
	/** NEXT, but only "selected" entries */
	public static final short NAVIGATE_NEXT_SELECTED = 14;
	/** PREV, but only "selected" entries */
	public static final short NAVIGATE_PREV_SELECTED = 22;
	/** Next selected main. (Next unread main can be found above.) */
	public static final short NAVIGATE_NEXT_SELECTED_MAIN = 32;
	/** Previous selected main. */
	public static final short NAVIGATE_PREV_SELECTED_MAIN = 33;
	/** NEXT, but only "expanded" entries */
	public static final short NAVIGATE_NEXT_EXPANDED = 15;
	/** PREV, but only "expanded" entries */
	public static final short NAVIGATE_PREV_EXPANDED = 16;
	/** NEXT, but only "expanded" AND "unread" entries */
	public static final short NAVIGATE_NEXT_EXPANDED_UNREAD = 23;
	/** PREV, but only "expanded" AND "unread" entries */
	public static final short NAVIGATE_PREV_EXPANDED_UNREAD = 24;
	/** NEXT, but only "expanded" AND "selected" entries */
	public static final short NAVIGATE_NEXT_EXPANDED_SELECTED = 25;
	/** PREV, but only "expanded" AND "selected" entries */
	public static final short NAVIGATE_PREV_EXPANDED_SELECTED = 26;
	/** NEXT, but only "expanded" AND "category" entries */
	public static final short NAVIGATE_NEXT_EXPANDED_CATEGORY = 27;
	/** PREV, but only "expanded" AND "category" entries */
	public static final short NAVIGATE_PREV_EXPANDED_CATEGORY = 28;
	/** NEXT, but only "expanded" "non-category" entries */
	public static final short NAVIGATE_NEXT_EXP_NONCATEGORY	= 39;
	/** PREV, but only "expanded" "non-category" entries */
	public static final short NAVIGATE_PREV_EXP_NONCATEGORY	= 40;
	/** NEXT, but only FTSearch "hit" entries (in the SAME ORDER as the hit's relevance ranking) */
	public static final short NAVIGATE_NEXT_HIT	= 29;
	
	/** PREV, but only FTSearch "hit" entries (in the SAME ORDER as the hit's relevance ranking) */
	public static final short NAVIGATE_PREV_HIT	= 30;
	
	/** Remain at current position in hit's relevance rank array (in the order of the hit's relevance ranking) */
	public static final short NAVIGATE_CURRENT_HIT = 31;
	
	/** NEXT, but only "selected" and FTSearch "hit" entries (in the SAME ORDER as the hit's relevance ranking) */
	public static final short NAVIGATE_NEXT_SELECTED_HIT = 35;
	
	/** PREV, but only "selected" and FTSearch "hit" entries (in the SAME ORDER as the hit's relevance ranking) */
	public static final short NAVIGATE_PREV_SELECTED_HIT = 36;
	
	/** NEXT, but only "unread" and FTSearch "hit" entries (in the SAME ORDER as the hit's relevance ranking) */
	public static final short NAVIGATE_NEXT_UNREAD_HIT = 37;
	
	/** PREV, but only "unread" and FTSearch "hit" entries (in the SAME ORDER as the hit's relevance ranking) */
	public static final short NAVIGATE_PREV_UNREAD_HIT = 38;
	
	/** NEXT, but only "category" entries */
	public static final short NAVIGATE_NEXT_CATEGORY = 41;
	/** PREV, but only "category" entries */
	public static final short NAVIGATE_PREV_CATEGORY = 42;
	/** NEXT, but only "non-category" entries */
	public static final short NAVIGATE_NEXT_NONCATEGORY = 43;
	/** PREV, but only "non-category" entries */
	public static final short NAVIGATE_PREV_NONCATEGORY = 44;

	public static final short NAVIGATE_MASK	= 0x007F;	/* Navigator code (see above) */


	/*
	 * Flag which can be used with ALL navigators which causes the navigation
	 * to be limited to entries at a specific level (specified by the
	 * field "MinLevel" in the collection position) or any higher levels
	 * but never a level lower than the "MinLevel" level.  Note that level 0
	 * means the top level of the index, so the term "minimum level" really
	 * means the "highest level" the navigation can move to.
	 * This can be used to find all entries below a specific position
	 * in the index, limiting yourself only to that subindex, and yet be
	 * able to use any of the navigators to move around within that subindex.
	 * This feature was added in Version 4 of Notes, so it cannot be used
	 * with earlier Notes Servers.
	 */

	/** Honor "Minlevel" field in position */
	public static final short NAVIGATE_MINLEVEL	= 0x0100;
	/** Honor "Maxlevel" field in position */
	public static final short NAVIGATE_MAXLEVEL	= 0x0200;

	/*
	 * This flag can be combined with any navigation directive to 
	 * prevent having a navigation (Skip) failure abort the (ReadEntries) operation. 
	 * For example, this is used by the Notes user interface when
	 * getting the entries to display in the view, so that if an attempt is made to 
	 * skip past either end of the index (e.g. using PageUp/PageDown), 
	 * the skip will be left at the end of the index, and the return will return 
	 * whatever can be returned using the separate return navigator. 
	 * 
	 * This flag is also used to get the "last" N entries of a view by setting the 
	 * Skip Navigator to NAVIGATE_NEXT | NAVIGATE_CONTINUE, setting the SkipCount to MAXDWORD,
	 * setting the ReturnNavigator to NAVIGATE_PREV_EXPANDED, and setting the ReturnCount 
	 * to N (N must be greater than 0).
	 */

	/** "Return" even if "Skip" error */
	public static final short NAVIGATE_CONTINUE	= (short) (0x8000 & 0xffff);

}
