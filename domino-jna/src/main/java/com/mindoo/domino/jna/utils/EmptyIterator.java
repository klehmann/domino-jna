package com.mindoo.domino.jna.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple implementation of {@link Iterator} that does not
 * contain any elements.
 * 
 * @author Karsten Lehmann
 *
 * @param <X> iterator data type
 */
public class EmptyIterator<X> implements Iterator<X> {

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public X next() {
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
