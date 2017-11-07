package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * This enumerates the command arguments supported in Domino urls.  (e.g., &amp;Start=..., &amp;Count=...).
 * 
 * @author Karsten Lehmann
 */
public enum CommandArgumentId {
	START(NotesCAPI.CAI_Start),
	STARTKEY(NotesCAPI.CAI_StartKey),
	COUNT(NotesCAPI.CAI_Count),
	EXPAND(NotesCAPI.CAI_Expand),
	FULLYEXPAND(NotesCAPI.CAI_FullyExpand),
	EXPANDVIEW(NotesCAPI.CAI_ExpandView),
	COLLAPSE(NotesCAPI.CAI_Collapse),
	COLLAPSEVIEW(NotesCAPI.CAI_CollapseView),
	THREEPANEUI(NotesCAPI.CAI_3PaneUI),
	TARGETFRAME(NotesCAPI.CAI_TargetFrame),
	FIELDELEMTYPE(NotesCAPI.CAI_FieldElemType),
	FIELDELEMFORMAT(NotesCAPI.CAI_FieldElemFormat),
	SEARCHQUERY(NotesCAPI.CAI_SearchQuery),
	OLDSEARCHQUERY(NotesCAPI.CAI_OldSearchQuery),
	SEARCHMAX(NotesCAPI.CAI_SearchMax),
	SEARCHWV(NotesCAPI.CAI_SearchWV),
	SEARCHORDER(NotesCAPI.CAI_SearchOrder),
	SEARCHTHESAURUS(NotesCAPI.CAI_SearchThesarus),
	RESORTASCENDING(NotesCAPI.CAI_ResortAscending),
	RESORTDESCENDING(NotesCAPI.CAI_ResortDescending),
	PARENTUNID(NotesCAPI.CAI_ParentUNID),
	CLICK(NotesCAPI.CAI_Click),
	USERNAME(NotesCAPI.CAI_UserName),
	PASSWORD(NotesCAPI.CAI_Password),
	TO(NotesCAPI.CAI_To),
	ISMAPx(NotesCAPI.CAI_ISMAPx),
	ISMAPy(NotesCAPI.CAI_ISMAPy),
	GRID(NotesCAPI.CAI_Grid),
	DATE(NotesCAPI.CAI_Date),
	TEMPLATETYPE(NotesCAPI.CAI_TemplateType),
	TARGETUNID(NotesCAPI.CAI_TargetUNID),
	EXPANDSECTION(NotesCAPI.CAI_ExpandSection),
	LOGIN(NotesCAPI.CAI_Login),
	PICKUPCERT(NotesCAPI.CAI_PickupCert),
	PICKUPCACERT(NotesCAPI.CAI_PickupCACert),
	SUBMITCERT(NotesCAPI.CAI_SubmitCert),
	SERVERREQUEST(NotesCAPI.CAI_ServerRequest),
	SERVERPICKUP(NotesCAPI.CAI_ServerPickup),
	PICKUPID(NotesCAPI.CAI_PickupID),
	TRANSLATEFORM(NotesCAPI.CAI_TranslateForm),
	SPECIALACTION(NotesCAPI.CAI_SpecialAction),
	ALLOWGETMETHOD(NotesCAPI.CAI_AllowGetMethod),
	SEQ(NotesCAPI.CAI_Seq),
	BASETARGET(NotesCAPI.CAI_BaseTarget),
	EXPANDOUTLINE(NotesCAPI.CAI_ExpandOutline),
	STARTOUTLINE(NotesCAPI.CAI_StartOutline),
	DAYS(NotesCAPI.CAI_Days),
	TABLETAB(NotesCAPI.CAI_TableTab),
	MIME(NotesCAPI.CAI_MIME),
	RESTRICTTOCATEGORY(NotesCAPI.CAI_RestrictToCategory),
	HIGHLIGHT(NotesCAPI.CAI_Highlight),
	FRAME(NotesCAPI.CAI_Frame),
	FRAMESRC(NotesCAPI.CAI_FrameSrc),
	NAVIGATE(NotesCAPI.CAI_Navigate),
	SKIPNAVIGATE(NotesCAPI.CAI_SkipNavigate),
	SKIPCOUNT(NotesCAPI.CAI_SkipCount),
	ENDVIEW(NotesCAPI.CAI_EndView),
	TABLEROW(NotesCAPI.CAI_TableRow),
	REDIRECTTO(NotesCAPI.CAI_RedirectTo),
	SESSIONID(NotesCAPI.CAI_SessionId),
	SOURCEFOLDER(NotesCAPI.CAI_SourceFolder),
	SEARCHFUZZY(NotesCAPI.CAI_SearchFuzzy),
	HARDDELETE(NotesCAPI.CAI_HardDelete),
	SIMPLEVIEW(NotesCAPI.CAI_SimpleView),
	SEARCHENTRY(NotesCAPI.CAI_SearchEntry),
	NAME(NotesCAPI.CAI_Name),
	ID(NotesCAPI.CAI_Id),
	ROOTALIAS(NotesCAPI.CAI_RootAlias),
	SCOPE(NotesCAPI.CAI_Scope),
	DBLCLKTARGET(NotesCAPI.CAI_DblClkTarget),
	CHARSET(NotesCAPI.CAI_Charset),
	EMPTYTRASH(NotesCAPI.CAI_EmptyTrash),
	ENDKEY(NotesCAPI.CAI_EndKey),
	PREFORMAT(NotesCAPI.CAI_PreFormat),
	IMGINDEX(NotesCAPI.CAI_ImgIndex),
	AUTOFRAMED(NotesCAPI.CAI_AutoFramed),
	OUTPUTFORMAT(NotesCAPI.CAI_OutputFormat),
	INHERITPARENT(NotesCAPI.CAI_InheritParent);

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