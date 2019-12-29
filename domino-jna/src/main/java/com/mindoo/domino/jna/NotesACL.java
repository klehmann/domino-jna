package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks.ACLENTRYENUMFUNC;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Access control list of a {@link NotesDatabase}
 * 
 * @author Karsten Lehmann
 */
public class NotesACL implements IAllocatedMemory {
	private NotesDatabase m_parentDb;
	private long m_hACL64;
	private int m_hACL32;
	
	NotesACL(NotesDatabase parentDb, long hACL) {
		if (!PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_parentDb = parentDb;
		m_hACL64 = hACL;
	}
	
	NotesACL(NotesDatabase parentDb, int hACL) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_parentDb = parentDb;
		m_hACL32 = hACL;
	}
	
	public NotesDatabase getParentDatabase() {
		return m_parentDb;
	}
	
	@Override
	public void free() {
		if (isFreed())
			return;

		if (PlatformUtils.is64Bit()) {
			NotesGC.__memoryBeeingFreed(this);
			short result = Mem64.OSMemFree(m_hACL64);
			NotesErrorUtils.checkResult(result);
			m_hACL64=0;
		}
		else {
			NotesGC.__memoryBeeingFreed(this);
			short result = Mem32.OSMemFree(m_hACL32);
			NotesErrorUtils.checkResult(result);
			m_hACL32=0;
		}
	}

	@Override
	public boolean isFreed() {
		if (PlatformUtils.is64Bit()) {
			return m_hACL64 == 0;
		}
		else {
			return m_hACL32 == 0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hACL32;
	}

	@Override
	public long getHandle64() {
		return m_hACL64;
	}

	/**
	 * Change the name of the administration server for the access control list.
	 * 
	 * @param server server, either in abbreviated or canonical format
	 */
	public void setAdminServer(String server) {
		checkHandle();
		
		Memory serverCanonicalMem = NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(server), true);
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().ACLSetAdminServer(m_hACL64, serverCanonicalMem);
		}
		else {
			result = NotesNativeAPI32.get().ACLSetAdminServer(m_hACL32, serverCanonicalMem);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function stores the access control list in the parent database.<br>
	 */
	public void save() {
		checkHandle();
		if (m_parentDb.isRecycled())
			throw new NotesError(0, "Parent database already recycled");
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbStoreACL(m_parentDb.getHandle64(), m_hACL64, 0, (short) 0);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbStoreACL(m_parentDb.getHandle32(), m_hACL32, 0, (short) 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Checks if the database is already recycled
	 */
	private void checkHandle() {
		if (PlatformUtils.is64Bit()) {
			if (m_hACL64==0)
				throw new NotesError(0, "Memory already freed");
			NotesGC.__b64_checkValidMemHandle(getClass(), m_hACL64);
		}
		else {
			if (m_hACL32==0)
				throw new NotesError(0, "Memory already freed");
			NotesGC.__b32_checkValidMemHandle(getClass(), m_hACL32);
		}
	}

	/**
	 * Looks up the access level for a user and his groups
	 * 
	 * @param userName username, either canonical or abbreviated
	 * @return acl access info, with access level, flags and roles
	 */
	public NotesACLAccess lookupAccess(String userName) {
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(m_parentDb.getServer(), userName);
		try {
			return lookupAccess(namesList);
		}
		finally {
			namesList.free();
		}
	}
	
	/**
	 * Looks up the access level for a {@link NotesNamesList}
	 * 
	 * @param namesList names list for a user
	 * @return acl access info, with access level, flags and roles
	 */
	public NotesACLAccess lookupAccess(NotesNamesList namesList) {
		if (namesList.isFreed())
			throw new NotesError(0, "Nameslist is already freed");
		
		ShortByReference retAccessLevel = new ShortByReference();
		Memory retPrivileges = new Memory(10);
		ShortByReference retAccessFlags = new ShortByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethPrivNames = new LongByReference();
			
			long hNamesList = namesList.getHandle64();
			Pointer pNamesList = Mem64.OSLockObject(hNamesList);
			try {
				result = NotesNativeAPI64.get().ACLLookupAccess(m_hACL64, pNamesList, retAccessLevel,
						retPrivileges, retAccessFlags, rethPrivNames);
				NotesErrorUtils.checkResult(result);
				
				long hPrivNames = rethPrivNames.getValue();
				List<String> roles;
				if (hPrivNames==0)
					roles = Collections.emptyList();
				else {
					Pointer pPrivNames = Mem64.OSLockObject(hPrivNames);
					ShortByReference retTextLength = new ShortByReference();
					Memory retTextPointer = new Memory(Native.POINTER_SIZE);
					try {
						int numEntriesAsInt = (int) (NotesNativeAPI.get().ListGetNumEntries(pPrivNames, 0) & 0xffff);
						roles = new ArrayList<String>(numEntriesAsInt);
						for (int i=0; i<numEntriesAsInt; i++) {
							result = NotesNativeAPI.get().ListGetText(pPrivNames, false, (short) (i & 0xffff), retTextPointer, retTextLength);
							NotesErrorUtils.checkResult(result);
							
							String currRole = NotesStringUtils.fromLMBCS(retTextPointer.getPointer(0), retTextLength.getValue() & 0xffff);
							roles.add(currRole);
						}
					}
					finally {
						Mem64.OSUnlockObject(hPrivNames);
						Mem64.OSMemFree(hPrivNames);
					}
				}

				int iAccessLevel = retAccessLevel.getValue();
				AclLevel accessLevel = AclLevel.toLevel(iAccessLevel);

				int iAccessFlag = (int) (retAccessFlags.getValue() & 0xffff);
				EnumSet<AclFlag> retFlags = EnumSet.noneOf(AclFlag.class);
				for (AclFlag currFlag : AclFlag.values()) {
					if ((iAccessFlag & currFlag.getValue()) == currFlag.getValue()) {
						retFlags.add(currFlag);
					}
				}

				NotesACLAccess access = new NotesACLAccess(accessLevel, roles, retFlags);
				return access;
			}
			finally {
				Mem64.OSUnlockObject(hNamesList);
			}
		}
		else {
			IntByReference rethPrivNames = new IntByReference();
			
			int hNamesList = namesList.getHandle32();
			Pointer pNamesList = Mem32.OSLockObject(hNamesList);
			try {
				result = NotesNativeAPI32.get().ACLLookupAccess(m_hACL32, pNamesList, retAccessLevel,
						retPrivileges, retAccessFlags, rethPrivNames);
				NotesErrorUtils.checkResult(result);
				
				int hPrivNames = rethPrivNames.getValue();
				List<String> roles;
				if (hPrivNames==0)
					roles = Collections.emptyList();
				else {
					Pointer pPrivNames = Mem32.OSLockObject(hPrivNames);
					ShortByReference retTextLength = new ShortByReference();
					Memory retTextPointer = new Memory(Native.POINTER_SIZE);
					try {
						int numEntriesAsInt = (int) (NotesNativeAPI.get().ListGetNumEntries(pPrivNames, 0) & 0xffff);
						roles = new ArrayList<String>(numEntriesAsInt);
						for (int i=0; i<numEntriesAsInt; i++) {
							result = NotesNativeAPI.get().ListGetText(pPrivNames, false, (short) (i & 0xffff), retTextPointer, retTextLength);
							NotesErrorUtils.checkResult(result);
							
							String currRole = NotesStringUtils.fromLMBCS(retTextPointer.getPointer(0), retTextLength.getValue() & 0xffff);
							roles.add(currRole);
						}
					}
					finally {
						Mem32.OSUnlockObject(hPrivNames);
						Mem32.OSMemFree(hPrivNames);
					}
				}
				
				int iAccessLevel = retAccessLevel.getValue();
				AclLevel accessLevel = AclLevel.toLevel(iAccessLevel);

				int iAccessFlag = (int) (retAccessFlags.getValue() & 0xffff);
				EnumSet<AclFlag> retFlags = EnumSet.noneOf(AclFlag.class);
				for (AclFlag currFlag : AclFlag.values()) {
					if ((iAccessFlag & currFlag.getValue()) == currFlag.getValue()) {
						retFlags.add(currFlag);
					}
				}

				NotesACLAccess access = new NotesACLAccess(accessLevel, roles, retFlags);
				return access;
			}
			finally {
				Mem32.OSUnlockObject(hNamesList);
			}
			
		}
	}
	
	/**
	 * Returns all ACL entries
	 * 
	 * @return ACL entries hashed by their username in the order they got returned from the C API
	 */
	public LinkedHashMap<String,NotesACLAccess> getEntries() {
		final Map<Integer,String> rolesByIndex = getRolesByIndex();
		
		final LinkedHashMap<String,NotesACLAccess> aclAccessInfoByName = new LinkedHashMap<String,NotesACLAccess>();
		
		ACLENTRYENUMFUNC callback;
		if (PlatformUtils.isWin32()) {
			callback = new Win32NotesCallbacks.ACLENTRYENUMFUNCWin32() {
				
				@Override
				public void invoke(Pointer enumFuncParam, Pointer nameMem, short accessLevelShort, Pointer privileges,
						short accessFlag) {
					
					String name = NotesStringUtils.fromLMBCS(nameMem, -1);
					AclLevel accessLevel = AclLevel.toLevel((int) (accessLevelShort & 0xffff));

					int iAccessFlag = (int) (accessFlag & 0xffff);
					EnumSet<AclFlag> retFlags = EnumSet.noneOf(AclFlag.class);
					for (AclFlag currFlag : AclFlag.values()) {
						if ((iAccessFlag & currFlag.getValue()) == currFlag.getValue()) {
							retFlags.add(currFlag);
						}
					}

					byte[] privilegesArr = privileges.getByteArray(0, 10);

					List<String> entryRoles = new ArrayList<String>();
					
					for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) { // Privilege names associated with privilege numbers 0 through 4 are privilege levels compatible with versions of Notes prior to Release 3
						//convert position to byte/bit position of byte[10]
						int byteOffsetWithBit = i / 8;
						byte byteValueWithBit = privilegesArr[byteOffsetWithBit];
						int bitToCheck = (int) Math.pow(2, i % 8);
						
						boolean enabled = (byteValueWithBit & bitToCheck) == bitToCheck;
						if (enabled) {
							String currRole = rolesByIndex.get(i);
							entryRoles.add(currRole);
						}
					}

					NotesACLAccess access = new NotesACLAccess(accessLevel, entryRoles, retFlags);
					aclAccessInfoByName.put(name, access);
				}
			};
		}
		else {
			callback = new ACLENTRYENUMFUNC() {

				@Override
				public void invoke(Pointer enumFuncParam, Pointer nameMem, short accessLevelShort, Pointer privileges,
						short accessFlag) {
					
					String name = NotesStringUtils.fromLMBCS(nameMem, -1);
					AclLevel accessLevel = AclLevel.toLevel((int) (accessLevelShort & 0xffff));

					int iAccessFlag = (int) (accessFlag & 0xffff);
					EnumSet<AclFlag> retFlags = EnumSet.noneOf(AclFlag.class);
					for (AclFlag currFlag : AclFlag.values()) {
						if ((iAccessFlag & currFlag.getValue()) == currFlag.getValue()) {
							retFlags.add(currFlag);
						}
					}

					byte[] privilegesArr = privileges.getByteArray(0, 10);

					List<String> entryRoles = new ArrayList<String>();
					
					for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) { // Privilege names associated with privilege numbers 0 through 4 are privilege levels compatible with versions of Notes prior to Release 3
						//convert position to byte/bit position of byte[10]
						int byteOffsetWithBit = i / 8;
						byte byteValueWithBit = privilegesArr[byteOffsetWithBit];
						int bitToCheck = (int) Math.pow(2, i % 8);
						
						boolean enabled = (byteValueWithBit & bitToCheck) == bitToCheck;
						if (enabled) {
							String currRole = rolesByIndex.get(i);
							entryRoles.add(currRole);
						}
					}

					NotesACLAccess access = new NotesACLAccess(accessLevel, entryRoles, retFlags);
					aclAccessInfoByName.put(name, access);
				}
			};
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().ACLEnumEntries(m_hACL64, callback, null);
		}
		else {
			result = NotesNativeAPI32.get().ACLEnumEntries(m_hACL32, callback, null);
		}
		NotesErrorUtils.checkResult(result);
		
		return aclAccessInfoByName;
	}
	
	/**
	 * Returns all roles declared in the ACL
	 * 
	 * @return roles
	 */
	public List<String> getRoles() {
		List<String> roles = new ArrayList<String>();

		short result;
		DisposableMemory retPrivName = new DisposableMemory(NotesConstants.ACL_PRIVSTRINGMAX);
		try {
			for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) { // Privilege names associated with privilege numbers 0 through 4 are privilege levels compatible with versions of Notes prior to Release 3
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().ACLGetPrivName(m_hACL64, (short) (i & 0xffff), retPrivName);
					if ((result & NotesConstants.ERR_MASK)==1060)  { //Error "The name is not in the list" => no more entries
						break;
					}

					NotesErrorUtils.checkResult(result);

					String role = NotesStringUtils.fromLMBCS(retPrivName, -1);
					if (!StringUtil.isEmpty(role)) {
						roles.add(role);
					}
				}
				else {
					result = NotesNativeAPI32.get().ACLGetPrivName(m_hACL32, (short) (i & 0xffff), retPrivName);
					if ((result & NotesConstants.ERR_MASK)==1060)  { //Error "The name is not in the list" => no more entries
						break;
					}

					NotesErrorUtils.checkResult(result);

					String role = NotesStringUtils.fromLMBCS(retPrivName, -1);
					if (!StringUtil.isEmpty(role)) {
						roles.add(role);
					}
				}
			}
		}
		finally {
			retPrivName.dispose();
		}

		return roles;
	}
	
	/**
	 * Returns the role names hashed by their internal position
	 * 
	 * @return roles
	 */
	private Map<Integer,String> getRolesByIndex() {
		Map<Integer,String> roles = new HashMap<Integer,String>();
		
		short result;
		Memory retPrivName = new Memory(NotesConstants.ACL_PRIVSTRINGMAX);
		
		for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) { // Privilege names associated with privilege numbers 0 through 4 are privilege levels compatible with versions of Notes prior to Release 3
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().ACLGetPrivName(m_hACL64, (short) (i & 0xffff), retPrivName);
				if ((result & NotesConstants.ERR_MASK)==1060)  { //Error "The name is not in the list" => no more entries
					break;
				}

				NotesErrorUtils.checkResult(result);
				
				String role = NotesStringUtils.fromLMBCS(retPrivName, -1);
				if (!StringUtil.isEmpty(role)) {
					roles.put(i, role);
				}
			}
			else {
				result = NotesNativeAPI32.get().ACLGetPrivName(m_hACL32, (short) (i & 0xffff), retPrivName);
				if ((result & NotesConstants.ERR_MASK)==1060)  { //Error "The name is not in the list" => no more entries
					break;
				}
				
				NotesErrorUtils.checkResult(result);
				
				String role = NotesStringUtils.fromLMBCS(retPrivName, -1);
				if (!StringUtil.isEmpty(role)) {
					roles.put(i, role);
				}
			}
		}
		
		return roles;
	}
	
	@Override
	public String toString() {
		if (isFreed()) {
			return "NotesACL [freed]";
		}
		else {
			String server = m_parentDb.getServer();
			String filePath = m_parentDb.getRelativeFilePath();
			String dbNetPath = StringUtil.isEmpty(server) ? filePath : server+"!!"+filePath;
			return "NotesACL [handle="+(PlatformUtils.is64Bit() ? m_hACL64 : m_hACL32)+", db="+dbNetPath+"]";
		}
	}

	
	public static class NotesACLAccess {
		private AclLevel m_accessLevel;
		private EnumSet<AclFlag> m_accessFlags;
		private List<String> m_roles;
		
		private NotesACLAccess(AclLevel accessLevel, List<String> roles, EnumSet<AclFlag> accessFlags) {
			m_accessLevel = accessLevel;
			m_roles = roles;
			m_accessFlags = accessFlags;
		}
		
		public List<String> getRoles() {
			return m_roles;
		}
		
		public AclLevel getAclLevel() {
			return m_accessLevel;
		}
		
		public EnumSet<AclFlag> getAclFlags() {
			return m_accessFlags;
		}
		
		@Override
		public String toString() {
			return "NotesACLAccess [level="+m_accessLevel+", roles="+m_roles+", flags="+m_accessFlags+"]";
		}
	}
}
