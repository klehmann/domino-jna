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
		BLACK(NotesConstants.NOTES_COLOR_BLACK),
		WHITE(NotesConstants.NOTES_COLOR_WHITE),
		RED(NotesConstants.NOTES_COLOR_RED),
		GREEN(NotesConstants.NOTES_COLOR_GREEN),
		BLUE(NotesConstants.NOTES_COLOR_BLUE),
		MAGENTA(NotesConstants.NOTES_COLOR_MAGENTA),
		YELLOW(NotesConstants.NOTES_COLOR_YELLOW),
		CYAN(NotesConstants.NOTES_COLOR_CYAN),
		DKRED(NotesConstants.NOTES_COLOR_DKRED),
		DKGREEN(NotesConstants.NOTES_COLOR_DKGREEN),
		DKBLUE(NotesConstants.NOTES_COLOR_DKBLUE),
		DKMAGENTA(NotesConstants.NOTES_COLOR_DKMAGENTA),
		DKYELLOW(NotesConstants.NOTES_COLOR_DKYELLOW),
		DKCYAN(NotesConstants.NOTES_COLOR_DKCYAN),
		GRAY(NotesConstants.NOTES_COLOR_GRAY),
		LTGRAY(NotesConstants.NOTES_COLOR_LTGRAY);
		
		private byte m_color;
		
		private StandardColors(byte colorIdx) {
			m_color = colorIdx;
		}
		
		public byte getColorConstant() {
			return m_color;
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
