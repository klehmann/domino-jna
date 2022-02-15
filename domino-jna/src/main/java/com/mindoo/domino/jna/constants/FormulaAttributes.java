package com.mindoo.domino.jna.constants;

import java.util.EnumSet;
import java.util.Set;

import com.mindoo.domino.jna.internal.NotesConstants;

public enum FormulaAttributes {

    /** TRUE if formula is completely self-contained, and uses only constants.<br>
     * That is, it doesn't have to be re-executed for each note, for example. */
    CONSTANT(NotesConstants.FA_CONSTANT),
    /** TRUE if formula uses @ functions which are based on the current clock time
     * (and therefore, may produce a different result depending on when you execute it). */
    TIME_VARIANT(NotesConstants.FA_TIME_VARIANT),
    /** TRUE if formula is simply a reference to a single field name, and nothing else.<br>
     * (If so, we could use the expression as a column title, for example). */
    ONLY_FIELD_NAME(NotesConstants.FA_ONLY_FIELD_NAME),
    /** contains <code>@DocSiblings</code> */
    FUNC_SIBLINGS(NotesConstants.FA_FUNC_SIBLINGS),
    /** contains <code>@DocChildren</code> */
    FUNC_CHILDREN(NotesConstants.FA_FUNC_CHILDREN),
    /** TRUE if <code>@DocDescendants</code> used */
    FUNC_DESCENDANTS(NotesConstants.FA_FUNC_DESCENDANTS),
    /** TRUE if <code>@AllChildren</code> used */
    FUNC_ALLCHILDREN(NotesConstants.FA_FUNC_ALLCHILDREN),
    /** TRUE if <code>@AllDescendants</code> used */
    FUNC_ALLDESCENDANTS(NotesConstants.FA_FUNC_ALLDESCENDANTS),
    /** TRUE if formula only contains <code>SELECT @all</code> */
    ONLY_SELECTALL(NotesConstants.FA_ONLY_SELECTALL),
    /** TRUE if formula is composed of a single <code>@Command</code> expression or single <code>@URLOpen</code>
     * with or without a single <code>@SetTargetFrame</code> (with possibly a single @All).<br>
     * There can be no item references. */
    ONLY_CONST_COMMAND_AND_SETTARGETFRAME(NotesConstants.FA_ONLY_CONST_COMMAND_AND_SETTARGETFRAME),
    /** TRUE if <code>@SetTargetFrame</code> used. */
    FUNC_SETTARGETFRAME(NotesConstants.FA_FUNC_SETTARGETFRAME);
	
	private int m_val;
	
	private FormulaAttributes(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}

	public static EnumSet<FormulaAttributes> toFormulaAttributes(int bitMask) {
		EnumSet<FormulaAttributes> set = EnumSet.noneOf(FormulaAttributes.class);
		for (FormulaAttributes currClass : values()) {
			if ((bitMask & currClass.getValue()) == currClass.getValue()) {
				set.add(currClass);
			}
		}
		return set;
	}
	
	public static int toBitMask(Set<FormulaAttributes> set) {
		int result = 0;
		if (set!=null) {
			for (FormulaAttributes currAttr : values()) {
				if (set.contains(currAttr)) {
					result = result | currAttr.getValue();
				}
			}
		}
		return result;
	}

}
