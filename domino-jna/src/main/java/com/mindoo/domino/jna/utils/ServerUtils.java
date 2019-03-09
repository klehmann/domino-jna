package com.mindoo.domino.jna.utils;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.constants.ClusterLookup;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

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
	 * {@link #getServerClusterMates} uses the Address book specified by the user's location record.<br>
	 * Unless cascading Address books or Directory Assistance is enabled, the Notes mail
	 * domain field in the user's location record must be set to the domain name for the
	 * server(s) in the cluster and the Home/mail server field must be set to a server in this domain.<br>
	 * <br>
	 * If the target server is in a different domain than specified in the user's location record
	 * then in order for {@link #getServerClusterMates} to succeed, you must have cascading Address
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
}
