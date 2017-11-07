package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * This enumerates the commands supported in Domino urls
 * 
 * @author Karsten Lehmann
 */
public enum CommandId {
	UNKNOWN(NotesCAPI.kUnknownCmdId),
	OPENSERVER(NotesCAPI.kOpenServerCmdId),
	OPENDATABASE(NotesCAPI.kOpenDatabaseCmdId),
	OPENVIEW(NotesCAPI.kOpenViewCmdId),
	OPENDOCUMENT(NotesCAPI.kOpenDocumentCmdId),
	OPENELEMENT(NotesCAPI.kOpenElementCmdId),
	OPENFORM(NotesCAPI.kOpenFormCmdId),
	OPENAGENT(NotesCAPI.kOpenAgentCmdId),
	OPENNAVIGATOR(NotesCAPI.kOpenNavigatorCmdId),
	OPENICON(NotesCAPI.kOpenIconCmdId),
	OPENABOUT(NotesCAPI.kOpenAboutCmdId),
	OPENHELP(NotesCAPI.kOpenHelpCmdId),
	CREATEDOCUMENT(NotesCAPI.kCreateDocumentCmdId),
	SAVEDOCUMENT(NotesCAPI.kSaveDocumentCmdId),
	EDITDOCUMENT(NotesCAPI.kEditDocumentCmdId),
	DELETEDOCUMENT(NotesCAPI.kDeleteDocumentCmdId),
	SEARCHVIEW(NotesCAPI.kSearchViewCmdId),
	SEARCHSITE(NotesCAPI.kSearchSiteCmdId),
	NAVIGATE(NotesCAPI.kNavigateCmdId),
	READFORM(NotesCAPI.kReadFormCmdId),
	REQUESTCERT(NotesCAPI.kRequestCertCmdId),
	READDESIGN(NotesCAPI.kReadDesignCmdId),
	READVIEWENTRIES(NotesCAPI.kReadViewEntriesCmdId),
	READENTRIES(NotesCAPI.kReadEntriesCmdId),
	OPENPAGE(NotesCAPI.kOpenPageCmdId),
	OPENFRAMESET(NotesCAPI.kOpenFrameSetCmdId),
	/** OpenField command for Java applet(s) and HAPI */
	OPENFIELD(NotesCAPI.kOpenFieldCmdId),
	SEARCHDOMAIN(NotesCAPI.kSearchDomainCmdId),
	DELETEDOCUMENTS(NotesCAPI.kDeleteDocumentsCmdId),
	LOGINUSER(NotesCAPI.kLoginUserCmdId),
	LOGOUTUSER(NotesCAPI.kLogoutUserCmdId),
	OPENIMAGERESOURCE(NotesCAPI.kOpenImageResourceCmdId),
	OPENIMAGE(NotesCAPI.kOpenImageCmdId),
	COPYTOFOLDER(NotesCAPI.kCopyToFolderCmdId),
	MOVETOFOLDER(NotesCAPI.kMoveToFolderCmdId),
	REMOVEFROMFOLDER(NotesCAPI.kRemoveFromFolderCmdId),
	UNDELETEDOCUMENTS(NotesCAPI.kUndeleteDocumentsCmdId),
	REDIRECT(NotesCAPI.kRedirectCmdId),
	GETORBCOOKIE(NotesCAPI.kGetOrbCookieCmdId),
	OPENCSSRESOURCE(NotesCAPI.kOpenCssResourceCmdId),
	OPENFILERESOURCE(NotesCAPI.kOpenFileResourceCmdId),
	OPENJAVASCRIPTLIB(NotesCAPI.kOpenJavascriptLibCmdId),
	UNIMPLEMENTED(NotesCAPI.kUnImplemented_01),
	CHANGEPASSWORD(NotesCAPI.kChangePasswordCmdId),
	OPENPREFERENCES(NotesCAPI.kOpenPreferencesCmdId),
	OPENWEBSERVICE(NotesCAPI.kOpenWebServiceCmdId),
	WSDL(NotesCAPI.kWsdlCmdId),
	GETIMAGE(NotesCAPI.kGetImageCmdId);

	int m_val;

	private static Map<Integer, CommandId> idsByIntValue = new HashMap<Integer, CommandId>();

	static {
		for (CommandId currId : CommandId.values()) {
			idsByIntValue.put(currId.m_val, currId);
		}
	}

	CommandId(int type) {
		m_val = type;
	}

	public int getValue() {
		return m_val;
	}

	public static CommandId getCommandId(int intVal) {
		CommandId type = idsByIntValue.get(intVal);
		if (type==null)
			throw new IllegalArgumentException("Unknown int value: "+intVal);
		return type;
	}
};