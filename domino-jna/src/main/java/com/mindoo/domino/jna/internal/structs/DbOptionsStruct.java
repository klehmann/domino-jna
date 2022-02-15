/*
 * ==========================================================================
 * Copyright (C) 2019-2022 HCL America, Inc. ( http://www.hcl.com/ )
 *                            All rights reserved.
 * ==========================================================================
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may
 * not use this file except in compliance with the License.  You may obtain a
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the  specific language  governing permissions  and limitations
 * under the License.
 * ==========================================================================
 */
package com.mindoo.domino.jna.internal.structs;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the DbOptions type
 * 
 * @author Karsten Lehmann
 */
public class DbOptionsStruct extends BaseStructure implements Serializable, IAdaptable {
	private static final long serialVersionUID = -496925509459204819L;
	public int options1;
	public int options2;
	public int options3;
	public int options4;
	
	public static DbOptionsStruct newInstance() {
		return AccessController.doPrivileged((PrivilegedAction<DbOptionsStruct>) () -> new DbOptionsStruct());
	}

	public static DbOptionsStruct.ByValue newInstanceByVal() {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> new DbOptionsStruct.ByValue());
	}
	
	public static DbOptionsStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<DbOptionsStruct>) () -> {
			DbOptionsStruct newObj = new DbOptionsStruct(peer);
			newObj.read();
			return newObj;
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	@Deprecated
	public DbOptionsStruct() {
		super();
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("options1", "options2", "options3", "options4"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	@Deprecated
	public DbOptionsStruct(Pointer peer) {
		super(peer);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == DbOptionsStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
	
	public static class ByReference extends DbOptionsStruct implements Structure.ByReference {
		private static final long serialVersionUID = -2958581285484373942L;
		
	};
	public static class ByValue extends DbOptionsStruct implements Structure.ByValue {
		private static final long serialVersionUID = -6538673668884547829L;
		
	};
	
}
