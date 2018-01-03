package com.mindoo.domino.jna.utils;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.NotesSSOToken;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.NotesSSOTokenStruct32;
import com.mindoo.domino.jna.internal.structs.NotesSSOTokenStruct64;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.WinNotesSSOTokenStruct32;
import com.mindoo.domino.jna.internal.structs.WinNotesSSOTokenStruct64;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Utility class to produce LtpaTokens using Domino's built in C API methods
 * 
 * @author Karsten Lehmann
 */
public class SSOUtils {

	/**
	 * Generates a new SSO token
	 * 
	 * @param orgName organization that the server belongs to, use null if not using Internet Site views for configuration
	 * @param configName name of Web SSO configuration to use
	 * @param userName Notes name to encode in the token, use either canonical or abbreviated format
	 * @param creationDate creation time to set for the token, use null for the current time
	 * @param expirationDate expiration time to set for the token, use null to use expiration from specified Web SSO configuration
	 * @param enableRenewal if true, the generated token contains the {@link NotesTimeDate} where the token expires (for Domino only)
	 * @return token
	 */
	public static NotesSSOToken generateSSOToken(String orgName, String configName,
			String userName, NotesTimeDate creationDate, NotesTimeDate expirationDate, boolean enableRenewal) {

		NotesTimeDateStruct creationDateStruct = creationDate==null ? null : creationDate.getAdapter(NotesTimeDateStruct.class);
		NotesTimeDateStruct expirationDateStruct = expirationDate==null ? null : expirationDate.getAdapter(NotesTimeDateStruct.class);
		
		Memory orgNameMem = NotesStringUtils.toLMBCS(orgName, true);
		Memory configNameMem = NotesStringUtils.toLMBCS(configName, true);

		String userNameCanonical = NotesNamingUtils.toCanonicalName(userName);
		Memory userNameCanonicalMem = NotesStringUtils.toLMBCS(userNameCanonical, true);

		NotesTimeDateStruct renewalDate = enableRenewal ? NotesTimeDateStruct.newInstance() : null;
		
		short result;
		
		Pointer ptr;
		long hToken64 = 0;
		int hToken32 = 0;
		
		if (PlatformUtils.is64Bit()) {
			LongByReference retmhToken = new LongByReference();
			retmhToken.setValue(0);

			result = NotesNativeAPI64.get().SECTokenGenerate(null, orgNameMem, configNameMem, userNameCanonicalMem,
					creationDateStruct, expirationDateStruct,
					retmhToken, enableRenewal ? NotesConstants.fSECToken_EnableRenewal : 0, renewalDate==null ? null : renewalDate.getPointer());
			NotesErrorUtils.checkResult(result);

			if (renewalDate!=null) {
				renewalDate.read();
			}

			hToken64 = retmhToken.getValue();
			
			if (hToken64==0)
				throw new IllegalStateException("SECTokenGenerate returned null value for the SSO token");

			ptr = NotesNativeAPI64.get().OSMemoryLock(hToken64);
		}
		else {
			IntByReference retmhToken = new IntByReference();
			retmhToken.setValue(0);
			
			result = NotesNativeAPI32.get().SECTokenGenerate(null, orgNameMem, configNameMem, userNameCanonicalMem,
					creationDateStruct, expirationDateStruct,
					retmhToken, enableRenewal ? NotesConstants.fSECToken_EnableRenewal : 0, renewalDate==null ? null : renewalDate.getPointer());
			NotesErrorUtils.checkResult(result);
			
			if (renewalDate!=null) {
				renewalDate.read();
			}

			hToken32 = retmhToken.getValue();
			if (hToken32==0)
				throw new IllegalStateException("SECTokenGenerate returned null value for the SSO token");

			ptr = NotesNativeAPI32.get().OSMemoryLock(hToken32);
		}
		
		try {
			String name=null;
			List<String> domains;
			String data=null;
			boolean isSecureOnly;
			
			if (PlatformUtils.is64Bit()) {
				if (PlatformUtils.isWindows()) {
					WinNotesSSOTokenStruct64 tokenData = WinNotesSSOTokenStruct64.newInstance(ptr);
					tokenData.read();
					
					//decode name
					if (tokenData.mhName!=0) {
						Pointer ptrName = NotesNativeAPI64.get().OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							NotesNativeAPI64.get().OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = NotesNativeAPI64.get().OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							NotesNativeAPI64.get().OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = NotesNativeAPI64.get().OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							NotesNativeAPI64.get().OSMemoryUnlock(tokenData.mhData);
						}
					}
					isSecureOnly = tokenData.isSecureOnly();

				}
				else {
					NotesSSOTokenStruct64 tokenData = NotesSSOTokenStruct64.newInstance(ptr);
					tokenData.read();
					
					//decode name
					if (tokenData.mhName!=0) {
						Pointer ptrName = NotesNativeAPI64.get().OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							NotesNativeAPI64.get().OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = NotesNativeAPI64.get().OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							NotesNativeAPI64.get().OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = NotesNativeAPI64.get().OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							NotesNativeAPI64.get().OSMemoryUnlock(tokenData.mhData);
						}
					}
					isSecureOnly = tokenData.isSecureOnly();
				}
			}
			else {
				if (PlatformUtils.isWindows()) {
					WinNotesSSOTokenStruct32 tokenData = WinNotesSSOTokenStruct32.newInstance(ptr);
					tokenData.read();
					
					//decode name
					if (tokenData.mhName!=0) {
						Pointer ptrName = NotesNativeAPI32.get().OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							NotesNativeAPI32.get().OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = NotesNativeAPI32.get().OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							NotesNativeAPI32.get().OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = NotesNativeAPI32.get().OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							NotesNativeAPI32.get().OSMemoryUnlock(tokenData.mhData);
						}
					}
					isSecureOnly = tokenData.isSecureOnly();
					
				}
				else {
					NotesSSOTokenStruct32 tokenData = NotesSSOTokenStruct32.newInstance(ptr);
					tokenData.read();
					
					//decode name
					if (tokenData.mhName!=0) {
						Pointer ptrName = NotesNativeAPI32.get().OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							NotesNativeAPI32.get().OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = NotesNativeAPI32.get().OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							NotesNativeAPI32.get().OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = NotesNativeAPI32.get().OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							NotesNativeAPI32.get().OSMemoryUnlock(tokenData.mhData);
						}
					}
					isSecureOnly = tokenData.isSecureOnly();
					
				}
			}

			NotesSSOToken ssoToken = new NotesSSOToken(name, domains, isSecureOnly, data, renewalDate==null ? null : new NotesTimeDate(renewalDate));
			return ssoToken;
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				if (hToken64!=0) {
					NotesNativeAPI64.get().OSMemoryUnlock(hToken64);
					//frees SSO_TOKEN and its members
					NotesNativeAPI64.get().OSMemoryFree(hToken64);
				}
			}
			else {
				if (hToken32!=0) {
					NotesNativeAPI32.get().OSMemoryUnlock(hToken32);
					//frees SSO_TOKEN and its members
					NotesNativeAPI32.get().OSMemoryFree(hToken32);
				}
			}
		}
	}

}
