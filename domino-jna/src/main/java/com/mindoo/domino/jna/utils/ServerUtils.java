package com.mindoo.domino.jna.utils;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.constants.ClusterLookup;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
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
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference phList = new LongByReference();
			
			result = notesAPI.b64_NSGetServerClusterMates(serverNameCanonical, lookupMode==null ? 0 : lookupMode.getValue(), phList);
			NotesErrorUtils.checkResult(result);
			
			long hList = phList.getValue();
			if (hList==0)
				return Collections.emptyList();
			
			Pointer pList = notesAPI.b64_OSLockObject(hList);
			try {
				@SuppressWarnings("rawtypes")
				List clusterMates = ItemDecoder.decodeTextListValue(notesAPI, pList, false);
				return clusterMates;
			}
			finally {
				notesAPI.b64_OSUnlockObject(hList);
				notesAPI.b64_OSMemFree(hList);
			}
		}
		else {
			IntByReference phList = new IntByReference();
			
			result = notesAPI.b32_NSGetServerClusterMates(serverNameCanonical, lookupMode==null ? 0 : lookupMode.getValue(), phList);
			NotesErrorUtils.checkResult(result);
			
			int hList = phList.getValue();
			if (hList==0)
				return Collections.emptyList();

			Pointer pList = notesAPI.b32_OSLockObject(hList);
			try {
				@SuppressWarnings("rawtypes")
				List clusterMates = ItemDecoder.decodeTextListValue(notesAPI, pList, false);
				return clusterMates;
			}
			finally {
				notesAPI.b32_OSUnlockObject(hList);
				notesAPI.b32_OSMemFree(hList);
			}
		}
	}
}
