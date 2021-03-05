package com.mindoo.domino.jna.internal.handles;

public interface IHANDLEBase<LOCKTYPE,LOCKBYVALTYPE> {
	
	void checkDisposed();

	boolean isNull();

}
