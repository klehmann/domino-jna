package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks.ACLENTRYENUMFUNC;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.utils.ListUtil;
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
	private Optional<NotesDatabase> m_parentDb = Optional.empty();
	private DHANDLE m_hACL;
	private String m_server;
	
	/**
	 * Creates a standalone ACL without parent database
	 * 
	 * @param dhandle ACL handle
	 * @param server parent server to lookup group membership
	 */
	NotesACL(DHANDLE dhandle, String server) {
		m_hACL = dhandle;
		m_server = server;
	}
	
	/**
	 * Creates a new NSF ACL instance
	 * 
	 * @param parentDb parent database
	 * @param hACL ACL handle
	 */
	NotesACL(NotesDatabase parentDb, DHANDLE hACL) {
		m_parentDb = Optional.ofNullable(parentDb);
		m_server = parentDb.getServer();
		m_hACL = hACL;
	}
	
	/**
	 * Returns the parent database
	 * 
	 * @return db or null if standalone ACL
	 */
	public NotesDatabase getParentDatabase() {
		return m_parentDb.isPresent() ? null : m_parentDb.get();
	}
	
	@Override
	public void free() {
		if (isFreed())
			return;

		NotesGC.__memoryBeeingFreed(this);
		short result = Mem.OSMemFree(m_hACL.getByValue());
		NotesErrorUtils.checkResult(result);
		m_hACL.setDisposed();
	}

	@Override
	public boolean isFreed() {
		return m_hACL.isDisposed();
	}

	@Override
	public int getHandle32() {
		return m_hACL instanceof DHANDLE32 ? ((DHANDLE32)m_hACL).hdl : 0;
	}

	@Override
	public long getHandle64() {
		return m_hACL instanceof DHANDLE64 ? ((DHANDLE64)m_hACL).hdl : 0;
	}

	public DHANDLE getHandle() {
		return m_hACL;
	}
	
	/**
	 * Change the name of the administration server for the access control list.
	 * 
	 * @param server server, either in abbreviated or canonical format
	 */
	public void setAdminServer(String server) {
		checkHandle();
		
		Memory serverCanonicalMem = NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(server), true);
		short result = NotesNativeAPI.get().ACLSetAdminServer(m_hACL.getByValue(), serverCanonicalMem);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function stores the access control list in the parent database.<br>
	 */
	public void save() {
		checkHandle();
		
		if (!m_parentDb.isPresent()) {
			throw new IllegalStateException("This is a detached ACL, so there's no parent database to save to.");
		}
		else if (m_parentDb.get().isRecycled())
			throw new NotesError(0, "Parent database already recycled");
		
		short result = NotesNativeAPI.get().NSFDbStoreACL(m_parentDb.get().getHandle().getByValue(), getHandle().getByValue(), 0, (short) 0);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Checks if the database is already recycled
	 */
	private void checkHandle() {
		if (m_hACL.isDisposed())
			throw new NotesError(0, "Memory already freed");

		NotesGC.__checkValidMemHandle(getClass(), getHandle());
	}

	/**
	 * Looks up the access level for a user and his groups
	 * 
	 * @param userName username, either canonical or abbreviated
	 * @return acl access info, with access level, flags and roles
	 */
	public NotesACLAccess lookupAccess(String userName) {
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(m_server, userName);
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
		
		DHANDLE hAcl = getHandle();

		LongByReference rethPrivNames = new LongByReference();
		
		long hNamesList = namesList.getHandle64();
		Pointer pNamesList = Mem64.OSLockObject(hNamesList);
		try {
			result = NotesNativeAPI.get().ACLLookupAccess(hAcl.getByValue(), pNamesList, retAccessLevel,
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
	
	/**
	 * Convenience method that call {@link #getEntries()} and returns a single
	 * value for the specified name
	 * 
	 * @param name name
	 * @return acl entry or null if not found
	 */
	public NotesACLEntry getEntry(String name) {
		LinkedHashMap<String,NotesACLEntry> entries = getEntries();
		NotesACLEntry entry = entries.get(name);
		if (entry==null) {
			for (Entry<String,NotesACLEntry> currEntry : entries.entrySet()) {
				if (NotesNamingUtils.equalNames(currEntry.getKey(), name)) {
					entry = currEntry.getValue();
					break;
				}
			}
		}
		return entry;
	}
	
	/**
	 * Returns all ACL entries
	 * 
	 * @return ACL entries hashed by their username in the order they got returned from the C API
	 */
	public LinkedHashMap<String,NotesACLEntry> getEntries() {
		final Map<Integer,String> rolesByIndex = getRolesByIndex();
		
		final LinkedHashMap<String,NotesACLEntry> aclAccessInfoByName = new LinkedHashMap<String,NotesACLEntry>();
		
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
					
					NotesACLEntry access = new NotesACLEntry(name, accessLevel, entryRoles, privilegesArr, retFlags);
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

					NotesACLEntry access = new NotesACLEntry(name, accessLevel, entryRoles, privilegesArr, retFlags);
					aclAccessInfoByName.put(name, access);
				}
			};
		}
		
		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLEnumEntries(hAcl.getByValue(), callback, null);
		NotesErrorUtils.checkResult(result);
		
		return aclAccessInfoByName;
	}
	
	/**
	 * Changes the name of a role
	 * 
	 * @param oldName old role name, either enclosed with [] or not
	 * @param newName new role name, either enclosed with [] or not
	 * @throws NotesError if role could not be found
	 */
	public void renameRole(String oldName, String newName) {
		if (oldName==null || newName==null) {
			throw new IllegalArgumentException("Neither the former role name, nor the new can be empty");
		}
		
		checkHandle();
		
		String oldNameStripped = oldName;
		if (oldNameStripped.startsWith("[")) {
			oldNameStripped = oldNameStripped.substring(1);
		}
		if (oldNameStripped.endsWith("]")) {
			oldNameStripped = oldNameStripped.substring(0, oldNameStripped.length()-1);
		}

		String newNameStripped = newName;
		if (newNameStripped.startsWith("[")) {
			newNameStripped = newNameStripped.substring(1);
		}
		if (newNameStripped.endsWith("]")) {
			newNameStripped = newNameStripped.substring(0, newNameStripped.length()-1);
		}
		
		if (oldNameStripped.length()==0 || newNameStripped.length()==0) {
			throw new IllegalArgumentException("Neither the former role name, nor the new can be empty");
		}

		if (newNameStripped.length() >= NotesConstants.ACL_PRIVNAMEMAX) {
			throw new IllegalArgumentException("Role name length cannot (content within brackets) exceed "+(NotesConstants.ACL_PRIVNAMEMAX-1)+" characters");
		}

		if (oldNameStripped.equals(newNameStripped)) {
			return; // nothing to do
		}

		String oldNameWithBrackets = "[" + oldNameStripped + "]";
		String newNameWithBrackets = "[" + newNameStripped + "]";

		Map<Integer,String> rolesByIndex = getRolesByIndex();
		int roleIndex = -1;
		int newRoleIndex = -1;
		
		for (Entry<Integer,String> currEntry : rolesByIndex.entrySet()) {
			Integer currIndex = currEntry.getKey();
			String currRole = currEntry.getValue();
			
			if (currRole.equalsIgnoreCase(oldNameWithBrackets)) {
				roleIndex = currIndex.intValue();
			}
			if (currRole.contentEquals(newNameWithBrackets)) {
				newRoleIndex = currIndex;
			}
		}
		
		if (roleIndex==-1) {
			throw new NotesError(0, "Role not found in ACL: "+oldName);
		}
		if (newRoleIndex!=-1) {
			throw new NotesError(0, "Role already exists in ACL: " + newName);
		}
		
		Memory newNameStrippedMem = NotesStringUtils.toLMBCS(newNameStripped, true);
		
		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLSetPrivName(hAcl.getByValue(), (short) (roleIndex & 0xffff), newNameStrippedMem);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Returns all roles declared in the ACL
	 * 
	 * @return roles
	 */
	public List<String> getRoles() {
		List<String> roles = new ArrayList<String>();

		DHANDLE hAcl = getHandle();
		
		short result;
		DisposableMemory retPrivName = new DisposableMemory(NotesConstants.ACL_PRIVSTRINGMAX);
		try {
			for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) { // Privilege names associated with privilege numbers 0 through 4 are privilege levels compatible with versions of Notes prior to Release 3
				result = NotesNativeAPI.get().ACLGetPrivName(hAcl.getByValue(), (short) (i & 0xffff), retPrivName);
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
		
		DHANDLE hAcl = getHandle();
		
		for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) { // Privilege names associated with privilege numbers 0 through 4 are privilege levels compatible with versions of Notes prior to Release 3
			result = NotesNativeAPI.get().ACLGetPrivName(hAcl.getByValue(), (short) (i & 0xffff), retPrivName);
			if ((result & NotesConstants.ERR_MASK)==1060)  { //Error "The name is not in the list" => no more entries
				break;
			}

			NotesErrorUtils.checkResult(result);
			
			String role = NotesStringUtils.fromLMBCS(retPrivName, -1);
			if (!StringUtil.isEmpty(role)) {
				roles.put(i, role);
			}
		}
		
		return roles;
	}

	/**
	 * This function adds an entry to an access control list.
	 * 
	 * @param name user or group to be added, either in abbreviated or canonical format
	 * @param accessLevel Access level ({@link AclLevel}), of the entry to be added
	 * @param roles roles to be set for this user
	 * @param accessFlags Access level modifier flags ({@link AclFlag}), e.g.: unable to delete documents, unable to create documents, of the entry to be added
	 */
	public void addEntry(String name, AclLevel accessLevel, List<String> roles, EnumSet<AclFlag> accessFlags) {
		checkHandle();
		
		List<String> rolesFormatted;
		if (roles.isEmpty()) {
			rolesFormatted = roles;
		}
		else {
			boolean rolesOk = true;
			
			for (String currRole : roles) {
				if (!currRole.startsWith("[")) {
					rolesOk = false;
					break;
				}
				else if (!currRole.endsWith("]")) {
					rolesOk = false;
					break;
				}
			}
			
			if (rolesOk) {
				rolesFormatted = roles;
			}
			else {
				rolesFormatted = new ArrayList<>();
				for (String currRole : roles) {
					if (!currRole.startsWith("[")) {
						currRole = "[" + currRole;
					}
					if (!currRole.endsWith("]")) {
						currRole = currRole + "]";
					}
					rolesFormatted.add(currRole);
				}
			}
		}
		
		String nameCanonical = NotesNamingUtils.toCanonicalName(name);
		
		Map<Integer,String> rolesByIndex = getRolesByIndex();
		
		byte[] privilegesArr = new byte[NotesConstants.ACL_PRIVCOUNT / 8];

		for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) {
			String currRole = rolesByIndex.get(i);
			if (currRole!=null) {
				if (ListUtil.containsIgnoreCase(rolesFormatted, currRole)) {
					int byteOffsetWithBit = i / 8;
					int bitToCheck = (int) Math.pow(2, i % 8);
					
					privilegesArr[byteOffsetWithBit] = (byte) ((privilegesArr[byteOffsetWithBit] | bitToCheck) & 0xff);
				}
			}
		}
		
		Memory nameCanonicalMem = NotesStringUtils.toLMBCS(nameCanonical, true);

		short accessFlagsAsShort = AclFlag.toBitMask(accessFlags);
		
		DisposableMemory privilegesMem = new DisposableMemory(privilegesArr.length);
		try {
			privilegesMem.write(0, privilegesArr, 0, privilegesArr.length);
			
			DHANDLE hAcl = getHandle();
			short result = NotesNativeAPI.get().ACLAddEntry(hAcl.getByValue(), nameCanonicalMem, (short) (accessLevel.getValue() & 0xffff), privilegesMem, accessFlagsAsShort);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			privilegesMem.dispose();
		}
	}

	/**
	 * This function deletes an entry from an access control list.
	 * 
	 * @param name user or group to be deleted, in abbreviated or canonical format
	 */
	public void removeEntry(String name) {
		checkHandle();
		
		String nameCanonical = NotesNamingUtils.toCanonicalName(name);
		Memory nameCanonicalMem = NotesStringUtils.toLMBCS(nameCanonical, true);

		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLDeleteEntry(hAcl.getByValue(), nameCanonicalMem);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Check if "Enforce consistent ACL" is set
	 * 
	 * @return true if set
	 */
	public boolean isUniformAccess() {
		checkHandle();
		
		IntByReference retFlags = new IntByReference();
		
		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLGetFlags(hAcl.getByValue(), retFlags);
		NotesErrorUtils.checkResult(result);
		
		return (retFlags.getValue() & NotesConstants.ACL_UNIFORM_ACCESS) == NotesConstants.ACL_UNIFORM_ACCESS;
	}
	
	/**
	 * Changes the value for "Enforce consistent ACL"
	 * 
	 * @param uniformAccess true to set "Enforce consistent ACL" flag
	 */
	public void setUniformAccess(boolean uniformAccess) {
		checkHandle();
		
		IntByReference retFlags = new IntByReference();
		
		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLGetFlags(hAcl.getByValue(), retFlags);
		NotesErrorUtils.checkResult(result);
		
		boolean isSet = (retFlags.getValue() & NotesConstants.ACL_UNIFORM_ACCESS) == NotesConstants.ACL_UNIFORM_ACCESS;
		if (uniformAccess == isSet) {
			return;
		}
		
		int newFlags = retFlags.getValue();
		if (uniformAccess) {
			newFlags = newFlags | NotesConstants.ACL_UNIFORM_ACCESS;
		}
		else {
			newFlags -= NotesConstants.ACL_UNIFORM_ACCESS;
		}
		
		result = NotesNativeAPI.get().ACLSetFlags(hAcl.getByValue(), newFlags);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Adds a role to the ACL. If the role is already in the ACL, the method does nothing.
	 * 
	 * @param role role to add
	 */
	public void addRole(String role) {
		if (role==null) {
			throw new IllegalArgumentException("Cannot add role with empty name");
		}
		
		checkHandle();

		String roleStripped = role;
		
		if (roleStripped.startsWith("[")) {
			roleStripped = roleStripped.substring(1);
		}
		
		if (roleStripped.endsWith("]")) {
			roleStripped = roleStripped.substring(0, roleStripped.length()-1);
		}
		
		if (roleStripped.length()==0) {
			throw new IllegalArgumentException("Cannot add role with empty name");
		}
		
		if (roleStripped.length() >= NotesConstants.ACL_PRIVNAMEMAX) {
			throw new IllegalArgumentException("Role name length (content within brackets) cannot exceed "+(NotesConstants.ACL_PRIVNAMEMAX-1)+" characters");
		}

		String roleWithBrackets = "[" + role + "]";

		List<String> roles = getRoles();
		if (roles.contains(roleWithBrackets)) {
			return;
		}
		
		Map<Integer,String> rolesByIndex = getRolesByIndex();
		int freeIndex = -1;
		
		for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) {
			if (!rolesByIndex.containsKey(i) || "".equals(rolesByIndex.get(i))) {
				freeIndex = i;
				break;
			}
		}
		
		if (freeIndex==-1) {
			throw new NotesError("No more space available to add role");
		}
		
		Memory roleStrippedMem = NotesStringUtils.toLMBCS(roleStripped, true);
		
		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLSetPrivName(hAcl.getByValue(), (short) (freeIndex & 0xffff), roleStrippedMem);
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Removes a role from the ACL
	 * 
	 * @param role role to remove
	 */
	public void removeRole(String role) {
		checkHandle();
		
		if (!role.startsWith("[")) {
			role = "[" + role;
		}
		if (!role.endsWith("]")) {
			role = role + "]";
		}

		Map<Integer,String> rolesByIndex = getRolesByIndex();
		int roleIndex = -1;
		
		for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) {
			if (role.equals(rolesByIndex.get(i))) {
				roleIndex = i;
				break;
			}
		}

		if (roleIndex==-1) {
			//nothing to do
			return;
		}
		
		int byteOffsetWithBit = roleIndex / 8;
		int bitToCheck = (int) Math.pow(2, roleIndex % 8);

		LinkedHashMap<String, NotesACLEntry> entries = getEntries();
		
		for (Entry<String,NotesACLEntry> currEntry : entries.entrySet()) {
			String currName = currEntry.getKey();
			NotesACLEntry currACLEntry = currEntry.getValue();
			
			byte[] currPrivileges = currACLEntry.getPrivilegesArray();
			if ((currPrivileges[byteOffsetWithBit] & bitToCheck) == bitToCheck) {
				byte[] newPrivileges = currPrivileges.clone();
				newPrivileges[byteOffsetWithBit] = (byte) ((newPrivileges[byteOffsetWithBit] - bitToCheck & 0xff));
				
				Memory currNameMem = NotesStringUtils.toLMBCS(currName, true);
				
				DisposableMemory newPrivilegesMem = new DisposableMemory(newPrivileges.length);
				newPrivilegesMem.write(0, newPrivileges, 0, newPrivileges.length);
				
				try {
					DHANDLE hAcl = getHandle();
					short result = NotesNativeAPI.get().ACLUpdateEntry(hAcl.getByValue(), currNameMem, NotesConstants.ACL_UPDATE_PRIVILEGES, null, (short) 0,
							newPrivilegesMem, (short) 0);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					newPrivilegesMem.dispose();
				}
			}
		}
		
		Memory emptyStrMem = NotesStringUtils.toLMBCS("", true);
		
		DHANDLE hAcl = getHandle();
		short result = NotesNativeAPI.get().ACLSetPrivName(hAcl.getByValue(), (short) (roleIndex & 0xffff), emptyStrMem);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function updates an entry in an access control list.<br>
	 * <br>
	 * Unless the user's name is specified to be modified, the information that is not specified to be
	 * modified remains intact.<br>
	 * <br>
	 * If the user's name is specified to be modified, the user entry is deleted and a new entry is created.<br>
	 * Unless the other access control information is specified to be modified as well, the other access control
	 * information will be cleared and the user will have No Access to the database.
	 * 
	 * @param name name of the entry to change
	 * @param newName optional new entry name or null
	 * @param newAccessLevel optional new entry access level or null
	 * @param newRoles optional new entry roles or null
	 * @param newFlags optional new acl flags or null
	 */
	public void updateEntry(String name, String newName, AclLevel newAccessLevel, List<String> newRoles, EnumSet<AclFlag> newFlags) {
		int updateFlags = 0;
		
		NotesACLEntry oldAclEntry = getEntry(name);
		if (oldAclEntry==null) {
			if (newName==null) {
				newName = name;
			}
			addEntry(newName, newAccessLevel, newRoles, newFlags);
			return;
		}
		
		Memory oldAclEntryNameMem = "-default-".equalsIgnoreCase(oldAclEntry.getName()) ? null : NotesStringUtils.toLMBCS(oldAclEntry.getName(), true);
		
		Memory newNameMem = null;
		
		if (newName!=null) {
			newName = NotesNamingUtils.toCanonicalName(newName);
			
			if (!NotesNamingUtils.equalNames(oldAclEntry.getName(), newName)) {
				updateFlags = updateFlags | NotesConstants.ACL_UPDATE_NAME;
				
				newNameMem = NotesStringUtils.toLMBCS(newName, true);
			}
		}
		
		int iNewAccessLevel = oldAclEntry.getAclLevel().getValue();
		// TODO somehow it seems this flag always needs to be set
        //             otherwise the level will be reset to NOACCESS, in case the level did not change
        updateFlags = updateFlags | NotesConstants.ACL_UPDATE_LEVEL;
		if (newAccessLevel!=null && !newAccessLevel.equals(oldAclEntry.getAclLevel())) {
			updateFlags = updateFlags | NotesConstants.ACL_UPDATE_LEVEL;
			
			iNewAccessLevel = newAccessLevel.getValue();
		}
		
		DisposableMemory newPrivilegesMem = null;
		
		if (newRoles!=null && !newRoles.equals(oldAclEntry.getRoles())) {
			updateFlags = updateFlags | NotesConstants.ACL_UPDATE_PRIVILEGES;
			
			List<String> newRolesFormatted;
			if (newRoles.isEmpty()) {
				newRolesFormatted = newRoles;
			}
			else {
				boolean rolesOk = true;
				
				for (String currRole : newRoles) {
					if (!currRole.startsWith("[")) {
						rolesOk = false;
						break;
					}
					else if (!currRole.endsWith("]")) {
						rolesOk = false;
						break;
					}
				}
				
				if (rolesOk) {
					newRolesFormatted = newRoles;
				}
				else {
					newRolesFormatted = new ArrayList<>();
					for (String currRole : newRoles) {
						if (!currRole.startsWith("[")) {
							currRole = "[" + currRole;
						}
						if (!currRole.endsWith("]")) {
							currRole = currRole + "]";
						}
						newRolesFormatted.add(currRole);
					}
				}
			}
			
			Map<Integer,String> rolesByIndex = getRolesByIndex();
			byte[] newPrivilegesArr = new byte[NotesConstants.ACL_PRIVCOUNT / 8];
			
			for (int i=5; i<NotesConstants.ACL_PRIVCOUNT; i++) {
				String currRole = rolesByIndex.get(i);
				if (currRole!=null) {
					if (ListUtil.containsIgnoreCase(newRolesFormatted, currRole)) {
						int byteOffsetWithBit = i / 8;
						int bitToCheck = (int) Math.pow(2, i % 8);
						
						newPrivilegesArr[byteOffsetWithBit] = (byte) ((newPrivilegesArr[byteOffsetWithBit] | bitToCheck) & 0xff);
					}
				}
			}
			
			newPrivilegesMem = new DisposableMemory(NotesConstants.ACL_PRIVCOUNT / 8);
			newPrivilegesMem.write(0, newPrivilegesArr, 0, newPrivilegesArr.length);
		}
		
		short newFlagsAsShort = 0;
		
		if (newFlags!=null && !newFlags.equals(oldAclEntry.getAclFlags())) {
			updateFlags = updateFlags | NotesConstants.ACL_UPDATE_FLAGS;
			
			newFlagsAsShort = AclFlag.toBitMask(newFlags);
		}
		
		try {
			DHANDLE hAcl = getHandle();
			short result = NotesNativeAPI.get().ACLUpdateEntry(hAcl.getByValue(), oldAclEntryNameMem, (short) (updateFlags & 0xffff),
					newNameMem, (short) (iNewAccessLevel & 0xffff),
					newPrivilegesMem, newFlagsAsShort);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			if (newPrivilegesMem!=null) {
				newPrivilegesMem.dispose();
			}
		}
	}
	
	@Override
	public String toString() {
		if (isFreed()) {
			return "NotesACL [freed]";
		}
		else {
			if (!m_parentDb.isPresent()) {
				return "NotesACL [handle="+m_hACL+", isdetached=true]";
			}
			else {
				String server = m_parentDb.get().getServer();
				String filePath = m_parentDb.get().getRelativeFilePath();
				String dbNetPath = StringUtil.isEmpty(server) ? filePath : server+"!!"+filePath;
				return "NotesACL [handle="+m_hACL+", db="+dbNetPath+"]";
			}
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
		
		public boolean isPerson() {
			return m_accessFlags.contains(AclFlag.PERSON);
		}
		
		public boolean isGroup() {
			return m_accessFlags.contains(AclFlag.GROUP);
		}
		
		public boolean isServer() {
			return m_accessFlags.contains(AclFlag.SERVER);
		}
		
		public boolean isAdminServer() {
			return m_accessFlags.contains(AclFlag.ADMIN_SERVER);
		}
		
		@Override
		public String toString() {
			return "NotesACLAccess [level="+m_accessLevel+", roles="+m_roles+", flags="+m_accessFlags+"]";
		}
	}
	
	public static class NotesACLEntry extends NotesACLAccess {
		private String m_name;
		private byte[] m_privilegesArr;

		public NotesACLEntry(String name, AclLevel accessLevel, List<String> roles, byte[] privilegesArr, EnumSet<AclFlag> accessFlags) {
			super(accessLevel, roles, accessFlags);
			m_name = name;
			m_privilegesArr = privilegesArr;
		}
		
		public String getName() {
			return m_name;
		}
	
		byte[] getPrivilegesArray() {
			return m_privilegesArr;
		}

		@Override
		public String toString() {
			return "NotesACLEntry [name="+m_name+", level="+getAclLevel()+", roles="+getRoles()+", flags="+getAclFlags()+"]";
		}

	}
	
	/**
	 * Clones the ACL data and returns a new {@link NotesACL} not bound to
	 * the parent database.
	 * 
	 * @return acl clone
	 */
	public NotesACL cloneDetached() {
		checkHandle();
		
		DHANDLE hAcl = getHandle();
		
		DHANDLE.ByReference retNewHandle = DHANDLE.newInstanceByReference();
		
		short result = NotesNativeAPI.get().ACLCopy(hAcl.getByValue(), retNewHandle);
		NotesErrorUtils.checkResult(result);
		
		NotesACL aclClone = new NotesACL(retNewHandle, m_server);
		NotesGC.__memoryAllocated(aclClone);
		return aclClone;
	}
}
