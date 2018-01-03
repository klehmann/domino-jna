package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * These values are used as input to the NSGetServerClusterMates function.<br>
 * <br>
 * When you specify the CLUSTER_LOOKUP_NOCACHE value, the call retrieves the input server's
 * cluster member list through a NameLookup on the input server.<br>
 * <br>
 * The client cluster cache is not used for determining this information.<br>
 * <br>
 * When you specify the CLUSTER_LOOKUP_CACHEONLY value, the call is forced to retrieve the server's
 * cluster member list from the local client cluster cache.<br>
 * <br>
 * There is no NameLookup performed on the server in this case.
 * 
 * @author Karsten Lehmann
 */
public enum ClusterLookup {
	/**
	 * Instructs the NSGetServerClusterMates function to not use the cluster name cache and forces
	 * a lookup on the target server instead
	 * */
	LOOKUP_NOCACHE(NotesConstants.CLUSTER_LOOKUP_NOCACHE),
	
	/**
	 * Instructs the NSGetServerClusterMates function to only use the cluster name cache and
	 * restricts lookup to the workstation cache
	 */
	LOOKUP_CACHEONLY(NotesConstants.CLUSTER_LOOKUP_CACHEONLY);
	
	private int m_val;
	
	ClusterLookup(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}

}
