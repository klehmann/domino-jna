package com.mindoo.domino.jna.richtext;

import java.util.Arrays;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.compoundtext.NotesCompoundStyleStruct;

/**
 * Container for paragraph style attributes used in richtext items
 * 
 * @author Karsten Lehmann
 */
public class TextStyle implements IAdaptable {
	String m_styleName;
	
	/** paragraph justification type */
	short m_justifyMode;
	/** Line spacing */
	short m_lineSpacing;
	/** # units above paragraph */
	short m_paragraphSpacingBefore;
	/** # units below paragraph */
	short m_paragraphSpacingAfter;
	/** leftmost margin in twips */
	short m_leftMargin;
	/** rightmost margin in twips */
	short m_rightMargin;
	/** leftmost margin on first line */
	short m_firstLineLeftMargin;
	/** # tab stops in table */
	short m_tabs;
	/**
	 * table of tab stops<br>
	 * C type : signed short[20]
	 */
	short[] m_tab = new short[20];
	/** paragraph attribute flags */
	short m_flags;
	
	public TextStyle(String styleName) {
		if (styleName==null)
			throw new NullPointerException("Style name cannot be null");
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		m_styleName = styleName;
		NotesCompoundStyleStruct style = NotesCompoundStyleStruct.newInstance();
		notesAPI.CompoundTextInitStyle(style);
		style.read();
		m_justifyMode = style.JustifyMode;
		m_lineSpacing = style.LineSpacing;
		m_paragraphSpacingBefore = style.ParagraphSpacingBefore;
		m_paragraphSpacingAfter = style.ParagraphSpacingAfter;
		m_leftMargin = style.LeftMargin;
		m_rightMargin = style.RightMargin;
		m_firstLineLeftMargin = style.FirstLineLeftMargin;
		m_tabs = style.Tabs;
		m_tab = style.Tab==null ? null : style.Tab.clone();
		m_flags = style.Flags;
		//unlink hide flags so that we can set hide when for preview
		m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_UNLINK) & 0xffff);
	}
	
	public String getName() {
		return m_styleName;
	}
	
	public TextStyle setAlign(Justify align) {
		m_justifyMode = align.getConstant();
		return this;
	}
	
	public Justify getAlign() {
		switch (m_justifyMode) {
		case NotesCAPI.JUSTIFY_LEFT:
			return Justify.LEFT;
		case NotesCAPI.JUSTIFY_RIGHT:
			return Justify.RIGHT;
		case NotesCAPI.JUSTIFY_BLOCK:
			return Justify.BLOCK;
		case NotesCAPI.JUSTIFY_CENTER:
			return Justify.CENTER;
		case NotesCAPI.JUSTIFY_NONE:
			return Justify.NONE;
		default:
			return null;
		}
	}
	
	public int getLineSpacing() {
		return (int) (m_lineSpacing & 0xffff);
	}
	
	public TextStyle setLineSpacing(int spacing) {
		if (spacing<0 || spacing>65535) {
			throw new IllegalArgumentException("Value must be between 0 and 65535");
		}
		m_lineSpacing = (short) (spacing & 0xffff);
		return this;
	}
	
	public int getParagraphSpacingBefore() {
		return (int) (m_paragraphSpacingBefore & 0xffff);
	}
	
	public TextStyle setParagraphSpacingBefore(int spacing) {
		if (spacing<0 || spacing>65535) {
			throw new IllegalArgumentException("Value must be between 0 and 65535");
		}
		m_paragraphSpacingBefore = (short) (spacing & 0xffff);
		return this;
	}

	public int getParagraphSpacingAfter() {
		return (int) (m_paragraphSpacingAfter& 0xffff);
	}

	public TextStyle setParagraphSpacingAfter(int spacing) {
		if (spacing<0 || spacing>65535) {
			throw new IllegalArgumentException("Value must be between 0 and 65535");
		}
		m_paragraphSpacingAfter = (short) (spacing & 0xffff);
		return this;
	}

	public double getLeftMargin() {
		 //there are 72 * 20 TWIPS to an inch
		return m_leftMargin / NotesCAPI.ONEINCH;
	}
	
	public TextStyle setLeftMargin(double margin) {
		double result = margin * NotesCAPI.ONEINCH;
		if (result < 0 || result > 65535)
			throw new IllegalArgumentException("Value must be between 0 and "+(65535/NotesCAPI.ONEINCH));
		m_leftMargin = (short) result;
		return this;
	}
	
	public double getRightMargin() {
		 //there are 72 * 20 TWIPS to an inch
		return m_rightMargin / NotesCAPI.ONEINCH;
	}
	
	public TextStyle setRightMargin(double margin) {
		double result = margin * NotesCAPI.ONEINCH;
		if (result < 0 || result > 65535)
			throw new IllegalArgumentException("Value must be between 0 and "+(65535/NotesCAPI.ONEINCH));
		m_rightMargin = (short) result;
		return this;
	}

	public double getFirstLineLeftMargin() {
		 //there are 72 * 20 TWIPS to an inch
		return m_firstLineLeftMargin / NotesCAPI.ONEINCH;
	}
	
	public TextStyle setFirstLineLeftMargin(double margin) {
		double result = margin * NotesCAPI.ONEINCH;
		if (result < 0 || result > 65535)
			throw new IllegalArgumentException("Value must be between 0 and "+(65535/NotesCAPI.ONEINCH));
		m_firstLineLeftMargin = (short) result;
		return this;
	}

	public int getTabsInTable() {
		return (int) (m_tabs & 0xffff);
	}
	
	public TextStyle setTabsInTable(int tabs) {
		if (tabs<0 || tabs>65535)
			throw new IllegalArgumentException("Value must be between 0 and 65535");
		m_tabs = (short) (tabs & 0xffff);
		return this;
	}
	
	public TextStyle setTabPositions(short[] tabPos) {
		m_tab = new short[20];
		for (int i=0; i<20; i++) {
			if (tabPos.length>=i) {
				m_tab[i] = tabPos[i];
			}
			else {
				m_tab[i] = tabPos[tabPos.length-1];
			}
		}
		return this;
	}
	
	public short[] getTabPositions() {
		return m_tab.clone();
	}
	
	public TextStyle setPaginateBefore(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_PAGINATE_BEFORE) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_PAGINATE_BEFORE) & 0xffff);
		}
		return this;
	}

	public boolean isPaginateBefore() {
		return (m_flags & NotesCAPI.PABFLAG_PAGINATE_BEFORE) == NotesCAPI.PABFLAG_PAGINATE_BEFORE;
	}
	
	public TextStyle setKeepWithNext(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_KEEP_WITH_NEXT) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_KEEP_WITH_NEXT) & 0xffff);
		}
		return this;
	}

	public boolean isKeepWithNext() {
		return (m_flags & NotesCAPI.PABFLAG_KEEP_WITH_NEXT) == NotesCAPI.PABFLAG_KEEP_WITH_NEXT;
	}

	public TextStyle setKeepTogether(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_KEEP_TOGETHER) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_KEEP_TOGETHER) & 0xffff);
		}
		return this;
	}

	public boolean isKeepTogether() {
		return (m_flags & NotesCAPI.PABFLAG_KEEP_TOGETHER) == NotesCAPI.PABFLAG_KEEP_TOGETHER;
	}

	public TextStyle setHideReadOnly(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_RO) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_HIDE_RO) & 0xffff);
		}
		return this;
	}

	public boolean isHideReadOnly() {
		return (m_flags & NotesCAPI.PABFLAG_HIDE_RO) == NotesCAPI.PABFLAG_HIDE_RO;
	}
	
	public TextStyle setHideReadWrite(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_RW) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_HIDE_RW) & 0xffff);
		}
		return this;
	}

	public boolean isHideReadWrite() {
		return (m_flags & NotesCAPI.PABFLAG_HIDE_RW) == NotesCAPI.PABFLAG_HIDE_RW;
	}
	
	public TextStyle setHideWhenPrinting(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_PR) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_HIDE_PR) & 0xffff);
		}
		return this;
	}

	public boolean isHideWhenPrinting() {
		return (m_flags & NotesCAPI.PABFLAG_HIDE_PR) == NotesCAPI.PABFLAG_HIDE_PR;
	}
	
	public TextStyle setHideWhenCopied(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_CO) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_HIDE_CO) & 0xffff);
		}
		return this;
	}

	public boolean isHideWhenCopied() {
		return (m_flags & NotesCAPI.PABFLAG_HIDE_CO) == NotesCAPI.PABFLAG_HIDE_CO;
	}

	public TextStyle setHideWhenPreviewed(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_PV) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_HIDE_PV) & 0xffff);
		}
		return this;
	}

	public boolean isHideWhenEditedInPreview() {
		return (m_flags & NotesCAPI.PABFLAG_HIDE_PV) == NotesCAPI.PABFLAG_HIDE_PV;
	}

	public TextStyle setHideWhenEditedInPreview(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_HIDE_PVE) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_HIDE_PVE) & 0xffff);
		}
		return this;
	}

	public boolean isHideWhenPreviewed() {
		return (m_flags & NotesCAPI.PABFLAG_HIDE_PVE) == NotesCAPI.PABFLAG_HIDE_PVE;
	}

	public TextStyle setDisplayAsNumberedList(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_NUMBEREDLIST) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_NUMBEREDLIST) & 0xffff);
		}
		return this;
	}

	public boolean isDisplayAsNumberedList() {
		return (m_flags & NotesCAPI.PABFLAG_NUMBEREDLIST) == NotesCAPI.PABFLAG_NUMBEREDLIST;
	}

	public TextStyle setDisplayAsBulletList(boolean b) {
		if (b) {
			m_flags = (short) ((m_flags | NotesCAPI.PABFLAG_BULLET) & 0xffff);
		}
		else {
			m_flags = (short) ((m_flags & ~NotesCAPI.PABFLAG_BULLET) & 0xffff);
		}
		return this;
	}

	public boolean isDisplayAsBulletList() {
		return (m_flags & NotesCAPI.PABFLAG_BULLET) == NotesCAPI.PABFLAG_BULLET;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_firstLineLeftMargin;
		result = prime * result + m_flags;
		result = prime * result + m_justifyMode;
		result = prime * result + m_leftMargin;
		result = prime * result + m_lineSpacing;
		result = prime * result + m_paragraphSpacingAfter;
		result = prime * result + m_paragraphSpacingBefore;
		result = prime * result + m_rightMargin;
		result = prime * result + ((m_styleName == null) ? 0 : m_styleName.hashCode());
		result = prime * result + Arrays.hashCode(m_tab);
		result = prime * result + m_tabs;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TextStyle other = (TextStyle) obj;
		if (m_firstLineLeftMargin != other.m_firstLineLeftMargin)
			return false;
		if (m_flags != other.m_flags)
			return false;
		if (m_justifyMode != other.m_justifyMode)
			return false;
		if (m_leftMargin != other.m_leftMargin)
			return false;
		if (m_lineSpacing != other.m_lineSpacing)
			return false;
		if (m_paragraphSpacingAfter != other.m_paragraphSpacingAfter)
			return false;
		if (m_paragraphSpacingBefore != other.m_paragraphSpacingBefore)
			return false;
		if (m_rightMargin != other.m_rightMargin)
			return false;
		if (m_styleName == null) {
			if (other.m_styleName != null)
				return false;
		} else if (!m_styleName.equals(other.m_styleName))
			return false;
		if (!Arrays.equals(m_tab, other.m_tab))
			return false;
		if (m_tabs != other.m_tabs)
			return false;
		return true;
	}

	public static enum Justify {
		/** flush left, ragged right */
		LEFT (NotesCAPI.JUSTIFY_LEFT),
		/** flush right, ragged left */
		RIGHT(NotesCAPI.JUSTIFY_RIGHT),
		/** full block justification */
		BLOCK(NotesCAPI.JUSTIFY_BLOCK),
		/** centered */
		CENTER(NotesCAPI.JUSTIFY_CENTER),
		/** no line wrapping AT ALL (except hard CRs) */
		NONE(NotesCAPI.JUSTIFY_NONE);
		
		private short m_constant;
		
		private Justify(short constant) {
			m_constant = constant;
		}
		
		public short getConstant() {
			return m_constant;
		}
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz==NotesCompoundStyleStruct.class) {
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
			NotesCompoundStyleStruct styleStruct = NotesCompoundStyleStruct.newInstance();
			notesAPI.CompoundTextInitStyle(styleStruct);
			styleStruct.read();
			styleStruct.JustifyMode = m_justifyMode;
			styleStruct.LineSpacing = m_lineSpacing;
			styleStruct.ParagraphSpacingBefore = m_paragraphSpacingBefore;
			styleStruct.ParagraphSpacingAfter = m_paragraphSpacingAfter;
			styleStruct.LeftMargin = m_leftMargin;
			styleStruct.RightMargin = m_rightMargin;
			styleStruct.FirstLineLeftMargin = m_firstLineLeftMargin;
			styleStruct.Tabs = m_tabs;
			styleStruct.Tab = m_tab==null ? null : m_tab.clone();
			styleStruct.Flags = m_flags;
			styleStruct.write();
			return (T) styleStruct;
		}
		else
			return null;
	}
}
