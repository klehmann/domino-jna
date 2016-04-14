package com.mindoo.domino.jna.test;

import java.util.Arrays;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.structs.NotesDbReplicaInfo;
import com.mindoo.domino.jna.utils.NotesStringUtils;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Session;

/**
 * Tests cases for string utilities
 * 
 * @author Karsten Lehmann
 */
public class TestStringUtils extends BaseJNATestClass {

	/**
	 * Tests reading the replication info from a database and conversion functions between
	 * innards arrays and hex strings
	 */
	@Test
	public void testStringUtils_replicaIdInnards() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting replica info get and conversion test");
				
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				NotesDbReplicaInfo replInfoJNA = dbData.getReplicaInfo();
				String replicaIdJNA = replInfoJNA.getReplicaID();
				System.out.println("Replica id of fakenames db: "+replicaIdJNA);
				String replicaIdLegacy = dbLegacyAPI.getReplicaID();
				
				Assert.assertEquals("Replica id matches between JNA and legacy API", replicaIdJNA, replicaIdLegacy);
				
				int[] innards = NotesStringUtils.replicaIdToInnards(replicaIdJNA);
				String convertedReplicaId = NotesStringUtils.innardsToReplicaId(innards);
				
				Assert.assertEquals("Conversion functions between replica id and innards are ok", replicaIdJNA, convertedReplicaId);
				
				Assert.assertTrue("Innards are equal after conversion", Arrays.equals(replInfoJNA.ID.Innards, innards));
				
				System.out.println("Done with replica info get and conversion test");
				return null;
			}
		});
	}

}
