package com.mindoo.domino.jna.richtext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.internal.FieldPropAdaptable;
import com.mindoo.domino.jna.internal.FormulaDecompiler;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDResourceStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCdHotspotBeginStruct;
import com.mindoo.domino.jna.richtext.IRichTextNavigator.RichTextNavPosition;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.Pair;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Collection of useful methods to process richtext CD records
 * 
 * @author Karsten Lehmann
 */
public class RichTextUtils {

	/**
	 * Calls {@link #collectFields(IRichTextNavigator)} and just returns the
	 * field names
	 * 
	 * @param rtNav richtext navigator
	 * @return field names
	 */
	public static List<String> collectFieldNames(IRichTextNavigator rtNav) {
		List<FieldInfo> fields = collectFields(rtNav);
		List<String> fieldNames = new ArrayList<String>(fields.size());
		for (FieldInfo currField : fields) {
			fieldNames.add(currField.getName());
		}
		return fieldNames;
	}
	
	/**
	 * Traverses the richtext CD records (of a design element's $body item)
	 * and collects all contained fields
	 * with data type, name, description and formulas. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param rtNav richtext navigator
	 * @return fields
	 */
	public static List<FieldInfo> collectFields(IRichTextNavigator rtNav) {
		List<FieldInfo> fields = new ArrayList<FieldInfo>();
		
		if (rtNav==null) {
			return fields;
		}
		
		RichTextNavPosition oldPos = rtNav.getCurrentRecordPosition();
		
		if (rtNav.gotoFirst()) {
			boolean inBeginEnd = false;
			
			do {
				if (CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = true;
				}
				else if (CDRecordType.END.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = false;
				}
				else if (CDRecordType.FIELD.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					//found record of type SIG_CD_FIELD
					Memory fieldRecordWithHeader = rtNav.getCurrentRecordDataWithHeader();
					
					//search for additional fields
					RichTextNavPosition fieldPos = rtNav.getCurrentRecordPosition();
					
					Memory idNameCDRecordWithHeader = null;

					if (inBeginEnd) {
						if (rtNav.gotoNext()) {
							do {
								// check if we have reached the end of the field block
								if (CDRecordType.END.getConstant() == rtNav.getCurrentRecordTypeAsShort() ||
										CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
									break;
								}
								else if (CDRecordType.IDNAME.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
									idNameCDRecordWithHeader = rtNav.getCurrentRecordDataWithHeader();
								}
							}
							while (rtNav.gotoNext());
						}
					}

					rtNav.restoreCurrentRecordPosition(fieldPos);

					FieldPropAdaptable fieldData = new FieldPropAdaptable(fieldRecordWithHeader, idNameCDRecordWithHeader);
					FieldInfo fldInfo = new FieldInfo(fieldData);
					fields.add(fldInfo);
				}
			}
			while (rtNav.gotoNext());
		}
		
		if (oldPos!=null) {
			rtNav.restoreCurrentRecordPosition(oldPos);
		}
		
		return fields;
	}
	
	/**
	 * Scans all richtext items of a note to find all attachment icons and
	 * return the attachment names hashed by richtext item name
	 * 
	 * @param note note to scan
	 * @return map with item name as key (case insensitive) and attachment names as value in order of appearance
	 */
	public static Map<String,LinkedHashSet<String>> collectAllAttachmentNamesByField(NotesNote note) {
		Map<String,LinkedHashSet<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		Set<String> allRichtextItemNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		
		note.getItems((item,loop) -> {
			if (item.getType() == NotesItem.TYPE_COMPOSITE) {
				allRichtextItemNames.add(item.getName());
			}
		});
		
		for (String currItemName : allRichtextItemNames) {
			IRichTextNavigator rtNav = note.getRichtextNavigator(currItemName);
			LinkedHashSet<String> attachmentNamesInItem = collectAttachmentNames(rtNav);
			result.put(currItemName, attachmentNamesInItem);
		}
		
		return result;
	}
	
	/**
	 * Traverses the richtext CD records and collects the filenames of all
	 * attachment icons. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param note note to scan
	 * @param itemName name of richtext item
	 * @return attachment names in order of appearance
	 */
	public static LinkedHashSet<String> collectAttachmentNames(NotesNote note, String itemName) {
		IRichTextNavigator rtNav = note.getRichtextNavigator(itemName);
		if (rtNav!=null) {
			return collectAttachmentNames(rtNav);
		}
		else {
			return new LinkedHashSet<>();
		}
	}
	
	/**
	 * Traverses the richtext CD records and collects the filenames of all
	 * attachment icons. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param rtNav richtext navigator
	 * @return attachment names in order of appearance
	 */
	public static LinkedHashSet<String> collectAttachmentNames(IRichTextNavigator rtNav) {
		LinkedHashSet<String> attNames = new LinkedHashSet<>();
		
		if (rtNav==null) {
			return attNames;
		}
		
		RichTextNavPosition oldPos = rtNav.getCurrentRecordPosition();
		
		if (rtNav.gotoFirst()) {
			do {
				if (CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
//					typedef struct {
//						   BSIG Header;    /* Signature and length of this record */
//						   WORD Version;		
//						   WORD Signature; /* Signature of record begin is for */
//						} CDBEGINRECORD;
					Memory beginDataBuf = rtNav.getCurrentRecordData();
					
					int signature = beginDataBuf.share(2).getShort(0);
					
					if (signature == NotesConstants.SIG_CD_V4HOTSPOTBEGIN) {
						RichTextNavPosition savedPos = rtNav.getCurrentRecordPosition();
						if (rtNav.gotoNext()) {
							//check what is next
							if (CDRecordType.HOTSPOTBEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
//								typedef struct {
//								   WSIG  Header; /* Signature and length of this record */	
//								   WORD  Type;
//								   DWORD Flags;
//								   WORD  DataLength;
//								   Data follows...
									/*  if HOTSPOTREC_RUNFLAG_SIGNED, WORD SigLen then SigData follows. */
//								} CDHOTSPOTBEGIN;

								Memory hotspotRecordDataBuf = rtNav.getCurrentRecordData();
								
								short type = hotspotRecordDataBuf.getShort(0);
								if (type == NotesConstants.HOTSPOTREC_TYPE_FILE) {
									String uniqueFileName = NotesStringUtils.fromLMBCS(hotspotRecordDataBuf.share(8), -1);
									attNames.add(uniqueFileName);
								}
							}
						}
						rtNav.restoreCurrentRecordPosition(savedPos);
					}
				}
			}
			while (rtNav.gotoNext());
			
		}
		
		if (oldPos!=null) {
			rtNav.restoreCurrentRecordPosition(oldPos);
		}
		
		return attNames;
	}
	
	/**
	 * Traverses the richtext CD records (of a design element's $body item)
	 * and collects all contained named subforms (those not computed via formula)
	 * with the replica id of their database. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param rtNav richtext navigator
	 * @return pairs of [replicaid, named subform], replicaid is "0000000000000000" for the local DB
	 */
	public static List<Pair<String,String>> collectNamedSubforms(IRichTextNavigator rtNav) {
		List<Pair<String,String>> subforms = new ArrayList<>();
		
		if (rtNav==null) {
			return subforms;
		}
		
		RichTextNavPosition oldPos = rtNav.getCurrentRecordPosition();
		
		if (rtNav.gotoFirst()) {
			boolean inBeginEnd = false;
			
			do {
				if (CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = true;
				}
				else if (CDRecordType.END.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = false;
				}
				else if (CDRecordType.HREF2.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					if (inBeginEnd) {
						//found record of type SIG_CD_HREF2
						Memory href2RecordWithHeader = rtNav.getCurrentRecordDataWithHeader();

						NotesCDResourceStruct cdResourceStruct = NotesCDResourceStruct.newInstance(href2RecordWithHeader);
						cdResourceStruct.read();
						
						//search for additional CD records V4HOTSPOTBEGIN / V4HOTSPOTEND

						RichTextNavPosition fieldPos = rtNav.getCurrentRecordPosition();

						boolean hotspotBeginFound = false;
						boolean hotspotEndFound = false;

						if (rtNav.gotoNext()) {
							do {
								// check if we have reached the end of the field block
								if (CDRecordType.V4HOTSPOTBEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
									Memory v4HotspotbeginRecordWithHeader = rtNav.getCurrentRecordDataWithHeader();

									NotesCdHotspotBeginStruct v4HotspotBegin = NotesCdHotspotBeginStruct.newInstance(v4HotspotbeginRecordWithHeader);
									v4HotspotBegin.read();
									
									if (v4HotspotBegin.Type == NotesConstants.HOTSPOTREC_TYPE_SUBFORM) {
										hotspotBeginFound = true;
									}
								}
								else if (CDRecordType.V4HOTSPOTEND.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
									hotspotEndFound = true;
								}
							}
							while (rtNav.gotoNext());
						}

						rtNav.restoreCurrentRecordPosition(fieldPos);

						if (hotspotBeginFound && hotspotEndFound) {
//							typedef struct {
//							   WSIG  Header;
//							   DWORD Flags;            /* one of CDRESOURCE_FLAGS_xxx */
//							   WORD  Type;             /* one of CDRESOURCE_TYPE_xxx */
//							   WORD  ResourceClass;    /* one of CDRESOURCE_CLASS_xxx */
//							   WORD  Length1;          /* meaning depends on Type */
//							   WORD  ServerHintLength; /* length of the server hint */
//							   WORD  FileHintLength;   /* length of the file hint */
//							   BYTE  Reserved[8];
							/* Variable length follows:
							 *   String of size ServerHintLength: hint as to resource's server
							 *   String of size FileHintLength: hint as to resource's file
							 *	- if CDRESOURCE_TYPE_URL : 
							 *     string of size Length1 - the URL.
							 *	- if CDRESOURCE_TYPE_NOTELINK: 
							 *     if CDRESOURCE_FLAGS_NOTELINKINLINE is NOT set in Flags:
							 *       WORD LinkID - index into $Links
							 *       string of size Length1 - the anchor name (optional)
							 *     if CDRESOURCE_FLAGS_NOTELINKINLINE is set in Flags:
							 *       NOTELINK NoteLink
							 *       string of size Length1 - the anchor name (optional)
							 *	- if CDRESOURCE_TYPE_NAMEDELEMENT :
							 *		TIMEDATE ReplicaID (zero if current db)
							 *		string of size Length1 - the name of element
							 */
//							} CDRESOURCE;
							
							if (cdResourceStruct.Type == NotesConstants.CDRESOURCE_TYPE_NAMEDELEMENT) {
								if (cdResourceStruct.ResourceClass == NotesConstants.CDRESOURCE_CLASS_SUBFORM) {
									NotesTimeDateStruct replicaId = NotesTimeDateStruct.newInstance(
											href2RecordWithHeader
											.share(cdResourceStruct.size()));
									replicaId.read();
									String replicaIdStr = NotesStringUtils.innardsToReplicaId(replicaId.Innards);
									
									String subformName = NotesStringUtils.fromLMBCS(
											href2RecordWithHeader
											.share(cdResourceStruct.size() + NotesConstants.timeDateSize), // => TIMEDATE is replica id
											cdResourceStruct.Length1);
									subforms.add(new Pair<>(replicaIdStr, subformName));
								}
							}
						}
					}
				}
			}
			while (rtNav.gotoNext());
		}
		
		if (oldPos!=null) {
			rtNav.restoreCurrentRecordPosition(oldPos);
		}
		
		return subforms;
	}
	
	/**
	 * Traverses the richtext CD records (of a design element's $body item)
	 * and collects all formulas computing the name of a subform. Restores the
	 * current {@link RichTextNavPosition} in the richtext item if not
	 * null.
	 * 
	 * @param rtNav richtext navigator
	 * @return subforms formulas
	 */
	public static List<String> collectSubformFormulas(IRichTextNavigator rtNav) {
		List<String> subforms = new ArrayList<String>();
		
		if (rtNav==null) {
			return subforms;
		}
		
		RichTextNavPosition oldPos = rtNav.getCurrentRecordPosition();
		
		if (rtNav.gotoFirst()) {
			boolean inBeginEnd = false;
			
			do {
				if (CDRecordType.BEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = true;
				}
				else if (CDRecordType.END.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					inBeginEnd = false;
				}
				else if (CDRecordType.V4HOTSPOTBEGIN.getConstant() == rtNav.getCurrentRecordTypeAsShort()) {
					if (inBeginEnd) {
//						[V4HOTSPOTBEGIN]
//						Record type: 65453 (V4HOTSPOTBEGIN)
//						Total length: 76
//						Header length: 4
//						Data length: 72
//
//						[ad ff 4c 00 0e 00 18 00]   [..L.....]
//						[00 00 40 00 40 00 00 00]   [..@.@...]
//						[32 00 05 00 01 00 78 00]   [2.....x.]
//						[01 00 00 00 0a 02 af 00]   [........]
//						[1e 00 12 00 01 00 08 00]   [........]
//						[53 75 62 66 6f 72 6d 32]   [Subform2]
//						[ae 00 0c 00 01 00 00 00]   [........]
//						[ae 00 04 00 b5 03 03 00]   [........]
//						[07 00 0c 00 05 00 09 30]   [.......0]
//						[53 30 45 00            ]   [S0E.    ]

						Memory v4HotspotbeginRecordWithHeader = rtNav.getCurrentRecordDataWithHeader();

						NotesCdHotspotBeginStruct v4HotspotBegin = NotesCdHotspotBeginStruct.newInstance(v4HotspotbeginRecordWithHeader);
						v4HotspotBegin.read();

						if (v4HotspotBegin.Type == NotesConstants.HOTSPOTREC_TYPE_SUBFORM) {
							if ((v4HotspotBegin.Flags & NotesConstants.HOTSPOTREC_RUNFLAG_FORMULA) == NotesConstants.HOTSPOTREC_RUNFLAG_FORMULA) {
								Pointer compiledFormulaPtr = v4HotspotbeginRecordWithHeader.share(12);
								String formula = FormulaDecompiler.decompileFormula(compiledFormulaPtr);
								subforms.add(formula);
							}
						}
					}
				}
			}
			while (rtNav.gotoNext());
		}
		
		if (oldPos!=null) {
			rtNav.restoreCurrentRecordPosition(oldPos);
		}
		
		return subforms;
	}
}
