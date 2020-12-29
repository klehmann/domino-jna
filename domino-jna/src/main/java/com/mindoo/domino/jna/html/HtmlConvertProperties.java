package com.mindoo.domino.jna.html;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Properties to control the fidality of the richtext-html conversion
 */
public class HtmlConvertProperties {
	private String userAgent;
	private HtmlLinkHandling linkHandling;
	private Map<String,String> options;
	
	public HtmlConvertProperties() {
		options = new HashMap<>();
		// use a different link handling value by default, because
		// the default setting did not produce any output for doclinks
		// in the local Notes Client
		linkHandling = HtmlLinkHandling.FOREIGN_LINKS_DIRECT;
	}
	
	public String getUserAgent() {
		return userAgent;
	}
	
	public HtmlConvertProperties setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}
	
	public HtmlLinkHandling getLinkHandling() {
		return linkHandling;
	}
	
	public HtmlConvertProperties setLinkHandling(HtmlLinkHandling linkHandling) {
		this.linkHandling = linkHandling;
		return this;
	}
	public HtmlConvertProperties options(Map<String,String> options) {
		this.options.putAll(options);
		return this;
	}
	
	/**
	 * Sets a HTML convert option to "1"
	 * 
	 * @param option option
	 * @return properties instance
	 */
	public HtmlConvertProperties option(HtmlConvertOption option) {
		return option(option, "1");
	}
	
	/**
	 * Sets a HTML convert option to the specified value
	 * 
	 * @param option option
	 * @param value value
	 * @return properties instance
	 */
	public HtmlConvertProperties option(HtmlConvertOption option, String value) {
		this.options.put(option.toString(), value);
		return this;
	}

	/**
	 * Sets the value of multiple HTML convert options to the specified value
	 * (e.g. {@link HtmlConvertOption#allSpecs()} or {@link HtmlConvertOption#allTags()}).
	 * 
	 * @param option option
	 * @param value value
	 * @return properties instance
	 */
	public HtmlConvertProperties options(Collection<HtmlConvertOption> options, String value) {
		for (HtmlConvertOption currOption : options) {
			option(currOption, value);
		}
		return this;
	}
	
	/**
	 * Sets a HTML convert option to "1"
	 * 
	 * @param option option as string value
	 * @return properties instance
	 */
	public HtmlConvertProperties option(String option) {
		return option(option, "1");
	}
	
	/**
	 * Sets a HTML convert option to the specified value
	 * 
	 * @param option option as string value
	 * @param value value
	 * @return properties instance
	 */
	public HtmlConvertProperties option(String option, String value) {
		this.options.put(option, value);
		return this;
	}

	public Map<String,String> getOptions() {
		return Collections.unmodifiableMap(this.options);
	}

	/**
	 * Indicates how LINKs should be handled.<br>
	 * In particular LINKs to objects in databases not on the same server as the
	 * document being converted (known as "foreign" links)
	 * 
	 * @author Karsten Lehmann
	 */
	public static enum HtmlLinkHandling {
		/**
		 * Default behavior: Links handled as they are normally in the web server.<br>
		 * For foreign links, the URL has a dummy database and a "RedirectTo" command,
		 * with the link information in an argument
		 */
		FOREIGN_LINKS_REDIRECTED((short)0),
		/**
		 * for all links, link info is placed directly into URL. For foreign links
		 * the database id, view id, and document id are placed in the URL and the
		 * server (if present) in the "Name" argument
		 */
		FOREIGN_LINKS_DIRECT((short)1);
		
		private short m_value;
		
		private HtmlLinkHandling(short value) {
			m_value = value;
		}
		
		public short getValue() {
			return m_value;
		}
	}
}