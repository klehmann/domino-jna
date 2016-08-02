package com.mindoo.domino.jna;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;

import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Wrapper class for a Notes Agent
 * 
 * @author Karsten Lehmann
 */
public class NotesAgent implements IRecyclableNotesObject {
	private NotesDatabase m_parentDb;
	private String m_title;
	private int m_hNoteId;
	private int m_hAgentB32;
	private long m_hAgentB64;
	private NotesNote m_agentNote;
	
	public NotesAgent(NotesDatabase parentDb, int hNoteId, int hAgent) {
		m_parentDb = parentDb;
		m_hNoteId = hNoteId;
		m_hAgentB32 = hAgent;
	}

	public NotesAgent(NotesDatabase parentDb, int hNoteId, long hAgent) {
		m_parentDb = parentDb;
		m_hNoteId = hNoteId;
		m_hAgentB64 = hAgent;
	}

	public NotesDatabase getParent() {
		return m_parentDb;
	}
	
	/**
	 * Opens the agent note and stores it in a variable for reuse
	 * 
	 * @return agent note
	 */
	private NotesNote getAgentNote() {
		if (m_agentNote==null || m_agentNote.isRecycled()) {
			m_agentNote = m_parentDb.openNoteById(m_hNoteId, EnumSet.noneOf(OpenNote.class));
		}
		return m_agentNote;
	}
	
	/**
	 * Returns the agent title
	 * 
	 * @return title
	 */
	public String getTitle() {
		if (m_title==null) {
			m_title = getAgentNote().getItemValueAsText("$Title", '\n');
		}
		return m_title;
	}

	/**
	 * Returns the agent comment content
	 * 
	 * @return comment
	 */
	public String getComment() {
		if (m_title==null) {
			m_title = getAgentNote().getItemValueAsText("$Comment", '\n');
		}
		return m_title;
	}

	@Override
	public void recycle() {
		if (isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_AgentClose(m_hAgentB64);
			NotesGC.__objectRecycled(this);
			m_hAgentB64=0;
		}
		else {
			notesAPI.b32_AgentClose(m_hAgentB32);
			NotesGC.__objectRecycled(this);
			m_hAgentB32=0;
		}
	}

	@Override
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_hAgentB64==0;
		}
		else {
			return m_hAgentB32==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hAgentB32;
	}

	@Override
	public long getHandle64() {
		return m_hAgentB64;
	}

	/**
	 * Checks if the agent is already recycled
	 */
	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_hAgentB64==0)
				throw new NotesError(0, "Agent already recycled");
			NotesGC.__b64_checkValidHandle(getClass(), m_hAgentB64);
		}
		else {
			if (m_hAgentB32==0)
				throw new NotesError(0, "Agent already recycled");
			NotesGC.__b32_checkValidHandle(getClass(), m_hAgentB32);
		}
	}

	/**
	 * Checks whether an agent is enabled
	 * 
	 * @return true if enabled
	 */
	public boolean isEnabled() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		boolean enabled;
		if (NotesJNAContext.is64Bit()) {
			enabled = notesAPI.b64_AgentIsEnabled(m_hAgentB64);
		}
		else {
			enabled = notesAPI.b32_AgentIsEnabled(m_hAgentB32);
		}

		return enabled;
	}

	/**
	 * Executes the agent
	 * 
	 * @param checkSecurity true to do security checks like restricted/unrestricted operations, can create databases, cis agent targeted to this machine, is user allowed to access this machine, can user run personal agents
	 * @param runAsSigner true to first reopen the database as the agent signer, false to use the current database instance
	 * @param stdOut optional writer to redirect the standard output content (use PRINT statements in the agent)
	 * @param timeoutSeconds optional timeout for the agent execution or 0 for no timeout
	 * @throws IOException
	 */
	public void run(boolean checkSecurity, boolean runAsSigner, Writer stdOut,
			int timeoutSeconds) throws IOException {
		run(checkSecurity, runAsSigner, stdOut, timeoutSeconds, (NotesNote) null);
	}
	
	/**
	 * Executes the agent on a context document
	 * 
	 * @param checkSecurity true to do security checks like restricted/unrestricted operations, can create databases, cis agent targeted to this machine, is user allowed to access this machine, can user run personal agents
	 * @param runAsSigner true to first reopen the database as the agent signer, false to use the current database instance
	 * @param stdOut optional writer to redirect the standard output content (use PRINT statements in the agent)
	 * @param timeoutSeconds optional timeout for the agent execution or 0 for no timeout
	 * @param note, either just in-memory or stored in the database
	 * @throws IOException
	 */
	public void run(boolean checkSecurity, boolean runAsSigner, Writer stdOut,
			int timeoutSeconds, NotesNote note) throws IOException {
		
		checkHandle();
		if (note!=null) {
			note.checkHandle();
		}
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		int ctxFlags = 0;
		int runFlags = 0;
		
		if (runAsSigner) {
			runFlags = NotesCAPI.AGENT_REOPEN_DB;
		}
		if (checkSecurity) {
			ctxFlags = NotesCAPI.AGENT_SECURITY_ON;
		}

		if (NotesJNAContext.is64Bit()) {
			LongByReference rethContext = new LongByReference();
			short result = notesAPI.b64_AgentCreateRunContext(m_hAgentB64, null, ctxFlags, rethContext);
			NotesErrorUtils.checkResult(result);

			try {
				if (stdOut!=null) {
					//redirect stdout to in memory buffer
					short redirType = NotesCAPI.AGENT_REDIR_MEMORY;
					result = notesAPI.b64_AgentRedirectStdout(rethContext.getValue(), redirType);
					NotesErrorUtils.checkResult(result);
				}

				if (timeoutSeconds!=0) {
					notesAPI.b64_AgentSetTimeExecutionLimit(rethContext.getValue(), timeoutSeconds);
				}

				if (note!=null) {
					notesAPI.b64_AgentSetDocumentContext(rethContext.getValue(), note.getHandle64());
				}
				
				result = notesAPI.b64_AgentRun(m_hAgentB64, rethContext.getValue(), 0, runFlags);
				NotesErrorUtils.checkResult(result);
				
				if (stdOut!=null) {
					LongByReference retBufHandle = new LongByReference();
					IntByReference retSize = new IntByReference();
					
					notesAPI.b64_AgentQueryStdoutBuffer(rethContext.getValue(), retBufHandle, retSize);
					int iRetSize = retSize.getValue();
					if (iRetSize!=0) {
						Pointer bufPtr = notesAPI.b64_OSLockObject(retBufHandle.getValue());
						try {
							//decode std out buffer content
							String bufContentUnicode = NotesStringUtils.fromLMBCS(bufPtr, iRetSize);
							stdOut.write(bufContentUnicode);
						}
						finally {
							notesAPI.b64_OSUnlockObject(retBufHandle.getValue());
						}
					}
				}
			}
			finally {
				notesAPI.b64_AgentDestroyRunContext(rethContext.getValue());
			}
		}
		else {
			IntByReference rethContext = new IntByReference();
			short result = notesAPI.b32_AgentCreateRunContext(m_hAgentB32, null, ctxFlags, rethContext);
			NotesErrorUtils.checkResult(result);

			try {
				if (stdOut!=null) {
					//redirect stdout to in memory buffer
					short redirType = NotesCAPI.AGENT_REDIR_MEMORY;
					result = notesAPI.b32_AgentRedirectStdout(rethContext.getValue(), redirType);
					NotesErrorUtils.checkResult(result);
				}

				if (timeoutSeconds!=0) {
					notesAPI.b32_AgentSetTimeExecutionLimit(rethContext.getValue(), timeoutSeconds);
				}

				if (note!=null) {
					notesAPI.b32_AgentSetDocumentContext(rethContext.getValue(), note.getHandle32());
				}

				result = notesAPI.b32_AgentRun(m_hAgentB32, rethContext.getValue(), 0, runFlags);
				NotesErrorUtils.checkResult(result);
				
				if (stdOut!=null) {
					IntByReference retBufHandle = new IntByReference();
					IntByReference retSize = new IntByReference();
					
					notesAPI.b32_AgentQueryStdoutBuffer(rethContext.getValue(), retBufHandle, retSize);
					int iRetSize = retSize.getValue();
					if (iRetSize!=0) {
						Pointer bufPtr = notesAPI.b32_OSLockObject(retBufHandle.getValue());
						try {
							//decode std out buffer content
							String bufContentUnicode = NotesStringUtils.fromLMBCS(bufPtr, iRetSize);
							stdOut.write(bufContentUnicode);
						}
						finally {
							notesAPI.b32_OSUnlockObject(retBufHandle.getValue());
						}
					}
				}
			}
			finally {
				notesAPI.b32_AgentDestroyRunContext(rethContext.getValue());
			}
		}
	}
}