package com.mindoo.domino.jna.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote.IAttachmentProducer;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to create database objects with binary data in small chunks.
 * The code handles allocation of objects with initial size and reallocation
 * with copying existing data if the object needs to grow.
 * 
 * @author Karsten Lehmann
 */
public class DatabaseObjectProducerUtil {
	/** found out via trial and error; 0xFFFFFFFF minus some header */
	private static final long MAX_OBJECTSIZE = 4294966925L; // 0xFFFFFE8D

	private DatabaseObjectProducerUtil() {
	}

	public static class ObjectInfo {
		private long objectSize;
		private int objectId;

		public long getObjectSize() {
			return objectSize;
		}

		public void setObjectSize(long objectSize) {
			this.objectSize = objectSize;
		}

		public int getObjectId() {
			return objectId;
		}

		public void setObjectId(int objectId) {
			this.objectId = objectId;
		}

	}

	/**
	 * Creates a new object in the database. The file content is produced on-the-fly
	 * in an {@link IAttachmentProducer}.
	 * 
	 * @param db parent database
	 * @param noteClass class of note to create
	 * @param objectType object type, e.g. {@link NotesConstants#OBJECT_FILE}
	 * @param producer interface to produce the object data on-the-fly
	 * @return database object size and ID (RRV)
	 */
	public static ObjectInfo createDbObject(NotesDatabase db, short noteClass, short objectType,
			IAttachmentProducer producer) {
		if (db.isRecycled()) {
			throw new NotesError(0, "Database already recycled");
		}

		//use a default initial object size of 1.000.000 bytes if nothing is specified
		long estimatedSize = producer.getSizeEstimation();

		final long initialObjectSize;
		if (estimatedSize > MAX_OBJECTSIZE) {
			initialObjectSize = MAX_OBJECTSIZE;
		}
		else if (estimatedSize < 1) {
			initialObjectSize = 1000000;
		}
		else {
			initialObjectSize = estimatedSize;
		}

		//in-memory buffer to collect data from the producer
		final int copyBufferSize = 5000000;
		final byte[] buffer = new byte[copyBufferSize];
		final AtomicInteger currBufferOffset = new AtomicInteger(0);

		final AtomicLong currFileSize = new AtomicLong(0);

		//allocate memory buffer used to transfer written data to the NSF binary object
		final DHANDLE.ByReference retCopyBufferHandle = DHANDLE.newInstanceByReference();
		short result = Mem.OSMemAlloc((short) 0, copyBufferSize, retCopyBufferHandle);
		NotesErrorUtils.checkResult(result);

		try {
			final IntByReference rtnRRV = new IntByReference();

			//allocate binary object with initial size
			short privs = 0;

			HANDLE.ByValue hDbByVal = db.getHandle().getByValue();
			short allocObjResult = NotesNativeAPI.get().NSFDbAllocObjectExtended2(hDbByVal,
					(int) (initialObjectSize & 0xffffffff),
					noteClass, privs, objectType, rtnRRV);
			NotesErrorUtils.checkResult(allocObjResult);

			try {
				try (OutputStream nsfObjectOutputStream = new OutputStream() {

					@Override
					public void write(int b) throws IOException {

						//write byte value at current buffer array position
						int iCurrBufferOffset = currBufferOffset.get();
						buffer[iCurrBufferOffset] = (byte) (b & 0xff);

						//check if buffer full
						if ((iCurrBufferOffset+1) == copyBufferSize) {
							//check if we need to grow the NSF object
							long newObjectSize = currFileSize.get() + copyBufferSize;
							if (newObjectSize > initialObjectSize) {
								int newRRV = resizeObjectWithData(db, rtnRRV.getValue(),
										noteClass, privs, objectType, newObjectSize);

								if (newRRV != rtnRRV.getValue()) {
									//remove current object
									short freeObjResult = NotesNativeAPI.get().NSFDbFreeObject(hDbByVal, rtnRRV.getValue());
									NotesErrorUtils.checkResult(freeObjResult);

									rtnRRV.setValue(newRRV);
								}
							}

							//copy buffer array data into memory buffer
							Pointer ptrBuffer = Mem.OSLockObject(retCopyBufferHandle);
							try {
								ptrBuffer.write(0, buffer, 0, copyBufferSize);
							}
							finally {
								Mem.OSUnlockObject(retCopyBufferHandle);
							}

							//write memory buffer to NSF object
							short result = NotesNativeAPI.get().NSFDbWriteObject(
									hDbByVal,
									rtnRRV.getValue(),
									retCopyBufferHandle.getByValue(),
									(int) (currFileSize.get() & 0xffffffff),
									copyBufferSize);
							NotesErrorUtils.checkResult(result);

							//increment NSF object offset by bufferSize
							currFileSize.addAndGet(copyBufferSize);
							//reset currBufferOffset
							currBufferOffset.set(0);
						}
						else {
							//buffer not full yet

							//increment buffer offset
							currBufferOffset.incrementAndGet();
						}
					}

				}) {

					producer.produceAttachment(nsfObjectOutputStream);

				}

				long finalFileSize;
				int iCurrBufferOffset = currBufferOffset.get();
				if (iCurrBufferOffset>0) {
					//we need to write the remaining buffer data to the NSF object

					//set the correct total filesize
					finalFileSize = currFileSize.get() + iCurrBufferOffset;
					int newRRV = resizeObjectWithData(db, rtnRRV.getValue(),
							noteClass, privs, objectType, finalFileSize);

					if (newRRV != rtnRRV.getValue()) {
						//remove current object
						short freeObjResult = NotesNativeAPI.get().NSFDbFreeObject(hDbByVal, rtnRRV.getValue());
						NotesErrorUtils.checkResult(freeObjResult);

						rtnRRV.setValue(newRRV);
					}

					//copy buffer array data into memory buffer
					Pointer ptrBuffer = Mem.OSLockObject(retCopyBufferHandle.getByValue());
					try {
						ptrBuffer.write(0, buffer, 0, iCurrBufferOffset);
					}
					finally {
						Mem.OSUnlockObject(retCopyBufferHandle.getByValue());
					}

					//write memory buffer to NSF object
					short writeObjResult = NotesNativeAPI.get().NSFDbWriteObject(
							hDbByVal,
							rtnRRV.getValue(),
							retCopyBufferHandle.getByValue(),
							(int) (currFileSize.get() & 0xffffffff),
							iCurrBufferOffset);
					NotesErrorUtils.checkResult(writeObjResult);

					currFileSize.set(finalFileSize);
				}
				else if (initialObjectSize != currFileSize.get()) {
					//shrink data object to the actual size
					finalFileSize = currFileSize.get();

					//make sure the object has the right size
					int newRRV = resizeObjectWithData(db, rtnRRV.getValue(),
							noteClass, privs, objectType, currFileSize.get());

					if (newRRV != rtnRRV.getValue()) {
						//remove current object
						short freeObjResult = NotesNativeAPI.get().NSFDbFreeObject(hDbByVal, rtnRRV.getValue());
						NotesErrorUtils.checkResult(freeObjResult);

						rtnRRV.setValue(newRRV);
					}
				}
				else {
					finalFileSize = currFileSize.get();
				}

				ObjectInfo objInfo = new ObjectInfo();
				objInfo.setObjectId(rtnRRV.getValue());
				objInfo.setObjectSize(finalFileSize);
				
				return objInfo;
			}
			catch (Exception e) {
				//delete the object in case of errors
				short freeObjResult = NotesNativeAPI.get().NSFDbFreeObject(hDbByVal, rtnRRV.getValue());
				NotesErrorUtils.checkResult(freeObjResult);
				throw new NotesError(0, "Error creating binary NSF DB object", e);
			}

		}
		finally {
			//free copy buffer
			Mem.OSMemFree(retCopyBufferHandle.getByValue());
		}
	}

	/**
	 * Method to resize a database object. If the object needs to grow, we allocate a new object,
	 * transfer the data and return the new object id.
	 * 
	 * @param db database
	 * @param objectId object ID of the existing object
	 * @param noteClass object class
	 * @param objectType object type
	 * @param newSize new object size
	 * @return new object ID (RRV); same as <code>rrv</code> if object is shrinked
	 */
	private static int resizeObjectWithData(NotesDatabase db, int objectId, short noteClass, short privs,
			short objectType, long newSize) {

		if (newSize > MAX_OBJECTSIZE) {
			throw new IllegalArgumentException(MessageFormat.format("Max DB object size exceeded ({0}>{1})",
					Long.toString(newSize), Long.toString(MAX_OBJECTSIZE)));
		}

		HANDLE.ByValue hDbByVal = db.getHandle().getByValue();

		IntByReference retSize = new IntByReference();
		ShortByReference retClass = new ShortByReference();
		ShortByReference retPrivileges = new ShortByReference();

		//read current size of database object
		short result = NotesNativeAPI.get().NSFDbGetObjectSize(hDbByVal,
				objectId, objectType, retSize, retClass, retPrivileges);
		NotesErrorUtils.checkResult(result);

		long currentSize = Integer.toUnsignedLong(retSize.getValue());

		if (currentSize == newSize) {
			//size is ok
			return objectId;
		}
		else if (currentSize > newSize) {
			//shrink object (keeps data)
			result = NotesNativeAPI.get().NSFDbReallocObject(hDbByVal, objectId, 
					(int) (newSize & 0xffffffff));
			NotesErrorUtils.checkResult(result);
			return objectId;
		}
		else {
			//create a new object and copy the data
			IntByReference rtnNewRRV = new IntByReference();
			result = NotesNativeAPI.get().NSFDbAllocObjectExtended2(hDbByVal,
					(int) (newSize & 0xffffffff), noteClass, privs, objectType, rtnNewRRV);
			NotesErrorUtils.checkResult(result);

			try {
				final int copyBufferSize = 20000000;

				AtomicLong currOffset = new AtomicLong(0);

				for (; currOffset.get() < currentSize; currOffset.addAndGet(copyBufferSize)) {
					long bytesToCopy = Math.min(copyBufferSize, currentSize - currOffset.get());

					if (bytesToCopy > 0) {

						//handle to receive object data
						DHANDLE.ByReference retCopyBufferHandle = DHANDLE.newInstanceByReference();

						short readResult = NotesNativeAPI.get().NSFDbReadObject(
								hDbByVal,
								objectId,
								(int) (currOffset.get() & 0xffffffff),
								(int) (bytesToCopy & 0xffffffff),
								retCopyBufferHandle);
						NotesErrorUtils.checkResult(readResult);

						try {
							short writeResult = NotesNativeAPI.get().NSFDbWriteObject(
									hDbByVal,
									rtnNewRRV.getValue(),
									retCopyBufferHandle.getByValue(),
									(int) (currOffset.get() & 0xffffffff),
									(int) (bytesToCopy  &0xffffffff));
							NotesErrorUtils.checkResult(writeResult);
						}
						finally {
							Mem.OSMemFree(retCopyBufferHandle.getByValue());
						}
					}
				}

				return rtnNewRRV.getValue();
			}
			catch (Exception e) {
				//delete the object in case of errors
				short freeObjResult = NotesNativeAPI.get().NSFDbFreeObject(hDbByVal, rtnNewRRV.getValue());
				NotesErrorUtils.checkResult(freeObjResult);

				throw new NotesError(0, "Error creating binary NSF DB object", e);
			}

		}
	}
}
