package com.mindoo.domino.jna.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollectionSummary;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.Encryption;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.utils.IDUtils;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Tests cases for database creation
 * 
 * @author Karsten Lehmann
 */
public class TestCreateDatabase extends BaseJNATestClass {
	
	@Test
	public void testCreateNewDbFromTemplate() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				SimpleDateFormat dtFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
				String timestamp = dtFormat.format(new Date());
				
				String server = "";
				String filePath = "test/tmpdb_"+timestamp+".nsf";
				
				NotesDatabase templateDb = new NotesDatabase("", "pernames.ntf", "");
				String templateName = templateDb.getTemplateName();
				String templateDbServer = templateDb.getServer();
				String tempDbReplicaId = templateDb.getReplicaID();
				
				//use absolute path because pernames.ntf is stored in a shared directory location
				//and NSFDbCreateAndCopyExtended cannot find it using relative paths
				String templateDbFilePath = templateDb.getAbsoluteFilePathOnLocal();
				
				NotesDatabase newDb = NotesDatabase.createDatabaseFromTemplate(templateDbServer, templateDbFilePath,
						server, filePath);
				System.out.println("Created database: "+newDb);
				try {
					List<NotesCollectionSummary> collections = newDb.getAllCollections();
					Assert.assertTrue("DB created from template has any views", !collections.isEmpty());
					
					String designTemplateName = newDb.getDesignTemplateName();
					Assert.assertEquals("Design template name is correct", templateName, designTemplateName);
					
					String newDbReplicaId = newDb.getReplicaID();
					Assert.assertTrue("New database has its own replica id", !tempDbReplicaId.equals(newDbReplicaId));
				}
				finally {
					newDb.recycle();
					NotesDatabase.deleteDatabase(server, filePath);
				}
				
				return null;
			}
		});
	}
	
	@Test
	public void testCreateNewBlankDb() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				SimpleDateFormat dtFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
				String timestamp = dtFormat.format(new Date());
				
				String server = "";
				String filePath = "test/tmpdb_"+timestamp+".nsf";
				
				NotesDatabase.createDatabase(server, filePath, Encryption.None, "Temp db "+timestamp,
						AclLevel.DESIGNER, IDUtils.getIdUsername(), false);
				
				NotesDatabase db = new NotesDatabase(server, filePath, "");
				System.out.println("Created database: "+db);
				try {
					List<NotesCollectionSummary> collections = db.getAllCollections();
					Assert.assertTrue("DB created from template has no views", collections.isEmpty());
				}
				finally {
					db.recycle();
					NotesDatabase.deleteDatabase(server, filePath);
				}
				
				return null;
			}
		});
	}
	
	@Test
	public void testCreateNewBlankDbWithInitialDesign() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				SimpleDateFormat dtFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
				String timestamp = dtFormat.format(new Date());
				
				String server = "";
				String filePath = "test/tmpdb_"+timestamp+".nsf";
				
				NotesDatabase.createDatabase(server, filePath, Encryption.None, "Temp db "+timestamp,
						AclLevel.DESIGNER, IDUtils.getIdUsername(), true);
				
				NotesDatabase db = new NotesDatabase(server, filePath, "");
				System.out.println("Created database: "+db);
				try {
					List<NotesCollectionSummary> collections = db.getAllCollections();
					Assert.assertTrue("DB created from template has any views", !collections.isEmpty());
				}
				finally {
					db.recycle();
					NotesDatabase.deleteDatabase(server, filePath);
				}
				
				return null;
			}
		});
	}
	
	@Test
	public void testCreateDbReplica() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				SimpleDateFormat dtFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
				String timestamp = dtFormat.format(new Date());

				String server = "";
				String filePath = "test/tmpdb_"+timestamp+".nsf";

				NotesDatabase namesDb = new NotesDatabase("", "names.nsf", "");
				String namesDbReplicaId = namesDb.getReplicaID();
				Set<String> namesDbViews = namesDb
						.getAllCollections()
						.stream()
						.map((colInfo) -> {
							return colInfo.getTitle();
						})
						.collect(Collectors.toSet());
				
				NotesDatabase newDbReplica = NotesDatabase.createDbReplica(namesDb.getServer(), namesDb.getRelativeFilePath(),
						server, filePath);
				System.out.println("Created database: "+newDbReplica);
				try {
					Set<String> replicaDbViews = newDbReplica
							.getAllCollections()
							.stream()
							.map((colInfo) -> {
								return colInfo.getTitle();
							})
							.collect(Collectors.toSet());
					Assert.assertEquals("Replica DB has same views", namesDbViews, replicaDbViews);
					
					String newDbReplicaId = newDbReplica.getReplicaID();
					Assert.assertTrue("New database has its own replica id", namesDbReplicaId.equals(newDbReplicaId));
				}
				finally {
					newDbReplica.recycle();
					NotesDatabase.deleteDatabase(server, filePath);
				}
				
				return null;
			}
		});
	}
	
}
