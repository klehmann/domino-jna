package com.mindoo.domino.jna.internal.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * This is the structure of a message returned by the Collector in a response to a request made by the application.<br>
 * <br>
 * The return message is put on the application's message queue.<br>
 * <br>
 * The hStatName member of this structure is a formatted buffer containing the requested statistics information.<br>
 * <br>
 * See the Message Queues chapter in the User Guide for details on how to parse this buffer.
 */
public class NotesStatReturnBlock64 extends BaseStructure {
	/** C type : char[MAXSPRINTF] */
	public byte[] StatName = new byte[NotesConstants.MAXSPRINTF];
	/** C type : DHANDLE */
	public long hStatName;
	public int StatNameSize;
	/** C type : char[MAXPATH] */
	public byte[] ServerName = new byte[NotesConstants.MAXPATH];
	/** C type : STATUS */
	public short error;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesStatReturnBlock64() {
		super();
	}
	
	public static NotesStatReturnBlock64 newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesStatReturnBlock64>() {

			@Override
			public NotesStatReturnBlock64 run() {
				return new NotesStatReturnBlock64();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("StatName", "hStatName", "StatNameSize", "ServerName", "error");
	}
	/**
	 * @param StatName C type : char[MAXSPRINTF]<br>
	 * @param hStatName C type : DHANDLE<br>
	 * @param ServerName C type : char[MAXPATH]<br>
	 * @param error C type : STATUS
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesStatReturnBlock64(byte StatName[], long hStatName, int StatNameSize, byte ServerName[], short error) {
		super();
		if ((StatName.length != this.StatName.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.StatName = StatName;
		this.hStatName = hStatName;
		this.StatNameSize = StatNameSize;
		if ((ServerName.length != this.ServerName.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.ServerName = ServerName;
		this.error = error;
	}
	
	public static NotesStatReturnBlock64 newInstance(final byte StatName[], final long hStatName, final int StatNameSize, final byte ServerName[], final short error) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesStatReturnBlock64>() {

			@Override
			public NotesStatReturnBlock64 run() {
				return new NotesStatReturnBlock64(StatName, hStatName, StatNameSize, ServerName, error);
			}
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesStatReturnBlock64(Pointer peer) {
		super(peer);
	}
	
	public static NotesStatReturnBlock64 newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesStatReturnBlock64>() {

			@Override
			public NotesStatReturnBlock64 run() {
				return new NotesStatReturnBlock64(p);
			}
		});
	}

	public static class ByReference extends NotesStatReturnBlock64 implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesStatReturnBlock64 implements Structure.ByValue {
		
	};
}
