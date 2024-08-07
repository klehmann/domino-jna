package com.mindoo.domino.jna;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.RecycleHierarchy;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;
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
	private String m_comment;
	private int m_hNoteId;
	private int m_hAgentB32;
	private long m_hAgentB64;
	private NotesNote m_agentNote;
	
	NotesAgent(NotesDatabase parentDb, int hNoteId, int hAgent) {
		m_parentDb = parentDb;
		m_hNoteId = hNoteId;
		m_hAgentB32 = hAgent;
		
		RecycleHierarchy.addChild(m_parentDb, this);
	}

	NotesAgent(NotesDatabase parentDb, int hNoteId, long hAgent) {
		m_parentDb = parentDb;
		m_hNoteId = hNoteId;
		m_hAgentB64 = hAgent;
		
		RecycleHierarchy.addChild(m_parentDb, this);
	}

	public NotesDatabase getParent() {
		return m_parentDb;
	}
	
	public int getNoteId() {
		return m_hNoteId;
	}
	
	public String getUNID() {
		return getParent().toUnid(m_hNoteId);
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
		if (m_comment==null) {
			m_comment = getAgentNote().getItemValueAsText("$Comment", '\n');
		}
		return m_comment;
	}

	/**
	 * Returns the signer of the agent
	 * 
	 * @return signer
	 */
	public String getSigner() {
		return getAgentNote().getSigner();
	}
	
	@Override
	public void recycle() {
		if (isRecycled())
			return;

		RecycleHierarchy.removeChild(m_parentDb, this);
		
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
	 * The returned document is created when you save an agent, and it is stored in
	 * the same database as the agent.<br>
	 * The document replicates, but is not displayed in views.<br>
	 * Each time you edit and re-save an agent, its saved data document is deleted
	 * and a new, blank one is created. When you delete an agent, its saved data document is deleted.
	 * 
	 * @return document or null if agent could not be found
	 */
	public NotesNote getAgentSavedData(String agentName) {
		checkHandle();

		NotesDatabase parentDb = getParent();
		if (parentDb.isRecycled()) {
			throw new NotesError(0, "Parent database is recycled");
		}
		
		NotesUniversalNoteIdStruct.ByReference retUNID = NotesUniversalNoteIdStruct.newInstanceByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().AssistantGetLSDataNote(parentDb.getHandle64(), m_hNoteId, retUNID);
		}
		else {
			result = NotesNativeAPI32.get().AssistantGetLSDataNote(parentDb.getHandle32(), m_hNoteId, retUNID);
		}
		NotesErrorUtils.checkResult(result);
		
		String unid = retUNID.toString();
		if (StringUtil.isEmpty(unid) || "00000000000000000000000000000000".equals(unid)) {
			return null;
		}
		else {
			return parentDb.openNoteByUnid(unid);
		}
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
		
		//always reopen the DB so that the agent runs in a consistent state; otherwise
		//we would have Session.EffectiveUsername set to the signer and Evaluate("@Username")
		//return the user specified via AgentSetUserName
		int runFlags = NotesConstants.AGENT_REOPEN_DB;

		int ctxFlags = 0;
		
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
			short result = NotesNativeAPI64.get().AgentCreateRunContext(m_hAgentB64, null, ctxFlags, rethContext);
			NotesErrorUtils.checkResult(result);

			NotesNamesList namesListForAgentExecution = null;
			boolean freeNamesListAfterAgentRun = false;
			
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

				//agent should run with the specified identity (e.g. name of current web user);
				//if no user has been specified, we use the "run as web user" flag to decide what to do
				if (effectiveUserNameAsNamesList!=null) {
					namesListForAgentExecution = effectiveUserNameAsNamesList;
					freeNamesListAfterAgentRun = false;
				}
				else if (effectiveUserNameAsStringList!=null) {
					namesListForAgentExecution = NotesNamingUtils.writeNewNamesList(effectiveUserNameAsStringList);
					freeNamesListAfterAgentRun = true;
				}
				else if (effectiveUserNameAsString!=null) {
					namesListForAgentExecution = NotesNamingUtils.buildNamesList(effectiveUserNameAsString);
					freeNamesListAfterAgentRun = true;
				}
				else if (isRunAsWebUser()) {
					//inherit name of DB opener
					namesListForAgentExecution = m_parentDb.m_namesList;
					freeNamesListAfterAgentRun = false;
				}
				else {
					String signer = getSigner();
					if (!StringUtil.isEmpty(signer)) {
						//run agent as signer
						namesListForAgentExecution = NotesNamingUtils.buildNamesList(signer);
						freeNamesListAfterAgentRun = true;
					}
				}
				
				if (namesListForAgentExecution!=null && effectiveUserNameAsNamesList==null) { // don't overwrite privileges if we got a NAMES_LIST
					//set proper access privileges for NAMES_LIST
					if (m_parentDb.hasFullAccess()) {
						NotesNamingUtils.setPrivileges(namesListForAgentExecution, EnumSet.of(Privileges.Authenticated, Privileges.FullAdminAccess));
					}
					else {
						NotesNamingUtils.setPrivileges(namesListForAgentExecution, EnumSet.of(Privileges.Authenticated));
					}
					
					result = NotesNativeAPI64.get().AgentSetUserName(rethContext.getValue(), namesListForAgentExecution.getHandle64());
					NotesErrorUtils.checkResult(result);
				}
				
				result = NotesNativeAPI64.get().AgentRun(m_hAgentB64, rethContext.getValue(), 0, runFlags);
				handleAgentTimeoutError(result, timeoutSeconds);
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
				
				if (freeNamesListAfterAgentRun && namesListForAgentExecution!=null) {
					namesListForAgentExecution.free();
				}
			}
		}
		else {
			IntByReference rethContext = new IntByReference();
			short result = NotesNativeAPI32.get().AgentCreateRunContext(m_hAgentB32, null, ctxFlags, rethContext);
			NotesErrorUtils.checkResult(result);

			NotesNamesList namesListForAgentExecution = null;
			boolean freeNamesListAfterAgentRun = false;

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
				
				//agent should run with the specified identity (e.g. name of current web user);
				//if no user has been specified, we use the "run as web user" flag to decide what to do
				if (effectiveUserNameAsNamesList!=null) {
					namesListForAgentExecution = effectiveUserNameAsNamesList;
					freeNamesListAfterAgentRun = false;
				}
				else if (effectiveUserNameAsStringList!=null) {
					namesListForAgentExecution = NotesNamingUtils.writeNewNamesList(effectiveUserNameAsStringList);
					freeNamesListAfterAgentRun = true;
				}
				else if (effectiveUserNameAsString!=null) {
					namesListForAgentExecution = NotesNamingUtils.buildNamesList(effectiveUserNameAsString);
					freeNamesListAfterAgentRun = true;
				}
				else if (isRunAsWebUser()) {
					//inherit name of DB opener
					namesListForAgentExecution = m_parentDb.m_namesList;
					freeNamesListAfterAgentRun = false;
				}
				else {
					String signer = getSigner();
					if (!StringUtil.isEmpty(signer)) {
						//run agent as signer
						namesListForAgentExecution = NotesNamingUtils.buildNamesList(signer);
						freeNamesListAfterAgentRun = true;
					}
				}
				
				if (namesListForAgentExecution!=null && effectiveUserNameAsNamesList==null) { // don't overwrite privileges if we got a NAMES_LIST
					//set proper access privileges for NAMES_LIST
					if (m_parentDb.hasFullAccess()) {
						NotesNamingUtils.setPrivileges(namesListForAgentExecution, EnumSet.of(Privileges.Authenticated, Privileges.FullAdminAccess));
					}
					else {
						NotesNamingUtils.setPrivileges(namesListForAgentExecution, EnumSet.of(Privileges.Authenticated));
					}
					
					result = NotesNativeAPI32.get().AgentSetUserName(rethContext.getValue(), namesListForAgentExecution.getHandle32());
					NotesErrorUtils.checkResult(result);
				}

				result = NotesNativeAPI32.get().AgentRun(m_hAgentB32, rethContext.getValue(), 0, runFlags);
				handleAgentTimeoutError(result, timeoutSeconds);
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
				
				if (freeNamesListAfterAgentRun && namesListForAgentExecution!=null) {
					namesListForAgentExecution.free();
				}
			}
		}
	}

	/**
	 * Improves Exception error message for an agent execution timeout
	 * 
	 * @param err C API error code
	 * @param timeoutSec timeout in seconds
	 * @throws NotesError if a timeout occurred
	 */
	private void handleAgentTimeoutError(short err, int timeoutSec) throws NotesError {
		short status = (short) (err & NotesConstants.ERR_MASK);
		
		if (status == INotesErrorConstants.ERR_ASSISTANT_TIMEOUT) {
			// Execution time limit exceeded by Agent '%s' in database '%p'. Agent signer '%a'.
			String errMsg = NotesErrorUtils.errToString(status);

			errMsg = errMsg.replace("%s", getTitle());

			String signer = getSigner();
			errMsg = errMsg.replace("%a", signer);
			
			String dbServer = m_parentDb.getServer();
			String dbFilePath = m_parentDb.getRelativeFilePath();
			if (StringUtil.isEmpty(dbServer)) {
				errMsg = errMsg.replace("%p", dbFilePath);
			}
			else {
				errMsg = errMsg.replace("%p", dbServer+"!!"+dbFilePath);
			}
			
			//add current timeout to error text
			errMsg += " (Timeout: "+timeoutSec+"s)";
			
			throw new NotesError(err, errMsg);
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
	 * @param noteIdParamDoc note id of parameter document
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