package com.mindoo.domino.jna.richtext.conversion;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.internal.FieldPropAdaptable;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDEmbeddedCtlStruct;
import com.mindoo.domino.jna.richtext.FieldInfo;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.IRichTextNavigator.RichTextNavPosition;
import com.mindoo.domino.jna.utils.StringTokenizerExt;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;

/**
 * Design richtext conversion that changes the width of fields to x % of their parent
 * container when they have a special HTML attribute ctlwidth="50%". This is useful
 * to make listboxes and comboboxes responsive which they currently are
 * not when created / saved in Domino Designer (they have a fixed width).
 * 
 * @author Karsten Lehmann
 */
public class FixFieldResponsivenessConversion implements IRichTextConversion {
	private ThreadLocal<Boolean> currentThreadInBeginEndBlock = new ThreadLocal<Boolean>();

	@Override
	public void richtextNavigationStart() {
		currentThreadInBeginEndBlock.set(Boolean.FALSE);
	}

	@Override
	public void richtextNavigationEnd() {
	}
	
	/**
	 * Scans the richtext field starting at the current position and tries to find
	 * CDFIELD and CDIDNAME records with field information. The current richtext
	 * position is restored at the end of the method.
	 * 
	 * @param nav richtext navigator
	 * @return field info or null if no field could be found
	 */
	private FieldInfo findNextFieldAndRestorePos(IRichTextNavigator nav) {
		RichTextNavPosition storedPos = nav.getCurrentRecordPosition();
		try {
			Memory cdFieldRecordDataWithHeader = null;
			Memory cdIdNameRecordDataWithHeader = null;
			
			do {
				if (nav.getCurrentRecordTypeAsShort() == CDRecordType.END.getConstant() ||
						nav.getCurrentRecordTypeAsShort() == CDRecordType.BEGIN.getConstant()) {
					//current BEGIN/END block is done
					break;
				}
				else if (nav.getCurrentRecordTypeAsShort() == CDRecordType.FIELD.getConstant()) {
					cdFieldRecordDataWithHeader = nav.getCurrentRecordDataWithHeader();
				}
				else if (CDRecordType.IDNAME.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					cdIdNameRecordDataWithHeader = nav.getCurrentRecordDataWithHeader();
				}

				if (cdFieldRecordDataWithHeader!=null && cdIdNameRecordDataWithHeader!=null) {
					break;
				}
			}
			while (nav.gotoNext());
			
			if (cdFieldRecordDataWithHeader!=null) {
				FieldInfo fld = new FieldInfo(new FieldPropAdaptable(cdFieldRecordDataWithHeader, cdIdNameRecordDataWithHeader));

				return fld;
			}

			return null;
		}
		finally {
			nav.restoreCurrentRecordPosition(storedPos);
		}
	}
	
	/**
	 * Analyzes the field properties to see if it needs any responsive design fixes,
	 * e.g. stretching the field width
	 * 
	 * @param fld field 
	 * @return list of fixes to apply
	 */
	protected List<ResponsiveFix> getResponsiveFieldFixes(FieldInfo fld) {
		List<ResponsiveFix> fixes = new ArrayList<ResponsiveFix>();

		String htmlExtraAttr = fld.getHtmlExtraAttr();
		if (!StringUtil.isEmpty(htmlExtraAttr)) {
			StringTokenizerExt st = new StringTokenizerExt(htmlExtraAttr, " ");
			while (st.hasMoreTokens()) {
				String currToken = st.nextToken();

				if (StringUtil.startsWithIgnoreCase(currToken, "ctlwidth=")) {
					String val = currToken.substring("ctlwidth=".length());
					if (val.startsWith("\"")) {
						val = val.substring(1);
					}
					if (val.endsWith("\"")) {
						val = val.substring(0, val.length()-1);
					}

					if (val.endsWith("%")) {
						String numPercStr = val.substring(0, val.length()-1);
						try {
							int numPerc = Integer.parseInt(numPercStr);
							fixes.add(new SetFieldWidth(numPerc));
						}
						catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return fixes;
	}

	@Override
	public boolean isMatch(IRichTextNavigator nav) {
		if (nav.gotoFirst()) {
			do {
				if (CDRecordType.BEGIN.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					currentThreadInBeginEndBlock.set(Boolean.TRUE);
				}
				else if (CDRecordType.END.getConstant() == nav.getCurrentRecordTypeAsShort()) {
					currentThreadInBeginEndBlock.set(Boolean.FALSE);
				}
				else if (nav.getCurrentRecordTypeAsShort() == CDRecordType.EMBEDDEDCTL.getConstant()) {
					if (Boolean.TRUE.equals(currentThreadInBeginEndBlock.get())) {
						//find CDFIELD and CDIDNAME records with field information to see
						//if this field requires any responsive fixes
						FieldInfo fld = findNextFieldAndRestorePos(nav);
						if (fld!=null) {
							List<ResponsiveFix> responsiveFixes = getResponsiveFieldFixes(fld);
							if (!responsiveFixes.isEmpty()) {
								return true;
							}
						}
					}
				}
			}
			while (nav.gotoNext());
		}
		return false;
	}
	
	@Override
	public void convert(IRichTextNavigator source, ICompoundText target) {
		if (source.gotoFirst()) {
			do {
				boolean processed = false;
				
				if (CDRecordType.BEGIN.getConstant() == source.getCurrentRecordTypeAsShort()) {
					currentThreadInBeginEndBlock.set(Boolean.TRUE);
				}
				else if (CDRecordType.END.getConstant() == source.getCurrentRecordTypeAsShort()) {
					currentThreadInBeginEndBlock.set(Boolean.FALSE);
				}
				else if (source.getCurrentRecordTypeAsShort() == CDRecordType.EMBEDDEDCTL.getConstant()) {
					if (Boolean.TRUE.equals(currentThreadInBeginEndBlock.get())) {
						RichTextNavPosition oldPos = source.getCurrentRecordPosition();
						//find CDFIELD and CDIDNAME records with field information to see
						//if this field requires any responsive fixes
						FieldInfo fld = findNextFieldAndRestorePos(source);
						RichTextNavPosition newPos = source.getCurrentRecordPosition();
						if (!newPos.equals(oldPos)) {
							throw new IllegalStateException("Richtext position restore failed, "+newPos+"!="+oldPos);
						}
						
						if (fld!=null) {
							List<ResponsiveFix> responsiveFixes = getResponsiveFieldFixes(fld);
							if (!responsiveFixes.isEmpty()) {
								Memory recordMemWithHeader = source.getCurrentRecordDataWithHeader();
								NotesCDEmbeddedCtlStruct embCtl = NotesCDEmbeddedCtlStruct.newInstance(recordMemWithHeader);
								embCtl.read();

								for (ResponsiveFix currFix : responsiveFixes) {
									currFix.apply(embCtl);
								}

								embCtl.write();

								target.addCDRecords(recordMemWithHeader);
								processed = true;
							}
						}
					}
				}

				if (!processed) {
					source.copyCurrentRecordTo(target);
				}
			}
			while (source.gotoNext());
		}
	}
	
	protected static abstract class ResponsiveFix {
		
		public abstract void apply(NotesCDEmbeddedCtlStruct embCtx);
		
	}
	
	protected static class SetFieldWidth extends ResponsiveFix {
		private int m_widthPerc;
		
		public SetFieldWidth(int widthPerc) {
			m_widthPerc = widthPerc;
			
			if (m_widthPerc<0) {
				m_widthPerc = 1;
			}
			else if (m_widthPerc>100) {
				m_widthPerc = 100;
			}
		}
		
		@Override
		public void apply(NotesCDEmbeddedCtlStruct embCtx) {
			embCtx.Flags = NotesConstants.EC_FLAG_FITTOWINDOW;

			// let control fill 50% of the paragraph
			embCtx.Percentage = (short) (m_widthPerc & 0xffff);
		}
	}


}
