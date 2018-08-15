package com.mindoo.domino.jna.utils;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDateRange;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to read and write Out-Of-Office (OOO) information
 * for a Dominp user.
 * 
 * @author Karsten Lehmann
 */
public class NotesOOOUtils {

	/**
	 * This function should be called prior to performing any OOO operation.<br>
	 * It initializes values for each specific user.<br>
	 * <br>
	 * When you are finished with the logic of a specific operation you are required
	 * to call OOOEndOperation routine.<br>
	 * For example,to check the state of OOO functionality for a specific user you would
	 * call OOOStartOperation, OOOGetState, OOOEndOperation. All strings are LMBCS
	 * strings.<br>
	 * The user is required to have a minimum of Editor level access in the ACL of
	 * their mail file.<br>
	 * <br>
	 * Effeciency Considerations:<br>
	 * For most efficient operation specify all optional parameters (home mail server
	 * and handle to the user’s mail file).<br>
	 * If home mail server is not specified or if the mail file handle is not provided,
	 * this function will look up this information on the server specified in
	 * <code>homeMailServer</code> parameter.<br>
	 * <br>
	 * If that lookup fails it will attempt a look up locally on the server where the
	 * application is running.<br>
	 * <br>
	 * If the second lookup fails and handle to the mail file was provided, then a lookup
	 * on the server where the database is located will be performed. If you would like
	 * to suppress the extra look ups and limit the look up only to the server which
	 * was specified in pMailServer parameter use the following ini variable on the
	 * server where this api/application is running.<br>
	 * <br>
	 * SUPRESS_OOO_DIRECTORY_FAILOVER_LOOKUP = 1<br>
	 * <br>
	 * When multiple lookups are performed it is typically a sign that there is a
	 * configuration problem in the domain and an event indicating this will be logged
	 * to the server console (and DDM).<br>
	 * <br>
	 * This event will be generated 5 or more minutes apart to avoid flooding the server.
	 * 
	 * @param mailOwnerName Canonical or abbreviated name of the owner of the mail where we are turning on OOO,Mandatory parameter.
	 * @param homeMailServer Canonical or abbreviated name of the server where the lookup for user information should be made (optional). If the server name is not a home mail server, an attempt will be made to figure out the home mail server by looking first locally and, if configured, in the extended directory. The lookups can be suppressed by providing the server name in <code>homeMailServer</code> parameter and setting the <code>isHomeMailServer</code> parameter to TRUE.  Suppressing lookups is a more efficient option.
	 * @param isHomeMailServer TRUE if the <code>homeMailServer</code> is user’s home mail(optional). Set it only if you are sure that user’s home mail server was specified.  If FALSE the look up for user’s home mail will be performed.
	 * @param dbMail If the application already has the mail file opened they can pass it in for better better efficiency.
	 * @return OOO context to read or write data
	 */
	public static NotesOOOContext startOperation(String mailOwnerName, String homeMailServer,
			boolean isHomeMailServer, NotesDatabase dbMail) {
		Memory mailOwnerNameMem = NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(mailOwnerName), true);
		Memory homeMailServerMem = NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(homeMailServer), true);
		
		PointerByReference pOOOOContext = new PointerByReference();
		
		short result;
		
		result = NotesNativeAPI.get().OOOInit();
		NotesErrorUtils.checkResult(result);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference hOOOContext = new LongByReference();
			
			result = NotesNativeAPI64.get().OOOStartOperation(mailOwnerNameMem,
					homeMailServerMem, isHomeMailServer ? 1 : 0, dbMail==null ? 0 : dbMail.getHandle64(), hOOOContext,
							pOOOOContext);
			NotesErrorUtils.checkResult(result);
			
			NotesOOOContext ctx = new NotesOOOContext(hOOOContext.getValue(), pOOOOContext.getValue());
			NotesGC.__memoryAllocated(ctx);
			return ctx;
		}
		else {
			IntByReference hOOOContext = new IntByReference();
			
			result = NotesNativeAPI32.get().OOOStartOperation(mailOwnerNameMem,
					homeMailServerMem, isHomeMailServer ? 1 : 0, dbMail==null ? 0 : dbMail.getHandle32(), hOOOContext,
							pOOOOContext);
			NotesErrorUtils.checkResult(result);
			
			NotesOOOContext ctx = new NotesOOOContext(hOOOContext.getValue(), pOOOOContext.getValue());
			NotesGC.__memoryAllocated(ctx);
			return ctx;
		}
	}

	public static class NotesOOOContext implements IAllocatedMemory {
		public static enum OOOType {AGENT, SERVICE}
		
		private int m_hOOOContext32;
		private long m_hOOOContext64;
		private Pointer m_pOOOContext;
		
		private NotesOOOContext(int hOOOContext, Pointer pOOOContext) {
			if (PlatformUtils.is64Bit())
				throw new NotesError(0, "Constructor is 32-bit only");
			
			m_hOOOContext32 = hOOOContext;
			m_pOOOContext = pOOOContext;
		}

		private NotesOOOContext(long hOOOContext, Pointer pOOOContext) {
			if (!PlatformUtils.is64Bit())
				throw new NotesError(0, "Constructor is 64-bit only");
			
			m_hOOOContext64 = hOOOContext;
			m_pOOOContext = pOOOContext;
		}

		@Override
		public void free() {
			if (isFreed())
				return;
			
			if (PlatformUtils.is64Bit()) {
				short result = NotesNativeAPI64.get().OOOEndOperation(m_hOOOContext64, m_pOOOContext);
				NotesErrorUtils.checkResult(result);
				m_hOOOContext64 = 0;
			}
			else {
				short result = NotesNativeAPI32.get().OOOEndOperation(m_hOOOContext32, m_pOOOContext);
				NotesErrorUtils.checkResult(result);
				m_hOOOContext32 = 0;
			}
		}

		@Override
		public boolean isFreed() {
			return PlatformUtils.is64Bit() ? m_hOOOContext64==0 : m_hOOOContext32==0;
		}

		@Override
		public int getHandle32() {
			return m_hOOOContext32;
		}

		@Override
		public long getHandle64() {
			return m_hOOOContext64;
		}

		private void checkHandle() {
			if (PlatformUtils.is64Bit()) {
				if (m_hOOOContext64==0)
					throw new NotesError(0, "OOO context already recycled");
				NotesGC.__b64_checkValidMemHandle(NotesOOOContext.class, m_hOOOContext64);
			}
			else {
				if (m_hOOOContext32==0)
					throw new NotesError(0, "OOO context already recycled");
				NotesGC.__b32_checkValidMemHandle(NotesOOOContext.class, m_hOOOContext32);
			}
		}
		
		/**
		 * This function returns time parameters that control OOO. 
		 * 
		 * @return away period
		 */
		public NotesDateRange getAwayPeriod() {
			checkHandle();
			
			NotesTimeDateStruct tdStartAwayStruct = NotesTimeDateStruct.newInstance();
			NotesTimeDateStruct tdEndAwayStruct = NotesTimeDateStruct.newInstance();
			
			short result = NotesNativeAPI.get().OOOGetAwayPeriod(m_pOOOContext, tdStartAwayStruct, tdEndAwayStruct);
			NotesErrorUtils.checkResult(result);
			
			NotesTimeDate tdStartAway = new NotesTimeDate(tdStartAwayStruct);
			NotesTimeDate tdEndAway = new NotesTimeDate(tdEndAwayStruct);
			
			return new NotesDateRange(tdStartAway, tdEndAway);
		}
		
		/**
		 * This function returns a flag which defines how to treat internet emails.<br>
		 * This functional call is optional.<br>
		 * If this flag is set to TRUE OOO notifications will not be generated for<br>
		 * email originating from the internet.  The default for this flag is TRUE.
		 * 
		 * @return true if excluded
		 */
		public boolean isExcludeInternet() {
			checkHandle();
			
			IntByReference bExcludeInternet = new IntByReference();
			short result = NotesNativeAPI.get().OOOGetExcludeInternet(m_pOOOContext, bExcludeInternet);
			NotesErrorUtils.checkResult(result);
			return bExcludeInternet.getValue()==1;
		}
		
		/**
		 * This function sets a flag which defines how to treat internet emails.<br>
		 * <br>
		 * This functional call is optional.<br>
		 * If this flag is set to TRUE OOO notifications will not be generated for
		 * email originating from the internet.<br>
		 * The default for this flag is TRUE.
		 * 
		 * @param exclude true to exclude
		 */
		public void setExcludeInternet(boolean exclude) {
			checkHandle();
			
			short result = NotesNativeAPI.get().OOOSetExcludeInternet(m_pOOOContext, exclude ? 1 : 0);
			NotesErrorUtils.checkResult(result);
		}
		
		/**
		 * Convenience method to check whether the OOO functionality is enabled. Calls
		 * {@link #getState(Ref, Ref)} internally.
		 * 
		 * @return true if enabled
		 */
		public boolean isEnabled() {
			Ref<Boolean> retIsEnabled = new Ref<Boolean>();
			getState(null, retIsEnabled);
			return Boolean.TRUE.equals(retIsEnabled.get());
		}
		
		/**
		 * Convenience method to read which kind of OOO system is used (agent or service).
		 * Calls {@link #getState(Ref, Ref)} internally.
		 * 
		 * @return type
		 */
		public OOOType getType() {
			Ref<OOOType> retType = new Ref<OOOType>();
			getState(retType, null);
			return retType.get();
		}
		
		/**
		 * This function returns the version (agent, service) and the state (disabled, enabled)
		 * of the out of office functionality.<br>
		 * The version information can be used to show or hide UI elements that might not be
		 * supported for a given version.<br>
		 * For example, the agent does not support durations of less than 1 day and some
		 * clients might choose not to show the hours in the user interface.<br>
		 * When you need to make {@link #getState(Ref, Ref)} as efficient as possible, call
		 * {@link NotesOOOUtils#startOperation(String, String, boolean, NotesDatabase)}
		 * with the home mail server and the opened mail database.<br>
		 * This function is read only and does not return an error if user ACL rights
		 * are below Editor (which are required to turn on/off the Out of office functionality).<br>
		 * If {@link #getState(Ref, Ref)} is called immediately following OOOEnable it will
		 * not reflect the state set by the OOOEnable.<br>
		 * To see the current state call {@link #free()} and start a new operation using
		 * {@link NotesOOOUtils#startOperation(String, String, boolean, NotesDatabase)},
		 * {@link NotesOOOContext#getState(Ref, Ref)} and {@link #free()}.

		 * @param retType returns the type of the OOO system (agent or service)
		 * @param retIsEnabled returns whether the service is enabled for the user
		 */
		public void getState(Ref<OOOType> retType, Ref<Boolean> retIsEnabled) {
			checkHandle();

			ShortByReference retVersion = new ShortByReference();
			ShortByReference retState = new ShortByReference();

			short result = NotesNativeAPI.get().OOOGetState(m_pOOOContext, retVersion, retState);
			NotesErrorUtils.checkResult(result);

			if (retType!=null) {
				if (retVersion.getValue() == 1) {
					retType.set(OOOType.AGENT);
				}
				else if (retVersion.getValue() == 2) {
					retType.set(OOOType.SERVICE);
				}
			}
			if (retIsEnabled!=null) {
				if (retState.getValue()==1) {
					retIsEnabled.set(Boolean.TRUE);
				}
				else {
					retIsEnabled.set(Boolean.FALSE);
				}
			}
		}

		/**
		 * OOO supports two sets of messages.<br>
		 * <br>
		 * They are called General message/subject and Special message/subject.<br>
		 * This function gets the general subject.<br>
		 * This is string that will appear as the subject line of the OOO notification. 
		 * 
		 * @return subject
		 */
		public String getGeneralSubject() {
			checkHandle();
			
			DisposableMemory retSubject = new DisposableMemory(NotesConstants.OOOPROF_MAX_BODY_SIZE);
			try {
				short result = NotesNativeAPI.get().OOOGetGeneralSubject(m_pOOOContext, retSubject);
				NotesErrorUtils.checkResult(result);
				
				String subject = NotesStringUtils.fromLMBCS(retSubject, -1);
				return subject;
			}
			finally {
				retSubject.dispose();
			}
		}
		
		/**
		 * OOO supports two sets of messages. They are called General message/subject and
		 * Special message/subject.<br>
		 * This function returns the text of the general message.
		 * 
		 * @return message
		 */
		public String getGeneralMessage() {
			checkHandle();
			
			//first get the length
			ShortByReference retGeneralMessageLen = new ShortByReference();
			
			short result = NotesNativeAPI.get().OOOGetGeneralMessage(m_pOOOContext, null, retGeneralMessageLen);
			NotesErrorUtils.checkResult(result);
			
			int iGeneralMessageLen = (int) (retGeneralMessageLen.getValue() & 0xffff);
			if (iGeneralMessageLen==0)
				return "";
			
			DisposableMemory retMessage = new DisposableMemory(iGeneralMessageLen + 1);
			try {
				result = NotesNativeAPI.get().OOOGetGeneralMessage(m_pOOOContext, retMessage, retGeneralMessageLen);
				NotesErrorUtils.checkResult(result);
				String msg = NotesStringUtils.fromLMBCS(retMessage, retGeneralMessageLen.getValue());
				return msg;
			}
			finally {
				retMessage.dispose();
			}
		}
		
		/**
		 * This function validates and sets the time parameters that control OOO.<br>
		 * <br>
		 * This information is required for enabling the OOO.<br>
		 * If you want turn on OOO functionality for a given period of time the
		 * sequence of calls needed is:<br>
		 * {@link NotesOOOUtils#startOperation(String, String, boolean, NotesDatabase)},
		 * {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)}, {@link #setEnabled(boolean)}
		 * and {@link #free()}.<br>
		 * <br>
		 * 	When you need to enable OOO (i.e. call it with <code>enabled</code> flag set to TRUE)
		 * you should call {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)} prior to calling
		 * {@link #setEnabled(boolean)}.<br>
		 * <br>
		 * If you need to change the length of the away period after OOO has already been
		 * enabled, the sequence of calls needed to perform this action is
		 * {@link NotesOOOUtils#startOperation(String, String, boolean, NotesDatabase)},
		 * {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)}, {@link #free()}.<br>
		 * <br>
		 * If the Domino server is configured to run an OOO agent, it can only be turned on
		 * for full days, the time portion of the date parameter will not be used.
		 * 
		 * @param tdStartAway This is date and time when Out of office will begin.
		 * @param tdEndAway This is date and time when Out of office will end.
		 */
		public void setAwayPeriod(NotesTimeDate tdStartAway, NotesTimeDate tdEndAway) {
			checkHandle();
			
			NotesTimeDateStruct.ByValue tdStartWayByVal = NotesTimeDateStruct.ByValue.newInstance(tdStartAway.getInnards());
			NotesTimeDateStruct.ByValue tdEndWayByVal = NotesTimeDateStruct.ByValue.newInstance(tdEndAway.getInnards());
			short result = NotesNativeAPI.get().OOOSetAwayPeriod(m_pOOOContext, tdStartWayByVal, tdEndWayByVal);
			NotesErrorUtils.checkResult(result);
		}
		
		/**
		 * This function changes the state of the OOO functionality as indicated by
		 * the <code>enabled</code> variable.<br>
		 * If the OOO functionality is already in the state indicated by the
		 * <code>enabled</code> flag, this function does nothing.<br>
		 * <br>
		 * When you need to enable OOO (i.e. call it with <code>enabled</code> flag set
		 * to TRUE) you should call {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)}
		 * prior to calling {@link #setEnabled(boolean)}.<br>
		 * <br>
		 * If {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)} is not called,
		 * {@link #setEnabled(boolean)} will use the previous value for start and end.<br>
		 * <br>
		 * If they are in the past then the OOO functionality will not be enabled.<br>
		 * When you need to disable OOO (i.e. call it with <code>enabled</code> set to FALSE)
		 * {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)} does not need to be called.<br>
		 * <br>
		 * When {@link #setEnabled(boolean)} is called with the <code>enabled</code> set
		 * to FALSE it means you want to disable OOO immediately.<br>
		 * If you don’t want to disable OOO functionality immediately, but rather you
		 * just want to change the time when OOO should stop operating, the sequence
		 * of calls is : {@link NotesOOOUtils#startOperation(String, String, boolean, NotesDatabase)},
		 * {@link #setAwayPeriod(NotesTimeDate, NotesTimeDate)}, {@link #free()}.<br>
		 * If OOO is configured to run as a service and {@link #setEnabled(boolean)} is
		 * used to disable, the OOO service will be auto-disabled immediately.<br>
		 * <br>
		 * The summary report will be generated on the first email received after the
		 * disable has been requested, or if no messages are received it will
		 * generated during the nightly router maintenance.<br>
		 * <br>
		 * If OOO is configured as an agent, the user will receive a summary report
		 * and a request to disable the agent on the next scheduled run of the agent will occur.
		 * 
		 * @param enabled true to enable
		 */
		public void setEnabled(boolean enabled) {
			checkHandle();
			
			short result = NotesNativeAPI.get().OOOEnable(m_pOOOContext, enabled ? 1 : 0);
			NotesErrorUtils.checkResult(result);
		}
		
		/**
		 * OOO supports two sets of notification messages.<br>
		 * <br>
		 * They are called General message/subject and Special message/subject.<br>
		 * The rest of the people will receive the general message/subject message.<br>
		 * This function sets the general subject.<br>
		 * If this field is not specified in by this API call, the value defined
		 * using Notes Client will be used, otherwise the default for this field
		 * is the following text <i>AUTO: Katherine Smith is out of the office (returning 02/23/2009 10:12:17 AM)</i>.
		 * 
		 * @param subject string that will appear as the subject line of the OOO notification
		 * @param displayReturnDate Boolean which controls whether (“returning &lt;date&gt;”) appears on the subject line
		 */
		public void setGeneralSubject(String subject, boolean displayReturnDate) {
			checkHandle();
			
			Memory subjectMem = NotesStringUtils.toLMBCS(subject, true);
			short result = NotesNativeAPI.get().OOOSetGeneralSubject(m_pOOOContext, subjectMem, displayReturnDate ? 1 : 0);
			NotesErrorUtils.checkResult(result);
		}
		
		/**
		 * OOO supports two sets of notification messages.<br>
		 * They are called General message/subject and Special message/subject.<br>
		 * The following text is always appended to the body of the message, where
		 * the "Message subject" is obtained from the message which caused the
		 * notification to be generated.<br>
		 * <i>"Note: This is an automated response to your message "Message subject"
		 * sent on 2/12/2009 10:12:17 AM. This is the only notification you will receive while this person is away."</i>
		 * 
		 * @param msg message, max 65535 bytes LMBCS encoded (WORD datatype for length)
		 */
		public void setGeneralMessage(String msg) {
			checkHandle();
			
			Memory msgMem = NotesStringUtils.toLMBCS(msg, false);
			if (msgMem.size() > 65535)
				throw new IllegalArgumentException("Message exceeds max length, "+msgMem.size() + "> 65535 bytes");
			
			short result = NotesNativeAPI.get().OOOSetGeneralMessage(m_pOOOContext, msgMem, (short) (msgMem.size() & 0xffff));
			NotesErrorUtils.checkResult(result);
		}
	}
}
