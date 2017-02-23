package com.mindoo.domino.jna.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesSSOTokenStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

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
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesTimeDateStruct creationDateStruct = creationDate==null ? null : creationDate.getAdapter(NotesTimeDateStruct.class);
		NotesTimeDateStruct expirationDateStruct = expirationDate==null ? null : expirationDate.getAdapter(NotesTimeDateStruct.class);
		
		Memory orgNameMem = NotesStringUtils.toLMBCS(orgName, true);
		Memory configNameMem = NotesStringUtils.toLMBCS(configName, true);

		String userNameCanonical = NotesNamingUtils.toCanonicalName(userName);
		Memory userNameCanonicalMem = NotesStringUtils.toLMBCS(userNameCanonical, true);

		IntByReference retmhToken = new IntByReference();
		NotesTimeDateStruct renewalDate = enableRenewal ? NotesTimeDateStruct.newInstance() : null;
		
		short result = notesAPI.SECTokenGenerate(null, orgNameMem, configNameMem, userNameCanonicalMem,
				creationDateStruct, expirationDateStruct,
				retmhToken, enableRenewal ? NotesCAPI.fSECToken_EnableRenewal : 0, renewalDate==null ? null : renewalDate.getPointer());
		NotesErrorUtils.checkResult(result);
		
		if (renewalDate!=null) {
			renewalDate.read();
		}
		
		int hToken = retmhToken.getValue();
		if (hToken==0)
			throw new IllegalStateException("SECTokenGenerate returned null value for the SSO token");
		
		Pointer ptr = notesAPI.OSMemoryLock(hToken);
		try {
			NotesSSOTokenStruct tokenData = NotesSSOTokenStruct.newInstance(ptr);
			tokenData.read();
			
			//decode name
			String name=null;
			if (tokenData.mhName!=0) {
				Pointer ptrName = notesAPI.OSMemoryLock(tokenData.mhName);
				try {
					name = NotesStringUtils.fromLMBCS(ptrName, -1);
				}
				finally {
					notesAPI.OSMemoryUnlock(tokenData.mhName);
				}
			}
			
			//decode domain list
			List<String> domains;
			if (tokenData.wNumDomains>0) {
				Pointer ptrDomains = notesAPI.OSMemoryLock(tokenData.mhDomainList);
				try {
					domains = NotesStringUtils.fromLMBCSStringList(ptrDomains, (int) (tokenData.wNumDomains & 0xffff));
				}
				finally {
					notesAPI.OSMemoryUnlock(tokenData.mhDomainList);
				}
			}
			else {
				domains = Collections.emptyList();
			}
			
			String data=null;
			if (tokenData.mhName!=0) {
				Pointer ptrData = notesAPI.OSMemoryLock(tokenData.mhData);
				try {
					data = NotesStringUtils.fromLMBCS(ptrData, -1);
				}
				finally {
					notesAPI.OSMemoryUnlock(tokenData.mhData);
				}
			}
			
			NotesSSOToken ssoToken = new NotesSSOToken(name, domains, tokenData.bSecureOnly, data, renewalDate==null ? null : new NotesTimeDate(renewalDate));
			return ssoToken;
		}
		finally {
			notesAPI.OSMemoryUnlock(hToken);
			//frees SSO_TOKEN and its members
			notesAPI.OSMemoryFree(hToken);
		}
	}
	
	/**
	 * SSO token data class
	 * 
	 * @author Karsten Lehmann
	 */
	public static class NotesSSOToken {
		private String m_name;
		private List<String> m_domains;
		private boolean m_secureOnly;
		private String m_data;
		private NotesTimeDate m_renewalDate;
		
		/**
		 * Creates a new instance
		 * 
		 * @param name name
		 * @param domains DNS domains
		 * @param secureOnly true to recommend that the token only be set on a secure connection
		 * @param data token data
		 * @param renewalDate
		 */
		private NotesSSOToken(String name, List<String> domains, boolean secureOnly, String data, NotesTimeDate renewalDate) {
			m_name = name;
			m_domains = domains;
			m_secureOnly = secureOnly;
			m_data = data;
			m_renewalDate = renewalDate;
		}
		
		public String getName() {
			return m_name;
		}
		
		public List<String> getDomains() {
			return m_domains;
		}
		
		public boolean isSecureOnly() {
			return m_secureOnly;
		}
		
		public String getData() {
			return m_data;
		}
		
		public NotesTimeDate getRenewalDate() {
			return m_renewalDate;
		}
		
		/**
		 * Produces a string to be used for the "LtpaToken" cookie for
		 * every domain
		 * 
		 * @return cookie strings
		 */
		public List<String> toHTTPCookieStrings() {
			final String DOMAIN_STRING = ";domain=";
			final String PATH_STRING = ";Path=/";
			final String SECURE_ONLY = ";secure";
			
			List<String> cookieStrings = new ArrayList<String>();
			
			StringBuilder sb = new StringBuilder();
			
			for (String currDomain : m_domains) {
				sb.append(m_data);
				sb.append(DOMAIN_STRING);
				sb.append(currDomain);
				sb.append(PATH_STRING);
				if (m_secureOnly) {
					sb.append(SECURE_ONLY);
				}
				
				cookieStrings.add(sb.toString());
				sb.setLength(0);
			}
			
			return cookieStrings;
		}
	}

}
