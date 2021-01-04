package com.mindoo.domino.jna.html;

import java.util.HashSet;
import java.util.Set;

import com.mindoo.domino.jna.html.HtmlConvertProperties.HtmlLinkHandling;

/**
 * An enum with existing HTML conversion options to be set via
 * {@link HtmlConvertProperties#option(HtmlConvertOption, String)}.<br>
 * <br>
 * Please note that we provide an additional open method to set further undocumented options
 * via {@link HtmlConvertProperties#option(String, String)}.<br>
 * <br>
 * See these blog articles to get an idea what can be done with these options:<br>
 * <a href="http://bobzblog.com/tuxedoguy.nsf/dx/what-if.....we-didnt-use-font-tags-in-our-html-output-anymore?opendocument&comments">What if.....we didn’t use &lt;font&gt; tags in our HTML output anymore?</a><br>
 * <a href="https://www.intec.co.uk/domino-html-generation-and-fontconversion1/">Domino HTML generation and FontConversion=1</a><br>
 * <br>
 * Possible values for the "spec" and "tag" attributes to control HTML output (e.g. {@link #FontSizeSpec}):<br>
 * <ul>
 * <li>0 (none) - do not generate anything for the spec</li>
 * <li>1 (native) - use the default HTML generation for the spec</li>
 * <li>2 (outer span tag with style attribute) - surround any native tag with a &lt;span&gt; tag that specifies a style attribute, e.g. &lt;span style="font-family: sans-serif ; "&gt;........&lt;/span&gt;</li>
 * <li>4 (inner font tag with style attribute) - use a &lt;font&gt; tag with a style attribute inside any native tag, e.g. == &lt;font style="font-family: sans-serif ; "&gt;........&lt;/font&gt;</li>
 * <li>8 (native tag with style attribute - only applicable if there is a native tag) - use the native tag with a style attribute, e.g. &lt;tt style="font-family: monospace"&gt;........&lt;/tt&gt;</li>
 * <li>16 (outer span tag with class attribute) - surround any native tag with a &lt;span&gt; tag that specifies a class attribute, e.g. &lt;span class="domino-font-sansserif "&gt;........&lt;/span&gt;</li>
 * <li>32 (native tag with class attribute - only applicable if there is a native tag) - use the native tag with a class attribute, e.g. &lt;tt class="domino-font-monospace"&gt;........&lt;/tt&gt;</li>
 * </ul>
 */
public enum HtmlConvertOption {
	/** Forces all sections to be expanded, regardless of their expansion in the Notes® rich text fields. */
	ForceSectionExpand,
	/** Forces alternate formatting of tables with tabbed sections.<br>
	 * All of the tabs are displayed at the same time, one below the other, with the tab labels included as headers. */
	RowAtATimeTableAlt,
	/** Forces all outlines to be expanded, regardless of their expansion in the Notes rich text. */
	ForceOutlineExpand,
	/** Disables passthru HTML, treating the HTML as plain text. */
	DisablePassThruHTML,
	/** Preserves Notes intraline whitespace (spaces between characters). */
	TextExactSpacing,
	/** enable new code for better representation of indented lists */
	ListFidelity,
	
	/** allow multiple embedded view if: 1) page is in raw mode; 2) view is marked render as html */
	MultiEmbeddedHTMLView,
	/** If set, converts tabs to the number of spaces specified. */
	TextConvertTabsToSpaces,
	/** run the ActiveContentfilter on open item (useful for HAPI) */
	ACFOnItem,
	/** automatically add class names. */
	AutoClass,
	/** add classes to field names - experimental */
	AutoClassField,
	/** add field name as inner span */
	FieldName,
	/** use xpages style theme files (reduced features from xpages) */
	Themes,
	/** generate &lt;label&gt; around input tags for checkboxes and radio buttons */
	FieldChoiceLabel,
	/** use the TITLE attribute of the TABLE as the &lt;caption&gt; */
	TableCaptionFromTitle,
	/** alternate Section representation (no dynamics) */
	SectionAlt,
	/** CD-&gt;HTML attachments don't have &lt;a&gt;&lt;img&gt;, just text saying "see attachment..." */
	AttachmentLink,
	/** alternate reps for table style ; 1 = dropm ec blank */
	TableStyle,
	/** include a notes:// url when generating doclink urls */
	OfferNotesURLInLink,
	/** similar to AttachmentLink, but for when the Item is type MIME separate option just to not
	 * interfere with already released behavior of the other option.<br>
	 * This option only has effect if the internal logic of the mime processor would have
	 * otherwise produced attachment links.<br>
	 * For example, explicitly setting this to 0 will NOT cause attachment links to appear
	 * if the internal logic has determined that attachment links will not appear. */
	MIMEAttachmentLink,
	/** Add bits of the link object to the link url. */
	LinkArg,
	/** Force using redirect URLs regardless of NAB setting. This is just to create the
	 * URLs not to process them. */
	ForceRedirectURL,
	/** works like setting {@link HtmlLinkHandling#FOREIGN_LINKS_DIRECT}
	 * via {@link HtmlConvertProperties#setLinkHandling(HtmlLinkHandling)} */
	LinkHandling,
	/** alternate html for links */
	LinkHTMLAlt,
	MIMEItemFrame,
	/** for FontConverstion = 0, render the notes highlight text */
	RenderHighlight,
	/** 0 will use &lt;ul&gt; for indenting, 1 will use &lt;div&gt; */
	ParagraphIndent,
	/** flags (see below) to control trimming of keyword values on fields */
	FieldKeywordTrim,
	/** Enable XML-compatible HTML output */
	XMLCompatibleHTML,

	/** FontConversion - the master option<br>
	 * <ul>
	 * <li>0 (classic) - the original web server handling</li>
	 * <li>1 (style) - use combinations of span and other new tags, as controlled by the remaining options.</li>
	 * </ul> */
	FontConversion,

	FontSizeBase,

	//set these to "2" to get modern inline CSS attributes
	FontColorSpec,
	FontFaceSerifSpec,
	FontFaceSansserifSpec,
	FontFaceMonospaceSpec,
	FontFaceSpecificSpec,
	FontSizeSpec,
	FontStyleBoldSpec,
	FontStyleItalicSpec,
	FontStyleUnderlineSpec,
	FontStyleStrikethroughSpec,

	//set these to "0" to remove old font tags from the output
	FontFaceMonospaceTag,
	FontStyleBoldTag,
	FontStyleItalicTag,
	FontStyleUnderlineTag,
	FontStyleStrikethroughTag;
	
	/**
	 * Returns all "spec" options, e.g. {@link #FontColorSpec}
	 * 
	 * @return spec options
	 */
	public static Set<HtmlConvertOption> allSpecs() {
		HashSet<HtmlConvertOption> specs = new HashSet<>();
		specs.add(FontColorSpec);
		specs.add(FontFaceSerifSpec);
		specs.add(FontFaceSansserifSpec);
		specs.add(FontFaceMonospaceSpec);
		specs.add(FontFaceSpecificSpec);
		specs.add(FontSizeSpec);
		specs.add(FontStyleBoldSpec);
		specs.add(FontStyleItalicSpec);
		specs.add(FontStyleUnderlineSpec);
		specs.add(FontStyleStrikethroughSpec);

		return specs;
	}
	
	/**
	 * Returns all "tag" options, e.g. {@link #FontStyleBoldTag}
	 * 
	 * @return tag options
	 */
	public static Set<HtmlConvertOption> allTags() {
		HashSet<HtmlConvertOption> tags = new HashSet<>();
		tags.add(FontFaceMonospaceTag);
		tags.add(FontStyleBoldTag);
		tags.add(FontStyleItalicTag);
		tags.add(FontStyleUnderlineTag);
		tags.add(FontStyleStrikethroughTag);

		return tags;
	}
}
