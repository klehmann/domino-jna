package com.mindoo.domino.jna.xsp.library;

import com.ibm.xsp.library.AbstractXspLibrary;
import com.mindoo.domino.jna.xsp.internal.DominoJNAActivator;

public class DominoJNALibrary extends AbstractXspLibrary {

	@Override
	public String getLibraryId() {
		return "com.mindoo.domino.jna.xsp.library";
	}
	
	@Override
	public String getPluginId() {
		return DominoJNAActivator.PLUGIN_ID;
	}
}
