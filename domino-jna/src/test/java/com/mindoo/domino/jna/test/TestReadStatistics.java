package com.mindoo.domino.jna.test;

import org.junit.Test;

import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.NotesStatistics;

import lotus.domino.Session;

/**
 * Tests cases for server statistics
 * 
 * @author Karsten Lehmann
 */
public class TestReadStatistics extends BaseJNATestClass {

	@Test
	public void testStats() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				
				{
					System.out.println("Reading all statistics for "+NotesStatistics.STATPKG_REPLICA);
					NotesStatistics stats = NotesStatistics.retrieveLocalStatistics(NotesStatistics.STATPKG_REPLICA);
					System.out.println("Formatted as string:\n");
					System.out.println(stats);
				}
				System.out.println("============================================================================");
				{
					System.out.println("Reading single statistic value Docs.Added for "+NotesStatistics.STATPKG_REPLICA);
					NotesStatistics stats = NotesStatistics.retrieveLocalStatistics(NotesStatistics.STATPKG_REPLICA, "Docs.Added");
					Object singleVal = stats.getFirstStatForFacility(NotesStatistics.STATPKG_REPLICA, "Docs.Added");
					System.out.println("Single value read:\n"+singleVal);
					System.out.println("Formatted as string:\n");
					System.out.println(stats);
					
				}
				System.out.println("============================================================================");
				{
					System.out.println("Reading all statistics for all facilities");
					NotesStatistics stats = NotesStatistics.retrieveLocalStatistics();
					System.out.println("Formatted as string:\n");
					System.out.println(stats);
				}

				String server = "MyServer/MyOrg";;
				String stats = NotesStatistics.retrieveRemoteStatisticsAsString(server, NotesStatistics.STATPKG_REPLICA);
				System.out.println("Remote statistics for server "+server+":\n"+stats);
				return null;
			}
		});
	}

}
