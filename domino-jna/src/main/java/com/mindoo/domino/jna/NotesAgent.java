package com.mindoo.domino.jna;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import lotus.domino.Document;

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
	
	NotesAgent(NotesDatabase parentDb, int hNoteId, int hAgent) {
		m_parentDb = parentDb;
		m_hNoteId = hNoteId;
		m_hAgentB32 = hAgent;
	}

	NotesAgent(NotesDatabase parentDb, int hNoteId, long hAgent) {
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

		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().AgentClose(m_hAgentB64);
			NotesGC.__objectBeeingBeRecycled(NotesAgent.class, this);
			m_hAgentB64=0;
		}
		else {
			NotesNativeAPI32.get().AgentClose(m_hAgentB32);
			NotesGC.__objectBeeingBeRecycled(NotesAgent.class, this);
			m_hAgentB32=0;
		}
	}

	@Override
	public boolean isRecycled() {
		if (PlatformUtils.is64Bit()) {
			return m_hAgentB64==0;
		}
		else {
			return m_hAgentB32==0;
		}
	}

	@Override
	public boolean isNoRecycle() {
		return false;
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
		if (PlatformUtils.is64Bit()) {
			if (m_hAgentB64==0)
				throw new NotesError(0, "Agent already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesAgent.class, m_hAgentB64);
		}
		else {
			if (m_hAgentB32==0)
				throw new NotesError(0, "Agent already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesAgent.class, m_hAgentB32);
		}
	}

	/**
	 * Checks whether an agent is enabled
	 * 
	 * @return true if enabled
	 */
	public boolean isEnabled() {
		checkHandle();

		boolean enabled;
		if (PlatformUtils.is64Bit()) {
			enabled = NotesNativeAPI64.get().AgentIsEnabled(m_hAgentB64);
		}
		else {
			enabled = NotesNativeAPI32.get().AgentIsEnabled(m_hAgentB32);
		}

		return enabled;
	}
	
	/**
	 * Checks whether an agent has the flag "run as web user"
	 * 
	 * @return true if flag is set
	 */
	public boolean isRunAsWebUser() {
		checkHandle();

		boolean isRunAsWebUser;
		if (PlatformUtils.is64Bit()) {
			isRunAsWebUser = NotesNativeAPI64.get().IsRunAsWebUser(m_hAgentB64);
		}
		else {
			isRunAsWebUser = NotesNativeAPI32.get().IsRunAsWebUser(m_hAgentB32);
		}

		return isRunAsWebUser;
	}
	
	/**
	 * Executes the agent on a context document
	 * 
	 * @param runCtx run context
	 * @throws IOException in case of i/o errors writing to output writer
	 */
	public void run(NotesAgentRunContext runCtx) throws IOException {
		checkHandle();
		NotesNote note = runCtx.getDocumentContextAsNote();
		if (note!=null) {
			note.checkHandle();
		}
		
		Document doc = runCtx.getDocumentContextAsLegacyDoc();
		long cHandle=0;
		if (doc!=null) {
			boolean isLocalDoc = false;
			try {
				Class<?> ldldClass = Class.forName("lotus.domino.local.Document");
				ldldClass.cast(doc);
				isLocalDoc = true;
			} catch (Throwable e) {
			}

			if (!isLocalDoc) {
				throw new IllegalArgumentException("Only lotus.domino.local.Document is supported");
			}
			
			cHandle = LegacyAPIUtils.getDocHandle(doc);
		}
		
		int ctxFlags = 0;
		int runFlags = 0;
		
		boolean reopenDbAsSigner = runCtx.isReopenDbAsSigner();
		if (reopenDbAsSigner) {
			runFlags = NotesConstants.AGENT_REOPEN_DB;
		}
		boolean checkSecurity = runCtx.isCheckSecurity();
		if (checkSecurity) {
			ctxFlags = NotesConstants.AGENT_SECURITY_ON;
		}

		Writer stdOut = runCtx.getOutputWriter();
		int timeoutSeconds = runCtx.getTimeoutSeconds();
		int paramDocId = runCtx.getParamDocId();
		
		String effectiveUserNameAsString = runCtx.getUsername();
		List<String> effectiveUserNameAsStringList = runCtx.getUsernameAsStringList();
		NotesNamesList effectiveUserNameAsNamesList = runCtx.getUsernameAsNamesList();
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethContext = new LongByReference();
			short result = NotesNativeAPI64.get().AgentCreateRunContextExt(m_hAgentB64, null, 0, ctxFlags, rethContext);
			NotesErrorUtils.checkResult(result);

			NotesNamesList namesListToFree = null;
			
			try {
				if (stdOut!=null) {
					//redirect stdout to in memory buffer
					short redirType = NotesConstants.AGENT_REDIR_MEMORY;
					result = NotesNativeAPI64.get().AgentRedirectStdout(rethContext.getValue(), redirType);
					NotesErrorUtils.checkResult(result);
				}

				if (timeoutSeconds!=0) {
					NotesNativeAPI64.get().AgentSetTimeExecutionLimit(rethContext.getValue(), timeoutSeconds);
				}

				if (note!=null) {
					NotesNativeAPI64.get().AgentSetDocumentContext(rethContext.getValue(), note.getHandle64());
				}
				else if (cHandle!=0) {
					NotesNativeAPI64.get().AgentSetDocumentContext(rethContext.getValue(), cHandle);
				}
				
				if (paramDocId!=0) {
					NotesNativeAPI64.get().SetParamNoteID(rethContext.getValue(), paramDocId);
				}
				
				if (effectiveUserNameAsNamesList!=null) {
					result = NotesNativeAPI64.get().AgentSetUserName(rethContext.getValue(), effectiveUserNameAsNamesList.getHandle64());
					NotesErrorUtils.checkResult(result);
				}
				else if (effectiveUserNameAsStringList!=null) {
					namesListToFree = NotesNamingUtils.writeNewNamesList(effectiveUserNameAsStringList);
					result = NotesNativeAPI64.get().AgentSetUserName(rethContext.getValue(), namesListToFree.getHandle64());
					NotesErrorUtils.checkResult(result);
				}
				else if (effectiveUserNameAsString!=null) {
					namesListToFree = NotesNamingUtils.buildNamesList(effectiveUserNameAsString);
					result = NotesNativeAPI64.get().AgentSetUserName(rethContext.getValue(), namesListToFree.getHandle64());
					NotesErrorUtils.checkResult(result);
				}
				
				result = NotesNativeAPI64.get().AgentRun(m_hAgentB64, rethContext.getValue(), 0, runFlags);
				NotesErrorUtils.checkResult(result);
				
				if (stdOut!=null) {
					LongByReference retBufHandle = new LongByReference();
					IntByReference retSize = new IntByReference();
					
					NotesNativeAPI64.get().AgentQueryStdoutBuffer(rethContext.getValue(), retBufHandle, retSize);
					int iRetSize = retSize.getValue();
					if (iRetSize!=0) {
						Pointer bufPtr = Mem64.OSLockObject(retBufHandle.getValue());
						try {
							//decode std out buffer content
							String bufContentUnicode = NotesStringUtils.fromLMBCS(bufPtr, iRetSize);
							stdOut.write(bufContentUnicode);
						}
						finally {
							Mem64.OSUnlockObject(retBufHandle.getValue());
						}
					}
				}
			}
			finally {
				NotesNativeAPI64.get().AgentDestroyRunContext(rethContext.getValue());
				
				if (namesListToFree!=null) {
					namesListToFree.free();
				}
			}
		}
		else {
			IntByReference rethContext = new IntByReference();
			short result = NotesNativeAPI32.get().AgentCreateRunContextExt(m_hAgentB32, null, 0, ctxFlags, rethContext);
			NotesErrorUtils.checkResult(result);

			NotesNamesList namesListToFree = null;

			try {
				if (stdOut!=null) {
					//redirect stdout to in memory buffer
					short redirType = NotesConstants.AGENT_REDIR_MEMORY;
					result = NotesNativeAPI32.get().AgentRedirectStdout(rethContext.getValue(), redirType);
					NotesErrorUtils.checkResult(result);
				}

				if (timeoutSeconds!=0) {
					NotesNativeAPI32.get().AgentSetTimeExecutionLimit(rethContext.getValue(), timeoutSeconds);
				}

				if (note!=null) {
					NotesNativeAPI32.get().AgentSetDocumentContext(rethContext.getValue(), note.getHandle32());
				}
				else if (cHandle!=0) {
					NotesNativeAPI32.get().AgentSetDocumentContext(rethContext.getValue(), (int) cHandle);
				}
				
				if (paramDocId!=0) {
					NotesNativeAPI32.get().SetParamNoteID(rethContext.getValue(), paramDocId);
				}
				
				if (effectiveUserNameAsNamesList!=null) {
					result = NotesNativeAPI32.get().AgentSetUserName(rethContext.getValue(), effectiveUserNameAsNamesList.getHandle32());
					NotesErrorUtils.checkResult(result);
				}
				else if (effectiveUserNameAsStringList!=null) {
					namesListToFree = NotesNamingUtils.writeNewNamesList(effectiveUserNameAsStringList);
					result = NotesNativeAPI32.get().AgentSetUserName(rethContext.getValue(), namesListToFree.getHandle32());
					NotesErrorUtils.checkResult(result);
				}
				else if (effectiveUserNameAsString!=null) {
					namesListToFree = NotesNamingUtils.buildNamesList(effectiveUserNameAsString);
					result = NotesNativeAPI32.get().AgentSetUserName(rethContext.getValue(), namesListToFree.getHandle32());
					NotesErrorUtils.checkResult(result);
				}

				result = NotesNativeAPI32.get().AgentRun(m_hAgentB32, rethContext.getValue(), 0, runFlags);
				NotesErrorUtils.checkResult(result);
				
				if (stdOut!=null) {
					IntByReference retBufHandle = new IntByReference();
					IntByReference retSize = new IntByReference();
					
					NotesNativeAPI32.get().AgentQueryStdoutBuffer(rethContext.getValue(), retBufHandle, retSize);
					int iRetSize = retSize.getValue();
					if (iRetSize!=0) {
						Pointer bufPtr = Mem32.OSLockObject(retBufHandle.getValue());
						try {
							//decode std out buffer content
							String bufContentUnicode = NotesStringUtils.fromLMBCS(bufPtr, iRetSize);
							stdOut.write(bufContentUnicode);
						}
						finally {
							Mem32.OSUnlockObject(retBufHandle.getValue());
						}
					}
				}
			}
			finally {
				NotesNativeAPI32.get().AgentDestroyRunContext(rethContext.getValue());
				
				if (namesListToFree!=null) {
					namesListToFree.free();
				}
			}
		}
	}

	/**
	 * Runs the agent on the server
	 * 
	 * @param suppressPrintToConsole true to not write "Print" statements in the agent code to the server console
	 */
	public void runOnServer(boolean suppressPrintToConsole) {
		runOnServer(0, suppressPrintToConsole);
	}
	
	/**
	 * Runs the agent on the server
	 * 
	 * @param noteIdParamDoc note id of parameter docunent
	 * @param suppressPrintToConsole true to not write "Print" statements in the agent code to the server console
	 */
	public void runOnServer(int noteIdParamDoc, boolean suppressPrintToConsole) {
		checkHandle();
		
		boolean bForeignServer = false;
				
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().ClientRunServerAgent(m_parentDb.getHandle64(),
					m_hNoteId, noteIdParamDoc, bForeignServer ? 1 : 0, suppressPrintToConsole ? 1 : 0);
		}
		else {
			result = NotesNativeAPI32.get().ClientRunServerAgent(m_parentDb.getHandle32(),
					m_hNoteId, noteIdParamDoc, bForeignServer ? 1 : 0, suppressPrintToConsole ? 1 : 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesAgent [recycled]";
		}
		else {
			return "NotesAgent [handle="+(PlatformUtils.is64Bit() ? m_hAgentB64 : m_hAgentB32)+", name="+getTitle()+"]";
		}
	}

}