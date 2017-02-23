package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.List;

/**
 * SSO token data class
 * 
 * @author Karsten Lehmann
 */
public class NotesSSOToken {
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
	 * @param renewalDate optional date when the token needs a renewal (if specified on token generation)
	 */
	public NotesSSOToken(String name, List<String> domains, boolean secureOnly, String data, NotesTimeDate renewalDate) {
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
		final String DOMAIN_STRING = ";Domain=";
		final String PATH_STRING = ";Path=/";
		final String SECURE_ONLY = ";Secure";
		
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