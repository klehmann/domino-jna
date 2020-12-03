package com.mindoo.domino.jna.mime.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.MimeStreamItemizeOptions;
import com.mindoo.domino.jna.constants.MimeStreamOpenOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.mime.IMimeDataAccessService;
import com.mindoo.domino.jna.mime.JakartaMailMIMEHelper;
import com.mindoo.domino.jna.mime.MIMEData;
import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;
import com.mindoo.domino.jna.mime.internal.jakartamail.org.apache.commons.mail.EmailAttachment;
import com.mindoo.domino.jna.mime.internal.jakartamail.org.apache.commons.mail.EmailException;
import com.mindoo.domino.jna.mime.internal.jakartamail.org.apache.commons.mail.HtmlEmail;
import com.mindoo.domino.jna.utils.StringUtil;

import jakarta.activation.DataSource;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * Implementation of {@link IMimeDataAccessService} that uses the Jakarta Mail
 * API to read and write MIME data.
 * 
 * @author Karsten Lehmann
 */
public class JakartaMailMimeDataAccessService implements IMimeDataAccessService {
	private static final MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
	
	@Override
	public MIMEData getMimeData(NotesNote note, String itemName) {
		try {
			MimeMessage mimeMessage = JakartaMailMIMEHelper.readMIMEMessage(note, itemName, EnumSet.of(MimeStreamOpenOptions.MIME_INCLUDE_HEADERS));

			MIMEData mimeData = new MIMEData();
			populateMIMEData(mimeMessage, mimeData);
			
			return mimeData;
		} catch (IOException e) {
			NotesDatabase db = note.getParent();
			throw new NotesError(0, "Error reading MIMEData from item "+itemName+" of document with UNID "+
					note.getUNID()+" in database "+db.getServer()+"!!"+db.getRelativeFilePath(), e);
		} catch (MessagingException e) {
			NotesDatabase db = note.getParent();
			throw new NotesError(0, "Error reading MIMEData from item "+itemName+" of document with UNID "+
					note.getUNID()+" in database "+db.getServer()+"!!"+db.getRelativeFilePath(), e);
		} catch (NotesError e) {
			if (e.getId() == 546) {
				//Note item not found
				return null;
			}
			else {
				throw e;
			}
		}
	}

	@Override
	public void setMimeData(NotesNote note, String itemName, MIMEData mimeData) {
		String html = mimeData.getHtml();
		String text = mimeData.getPlainText();

		try {
			HtmlEmail mail = new HtmlEmail();

			//add some required fields required by Apache Commons Email (will not be written to the doc)
			mail.setFrom("mr.sender@acme.com", "Mr. Sender");
			mail.addTo("mr.receiver@acme.com", "Mr. Receiver");
			mail.setHostName("acme.com");

			mail.setCharset("UTF-8");

			//add embeds
			for (String currCID : mimeData.getContentIds()) {
				IMimeAttachment currAtt = mimeData.getEmbed(currCID);
				
				MimeAttachmentDataSource dataSource = new MimeAttachmentDataSource(currAtt);
				mail.embed(dataSource, currAtt.getFileName(), currCID);
			}
			
			//add attachments
			for (IMimeAttachment currAtt : mimeData.getAttachments()) {
				MimeAttachmentDataSource dataSource = new MimeAttachmentDataSource(currAtt);

				mail.attach(dataSource, currAtt.getFileName(), null, EmailAttachment.ATTACHMENT);
			}

			if (!StringUtil.isEmpty(text)) {
				mail.setTextMsg(text);
			}
			mail.setHtmlMsg(html);

			mail.buildMimeMessage();
			MimeMessage mimeMsg = mail.getMimeMessage();

			while (note.hasItem(itemName)) {
				note.removeItem(itemName);
			}

			JakartaMailMIMEHelper.writeMIMEMessage(note, itemName, mimeMsg, EnumSet.of(MimeStreamItemizeOptions.ITEMIZE_BODY));

		} catch (EmailException | IOException | MessagingException e) {
			NotesDatabase db = note.getParent();
			throw new NotesError(0, "Error writing MIME content to item "+itemName+" of document with UNID "+
					note.getUNID()+" of database "+db.getServer()+"!!"+db.getRelativeFilePath(), e);
		}
	}

	/**
	 * Recursively traverse the MIME structure reading HTML/plaintext
	 * content and information about inlines/attachments
	 * 
	 * @param content return value of {@link MimeMessage#getContent()}
	 * @param retMimeData {@link MIMEData} to populate with html/plaintext/inlines/attachments
	 * @throws MessagingException for errors parsing the MIME data
	 * @throws IOException for general I/O errors
	 */
	private void populateMIMEData(Object content, MIMEData retMimeData) throws MessagingException, IOException {
		if (content==null) {
			return;
		}
		
		if (content instanceof Multipart) {
			Multipart multipart = (Multipart) content;

			for (int i=0; i<multipart.getCount(); i++) {
				BodyPart currBodyPart = multipart.getBodyPart(i);

				populateMIMEData(currBodyPart, retMimeData);	
			}
		}
		else if (content instanceof Part) {
			Part part = (Part) content;
			
			String disposition = part.getDisposition();
			
			if (part.isMimeType("text/html")
					&& (StringUtil.isEmpty(disposition) || "inline".equals(disposition))) {
				Object htmlContent = part.getContent();
				if (htmlContent instanceof String) {
					retMimeData.setHtml((String) htmlContent);
				}
				return;
			}
			else if (part.isMimeType("text/plain")
					&& (StringUtil.isEmpty(disposition) || "inline".equals(disposition))) {
				Object textContent = part.getContent();
				if (textContent instanceof String) {
					retMimeData.setPlainText((String) textContent);
				}
				return;
			}
			
			if (!part.isMimeType("multipart/related") &&
					!part.isMimeType("multipart/mixed") &&
					!part.isMimeType("multipart/alternative")) {
				//either inline file or attachment
				JakartaMailMimeBodyPartAttachment mimeAtt = new JakartaMailMimeBodyPartAttachment(part);

				String[] currContentIdArr = part.getHeader("Content-ID");
				String currContentId = currContentIdArr!=null && currContentIdArr.length>0 ? currContentIdArr[0] : null;

				if (StringUtil.isEmpty(currContentId)) {
					retMimeData.attach(mimeAtt);
				}
				else {
					if (currContentId.startsWith("<")) {
						currContentId = currContentId.substring(1);
					}
					if (currContentId.endsWith(">")) {
						currContentId = currContentId.substring(0, currContentId.length()-1);
					}
					
					retMimeData.embed(currContentId, mimeAtt);
				}
			}
			else {
				Object bodyContent = part.getContent();
				if (bodyContent!=null) {
					populateMIMEData(bodyContent, retMimeData);
				}
			}
		}
		else if (content instanceof InputStream) {
			((InputStream)content).close();
		}
	}

	/**
	 * Implementation of {@link DataSource} that reads its data from
	 * a {@link IMimeAttachment}.
	 * 
	 * @author Karsten Lehmann
	 */
	private static class MimeAttachmentDataSource implements DataSource {
		private IMimeAttachment m_att;

		public MimeAttachmentDataSource(IMimeAttachment att) {
			m_att = att;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return m_att.getInputStream();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new IOException("cannot do this");
		}

		@Override
		public String getContentType() {
			try {
				String contentType = m_att.getContentType();
				
				if (StringUtil.isEmpty(contentType)) {
					String fileName = getName();
					if (!StringUtil.isEmpty(fileName)) {
						contentType = mimeTypes.getContentType(fileName);
					}
				}
				
				if (StringUtil.isEmpty(contentType)) {
					contentType = "application/octet-stream";
				}
				
				return contentType;
				
			} catch (IOException e) {
				throw new NotesError(0, "Error reading content type from MIME attachment", e);
			}
		}

		@Override
		public String getName() {
			try {
				return m_att.getFileName();
			} catch (IOException e) {
				throw new NotesError(0, "Error reading content type from MIME attachment", e);
			}
		}

	}
}
