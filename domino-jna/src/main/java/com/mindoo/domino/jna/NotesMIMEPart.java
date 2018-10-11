package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import com.mindoo.domino.jna.NotesAttachment.IDataCallback;
import com.mindoo.domino.jna.constants.MimePartOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringTokenizerExt;

public class NotesMIMEPart {
	public enum PartType {
		/** Mime part type is a prolog. */
		PROLOG,
		/** Mime part type is a body. */
		BODY,
		/** Mime part type is a epilog. */
		EPILOG,
		/** Mime part type is retrieve information. */
		RETRIEVE_INFO,
		/** Mime part type is a message. */
		MESSAGE}
	
	private NotesNote m_parentNote;
	private EnumSet<MimePartOptions> m_options;
	private PartType m_partType;
	private String m_boundaryStr;
	private String m_headers;
	private LinkedHashMap<String,String> m_headersParsed;
	private byte[] m_data;
	
	public NotesMIMEPart(NotesNote parentNote, EnumSet<MimePartOptions> options, PartType partType, String boundaryStr, String headers, byte[] data) {
		m_parentNote = parentNote;
		m_options = options;
		m_partType = partType;
		m_boundaryStr = boundaryStr;
		m_headers = headers;
		m_headersParsed = parseHeaders(headers);
		m_data = data;
	}
	private static final Pattern FOLDING = Pattern.compile("\\r\\n[\\t| ]+");
	
	private static LinkedHashMap<String,String> parseHeaders(String headers) {
		LinkedHashMap<String,String> headersMap = new LinkedHashMap<String,String>();
		headers = FOLDING.matcher(headers).replaceAll(" ");
		StringTokenizerExt st = new StringTokenizerExt(headers, "\r\n");
		while (st.hasMoreTokens()) {
			String currToken = st.nextToken();
			if (currToken.length()>0) {
				int iPos = currToken.indexOf(':');
				if (iPos!=-1) {
					String currHeaderName = currToken.substring(0, iPos);
					String currHeaderValue = currToken.substring(iPos+1).trim();
					headersMap.put(currHeaderName, currHeaderValue);
				}
			}
		}
		return headersMap;
	}
	
	public NotesNote getParentNote() {
		return m_parentNote;
	}
	
	/**
	 * Returns the mime part options
	 * 
	 * @return options
	 */
	public EnumSet<MimePartOptions> getOptions() {
		return m_options;
	}
	
	/**
	 * Returns the mime part type
	 * 
	 * @return type
	 */
	public PartType getType() {
		return m_partType;
	}
	
	/**
	 * Returns the boundary string
	 * 
	 * @return boundary string
	 */
	public String getBoundaryString() {
		return m_boundaryStr;
	}
	
	public LinkedHashMap<String,String> getHeaders() {
		return m_headersParsed;
	}
	
	public String getEncoding() {
		String encoding = getHeaders().get("Content-Transfer-Encoding");
		if (encoding==null)
			return "binary";
		else
			return encoding;
		
		
		//https://www.w3.org/Protocols/rfc1341/5_Content-Transfer-Encoding.html
//		Content-Transfer-Encoding := "BASE64" / "QUOTED-PRINTABLE" / 
//                "8BIT"   / "7BIT" / 
//                "BINARY" / x-token
//
//These values are not case sensitive. That is, Base64 and BASE64 and bAsE64 are all equivalent.
//		An encoding type of 7BIT requires that the body is already in a seven-bit mail- ready
//		representation. This is the default value -- that is, "Content-Transfer-Encoding:
//		7BIT" is assumed if the Content-Transfer-Encoding header field is not present.
//		The values "8bit", "7bit", and "binary" all imply that NO encoding has been performed.
//		However, they are potentially useful as indications of the kind of data contained in
//		the object, and therefore of the kind of encoding that might need to be performed
//		for transmission in a given transport system. "7bit" means that the data is all
//		represented as short lines of US-ASCII data. "8bit" means that the lines are short,
//		but there may be non-ASCII characters (octets with the high-order bit set). "Binary"
//		means that not only may non-ASCII characters be present, but also that the lines are
//		not necessarily short enough for SMTP transport.
//
//		The difference between "8bit" (or any other conceivable bit-width token) and the "binary"
//		token is that "binary" does not require adherence to any limits on line length or to
//		the SMTP CRLF semantics, while the bit-width tokens do require such adherence. If the body
//		contains data in any bit-width other than 7-bit, the appropriate bit-width
//		Content-Transfer-Encoding token must be used (e.g., "8bit" for unencoded 8 bit wide data).
//		If the body contains binary data, the "binary" Content-Transfer-Encoding token must be used.
		
		//https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/net/QuotedPrintableCodec.html

	}
	
	public String getContentAsText() {
		if (m_options.contains(MimePartOptions.BODY_IN_DBOBJECT)) {
			final String fileName = NotesStringUtils.fromLMBCS(m_data);
			NotesAttachment att = m_parentNote.getAttachment(fileName);
			if (att==null) {
				throw new NotesError(0, "No attachment item found for filename "+fileName);
			}
			final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			att.readData(new IDataCallback() {

				@Override
				public Action read(byte[] data) {
					try {
						bOut.write(data);
						return Action.Continue;
					} catch (IOException e) {
						throw new NotesError(0, "Error writing attachment data", e);
					}
				}
			});
			byte[] bOutArr = bOut.toByteArray();
			return NotesStringUtils.fromLMBCS(bOutArr);
		}
		else {
			return NotesStringUtils.fromLMBCS(m_data);
		}
	}
	
	/**
	 * Method to access the MIME part's data
	 * 
	 * @param callback callback to receive data
	 */
	public void getContentAsBytes(final IDataCallbackWithFileSize callback) {
		if (m_options.contains(MimePartOptions.BODY_IN_DBOBJECT)) {
			final String fileName = NotesStringUtils.fromLMBCS(m_data);
			NotesAttachment att = m_parentNote.getAttachment(fileName);
			if (att==null) {
				throw new NotesError(0, "No attachment item found for filename "+fileName);
			}
			final int size = att.getFileSize();
			
			callback.setFileSize(size);
			att.readData(callback);
		}
		else {
			callback.setFileSize(m_data.length);
			callback.read(m_data);
		}
	}
	
	@Override
	public String toString() {
		return "NotesMIMEPart [options="+m_options+", type="+m_partType+", boundary="+m_boundaryStr+", headers="+m_headers+", datalen="+m_data.length+"]";
	}

	/**
	 * Extension of {@link IDataCallback} that also receives the filesize
	 */
	public static interface IDataCallbackWithFileSize extends IDataCallback {
		
		public void setFileSize(int fileSize);
		
	}
}
