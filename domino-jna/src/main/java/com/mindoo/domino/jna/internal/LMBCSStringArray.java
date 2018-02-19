package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;

/** 
 * Utility class to write a const char*[] of LMBCS encoded strings.
 * Code partially copied and modified from JNA's {@link StringArray}.
 */
public class LMBCSStringArray extends Memory implements Function.PostCallRead {
	private List<Memory> natives = new ArrayList<Memory>();
	private Object[] original;

	public LMBCSStringArray(Object[] strValues) {
		super((strValues.length + 1) * Pointer.SIZE);
		this.original = strValues;

		for (int i=0; i < strValues.length;i++) {
			Pointer p = null;
			if (strValues[i] != null) {
				Memory currStrMem = NotesStringUtils.toLMBCS(strValues[i].toString(), true);
				natives.add(currStrMem);
				p = currStrMem;
			}
			setPointer(Pointer.SIZE * i, p);
		}
		setPointer(Pointer.SIZE * strValues.length, null);
	}

	@Override
	public void read() {
		for (int i=0;i < original.length;i++) {
			Pointer p = getPointer(i * Pointer.SIZE);
			Object s = null;
			if (p != null) {
				s = NotesStringUtils.fromLMBCS(p, -1);
			}
			original[i] = s;
		}
	}

	@Override
	public String toString() {
		String s = "const char*[]";
		s += Arrays.asList(original);
		return s;
	}
}