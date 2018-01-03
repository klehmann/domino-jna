package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * This enumerates the command arguments supported in Domino urls.  (e.g., &amp;Start=..., &amp;Count=...).
 * 
 * @author Karsten Lehmann
 */
public enum CommandArgumentId {
	START(NotesConstants.CAI_Start),
	STARTKEY(NotesConstants.CAI_StartKey),
	COUNT(NotesConstants.CAI_Count),
	EXPAND(NotesConstants.CAI_Expand),
	FULLYEXPAND(NotesConstants.CAI_FullyExpand),
	EXPANDVIEW(NotesConstants.CAI_ExpandView),
	COLLAPSE(NotesConstants.CAI_Collapse),
	COLLAPSEVIEW(NotesConstants.CAI_CollapseView),
	THREEPANEUI(NotesConstants.CAI_3PaneUI),
	TARGETFRAME(NotesConstants.CAI_TargetFrame),
	FIELDELEMTYPE(NotesConstants.CAI_FieldElemType),
	FIELDELEMFORMAT(NotesConstants.CAI_FieldElemFormat),
	SEARCHQUERY(NotesConstants.CAI_SearchQuery),
	OLDSEARCHQUERY(NotesConstants.CAI_OldSearchQuery),
	SEARCHMAX(NotesConstants.CAI_SearchMax),
	SEARCHWV(NotesConstants.CAI_SearchWV),
	SEARCHORDER(NotesConstants.CAI_SearchOrder),
	SEARCHTHESAURUS(NotesConstants.CAI_SearchThesarus),
	RESORTASCENDING(NotesConstants.CAI_ResortAscending),
	RESORTDESCENDING(NotesConstants.CAI_ResortDescending),
	PARENTUNID(NotesConstants.CAI_ParentUNID),
	CLICK(NotesConstants.CAI_Click),
	USERNAME(NotesConstants.CAI_UserName),
	PASSWORD(NotesConstants.CAI_Password),
	TO(NotesConstants.CAI_To),
	ISMAPx(NotesConstants.CAI_ISMAPx),
	ISMAPy(NotesConstants.CAI_ISMAPy),
	GRID(NotesConstants.CAI_Grid),
	DATE(NotesConstants.CAI_Date),
	TEMPLATETYPE(NotesConstants.CAI_TemplateType),
	TARGETUNID(NotesConstants.CAI_TargetUNID),
	EXPANDSECTION(NotesConstants.CAI_ExpandSection),
	LOGIN(NotesConstants.CAI_Login),
	PICKUPCERT(NotesConstants.CAI_PickupCert),
	PICKUPCACERT(NotesConstants.CAI_PickupCACert),
	SUBMITCERT(NotesConstants.CAI_SubmitCert),
	SERVERREQUEST(NotesConstants.CAI_ServerRequest),
	SERVERPICKUP(NotesConstants.CAI_ServerPickup),
	PICKUPID(NotesConstants.CAI_PickupID),
	TRANSLATEFORM(NotesConstants.CAI_TranslateForm),
	SPECIALACTION(NotesConstants.CAI_SpecialAction),
	ALLOWGETMETHOD(NotesConstants.CAI_AllowGetMethod),
	SEQ(NotesConstants.CAI_Seq),
	BASETARGET(NotesConstants.CAI_BaseTarget),
	EXPANDOUTLINE(NotesConstants.CAI_ExpandOutline),
	STARTOUTLINE(NotesConstants.CAI_StartOutline),
	DAYS(NotesConstants.CAI_Days),
	TABLETAB(NotesConstants.CAI_TableTab),
	MIME(NotesConstants.CAI_MIME),
	RESTRICTTOCATEGORY(NotesConstants.CAI_RestrictToCategory),
	HIGHLIGHT(NotesConstants.CAI_Highlight),
	FRAME(NotesConstants.CAI_Frame),
	FRAMESRC(NotesConstants.CAI_FrameSrc),
	NAVIGATE(NotesConstants.CAI_Navigate),
	SKIPNAVIGATE(NotesConstants.CAI_SkipNavigate),
	SKIPCOUNT(NotesConstants.CAI_SkipCount),
	ENDVIEW(NotesConstants.CAI_EndView),
	TABLEROW(NotesConstants.CAI_TableRow),
	REDIRECTTO(NotesConstants.CAI_RedirectTo),
	SESSIONID(NotesConstants.CAI_SessionId),
	SOURCEFOLDER(NotesConstants.CAI_SourceFolder),
	SEARCHFUZZY(NotesConstants.CAI_SearchFuzzy),
	HARDDELETE(NotesConstants.CAI_HardDelete),
	SIMPLEVIEW(NotesConstants.CAI_SimpleView),
	SEARCHENTRY(NotesConstants.CAI_SearchEntry),
	NAME(NotesConstants.CAI_Name),
	ID(NotesConstants.CAI_Id),
	ROOTALIAS(NotesConstants.CAI_RootAlias),
	SCOPE(NotesConstants.CAI_Scope),
	DBLCLKTARGET(NotesConstants.CAI_DblClkTarget),
	CHARSET(NotesConstants.CAI_Charset),
	EMPTYTRASH(NotesConstants.CAI_EmptyTrash),
	ENDKEY(NotesConstants.CAI_EndKey),
	PREFORMAT(NotesConstants.CAI_PreFormat),
	IMGINDEX(NotesConstants.CAI_ImgIndex),
	AUTOFRAMED(NotesConstants.CAI_AutoFramed),
	OUTPUTFORMAT(NotesConstants.CAI_OutputFormat),
	INHERITPARENT(NotesConstants.CAI_InheritParent);

	int m_val;

	private static Map<Integer, CommandArgumentId> idsByIntValue = new HashMap<Integer, CommandArgumentId>();

	static {
		for (CommandArgumentId currId : CommandArgumentId.values()) {
			idsByIntValue.put(currId.m_val, currId);
		}
	}

	CommandArgumentId(int type) {
		m_val = type;
	}

	public int getValue() {
		return m_val;
	}

	public static CommandArgumentId getId(int intVal) {
		CommandArgumentId id = idsByIntValue.get(intVal);
		if (id==null)
			throw new IllegalArgumentException("Unknown int value: "+intVal);
		return id;
	}
};