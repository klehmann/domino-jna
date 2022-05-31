package com.mindoo.domino.jna;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class for reading and writing database objects
 * 
 * @author Karsten Lehmann
 */
public class NotesDatabaseObject {
	private NotesDatabase db;
	private int objId;
	private boolean deleted;
	
	private int size;
	private Set<NoteClass> objClass;
	private short privileges;
	private int sizeModCnt;
	
	private int bufSize = -1;
	private byte[] buf;
	//database object offset of first byte in "buf"
	int bufStartOffset = -1;
	//database object offset of last byte in "buf"
	int bufEndOffset = -1;

	/**
	 * Allocates a new database object
	 * 
	 * @param db parent database
	 * @param size object size
	 * @param noteClass Object class. Normally, set this to {@link NoteClass#DOCUMENT}. Domino and Notes uses the same classes for objects as for notes. See {@link NoteClass}
	 * @param privileges Privilege mask controlling access to this object. Specify zero if not used.
	 * @param objType type of object, e.g. {@link NotesConstants#OBJECT_FILE}
	 */
	public NotesDatabaseObject(NotesDatabase db, int size, Set<NoteClass> noteClass, short privileges, short objType) {
		if (db.isRecycled()) {
			throw new NotesError("Parent database is recycled");
		}
		
		this.db = db;
		
		if (size<=0) {
			throw new IllegalArgumentException("Size must be greater than zero");
		}
		
		HANDLE.ByValue hDbByVal = db.getHandle().getByValue();
		
		IntByReference rtnRRV = new IntByReference();
		
		short result = NotesNativeAPI.get().NSFDbAllocObjectExtended2(hDbByVal,
			size, NoteClass.toBitMask(noteClass), privileges, objType, rtnRRV);
		NotesErrorUtils.checkResult(result);
		
		this.objId = rtnRRV.getValue();
		this.size = size;
		this.objClass = noteClass;
		this.privileges = privileges;
		
		//allocate space for the read buffer
		this.bufSize = Math.min(size, 500000);
		this.buf = new byte[this.bufSize];
	}
	
	/**
	 * Opens an existing database object
	 * 
	 * @param db parent database
	 * @param objId object id (RRV)
	 */
	public NotesDatabaseObject(NotesDatabase db, int objId) {
		this.db = db;
		this.objId = objId;
		
		//read size and type of db object
		IntByReference retSize = new IntByReference();
		ShortByReference retClass = new ShortByReference();
		ShortByReference retPrivileges = new ShortByReference();

		HANDLE.ByValue hDbByVal = db.getHandle().getByValue();
		
		short result = NotesNativeAPI.get().NSFDbGetObjectSize(
				hDbByVal,
				objId,
				NotesConstants.OBJECT_UNKNOWN,
				retSize,
				retClass,
				retPrivileges);
		NotesErrorUtils.checkResult(result);

		this.size = retSize.getValue();
		this.objClass = NoteClass.toNoteClasses(retClass.getValue());
		this.privileges = retClass.getValue();
		
		//allocate space for the read buffer
		this.bufSize = Math.min(size, 500000);
		if (this.bufSize==0) {
			this.bufSize=1;
		}
		this.buf = new byte[this.bufSize];
	}
	
	public int getRRV() {
		return this.objId;
	}
	
	public int size() {
		return this.size;
	}

	public Set<NoteClass> getNoteClass() {
		return this.objClass;
	}
	
	public short getPrivileges() {
		return this.privileges;
	}
	
	private void checkRecycled() {
		if (this.db.isRecycled()) {
			throw new NotesError("Parent database is recycled");
		}
		if (this.deleted) {
			throw new NotesError("Database object has been removed from the NSF");
		}
	}
	
	/**
	 * Writes the remaining data in the {@link ByteBuffer} into the database object
	 * 
	 * @param objOffset write position in database object
	 * @param buf buffer to write
	 * @return this instance
	 */
	public NotesDatabaseObject put(int objOffset, byte[] buf) {
		return put(objOffset, buf, 0, buf.length);
	}

	/**
	 * Writes the remaining data in the {@link ByteBuffer} into the database object
	 * 
	 * @param objOffset write position in database object
	 * @param buf buffer to write
	 * @param arrOffset start offset in buffer array
	 * @param count number of bytes to copy
	 * @return this instance
	 */
	public NotesDatabaseObject put(int objOffset, byte[] buf, int arrOffset, int count) {
		ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.nativeOrder());
		bb.position(arrOffset);
		bb = bb.slice().order(ByteOrder.nativeOrder());
		bb.limit(count);
		
		return put(objOffset, bb);
	}
	
	/**
	 * Writes the remaining data in the {@link ByteBuffer} into the database object
	 * 
	 * @param objOffset write position in database object
	 * @param buf buffer to write
	 * @return this instance
	 */
	public NotesDatabaseObject put(int objOffset, ByteBuffer buf) {
		checkRecycled();

		int remainingInBuf = buf.remaining();
		if (remainingInBuf==0) {
			return this;
		}
		
		byte[] dataToWrite = new byte[remainingInBuf];
		buf.get(dataToWrite);
		
		HANDLE.ByValue hDbByVal = this.db.getHandle().getByValue();
		
		//allocate copy buffer
		final DHANDLE.ByReference retCopyBufferHandle = DHANDLE.newInstanceByReference();
		short result = Mem.OSMemAlloc((short) 0, dataToWrite.length, retCopyBufferHandle);
		NotesErrorUtils.checkResult(result);

		try {
			//write data into copy buffer
			Pointer ptrBuffer = Mem.OSLockObject(retCopyBufferHandle);
			try {
				ptrBuffer.write(0, dataToWrite, 0, dataToWrite.length);
			}
			finally {
				Mem.OSUnlockObject(retCopyBufferHandle);
			}

			//and write copy buffer to database object
			result = NotesNativeAPI.get().NSFDbWriteObject(
					hDbByVal,
				objId,
				retCopyBufferHandle.getByValue(),
				objOffset,
				dataToWrite.length);
			NotesErrorUtils.checkResult(result);
			
		}
		finally {
			//free copy buffer
			Mem.OSMemFree(retCopyBufferHandle.getByValue());
		}
		
		//invalidate our read cache
		this.bufStartOffset = -1;
		this.bufEndOffset = -1;
		
		return this;
	}
	
	/**
	 * Copies the content of this database object into another one
	 * 
	 * @param targetObj target database object
	 * @return this instance
	 */
	public NotesDatabaseObject copyInto(NotesDatabaseObject targetObj) {
		byte[] buf = new byte[Math.min(size(), 500000)];
		int len = 0;
		
		int targetOffset = 0;
		
		for (int i=0; i<size(); i++) {
			byte b = get(i);
			buf[len++] = b;
			
			if (len==buf.length) {
				//flush buffer
				targetObj.put(targetOffset, buf, 0, len);
				targetOffset += len;
				len = 0;
			}
			
			if (i == targetObj.size()) {
				//end of target reached
				break;
			}
		}
		
		if (len>0) {
			targetObj.put(targetOffset, buf, 0, len);
		}
		return this;
	}
	
	/**
	 * Changes the size of the database object
	 * 
	 * @param newSize new size
	 * @param keepContent true to transfer the old content
	 * @return this instance
	 */
	public NotesDatabaseObject reallocate(int newSize, boolean keepContent) {
		checkRecycled();

		if (size() == newSize) {
			//no action required
			return this;
		}
		
		NotesDatabaseObject tmpObject = null;
		int oldSize = size();
		try {
			if (keepContent) {
				tmpObject = new NotesDatabaseObject(this.db, size(), getNoteClass(), getPrivileges(), NotesConstants.OBJECT_FILE);
				copyInto(tmpObject);
			}

			HANDLE.ByValue hDbByVal = this.db.getHandle().getByValue();
			short result = NotesNativeAPI.get().NSFDbReallocObject(hDbByVal, objId, newSize);
			NotesErrorUtils.checkResult(result);
			this.size = newSize;

			if (keepContent && tmpObject!=null) {
				tmpObject.copyInto(this);
			}

			if (oldSize != newSize) {
				//invalidates open InputStreams
				sizeModCnt++;
			}
			
			return this;
		}
		finally {
			if (tmpObject!=null) {
				tmpObject.delete();
			}
		}
	}

	/**
	 * Deletes the database object from the NSF
	 */
	public void delete() {
		checkRecycled();
		if (this.deleted) {
			return;
		}
		
		HANDLE.ByValue hDbByVal = this.db.getHandle().getByValue();
		short result = NotesNativeAPI.get().NSFDbFreeObject(hDbByVal, objId);
		NotesErrorUtils.checkResult(result);
		this.deleted = true;
	}
	
	public boolean isDeleted() {
		return this.deleted;
	}

	/**
	 * Copies the content of this database object into a {@link ByteBuffer}
	 * 
	 * @return byte buffer
	 */
	public ByteBuffer asByteBuffer() {
		byte[] data = new byte[size()];
		get(0, data);
		return ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
	}
	
	/**
	 * Reads content of this database object into the specified byte array
	 * 
	 * @param offset offset in database object
	 * @param arr target byte array
	 * @return this instance
	 */
	public NotesDatabaseObject get(int offset, byte[] arr) {
		return get(offset, arr, 0, arr.length);
	}
	
	/**
	 * Reads content of this database object into the specified byte array
	 * 
	 * @param offset offset in database object
	 * @param arr target byte array
	 * @param arrOffset start writing at this array offset
	 * @param count number of bytes to read
	 * @return this instance
	 */
	public NotesDatabaseObject get(int offset, byte[] arr, int arrOffset, int count) {
		for (int i=0; i<count; i++) {
			arr[arrOffset + i] = get(offset + i);
		}
		return this;
	}
	
	/**
	 * Reads a single byte at the specified offset, using internal caching to
	 * improve throughput
	 * 
	 * @param offset read offset
	 * @return database object content
	 */
	public byte get(int offset) {
		if (this.buf!=null && this.bufSize!=-1 && this.bufStartOffset!=-1 && this.bufEndOffset!=-1) {
			//check if we have that content in the buffer
			if (offset >= this.bufStartOffset && offset <= this.bufEndOffset) {
				return buf[offset - bufStartOffset];
			}
		}
		
		int pageIdx = (offset / this.bufSize);
		int pageOffset = pageIdx * this.bufSize;
		int bytesToRead = Math.min(size() - pageOffset, this.buf.length);
		
		
		//fetch new data
		checkRecycled();
		HANDLE.ByValue hDbByVal = this.db.getHandle().getByValue();
		
		if (bytesToRead<=0) {
			throw new UncheckedIOException(new IOException("No more data to read"));
		}
		
		DHANDLE.ByReference rethBuffer = DHANDLE.newInstanceByReference();
		
		short result = NotesNativeAPI.get().NSFDbReadObject(
				hDbByVal,
				this.objId,
				pageOffset,
				bytesToRead,
				rethBuffer);
		NotesErrorUtils.checkResult(result);
		
		byte[] dbObjData;

		Pointer ptr = Mem.OSLockObject(rethBuffer.getByValue());
		try {
			dbObjData = ptr.getByteArray(0, bytesToRead);
			System.arraycopy(dbObjData, 0, this.buf, 0, bytesToRead);

			this.bufStartOffset = pageOffset;
			this.bufEndOffset = pageOffset + dbObjData.length - 1;
		}
		finally {
			Mem.OSUnlockObject(rethBuffer.getByValue());
			result = Mem.OSMemFree(rethBuffer.getByValue());
			NotesErrorUtils.checkResult(result);
		}

		return buf[bufStartOffset - offset];
	}
	
	public InputStream asStream() {
		return asStream(0);
	}
	
	public InputStream asStream(int offset) {
		int currentSizeModCnt = sizeModCnt;
		int size = size();
		int fOffset = offset;
		
		return new InputStream() {
			int offset = fOffset;

			@Override
			public int read() throws IOException {
				checkRecycled();

				if (currentSizeModCnt != sizeModCnt) {
					throw new IOException("Database object has been resized. InputStream is invalid");
				}

				if (offset >= size) {
					return -1;
				}

				byte v = get(offset++);
				return (int) v;
			}};
	}

}
