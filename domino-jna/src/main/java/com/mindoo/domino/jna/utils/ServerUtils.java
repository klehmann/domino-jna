package com.mindoo.domino.jna.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.constants.ClusterLookup;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.ConsoleLine;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.structs.NotesConsoleEntryStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils.LineBreakConversion;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public class ServerUtils {

	/**
	 * The NSGetServerClusterMates function retrieves a list of server names that belong to the
	 * same cluster as the server specified by pServerName.<br>
	 * If the <code>serverName</code> parameter is NULL then the function retrieves the cluster
	 * members of the user's home server.<br>
	 * <br>
	 * The <code>lookupMode</code> parameter controls how the information is retrieved.<br>
	 * If the {@link ClusterLookup#LOOKUP_NOCACHE} flag is specified then the information is
	 * retrieved using a NameLookup on the server only.<br>
	 * <br>
	 * If the {@link ClusterLookup#LOOKUP_CACHEONLY} flag is specified then the information is
	 * retrieved using the client's cluster name cache.<br>
	 * <br>
	 * If no flag (a value of NULL) is specified, then the information is retrieved first
	 * through the client's cluster name cache and if that is not successful, then through
	 * a NameLookup on the server.<br>
	 * Note that the list returned does not include the input server name (or home server
	 * name if NULL was specified).<br>
	 * <br>
	 * {@link #getServerClusterMates(String, ClusterLookup)} uses the Address book specified by the user's location record.<br>
	 * Unless cascading Address books or Directory Assistance is enabled, the Notes mail
	 * domain field in the user's location record must be set to the domain name for the
	 * server(s) in the cluster and the Home/mail server field must be set to a server in this domain.<br>
	 * <br>
	 * If the target server is in a different domain than specified in the user's location record
	 * then in order for {@link #getServerClusterMates(String, ClusterLookup)} to succeed, you must have cascading Address
	 * books or Directory Assistance enabled and the target domain's Address book must be in the
	 * list of Address books to be searched.

	 * @param serverName The name of the Lotus Domino Server where the lookup will be performed (canonical or abbreviated format). Specify a value of NULL if the client's home server is to be used for the lookup.
	 * @param lookupMode lookup mode or null for "first local cache, then remote lookup"
	 * @return server list
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getServerClusterMates(String serverName, ClusterLookup lookupMode) {
		Memory serverNameCanonical = serverName==null ? null : NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(serverName), true);

		short result;

		if (PlatformUtils.is64Bit()) {
			LongByReference phList = new LongByReference();

			result = NotesNativeAPI64.get().NSGetServerClusterMates(serverNameCanonical, lookupMode==null ? 0 : lookupMode.getValue(), phList);
			if (result == 2078) // "No cluster mates found"
				return Collections.emptyList();
			NotesErrorUtils.checkResult(result);

			long hList = phList.getValue();
			if (hList==0)
				return Collections.emptyList();

			Pointer pList = Mem64.OSLockObject(hList);
			try {
				@SuppressWarnings("rawtypes")
				List clusterMates = ItemDecoder.decodeTextListValue(pList, false);
				return clusterMates;
			}
			finally {
				Mem64.OSUnlockObject(hList);
				result = Mem64.OSMemFree(hList);
				NotesErrorUtils.checkResult(result);
			}
		}
		else {
			IntByReference phList = new IntByReference();

			result = NotesNativeAPI32.get().NSGetServerClusterMates(serverNameCanonical, lookupMode==null ? 0 : lookupMode.getValue(), phList);
			if (result == 2078) // "No cluster mates found"
				return Collections.emptyList();
			NotesErrorUtils.checkResult(result);

			int hList = phList.getValue();
			if (hList==0)
				return Collections.emptyList();

			Pointer pList = Mem32.OSLockObject(hList);
			try {
				@SuppressWarnings("rawtypes")
				List clusterMates = ItemDecoder.decodeTextListValue(pList, false);
				return clusterMates;
			}
			finally {
				Mem32.OSUnlockObject(hList);
				result = Mem32.OSMemFree(hList);
				NotesErrorUtils.checkResult(result);
			}
		}
	}

	/**
	 * This function is used to issue a console command to a server from an API program
	 * or from an API server add-in program.<br>
	 * <br>
	 * If you do not have remote access to the server an error will be returned.<br>
	 * To have remote access to a server, you must be listed in the Server Document
	 * Administrators field or in the Admin variable in the server's notes.ini.<br>
	 * <br>
	 * NOTE: If you use this function to shut down a server (by entering the "exit" or
	 * "quit" commands), you may receive an error code of "Server not responding"
	 * or "Remote system no longer responding".<br>
	 * <br>
	 * Assuming that the server was active when you issued the command, these errors
	 * usually mean that your command was successful (the server shuts down before it
	 * can return a meaningful response).<br>
	 * <br>
	 * NOTE:  This function will return NOERROR if the server's disk is full, but
	 * the returned response buffer will be 0-length. 
	 * 
	 * @param server name of the server that is to receive the console command
	 * @param command  text command to send to the server's console
	 * @return returned server's response
	 */
	public static String sendConsoleCommand(String server, String command) {
		if (StringUtil.isEmpty(server)) {
			//prevent error "Invalid server or network name syntax" (error code: 274)
			server = IDUtils.getIdUsername();
		}
		Memory serverMem = NotesStringUtils.toLMBCS(server, true);
		Memory commandMem = NotesStringUtils.toLMBCS(command, true);

		if (PlatformUtils.is64Bit()) {
			LongByReference hResponseText = new LongByReference();

			short result = NotesNativeAPI64.get().NSFRemoteConsole(serverMem, commandMem, hResponseText);
			NotesErrorUtils.checkResult(result);

			long hResponseTextValue = hResponseText.getValue();
			if (hResponseTextValue==0) {
				return "";
			}

			Pointer ptr = Mem64.OSLockObject(hResponseTextValue);
			try {
				String txt = NotesStringUtils.fromLMBCS(ptr, -1);
				return txt;
			}
			finally {
				Mem64.OSUnlockObject(hResponseTextValue);
				Mem64.OSMemFree(hResponseTextValue);
			}
		}
		else {
			IntByReference hResponseText = new IntByReference();

			short result = NotesNativeAPI32.get().NSFRemoteConsole(serverMem, commandMem, hResponseText);
			NotesErrorUtils.checkResult(result);

			int hResponseTextValue = hResponseText.getValue();
			if (hResponseTextValue==0) {
				return "";
			}

			Pointer ptr = Mem32.OSLockObject(hResponseTextValue);
			try {
				String txt = NotesStringUtils.fromLMBCS(ptr, -1);
				return txt;
			}
			finally {
				Mem32.OSUnlockObject(hResponseTextValue);
				Mem32.OSMemFree(hResponseTextValue);
			}
		}
	}

	/**
	 * Handler to receive a server console line with text and meta data
	 */
	public interface ConsoleHandler {

		/**
		 * Method is called by {@link ServerUtils#openServerConsole(String, ConsoleHandler)}
		 * to check if we should stop listening for console messages.
		 * 
		 * @return true to stop
		 */
		public boolean shouldStop();

		/**
		 * Method to receive the console line with text and meta data like the pid/tid/executable
		 * name
		 * 
		 * @param line console line
		 */
		void messageReceived(IConsoleLine line);
		
	}
	
	/**
	 * Opens a remote console for the specified server. Please note that for Domino versions until
	 * R11 this command only works locally in the client. Domino R12.0.0 will provide console access
	 * on server side after the following Notes.ini variable has been set:
	 * <code>DEBUG_ALLOW_REMOTE_CON_SERVER=1</code>.
	 * 
	 * @param serverName server name (abbreviated or canonical format) or empty string for local server
	 * @param handler handler to receive the console messages
	 */
	public static void openServerConsole(String serverName, ConsoleHandler handler) {
		String serverNameCanonical = StringUtil.isEmpty(serverName) ? IDUtils.getIdUsername() : NotesNamingUtils.toCanonicalName(serverName);
		Memory serverNameCanonicalMem = NotesStringUtils.toLMBCS(serverNameCanonical, true);
		
		{
			//for simplicity we send an empty synchronous remote console command
			//to check for the error "You are not authorized to use the remote console on this server"
			//otherwise that error status is just returned asynchronously via ASYNC_CONTEXT and the required
			//data structure is quite complex
			DHANDLE.ByReference hResponseText = DHANDLE.newInstanceByReference();

			short result = NotesNativeAPI.get().NSFRemoteConsole(serverNameCanonicalMem, null, hResponseText);
			NotesErrorUtils.checkResult(result);

			if (!hResponseText.isNull()) {
				Mem.OSMemFree(hResponseText.getByValue());
			}
		}
		
		String cmd = null; //"sh ta";
		Memory cmdMem = StringUtil.isEmpty(cmd) ? null : NotesStringUtils.toLMBCS(cmd, true);
		
		PointerByReference pAsyncCtx = new PointerByReference();
		DHANDLE.ByReference hAsyncQueue = DHANDLE.newInstanceByReference();
		DHANDLE.ByReference hAsyncBuffer = DHANDLE.newInstanceByReference();

		ShortByReference wSignals = new ShortByReference();
		IntByReference dwConsoleBuffID = new IntByReference();

		short result = NotesNativeAPI.get().QueueCreate(hAsyncQueue);
		NotesErrorUtils.checkResult(result);
		
		boolean asyncIOInitDone = false;

		try {
			NotesCallbacks.ASYNCNOTIFYPROC callback;
			if (PlatformUtils.isWin32()) {
				callback = new Win32NotesCallbacks.ASYNCNOTIFYPROCWin32() {
					
					@Override
					public void invoke(Pointer p1, Pointer p2) {
					}
				};
			}
			else {
				callback = new NotesCallbacks.ASYNCNOTIFYPROC() {
					
					@Override
					public void invoke(Pointer p1, Pointer p2) {
					}
				};
			}

			result = AccessController.doPrivileged((PrivilegedAction<Short>) ()->{
				return NotesNativeAPI.get().NSFRemoteConsoleAsync(serverNameCanonicalMem, cmdMem,
						NotesConstants.REMCON_GET_CONSOLE | NotesConstants.REMCON_GET_CONSOLE_META,
						hAsyncBuffer,
						null, null, wSignals, dwConsoleBuffID, hAsyncQueue.getByValue(), callback, null, pAsyncCtx);
			});
			
			NotesErrorUtils.checkResult(result);
			
			while (true) {
				if (handler.shouldStop()) {
					break;
				}

				NotesNativeAPI.get().NSFAsyncNotifyPoll(new Pointer(0), null, null);
				NotesNativeAPI.get().NSFUpdateAsyncIOStatus(pAsyncCtx.getValue());
				asyncIOInitDone = true;

				short hasData = NotesNativeAPI.get().QueueGet(hAsyncQueue.getByValue(), hAsyncBuffer);
				
				if (hasData==0) {
					Pointer ptr = Mem.OSLockObject(hAsyncBuffer);
					try {
						NotesConsoleEntryStruct consoleEntry = NotesConsoleEntryStruct.newInstance(ptr);
						consoleEntry.read();

						int len = consoleEntry.length;
						if (consoleEntry.type == 1) {
							String lineEncoded = NotesStringUtils.fromLMBCS(ptr.share(consoleEntry.size()),
									len);
							IConsoleLine consoleLine = ConsoleLine.parseConsoleLine(lineEncoded, 0);
							handler.messageReceived(consoleLine);
						}
					}
					finally {
						Mem.OSUnlockObject(hAsyncBuffer);
						Mem.OSMemFree(hAsyncBuffer.getByValue());
					}
				}
				else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}
		finally {
			if (asyncIOInitDone) {
				if (pAsyncCtx.getValue()!=null) {
					NotesNativeAPI.get().NSFCancelAsyncIO(pAsyncCtx.getValue());
				}
			}
		
			NotesNativeAPI.get().QueueDelete(hAsyncQueue.getByValue());
		}
		
	}
	
	public enum PasswordDigestType {
		/** Compatible with 4.5+ */ 
		V1(NotesConstants.SEC_pwddigest_V1),
		/** More secure, compatible with R4.6+ */
		V2(NotesConstants.SEC_pwddigest_V2),
		/** Even more secure compatible with 8.01+ */
		V3(NotesConstants.SEC_pwddigest_V3);
		
		private int val;
		
		private PasswordDigestType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return val;
		}
	}
	
	/**
	 * This function takes an unencoded password and returns the more secure version of the digest.
	 * The Internet Password is in this "more secure" format.
	 * 
	 * @param pwd password
	 * @param type digest type to produce
	 * @return hashed password
	 */
	public static String hashPassword(String pwd, PasswordDigestType type) {
		if (pwd==null) {
			throw new IllegalArgumentException("Password is null");
		}
		if (StringUtil.isEmpty(pwd)) {
			throw new IllegalArgumentException("Password is empty");
		}

		Memory pwdMem = NotesStringUtils.toLMBCS(pwd, false);
		if (pwdMem.size() > 65535) {
			throw new IllegalArgumentException("Password exceed max size of 0xffff bytes");
		}
		short wPasswordLen = (short) (pwdMem.size() & 0xffff);

		DisposableMemory retDigest = new DisposableMemory(Short.toUnsignedInt(NotesConstants.MAXWORD) - 128);
		try {
			short wVersion = (short) (type.getVal() & 0xffff);
			short wHashType = NotesConstants.SEC_ai_HMAC_SHA1;
			ShortByReference retDigestLen = new ShortByReference();

			short result = NotesNativeAPI.get().SECHashPassword3(
					wPasswordLen,
					pwdMem,
					wVersion,
					wHashType,
					(Pointer) null,
					12345, // not sure about this one, copied from example hashpwd.c
					(Pointer) null,
					0,
					(short) (retDigest.size() & 0xffff),
					retDigestLen,
					retDigest,
					0,
					(Pointer) null
					);
			NotesErrorUtils.checkResult(result);
			
			String retDigestStr = NotesStringUtils.fromLMBCS(retDigest, Short.toUnsignedInt(retDigestLen.getValue()));
			return retDigestStr;
		}
		finally {
			retDigest.dispose();
		}

	}
	
	/**
	 * This function verifies an unencoded password against a digest password value. The unencoded password
	 * can be either an unencoded Internet Password or Notes ID Password. The digest password value can be
	 * either an Internet Password (more secure digest value) or a Password Digest. The unencoded Internet
	 * Password is verified against the Internet Password. The Notes ID Password is verified against
	 * the Password Digest.
	 * 
	 * @param pwd Unencoded password to be verified.
	 * @param digest Digest to be compared against.
	 */
	public static void verifyPassword(String pwd, String digest) {
		if (pwd==null) {
			throw new IllegalArgumentException("Password is null");
		}
		if (StringUtil.isEmpty(pwd)) {
			throw new IllegalArgumentException("Password is empty");
		}
		
		Memory pwdMem = NotesStringUtils.toLMBCS(pwd, false);
		if (pwdMem.size() > 65535) {
			throw new IllegalArgumentException("Password exceed max size of 0xffff bytes");
		}

		if (digest==null) {
			throw new IllegalArgumentException("Digest is null");
		}
		if (StringUtil.isEmpty(digest)) {
			throw new IllegalArgumentException("Digest is empty");
		}
		
		Memory digestMem = NotesStringUtils.toLMBCS(digest, false);
		if (digestMem.size() > 65535) {
			throw new IllegalArgumentException("Digest exceed max size of 0xffff bytes");
		}
		
		short pwdLen = (short) (pwdMem.size() & 0xffff);
		short digestLen = (short) (digestMem.size() & 0xffff);
		
		short result = NotesNativeAPI.get().SECVerifyPassword(pwdLen, pwdMem,
				digestLen, digestMem, 0, null);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Format and write a message to the log file. <br>
	 * <br>
	 * This function formats a message, displays it on the standard output, and appends the message as
	 * new line to the "Events" field in a Miscellaneous Events document in the Domino server or Notes client log.<br>
	 * <br>
	 * The generated message has the form:<br>
	 * <br>
	 * <code>&lt;DATE&gt;  &lt;TIME&gt; &lt;Primary status message&gt;</code><br>
	 * <br>
	 * Using this method instead of a simple <code>System.out.println()</code> has the benefit that
	 * the message will be directly visible on the server console, while in other cases Domino might
	 * buffer the messages written by Java (e.g. when an OSGi command is registered via the Eclipse
	 * CommandInterpreter) until Java execution is done.
	 * 
	 * @param messageText message text
	 */
	public static void writeLogMessage(String messageText) {
	    String[] lines = messageText.split("\\r?\\n", -1);
	    for (String line : lines) {
	      Memory lmbcs = NotesStringUtils.toLMBCS(line, true);
	      NotesNativeAPI.get().AddInLogMessageText(lmbcs, (short)0, new Object[0]);
	    }
	}
	
}
