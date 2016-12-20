package com.mindoo.domino.jna.xsp.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class DominoJNAActivator extends Plugin {
	public static final String PLUGIN_ID = "com.mindoo.domino.jna.xsp";
	
	private static DominoJNAActivator plugin;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
