package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.richtext.FontStyle;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Abstract base class for a mail marge of richtext
 * 
 * @author Karsten Lehmann
 */
public abstract class AbstractMailMergeConversion implements IRichTextConversion {
	
	public AbstractMailMergeConversion() {
	}
	
	@Override
	public boolean isMatch(IRichTextNavigator nav) {
		//TODO add support for search word recognition across CD record boundaries like in this JS lib: https://github.com/padolsey/findAndReplaceDOMText
		if (nav.gotoFirst()) {
			do {
				if (CDRecordType.TEXT.getConstant() == nav.getCurrentRecordTypeAsShort()) {
//					typedef struct {
//						   WSIG   Header; /* Tag and length */
//						   FONTID FontID; /* Font ID */
//						/* The 8-bit text string follows... */
//						} CDTEXT;
					Memory recordData = nav.getCurrentRecordData();
					int recordDataLength = nav.getCurrentRecordDataLength();
					int txtMemLength = recordDataLength-4;
					
					if (txtMemLength>0) {
						//skip FONTID
						Pointer txtPtr = recordData.share(4);
						String txt = NotesStringUtils.fromLMBCS(txtPtr, txtMemLength);
						
						if (containsMatch(txt)) {
							return true;
						}
					}
				}
			}
			while (nav.gotoNext());
		}
		return false;
	}

	protected abstract boolean containsMatch(String txt);
	
	protected abstract String replaceAllMatches(String txt);
	
	@Override
	public void convert(IRichTextNavigator source, ICompoundText target) {
		if (source.gotoFirst()) {
			do {
				if (CDRecordType.TEXT.getConstant() == source.getCurrentRecordTypeAsShort()) {
//					typedef struct {
//						   WSIG   Header; /* Tag and length */
//						   FONTID FontID; /* Font ID */
//						/* The 8-bit text string follows... */
//						} CDTEXT;
					Memory recordData = source.getCurrentRecordData();
					int recordDataLength = source.getCurrentRecordDataLength();
					final byte[] fontIdArr = recordData.getByteArray(0, 4);
					int txtMemLength = recordDataLength-4;
					
					if (txtMemLength>0) {
						//skip FONTID
						Pointer txtPtr = recordData.share(4);
						String txt = NotesStringUtils.fromLMBCS(txtPtr, txtMemLength);
						
						if (containsMatch(txt)) {
							String newTxt = replaceAllMatches(txt);
							//add text, prevent creating extra linebreaks for newlines (false parameter)
							target.addText(newTxt, null, new FontStyle(new IAdaptable() {
								
								@Override
								public <T> T getAdapter(Class<T> clazz) {
									if (clazz==byte[].class) {
										return (T) fontIdArr;
									}
									else
										return null;
								}
							}), false);
						}
						else {
							source.copyCurrentRecordTo(target);
						}
					}
					else {
						source.copyCurrentRecordTo(target);
					}
				}
				else {
					source.copyCurrentRecordTo(target);
				}
			}
			while (source.gotoNext());
		}
	}
}
