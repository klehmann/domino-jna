package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags of a Notes ID
 * 
 * @author Karsten Lehmann
 */
public enum IDFlag {
	
	/** File is password protected */
	PASSWORD(NotesConstants.fIDFH_Password),
	
	/** File password is required. */
	PASSWORD_REQUIRED (NotesConstants.fIDFH_Password_Required),
	
	/** Password may be shared by all processes */
	PASSWORD_SHAREABLE(NotesConstants.fIDFH_PWShareable),
	
	/** ID file has an extra that descibes special password features (eg, 128 bit key) */
	PASSWORD_EXTRA(NotesConstants.fIDFH_PWExtra),
	
	/** Must prompt user before automatically accepting a name change */ 
	CHANGE_NAME_PROMPT(NotesConstants.fIDFH_ChangeNamePrompt),
	
	/** For mailed in requests to certifier usually using a "safe-copy".<br>
	 * This flags says that the requestor does not need a response via Mail -- usually because the response
	 * will be detected during authentication with a server whose Address Book has been
	 * updated with a new certificate for the requestor. */ 
	DONT_REPLY_VIA_MAIL(0x80000000 | NotesConstants.f1IDFH_DontReplyViaMail),
	
	/** Admin has locked down the value of this field. See fIDFH_PWShareable */ 
	PASSWORD_SHAREABLE_LOCKDOWN(0x8000000 | NotesConstants.f1IDFH_PWShareableLockdown);

	private int m_val;
	
	IDFlag(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
}
