package com.mindoo.domino.jna.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesCallbacks.STATTRAVERSEPROC;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks.STATTRAVERSEPROCWin32;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Utility class to read server statistics
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
	public static NotesStatistics retrieveStatistics() {
		return retrieveStatistics((String) null, (String) null);
	}

	/**
	 * This function reads statistics for the specified facility
	 * 
	 * @param facility facility, e.g. {@link #STATPKG_NSF}
	 * @return statistics
	 */
	public static NotesStatistics retrieveStatistics(String facility) {
		return retrieveStatistics(facility, (String) null);
	}

	/**
	 * This function reads a single statistic value for a facility
	 * 
	 * @param facility facility, e.g. {@link #STATPKG_NSF} or {@link #STATPKG_REPLICA}
	 * @param statName name of statistic
	 * @return statistics
	 */
	public static NotesStatistics retrieveStatistics(String facility, String statName) {
		Memory facilityMem = NotesStringUtils.toLMBCS(facility, true);
		Memory statNameMem = NotesStringUtils.toLMBCS(statName, true);

		final Map<String,Map<String,Object>> statsByFacility = new LinkedHashMap<String, Map<String,Object>>();

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

					Map<String,Object> statsForFacility = statsByFacility.get(facility);
					if (statsForFacility==null) {
						statsForFacility = new LinkedHashMap<String, Object>();
						statsByFacility.put(facility, statsForFacility);
					}
					statsForFacility.put(statName, decodedValue);

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

					Map<String,Object> statsForFacility = statsByFacility.get(facility);
					if (statsForFacility==null) {
						statsForFacility = new LinkedHashMap<String, Object>();
						statsByFacility.put(facility, statsForFacility);
					}
					statsForFacility.put(statName, decodedValue);

					return 0;
				}};
		}

		NotesNativeAPI.get().StatTraverse(facilityMem, statNameMem, callback, null);

		return new NotesStatistics(statsByFacility);
	}

	private Map<String,Map<String,Object>> m_data;
	private String m_toString;
	
	private NotesStatistics(Map<String,Map<String,Object>> data) {
		m_data = data;
	}

	public Iterator<String> getFacilityNames() {
		return m_data.keySet().iterator();
	}

	/**
	 * Reads all statistics of the specified facility
	 * 
	 * @param facility facility
	 * @return stats map or null if not read
	 */
	public Map<String,Object> getData(String facility) {
		Map<String,Object> facilityData = m_data.get(facility);
		return facilityData==null ? null : Collections.unmodifiableMap(facilityData);
	}

	/**
	 * Method to check if a stats value has been read for a facility
	 * 
	 * @param facility facility
	 * @param statName name of statistic
	 * @return true if value exists
	 */
	public boolean hasData(String facility, String statName) {
		Map<String,Object> facilityData = m_data.get(facility);
		if (facilityData!=null) {
			return facilityData.containsKey(statName);
		}
		return false;
	}

	/**
	 * Method to read a single stats value of a facility
	 * 
	 * @param facility facility
	 * @param statName name of statistic
	 * @return value or null if not read
	 */
	public Object getData(String facility, String statName) {
		Map<String,Object> facilityData = m_data.get(facility);
		if (facilityData!=null) {
			return facilityData.get(statName);
		}
		return null;
	}
	
	@Override
	public String toString() {
		if (m_toString==null) {
			StringBuilder sb = new StringBuilder();
			for (Entry<String,Map<String,Object>> currFacitityStatsEntry : m_data.entrySet()) {
				String currFacility = currFacitityStatsEntry.getKey();
				Map<String,Object> currFacilityStats = currFacitityStatsEntry.getValue();
				
				if (sb.length()>0) {
					sb.append("\n\n");
				}
				sb.append(currFacility).append("\n");
				sb.append(StringUtil.repeat('=', currFacility.length()));
				sb.append("\n\n");
				
				for (Entry<String,Object> currStatEntry : currFacilityStats.entrySet()) {
					String currStatName = currStatEntry.getKey();
					Object currStatValue = currStatEntry.getValue();
					
					sb.append(currStatName).append(":" ).append(currStatValue).append("\n");
				}
			}
			m_toString = sb.toString();
		}
		return m_toString;
	}
}
