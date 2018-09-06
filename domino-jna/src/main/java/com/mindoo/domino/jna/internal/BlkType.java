package com.mindoo.domino.jna.internal;

public enum BlkType {
	BLK_LOCAL_BLOCK(0x0168),
	BLK_LOCAL(0x4129),
	BLK_NET_SESSION_CACHEPTR_TABLE(0x0a21),
	BLK_RM_PTHREAD_TRANENTRY(0x0380),
	BLK_PCB(0x0128),
	BLK_NSF_UBM_BUFFEXT(0x02c6),
	BLK_TLA(0x0130),
	BLK_DIRASSIST(0x031b),
	BLK_PHTCHUNK(0x0149),
	BLK_DA_SYMBOL_TABLE(0x0317),
	BLK_CACHED_NLS_INFOS(0x0166),
	BLK_INICACHE(0x0146),
	BLK_DA_RULE_TABLE(0x0318),
	BLK_DA_REPLICA_TABLE(0x0319),
	BLK_COMPILER_STRING_STORE_MEM(0x138e),
	BLK_NSFT(0x0275),
	BLK_CLIENT_OPENSESSION_TIME(0x0821),
	BLK_LOOKUP_THREAD(0x030a),
	BLK_SDKT(0x0176),
	BLK_SDK(0x0132),
	BLK_FILE_EXISTANCE_CACHE(0x01b8),
	BLK_DA_SERVER_TABLE(0x031a),
	UNKNOWN(0);

	private int blkType;

	private BlkType(int blkType) {
		this.blkType = blkType;
	}

	public int getType() {
		return this.blkType;
	}

	/**
	 * Finds a block type for a type constant
	 * 
	 * @param type type
	 * @return block type or null if unknown
	 */
	public static BlkType forId(short type) {
		int typeAsInt = (int) (type & 0xffff);
		
		for (BlkType currType : values()) {
			if (currType.getType() == typeAsInt)
				return currType;
		}
		return null;
	}
}
