package com.mindoo.domino.jna.internal.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class NIFFindByKeyContextStruct extends BaseStructure {
	public short EntriesThisChunk;
	public short wSizeOfChunk;
	/** C type : void* */
	public Pointer SummaryBuffer;
	public int hUserData;
	public int UserDataLen;
	public int TotalDataInBuffer;

	public NIFFindByKeyContextStruct() {
		super();
		setAlignType(Structure.ALIGN_DEFAULT);
	}

	public static NIFFindByKeyContextStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NIFFindByKeyContextStruct>() {

			@Override
			public NIFFindByKeyContextStruct run() {
				return new NIFFindByKeyContextStruct();
			}
		});
	}

	protected List<String> getFieldOrder() {
		return Arrays.asList("EntriesThisChunk", "wSizeOfChunk", "SummaryBuffer", "hUserData", "UserDataLen", "TotalDataInBuffer");
	}

	/**
	 * Creates a new context
	 * 
	 * @param EntriesThisChunk entries in this chunk
	 * @param wSizeOfChunk size of chunk
	 * @param SummaryBuffer summary buffer pointer
	 * @param hUserData handle user data
	 * @param UserDataLen length of user data
	 * @param TotalDataInBuffer total size of buffer
	 */
	public NIFFindByKeyContextStruct(short EntriesThisChunk, short wSizeOfChunk, Pointer SummaryBuffer, int hUserData, int UserDataLen, int TotalDataInBuffer) {
		super();
		this.EntriesThisChunk = EntriesThisChunk;
		this.wSizeOfChunk = wSizeOfChunk;
		this.SummaryBuffer = SummaryBuffer;
		this.hUserData = hUserData;
		this.UserDataLen = UserDataLen;
		this.TotalDataInBuffer = TotalDataInBuffer;
		setAlignType(Structure.ALIGN_DEFAULT);
	}

	public static NIFFindByKeyContextStruct newInstance(final short EntriesThisChunk, final short wSizeOfChunk, final Pointer SummaryBuffer, final int hUserData, final int UserDataLen, final int TotalDataInBuffer) {
		return AccessController.doPrivileged(new PrivilegedAction<NIFFindByKeyContextStruct>() {

			@Override
			public NIFFindByKeyContextStruct run() {
				return new NIFFindByKeyContextStruct(EntriesThisChunk, wSizeOfChunk, SummaryBuffer, hUserData, UserDataLen, TotalDataInBuffer);
			}
		});
	}

	public NIFFindByKeyContextStruct(Pointer peer) {
		super(peer);
		setAlignType(Structure.ALIGN_DEFAULT);
	}

	public static NIFFindByKeyContextStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NIFFindByKeyContextStruct>() {

			@Override
			public NIFFindByKeyContextStruct run() {
				return new NIFFindByKeyContextStruct(peer);
			}
		});	
	}

	public static class ByReference extends NIFFindByKeyContextStruct implements Structure.ByReference {

	};

	public static class ByValue extends NIFFindByKeyContextStruct implements Structure.ByValue {

	};
}
