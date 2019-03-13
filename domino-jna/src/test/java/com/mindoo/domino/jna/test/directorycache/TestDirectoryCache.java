package com.mindoo.domino.jna.test.directorycache;

import org.junit.Test;

import com.mindoo.domino.jna.test.BaseJNATestClass;
import com.mindoo.domino.jna.test.directorycache.DirectoryCache.NameLookupResult;

import lotus.domino.Session;

/**
 * This test case demonstrates incremental NSF searches by hashing the
 * names.nsf database into an in-memory lookup map and doing very fast name
 * lookups.
 * 
 * @author Karsten Lehmann
 */
public class TestDirectoryCache extends BaseJNATestClass {

	@Test
	public void testNameLookupViaCache() {
		runWithSession(new IDominoCallable<Object>() {
			@Override
			public Object call(Session session) throws Exception {
				//the DirectoryCache instance would normally exist permanently in a Java application
				//or OSGi plugin; it remembers the last time we searched the NSF for changes
				//and runs a fast incremental NSF search on each usage of the lookup functions once
				//per NotesGC.runWithAutoGC block to ensure we have the most up-to-date data
				
				DirectoryCache dirCache = new DirectoryCache("", "names.nsf");
				
				//trigger manual hashing of all directory users, would also be done automatically
				//when calling one of the lookup functions
				
				//enforceRefresh = false => only run the NSFSearch once per NotesGC.runWithAutoGC block,
				//e.g. once per HTTP request processing thread; should only be "true" if we change
				//anything in the DB ourselves and would to read our own changes
				boolean enforceRefresh = false;
				dirCache.hashDirectoryUsers(enforceRefresh);

				//some sample lookups using our in-memory cache
				
				//example paging parameters:
				int offset = 0;
				int count = 10;
				
				System.out.println("Max. 10 lookup results for 'a':");
				NameLookupResult usersWithA = dirCache.lookupNames("a", offset, count);
				System.out.println("total: "+usersWithA.getNumRows());
				for (DirectoryUser currUser : usersWithA.getMatches()) {
					System.out.println(DirectoryUser.toJson(currUser));
				}
				System.out.println("===============================");
				
				
				
				System.out.println("Max. 10 lookup results for 'b':");
				NameLookupResult usersWithB = dirCache.lookupNames("b", offset, count);
				System.out.println("total: "+usersWithB.getNumRows());
				for (DirectoryUser currUser : usersWithB.getMatches()) {
					System.out.println(DirectoryUser.toJson(currUser));
				}
				System.out.println("===============================");
				
				
				
				
				System.out.println("Max. 10 lookup results for 'c':");
				NameLookupResult usersWithC = dirCache.lookupNames("c", offset, count);
				System.out.println("total: "+usersWithC.getNumRows());
				for (DirectoryUser currUser : usersWithC.getMatches()) {
					System.out.println(DirectoryUser.toJson(currUser));
				}
				System.out.println("===============================");

				return null;
			}
		});
	}
}
