package com.mindoo.domino.jna.mime;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringTokenizerExt;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * Settings to control the richtext to MIME conversion
 * 
 * @author Karsten Lehmann
 */
public class MimeConversionControl implements IRecyclableNotesObject, IAdaptable {
	private Pointer m_convControls;
	private boolean m_recycled;
	
	/**
	 * Creates a Conversions Controls context for the reading and writing of various conversion
	 * configuration settings.<br>
	 * The various settings are initialized to their default settings (the same as those set by {@link #setDefaults()}).
	 */
	public MimeConversionControl() {
		PointerByReference retConvControls = new PointerByReference();
		NotesNativeAPI.get().MMCreateConvControls(retConvControls);
		m_convControls = retConvControls.getValue();
		
		NotesGC.__objectCreated(MimeConversionControl.class, this);
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == Pointer.class && !isRecycled()) {
			return (T) m_convControls;
		}

		return null;
	}
	
	@Override
	public void recycle() {
		if (isRecycled()) {
			return;
		}
		
		NotesNativeAPI.get().MMDestroyConvControls(m_convControls);
		m_recycled = true;
	}

	private void checkRecycled() {
		if (isRecycled()) {
			throw new NotesError(0, "Conversion controls already recycled");
		}
	}
	
	@Override
	public boolean isRecycled() {
		return m_recycled;
	}

	@Override
	public boolean isNoRecycle() {
		return false;
	}

	@Override
	public int getHandle32() {
		return 0;
	}

	@Override
	public long getHandle64() {
		return 0;
	}
	
	/**
	 * The function sets Conversion Controls configuration settings to their default values.
	 * 
	 * @return this instance
	 */
	public MimeConversionControl setDefaults() {
		checkRecycled();
		
		NotesNativeAPI.get().MMConvDefaults(m_convControls);
		
		return this;
	}
	
	public enum AttachmentEncoding { BASE64, QUOTEDPRINTABLE, UUENCODE, BINHEX40 }
	
	/**
	 * The function returns the Conversions Controls 'attachment encoding' setting.
	 * 
	 * @return encoding or null if known return value
	 */
	public AttachmentEncoding getAttachmentEncoding() {
		checkRecycled();
		
		short enc = NotesNativeAPI.get().MMGetAttachEncoding(m_convControls);
		if (enc == 1) {
			return AttachmentEncoding.BASE64;
		}
		else if (enc == 2) {
			return AttachmentEncoding.QUOTEDPRINTABLE;
		}
		else if (enc == 3) {
			return AttachmentEncoding.UUENCODE;
		}
		else if (enc == 4) {
			return AttachmentEncoding.BINHEX40;
		}
		else {
			return null;
		}
	}
	
	/**
	 * The function sets the Conversions Controls 'attachment encoding' setting to the input value.<br>
	 * 
	 * @param encoding new encoding
	 * @return this instance
	 */
	public MimeConversionControl setAttachmentEncoding(AttachmentEncoding encoding) {
		checkRecycled();
		
		short newVal = 0;
		switch (encoding) {
		case BASE64:
			newVal = 1;
			break;
		case QUOTEDPRINTABLE:
			newVal = 2;
			break;
		case UUENCODE:
			newVal = 3;
			break;
		case BINHEX40:
			newVal = 4;
			break;
			default:
				throw new IllegalArgumentException("Unknown encoding: "+encoding);
		}
		
		NotesNativeAPI.get().MMSetAttachEncoding(m_convControls, newVal);
		return this;
	}
	
	/**
	 * The function returns the Conversions Controls 'drop items' setting.
	 * 
	 * @return list of items to drop during export
	 */
	public List<String> getDropItems() {
		checkRecycled();
		
		List<String> itemNames = new ArrayList<>();
		
		Pointer ptr = NotesNativeAPI.get().MMGetDropItems(m_convControls);
		String itemNamesConc = NotesStringUtils.fromLMBCS(ptr, -1);
		StringTokenizerExt st = new StringTokenizerExt(itemNamesConc, ",");
		while (st.hasMoreTokens()) {
			String currToken = st.nextToken();
			if (!StringUtil.isEmpty(currToken)) {
				itemNames.add(currToken);
			}
		}
		return itemNames;
	}
	
	/**
	 * The function sets the Conversions Controls 'drop items setting' setting to the input value,
	 * a list of items to drop during export.
	 * 
	 * @param itemNames item names
	 * @return this instance
	 */
	public MimeConversionControl setDropItems(List<String> itemNames) {
		checkRecycled();
		
		StringBuilder sb = new StringBuilder();
		for (String currItemName : itemNames) {
			if (sb.length()>0) {
				sb.append(",");
			}
			sb.append(currItemName.trim());
		}
		String itemNamesConc = sb.toString();
		Memory itemNamesConcMem = NotesStringUtils.toLMBCS(itemNamesConc, true);
		
		NotesNativeAPI.get().MMSetDropItems(m_convControls, itemNamesConcMem);
		
		return this;
	}

	/**
	 * The function returns the Conversions Controls 'keep tabs' setting.
	 * 
	 * @return true to keep tabs
	 */
	public boolean isKeepTabs() {
		checkRecycled();
		
		return NotesNativeAPI.get().MMGetKeepTabs(m_convControls);
	}
	
	/**
	 * The function  sets the Conversions Controls 'keep tabs' setting to the input value.
	 * 
	 * @param b true to keep tabs
	 * @return this instance
	 */
	public MimeConversionControl setKeepTabs(boolean b) {
		checkRecycled();
		
		NotesNativeAPI.get().MMSetKeepTabs(m_convControls, b);
		return this;
	}
	
	/**
	 * The function returns the Conversions Controls 'point size' setting.
	 * 
	 * @return point size
	 */
	public int getPointSize() {
		checkRecycled();
		
		return (int) (NotesNativeAPI.get().MMGetPointSize(m_convControls) & 0xffff);
	}
	
	/**
	 * The function sets the Conversions Controls 'point size' setting to the input value.
	 * 
	 * @param size new size, one of: 6, 8, 9, 10 (default), 12, 14, 18, 24
	 * @return this instance
	 */
	public MimeConversionControl setPointSize(int size) {
		checkRecycled();
		
		NotesNativeAPI.get().MMSetPointSize(m_convControls, size> 65535 ? (short) 0xffff : (short) (size & 0xffff));
		return this;
	}
	
	public enum Typeface { TIMESROMAN, HELVETICA, COURIER }
	
	/**
	 * The function returns the Conversions Controls 'typeface' setting.
	 * 
	 * @return typeface or null if we received an unknown value
	 */
	public Typeface getTypeface() {
		checkRecycled();
		
		short tf = NotesNativeAPI.get().MMGetTypeFace(m_convControls);
		if (tf==0) {
			return Typeface.TIMESROMAN;
		}
		else if (tf==1) {
			return Typeface.HELVETICA;
		}
		else if (tf==2) {
			return Typeface.COURIER;
		}
		else {
			return null;
		}
	}
	
	/**
	 * The function sets the Conversions Controls 'typeface' setting to the input value.
	 * 
	 * @param tf new typeface
	 * @return this instance
	 */
	public MimeConversionControl setTypeface(Typeface tf) {
		checkRecycled();
		
		short tfAsShort;
		switch (tf) {
		case COURIER:
			tfAsShort = 2;
			break;
		case HELVETICA:
			tfAsShort = 1;
			break;
		case TIMESROMAN:
			tfAsShort = 0;
			break;
			default:
				throw new IllegalArgumentException("Unknown typeface: "+tf);
		}
		
		NotesNativeAPI.get().MMSetTypeFace(m_convControls, tfAsShort);
		return this;
	}
	
	/**
	 * The function returns the Conversions Controls 'add items setting' setting.
	 * 
	 * @return list of items to add during export
	 */
	public List<String> getAddItems() {
		checkRecycled();
		
		List<String> itemNames = new ArrayList<>();
		
		Pointer ptr = NotesNativeAPI.get().MMGetAddItems(m_convControls);
		String itemNamesConc = NotesStringUtils.fromLMBCS(ptr, -1);
		StringTokenizerExt st = new StringTokenizerExt(itemNamesConc, ",");
		while (st.hasMoreTokens()) {
			String currToken = st.nextToken();
			if (!StringUtil.isEmpty(currToken)) {
				itemNames.add(currToken);
			}
		}
		return itemNames;
	}
	
	/**
	 * The function sets the Conversions Controls 'add items setting' setting to the input value.
	 * 
	 * @param itemNames item names, list of item names to preserve in msgs as they are exported (default is empty list)
	 * @return this instance
	 */
	public MimeConversionControl setAddItems(List<String> itemNames) {
		checkRecycled();
		
		StringBuilder sb = new StringBuilder();
		for (String currItemName : itemNames) {
			if (sb.length()>0) {
				sb.append(",");
			}
			sb.append(currItemName.trim());
		}
		String itemNamesConc = sb.toString();
		Memory itemNamesConcMem = NotesStringUtils.toLMBCS(itemNamesConc, true);
		
		NotesNativeAPI.get().MMSetAddItems(m_convControls, itemNamesConcMem);
		
		return this;
	}

	public enum MessageContentEncoding {
		/** text/plain (w/o images and attachments)  */
		TEXT_PLAIN_NO_IMAGES_ATTACHMENTS,
		/** text/plain (w/images and attachments) */
		TEXT_PLAIN_WITH_IMAGES_ATTACHMENTS,
		/** text/html (w/images and attachments) */
		TEXT_HTML_WITH_IMAGES_ATTACHMENTS,
		/** multipart/alternative: text/plain and text/html (w/images and attachments) */
		TEXT_PLAIN_AND_HTML_WITH_IMAGES_ATTACHMENTS
	}

	public MessageContentEncoding getMessageContentEncoding() {
		checkRecycled();

		short enc = NotesNativeAPI.get().MMGetMessageContentEncoding(m_convControls);
		if (enc == 0) {
			return MessageContentEncoding.TEXT_PLAIN_NO_IMAGES_ATTACHMENTS;
		}
		else if (enc == 1) {
			return MessageContentEncoding.TEXT_PLAIN_WITH_IMAGES_ATTACHMENTS;
		}
		else if (enc == 2) {
			return MessageContentEncoding.TEXT_HTML_WITH_IMAGES_ATTACHMENTS;
		}
		else if (enc == 3) {
			return MessageContentEncoding.TEXT_PLAIN_AND_HTML_WITH_IMAGES_ATTACHMENTS;
		}
		else {
			return null;
		}
	}
	
	public MimeConversionControl setMessageContentEncoding(MessageContentEncoding enc) {
		checkRecycled();
		
		short encAsShort;
		
		switch (enc) {
		case TEXT_PLAIN_NO_IMAGES_ATTACHMENTS:
			encAsShort = 0;
			break;
		case TEXT_PLAIN_WITH_IMAGES_ATTACHMENTS:
			encAsShort = 1;
			break;
		case TEXT_HTML_WITH_IMAGES_ATTACHMENTS:
			encAsShort = 2;
			break;
		case TEXT_PLAIN_AND_HTML_WITH_IMAGES_ATTACHMENTS:
			encAsShort = 3;
			break;
			default:
				throw new IllegalArgumentException("Unknown encoding: "+enc);
		}
		
		NotesNativeAPI.get().MMSetMessageContentEncoding(m_convControls, encAsShort);
		return this;
	}
	
	public enum ReadReceipt {
		/** Do not pass read receipt requests when importing or exporting */
		DONT_PASS_READ_RECEIPT,
		/** Support read receipt requests (as Return-Receipt-To when exporting) */
		RETURN_RECEIPT_TO,
		/** Support read receipt requests (as Disposition-Notification-To when exporting) */
		DISPOSITION_NOTIFICATION_TO
	}

	/**
	 * The function returns the Conversions Controls 'read receipt' setting.
	 * 
	 * @return read receipt or null if we received an unknown value
	 */
	public ReadReceipt getReadReceipt() {
		checkRecycled();
		
		short rc = NotesNativeAPI.get().MMGetReadReceipt(m_convControls);
		if (rc == 0) {
			return ReadReceipt.DONT_PASS_READ_RECEIPT;
		}
		else if (rc == 1) {
			return ReadReceipt.RETURN_RECEIPT_TO;
		}
		else if (rc == 2) {
			return ReadReceipt.DISPOSITION_NOTIFICATION_TO;
		}
		else {
			return null;
		}
	}
	
	/**
	 * The function sets the Conversions Controls read receipt setting to the input value
	 * 
	 * @param rc read receipt
	 * @return this instance
	 */
	public MimeConversionControl setReadReceipt(ReadReceipt rc) {
		checkRecycled();
		
		short rcAsShort;
		
		switch (rc) {
		case DONT_PASS_READ_RECEIPT:
			rcAsShort = 0;
			break;
		case RETURN_RECEIPT_TO:
			rcAsShort = 1;
			break;
		case DISPOSITION_NOTIFICATION_TO:
			rcAsShort = 2;
			break;
			default:
				throw new IllegalArgumentException("Unknown read receipt: "+rc);
		}
		
		NotesNativeAPI.get().MMSetReadReceipt(m_convControls, rcAsShort);
		return this;
	}
	
	/**
	 * The function returns the Conversions Controls 'skip X-Notes-Item headers' setting.
	 * 
	 * @return if TRUE, don't export any headers named x-notes-item (default FALSE)
	 */
	public boolean getSkipX() {
		checkRecycled();
		
		return NotesNativeAPI.get().MMGetSkipX(m_convControls);
	}
	
	/**
	 * The function sets the Conversions Controls 'skip X-Notes-Item headers' setting to the input value.
	 * 
	 * @param b if TRUE, don't export any headers named x-notes-item (default FALSE)
	 * @return this instance
	 */
	public MimeConversionControl setSkipX(boolean b) {
		checkRecycled();
		
		NotesNativeAPI.get().MMSetSkipX(m_convControls, b);
		return this;
	}
}
