package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.utils.PlatformUtils;

public class Handle implements IAdaptable {
	private long m_hdl64;
	private int m_hdl32;
	
	public Handle(long hdl) {
		if (PlatformUtils.is32Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_hdl64 = hdl;
	}
	
	public Handle(int hdl) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_hdl32 = hdl;
	}
	
	public long getHandle64() {
		return m_hdl64;
	}
	
	public int getHandle32() {
		return m_hdl32;
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (Handle.class.equals(clazz)) {
			return (T) this;
		}
		return null;
	}
}
