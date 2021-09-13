package com.mindoo.domino.jna.utils;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.NotesSSOToken;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
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

		NotesTimeDateStruct creationDateStruct = creationDate==null ? null : NotesTimeDateStruct.newInstance(creationDate.getInnards());
		NotesTimeDateStruct expirationDateStruct = expirationDate==null ? null : NotesTimeDateStruct.newInstance(expirationDate.getInnards());
		
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

			ptr = Mem64.OSMemoryLock(hToken64);
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

			ptr = Mem32.OSMemoryLock(hToken32);
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
						Pointer ptrName = Mem64.OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							Mem64.OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = Mem64.OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							Mem64.OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = Mem64.OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							Mem64.OSMemoryUnlock(tokenData.mhData);
						}
					}
					isSecureOnly = tokenData.isSecureOnly();

				}
				else {
					NotesSSOTokenStruct64 tokenData = NotesSSOTokenStruct64.newInstance(ptr);
					tokenData.read();
					
					//decode name
					if (tokenData.mhName!=0) {
						Pointer ptrName = Mem64.OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							Mem64.OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = Mem64.OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							Mem64.OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = Mem64.OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							Mem64.OSMemoryUnlock(tokenData.mhData);
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
						Pointer ptrName = Mem32.OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							Mem32.OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = Mem32.OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							Mem32.OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = Mem32.OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							Mem32.OSMemoryUnlock(tokenData.mhData);
						}
					}
					isSecureOnly = tokenData.isSecureOnly();
					
				}
				else {
					NotesSSOTokenStruct32 tokenData = NotesSSOTokenStruct32.newInstance(ptr);
					tokenData.read();
					
					//decode name
					if (tokenData.mhName!=0) {
						Pointer ptrName = Mem32.OSMemoryLock(tokenData.mhName);
						try {
							name = NotesStringUtils.fromLMBCS(ptrName, -1);
						}
						finally {
							Mem32.OSMemoryUnlock(tokenData.mhName);
						}
					}
					
					//decode domain list
					if (tokenData.wNumDomains>0 && tokenData.mhDomainList!=0) {
						Pointer ptrDomains = Mem32.OSMemoryLock(tokenData.mhDomainList);
						try {
							domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
						}
						finally {
							Mem32.OSMemoryUnlock(tokenData.mhDomainList);
						}
					}
					else {
						domains = Collections.emptyList();
					}
					
					if (tokenData.mhData!=0) {
						Pointer ptrData = Mem32.OSMemoryLock(tokenData.mhData);
						try {
							data = NotesStringUtils.fromLMBCS(ptrData, -1);
						}
						finally {
							Mem32.OSMemoryUnlock(tokenData.mhData);
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
					Mem64.OSMemoryUnlock(hToken64);
					//frees SSO_TOKEN and its members
					Mem64.OSMemoryFree(hToken64);
				}
			}
			else {
				if (hToken32!=0) {
					Mem32.OSMemoryUnlock(hToken32);
					//frees SSO_TOKEN and its members
					Mem32.OSMemoryFree(hToken32);
				}
			}
		}
	}

	public static class TokenInfo {
		private String userName;
		private NotesTimeDate idleTimeout;
		private NotesTimeDate creationTime;
		private NotesTimeDate expirationTime;
		
		public String getUserName() {
			return userName;
		}

		private TokenInfo setUserName(String userName) {
			this.userName = userName;
			return this;
		}

		public NotesTimeDate getIdleTimeout() {
			return idleTimeout;
		}
		
		private TokenInfo setIdleTimeout(NotesTimeDate renewalTime) {
			this.idleTimeout = renewalTime;
			return this;
		}

		public NotesTimeDate getCreationTime() {
			return creationTime;
		}

		private TokenInfo setCreationTime(NotesTimeDate creationTime) {
			this.creationTime = creationTime;
			return this;
		}

		public NotesTimeDate getExpirationTime() {
			return expirationTime;
		}

		private TokenInfo setExpirationTime(NotesTimeDate expirationTime) {
			this.expirationTime = expirationTime;
			return this;
		}
	}
	
	public static class TokenExpiredException extends NotesError {
		private static final long serialVersionUID = -713498735905846965L;
		
		private String userName;
		private NotesTimeDate idleTimeout;
		private NotesTimeDate creationTime;
		private NotesTimeDate expirationTime;

		public TokenExpiredException(int id, String msg, String userName,
				NotesTimeDate creationTime, NotesTimeDate expirationTime, NotesTimeDate idleTimeout) {
			super(id, msg);
			this.userName = userName;
			this.creationTime = creationTime;
			this.expirationTime = expirationTime;
			this.idleTimeout = idleTimeout;
		}
		
		public String getUserName() {
			return userName;
		}
		
		public NotesTimeDate getCreationTime() {
			return creationTime;
		}

		public NotesTimeDate getExpirationTime() {
			return expirationTime;
		}

		public NotesTimeDate getIdleTimeout() {
			return idleTimeout;
		}

	}
	
	/**
	 * Use this function to validate an authentication token for interoperability with Domino protocols
	 * that support Single Sign-On (IE: HTTP and DIIOP), as well as IBM WebSphere Advanced Server.<br>
	 * Returns the username encoded in the token as well as the token creation and expiration time.<br>
	 * The creation time is not returned if the configuration specified is set up for interoperability
	 * with IBM's WebSphere. Any error returned from this function represents authentication failure.
	 * For expired tokens, we throw an {@link TokenExpiredException} that provides access to
	 * the token's username, creation time, expiration time and idle timeout.
	 * 
	 * @param orgName Organization that the server belongs to (OPTIONAL: Specify NULL if not using 'Internet Sites' view for configuration)
	 * @param configName Name of Web SSO Configuration to use.
	 * @param tokenData token data
	 * @param enableRenewal  If true, the routine will return the time at which the Token does its IdleTimeout (which may be sooner than the value returned by Expiration if idle timeout is enabled for this LTPA token). Idle timeout can only be enabled for Domino style tokens. The Websphere format does not support them.
	 * @return token info
	 * @throws TokenExpiredException if token is expired; exception provides access to username, creation time, expiration time and idle timeout
	 */
	public static TokenInfo validateToken(String orgName, String configName, String tokenData,
			boolean enableRenewal) throws TokenExpiredException {
		Memory orgNameMem = NotesStringUtils.toLMBCS(orgName, true);
		Memory configNameMem = NotesStringUtils.toLMBCS(configName, true);
		Memory tokenDataMem = NotesStringUtils.toLMBCS(tokenData, true);
		DisposableMemory retUsernameMem = new DisposableMemory(NotesConstants.MAXUSERNAME);
		retUsernameMem.clear();
		
		try {
			NotesTimeDateStruct.ByReference retCreationTimeStruct = NotesTimeDateStruct.ByReference.newInstanceByReference();
			NotesTimeDateStruct.ByReference retExpirationTimeStruct = NotesTimeDateStruct.ByReference.newInstanceByReference();
			NotesTimeDateStruct.ByReference retIdleTimeStruct = NotesTimeDateStruct.ByReference.newInstanceByReference();

			short result = NotesNativeAPI.get().SECTokenValidate(null, orgNameMem, configNameMem, tokenDataMem,
					retUsernameMem, retCreationTimeStruct, retExpirationTimeStruct,
					enableRenewal ? NotesConstants.fSECToken_EnableRenewal : 0, retIdleTimeStruct.getPointer());
			
			boolean tokenExpired = false;
			if ((result & NotesConstants.ERR_MASK)==INotesErrorConstants.ERR_LTPA_TOKEN_EXPIRED) { // "Single Sign-On token is expired"
				tokenExpired = true;
				//don't check error code, we throw a TokenExpiredException later
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
			
			String retUsername = NotesStringUtils.fromLMBCS(retUsernameMem, -1);
			
			retCreationTimeStruct.read();
			NotesTimeDate retCreationTime = new NotesTimeDate(retCreationTimeStruct.Innards);
			
			retExpirationTimeStruct.read();
			NotesTimeDate retExpirationTime = new NotesTimeDate(retExpirationTimeStruct.Innards);
			
			retIdleTimeStruct.read();
			NotesTimeDate retIdleTime = new NotesTimeDate(retIdleTimeStruct.Innards);
			
			if (tokenExpired) {
				String msg = NotesErrorUtils.errToString(result);
				throw new TokenExpiredException(result, msg, retUsername,
						retCreationTime, retExpirationTime, retIdleTime);
			}
			else {
				return new TokenInfo()
				.setUserName(retUsername)
				.setCreationTime(retCreationTime)
				.setExpirationTime(retExpirationTime)
				.setIdleTimeout(retIdleTime);
			}
		}
		finally {
			retUsernameMem.dispose();
		}
	}
	
}
