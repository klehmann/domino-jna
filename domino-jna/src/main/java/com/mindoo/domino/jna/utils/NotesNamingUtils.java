package com.mindoo.domino.jna.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindoo.domino.jna.NotesNamesList;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.LMBCSStringArray;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.LinuxNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.MacNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.NotesNamesListHeader32Struct;
import com.mindoo.domino.jna.internal.structs.WinNotesNamesListHeader32Struct;
import com.mindoo.domino.jna.internal.structs.WinNotesNamesListHeader64Struct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Notes name utilities to convert between various Name formats and evaluate
 * user groups on a server.
 * 
 * @author Karsten Lehmann
 */
public class NotesNamingUtils {
	private static final int MAX_STRINGCACHE_SIZE = 500;
	
	private static Map<String, String> m_nameAbbrCache = Collections.synchronizedMap(new LinkedHashMap<String, String>(16,0.75f, true) {
		private static final long serialVersionUID = -5818239831757810895L;

		@Override
		protected boolean removeEldestEntry (Map.Entry<String,String> eldest) {
			if (size() > MAX_STRINGCACHE_SIZE) {
				return true;
			}
			else {
				return false;
			}
		}
	});
	private static Map<String, String> m_nameCanonicalCache = Collections.synchronizedMap(new LinkedHashMap<String, String>(16,0.75f, true) {
		private static final long serialVersionUID = -5818239831757810895L;

		@Override
		protected boolean removeEldestEntry (Map.Entry<String,String> eldest) {
			if (size() > MAX_STRINGCACHE_SIZE) {
				return true;
			}
			else {
				return false;
			}
		}
	});
	
	/**
	 * This function converts a distinguished name in abbreviated format to canonical format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @return canonical name
	 */
	public static String toCanonicalName(String name) {
		return toCanonicalName(name, null);
	}
	
	/**
	 * This function converts a distinguished name in abbreviated format to canonical format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @param templateName name to be used when the input name is in common name format
	 * @return canonical name
	 */
	public static String toCanonicalName(String name, String templateName) {
		if (name==null)
			return null;
		if (name.length()==0)
			return name;

		String cacheKey = name + ((templateName!=null && templateName.length()>0) ? ("|" + templateName) : "");
		String abbrName = m_nameCanonicalCache.get(cacheKey);
		if (abbrName!=null) {
			return abbrName;
		}

		Memory templateNameMem = templateName==null ? null : NotesStringUtils.toLMBCS(templateName, true); //used when abbrName is only a common name
		Memory inNameMem = NotesStringUtils.toLMBCS(name, true);
		DisposableMemory outNameMem = new DisposableMemory(NotesConstants.MAXUSERNAME);
		ShortByReference outLength = new ShortByReference();
		
		short result = NotesNativeAPI.get().DNCanonicalize(0, templateNameMem, inNameMem, outNameMem, NotesConstants.MAXUSERNAME, outLength);
		NotesErrorUtils.checkResult(result);
		
		String sOutName = NotesStringUtils.fromLMBCS(outNameMem, (int) (outLength.getValue() & 0xffff));
		outNameMem.dispose();
		
		m_nameCanonicalCache.put(cacheKey, sOutName);
		
		return sOutName;
	}
	
	/**
	 * This function converts a distinguished name in canonical format to abbreviated format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @return abbreviated name
	 */
	public static String toAbbreviatedName(String name) {
		if (name==null)
			return null;
		if (name.length()==0)
			return name;
		
		final String cacheKey = name;
		String abbrName = m_nameAbbrCache.get(cacheKey);
		
		if (abbrName==null) {
			StringTokenizerExt st=new StringTokenizerExt(name, "/");
			StringBuilder sb=new StringBuilder(name.length());
			while (st.hasMoreTokens()) {
				String currToken=st.nextToken();
				int iPos = currToken.indexOf("=");
				if (sb.length()>0)
					sb.append("/");
				
				if (iPos!=-1) {
					sb.append(currToken.substring(iPos+1));
				}
				else {
					sb.append(currToken);
				}
			}
			
			abbrName = sb.toString();
			m_nameAbbrCache.put(cacheKey, abbrName);
		}

		return abbrName;
	}

	/**
	 * Method to compare two Notes names. We compare the abbreviated forms of both names
	 * ignoring the case
	 * 
	 * @param p_sNotesName1 Notes name 1
	 * @param p_sNotesName2 Notes name 2
	 * @return true if equal
	 */
	public static boolean equalNames(String p_sNotesName1, String p_sNotesName2) {
		String sNotesName1Abbr = toAbbreviatedName(p_sNotesName1);
		String sNotesName2Abbr = toAbbreviatedName(p_sNotesName2);
		
		if (sNotesName1Abbr==null) {
			return sNotesName2Abbr==null;
		}
		else {
			return sNotesName1Abbr.equalsIgnoreCase(sNotesName2Abbr);
		}
	}

	/**
	 * Extracts the common name part of an abbreviated or canonical name
	 * 
	 * @param name abbreviated or canonical name
	 * @return common name
	 */
	public static String toCommonName(String name) {
		int iPos = name.indexOf('/');
		String firstPart = iPos==-1 ? name : name.substring(0, iPos);
		if (StringUtil.startsWithIgnoreCase(firstPart, "cn=")) {
			return firstPart.substring(3);
		}
		else {
			return firstPart;
		}
	}
	
	/**
	 * Checks whether a Notes name matches a wildcard string, e.g. "Karsten Lehmann / Mindoo" would match
	 * "* / Mindoo".
	 * 
	 * @param name notes name (abbreviated or canonical)
	 * @param wildcard (abbreviated or canonical)
	 * @return true if match
	 */
	public static boolean nameMatchesWildcard(String name, String wildcard) {
		if ("*".equals(wildcard))
			return true;
		
		String nameAbbr = toAbbreviatedName(name);
		String wildcardAbbr = toAbbreviatedName(wildcard);
		
		ReverseStringTokenizer nameSt = new ReverseStringTokenizer(nameAbbr, "/");
		ReverseStringTokenizer wildcardSt = new ReverseStringTokenizer(wildcardAbbr, "/");
		
		while (nameSt.hasMoreTokens()) {
			String currNameToken = nameSt.nextToken();
			
			if (!wildcardSt.hasMoreTokens()) {
				return false;
			}
			else {
				String currWildcardToken = wildcardSt.nextToken();
				if ("*".equals(currWildcardToken)) {
					if (wildcardSt.hasMoreTokens())
						throw new IllegalArgumentException("The wildcard * can only be the leftmost part of the wildcard pattern");
					
					return true;
				}
				else if (!currNameToken.equalsIgnoreCase(currWildcardToken)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * This function converts a distinguished name in canonical format to abbreviated format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @param templateName name to be used when the input name is in common name format
	 * @return abbreviated name
	 */
	public static String toAbbreviatedName(String name, String templateName) {
		if (name==null)
			return null;
		if (name.length()==0)
			return name;
		
		String cacheKey = name + ((templateName!=null && templateName.length()>0) ? ("|" + templateName) : "");
		String abbrName = m_nameAbbrCache.get(cacheKey);
		if (abbrName!=null) {
			return abbrName;
		}
		
		Memory templateNameMem = templateName==null || templateName.length()==0 ? null : NotesStringUtils.toLMBCS(templateName, true); //used when abbrName is only a common name
		Memory inNameMem = NotesStringUtils.toLMBCS(name, true);
		DisposableMemory outNameMem = new DisposableMemory(NotesConstants.MAXUSERNAME);
		ShortByReference outLength = new ShortByReference();
		
		short result = NotesNativeAPI.get().DNAbbreviate(0, templateNameMem, inNameMem, outNameMem, NotesConstants.MAXUSERNAME, outLength);
		NotesErrorUtils.checkResult(result);
		
		String sOutName = NotesStringUtils.fromLMBCS(outNameMem, (int) (outLength.getValue() & 0xffff));
		outNameMem.dispose();
		
		m_nameAbbrCache.put(cacheKey, sOutName);
		
		return sOutName;
	}

	/**
	 * Writes the specified names list in null terminated LMBCS strings to a {@link ByteArrayOutputStream}
	 * 
	 * @param names names to write
	 * @param bOut target output stream
	 */
	private static void storeAsUserNamesList(List<String> names, ByteArrayOutputStream bOut) {
		//convert to canonical format
		List<String> namesCanonical = new ArrayList<String>(names.size());
		for (int i=0; i<names.size(); i++) {
			namesCanonical.add(toCanonicalName(names.get(i)));
		}
		
		for (int i=0; i<namesCanonical.size(); i++) {
			String currName = namesCanonical.get(i);
			Memory currNameLMBCS = NotesStringUtils.toLMBCS(currName, true);
			
			try {
				bOut.write(currNameLMBCS.getByteArray(0, (int) currNameLMBCS.size()));
			} catch (IOException e) {
				throw new NotesError(0, "Error writing to ByteArrayOutputStream");
			}
		}
	}
	
	/**
	 * Allocates memory in the Notes memory pool and writes a NAMES_LIST data structure with
	 * the specified names. The names are automatically converted to canonical format.
	 * 
	 * @param names names for names list, similar to result of @UserNamesList formula, e.g. usernames, wildcards, groups and roles; either abbreviated or canonical
	 * @return memory handle to NAMES_LIST
	 */
	private static int b32_writeUserNamesList(List<String> names) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		storeAsUserNamesList(names, bOut);
		
		if (PlatformUtils.is64Bit()) {
			throw new IllegalStateException("Only supported for 32 bit");
		}
		
		Memory namesListMem;
		if (PlatformUtils.isWindows()) {
			namesListMem = new Memory(NotesConstants.winNamesListHeaderSize32);
			WinNotesNamesListHeader32Struct namesListHeader = WinNotesNamesListHeader32Struct.newInstance(namesListMem);
			namesListHeader.NumNames = (short) (names.size() & 0xffff);
			namesListHeader.write();
		}
		else {
			namesListMem = new Memory(NotesConstants.namesListHeaderSize32);
			NotesNamesListHeader32Struct namesListHeader = NotesNamesListHeader32Struct.newInstance(namesListMem);
			namesListHeader.NumNames = (short) (names.size() & 0xffff);
			namesListHeader.write();
		}
		
		IntByReference retHandle = new IntByReference();
		short result = Mem32.OSMemAlloc((short) 0, NotesConstants.namesListHeaderSize32 + bOut.size(), retHandle);
		NotesErrorUtils.checkResult(result);
		
		final int retHandleAsInt = retHandle.getValue();
		
		//write the data
		Pointer ptr = Mem32.OSLockObject(retHandleAsInt);
		try {
			byte[] namesListByteArr = namesListMem.getByteArray(0, (int) namesListMem.size());
			ptr.write(0, namesListByteArr, 0, namesListByteArr.length);
			
			byte[] namesDataArr = bOut.toByteArray();
			ptr.write(namesListByteArr.length, namesDataArr, 0, namesDataArr.length);
		}
		finally {
			Mem32.OSUnlockObject(retHandleAsInt);
		}

		return retHandleAsInt;
	}

	/**
	 * Allocates memory in the Notes memory pool and writes a NAMES_LIST data structure with
	 * the specified names. The names are automatically converted to canonical format.
	 * 
	 * @param names names for names list, similar to result of @UserNamesList formula, e.g. usernames, wildcards, groups and roles; either abbreviated or canonical
	 * @return memory handle to NAMES_LIST
	 */
	private static long b64_writeUserNamesList(List<String> names) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		storeAsUserNamesList(names, bOut);
		
		if (!PlatformUtils.is64Bit()) {
			throw new IllegalStateException("Only supported for 64 bit");
		}
		
		Memory namesListMem;
		if (PlatformUtils.isWindows()) {
			namesListMem = new Memory(NotesConstants.winNamesListHeaderSize64);
			WinNotesNamesListHeader64Struct namesListHeader = WinNotesNamesListHeader64Struct.newInstance(namesListMem);
			namesListHeader.NumNames = (short) (names.size() & 0xffff);
			namesListHeader.write();
		}
		else if (PlatformUtils.isMac()) {
			namesListMem = new Memory(NotesConstants.macNamesListHeaderSize64);
			MacNotesNamesListHeader64Struct namesListHeader = MacNotesNamesListHeader64Struct.newInstance(namesListMem);
			namesListHeader.NumNames = (short) (names.size() & 0xffff);
			namesListHeader.write();
		}
		else {
			namesListMem = new Memory(NotesConstants.linuxNamesListHeaderSize64);
			LinuxNotesNamesListHeader64Struct namesListHeader = LinuxNotesNamesListHeader64Struct.newInstance(namesListMem);
			namesListHeader.NumNames = (short) (names.size() & 0xffff);
			namesListHeader.write();
		}

		LongByReference retHandle = new LongByReference();
		short result = Mem64.OSMemAlloc((short) 0, (int) namesListMem.size() + bOut.size(), retHandle);
		NotesErrorUtils.checkResult(result);
		
		long retHandleAsLong = retHandle.getValue();
		
		//write the data
		Pointer ptr = Mem64.OSLockObject(retHandleAsLong);
		try {
			byte[] namesListByteArr = namesListMem.getByteArray(0, (int) namesListMem.size());
			ptr.write(0, namesListByteArr, 0, namesListByteArr.length);
			
			byte[] namesDataArr = bOut.toByteArray();
			ptr.write(namesListByteArr.length, namesDataArr, 0, namesDataArr.length);
		}
		finally {
			Mem64.OSUnlockObject(retHandleAsLong);
		}
		
		return retHandleAsLong;
	}

	/**
	 * Programatically creates a {@link NotesNamesList}
	 * 
	 * @param names names for names list, similar to result of @UserNamesList formula, e.g. usernames, wildcards, groups and roles; either abbreviated or canonical
	 * @return names list
	 */
	public static NotesNamesList writeNewNamesList(List<String> names) {
		if (PlatformUtils.is64Bit()) {
			long handle64 = b64_writeUserNamesList(names);
			NotesNamesList namesList = new NotesNamesList(handle64);
			NotesGC.__memoryAllocated(namesList);
			return namesList;
		}
		else {
			int handle32 = b32_writeUserNamesList(names);
			NotesNamesList namesList = new NotesNamesList(handle32);
			NotesGC.__memoryAllocated(namesList);
			return namesList;
		}
	}
	

	/**
	 * Computes a {@link NotesNamesList} structure with all name variants, wildcards and groups for
	 * the specified user
	 * 
	 * @param server name of server, either abbreviated or canonical or null/empty string for local
	 * @param userName username, either abbreviated or canonical
	 * @return names list
	 */
	public static NotesNamesList buildNamesList(String server, String userName) {
		if (userName==null)
			throw new NullPointerException("Name cannot be null");
		
		//make sure that server and username are canonical
		userName = toCanonicalName(userName);
		server = toCanonicalName(server);
		
		Memory userNameLMBCS = NotesStringUtils.toLMBCS(userName, true);
		Memory serverNameLMBCS = NotesStringUtils.toLMBCS(server, true);
		
		boolean bDontLookupAlternateNames = false;
		short fDontLookupAlternateNames = (short) (bDontLookupAlternateNames ? 1 : 0);
		Pointer pLookupFlags = null;
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNamesList = new LongByReference();
			short result = NotesNativeAPI64.get().CreateNamesListFromSingleName(serverNameLMBCS,
					fDontLookupAlternateNames, pLookupFlags, userNameLMBCS, rethNamesList);
			NotesErrorUtils.checkResult(result);
			long hUserNamesList64 = rethNamesList.getValue();
			
			NotesNamesList newList =  new NotesNamesList(hUserNamesList64);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
		else {
			IntByReference rethNamesList = new IntByReference();
			short result = NotesNativeAPI32.get().CreateNamesListFromSingleName(serverNameLMBCS,
					fDontLookupAlternateNames, pLookupFlags, userNameLMBCS, rethNamesList);
			NotesErrorUtils.checkResult(result);
			int hUserNamesList32 = rethNamesList.getValue();
			
			NotesNamesList newList = new NotesNamesList(hUserNamesList32);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
	}
	
	/**
	 * Expand one or more target names (e.g., might contain an alternate name) into a list of
	 * names (including any groups the target names belong to) by any given server name.
	 * 
	 * @param server name of server, either abbreviated or canonical or null/empty string for local
	 * @param names names to expand
	 * @return names list
	 */
	public static NotesNamesList createNamesListFromNames(String server, String[] names) {
		server = toCanonicalName(server);
		
		Memory ptrArrMem = new Memory(Pointer.SIZE * names.length);
		Memory[] namesMem = new Memory[names.length];
		
		for (int i=0; i<names.length; i++) {
			namesMem[i] = NotesStringUtils.toLMBCS(names[i], true);
			ptrArrMem.setPointer(i, namesMem[i]);
		}
		
		Memory serverNameLMBCS = NotesStringUtils.toLMBCS(server, true);
		
		PointerByReference ptrRef = new PointerByReference();
		ptrRef.setValue(ptrArrMem);
		
		LMBCSStringArray sArr = new LMBCSStringArray(names);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNamesList = new LongByReference();
			short result = NotesNativeAPI64.get().CreateNamesListFromNamesExtend(serverNameLMBCS, (short) (names.length & 0xffff), sArr, rethNamesList);
			NotesErrorUtils.checkResult(result);
			long hUserNamesList64 = rethNamesList.getValue();
			
			NotesNamesList newList =  new NotesNamesList(hUserNamesList64);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
		else {
			IntByReference rethNamesList = new IntByReference();
			short result = NotesNativeAPI32.get().CreateNamesListFromNamesExtend(serverNameLMBCS, (short) (names.length & 0xffff), sArr, rethNamesList);
			NotesErrorUtils.checkResult(result);
			int hUserNamesList32 = rethNamesList.getValue();
			
			NotesNamesList newList = new NotesNamesList(hUserNamesList32);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
	}
	
	/**
	 * Expand a target group/subtree name into a list of names (including any groups
	 * or subtrees the target names belong to) by any given server name
	 * 
	 * @param server name of server, either abbreviated or canonical or null/empty string for local
	 * @param group group name
	 * @return names list
	 */
	public static NotesNamesList createNamesListFromGroupName(String server, String group) {
		server = toCanonicalName(server);
		
		Memory groupLMBCS = NotesStringUtils.toLMBCS(group, true);
		Memory serverNameLMBCS = NotesStringUtils.toLMBCS(server, true);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNamesList = new LongByReference();
			short result = NotesNativeAPI64.get().CreateNamesListFromGroupNameExtend(serverNameLMBCS, groupLMBCS, rethNamesList);
			NotesErrorUtils.checkResult(result);
			long hUserNamesList64 = rethNamesList.getValue();
			
			NotesNamesList newList =  new NotesNamesList(hUserNamesList64);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
		else {
			IntByReference rethNamesList = new IntByReference();
			short result = NotesNativeAPI32.get().CreateNamesListFromGroupNameExtend(serverNameLMBCS, groupLMBCS, rethNamesList);
			NotesErrorUtils.checkResult(result);
			int hUserNamesList32 = rethNamesList.getValue();
			
			NotesNamesList newList = new NotesNamesList(hUserNamesList32);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
	}
	
	/**
	 * Computes a {@link NotesNamesList} structure with all name variants, wildcards and groups for
	 * the specified user on a remote server
	 * 
	 * @param userName username, either abbreviated or canonical
	 * @return names list
	 */
	public static NotesNamesList buildNamesList(String userName) {
		if (userName==null)
			throw new NullPointerException("Name cannot be null");
		
		//make sure that username is canonical
		userName = toCanonicalName(userName);
		
		Memory userNameLMBCS = NotesStringUtils.toLMBCS(userName, true);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNamesList = new LongByReference();
			short result = NotesNativeAPI64.get().NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
			NotesErrorUtils.checkResult(result);
			long hUserNamesList64 = rethNamesList.getValue();
			
			NotesNamesList newList =  new NotesNamesList(hUserNamesList64);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
		else {
			IntByReference rethNamesList = new IntByReference();
			short result = NotesNativeAPI32.get().NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
			NotesErrorUtils.checkResult(result);
			int hUserNamesList32 = rethNamesList.getValue();
			
			NotesNamesList newList = new NotesNamesList(hUserNamesList32);
			NotesGC.__memoryAllocated(newList);
			return newList;
		}
	}
	
	/**
	 * Computes the usernames list for the specified user, which is his name, name wildcards
	 * and all his groups and nested groups
	 * 
	 * @param server name of server, either abbreviated or canonical or null/empty string for local
	 * @param userName username in canonical format
	 * @return usernames list
	 */
	public static List<String> getUserNamesList(String server, String userName) {
		NotesNamesList namesList = buildNamesList(server, userName);
		List<String> names = namesList.getNames();
		namesList.free();
		
		return names;
	}

	/**
	 * Computes the usernames list for the specified user, which is his name, name wildcards
	 * and all his groups and nested groups
	 * 
	 * @param userName username in canonical format
	 * @return usernames list
	 */
	public static List<String> getUserNamesList(String userName) {
		NotesNamesList namesList = buildNamesList(userName);
		List<String> names = namesList.getNames();
		namesList.free();
		
		return names;
	}
	
	/**
	 * Enum of available user privileges
	 * 
	 * @author Karsten Lehmann
	 */
	public static enum Privileges {

		/** Set if names list has been authenticated via Notes (e.g. user is allowed to open a database) */
		Authenticated(0x0001),

		/**	Set if names list has been authenticated using external password -- Triggers "maximum password access allowed" feature */
		PasswordAuthenticated(0x0002),

		/**	Set if user requested full admin access and it was granted */
		FullAdminAccess(0x0004);

		private int m_flag;

		Privileges(int flag) {
			m_flag = flag;
		}

		public int getValue() {
			return m_flag;
		}
	};
	
	/**
	 * Internal helper function that modifies the Authenticated flag of a {@link NotesNamesList}
	 * in order to grant access to certain C API functionality (e.g. when opening a database).
	 * 
	 * @param namesList names list
	 * @param privileges new privileges
	 */
	public static void setPrivileges(NotesNamesList namesList, EnumSet<Privileges> privileges) {
		int bitMask = 0;
		for (Privileges currPrivilege : Privileges.values()) {
			if (privileges.contains(currPrivilege)) {
				bitMask = bitMask | currPrivilege.getValue();
			}
		}
		
		short bitMaskAsShort = (short) (bitMask & 0xffff);

		/*Use different header implementations based on architecture, because we have
		//different alignments and data types:

		typedef struct {
			WORD		NumNames;
			LICENSEID	License;
											

			#if defined(UNIX) || defined(OS2_2x) || defined(W32)
			DWORD		Authenticated;
			#else							
			WORD		Authenticated;
			#endif
			} NAMES_LIST;
		*/
		
		if (PlatformUtils.is64Bit()) {
			Pointer namesListBufferPtr = Mem64.OSLockObject(namesList.getHandle64());
			
			try {
				if (PlatformUtils.isWindows()) {
					WinNotesNamesListHeader64Struct namesListHeader = WinNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					namesListHeader.Authenticated = bitMask;
					namesListHeader.write();
					namesListHeader.read();
				}
				else if (PlatformUtils.isMac()) {
					MacNotesNamesListHeader64Struct namesListHeader = MacNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();

					namesListHeader.Authenticated = bitMaskAsShort;
					namesListHeader.write();
					namesListHeader.read();
				}
				else {
					LinuxNotesNamesListHeader64Struct namesListHeader = LinuxNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					
					//setting authenticated flag for the user is required when running on the server
					namesListHeader.Authenticated = bitMask;
					namesListHeader.write();
					namesListHeader.read();
				}
			}
			finally {
				Mem64.OSUnlockObject(namesList.getHandle64());
			}
		}
		else {
			Pointer namesListBufferPtr = Mem32.OSLockObject(namesList.getHandle32());
			
			try {
				//setting authenticated flag for the user is required when running on the server
				if (PlatformUtils.isWindows()) {
					WinNotesNamesListHeader32Struct namesListHeader = WinNotesNamesListHeader32Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					namesListHeader.Authenticated = bitMask;
					namesListHeader.write();
					namesListHeader.read();
				}
				else {
					NotesNamesListHeader32Struct namesListHeader = NotesNamesListHeader32Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					namesListHeader.Authenticated = bitMask;
					namesListHeader.write();
					namesListHeader.read();
				}
			}
			finally {
				Mem32.OSUnlockObject(namesList.getHandle32());
			}
		}
	}
	
	/**
	 * Reads which privileges have been set in the names list by method {@link #setPrivileges(NotesNamesList, EnumSet)}
	 * 
	 * @param namesList names list
	 * @return privileges
	 */
	public static EnumSet<Privileges> getPrivileges(NotesNamesList namesList) {
		/*Use different header implementations based on architecture, because we have
		//different alignments and data types:

		typedef struct {
			WORD		NumNames;
			LICENSEID	License;
											

			#if defined(UNIX) || defined(OS2_2x) || defined(W32)
			DWORD		Authenticated;
			#else							
			WORD		Authenticated;
			#endif
			} NAMES_LIST;
		*/
		
		int authenticated;
		
		if (PlatformUtils.is64Bit()) {
			Pointer namesListBufferPtr = Mem64.OSLockObject(namesList.getHandle64());
			
			try {
				if (PlatformUtils.isWindows()) {
					WinNotesNamesListHeader64Struct namesListHeader = WinNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					authenticated = namesListHeader.Authenticated;
				}
				else if (PlatformUtils.isMac()) {
					MacNotesNamesListHeader64Struct namesListHeader = MacNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();

					authenticated = namesListHeader.Authenticated;
				}
				else {
					LinuxNotesNamesListHeader64Struct namesListHeader = LinuxNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					
					//setting authenticated flag for the user is required when running on the server
					authenticated = namesListHeader.Authenticated;
				}
			}
			finally {
				Mem64.OSUnlockObject(namesList.getHandle64());
			}
		}
		else {
			Pointer namesListBufferPtr = Mem32.OSLockObject(namesList.getHandle32());
			
			try {
				//setting authenticated flag for the user is required when running on the server
				if (PlatformUtils.isWindows()) {
					WinNotesNamesListHeader32Struct namesListHeader = WinNotesNamesListHeader32Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					authenticated = namesListHeader.Authenticated;
				}
				else {
					NotesNamesListHeader32Struct namesListHeader = NotesNamesListHeader32Struct.newInstance(namesListBufferPtr);
					namesListHeader.read();
					authenticated = namesListHeader.Authenticated;
				}
			}
			finally {
				Mem32.OSUnlockObject(namesList.getHandle32());
			}
		}
		
		EnumSet<Privileges> privileges = EnumSet.noneOf(Privileges.class);
		
		for (Privileges currPrivilege : Privileges.values()) {
			if ((authenticated & currPrivilege.getValue()) == currPrivilege.getValue()) {
				privileges.add(currPrivilege);
			}
		}
		return privileges;
	}
}
