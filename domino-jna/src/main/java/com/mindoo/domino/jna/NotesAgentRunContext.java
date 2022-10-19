package com.mindoo.domino.jna;

import java.io.Writer;
import java.util.List;

import lotus.domino.Document;

/**
 * Container object for values used to run an agent
 * 
 * @author Karsten Lehmann
 */
public class NotesAgentRunContext {
	private boolean m_checkSecurity;
	private Writer m_stdOut;
	private int m_timeoutSeconds;
	private NotesNote m_documentContextAsNote;
	private Document m_documentContextAsLegacyDoc;
	private int m_paramDocId;
	private String m_userName;
	private List<String> m_userNameAsStringList;
	private NotesNamesList m_userNameAsNamesList;

	/**
	 * Creates a new instance
	 */
	public NotesAgentRunContext() {
		m_checkSecurity = true;
	}

	/**
	 * Returns whether security should be checked
	 * 
	 * @return true to check
	 */
	public boolean isCheckSecurity() {
		return m_checkSecurity;
	}

	/**
	 * Use this method to set the AGENT_SECURITY_ON flag:<br>
	 * <br>
	 * AGENT_SECURITY_ON:<br>
	 * Use this flag to tell the run context that when it runs an agent, you
	 * want it to check the privileges of the signer of that agent and apply
	 * them. For example, if the signer of the agent has "restricted" agent
	 * privileges, then the agent will be restricted. If you don't set this
	 * flag, all agents run as unrestricted.<br>
	 * <ul>
	 * <li>List of security checks enabled by this flag:</li>
	 * <li>Restricted/unrestricted agent</li>
	 * <li>Can create databases</li>
	 * <li>Is agent targeted to this machine</li>
	 * <li>Is user allowed to access this machine</li>
	 * <li>Can user run personal agents</li>
	 * </ul>
	 * @param checkSecurity true to check security, true by default
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setCheckSecurity(boolean checkSecurity) {
		this.m_checkSecurity = checkSecurity;
		return this;
	}

	/**
	 * Returns the output writer used for Print statements during agent execution
	 * 
	 * @return writer
	 */
	public Writer getOutputWriter() {
		return m_stdOut;
	}

	/**
	 * If this method is used to set an output writer, we will collect the agent output produced
	 * during execution (e.g. via Print statements in LotusScript) and write it to the
	 * specified writer <b>when the agent execution is done</b>.
	 * 
	 * @param writer output writer, null by default
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setOutputWriter(Writer writer) {
		this.m_stdOut = writer;
		return this;
	}

	/**
	 * Returns the agent timeout
	 * 
	 * @return timeout
	 */
	public int getTimeoutSeconds() {
		return m_timeoutSeconds;
	}

	/**
	 * Sets an execution timeout in seconds
	 * 
	 * @param timeoutSeconds timeout in seconds, 0 by default
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setTimeoutSeconds(int timeoutSeconds) {
		this.m_timeoutSeconds = timeoutSeconds;
		return this;
	}

	/**
	 * Returns the Document for Session.DocumentContext as a {@link NotesNote}
	 * 
	 * @return document context
	 */
	public NotesNote getDocumentContextAsNote() {
		return m_documentContextAsNote;
	}

	/**
	 * Sets the Document for Session.DocumentContext as a {@link NotesNote}.<br>
	 * <br>
	 * @param documentContextAsNote document context, can be in-memory only (not saved yet)
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setDocumentContextAsNote(NotesNote documentContextAsNote) {
		this.m_documentContextAsNote = documentContextAsNote;
		return this;
	}

	/**
	 * Returns the Document for Session.DocumentContext as a legacy {@link Document}.
	 * 
	 * @return document context
	 */
	public Document getDocumentContextAsLegacyDoc() {
		return m_documentContextAsLegacyDoc;
	}

	/**
	 * Sets the Document for Session.DocumentContext as a legacy {@link Document}.<br>
	 * <br>
	 * Method only works in Standard Client and on a Domino server, as it uses the class
	 * com.ibm.domino.napi.c.BackendBridge to extract the C handle from the Document,
	 * which is not available in the Basic Client (in R9) and the used library njnotes.dll / libjnotes.so
	 * is not part of the build.<br>
	 * If this API is used inside an OSGi plugin, also make sure that there is a defined
	 * dependency on com.ibm.domino.napi to let the classloader find the BackendBridge class.
	 * 
	 * @param doc document context, can be in-memory only (not saved yet)
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setDocumentContextAsLegacyDoc(Document doc) {
		this.m_documentContextAsLegacyDoc = doc;
		return this;
	}

	/**
	 * Returns the note id for Session.ParameterDocId
	 * 
	 * @return note id
	 */
	public int getParamDocId() {
		return m_paramDocId;
	}

	/**
	 * Sets the note id for Session.ParameterDocId
	 * 
	 * @param paramDocId note id, 0 by default
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setParamDocId(int paramDocId) {
		this.m_paramDocId = paramDocId;
		return this;
	}

	/**
	 * Returns the Notes username e.g. to be used for evaluating @UserNamesList
	 * 
	 * @return username
	 */
	public String getUsername() {
		return m_userName;
	}

	/**
	 * Sets the Notes username e.g. to be used for evaluating @UserNamesList.
	 * Unfortunately, this does not cover Session.EffectiveUserName.
	 * We still need to find a way to change this (when calling WebQueryOpen/Save agents manually), if there is any.
	 * 
	 * @param sessionEffectiveName either in canonical or abbreviated format, null by default, which means Session.EffectiveUserName will be the agent signer
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setUsername(String sessionEffectiveName) {
		this.m_userName = sessionEffectiveName;
		return this;
	}

	/**
	 * Returns the Notes username e.g. to be used for evaluating @UserNamesList as a string list
	 * to write a custom {@link NotesNamesList}.
	 * 
	 * @return string list with output like from @UserNamesList in canonical or abbreviated format
	 */
	public List<String> getUsernameAsStringList() {
		return m_userNameAsStringList;
	}

	/**
	 * Returns the Notes username e.g. to be used for evaluating @UserNamesList as a string list
	 * to write a custom {@link NotesNamesList}. Unfortunately, this does not cover Session.EffectiveUserName.
	 * We still need to find a way to change this (when calling WebQueryOpen/Save agents manually), if there is any.
	 * 
	 * @param sessionEffectiveNameAsStringList string list with output like from @UserNamesList in canonical or abbreviated format, null by default, which means Session.EffectiveUserName will be the agent signer
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setUsernameAsStringList(List<String> sessionEffectiveNameAsStringList) {
		this.m_userNameAsStringList = sessionEffectiveNameAsStringList;
		return this;
	}

	/**
	 * Returns the Notes username e.g. to be used for evaluating @UserNamesList as a {@link NotesNamesList}
	 * 
	 * @return names list
	 */
	public NotesNamesList getUsernameAsNamesList() {
		return m_userNameAsNamesList;
	}

	/**
	 * Sets the Notes username e.g. to be used for evaluating @UserNamesList as a {@link NotesNamesList}.
	 * Unfortunately, this does not cover Session.EffectiveUserName.
	 * We still need to find a way to change this (when calling WebQueryOpen/Save agents manually), if there is any.
	 * 
	 * @param sessionEffectiveNameAsNamesList names list, null by default, which means Session.EffectiveUserName will be the agent signer
	 * @return this context object (for chained calls)
	 */
	public NotesAgentRunContext setUsernameAsNamesList(NotesNamesList sessionEffectiveNameAsNamesList) {
		this.m_userNameAsNamesList = sessionEffectiveNameAsNamesList;
		return this;
	}

}
