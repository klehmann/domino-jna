package com.mindoo.domino.jna.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;

import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.sun.jna.Memory;

/**
 * Implementation of {@link IRichTextNavigator} that works with an on-disk CD record file
 * 
 * @author Karsten Lehmann
 */
public class CDFileRichTextNavigator implements IRichTextNavigator {
	private FileInputStream m_fileIn;
	private String m_filePath;
	private FileChannel m_fileChannel;
	private long m_fileSize;
	private long m_position;
	private CDRecordMemory m_currentCDRecord;
	private Map<Integer,Integer> m_cdRecordSizeAtIndex;
	private Map<Long,Integer> m_cdRecordIndexAtFilePos;
	private int m_currentCDRecordIndex;
	private long m_lastElementPosition = -1;
	private int m_lastElementIndex = -1;
	
	public CDFileRichTextNavigator(FileInputStream cdFileStream, String filePath, long fileSize) throws IOException {
		m_fileIn = cdFileStream;
		m_filePath = filePath;
		m_fileSize = fileSize;
		m_fileChannel = m_fileIn.getChannel();
		m_cdRecordSizeAtIndex = new TreeMap<Integer, Integer>();
		m_cdRecordIndexAtFilePos = new TreeMap<Long, Integer>();
		gotoFirst();
	}
	
	/**
	 * Reads the CD record information at the current file position
	 * 
	 * @throws IOException
	 */
	private CDRecordMemory readCurrentCDRecordUnchecked() {
		try {
			return readCurrentCDRecord();
		} catch (IOException e) {
			throw new NotesError(0, "Error reading CD record at position "+m_position+" of file "+m_filePath, e);
		}
	}
	
	/**
	 * Reads the CD record information at the current file position
	 * 
	 * @throws IOException
	 */
	private CDRecordMemory readCurrentCDRecord() throws IOException {
		m_fileChannel.position(m_position);
		
		Memory signatureMem = new Memory(2);
		ByteBuffer signatureBuf = signatureMem.getByteBuffer(0, signatureMem.size());
		m_fileChannel.read(signatureBuf);
		
		short typeAsShort = signatureMem.getShort(0);
		int dwLength;

		/* structures used to define and read the signatures 

			 0		   1
		+---------+---------+
		|   Sig   |  Length	|						Byte signature
		+---------+---------+

			 0		   1        2         3
		+---------+---------+---------+---------+
		|   Sig   |   ff    |		Length	   |		Word signature
		+---------+---------+---------+---------+

			 0		   1        2         3          4         5
		+---------+---------+---------+---------+---------+---------+
		|   Sig   |   00	    |                 Length		           | DWord signature
		+---------+---------+---------+---------+---------+---------+

		 */

		short highOrderByte = (short) (typeAsShort & 0xFF00);
		int fixedSize;

		switch (highOrderByte) {
		case NotesConstants.LONGRECORDLENGTH:      /* LSIG */
			Memory intLengthMem = new Memory(4);
			ByteBuffer intLengthBuf = intLengthMem.getByteBuffer(0, intLengthMem.size());
			m_fileChannel.read(intLengthBuf);
			dwLength = intLengthMem.getInt(0);

			fixedSize = 6; //sizeof(LSIG);

			break;

		case NotesConstants.WORDRECORDLENGTH:      /* WSIG */
			Memory shortLengthMem = new Memory(2);
			ByteBuffer shortLengthBuf = shortLengthMem.getByteBuffer(0, shortLengthMem.size());
			m_fileChannel.read(shortLengthBuf);
			dwLength = (int) (shortLengthMem.getShort(0) & 0xffff);

			fixedSize = 4; //sizeof(WSIG);

			break;

		default:                    /* BSIG */
			dwLength = (int) ((typeAsShort >> 8) & 0x00ff);
			typeAsShort &= 0x00FF; /* Length not part of signature */
			fixedSize = 2; //sizeof(BSIG);
		}
		
		//file channel position points to the start of data, so reset it to the CD record start
		m_fileChannel.position(m_position);
		int cdRecordTotalLength = dwLength;
		ReadOnlyMemory cdRecordMem = new ReadOnlyMemory(cdRecordTotalLength);
		int bytesRead = m_fileChannel.read(cdRecordMem.getByteBuffer(0, cdRecordMem.size()));
		if (bytesRead != cdRecordTotalLength) {
			throw new IllegalStateException("Bytes read from CD record file for CD record at index "+m_currentCDRecordIndex+" is expected to be "+cdRecordTotalLength+" but we only could read "+bytesRead+" bytes");
		}
		cdRecordMem.seal();
		
		CDRecordMemory record = new CDRecordMemory(cdRecordMem, typeAsShort, dwLength-fixedSize, dwLength);
		//remember the length of the CD records
		m_cdRecordSizeAtIndex.put(m_currentCDRecordIndex, record.getCDRecordLength());
		m_cdRecordIndexAtFilePos.put(m_position, m_currentCDRecordIndex);
		
		return record;
	}
	
	@Override
	public boolean isEmpty() {
		return m_fileSize<=2;
	}

	@Override
	public boolean gotoFirst() {
		if (isEmpty())
			return false;
		
		if (m_position!=2 || m_currentCDRecord==null) {
			m_position = 2; // datatype TYPE_COMPOSITE (WORD)
			m_currentCDRecordIndex = 0;
			m_currentCDRecord = readCurrentCDRecordUnchecked();
		}
		return true;
	}

	@Override
	public boolean gotoLast() {
		if (m_lastElementPosition!=-1 && m_lastElementIndex!=-1) {
			//we already know the exact position, because we have been there before
			m_position = m_lastElementPosition;
			m_currentCDRecordIndex = m_lastElementIndex;
			m_currentCDRecord = readCurrentCDRecordUnchecked();
			return true;
		}
		else {
			if (gotoFirst()) {
				CDRecordMemory lastReadRecord = null;
				long lastReadRecordPosition;
				int lastReadRecordIndex;
				
				do {
					lastReadRecord = readCurrentCDRecordUnchecked();
					lastReadRecordPosition = m_position;
					lastReadRecordIndex = m_currentCDRecordIndex;
				}
				while (gotoNext());
				
				m_position = lastReadRecordPosition;
				m_currentCDRecord = lastReadRecord;
				m_currentCDRecordIndex = lastReadRecordIndex;
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	public boolean gotoNext() {
		int cdRecordLength = m_currentCDRecord.getCDRecordLength();
		long nextPosition = m_position + cdRecordLength;
		if ((nextPosition & 1L)==1) {
            nextPosition += 1;
        }
		if (nextPosition>=m_fileSize)
			return false;
		try {
			m_fileChannel.position(nextPosition);
		} catch (IOException e) {
			throw new NotesError(0, "Error navigating to position "+nextPosition+" of file "+m_filePath+" with size "+m_fileSize, e);
		}
		m_position = nextPosition;
		m_currentCDRecord = readCurrentCDRecordUnchecked();
		return true;
	}

	@Override
	public boolean gotoPrev() {
		if (m_currentCDRecordIndex==-1) {
			return false;
		}
		else if (m_currentCDRecordIndex>0) {
			m_currentCDRecordIndex--;
			long prevRecordLength = m_cdRecordSizeAtIndex.get(m_currentCDRecordIndex);
			long prevPosition = m_position - prevRecordLength;
			try {
				m_fileChannel.position(prevPosition);
			} catch (IOException e) {
				throw new NotesError(0, "Error navigating to position "+prevPosition+" of file "+m_filePath+" with size "+m_fileSize, e);
			}
			
			m_position = prevPosition;
			m_currentCDRecord = readCurrentCDRecordUnchecked();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean hasNext() {
		if (m_currentCDRecord==null)
			return false;
		return (m_position + m_currentCDRecord.getCDRecordLength()) < m_fileSize;
	}

	@Override
	public boolean hasPrev() {
		return m_currentCDRecordIndex>0;
	}

	@Override
	public Memory getCurrentRecordData() {
		if (m_currentCDRecord==null)
			return null;
		return m_currentCDRecord.getRecordDataWithoutHeader();
	}

	@Override
	public short getCurrentRecordTypeAsShort() {
		if (m_currentCDRecord==null)
			return 0;
		return m_currentCDRecord.getTypeAsShort();
	}

	@Override
	public int getCurrentRecordDataLength() {
		if (m_currentCDRecord==null)
			return 0;
		return m_currentCDRecord.getDataSize();
	}

	@Override
	public int getCurrentRecordTotalLength() {
		if (m_currentCDRecord==null)
			return 0;
		return m_currentCDRecord.getCDRecordLength();
	}

	@Override
	public RichTextNavPosition getCurrentRecordPosition() {
		return new RichTextNavPositionImpl(this, m_position);
	}

	@Override
	public void restoreCurrentRecordPosition(RichTextNavPosition pos) {
		if (!(pos instanceof RichTextNavPositionImpl))
			throw new IllegalArgumentException("Invalid position, not generated by this navigator");
		
		RichTextNavPositionImpl posImpl = (RichTextNavPositionImpl) pos;
		if (posImpl.m_parentNav!=this)
			throw new IllegalArgumentException("Invalid position, not generated by this navigator");

		if (!gotoFirst()) {
			throw new IllegalStateException("File does not have any content: "+m_filePath);
		}
		long targetFilePos = posImpl.m_filePosition;
		Integer indexAtFilePos = m_cdRecordIndexAtFilePos.get(targetFilePos);
		if (indexAtFilePos==null) {
			throw new IllegalArgumentException("Unknown position");
		}
		m_position = posImpl.m_filePosition;
		m_currentCDRecordIndex = indexAtFilePos.intValue();
		m_currentCDRecord = readCurrentCDRecordUnchecked();
	}
	
	@Override
	public void copyCurrentRecordTo(ICompoundText target) {
		if (m_currentCDRecord==null)
			throw new IllegalStateException("Current CD record is null");
		
		CompoundTextWriter ctWriter = target.getAdapter(CompoundTextWriter.class);
		if (ctWriter==null)
			throw new NotesError(0, "Could not get "+CompoundTextWriter.class.getSimpleName()+" from "+RichTextBuilder.class.getSimpleName());
		if (ctWriter.isClosed())
			throw new NotesError(0, "Target compound text is already closed");

		Memory cdRecordMem = m_currentCDRecord.getRecordDataWithHeader();
		int totalCDRecordLength = m_currentCDRecord.getCDRecordLength();
		ctWriter.addCDRecords(cdRecordMem, totalCDRecordLength);
	}
	
	private class RichTextNavPositionImpl implements RichTextNavPosition {
		private IRichTextNavigator m_parentNav;
		private long m_filePosition;
		
		public RichTextNavPositionImpl(IRichTextNavigator parentNav, long filePosition) {
			m_parentNav = parentNav;
			m_filePosition = filePosition;
		}

		private CDFileRichTextNavigator getOuterType() {
			return CDFileRichTextNavigator.this;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (m_filePosition ^ (m_filePosition >>> 32));
			result = prime * result + ((m_parentNav == null) ? 0 : m_parentNav.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RichTextNavPositionImpl other = (RichTextNavPositionImpl) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (m_filePosition != other.m_filePosition)
				return false;
			if (m_parentNav == null) {
				if (other.m_parentNav != null)
					return false;
			} else if (!m_parentNav.equals(other.m_parentNav))
				return false;
			return true;
		}

	}
	/**
	 * Data container for a single CD record
	 * 
	 * @author Karsten Lehmann
	 */
	private class CDRecordMemory {
		private ReadOnlyMemory m_cdRecordBuf;
		private short m_typeAsShort;
		private int m_dataSize;
		private int m_cdRecordLength;
		
		public CDRecordMemory(ReadOnlyMemory recordBuf, short typeAsShort, int dataSize, int cdRecordLength) {
			m_cdRecordBuf = recordBuf;
			m_typeAsShort = typeAsShort;
			m_dataSize = dataSize;
			m_cdRecordLength = cdRecordLength;
		}
		
		public Memory getRecordDataWithHeader() {
			return m_cdRecordBuf;
		}
		
		public Memory getRecordDataWithoutHeader() {
			return (Memory) m_cdRecordBuf.share(m_cdRecordLength - m_dataSize);
		}

		/**
		 * Use this value in
		 * {@link CDRecordType#getRecordTypeForConstant(short, com.mindoo.domino.jna.constants.CDRecordType.Area)}
		 * to get an enum value
		 * 
		 * @return CD record type
		 */
		public short getTypeAsShort() {
			return m_typeAsShort;
		}
		
		public int getDataSize() {
			return m_dataSize;
		}
		
		public int getCDRecordLength() {
			return m_cdRecordLength;
		}
	}

}