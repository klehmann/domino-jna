package com.mindoo.domino.jna.xsp.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class DominoJNAActivator extends Plugin {
	public static final String PLUGIN_ID = "com.mindoo.domino.jna.xsp";
	
	private static DominoJNAActivator plugin;
	private static Class m_jnaNativeClazz;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		if (m_jnaNativeClazz==null) {
			m_jnaNativeClazz = AccessController.doPrivileged(new PrivilegedAction<Class>() {

				@Override
				public Class run() {
					//enforce using the extracted JNA .dll/.so file instead of what we find on the PATH
					System.setProperty("jna.nosys", "true");
					//change the library name from the default "jnidispatch" to our own name, so that
					//JNA does not load an jnidispatcher.dll from the Server's program directory
					String oldLibName = System.getProperty("jna.boot.library.name", "jnidispatch");
					System.setProperty("jna.boot.library.name", "dominojnadispatch");
					try {
						//loading the Native class runs its static code that extracts and loads the jna dll
						return DominoJNAActivator.class.forName("com.sun.jna.Native");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					finally {
						System.setProperty("jna.boot.library.name",oldLibName);
					}
					return null;
				}
			});
		}
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	public static DominoJNAActivator getDefault() {
		return plugin;
	}
}
