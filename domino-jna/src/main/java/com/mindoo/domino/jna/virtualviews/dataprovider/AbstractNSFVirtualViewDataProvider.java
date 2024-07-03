package com.mindoo.domino.jna.virtualviews.dataprovider;

import com.mindoo.domino.jna.NotesDatabase;

/**
 * Base class for data providers that fetch data from a Notes database
 */
public abstract class AbstractNSFVirtualViewDataProvider implements IVirtualViewDataProvider {
	
	public abstract NotesDatabase getDatabase();

}
