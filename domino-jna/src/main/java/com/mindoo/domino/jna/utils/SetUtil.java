package com.mindoo.domino.jna.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to work with sets
 * 
 * @author Karsten Lehmann
 */
public class SetUtil {

	/**
	 * Intersects the specified sets and returns a set that contains elements that
	 * exist in all of the sets.
	 * 
	 * @param ids sets
	 * @return intersection of sets
	 */
	public static Set<Integer> and(Collection<Set<Integer>> ids) {
		if (ids==null || ids.size()==0) {
			return Collections.emptySet();
		}
		else if (ids.size()==1) {
			return new HashSet<Integer>(ids.iterator().next());
		}
		else {
			Iterator<Set<Integer>> idSetsIt = ids.iterator();
			
			Set<Integer> idsOfAll = new HashSet<Integer>(idSetsIt.next());
			while (idSetsIt.hasNext()) {
				idsOfAll.retainAll(idSetsIt.next());
			}
			return idsOfAll;
		}
	}
	
	/**
	 * Intersects the specified sets and returns a set that contains elements that
	 * exist in all of the sets.
	 * 
	 * @param ids sets
	 * @return intersection of sets
	 */
	public static Set<Integer> and(Set<Integer>... ids) {
		if (ids==null || ids.length==0) {
			return Collections.emptySet();
		}
		else if (ids.length==1) {
			return new HashSet<Integer>(ids[0]);
		}
		else {
			Set<Integer> idsOfAll = new HashSet<Integer>(ids[0]);
			for (int i=1; i<ids.length; i++) {
				idsOfAll.retainAll(ids[i]);
			}
			return idsOfAll;
		}
	}

	/**
	 * Merges the specified sets and returns a set with elements from all sets
	 * 
	 * @param ids ids to merge
	 * @return merge result
	 */
	public static Set<Integer> or(Collection<Set<Integer>> ids) {
		Set<Integer> allIds = new HashSet<Integer>();
		if (ids!=null) {
			for (Set<Integer> currSet : ids) {
				allIds.addAll(currSet);
			}
		}
		return allIds;
	}
	
	/**
	 * Merges the specified sets and returns a set with elements from all sets
	 * 
	 * @param ids ids to merge
	 * @return merge result
	 */
	public static Set<Integer> or(Set<Integer>... ids) {
		Set<Integer> allIds = new HashSet<Integer>();
		if (ids!=null) {
			for (int i=0; i<ids.length; i++) {
				allIds.addAll(ids[i]);
			}
		}
		return allIds;
	}

	/**
	 * Converts a Collection of Integer values to an int array using the order of the {@link Set#iterator()}
	 * 
	 * @param collection collection
	 * @return int array
	 */
	public static int[] toPrimitiveArray(Collection<Integer> collection) {
		int[] arr = new int[collection.size()];
		int i=0;
		Iterator<Integer> values = collection.iterator();
		while (values.hasNext()) {
			arr[i++] = values.next().intValue();
		}
		return arr;
	}
	
	/**
	 * Converts an int array to a {@link LinkedHashSet} keeping the original order
	 * 
	 * @param arr int array
	 * @return set
	 */
	public static LinkedHashSet<Integer> fromPrimitiveArray(int[] arr) {
		LinkedHashSet<Integer> set = new LinkedHashSet<Integer>();
		for (int i=0; i<arr.length; i++) {
			set.add(Integer.valueOf(arr[i]));
		}
		return set;
	}
}
