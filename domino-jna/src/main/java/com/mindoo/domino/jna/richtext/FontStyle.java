package com.mindoo.domino.jna.richtext;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.FontId;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesFontIDFieldsStruct;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Container for font styles used in richtext items
 * 
 * @author Karsten Lehmann
 */
public class FontStyle implements IAdaptable {
	/** Font face (FONT_FACE_xxx) */
	private byte m_face;
	/** Attributes (ISBOLD,etc) */
	private byte m_attrib;
	/** Color index (NOTES_COLOR_xxx) */
	private byte m_color;
	/** Size of font in points */
	private byte m_pointSize;
	
	public FontStyle() {
		m_face = NotesConstants.FONT_FACE_SWISS;
		m_attrib = 0;
		m_color = 0;
		m_pointSize = 10;
	}
	
	public FontStyle(IAdaptable adaptable) {
		byte[] values = adaptable.getAdapter(byte[].class);
		if (values!=null && values.length==4) {
			m_face = values[0];
			m_attrib = values[1];
			m_color = values[2];
			m_pointSize = values[3];
		}
		else
			throw new NotesError(0, "Unsupported adaptable parameter");
	}
	
	public FontStyle setPointSize(int size) {
		if (size<0 || size>255) {
			throw new IllegalArgumentException("Point size can only be between 0 and 255 (BYTE data type)");
		}
		m_pointSize = (byte) (size & 0xff);
		return this;
	}
	
	public int getPointSize() {
		return (int) (m_pointSize & 0xff);
	}
	
	public StandardFonts getFontFace() {
		switch (m_face) {
		case 0:
			return StandardFonts.ROMAN;
		case 1:
			return StandardFonts.SWISS;
		case 2:
			return StandardFonts.UNICODE;
		case 3:
			return StandardFonts.USERINTERFACE;
		case 4:
			return StandardFonts.TYPEWRITER;
		default:
			return StandardFonts.CUSTOMFONT;
		}
	}
	
	/**
	 * Returns the current text color
	 * 
	 * @return color
	 */
	public StandardColors getColor() {
		switch (m_color) {
		case NotesConstants.NOTES_COLOR_BLACK:
			return StandardColors.BLACK;
		case NotesConstants.NOTES_COLOR_WHITE:
			return StandardColors.WHITE;
		case NotesConstants.NOTES_COLOR_RED:
			return StandardColors.RED;
		case NotesConstants.NOTES_COLOR_GREEN:
			return StandardColors.GREEN;
		case NotesConstants.NOTES_COLOR_BLUE:
			return StandardColors.BLUE;
		case NotesConstants.NOTES_COLOR_MAGENTA:
			return StandardColors.MAGENTA;
		case NotesConstants.NOTES_COLOR_YELLOW:
			return StandardColors.YELLOW;
		case NotesConstants.NOTES_COLOR_CYAN:
			return StandardColors.CYAN;
		case NotesConstants.NOTES_COLOR_DKRED:
			return StandardColors.DKRED;
		case NotesConstants.NOTES_COLOR_DKGREEN:
			return StandardColors.DKGREEN;
		case NotesConstants.NOTES_COLOR_DKBLUE:
			return StandardColors.DKBLUE;
		case NotesConstants.NOTES_COLOR_DKMAGENTA:
			return StandardColors.DKMAGENTA;
		case NotesConstants.NOTES_COLOR_DKCYAN:
			return StandardColors.DKCYAN;
		case NotesConstants.NOTES_COLOR_DKYELLOW:
			return StandardColors.DKYELLOW;
		case NotesConstants.NOTES_COLOR_GRAY:
			return StandardColors.GRAY;
		case NotesConstants.NOTES_COLOR_LTGRAY:
			return StandardColors.LTGRAY;
		default:
			//should not happen because we only support setting it via setColor(StandardColors)
			return null;
		}
	}
	
	/**
	 * Changes the text color
	 * 
	 * @param color new color
	 * @return this font style instance for chaining
	 */
	public FontStyle setColor(StandardColors color) {
		if (color==null)
			throw new NullPointerException("Color is null");
		m_color = color.getColorConstant();
		return this;
	}
	
	public FontStyle setFontFace(StandardFonts font) {
		if (font==null)
			throw new NullPointerException("Font is null");
		if (font==StandardFonts.CUSTOMFONT)
			throw new IllegalArgumentException("CUSTOMFONT cannot be set as face");
		m_face = font.getFaceConstant();
		return this;
	}
	
	/**
	 * Computes the numeric id containing all font style infos
	 * 
	 * @return font id
	 */
	int getFontId() {
		NotesFontIDFieldsStruct fontIdStruct = NotesFontIDFieldsStruct.newInstance();
		fontIdStruct.Face = m_face;
		fontIdStruct.Attrib = m_attrib;
		fontIdStruct.Color = m_color;
		fontIdStruct.PointSize = m_pointSize;
		fontIdStruct.write();
		return fontIdStruct.getPointer().getInt(0);
	}
	
	/**
	 * These symbols define the standard type faces.
	 * The Face member of the {@link FontStyle} may be either one of these standard font faces,
	 * or a font ID resolved by a font table.
	 * 
	 * @author Karsten Lehmann
	 */
	public static enum StandardFonts {
		/** (e.g. Times Roman family) */
		ROMAN(NotesConstants.FONT_FACE_ROMAN),
		/** (e.g. Helv family) */
		SWISS(NotesConstants.FONT_FACE_SWISS),
		/** (e.g. Monotype Sans WT) */
		UNICODE(NotesConstants.FONT_FACE_UNICODE),
		/** (e.g. Arial */
		USERINTERFACE(NotesConstants.FONT_FACE_USERINTERFACE),
		/** (e.g. Courier family) */
		TYPEWRITER(NotesConstants.FONT_FACE_TYPEWRITER),
		/** returned if font is not in the standard table; cannot be set via {@link FontStyle#setFontFace(StandardFonts)} */
		CUSTOMFONT((byte)255);
		
		private byte m_face;
		
		private StandardFonts(byte face) {
			m_face = face;
		}
		
		public byte getFaceConstant() {
			return m_face;
		}
	}
	
	/**
	 * These symbols are used to specify text color, graphic color and background color in a variety of C API structures.
	 * 
	 * @author Karsten Lehmann
	 */
	public static enum StandardColors {
		BLACK(NotesConstants.NOTES_COLOR_BLACK, 0x00, 0x00, 0x00),
		WHITE(NotesConstants.NOTES_COLOR_WHITE, 0xff, 0xff, 0xff),
		RED(NotesConstants.NOTES_COLOR_RED, 0xff, 0x00, 0x00),
		GREEN(NotesConstants.NOTES_COLOR_GREEN, 0x00, 0xff, 0x00),
		BLUE(NotesConstants.NOTES_COLOR_BLUE, 0x00, 0x00, 0xff),
		MAGENTA(NotesConstants.NOTES_COLOR_MAGENTA, 0xff, 0x00, 0xff),
		YELLOW(NotesConstants.NOTES_COLOR_YELLOW, 0xff, 0xff, 0x00),
		CYAN(NotesConstants.NOTES_COLOR_CYAN, 0x00, 0xff, 0xff),
		DKRED(NotesConstants.NOTES_COLOR_DKRED, 0x80, 0x00, 0x00),
		DKGREEN(NotesConstants.NOTES_COLOR_DKGREEN, 0x00, 0x80, 0x00),
		DKBLUE(NotesConstants.NOTES_COLOR_DKBLUE, 0x00, 0x00, 0x80),
		DKMAGENTA(NotesConstants.NOTES_COLOR_DKMAGENTA, 0x80, 0x00, 0x80),
		DKYELLOW(NotesConstants.NOTES_COLOR_DKYELLOW, 0x80, 0x80, 0x00),
		DKCYAN(NotesConstants.NOTES_COLOR_DKCYAN, 0x00, 0x80, 0x80),
		GRAY(NotesConstants.NOTES_COLOR_GRAY, 0x80, 0x80, 0x80),
		LTGRAY(NotesConstants.NOTES_COLOR_LTGRAY, 0xc0, 0xc0, 0xc0),

		WHITE2(16, 0xff, 0xff, 0xff),
		VANILLA(17, 0xff, 0xef, 0xce),
		PARCHMENT(18, 0xff, 0xff, 0xc2),
		IVORY(19, 0xff, 0xff, 0xd0),
		PALEGREEN(20, 0xe0, 0xff, 0xbf),
		SEAMIST(21, 0xe0, 0xff, 0xdf),
		ICEBLUE(22, 0xe0, 0xff, 0xff),
		POWDERBLUE(23, 0xc2, 0xef, 0xff),
		ARTICBLUE(24, 0xe0, 0xf1, 0xff),
		LILACMIST(25, 0xe0, 0xe0, 0xff),
		PURPLEWASH(26, 0xe8, 0xe0, 0xff),
		VIOLETFROST(27, 0xf1, 0xe0, 0xff),
		SEASHELL(28, 0xff, 0xe0, 0xff),
		ROLEPEARL(29, 0xff, 0xe0, 0xf5),
		PALECHERRY(30, 0xff, 0xe0, 0xe6),
		WHITE3(31, 0xff, 0xff, 0xff),
		BLUSH(32, 0xff, 0xe1, 0xdc),
		SAND(33, 0xff, 0xe1, 0xb0),
		LIGHTYELLOW(34, 0xff, 0xff, 0x80),
		HONEYDEW(35, 0xf1, 0xf1, 0xb4),
		CELERY(36, 0xc2, 0xff, 0x91),
		PALEAQUA(37, 0xc1, 0xff, 0xd5),
		PALEBLUE(38, 0xc1, 0xff, 0xff),
		CRYSTALBLUE(39, 0xa1, 0xe2, 0xff),
		LIGHTCORNFLOWER(40, 0xc0, 0xe1, 0xff),
		PALELAVENDER(41, 0xbf, 0xbf, 0xff),
		GRAPEFIZZ(42, 0xd2, 0xbf, 0xff),
		PALEPLUM(43, 0xe1, 0xbf, 0xff),
		PALEPINK(44, 0xff, 0xc1, 0xfd),
		PALEROSE(45, 0xff, 0xc0, 0xe4),
		ROSEQUARTZ(46, 0xff, 0xc0, 0xce),
		GRAY5(47, 0xf7, 0xf7, 0xf7),
		REDSAND(48, 0xff, 0xc0, 0xb6),
		BUFF(49, 0xff, 0xc2, 0x81),
		LEMON(50, 0xff, 0xff, 0x35),
		PALELEMONLIME(51, 0xf1, 0xf1, 0x80),
		MINTGREEN(52, 0x80, 0xff, 0x80),
		PASTELGREEN(53, 0x82, 0xff, 0xca),
		PASTELBLUE(54, 0x80, 0xff, 0xff),
		SAPPHIRE(55, 0x82, 0xe0, 0xff),
		CORNFLOWER(56, 0x82, 0xc0, 0xff),
		LIGHTLAVENDER(57, 0x9f, 0x9f, 0xff),
		PALEPURPLE(58, 0xc2, 0x9f, 0xff),
		LIGHTORCHID(59, 0xe2, 0x9f, 0xff),
		PINKORCHID(60, 0xff, 0x9f, 0xff),
		APPLEBLOSSOM(61, 0xff, 0x9f, 0xcf),
		PINKCORAL(62, 0xff, 0x9f, 0xa9),
		GRAY10(63, 0xef, 0xef, 0xef),
		LIGHTSALMON(64, 0xff, 0x9f, 0x9f),
		LIGHTPEACH(65, 0xff, 0x9f, 0x71),
		YELLOW2(66, 0xff, 0xff, 0x00),
		AVOCADO(67, 0xe0, 0xe0, 0x74),
		LEAFGREEN(68, 0x41, 0xff, 0x32),
		LIGHTAQUA(69, 0x42, 0xff, 0xc7),
		LIGHTTURQUOISE(70, 0x42, 0xff, 0xff),
		LIGHTCERULEAN(71, 0x00, 0xbf, 0xff),
		AZURE(72, 0x52, 0x91, 0xef),
		LAVENDER(73, 0x80, 0x80, 0xff),
		LIGHTPURPLE(74, 0xc0, 0x82, 0xff),
		DUSTYVIOLET(75, 0xe0, 0x81, 0xff),
		PINK(76, 0xff, 0x7f, 0xff),
		PASTELPINK(77, 0xff, 0x82, 0xc2),
		PASTELRED(78, 0xff, 0x82, 0xa0),
		GRAY15(79, 0xe1, 0xe1, 0xe1),
		SALMON(80, 0xff, 0x80, 0x80),
		PEACH(81, 0xff, 0x81, 0x41),
		MUSTARD(82, 0xff, 0xe1, 0x18),
		LEMONLIME(83, 0xe1, 0xe1, 0x40),
		NEONGREEN(84, 0x00, 0xff, 0x00),
		AQUA(85, 0x00, 0xff, 0xb2),
		TURQUOISE(86, 0x00, 0xff, 0xff),
		CERULEAN(87, 0x00, 0xa1, 0xe0),
		WEDGEWOOD(88, 0x21, 0x81, 0xff),
		HEATHER(89, 0x61, 0x81, 0xff),
		PURPLEHAZE(90, 0xa1, 0x60, 0xff),
		ORCHID(91, 0xc0, 0x62, 0xff),
		FLAMINGO(92, 0xff, 0x5f, 0xff),
		CHERRYPINK(93, 0xff, 0x60, 0xaf),
		REDCORAL(94, 0xff, 0x60, 0x88),
		GRAY20(95, 0xd2, 0xd2, 0xd2),
		DARKSALMON(96, 0xff, 0x40, 0x40),
		DARKPEACH(97, 0xff, 0x42, 0x1e),
		GOLD(98, 0xff, 0xbf, 0x18),
		YELLOWGREEN(99, 0xe1, 0xe1, 0x00),
		LIGHTGREEN(100, 0x00, 0xe1, 0x00),
		CARIBBEAN(101, 0x00, 0xe1, 0xad),
		DARKPASTELBLUE(102, 0x00, 0xe0, 0xe0),
		DARKCERULEAAN(103, 0x00, 0x82, 0xbf),
		MANGANESEBLUE(104, 0x00, 0x80, 0xff),
		LILAC(105, 0x41, 0x81, 0xff),
		PURPLE(106, 0x82, 0x42, 0xff),
		LIGHTREDVIOLET(107, 0xc1, 0x40, 0xff),
		LIGHTMAGENTA(108, 0xff, 0x42, 0xf9),
		ROSE(109, 0xff, 0x40, 0xa0),
		CARNATIONPINK(110, 0xff, 0x40, 0x70),
		GRAY25(111, 0xc0, 0xc0, 0xc0),
		WATERMELON(112, 0xff, 0x1f, 0x35),
		TANGERINE(113, 0xff, 0x1f, 0x10),
		ORANGE(114, 0xff, 0x81, 0x00),
		CHARTREUSE(115, 0xbf, 0xbf, 0x00),
		GREEN2(116, 0x00, 0xc2, 0x00),
		TEAL(117, 0x00, 0xc1, 0x96),
		DARKTURQUOISE(118, 0x00, 0xc1, 0xc2),
		LIGHTSLATEBLUE(119, 0x41, 0x81, 0xc0),
		MEDIUMBLUE(120, 0x00, 0x62, 0xe1),
		DARKLILAC(121, 0x41, 0x41, 0xff),
		ROYALPURPLE(122, 0x42, 0x00, 0xff),
		FUCHSIA(123, 0xc2, 0x00, 0xff),
		CONFETTIPINK(124, 0xff, 0x22, 0xff),
		PALEBURGUNDY(125, 0xf5, 0x2b, 0x97),
		STRAWBERRY(126, 0xff, 0x22, 0x59),
		GRAY30(127, 0xb2, 0xb2, 0xb2),
		ROUGE(128, 0xe0, 0x1f, 0x25),
		BURNTORANGE(129, 0xe1, 0x20, 0x00),
		DARKORANGE(130, 0xe2, 0x62, 0x00),
		LIGHTOLIVE(131, 0xa1, 0xa1, 0x00),
		KELLYGREEN(132, 0x00, 0xa0, 0x00),
		SEAGREEN(133, 0x00, 0x9f, 0x82),
		AZTECBLUE(134, 0x00, 0x80, 0x80),
		DUSTYBLUE(135, 0x00, 0x60, 0xa0),
		BLUEBERRY(136, 0x00, 0x41, 0xc2),
		VIOLET(137, 0x00, 0x21, 0xbf),
		DEEPPURPLE(138, 0x41, 0x00, 0xc2),
		REDVIOLET(139, 0x81, 0x00, 0xff),
		HOTPINK(140, 0xff, 0x00, 0xff),
		DARKROSE(141, 0xff, 0x00, 0x80),
		POPPYRED(142, 0xff, 0x00, 0x41),
		GRAY35(143, 0xa2, 0xa2, 0xa2),
		CRIMSON(144, 0xc2, 0x00, 0x00),
		RED2(145, 0xff, 0x00, 0x00),
		LIGHTBROWN(146, 0xbf, 0x41, 0x00),
		OLIVE(147, 0x80, 0x80, 0x00),
		DARKGREEN2(148, 0x00, 0x80, 0x00),
		DARKTEAL(149, 0x00, 0x82, 0x50),
		SPRUCE(150, 0x00, 0x60, 0x62),
		SLATEBLUE(151, 0x00, 0x40, 0x80),
		NAVYBLUE(152, 0x00, 0x1f, 0xe2),
		BLUEBVIOLET(153, 0x40, 0x40, 0xc2),
		AMETHYST(154, 0x40, 0x00, 0xa2),
		DARKREDVIOLET(155, 0x60, 0x00, 0xa1),
		MAGENTA2(156, 0xe0, 0x00, 0xe0),
		LIGHTBURGUNDY(157, 0xdf, 0x00, 0x7f),
		CHERRYRED(158, 0xc2, 0x00, 0x41),
		GRAY40(159, 0x8f, 0x8f, 0x8f),
		DARKCRIMSON(160, 0xa0, 0x00, 0x00),
		DARKRED2(161, 0xe1, 0x00, 0x00),
		HAZELNUT(162, 0xa1, 0x3f, 0x00),
		DARKOLIVE(163, 0x62, 0x62, 0x00),
		EMERALD(164, 0x00, 0x60, 0x00),
		MALACHITE(165, 0x00, 0x60, 0x3c),
		DARKSPRUCE(166, 0x00, 0x40, 0x41),
		STEELBLUE(167, 0x00, 0x2f, 0x80),
		BLUE2(168, 0x00, 0x00, 0xff),
		IRIS(169, 0x20, 0x20, 0xa0),
		GRAPE(170, 0x22, 0x00, 0xa1),
		PLUM(171, 0x40, 0x00, 0x80),
		DARKMAGENTA2(172, 0xa1, 0x00, 0x9f),
		BURGUNDY(173, 0xc0, 0x00, 0x7f),
		CRANBERRY(174, 0x9f, 0x00, 0x0f),
		GRAY50(175, 0x80, 0x80, 0x80),
		MAHOGANY(176, 0x60, 0x00, 0x00),
		BRICK(177, 0xc2, 0x12, 0x12),
		DARKBROWN(178, 0x82, 0x42, 0x00),
		DEEPOLIVE(179, 0x42, 0x42, 0x00),
		DARKEMERALD(180, 0x00, 0x42, 0x00),
		EVERGREEN(181, 0x00, 0x40, 0x23),
		BALTICBLUE(182, 0x00, 0x32, 0x3f),
		BLUEDENIM(183, 0x00, 0x20, 0x60),
		COBALTBLUE(184, 0x00, 0x20, 0xc2),
		DARKIRIS(185, 0x22, 0x22, 0xc0),
		MIDNIGHT(186, 0x00, 0x00, 0x80),
		DARKPLUM(187, 0x1f, 0x00, 0x7f),
		PLUMRED(188, 0x80, 0x00, 0x80),
		DARKBURGUNDY(189, 0x82, 0x00, 0x40),
		SCARLET(190, 0x80, 0x00, 0x00),
		GRAY60(191, 0x5f, 0x5f, 0x5f),
		CHESTNUT(192, 0x40, 0x00, 0x00),
		TERRACOTTA(193, 0xa1, 0x1f, 0x12),
		UMBER(194, 0x60, 0x42, 0x00),
		AMAZON(195, 0x21, 0x21, 0x00),
		PEACOCKGREEN(196, 0x00, 0x21, 0x00),
		PINE(197, 0x00, 0x20, 0x1f),
		SEALBLUE(198, 0x00, 0x20, 0x41),
		DARKSLATEBLUE(199, 0x00, 0x20, 0x4f),
		ROYALBLUE(200, 0x00, 0x00, 0xe0),
		LAPIS(201, 0x00, 0x00, 0xa1),
		DARKGRAPE(202, 0x00, 0x00, 0x61),
		AUBERGINE(203, 0x1f, 0x00, 0x62),
		DARKPLUMRED(204, 0x40, 0x00, 0x5f),
		RASPBERRY(205, 0x62, 0x00, 0x42),
		DEEPSCARLET(206, 0x62, 0x00, 0x12),
		GRAY70(207, 0x4f, 0x4f, 0x4f),
		REDGRAY(208, 0xd0, 0xb1, 0xa1),
		TAN(209, 0xe0, 0xa1, 0x75),
		KHAKI(210, 0xd2, 0xb0, 0x6a),
		PUTTY(211, 0xc0, 0xc2, 0x7c),
		BAMBOOGREEN(212, 0x82, 0xc1, 0x68),
		GREENGRAY(213, 0x81, 0xc0, 0x97),
		BALTICGRAY(214, 0x7f, 0xc2, 0xbc),
		BLUEGRAY(215, 0x71, 0xb2, 0xcf),
		RAINCLOUD(216, 0xb1, 0xb1, 0xd2),
		LILACGRAY(217, 0x9f, 0x9f, 0xe0),
		LIGHTPURPLEGRAY(218, 0xc0, 0xa1, 0xe0),
		LIGHTMAUVE(219, 0xe2, 0x9f, 0xde),
		LIGHTPLUMGRAY(220, 0xef, 0x91, 0xeb),
		LIGHTBURGUNDYGRAY(221, 0xe2, 0x9f, 0xc8),
		ROSEGRAY(222, 0xf1, 0x8f, 0xbc),
		GRAY80(223, 0x2f, 0x2f, 0x2f),
		DARKREDGRAY(224, 0x7f, 0x60, 0x4f),
		DARKTAN(225, 0xa1, 0x62, 0x52),
		SAFARI(226, 0x80, 0x62, 0x10),
		OLIVEGRAY(227, 0x82, 0x82, 0x3f),
		JADE(228, 0x3f, 0x62, 0x1f),
		DARKGREENGRAY(229, 0x3c, 0x61, 0x3e),
		SPRUCEGRAY(230, 0x37, 0x60, 0x5e),
		DARKBLUEGRAY(231, 0x10, 0x41, 0x60),
		ATLANTICGRAY(232, 0x42, 0x42, 0x82),
		DARKLILACGRAY(233, 0x62, 0x60, 0xa1),
		PURPLEGRAY(234, 0x62, 0x41, 0x81),
		MAUVE(235, 0x60, 0x31, 0x81),
		PLUMGRAY(236, 0x60, 0x21, 0x62),
		BURGUNDYGRAY(237, 0x62, 0x21, 0x52),
		DARKROSEGRAY(238, 0x81, 0x3f, 0x62),
		BLACK2(239, 0x00, 0x00, 0x00);

		private byte color;
		private final short red;
		private final short green;
		private final short blue;

		StandardColors(final int colorIdx, int red, int green, int blue) {
			this.color = (byte) (colorIdx);
			this.red = (short)red;
			this.green = (short)green;
			this.blue = (short)blue;
		}

		public byte getColorConstant() {
			return color;
		}

		public short getGreen() {
			return green;
		}

		public short getRed() {
			return red;
		}

		public short getBlue() {
			return blue;
		}

		public static StandardColors valueOf(byte b) {
			for (StandardColors currColor : values()) {
				if (b == currColor.getColorConstant()) {
					return currColor;
				}
			}
			return BLACK;
		}

	}
	
	public FontStyle setBold(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISBOLD) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISBOLD) & 0xff);
		}
		return this;
	}

	public boolean isBold() {
		return (m_attrib & NotesConstants.ISBOLD) == NotesConstants.ISBOLD;
	}
	
	public FontStyle setItalic(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISITALIC) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISITALIC) & 0xff);
		}
		return this;
	}
	
	public boolean isItalic() {
		return (m_attrib & NotesConstants.ISUNDERLINE) == NotesConstants.ISUNDERLINE;
	}

	public FontStyle setUnderline(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISUNDERLINE) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISUNDERLINE) & 0xff);
		}
		return this;
	}
	
	public boolean isUnderline() {
		return (m_attrib & NotesConstants.ISUNDERLINE) == NotesConstants.ISUNDERLINE;
	}

	public FontStyle setStrikeout(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISSTRIKEOUT) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISSTRIKEOUT) & 0xff);
		}
		return this;
	}

	public boolean isStrikeout() {
		return (m_attrib & NotesConstants.ISSTRIKEOUT) == NotesConstants.ISSTRIKEOUT;
	}

	public FontStyle setSuper(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISSUPER) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISSUPER) & 0xff);
		}
		return this;
	}

	public boolean isSuper() {
		return (m_attrib & NotesConstants.ISSUPER) == NotesConstants.ISSUPER;
	}

	public FontStyle setSub(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISSUB) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISSUB) & 0xff);
		}
		return this;
	}

	public boolean isSub() {
		return (m_attrib & NotesConstants.ISSUB) == NotesConstants.ISSUB;
	}

	public FontStyle setShadow(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISSHADOW) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISSHADOW) & 0xff);
		}
		return this;
	}

	public boolean isShadow() {
		return (m_attrib & NotesConstants.ISSHADOW) == NotesConstants.ISSHADOW;
	}

	public FontStyle setExtrude(boolean b) {
		if (b) {
			m_attrib = (byte) ((m_attrib | NotesConstants.ISEXTRUDE) & 0xff);
		}
		else {
			m_attrib = (byte) ((m_attrib & ~NotesConstants.ISEXTRUDE) & 0xff);
		}
		return this;
	}

	public boolean isExtrude() {
		return (m_attrib & NotesConstants.ISEXTRUDE) == NotesConstants.ISEXTRUDE;
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz==FontId.class) {
			FontId fontId = new FontId();
			fontId.setFontId(getFontId());
			return (T) fontId;
		}
		else
			return null;
	}

}
