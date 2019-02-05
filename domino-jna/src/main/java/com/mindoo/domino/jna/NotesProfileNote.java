package com.mindoo.domino.jna;

import java.util.EnumSet;

import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;

/**
 * Subclass of {@link NotesNote} to access data of profile notes.
 * Use {@link NotesDatabase#getProfileNote(String, String)} to load a profile.
 * 
 * @author Karsten Lehmann
 */
public class NotesProfileNote extends NotesNote {
	private String m_profileName;
	private String m_userName;
	
	NotesProfileNote(IAdaptable adaptable) {
		super(adaptable);
	}
	
	NotesProfileNote(NotesDatabase parentDb, int hNote) {
		super(parentDb, hNote);
	}
	
	NotesProfileNote(NotesDatabase parentDb, long hNote) {
		super(parentDb, hNote);
	}
	
	public String getProfileName() {
		if (m_profileName==null) {
			parseProfileAndUserName();
		}
		return m_profileName;
	}
	
	public void parseProfileAndUserName() {
		String name = getItemValueString("$name"); //$profile_015calendarprofile_<username>
		if (!name.startsWith("$profile_")) {
			m_profileName="";
			m_userName="";
			return;
		}
		String remainder = name.substring(9); //"$profile_".length()
		if (remainder.length()<3) {
			m_profileName="";
			m_userName="";
			return;
		}
		
		String profileNameLengthStr = remainder.substring(0, 3);
		int profileNameLength = Integer.parseInt(profileNameLengthStr);
		
		remainder = remainder.substring(3);
		m_profileName = remainder.substring(0, profileNameLength);
		
		remainder = remainder.substring(profileNameLength+1);
		
		m_userName = remainder;
	}
	
	public String getUserName() {
		if (m_userName==null) {
			parseProfileAndUserName();
		}
		return m_userName;
	}
	
	@Override
	public void update(EnumSet<UpdateNote> updateFlags) {
		update();
	}
	
	@Override
	public void update() {
		checkHandle();
		
		Memory profileNameMem = NotesStringUtils.toLMBCS(m_profileName, false);
		Memory userNameMem = StringUtil.isEmpty(m_userName) ? null : NotesStringUtils.toLMBCS(m_userName, false);

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFProfileUpdate(getHandle64(),
					profileNameMem, (short) (profileNameMem.size() & 0xffff), userNameMem,
					(short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)));
			NotesErrorUtils.checkResult(result);
			
		}
		else {
			short result = NotesNativeAPI32.get().NSFProfileUpdate(getHandle32(),
					profileNameMem, (short) (profileNameMem.size() & 0xffff), userNameMem,
					(short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)));
			NotesErrorUtils.checkResult(result);
			
		}
	}

}
