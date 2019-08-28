package com.mindoo.domino.jna.test;

import org.junit.Test;

import com.mindoo.domino.jna.utils.NotesStatistics;

import lotus.domino.Session;

/**
 * Tests cases for server statistics
 * 
 * @author Karsten Lehmann
 */
public class TestReadStatistics extends BaseJNATestClass {

	@Test
	public void testSignDb() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				{
					System.out.println("Reading all statistics for "+NotesStatistics.STATPKG_REPLICA);
					NotesStatistics stats = NotesStatistics.retrieveStatistics(NotesStatistics.STATPKG_REPLICA);
					System.out.println(stats);
				}
				System.out.println("============================================================================");
				{
					System.out.println("Reading single statistic value Docs.Added for "+NotesStatistics.STATPKG_REPLICA);
					NotesStatistics stats = NotesStatistics.retrieveStatistics(NotesStatistics.STATPKG_REPLICA, "Docs.Added");
					System.out.println(stats);
					
				}
				System.out.println("============================================================================");
				{
					System.out.println("Reading all statistics for all facilities");
					NotesStatistics stats = NotesStatistics.retrieveStatistics();
					System.out.println(stats);
				}

				return null;
			}
		});
	}

}
