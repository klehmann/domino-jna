package com.mindoo.domino.jna.mq;

import java.nio.ByteBuffer;

import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.mq.MessageQueue.IMQCallback.Action;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Message queues provide Domino and Notes applications with inter-process communication capabilities,
 * e.g. to communicate between external applications and code running in the Notes Client or
 * between different components runnings in the same Notes Client or Domino server (e.g. between code
 * running in the HTTP task and DOTS code).<br>
 * <br>
 * Set <a href="http://www-12.lotus.com/ldd/doc/domino_notes/9.0/api90ug.nsf/85255d56004d2bfd85255b1800631684/05faf7d6737bf1d085256368005a7b9d?OpenDocument" target="_blank">the API documentation</a>
 * for for information about message queues.
 * 
 * @author Karsten Lehmann
 */
public class MessageQueue implements IRecyclableNotesObject {
	private String m_queueName;
	private int m_queue;

	/**
	 * Creates a new instance. Use {@link #createAndOpen(String, int, boolean)} to create a
	 * new queue or {@link #open(String, boolean)} to open an existing one.
	 * 
	 * @param queueName name of the message queue
	 * @param queue handle
	 */
	private MessageQueue(String queueName, int queue) {
		m_queueName = queueName;
		m_queue = queue;
	}

	@Override
	public String toString() {
		if (isRecycled()) {
			return "MessageQueue [recycled]";
		}
		else {
			return "MessageQueue [handle="+m_queue+", name="+m_queueName+"]";
		}
	}
	
	void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_queue==0)
				throw new NotesError(0, "MessageQueue already recycled");
			NotesGC.__b64_checkValidObjectHandle(MessageQueue.class, m_queue);
		}
		else {
			if (m_queue==0)
				throw new NotesError(0, "MessageQueue already recycled");
			NotesGC.__b32_checkValidObjectHandle(MessageQueue.class, m_queue);
		}
	}

	/**
	 * Returns the name of the message queue
	 * 
	 * @return name
	 */
	public String getName() {
		return m_queueName;
	}

	/**
	 * Method to test whether a queue with a specified name exists (tries to open the queue)
	 * 
	 * @param queueName queue name
	 * @return true if queue exists
	 */
	public static boolean hasQueue(String queueName) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory queueNameMem = NotesStringUtils.toLMBCS(queueName, true);

		IntByReference retQueue = new IntByReference();
		short result = notesAPI.MQOpen(queueNameMem, 0, retQueue);
		if (result==INotesErrorConstants.ERR_NO_SUCH_MQ) {
			return false;
		}
		else if (result==0) {
			result = notesAPI.MQClose(retQueue.getValue(), 0);
			NotesErrorUtils.checkResult(result);
			return true;
		}
		else {
			NotesErrorUtils.checkResult(result);
			return false;
		}
	}
	
	/**
	 * Create a message queue with the specified name.
	 * 
	 * @param queueName name to be assigned to the message queue
	 * @param quota Maximum number of messages that the queue may contain. Set this to zero for the default maximum. The default maximum number of messages that the queue may contain is MAXWORD.
	 * @param noRecycle true to exclude the queue from auto GC so that it can be used later on by other processes
	 * @return queue
	 */
	public static MessageQueue createAndOpen(String queueName, int quota, boolean noRecycle) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory queueNameMem = NotesStringUtils.toLMBCS(queueName, true);

		short result = notesAPI.MQCreate(queueNameMem, (short) (quota & 0xffff), 0);
		NotesErrorUtils.checkResult(result);

		IntByReference retQueue = new IntByReference();
		result = notesAPI.MQOpen(queueNameMem, 0, retQueue);
		NotesErrorUtils.checkResult(result);

		MessageQueue queue = new MessageQueue(queueName, retQueue.getValue());
		if (!noRecycle) {
			NotesGC.__objectCreated(MessageQueue.class, queue);
		}
		return queue;
	}

	/**
	 * Open a message queue, get a handle to it, and increment the queue's reference counter.
	 * The handle is used by the functions that write to and read from the message queue.
	 * 
	 * @param queueName name of the queue that is to be opened
	 * @param createOnFail true to create the queue if it doesn't exist
	 * @return queue
	 */
	public static MessageQueue open(String queueName, boolean createOnFail) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory queueNameMem = NotesStringUtils.toLMBCS(queueName, true);

		IntByReference retQueue = new IntByReference();
		short result = notesAPI.MQOpen(queueNameMem, createOnFail ? NotesCAPI.MQ_OPEN_CREATE : 0, retQueue);
		NotesErrorUtils.checkResult(result);

		MessageQueue queue = new MessageQueue(queueName, retQueue.getValue());
		NotesGC.__objectCreated(MessageQueue.class, queue);
		return queue;
	}

	/**
	 * This function parses a message queue, calling an action routine for each message in the queue.<br>
	 * If the message queue is empty, or if all messages in the queue have been enumerated without
	 * returning an error code, MQScan returns ERR_MQ_EMPTY.<br>
	 * <br>
	 * In the simple case, MQScan() does not modify the contents of the queue;
	 * by returning the appropriate error codes to MQScan, the action routine can specify that
	 * messages are to be removed from the queue or skipped, or that enumeration is to be
	 * terminated immediately.  See the reference entry for {@link IMQCallback} for more details.<br>
	 * <br>
	 * Note: MQScan locks out all other message queue function calls until it completes.

	 * @param buffer buffer to be used to read messages, max size is {@link NotesCAPI#MQ_MAX_MSGSIZE} (65326 bytes)
	 * @param callback callback to be called for each message; if null, we dequeue the a message and return it in the specified buffer
	 * @return The number of bytes written to the buffer (important if <code>callback</code> has been set to null)
	 */
	public int scan(byte[] buffer, final IMQCallback callback) {
		return scan(buffer, 0, buffer.length, callback);
	}

	/**
	 * This function parses a message queue, calling an action routine for each message in the queue.<br>
	 * If the message queue is empty, or if all messages in the queue have been enumerated without
	 * returning an error code, MQScan returns ERR_MQ_EMPTY.<br>
	 * <br>
	 * In the simple case, MQScan() does not modify the contents of the queue;
	 * by returning the appropriate error codes to MQScan, the action routine can specify that
	 * messages are to be removed from the queue or skipped, or that enumeration is to be
	 * terminated immediately.  See the reference entry for {@link IMQCallback} for more details.<br>
	 * <br>
	 * Note: MQScan locks out all other message queue function calls until it completes.

	 * @param buffer buffer to be used to read messages, max size is {@link NotesCAPI#MQ_MAX_MSGSIZE} (65326 bytes)
	 * @param offset the offset in the buffer where to start writing the message
	 * @param length the max length of the message in the buffer
	 * @param callback callback to be called for each message; if null, we dequeue the a message and return it in the specified buffer
	 * @return The number of bytes written to the buffer (important if <code>callback</code> has been set to null)
	 */
	public int scan(byte[] buffer, int offset, int length, final IMQCallback callback) {
		checkHandle();
		if (buffer!=null && length==0) {
			throw new IllegalArgumentException("Buffer cannot be empty");
		}
		else if (buffer!=null && length > NotesCAPI.MQ_MAX_MSGSIZE) {
			throw new IllegalArgumentException("Max size for the buffer is "+NotesCAPI.MQ_MAX_MSGSIZE+" bytes. You specified one with "+length+" bytes.");
		}

		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesCAPI.MQScanCallback cCallback;
		if (notesAPI instanceof WinNotesCAPI) {
			cCallback = new WinNotesCAPI.MQScanCallbackWin() {

				@Override
				public short invoke(Pointer pBuffer, short length, short priority, Pointer ctx) {
					if (callback==null) {
						return INotesErrorConstants.ERR_MQSCAN_DEQUEUE;
					}
					ByteBuffer byteBuf = pBuffer.getByteBuffer(0, length & 0xffff);
					ByteBuffer roByteBuf = byteBuf.asReadOnlyBuffer();

					Action action = callback.dataReceived(roByteBuf, priority & 0xffff);
					switch (action) {
					case Continue:
						return 0;
					case Abort:
						return INotesErrorConstants.ERR_MQSCAN_ABORT;
					case Dequeue:
						return INotesErrorConstants.ERR_MQSCAN_DEQUEUE;
					case Delete:
						return INotesErrorConstants.ERR_MQSCAN_DELETE;
					}

					return 0;
				}
			};
		}
		else {
			cCallback = new NotesCAPI.MQScanCallback() {

				@Override
				public short invoke(Pointer pBuffer, short length, short priority, Pointer ctx) {
					if (callback==null) {
						return INotesErrorConstants.ERR_MQSCAN_DEQUEUE;
					}
					ByteBuffer byteBuf = pBuffer.getByteBuffer(0, length & 0xffff);
					ByteBuffer roByteBuf = byteBuf.asReadOnlyBuffer();

					Action action = callback.dataReceived(roByteBuf, priority & 0xffff);
					switch (action) {
					case Continue:
						return 0;
					case Abort:
						return INotesErrorConstants.ERR_MQSCAN_ABORT;
					case Dequeue:
						return INotesErrorConstants.ERR_MQSCAN_DEQUEUE;
					case Delete:
						return INotesErrorConstants.ERR_MQSCAN_DELETE;
					}

					return 0;
				}
			};
		}

		ShortByReference retMsgLength = new ShortByReference();
		short result = notesAPI.MQScan(m_queue, byteBuffer, (short) (buffer.length & 0xffff), 0, cCallback, null, retMsgLength);
		NotesErrorUtils.checkResult(result);

		return retMsgLength.getValue() & 0xffff;
	}

	/**
	 * This function puts the message queue in a QUIT state, which indicates to
	 * applications that read the message queue that they should terminate.
	 */
	public void putQuitMsg() {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		notesAPI.MQPutQuitMsg(m_queue);
	}

	/**
	 * This function adds a message to the message queue.<br>
	 * The message will be placed in the queue according to the value of its priority argument -
	 * higher priority messages will be enqueued ahead of lower priority messages.<br>
	 * <br>
	 * If the queue is full or in a QUIT state, the message will not be put in the queue, and
	 * the function will return an appropriate error code.
	 * 
	 * @param buffer buffer containing the message.  Maximum buffer length is {@link NotesCAPI#MQ_MAX_MSGSIZE} (65326 bytes)
	 * @param priority priority
	 * @param offset offset in the buffer where the message starts
	 * @param length lengths of the message in the buffer
	 */
	public void put(byte[] buffer, int priority, int offset, int length) {
		checkHandle();

		if (length > NotesCAPI.MQ_MAX_MSGSIZE) {
			throw new IllegalArgumentException("Max size for the buffer is "+NotesCAPI.MQ_MAX_MSGSIZE+" bytes. You specified one with "+length+" bytes.");
		}

		ByteBuffer byteBuf = ByteBuffer.wrap(buffer, offset, length);

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result = notesAPI.MQPut(m_queue, (short) (priority & 0xffff), byteBuf, (short) (length & 0xffff), 0);
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This function adds a message to the message queue.<br>
	 * The message will be placed in the queue according to the value of its priority argument -
	 * higher priority messages will be enqueued ahead of lower priority messages.<br>
	 * <br>
	 * If the queue is full or in a QUIT state, the message will not be put in the queue, and
	 * the function will return an appropriate error code.
	 * 
	 * @param buffer buffer containing the message.  Maximum buffer length is {@link NotesCAPI#MQ_MAX_MSGSIZE} (65326 bytes)
	 * @param priority priority
	 */
	public void put(byte[] buffer, int priority) {
		put(buffer, priority, 0, buffer.length);
	}

	/**
	 * Retrieves a message from the message queue, provided the queue is not in a QUIT state.
	 * The message will be stored in the buffer specified in the Buffer argument.<br>
	 * Note: The error code {@link INotesErrorConstants#ERR_MQ_QUITTING} indicates that the
	 * message queue is in the QUIT state, denoting that applications that are reading
	 * the message queue should terminate. For instance, a server addin's message queue
	 * will be placed in the QUIT state when a "tell <addin> quit" command is input at the console.

	 * @param buffer buffer used to read data
	 * @param waitForMessage if the specified message queue is empty, wait for a message to appear in the queue. The timeout argument specifies the amount of time to wait for a message.
	 * @param timeoutMillis if waitForMessage is set to <code>true</code>, the number of milliseconds to wait for a message before timing out. Specify 0 to wait forever. If the message queue goes into a QUIT state before the Timeout expires, MQGet will return immediately.
	 * @return Number of bytes written to the buffer
	 */
	public int get(byte[] buffer, boolean waitForMessage, int timeoutMillis) {
		return get(buffer, waitForMessage, timeoutMillis, 0, buffer.length);
	}

	/**
	 * Retrieves a message from a message queue, provided the queue is not in a QUIT state.
	 * The message will be stored in the buffer specified in the Buffer argument.<br>
	 * Note: The error code {@link INotesErrorConstants#ERR_MQ_QUITTING} indicates that the
	 * message queue is in the QUIT state, denoting that applications that are reading
	 * the message queue should terminate. For instance, a server addin's message queue
	 * will be placed in the QUIT state when a "tell <addin> quit" command is input at the console.

	 * @param buffer buffer used to read data
	 * @param waitForMessage if the specified message queue is empty, wait for a message to appear in the queue. The timeout argument specifies the amount of time to wait for a message.
	 * @param timeoutMillis if waitForMessage is set to <code>true</code>, the number of milliseconds to wait for a message before timing out. Specify 0 to wait forever. If the message queue goes into a QUIT state before the Timeout expires, MQGet will return immediately.
	 * @param offset the offset in the buffer where to start writing the message
	 * @param length the max length of the message in the buffer
	 * @return Number of bytes written to the buffer
	 */
	public int get(byte[] buffer, boolean waitForMessage, int timeoutMillis, int offset, int length) {
		checkHandle();

		if (length > NotesCAPI.MQ_MAX_MSGSIZE) {
			throw new IllegalArgumentException("Max size for the buffer is "+NotesCAPI.MQ_MAX_MSGSIZE+" bytes. You specified one with "+length+" bytes.");
		}

		ByteBuffer byteBuf = ByteBuffer.wrap(buffer, offset, length);

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		ShortByReference retMsgLength = new ShortByReference();

		short result = notesAPI.MQGet(m_queue, byteBuf, (short) (length & 0xffff),
				waitForMessage ? NotesCAPI.MQ_WAIT_FOR_MSG : 0,
						timeoutMillis, retMsgLength);
		NotesErrorUtils.checkResult(result);
		return retMsgLength.getValue();
	}

	public boolean isQuitPending() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		boolean quitPending = notesAPI.MQIsQuitPending(m_queue);
		return quitPending;
	}

	/**
	 * Callback interface to scan a message queue for new messages
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IMQCallback {
		public enum Action {
			/** Process the next message */
			Continue,
			/** Return from MQScan immediately without dequeueing a message.<br>
			 * Note that MQScan returns ERR_MQSCAN_ABORT */
			Abort,
			/** Remove the message from the queue, terminate the enumeration, and return
			 * the current message to the caller of MQScan.<br>
			 * If the Buffer is smaller than the message, MQScan can return ERR_MQ_BFR_TOO_SMALL. */
			Dequeue,
			/** Remove the current message from the message queue and continue the enumeration */
			Delete}

		/**
		 * Implement this method to read 
		 * 
		 * @param buffer read only byte buffer with message data
		 * @param priority priority
		 * @return what to do next
		 */
		public Action dataReceived(ByteBuffer buffer, int priority);
	}

	@Override
	public void recycle() {
		if (isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result = notesAPI.MQClose(m_queue, 0);
		NotesErrorUtils.checkResult(result);
		NotesGC.__objectBeeingBeRecycled(MessageQueue.class, this);
		m_queue = 0;
	}

	@Override
	public boolean isRecycled() {
		return m_queue!=0;
	}

	@Override
	public int getHandle32() {
		return m_queue;
	}

	@Override
	public long getHandle64() {
		return m_queue;
	}
}
