package com.mindoo.domino.jna.mime.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.james.mime4j.Charsets;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.BodyPartBuilder;
import org.apache.james.mime4j.message.MultipartBuilder;
import org.apache.james.mime4j.storage.Storage;
import org.apache.james.mime4j.storage.StorageBodyFactory;
import org.apache.james.mime4j.storage.StorageOutputStream;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.NameValuePair;
import org.apache.james.mime4j.stream.RawField;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.MimeStreamItemizeOptions;
import com.mindoo.domino.jna.constants.MimeStreamOpenOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.mime.IMimeDataAccessService;
import com.mindoo.domino.jna.mime.MIMEData;
import com.mindoo.domino.jna.mime.attachments.ByteArrayMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;
import com.mindoo.domino.jna.utils.StringUtil;

/**
 * Implementation of {@link IMimeDataAccessService} that uses MIME4J
 * to read and write the MIME data to a MIME item
 * 
 * @author Karsten Lehmann
 */
public class MIME4JMimeDataAccessService implements IMimeDataAccessService {

	@Override
	public int getPriority() {
		return 5;
	}

	@Override
	public MIMEData getMimeData(NotesNote note, String itemName) {
		try {
			Message mimeMessage = MIME4JMailMIMEHelper.readMIMEMessage(note, itemName, EnumSet.of(MimeStreamOpenOptions.MIME_INCLUDE_HEADERS));

			MIMEData mimeData = new MIMEData();
			populateMIMEData(mimeMessage, mimeData);
			
			return mimeData;
		} catch (IOException e) {
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
		AccessController.doPrivileged((PrivilegedAction<Object>) ()->{
			//structure copied from: https://stackoverflow.com/a/23853079
			
//			mixed
//				alternative
//					text
//					related
//						html
//						inline image
//						inline image
//				attachment
//				attachment
			
			String html = mimeData.getHtml();
			if (html==null) {
				html = "";
			}
			String text = mimeData.getPlainText();
			boolean hasPlainText = StringUtil.isNotEmpty(text);
			
			StorageBodyFactory bodyFactory = new StorageBodyFactory();
			
			try {
				//multipart/mixed of textual MIME content and attachments
				MultipartBuilder multiPartMixedBuilder = MultipartBuilder.create("mixed")
				.use(bodyFactory)
				// a multipart may have a preamble
				.setPreamble("This is a multi-part message in MIME format.");
				
				{
					//multipart/alternative with text content and multipart/related
					MultipartBuilder multiPartAlternativeBuilder = MultipartBuilder.create("alternative")
							.use(bodyFactory)
							// a multipart may have a preamble
							.setPreamble("This is a multi-part message in MIME format.");
					
					{
						//fill multipart/alternative
						if (hasPlainText) {
							BodyPart txtPart = BodyPartBuilder.create()
							.use(bodyFactory)
							.setBody(text, Charsets.UTF_8)
							.setContentType("text/plain", new NameValuePair("charset", "utf-8"))
							.setContentTransferEncoding("quoted-printable")
							.build();
							
							multiPartAlternativeBuilder = multiPartAlternativeBuilder
									.addBodyPart(txtPart);
						}
						
						//multipart/related with HTML and embedded images
						MultipartBuilder multiPartRelatedBuilder = MultipartBuilder.create("related")
								.use(bodyFactory)
								// a multipart may have a preamble
								.setPreamble("This is a multi-part message in MIME format.");

						{
							//html part
							BodyPart htmlPart = BodyPartBuilder.create()
									.use(bodyFactory)
									.setBody(html, Charsets.UTF_8)
									.setContentType("text/html", new NameValuePair("charset", "utf-8"))
									.setContentTransferEncoding("quoted-printable")
									.build();
							
							multiPartRelatedBuilder = multiPartRelatedBuilder.addBodyPart(htmlPart);
							
							//embedded images
							for (String currCID : mimeData.getContentIds()) {
								IMimeAttachment currAtt = mimeData.getEmbed(currCID);
								
								String fileName = currAtt.getFileName();
								String contentType = currAtt.getContentType();
								if (StringUtil.isEmpty(contentType)) {
									contentType = URLConnection.guessContentTypeFromName(fileName);
								}

								BodyPart attPart = BodyPartBuilder.create()
								.use(bodyFactory)
								.setBody(createBodyFromMimeAttachment(bodyFactory, currAtt))
								.setContentType(contentType)
								.setContentTransferEncoding("base64")
								.setField(new RawField("Content-ID", currCID))
								.build();
								
								multiPartRelatedBuilder = multiPartRelatedBuilder.addBodyPart(attPart);
							}
						}

						multiPartAlternativeBuilder = multiPartAlternativeBuilder
								.addBodyPart(
										Message.Builder.of()
										.setBody(multiPartRelatedBuilder.build())
										.build());
					}

					multiPartMixedBuilder = multiPartMixedBuilder
							.addBodyPart(
									Message.Builder.of()
									.setBody(multiPartAlternativeBuilder.build())
									.build());
					
				}

				//add attachments
				for (IMimeAttachment currAtt : mimeData.getAttachments()) {
					String fileName = currAtt.getFileName();
					String contentType = currAtt.getContentType();
					if (StringUtil.isEmpty(contentType)) {
						contentType = URLConnection.guessContentTypeFromName(fileName);
					}
					
					multiPartMixedBuilder = multiPartMixedBuilder.addBodyPart(
							BodyPartBuilder.create()
							.use(bodyFactory)
							.setBody(createBodyFromMimeAttachment(bodyFactory, currAtt))
							.setContentType(contentType)
							.setContentTransferEncoding("base64")
							.setContentDisposition("attachment", fileName)
							.build());
				}
				
				Message mimeMsg = Message.Builder.of()
						.generateMessageId("acme.com")
						.setBody(multiPartMixedBuilder.build())
						.build();
				
				
				while (note.hasItem(itemName)) {
					note.removeItem(itemName);
				}

				try {
//					MessageWriter writer = new DefaultMessageWriter();
//					writer.writeMessage(mimeMsg, System.out);
					

					MIME4JMailMIMEHelper.writeMIMEMessage(note, itemName, mimeMsg,
							EnumSet.of(MimeStreamItemizeOptions.ITEMIZE_BODY));

				} finally {
					mimeMsg.dispose();
				}

				return null;
			} catch (IOException e) {
				NotesDatabase db = note.getParent();
				throw new NotesError(0, "Error writing MIME content to item "+itemName+" of document with UNID "+
						note.getUNID()+" of database "+db.getServer()+"!!"+db.getRelativeFilePath(), e);
			}
		});
	
	}

	/**
	 * Creates a binary part from the specified {@link IMimeAttachment}.
	 * 
	 * @param bodyFactory storage factory
	 * @param att attachment
	 */
	private static BinaryBody createBodyFromMimeAttachment(StorageBodyFactory bodyFactory,
			IMimeAttachment att) throws IOException {

		StorageProvider storageProvider = bodyFactory.getStorageProvider();
		StorageOutputStream out = storageProvider.createStorageOutputStream();

		try (InputStream attIn = att.getInputStream()) {
			byte[] buf = new byte[16384];
			int len;

			while ((len = attIn.read(buf))>0) {
				out.write(buf, 0, len);
			}
		}

		Storage storage = out.toStorage();
		return bodyFactory.binaryBody(storage);
	}

	/**
	 * Recursively traverse the MIME structure reading HTML/plaintext
	 * content and information about inlines/attachments
	 * 
	 * @param content return value of {@link MimeMessage#getContent()}
	 * @param retMimeData {@link MIMEData} to populate with html/plaintext/inlines/attachments
	 * @throws IOException for general I/O errors
	 */
	private void populateMIMEData(Object content, MIMEData retMimeData) throws IOException {
		if (content==null) {
			return;
		}
		else if (content instanceof Message) {
			Message msg = (Message) content;
			
			populateMIMEData(msg.getBody(), retMimeData);
		}
		else if (content instanceof Multipart) {
			Multipart multiPart = (Multipart) content;
			List<Entity> bodyParts = multiPart.getBodyParts();
			
			for (Entity currPart : bodyParts) {
				populateMIMEData(currPart, retMimeData);
			}
		}
		else if (content instanceof SingleBody) {
			SingleBody body = (SingleBody) content;
			
			Entity parentEntity = body.getParent();
			Header header = parentEntity.getHeader();
			String fileName = parentEntity.getFilename();
			
			Field contentIdField = header.getField("Content-ID");
			String contentId = contentIdField==null ? null : contentIdField.getBody();

			String contentType = parentEntity.getMimeType();
			
			if (!StringUtil.isEmpty(fileName) || !StringUtil.isEmpty(contentId)) {
				ByteArrayOutputStream binOut = new ByteArrayOutputStream();
				body.writeTo(binOut);
				
				ByteArrayMimeAttachment mimeAtt = new ByteArrayMimeAttachment(binOut.toByteArray(),
						fileName, contentType);
				
				if (contentId!=null) {
					retMimeData.embed(contentId, mimeAtt);
				}
				else {
					retMimeData.attach(mimeAtt);
				}
			}
			else if (content instanceof TextBody) {
				TextBody txtBody = (TextBody) content;
				
				Optional<Consumer<String>> consumer = Optional.empty();

				if (contentType.startsWith("text/html")) {
					consumer = Optional.of(retMimeData::setHtml);
				}
				else if (contentType.startsWith("text/plain")) {
					consumer = Optional.of(retMimeData::setPlainText);
				}
				
				if (consumer.isPresent()) {
					StringBuilder sb = new StringBuilder();

					try (Reader reader = txtBody.getReader()) {
						if (reader!=null) {
							char[] buf = new char[16384];
							int len;
							
							while ((len=reader.read(buf))>0) {
								sb.append(buf, 0, len);
							}
						}
					}
					
					consumer.get().accept(sb.toString());
				}
			}
			
		}
		else if (content instanceof BodyPart) {
			BodyPart bodyPart = (BodyPart) content;
			Body body = bodyPart.getBody();
			populateMIMEData(body, retMimeData);
		}
	}
	
}
