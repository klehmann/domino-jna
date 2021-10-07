package com.mindoo.domino.jna.xsp.internal.mime;

import java.net.URL;

/**
 * Classloader that blocks com.sun.mail, javax.mail and javax.activation class lookups so
 * that they are not resolved by the bootstrap classloader (we want to load
 * our own version).
 * 
 * @author Karsten Lehmann
 */
public class PackageBlockingClassLoader extends ClassLoader {

	public PackageBlockingClassLoader(ClassLoader cl) {
		super(cl);
	}
	
	private boolean isLocalPackage(String name) {
		if (name.startsWith("com.sun.mail.") || name.startsWith("javax.mail.") ||
				name.startsWith("javax.activation.") || name.startsWith("com.sun.activation.") ||
				
				name.contains("com/sun/mail") || name.contains("javax/mail") ||
				name.contains("javax/activation") || name.contains("com/sun/activation")) {
			return true;
		}
		else {
			return false;
		}
	}
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (isLocalPackage(name)) {
			throw new ClassNotFoundException(name);
		}
		
		return super.findClass(name);
	}
	
	@Override
	protected URL findResource(String name) {
		if (isLocalPackage(name)) {
			return null;
		}

		return super.findResource(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (isLocalPackage(name)) {
			throw new ClassNotFoundException(name);
		}
		
		return super.loadClass(name, resolve);
	}
}
