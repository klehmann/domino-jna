package com.mindoo.domino.jna;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.Handle;
import com.mindoo.domino.jna.utils.PlatformUtils;

/**
 * Container for an in-memory user ID fetched from the ID vault
 * 
 * @author Karsten Lehmann
 */
public class NotesUserId  {
	private long m_memHandle64;
	private int m_memHandle32;
	
	public NotesUserId(IAdaptable adaptable) {
		Handle hdl = adaptable.getAdapter(Handle.class);
		if (hdl!=null) {
			if (PlatformUtils.is64Bit()) {
				m_memHandle64 = hdl.getHandle64();
			}
			else {
				m_memHandle32 = hdl.getHandle32();
			}
			return;
		}
		throw new NotesError(0, "Unsupported adaptable parameter");
	}
	
	public String getUsername() {
		//TODO find a better way to read the username from the hKFC
		NotesDatabase db = new NotesDatabase("", "names.nsf", "");
		NotesNote note = db.createNote();
		note.sign(this, false);
		String signer = note.getSigner();
		note.recycle();
		db.recycle();
		
		return signer;
	}
	
	/**
	 * Returns the handle to the in-memory ID for 32 bit
	 * 
	 * @return handle
	 */
	public int getHandle32() {
		return m_memHandle32;
	}
	
	/**
	 * Returns the handle to the in-memory ID for 64 bit
	 * 
	 * @return handle
	 */
	public long getHandle64() {
		return m_memHandle64;
	}
}
