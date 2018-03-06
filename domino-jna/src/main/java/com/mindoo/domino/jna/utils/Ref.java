package com.mindoo.domino.jna.utils;

/**
 * A class to hold a reference to another object, for use with inner classes.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class Ref<T> {
	private T m_obj;
	
	public Ref() {
		
	}
	public Ref(T obj) {
		this.m_obj = obj;
	}
	
	public T get() {
		return m_obj;
	}
	public void set(T obj) {
		this.m_obj = obj;
	}
	
	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + ": " + m_obj + "]";
	}
}
