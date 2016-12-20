package com.mindoo.domino.jna.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;

/**
 * Extension of {@link Structure} to make it work in an execution environment
 * that is secured by the Domino SecurityManager.
 * 
 * @author Karsten Lehmann
 */
public abstract class BaseStructure extends Structure {

	public BaseStructure() {
		super();
	}

	public BaseStructure(int alignType, TypeMapper mapper) {
		super(alignType, mapper);
	}

	public BaseStructure(int alignType) {
		super(alignType);
	}

	public BaseStructure(Pointer p, int alignType, TypeMapper mapper) {
		super(p, alignType, mapper);
	}

	public BaseStructure(Pointer p, int alignType) {
		super(p, alignType);
	}

	public BaseStructure(Pointer p) {
		super(p);
	}

	public BaseStructure(TypeMapper mapper) {
		super(mapper);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected List getFieldList() {
		return AccessController.doPrivileged(new PrivilegedAction<List>() {

			@Override
			public List run() {
				return BaseStructure.super.getFieldList();
			}
		});
	}
	
	@Override
	public void read() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				BaseStructure.super.read();
				return null;
			}
		});
	}
	
	@Override
	public void write() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				BaseStructure.super.write();
				return null;
			}
		});
	}
	
}
