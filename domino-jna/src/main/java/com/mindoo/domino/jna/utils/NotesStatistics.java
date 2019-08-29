package com.mindoo.domino.jna.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks.STATTRAVERSEPROC;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks.STATTRAVERSEPROCWin32;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Utility class to read client and server statistics
 * 
 * @author Karsten Lehmann
 */
public class NotesStatistics {
	//all available facilities to read stats:
	
	public static final String STATPKG_OS = "OS";
	public static final String STATPKG_STATS = "Stats";
	public static final String STATPKG_OSMEM = "Mem";
	public static final String STATPKG_OSSEM = "Sem";
	public static final String STATPKG_OSSPIN = "Spin";
	public static final String STATPKG_OSFILE = "Disk";
	public static final String STATPKG_SERVER = "Server";
	public static final String STATPKG_REPLICA = "Replica";
	public static final String STATPKG_MAIL = "Mail";
	public static final String STATPKG_MAILBYDEST = "MailByDest";
	public static final String STATPKG_COMM = "Comm";
	public static final String STATPKG_NSF = "Database";
	public static final String STATPKG_NIF = "Database";
	public static final String STATPKG_TESTNSF = "Testnsf";
	public static final String STATPKG_OSIO = "IO";
	public static final String STATPKG_NET = "NET";
	public static final String STATPKG_OBJSTORE = "Object";
	/** used by agent manager */
	public static final String STATPKG_AGENT = "Agent";
	/** used by Web retriever */
	public static final String STATPKG_WEB = "Web";
	/** used by schedule manager */
	public static final String STATPKG_CAL = "Calendar";
	/** Used by SMTP listener */
	public static final String STATPKG_SMTP = "SMTP";
	/** Used by the LDAP Server */
	public static final String STATPKG_LDAP = "LDAP";
	/** Used by the NNTP Server */
	public static final String STATPKG_NNTP = "NNTP";
	/** Used by the ICM Server */
	public static final String STATPKG_ICM = "ICM";
	/** Used by Administration Process */
	public static final String STATPKG_ADMINP = "ADMINP";
	/** used by IMAP Server    */
	public static final String STATPKG_IMAP = "IMAP";
	public static final String STATPKG_MONITOR = "Monitor";
	/** Used by the POP3 Server */
	public static final String STATPKG_POP3 = "POP3";
	public static final String STATPKG_FT = "FT";
	/** Used by the DECS Server */
	public static final String STATPKG_DECS = "DECS";
	/** Used by the Event Monitor */
	public static final String STATPKG_EVENT = "EVENT";
	/** Used by DB2 NSF */
	public static final String STATPKG_DB2NSF = "DB2NSF";
	/** Used by DB2 NIF */
	public static final String STATPKG_DB2NIF = "DB2NIF";	
	public static final String STATPKG_FA = "FaultAnalyzer";
	public static final String STATPKG_DAOS = "DAOS";	
	public static final String STATPKG_LOTUSSCRIPT = "LotusScript";
	/** Used by DBMaintTool (in compact dir) */
	public static final String STATPKG_DBMT = "DBMT";

	public List<String> ALL_FACILITIES = Arrays.asList(
			STATPKG_OS,
			STATPKG_STATS,
			STATPKG_OSMEM,
			STATPKG_OSSEM,
			STATPKG_OSSPIN,
			STATPKG_OSFILE,
			STATPKG_SERVER,
			STATPKG_REPLICA,
			STATPKG_MAIL,
			STATPKG_MAILBYDEST,
			STATPKG_COMM,
			STATPKG_NSF,
			STATPKG_NIF,
			STATPKG_TESTNSF,
			STATPKG_OSIO,
			STATPKG_NET,
			STATPKG_OBJSTORE,
			STATPKG_AGENT,
			STATPKG_WEB,
			STATPKG_CAL,
			STATPKG_SMTP,
			STATPKG_LDAP,
			STATPKG_NNTP,
			STATPKG_ICM,
			STATPKG_ADMINP,
			STATPKG_IMAP,
			STATPKG_MONITOR,
			STATPKG_POP3,
			STATPKG_FT,
			STATPKG_DECS,
			STATPKG_EVENT,
			STATPKG_DB2NSF,
			STATPKG_DB2NIF,
			STATPKG_FA,
			STATPKG_DAOS,
			STATPKG_LOTUSSCRIPT,
			STATPKG_DBMT
			);
	
	/**
	 * This function reads statistics for all facilities
	 * 
	 * @return statistics
	 */
	public static NotesStatistics retrieveLocalStatistics() {
		return retrieveLocalStatistics((String) null, (String) null);
	}

	/**
	 * This function reads statistics for the specified facility
	 * 
	 * @param facility facility, e.g. {@link #STATPKG_NSF} or null for all
	 * @return statistics
	 */
	public static NotesStatistics retrieveLocalStatistics(String facility) {
		return retrieveLocalStatistics(facility, (String) null);
	}

	/**
	 * Request the specified information from the named server.<br>
	 * <br>
	 * Statistics are identified by a Facility name (see {@link #STATPKG_OS} etc. for Domino facilities) and a statistic Name.<br>
	 * 
	 * @param server server
	 * @param facility facility, e.g. {@link #STATPKG_NSF} or null for all
	 * @return statistics formatted as string
	 */
	public static String retrieveRemoteStatisticsAsString(String server) {
		return retrieveRemoteStatisticsAsString(server, (String) null, (String) null);
	}
	
	/**
	 * Request the specified information from the named server.<br>
	 * <br>
	 * Statistics are identified by a Facility name (see {@link #STATPKG_OS} etc. for Domino facilities) and a statistic Name.<br>
	 * 
	 * @param server server
	 * @param facility facility, e.g. {@link #STATPKG_NSF} or null for all
	 * @return statistics formatted as string
	 */
	public static String retrieveRemoteStatisticsAsString(String server, String facility) {
		return retrieveRemoteStatisticsAsString(server, (String) null, (String) null);
	}
	
	/**
	 * Request the specified information from the named server.<br>
	 * <br>
	 * Statistics are identified by a Facility name (see {@link #STATPKG_OS} etc. for Domino facilities) and a statistic Name.<br>
	 * 
	 * @param server server
	 * @param facility facility, e.g. {@link #STATPKG_NSF} or null for all
	 * @param statName name of statistic or null for all
	 * @return statistics formatted as string
	 */
	public static String retrieveRemoteStatisticsAsString(String server, String facility, String statName) {
		String serverCanonical = NotesNamingUtils.toCanonicalName(server);
		
		Memory serverCanonicalMem = NotesStringUtils.toLMBCS(serverCanonical, true);
		Memory facilityMem = NotesStringUtils.toLMBCS(facility, true);
		Memory statNameMem = NotesStringUtils.toLMBCS(statName, true);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			IntByReference retTableSize = new IntByReference();
			
			short result = NotesNativeAPI64.get().NSFGetServerStats(serverCanonicalMem, facilityMem, statNameMem, rethTable, retTableSize);
			NotesErrorUtils.checkResult(result);
			
			long hTable = rethTable.getValue();
			if (hTable==0) {
				return "";
			}
			int tableSize = retTableSize.getValue();
			
			Pointer ptrTable = Mem64.OSLockObject(hTable);
			try {
				String statsStr = NotesStringUtils.fromLMBCS(ptrTable, tableSize);
				return statsStr;
			}
			finally {
				Mem64.OSUnlockObject(hTable);
				Mem64.OSMemFree(hTable);
			}
		}
		else {
			IntByReference rethTable = new IntByReference();
			IntByReference retTableSize = new IntByReference();
			
			short result = NotesNativeAPI32.get().NSFGetServerStats(statNameMem, facilityMem, statNameMem, rethTable, retTableSize);
			NotesErrorUtils.checkResult(result);
			
			int hTable = rethTable.getValue();
			if (hTable==0) {
				return "";
			}
			int tableSize = retTableSize.getValue();
			
			Pointer ptrTable = Mem32.OSMemoryLock(hTable);
			try {
				String statsStr = NotesStringUtils.fromLMBCS(ptrTable, tableSize);
				return statsStr;
			}
			finally {
				Mem32.OSMemoryUnlock(hTable);
				Mem32.OSMemFree(hTable);
			}
		}
	}

	/**
	 * This function reads statistic values for one or all facilities
	 * 
	 * @param facility facility, e.g. {@link #STATPKG_NSF} or {@link #STATPKG_REPLICA} or null for all
	 * @param statName name of statistic or null for all
	 * @return statistics
	 */
	public static NotesStatistics retrieveLocalStatistics(String facility, String statName) {
		Memory facilityMem = NotesStringUtils.toLMBCS(facility, true);
		Memory statNameMem = NotesStringUtils.toLMBCS(statName, true);

		final Map<String,List<Pair<String,Object>>> statsByFacility = new LinkedHashMap<String, List<Pair<String,Object>>>();

		STATTRAVERSEPROC callback;
		if (PlatformUtils.isWin32()) {
			callback = new STATTRAVERSEPROCWin32() {

				@Override
				public short invoke(Pointer ctx, Pointer facilityMem, Pointer statNameMem, short valueType, Pointer valueMem) {
					int valueTypeAsInt = (int) (valueType & 0xffff);
					
					String facility = NotesStringUtils.fromLMBCS(facilityMem, -1);
					if (facility==null) {
						//should not be returned
						facility = "null";
					}

					String statName = NotesStringUtils.fromLMBCS(statNameMem, -1);
					if (statName==null) {
						//should not be returned
						statName = "null";
					}

					Object decodedValue;

					if (valueMem==null) {
						decodedValue = null;
					}
					else {
						if (valueTypeAsInt==1 /* NotesConstants.VT_TEXT */) {
							decodedValue = NotesStringUtils.fromLMBCS(valueMem, -1);
						}
						else if (valueTypeAsInt==2 /* NotesConstants.VT_TIMEDATE */) {
							NotesTimeDateStruct tdStruct = NotesTimeDateStruct.newInstance(valueMem);
							tdStruct.read();
							decodedValue = new NotesTimeDate(tdStruct.Innards);
						}
						else if (valueTypeAsInt==0 /* NotesConstants.VT_LONG */) {
							decodedValue = valueMem.getInt(0);
						}
						else if (valueTypeAsInt==3 /* NotesConstants.VT_NUMBER */) {
							decodedValue = valueMem.getDouble(0);
						}
						else {
							//unknown type
							decodedValue = null;
						}
					}

					List<Pair<String,Object>> statsForFacility = statsByFacility.get(facility);
					if (statsForFacility==null) {
						statsForFacility = new ArrayList<Pair<String,Object>>();
						statsByFacility.put(facility, statsForFacility);
					}
					statsForFacility.add(new Pair<String,Object>(statName, decodedValue));

					return 0;
				}
			};
		}
		else {
			callback = new STATTRAVERSEPROC() {

				@Override
				public short invoke(Pointer ctx, Pointer facilityMem, Pointer statNameMem, short valueType, Pointer valueMem) {
					int valueTypeAsInt = (int) (valueType & 0xffff);
					
					String facility = NotesStringUtils.fromLMBCS(facilityMem, -1);
					if (facility==null) {
						//should not be returned
						facility = "null";
					}

					String statName = NotesStringUtils.fromLMBCS(statNameMem, -1);
					if (statName==null) {
						//should not be returned
						statName = "null";
					}

					Object decodedValue;

					if (valueMem==null) {
						decodedValue = null;
					}
					else {
						if (valueTypeAsInt==1 /* NotesConstants.VT_TEXT */) {
							decodedValue = NotesStringUtils.fromLMBCS(valueMem, -1);
						}
						else if (valueTypeAsInt==2 /* NotesConstants.VT_TIMEDATE */) {
							NotesTimeDateStruct tdStruct = NotesTimeDateStruct.newInstance(valueMem);
							tdStruct.read();
							decodedValue = new NotesTimeDate(tdStruct.Innards);
						}
						else if (valueTypeAsInt==0 /* NotesConstants.VT_LONG */) {
							decodedValue = valueMem.getInt(0);
						}
						else if (valueTypeAsInt==3 /* NotesConstants.VT_NUMBER */) {
							decodedValue = valueMem.getDouble(0);
						}
						else {
							//unknown type
							decodedValue = null;
						}
					}

					List<Pair<String,Object>> statsForFacility = statsByFacility.get(facility);
					if (statsForFacility==null) {
						statsForFacility = new ArrayList<Pair<String,Object>>();
						statsByFacility.put(facility, statsForFacility);
					}
					statsForFacility.add(new Pair<String,Object>(statName, decodedValue));

					return 0;
				}};
		}

		NotesNativeAPI.get().StatTraverse(facilityMem, statNameMem, callback, null);

		return new NotesStatistics("", statsByFacility);
	}

	private String m_server;
	private Map<String,List<Pair<String,Object>>> m_data;
	private String m_toString;
	
	private NotesStatistics(String server, Map<String,List<Pair<String,Object>>> data) {
		m_server = server;
		m_data = data;
	}

	public String getServer() {
		return m_server;
	}
	
	public Iterator<String> getFacilityNames() {
		return m_data.keySet().iterator();
	}

	/**
	 * Returns all statistics for the specified facility
	 * 
	 * @param facility facility
	 * @return list of statistics
	 */
	public Iterable<Pair<String,Object>> statsForFacility(String facility) {
		return statsForFacility(facility, (String) null);
	}
	
	/**
	 * Returns all statistics for the specified facility and stat name
	 * 
	 * @param facility facility
	 * @param statName stat name
	 * @return list of statistics
	 */
	public Iterable<Pair<String,Object>> statsForFacility(String facility, String statName) {
		List<Pair<String,Object>> stats = m_data.get(facility);
		if (stats!=null) {
			if (statName==null) {
				return stats;
			}
			else {
				List<Pair<String,Object>> filteredStats = new ArrayList<Pair<String,Object>>();
				for (Pair<String,Object> currEntry : stats) {
					if (statName.equals(currEntry.getValue1())) {
						filteredStats.add(currEntry);
					}
				}
				return filteredStats;
			}
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Method to read a single stats value of a facility
	 * 
	 * @param facility facility
	 * @param statName name of statistic
	 * @return value or null if not read
	 */
	public Object getFirstStatForFacility(String facility, String statName) {
		List<Pair<String,Object>> stats = m_data.get(facility);
		if (stats!=null && !stats.isEmpty()) {
			return stats.get(0).getValue2();
		}
		return null;
	}
	
	@Override
	public String toString() {
		if (m_toString==null) {
			StringBuilder sb = new StringBuilder();
			for (Entry<String,List<Pair<String,Object>>> currFacitityStatsEntry : m_data.entrySet()) {
				String currFacility = currFacitityStatsEntry.getKey();
				List<Pair<String,Object>> currFacilityStats = currFacitityStatsEntry.getValue();
				
				for (Pair<String,Object> currStatEntry : currFacilityStats) {
					String currStatName = currStatEntry.getValue1();
					Object currStatValue = currStatEntry.getValue2();
					
					sb.append(currFacility).append(".").append(currStatName).append("\t");
					if (currStatValue instanceof Double) {
						String dblAsStr = String.format("%.12f",((Double)currStatValue));
						while (dblAsStr.endsWith("0")) {
							dblAsStr = dblAsStr.substring(0,dblAsStr.length()-1);
						}
						if (dblAsStr.endsWith(",")) {
							dblAsStr = dblAsStr.substring(0, dblAsStr.length()-1);
						}
						if (dblAsStr.endsWith(".")) {
							dblAsStr = dblAsStr.substring(0, dblAsStr.length()-1);
						}
						sb.append(dblAsStr);
					}
					else {
						sb.append(currStatValue);
					}
					sb.append("\n");
				}
			}
			m_toString = sb.toString();
		}
		return m_toString;
	}
}
