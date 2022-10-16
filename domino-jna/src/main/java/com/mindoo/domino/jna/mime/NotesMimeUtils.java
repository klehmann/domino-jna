package com.mindoo.domino.jna.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.IHtmlItemImageConversionCallback;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.html.HtmlConvertOption;
import com.mindoo.domino.jna.html.HtmlConvertProperties;
import com.mindoo.domino.jna.html.IHtmlConversionResult;
import com.mindoo.domino.jna.html.IHtmlImageRef;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.mime.attachments.ByteArrayMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.ByteBufferMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;

public class NotesMimeUtils {

	/**
	 * Exports a MIME email to a file in EML format on disk
	 * 
	 * @param note note to export
	 * @param exportFilePath target path
	 */
	public static void exportMIMEToEML(NotesNote note, String exportFilePath) {
		NotesDatabase db = note.getParent();
		exportMIMEToEML(db.getServer(), db.getRelativeFilePath(), note.getNoteId(), exportFilePath);
	}

	/**
	 * Exports a MIME email to a file in EML format on disk
	 * 
	 * @param dbServer database server
	 * @param dbFilePath database filepath
	 * @param noteID note id of note to export
	 * @param exportFilePath target path
	 */
	public static void exportMIMEToEML(String dbServer, String dbFilePath, int noteID, String exportFilePath) {
		Memory dbServerLMBCS = NotesStringUtils.toLMBCS(dbServer, true);
		Memory dbFilePathLMBCS = NotesStringUtils.toLMBCS(dbFilePath, true);
		Memory retFullNetPath = new Memory(NotesConstants.MAXPATH);

		short result = NotesNativeAPI.get().OSPathNetConstruct(null, dbServerLMBCS, dbFilePathLMBCS, retFullNetPath);
		NotesErrorUtils.checkResult(result);

		Memory exportFilePathMem = NotesStringUtils.toLMBCS(exportFilePath, true);
		
		result = NotesNativeAPI.get().MIMEEMLExport(retFullNetPath, noteID, exportFilePathMem);
		NotesErrorUtils.checkResult(result);
	}

	private static IMimeDataAccessService getAccessService() {
		IMimeDataAccessService service = null;
		int servicePrio = Integer.MAX_VALUE;
		
		Iterator<IMimeDataAccessService> implementations = PlatformUtils.getService(IMimeDataAccessService.class);
		while (implementations.hasNext()) {
			IMimeDataAccessService currService = implementations.next();
			int currPrio = currService.getPriority();
			//pick service with lowest priority value
			if (service==null || currPrio<servicePrio) {
				service = currService;
				servicePrio = currPrio;
			}
		}
		
		if(service==null) {
			throw new IllegalStateException("Unable to locate implementation of " + IMimeDataAccessService.class.getName());
		}
		return service;
	}

	/**
	 * Reads {@link MIMEData} from a note
	 * 
	 * @param note note
	 * @param itemName MIME item name
	 * @return MIME data or null if item could not be found
	 */
	public static MIMEData getMimeData(NotesNote note, String itemName) {
		return getAccessService().getMimeData(note, itemName);
	}

	/**
	 * Writes {@link MIMEData} to a note
	 * 
	 * @param note note
	 * @param itemName MIME item name
	 * @param mimeData MIME data to write
	 */
	public static void setMimeData(NotesNote note, String itemName, MIMEData mimeData) {
		getAccessService().setMimeData(note, itemName, mimeData);
	}

	public static MIMEData renderItemAsMime(NotesNote note, String itemName) {
		HtmlConvertProperties props = new HtmlConvertProperties()
				//expand outlines and sections
				.option(HtmlConvertOption.ForceOutlineExpand)
				.option(HtmlConvertOption.ForceSectionExpand)
				//display all tabs of tabbed tables
				.option(HtmlConvertOption.RowAtATimeTableAlt)
				.option(HtmlConvertOption.ListFidelity)
				.option(HtmlConvertOption.TextExactSpacing)
				//use Notes URLs in links instead of web URLs
				.option(HtmlConvertOption.OfferNotesURLInLink)
				//enable the font conversion master option so that we
				//can set "specs" and "tags" options to produce modern HTML
				.option(HtmlConvertOption.FontConversion)
				.options(HtmlConvertOption.allSpecs(), "2")
				.options(HtmlConvertOption.allTags(), "0");

		return renderItemAsMime(note, itemName, (att) -> {
			ByteBuffer attData = att.toByteBuffer();
			IMimeAttachment mimeAtt = new ByteBufferMimeAttachment(attData, att.getFileName());
			return mimeAtt;
		}, props);
	}
	
	private static String simpleHtmlEscape(String txt) {
		final StringBuilder sb = new StringBuilder();

		for (int i=0; i<txt.length(); i++) {
			char c = txt.charAt(i);
			
			if (c == '<') {
				sb.append("&lt;");
			}
			else if (c == '>') {
				sb.append("&gt;");
			}
			else if (c == '\"') {
				sb.append("&quot;");
			}
			else if (c == '\'') {
				sb.append("&#039;");
			}
			else if (c == '\\') {
				sb.append("&#092;");
			}
			else if (c == '&') {
				sb.append("&amp;");
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Renders a richtext item as HTML and produces a {@link MIMEData} object
	 * with all referenced content
	 * 
	 * @param note note
	 * @param itemName item name
	 * @param attConverter converter from {@link NotesAttachment} to {@link IMimeAttachment} or null to ignore attachents
	 * @param props converter properties
	 * @return MIME data
	 */
	public static MIMEData renderItemAsMime(NotesNote note, String itemName,
			Function<NotesAttachment,IMimeAttachment> attConverter, HtmlConvertProperties props) {
		MIMEData data = new MIMEData();
		
		NotesItem bodyItem = note.getFirstItem(itemName);
		if (bodyItem==null) {
			data.setHtml("");
			return data;
		}

		IHtmlConversionResult bodyConvResult;
		try {
			bodyConvResult = note.convertItemToHtml(itemName, props);
		}
		catch (NotesError e) {
			//give devs a way to suppress this errors
			String key = NotesMimeUtils.class.getName()+".suppressRenderErrors";
			
			if (!"true".equals(NotesGC.getCustomValue(key)) &&
					Boolean.TRUE.equals(NotesGC.getCustomValue(key))) {
				System.err.println("renderItemAsMime failed for item type "+bodyItem.getType()+
						" on doc with UNID "+note.getUNID());
				e.printStackTrace();
			}
			String txtContent = simpleHtmlEscape(note.getItemValueAsText(itemName, '\n'));
			data.setHtml(txtContent);
			return data;
		}

		data.setHtml(bodyConvResult.getText());
		
		List<IHtmlImageRef> bodyImages = bodyConvResult.getImages();
		
		AtomicInteger imageCount = new AtomicInteger();
		
		//inline all embedded images
		bodyImages.forEach((currImage) -> {
			String imgSrc = currImage.getReferenceText();
			String imgFormat = currImage.getFormat();
			
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			
			AtomicReference<MessageDigest> digest = new AtomicReference<>();
			try {
				digest.set(MessageDigest.getInstance("SHA-256"));
			} catch (NoSuchAlgorithmException e1) {
				digest.set(null);
			}
		    
			currImage.readImage(new IHtmlItemImageConversionCallback() {

				@Override
				public int setSize(int size) {
					return 0;
				}

				@Override
				public Action read(byte[] data) {
					try {
						bOut.write(data);
						
						if (digest.get()!=null) {
							digest.get().update(data, 0, data.length);
						}
						
					} catch (IOException e) {
						throw new UncheckedIOException("Error reading image '"+imgSrc+
								"' from document with UNID "+note.getUNID(), e);
					}
					return Action.Continue;
				}
				
			});
			
			String contentId;
			
			if (digest.get()!=null) {
			    byte[] digestVal = digest.get().digest();
			    contentId = digestToString(digestVal);
			}
			else {
				contentId = "img_"+imageCount.incrementAndGet();
			}
			
			if (data.getEmbed(contentId)==null) {
				IMimeAttachment imgAtt = new ByteArrayMimeAttachment(bOut.toByteArray(), contentId+"."+imgFormat);
				data.embed(contentId, imgAtt);
				
				data.setHtml(data.getHtml().replace(imgSrc, "cid:" + contentId));
			}
			
		});
	
		if (!StringUtil.startsWithIgnoreCase(data.getHtml(), "<html>")) {
			data.setHtml("<html>" + data.getHtml() + "</html>");
		}

		if (attConverter!=null) {
			for (String currAttName : note.getAttachmentNames()) {
				NotesAttachment currAtt = note.getAttachment(currAttName);
				if (currAtt!=null) {
					IMimeAttachment att = attConverter.apply(currAtt);
					if (att!=null) {
						data.attach(att);
					}
				}
			}
		}
		
		return data;
	}
	
	/**
	 * Converts a hash digest to a hex string
	 * 
	 * @param digest hash digest
	 * @return hash as String
	 */
	private static String digestToString(byte[] digest) {
		StringBuilder hexString = new StringBuilder();

		for (int i = 0; i < digest.length; i++) {
			if ((0xff & digest[i]) < 0x10) {
				hexString.append("0"
						+ Integer.toHexString((0xFF & digest[i])));
			} else {
				hexString.append(Integer.toHexString(0xFF & digest[i]));
			}
		}

		return hexString.toString();
	}

}
