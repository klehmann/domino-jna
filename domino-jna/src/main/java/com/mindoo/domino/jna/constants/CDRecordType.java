package com.mindoo.domino.jna.constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Enum with all available CD record types, which are the building blocks of Notes Richtext.
 * 
 * @author Karsten Lehmann
 */
public enum CDRecordType {
	
	/* Signatures for Composite Records in items of data type COMPOSITE */
	
	SRC(NotesConstants.SIG_CD_SRC, 1),
	IMAGEHEADER2(NotesConstants.SIG_CD_IMAGEHEADER2, 1),
	PDEF_MAIN(NotesConstants.SIG_CD_PDEF_MAIN, 1),
	PDEF_TYPE(NotesConstants.SIG_CD_PDEF_TYPE, 1),
	PDEF_PROPERTY(NotesConstants.SIG_CD_PDEF_PROPERTY, 1),
	PDEF_ACTION(NotesConstants.SIG_CD_PDEF_ACTION, 1),
	/** This definition was added to provide a unique signature for table cell dataflags.<br>
	 * As an alternative, the standard CDDATAFLAGS structure can also be used. */
	TABLECELL_DATAFLAGS(NotesConstants.SIG_CD_TABLECELL_DATAFLAGS, 1),
	/** This CD record defines properties of an embedded contact list */
	EMBEDDEDCONTACTLIST(NotesConstants.SIG_CD_EMBEDDEDCONTACTLIST, 1),
	/** Used to ignore a block of CD records for a particular version of Notes */
	IGNORE(NotesConstants.SIG_CD_IGNORE, 1),
	TABLECELL_HREF2(NotesConstants.SIG_CD_TABLECELL_HREF2, 1),
	HREFBORDER(NotesConstants.SIG_CD_HREFBORDER, 1),
	/** This record was added because the Pre Table Begin Record can not be expanded and R6 required
	 * more data to be stored.<br>
	 * This CD record specifies extended table properties */
	TABLEDATAEXTENSION(NotesConstants.SIG_CD_TABLEDATAEXTENSION, 1),
	/** This CD record defines properties of an embedded calendar control (date picker). */
	EMBEDDEDCALCTL(NotesConstants.SIG_CD_EMBEDDEDCALCTL, 1),
	/** New field attributes have been added in Notes/Domino 6.<br>
	 * To preserve compatibility with existing applications, the new attributes have been
	 * placed in this extension to the CDACTION record.  This record is optional, and may not
	 * be present in the $Body item of the form note */
	ACTIONEXT(NotesConstants.SIG_CD_ACTIONEXT, 1),
	EVENT_LANGUAGE_ENTRY(NotesConstants.SIG_CD_EVENT_LANGUAGE_ENTRY, 1),
	/** This structure defines the file segment data of a Cascading Style Sheet (CSS) and
	 * follows a CDFILEHEADER structure */
	FILESEGMENT(NotesConstants.SIG_CD_FILESEGMENT, 1),
	/** This structure is used to define a Cascading Style Sheet (CSS) that is part of a Domino database. */
	FILEHEADER(NotesConstants.SIG_CD_FILEHEADER, 1),
	/** Contains collapsible section, button type, style sheet or field limit information for Notes/Domino 6.
	 * A CD record (CDBAR, CDBUTTON, CDBORDERINFO, CDFIELDHINT, etc.) may be followed by a CDDATAFLAGS structure. */
	DATAFLAGS(NotesConstants.SIG_CD_DATAFLAGS, 1),
	/** This CD Record gives information pertaining to Background Properties for a box. A
	 * CDBACKGROUNDPROPERTIES record may be encapsulated within a CDBEGINRECORD and CDENDRECORD.  */
	BACKGROUNDPROPERTIES(NotesConstants.SIG_CD_BACKGROUNDPROPERTIES, 1),
	EMBEDEXTRA_INFO(NotesConstants.SIG_CD_EMBEDEXTRA_INFO, 1),
	CLIENT_BLOBPART(NotesConstants.SIG_CD_CLIENT_BLOBPART, 1),
	CLIENT_EVENT(NotesConstants.SIG_CD_CLIENT_EVENT, 1),
	BORDERINFO_HS(NotesConstants.SIG_CD_BORDERINFO_HS, 1),
	LARGE_PARAGRAPH(NotesConstants.SIG_CD_LARGE_PARAGRAPH, 1),
	EXT_EMBEDDEDSCHED(NotesConstants.SIG_CD_EXT_EMBEDDEDSCHED, 1),
	/** This CD record contains size information for a layer box. The units (pixels, twips, etc.)
	 * for the Width and Height are set in the "Units" members of the "Top", "Left", "Bottom"
	 * and "Right" members of the CDPOSITIONING structure.  */
	BOXSIZE(NotesConstants.SIG_CD_BOXSIZE, 1),
	/** This CD record contains position information for a layer box. */
	POSITIONING(NotesConstants.SIG_CD_POSITIONING, 1),
	/** The definition for a layer on a form is stored as CD records in the $Body item of the form note.<br>
	 * A layer is comprised of a Layer Object Run (pointer to box that represents the layer), Box Run and Position Data.  */
	LAYER(NotesConstants.SIG_CD_LAYER, 1),
	/** This CD Record gives information pertaining to data connection resource information in a field or form. */
	DECSFIELD(NotesConstants.SIG_CD_DECSFIELD, 1),
	SPAN_END(NotesConstants.SIG_CD_SPAN_END, 1),
	SPAN_BEGIN(NotesConstants.SIG_CD_SPAN_BEGIN, 1),
	/** A field or a run of rich text may contain language information. This language information is stored in
	 * a $TEXTPROPERTIES item. The start or end of a span of language information is indicated by a
	 * CDSPANRECORD structure. The $TEXTPROPERTIES item and the CDSPANRECORD structures may be stored on a
	 * form note and/or a document. */
	TEXTPROPERTIESTABLE(NotesConstants.SIG_CD_TEXTPROPERTIESTABLE, 1),
	HREF2(NotesConstants.SIG_CD_HREF2, 1),
	BACKGROUNDCOLOR(NotesConstants.SIG_CD_BACKGROUNDCOLOR, 1),
	/** This CD Record gives information pertaining to shared resources and/or shared code in a form.<br>
	 * A CDINLINE record may be preceded by a CDBEGINRECORD and followed by a CDRESOURCE and then a CDENDRECORD. */
	INLINE(NotesConstants.SIG_CD_INLINE, 1),
	V6HOTSPOTBEGIN_CONTINUATION(NotesConstants.SIG_CD_V6HOTSPOTBEGIN_CONTINUATION, 1),
	TARGET_DBLCLK(NotesConstants.SIG_CD_TARGET_DBLCLK, 1),
	/** This CD record defines the properties of a caption for a grapic record.  */
	CAPTION(NotesConstants.SIG_CD_CAPTION, 1),
	/** Color properties to various HTML Links. */
	LINKCOLORS(NotesConstants.SIG_CD_LINKCOLORS, 1),
	TABLECELL_HREF(NotesConstants.SIG_CD_TABLECELL_HREF, 1),
	/** This CD record defines the Action Bar attributes.  It is an extension of the CDACTIONBAR record.<br>
	 * It is found within a $V5ACTIONS item and is preceded by a CDACTIONBAR record. */
	ACTIONBAREXT(NotesConstants.SIG_CD_ACTIONBAREXT, 1),
	/** This CD record describes the HTML field properties, ID, Class, Style, Title, Other and Name associated for any given field defined within a Domino Form */
	IDNAME(NotesConstants.SIG_CD_IDNAME, 1),
	TABLECELL_IDNAME(NotesConstants.SIG_CD_TABLECELL_IDNAME, 1),
	/** This structure defines the image segment data of a JPEG or GIF image and follows a CDIMAGEHEADER structure.<br>
	 * The number of segments in the image is contained in the CDIMAGEHEADER and specifies the number of
	 * CDIMAGESEGMENT structures to follow.  An image segment size is 10250 bytes. */
	IMAGESEGMENT(NotesConstants.SIG_CD_IMAGESEGMENT, 1),
	/** This structure is used to define a JPEG or GIF Image that is part of a Domino document.<br>
	 * The CDIMAGEHEADER structure follows a CDGRAPHIC structure.<br>
	 * CDIMAGESEGMENT structure(s) then follow the CDIMAGEHEADER. */
	IMAGEHEADER(NotesConstants.SIG_CD_IMAGEHEADER, 1),
	V5HOTSPOTBEGIN(NotesConstants.SIG_CD_V5HOTSPOTBEGIN, 1),
	V5HOTSPOTEND(NotesConstants.SIG_CD_V5HOTSPOTEND, 1),
	/** This CD record contains language information for a field or a run of rich text. */
	TEXTPROPERTY(NotesConstants.SIG_CD_TEXTPROPERTY, 1),
	/** This structure defines the start of a new paragraph within a rich-text field.<br>
	 * Each paragraph in a rich text field may have different style attributes, such as indentation
	 * and interline spacing. Use this structure when accessing a rich text field at the level of the CD records. */
	PARAGRAPH(NotesConstants.SIG_CD_PARAGRAPH, 1),
	/** This structure specifies a format for paragraphs in a rich-text field. There may be more than one paragraph
	 * using the same paragraph format, but there may be no more than one CDPABDEFINITION with the same
	 * ID in a rich-text field. */
	PABDEFINITION(NotesConstants.SIG_CD_PABDEFINITION, 1),
	/** This structure is placed at the start of each paragraph in a rich-text field, and specifies which
	 * CDPABDEFINITION is used as the format for the paragraph. */
	PABREFERENCE(NotesConstants.SIG_CD_PABREFERENCE, 1),
	/** This structure defines the start of a run of text in a rich-text field. */
	TEXT(NotesConstants.SIG_CD_TEXT, 1),
	XML(NotesConstants.SIG_CD_XML, 1),
	/** Contains the header or footer used in a document. */
	HEADER(NotesConstants.SIG_CD_HEADER, 1),
	/** This structure is used to create a document link in a rich text field.<br>
	 * It contains all the information necessary to open the specified document from any database on any server. */
	LINKEXPORT2(NotesConstants.SIG_CD_LINKEXPORT2, 1),
	/** A rich text field may contain a bitmap image.  There are three types, monochrome, 8-bit mapped color,
	 * and 16-bit color;  a gray scale bitmap is stored as an 8-bit color bitmap with a color table
	 * having entries [0, 0, 0], [1, 1, 1], . . . , [255, 255, 255].  All bitmaps are stored as a single
	 * plane (some graphics devices support multiple planes). */
	BITMAPHEADER(NotesConstants.SIG_CD_BITMAPHEADER, 1),
	/** The bitmap data is divided into segments to optimize data storage within Domino.<br>
	 * It is recommended that each segment be no larger than 10k bytes.  For best display speed,
	 * the segments sould be as large as possible, up to the practical 10k limit.  A scanline must
	 * be contained within a single segment, and cannot be divided between two segments.  A bitmap
	 * must contain at least one segment, but may have many segments. */
	BITMAPSEGMENT(NotesConstants.SIG_CD_BITMAPSEGMENT, 1),
	/** A color table is one of the optional records following a CDBITMAPHEADER record.<br>
	 * The color table specifies the mapping between 8-bit bitmap samples and 24-bit Red/Green/Blue colors. */
	COLORTABLE(NotesConstants.SIG_CD_COLORTABLE, 1),
	/** The CDGRAPHIC record contains information used to control display of graphic objects in a document.<br>
	 * This record marks the beginning of a composite graphic object, and must be present for any graphic
	 * object to be loaded or displayed. */
	GRAPHIC(NotesConstants.SIG_CD_GRAPHIC, 1),
	/** A portion of a Presentation Manager GPI metafile.  This record must be preceded by a CDPMMETAHEADER record.<br>
	 * Since metafiles can be large, but Domino and Notes have an internal limit of 65,536 bytes (64kB) for a
	 * segment, a metafile may be divided into segments of up to 64kB;<br>
	 * each segment must be preceded by a CDPMMETASEG record. */
	PMMETASEG(NotesConstants.SIG_CD_PMMETASEG, 1),
	/** A portion of a Windows GDI metafile.  This record must be preceded by a CDWINMETAHEADER record.<br>
	 * Since Windows GDI metafiles can be large, but Domino and Notes have an internal limit of 65,536 bytes
	 * (64kB) for a segment, a metafile may be divided into segments of up to 64kB;  each segment must be
	 * preceded by a CDWINMETASEG record. */
	WINMETASEG(NotesConstants.SIG_CD_WINMETASEG, 1),
	/** A portion of a Macintosh metafile.  This record must be preceded by a CDMACMETAHEADER record.<br>
	 * Since metafiles can be large, but Domino and Notes have an internal limit of 65,536 bytes (64kB)
	 * for a segment, a metafile may be divided into segments of up to 64kB;  each segment must be
	 * preceded by a CDMACMETASEG record. */
	MACMETASEG(NotesConstants.SIG_CD_MACMETASEG, 1),
	/** Identifies a CGM metafile embedded in a rich text field.  This record must be preceded by a CDGRAPHIC record.<br>
	 * A CDCGMMETA record may contain all or part of a CGM metafile, and is limited to 65,536 bytes (64K). */
	CGMMETA(NotesConstants.SIG_CD_CGMMETA, 1),
	/** Identifies a Presentation Manger GPI metafile embedded in a rich text field.<br>
	 * This record must be preceded by a CDGRAPHIC record.  Since metafiles can be large, but Domino
	 * and Notes have an internal limit of 65,536 bytes (64kB) for a segment, a metafile may be divided
	 * into segments of up to 64kB;  each segment must be preceded by a CDPMMETASEG record. */
	PMMETAHEADER(NotesConstants.SIG_CD_PMMETAHEADER, 1),
	/** Identifies a Windows Graphics Device Interface (GDI) metafile embedded in a rich text field.<br>
	 * This record must be preceded by a CDGRAPHIC record.<br>
	 * Since Windows GDI metafiles can be large, but Domino and Notes have an internal limit of
	 * 65,536 bytes (64kB) for a segment, a metafile may be divided into segments of up to 64kB;<br>
	 * each segment must be preceded by a CDWINMETASEG record. */
	WINMETAHEADER(NotesConstants.SIG_CD_WINMETAHEADER, 1),
	/** Identifies a Macintosh metafile embedded in a rich text field.<br>
	 * This record must be preceded by a CDGRAPHIC record.<br>
	 * Since metafiles can be large, but Domino and Notes has an internal limit of 65,536 bytes (64kB)
	 * for a segment, a metafile may be divided into segments of up to 64kB;<br>
	 * each segment must be preceded by a CDMACMETASEG record. */
	MACMETAHEADER(NotesConstants.SIG_CD_MACMETAHEADER, 1),
	/** This structure specifies the beginning of a table.<br>
	 * It contains information about the format and size of the table.<br>
	 * Use this structure when accessing a table in a rich text field.<br>
	 * As of R5, this structure is preceded by a CDPRETABLEBEGIN structure.<br>
	 * The CDPRETABLEBEGIN structure specifies additional table properties. */
	TABLEBEGIN(NotesConstants.SIG_CD_TABLEBEGIN, 1),
	/** This structure specifies the cell of a table.  Use this structure when accessing a table in a rich text field. */
	TABLECELL(NotesConstants.SIG_CD_TABLECELL, 1),
	/** This structure specifies the end of a table. Use this structure when accessing a table in a rich text field. */
	TABLEEND(NotesConstants.SIG_CD_TABLEEND, 1),
	/** This structure stores the style name for a Paragraph Attributes Block (PAB). */
	STYLENAME(NotesConstants.SIG_CD_STYLENAME, 1),
	/** This structure stores information for an externally stored object. */
	STORAGELINK(NotesConstants.SIG_CD_STORAGELINK, 1),
	/** Bitmap Transparency Table (optionally one per bitmap).  The colors in this table specify the bitmap
	 * colors that are "transparent".<br>
	 * The pixels in the bitmap whose colors are in this table will not affect the background; the
	 * background will "bleed through" into the bitmap.<br>
	 * The entries in the transparency table should be in the same format as entries in the color
	 * table.<br>
	 * If a transparency table is used for a bitmap, it must immediately follow the CDBITMAPHEADER. */
	TRANSPARENTTABLE(NotesConstants.SIG_CD_TRANSPARENTTABLE, 1),
	/** Specifies a horizontal line. */
	HORIZONTALRULE(NotesConstants.SIG_CD_HORIZONTALRULE, 1),
	/** Documents stored on a Lotus Domino server that are viewed via a Web browser may contain
	 * elements that cannot be displayed by the browser.<br>
	 * These elements may have alternate text that the browser will display in their place.<br>
	 * The alternate text is stored in a CDALTTEXT record. */
	ALTTEXT(NotesConstants.SIG_CD_ALTTEXT, 1),
	/** An anchor hotlink points to a specific location in a rich text field of a document.<br>
	 * That target location is identified by a CDANCHOR record containing a specified text string.<br>
	 * When the anchor hotlink is selected by a user, Notes displays the anchor location in the target document.  */
	ANCHOR(NotesConstants.SIG_CD_ANCHOR, 1),
	/** Text in a rich-text field can have the "Pass-Thru HTML" attribute.<br>
	 * Pass-through HTML text is not translated to the Domino rich text format.<br>
	 * Pass-through HTML text is marked by CDHTMLBEGIN and CDHTMLEND records. */
	HTMLBEGIN(NotesConstants.SIG_CD_HTMLBEGIN, 1),
	/** Text in a rich-text field can have the "Pass-Thru HTML" attribute.<br>
	 * Pass-through HTML text is not translated to the Domino rich text format.<br>
	 * Pass-through HTML text is marked by CDHTMLBEGIN and CDHTMLEND records. */
	HTMLEND(NotesConstants.SIG_CD_HTMLEND, 1),
	/** A CDHTMLFORMULA record contains a formula used to generate either an attribute or alternate HTML text for a Java applet. */
	HTMLFORMULA(NotesConstants.SIG_CD_HTMLFORMULA, 1),
	NESTEDTABLEBEGIN(NotesConstants.SIG_CD_NESTEDTABLEBEGIN, 1),
	NESTEDTABLECELL(NotesConstants.SIG_CD_NESTEDTABLECELL, 1),
	NESTEDTABLEEND(NotesConstants.SIG_CD_NESTEDTABLEEND, 1),
	/** This CD Record identifies the paper color for a given document. */
	COLOR(NotesConstants.SIG_CD_COLOR, 1),
	TABLECELL_COLOR(NotesConstants.SIG_CD_TABLECELL_COLOR, 1),
	/** This CD record is used in conjunction with CD record CDEVENT.<br>
	 * If a CDEVENT record has an ActionType of ACTION_TYPE_JAVASCRIPT then CDBLOBPART
	 * contains the JavaScript code.  There may be more then one CDBLOBPART record for each CDEVENT.<br>
	 * Therefore it may be necessary to loop thorough all of the CDBLOBPART records to read in
	 * the complete JavaScript code. */
	BLOBPART(NotesConstants.SIG_CD_BLOBPART, 1),
	/** This CD record defines the beginning of a series of CD Records.<br>
	 * Not all CD records are enclosed within a CDBEGINRECORD/CDENDRECORD combination.  */
	BEGIN(NotesConstants.SIG_CD_BEGIN, 1),
	/** This CD record defines the end of a series of CD records.<br>
	 * Not all CD records are enclosed within a CDBEGINRECORD/CDENDRECORD combination */
	END(NotesConstants.SIG_CD_END, 1),
	/** This CD record allows for additional information to be provided for a graphic. */
	VERTICALALIGN(NotesConstants.SIG_CD_VERTICALALIGN, 1),
	FLOATPOSITION(NotesConstants.SIG_CD_FLOATPOSITION, 1),
	/** This CD record provides the time interval information for tables created where a
	 * different row is displayed within the time interval specified.<br>
	 * This structure is stored just before the CDTABLEEND structure. */
	TIMERINFO(NotesConstants.SIG_CD_TIMERINFO, 1),
	/** This CD record describes the Row Height property for a table. */
	TABLEROWHEIGHT(NotesConstants.SIG_CD_TABLEROWHEIGHT, 1),
	/** This CD Record further defines information for a table.<br>
	 * Specifically the tab and row labels. */
	TABLELABEL(NotesConstants.SIG_CD_TABLELABEL, 1),
	BIDI_TEXT(NotesConstants.SIG_CD_BIDI_TEXT, 1),
	BIDI_TEXTEFFECT(NotesConstants.SIG_CD_BIDI_TEXTEFFECT, 1),
	/** This CD Record is used within mail templates. */
	REGIONBEGIN(NotesConstants.SIG_CD_REGIONBEGIN, 1),
	REGIONEND(NotesConstants.SIG_CD_REGIONEND, 1),
	TRANSITION(NotesConstants.SIG_CD_TRANSITION, 1),
	/** The designer of a form may define a "hint" associated with a field. This descriptive text
	 * is visible in dimmed text within the field when a document is created using the form and
	 * helps the user to know what to select or fill in for the field. This text does not get saved
	 * with the document and disappears when the cursor enters the field. */
	FIELDHINT(NotesConstants.SIG_CD_FIELDHINT, 1),
	/** A CDPLACEHOLDER record stores additional information about various embedded type CD records,
	 * such as CDEMBEDDEDCTL, CDEMBEDDEDOUTLINE and other embedded CD record types defined in HOTSPOTREC_TYPE_xxx. */
	PLACEHOLDER(NotesConstants.SIG_CD_PLACEHOLDER, 1),
	HTMLNAME(NotesConstants.SIG_CD_HTMLNAME, 1),
	/** This CD Record defines the attributes of an embedded outline.<br>
	 * It is preceded by a CDHOTSPOTBEGIN and a CDPLACEHOLDER.<br>
	 * The CD record, CDPLACEHOLDER, further defines the CDEMBEDDEDOUTLINE. */
	EMBEDDEDOUTLINE(NotesConstants.SIG_CD_EMBEDDEDOUTLINE, 1),
	/** This CD Record describes a view as an embedded element.<br>
	 * A CDEMBEDDEDVIEW record will be preceded by a CDPLACEHOLDER record.<br>
	 * Further description of the embedded view can be found in the CD record CDPLACEHOLDER. */
	EMBEDDEDVIEW(NotesConstants.SIG_CD_EMBEDDEDVIEW, 1),
	/** This CD Record gives information pertaining to Background Data for a Table, specifically the 'Cell Image' repeat value. */
	CELLBACKGROUNDDATA(NotesConstants.SIG_CD_CELLBACKGROUNDDATA, 1),
	/** This CD record provides additional table properties, expanding the information provided in CDTABLEBEGIN.<br>
	 * It will only be recognized in Domino versions 5.0 and greater.  This record will be ignored in pre 5.0 versions. */
	PRETABLEBEGIN(NotesConstants.SIG_CD_PRETABLEBEGIN, 1),
	EXT2_FIELD(NotesConstants.SIG_CD_EXT2_FIELD, 1),
	/** This CD record may further define attributes within a CDFIELD such as tab order. */
	EMBEDDEDCTL(NotesConstants.SIG_CD_EMBEDDEDCTL, 1),
	/** This CD record describes border information for a given table.<br>
	 * This CD record will be preceded with CD record CDPRETABLEBEGIN both encapsulated between a
	 * CDBEGINRECORD and a CDENDRECORD record with CD record signature CDPRETABLEBEGIN. */
	BORDERINFO(NotesConstants.SIG_CD_BORDERINFO, 1),
	/** This CD record defines an embedded element of type 'group scheduler'.<br>
	 * It is preceded by a CDHOTSPOTBEGIN and a CDPLACEHOLDER.  The CD record, CDPLACEHOLDER,
	 * further defines the CDEMBEDDEDSCHEDCTL.  */
	EMBEDDEDSCHEDCTL(NotesConstants.SIG_CD_EMBEDDEDSCHEDCTL, 1),
	/** This CD record defines an embedded element of type 'editor'. It is preceded by a
	 * CDHOTSPOTBEGIN and a CDPLACEHOLDER. The CD record, CDPLACEHOLDER, further defines the CDEMBEDDEDEDITCTL */
	EMBEDDEDEDITCTL(NotesConstants.SIG_CD_EMBEDDEDEDITCTL, 1),

	/* Signatures for Frameset CD records */
	
	/** Beginning header record to both a CDFRAMESET and CDFRAME record. */
	FRAMESETHEADER(NotesConstants.SIG_CD_FRAMESETHEADER, 2),
	/** Used to specify an HTML FRAMESET element */
	FRAMESET(NotesConstants.SIG_CD_FRAMESET, 2),
	/** Used to specify an HTML FRAME element */
	FRAME(NotesConstants.SIG_CD_FRAME, 2),
	
	/* Signature for Target Frame info on a link */
	
	/** The CDTARGET structure specifies the target (ie:  the frame) where a resource link hotspot is to be displayed. */
	TARGET(NotesConstants.SIG_CD_TARGET, 3),
	/** Part of a client side image MAP which describes each region in an image and indicates the
	 * location of the document to be retrieved when the defined area is activated.. */
	MAPELEMENT(NotesConstants.SIG_CD_MAPELEMENT, 3),
	/** An AREA element defines the shape and coordinates of a region within a client side image MAP. */
	AREAELEMENT(NotesConstants.SIG_CD_AREAELEMENT, 3),
	HREF(NotesConstants.SIG_CD_HREF, 3),
	HTML_ALTTEXT(NotesConstants.SIG_CD_HTML_ALTTEXT, 3),
	/** Structure which defines simple actions, formulas or LotusScript for an image map.. */
	EVENT(NotesConstants.SIG_CD_EVENT, 3),

	/* Signatures for Composite Records that are reserved internal records, */
	/* whose format may change between releases. */
	
	NATIVEIMAGE(NotesConstants.SIG_CD_NATIVEIMAGE, 4),
	DOCUMENT_PRE_26(NotesConstants.SIG_CD_DOCUMENT_PRE_26, 4),
	/** * OBSOLETE * Defines the attributes of a field in a form. */
	FIELD_PRE_36(NotesConstants.SIG_CD_FIELD_PRE_36, 4),
	/** This defines the structure of a CDFIELD record in the $Body item of a form note.<br>
	 * Each CDFIELD record  defines the attributes of one field in the form. */
	FIELD(NotesConstants.SIG_CD_FIELD, 4),
	/** This defines the structure of the document information field in a form note. A
	 * document information field is an item with name $INFO (ITEM_NAME_DOCUMENT) and
	 * data type TYPE_COMPOSITE. The document information field defines attributes of
	 * documents created with that form. */
	DOCUMENT(NotesConstants.SIG_CD_DOCUMENT, 4),
	METAFILE(NotesConstants.SIG_CD_METAFILE, 4),
	BITMAP(NotesConstants.SIG_CD_BITMAP, 4),
	/** This defines part of the structure of a font table item in a note.<br>
	 * A font table item in a note allows rich text in the note to be displayed using
	 * fonts other than those defined in FONT_FACE_xxx. */
	FONTTABLE(NotesConstants.SIG_CD_FONTTABLE, 4),
	LINK(NotesConstants.SIG_CD_LINK, 4),
	LINKEXPORT(NotesConstants.SIG_CD_LINKEXPORT, 4),
	/** This structure is the header of a record containing the predefined keywords allowed for a field (defined by a CDFIELD record). */
	KEYWORD(NotesConstants.SIG_CD_KEYWORD, 4),
	/** This structure implements a document link in a rich text field.<br>
	 * It contains an index into a Doc Link Reference List.<br>
	 * A Doc Link Reference (a NOTELINK structure) contains all the information
	 * necessary to open the specified document from any database on any server. */
	LINK2(NotesConstants.SIG_CD_LINK2, 4),
	CGM(NotesConstants.SIG_CD_CGM, 4),
	TIFF(NotesConstants.SIG_CD_TIFF, 4),
	/** A pattern table is one of the optional records following a CDBITMAPHEADER record.<br>
	 * The pattern table is used to compress repetitive bitmap data. */
	PATTERNTABLE(NotesConstants.SIG_CD_PATTERNTABLE, 4),
	/** A CD record of this type specifies the start of a DDE link. */
	DDEBEGIN(NotesConstants.SIG_CD_DDEBEGIN, 4),
	/** This structure specifies the end of a DDE link. */
	DDEEND(NotesConstants.SIG_CD_DDEEND, 4),
	/** This structure specifies the start of an OLE Object. */
	OLEBEGIN(NotesConstants.SIG_CD_OLEBEGIN, 4),
	/** This structure specifies the end of an OLE Object in a rich text field. */
	OLEEND(NotesConstants.SIG_CD_OLEEND, 4),
	/** This structure specifies the start of a "hot" region in a rich text field.<br>
	 * Clicking on a hot region causes some other action to occur.<br>
	 * For instance, clicking on a popup will cause a block of text associated
	 * with that popup to be displayed. */
	HOTSPOTBEGIN(NotesConstants.SIG_CD_HOTSPOTBEGIN, 4),
	/** This structure specifies the end of a hot region in a rich text field. */
	HOTSPOTEND(NotesConstants.SIG_CD_HOTSPOTEND, 4),
	/** This structure defines the appearance of a button in a rich text field. */
	BUTTON(NotesConstants.SIG_CD_BUTTON, 4),
	/** This structure defines the appearance of the bar used with collapsible sections. */
	BAR(NotesConstants.SIG_CD_BAR, 4),
	V4HOTSPOTBEGIN(NotesConstants.SIG_CD_V4HOTSPOTBEGIN, 4),
	V4HOTSPOTEND(NotesConstants.SIG_CD_V4HOTSPOTEND, 4),
	EXT_FIELD(NotesConstants.SIG_CD_EXT_FIELD, 4),
	/** The CD record contains Lotus Script object code. */
	LSOBJECT(NotesConstants.SIG_CD_LSOBJECT, 4),
	/** This record is included for future use.<br>
	 * Applications should not generate these records.<br>
	 * Domino and Notes will ignore this record. */
	HTMLHEADER(NotesConstants.SIG_CD_HTMLHEADER, 4),
	/** This record is included for future use.  Applications should not generate these records.<br>
	 * Domino and Notes will ignore this record. */
	HTMLSEGMENT(NotesConstants.SIG_CD_HTMLSEGMENT, 4),
	OLEOBJPH(NotesConstants.SIG_CD_OLEOBJPH, 4),
	MAPIBINARY(NotesConstants.SIG_CD_MAPIBINARY, 4),

	/** The definition for a layout region on a form is stored as CD records in the $Body item of the form note.<br>
	 * The layout region begins with a CDLAYOUT record and ends with a CDLAYOUTEND record.<br>
	 * Other records in the layout region define buttons, graphics, fields, or other rich text elements. */
	LAYOUT(NotesConstants.SIG_CD_LAYOUT, 4),
	/** A text element in a layout region of a form is defined by a CDLAYOUTTEXT record.<br>
	 * This record must be between a CDLAYOUT record and a CDLAYOUTEND record.<br>
	 * This record is usually followed by other CD records identifying text, graphical, or
	 * action elements associated with the element */
	LAYOUTTEXT(NotesConstants.SIG_CD_LAYOUTTEXT, 4),
	/** The CDLAYOUTEND record marks the end of the elements defining a layout region within a form. */
	LAYOUTEND(NotesConstants.SIG_CD_LAYOUTEND, 4),
	/** A field in a layout region of a form is defined by a CDLAYOUTFIELD record.<br>
	 * This record must be between a CDLAYOUT record and a CDLAYOUTEND record.<br>
	 * This record is usually followed by other CD records identifying text, graphical, or
	 * action elements associated with the field. */
	LAYOUTFIELD(NotesConstants.SIG_CD_LAYOUTFIELD, 4),
	/** This record contains the "Hide When" formula for a paragraph attributes block. */
	PABHIDE(NotesConstants.SIG_CD_PABHIDE, 4),
	PABFORMREF(NotesConstants.SIG_CD_PABFORMREF, 4),
	/** The designer of a form or view may define custom actions for that form or view.<br>
	 * The attributes for the button bar are stored in the CDACTIONBAR record in the $ACTIONS
	 * and/or $V5ACTIONS item for the design note describing the form or view. */
	ACTIONBAR(NotesConstants.SIG_CD_ACTIONBAR, 4),
	/** The designer of a form or view may define custom actions associated with that form or view.<br>
	 * Actions may be presented to the user as buttons on the action button bar or as options on the "Actions" menu. */
	ACTION(NotesConstants.SIG_CD_ACTION, 4),
	/** Structure of an on-disk autolaunch item.<br>
	 * Most of the information contained in this structure refers to OLE autolaunching behaviors. */
	DOCAUTOLAUNCH(NotesConstants.SIG_CD_DOCAUTOLAUNCH, 4),
	/** A graphical element in a layout region of a form is defined by a CDLAYOUTGRAPHIC record.<br>
	 * This record must be between a CDLAYOUT record and a CDLAYOUTEND record.<br>
	 * This record is usually followed by other CD records identifying text, graphical, or
	 * action elements associated with the graphical element. */
	LAYOUTGRAPHIC(NotesConstants.SIG_CD_LAYOUTGRAPHIC, 4),
	OLEOBJINFO(NotesConstants.SIG_CD_OLEOBJINFO, 4),
	/** A button in a layout region of a form is defined by a CDLAYOUTBUTTON record.<br>
	 * This record must be between a CDLAYOUT record and a CDLAYOUTEND record.<br>
	 * This record is usually followed by other CD records identifying text, graphical, or
	 * action elements associated with the button. */
	LAYOUTBUTTON(NotesConstants.SIG_CD_LAYOUTBUTTON, 4),
	/** The CDTEXTEFFECT record stores a "special effect" font ID for a run of rich text. */
	TEXTEFFECT(NotesConstants.SIG_CD_TEXTEFFECT, 4),

	/* Signatures for items of type TYPE_VIEWMAP */
	
	VMHEADER(NotesConstants.SIG_CD_VMHEADER, 5),
	VMBITMAP(NotesConstants.SIG_CD_VMBITMAP, 5),
	VMRECT(NotesConstants.SIG_CD_VMRECT, 5),
	VMPOLYGON_BYTE(NotesConstants.SIG_CD_VMPOLYGON_BYTE, 5),
	VMPOLYLINE_BYTE(NotesConstants.SIG_CD_VMPOLYLINE_BYTE, 5),
	VMREGION(NotesConstants.SIG_CD_VMREGION, 5),
	VMACTION(NotesConstants.SIG_CD_VMACTION, 5),
	VMELLIPSE(NotesConstants.SIG_CD_VMELLIPSE, 5),
	VMSMALLTEXTBOX(NotesConstants.SIG_CD_VMSMALLTEXTBOX, 5),
	VMRNDRECT(NotesConstants.SIG_CD_VMRNDRECT, 5),
	VMBUTTON(NotesConstants.SIG_CD_VMBUTTON, 5),
	VMACTION_2(NotesConstants.SIG_CD_VMACTION_2, 5),
	VMTEXTBOX(NotesConstants.SIG_CD_VMTEXTBOX, 5),
	VMPOLYGON(NotesConstants.SIG_CD_VMPOLYGON, 5),
	VMPOLYLINE(NotesConstants.SIG_CD_VMPOLYLINE, 5),
	VMPOLYRGN(NotesConstants.SIG_CD_VMPOLYRGN, 5),
	VMCIRCLE(NotesConstants.SIG_CD_VMCIRCLE, 5),
	VMPOLYRGN_BYTE(NotesConstants.SIG_CD_VMPOLYRGN_BYTE, 5),

	/* Signatures for alternate CD sequences*/
	
	/** Documents converted from HTML (Hyper-Text Markup Language), usually from a World Wide Web source,
	 * may contain "active object" instructions (such as a Java applet).<br>
	 * An active object may have an alternate representation which is to be displayed if the
	 * active object is not supported or is disabled.  When an active object is converted to a
	 * compound text representation, the alternate representation is stored beginning with a
	 * CDALTERNATEBEGIN record and ending with a CDALTERNATEEND record.  An alternate representation
	 * corresponds to the most recent active object with the same ACTIVEOBJECT_TYPE_xxx code, found in the Type field.  */
	ALTERNATEBEGIN(NotesConstants.SIG_CD_ALTERNATEBEGIN, 6),
	/** This record marks the end of a sequence of CD records comprising the alternate representation of
	 * an active object.<br>
	 * For more information, please see the entry for CDALTERNATEBEGIN.  */
	ALTERNATEEND(NotesConstants.SIG_CD_ALTERNATEEND, 6),
	/** This record is simply a marker in an OLE rich text hot spot that indicates that an OLE
	 * object with an associated rich text field (a $OLEObjRichTextField item) was updated by
	 * Release 4.6 or later of Domino or Notes. */
	OLERTMARKER(NotesConstants.SIG_CD_OLERTMARKER, 6);
	
	private static Map<String,CDRecordType> m_recordsByConstant;
	static {
		m_recordsByConstant = new HashMap<String, CDRecordType>();
		for (CDRecordType currType : values()) {
			String key = currType.getConstant()+"|"+currType.getArea();
			m_recordsByConstant.put(key,  currType);
		}
	}
	private int m_area;
	private short m_val;
	
	CDRecordType(short val, int area) {
		m_val = val;
		m_area = area;
	}
	
	public short getConstant() {
		return m_val;
	}
	
	public int getArea() {
		return m_area;
	}
	
	public enum Area {
		/** Signatures for Composite Records in items of data type COMPOSITE */
		TYPE_COMPOSITE,
		/** Signatures for Frameset CD records */
		FRAMESETS,
		/** Signature for Target Frame info on a link */
		TARGET_FRAME,
		/** Signatures for Composite Records that are reserved internal records,
		whose format may change between releases. */
		RESERVED_INTERNAL,
		/** Signatures for items of type TYPE_VIEWMAP */
		TYPE_VIEWMAP,
		/** Signatures for alternate CD sequences*/
		ALTERNATE_SEQ
	}
	
	/**
	 * Looks up an {@link CDRecordType} for the signature WORD value contained
	 * in a CD record.
	 * 
	 * @param constant signature WORD defining the record type
	 * @return a set of {@link CDRecordType} values that have the value {@link #getConstant()} (there may be duplicates like PABHIDE/VMTEXTBOX or ACTION/VMPOLYRGN)
	 */
	public static Set<CDRecordType> getRecordTypesForConstant(short constant) {
		Set<CDRecordType> types = new HashSet<>();
		for (CDRecordType currType : CDRecordType.values()) {
			if (constant == currType.getConstant()) {
				types.add(currType);
			}
		}
		return types;
	}

	/**
	 * Looks up an {@link CDRecordType} for the signature WORD value contained
	 * in a CD record.
	 * 
	 * @param constant signature WORD defining the record type
	 * @param area type of data you are processing, e.g. {@link Area#TYPE_COMPOSITE} for richtext or design element $Body items (required because the same record type constant value is used multiple times)
	 * @return record type or <code>null</code> if unknown constant
	 */
	public static CDRecordType getRecordTypeForConstant(short constant, Area area) {
		switch (area) {
		case TYPE_COMPOSITE:
			return m_recordsByConstant.get(constant + "|1");
		case FRAMESETS:
			return m_recordsByConstant.get(constant + "|2");
		case TARGET_FRAME:
			return m_recordsByConstant.get(constant + "|3");
		case RESERVED_INTERNAL:
			return m_recordsByConstant.get(constant + "|4");
		case TYPE_VIEWMAP:
			return m_recordsByConstant.get(constant + "|5");
		case ALTERNATE_SEQ:
			return m_recordsByConstant.get(constant + "|6");
			default:
				throw new IllegalArgumentException("Unknown area: "+area);
		}
	}
}
