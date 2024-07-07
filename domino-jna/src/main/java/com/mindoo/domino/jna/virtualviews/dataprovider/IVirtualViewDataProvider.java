package com.mindoo.domino.jna.virtualviews.dataprovider;

import com.mindoo.domino.jna.virtualviews.VirtualView;

/**
 * Interface for a class that provides data to a {@link VirtualView}
 */
public interface IVirtualViewDataProvider {

	/**
	 * Method is called when this data provider is added to a {@link VirtualView}
	 * 
	 * @param view view
	 */
	void init(VirtualView view);
	
	/**
	 * Returns a unique id that identifies the origin of the data
	 * @return id
	 */
	String getOrigin();
	
	/**
	 * Fetches the latest data and sends updates to the {@link VirtualView}
	 */
	void update();

}
