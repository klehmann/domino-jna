package com.mindoo.domino.jna.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.devskiller.jfairy.Fairy;
import com.devskiller.jfairy.producer.person.Person;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.Encryption;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.constants.CreateDatabase;
import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.constants.DBCompact;
import com.mindoo.domino.jna.constants.DBCompact2;
import com.mindoo.domino.jna.constants.DatabaseOption;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.dxl.DXLImporter;
import com.mindoo.domino.jna.dxl.DXLImporter.DXLImportOption;
import com.mindoo.domino.jna.dxl.DXLImporter.XMLValidationOption;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.NotesInitUtils;
import com.mindoo.domino.jna.utils.Pair;
import com.mindoo.domino.jna.utils.StringUtil;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;

public class BaseJNATestClass {
	public static final String DBPATH_SAMPLEDATA_VIEWS_NSF = "dominojna-test-views.nsf";
	public static final String DBPATH_SAMPLEDATA_NSF = "dominojna-test-data.nsf";

	public static final String DBPATH_FAKENAMES_VIEWS_NSF = "fakenames-views.nsf";
	public static final String DBPATH_FAKENAMES_NSF = "fakenames.nsf";

	public enum LargeDataLevel {
		/** R10/R11 ODS supporting up to 16MB of summary data */
		R11,
		/** R12 ODS supporting large text items and search results */
		R12
	}

	private ThreadLocal<Session> m_threadSession = new ThreadLocal<Session>();
	private static boolean m_notesInitExtendedCalled = false;
	private static boolean m_notesThreadInitCalled = false;

	@BeforeClass
	public static void initNotes() {
		String notesProgramDir = System.getenv("Notes_ExecDirectory");
		String notesIniPath = System.getenv("NotesINI");

		NotesThread.sinitThread();
		m_notesThreadInitCalled = true;
		
		if (notesProgramDir!=null && notesProgramDir.length()>0 && notesIniPath!=null && notesIniPath.length()>0) {
			NotesInitUtils.notesInitExtended(new String[] {
					notesProgramDir,
					"="+notesIniPath
			});
			m_notesInitExtendedCalled = true;
		}
	}

	@AfterClass
	public static void termNotes() {
		if (m_notesInitExtendedCalled) {
			NotesInitUtils.notesTerm();
		}
		
		if (m_notesThreadInitCalled) {
			NotesThread.stermThread();
		}
	}

	public synchronized NotesDatabase getSampleDataDb() throws NotesException, IOException {
		Session session = getSession();
		NotesDatabase db;
		try {
			db = new NotesDatabase(session, "", DBPATH_SAMPLEDATA_NSF);

			//check if the db contains data
			NotesCollection peopleView = db.openCollectionByName("People");
			List<NotesViewEntryData> entries = peopleView.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), 1, EnumSet.of(ReadMask.NOTEID), new NotesCollection.EntriesAsListCallback(1));
			if (entries.isEmpty()) {
				Database dbSampleDataLegacy = session.getDatabase("", DBPATH_SAMPLEDATA_NSF);
				addLookupViews(session, dbSampleDataLegacy);
				addFakeNameDocuments(session, dbSampleDataLegacy);
				dbSampleDataLegacy.recycle();
				peopleView.update();
				peopleView.recycle();
			}

			return db;
		}
		catch (NotesError e) {
			if (e.getId() != 259) { // file does not exist
				throw e;
			}
		}
		System.out.println("Looks like our sample database "+DBPATH_SAMPLEDATA_NSF+" with demo data does not exist. We will now (re)create it.");

		String location = session.getEnvironmentString("Location", true);
		String[] locationParts = location.split(",");
		String locationNoteIdHex = locationParts[1];

		Database dbNames = session.getDatabase("", "names.nsf");
		Document docLocation = dbNames.getDocumentByID(locationNoteIdHex);
		String homeServer = docLocation.getItemValueString("MailServer");

		System.out.println("Creating new demo database from template pubnames.ntf on home server "+homeServer+"...");

		Database dbTemplate = session.getDatabase(homeServer, "pubnames.ntf");
		Database dbSampleDataLegacy = dbTemplate.createFromTemplate("", DBPATH_SAMPLEDATA_NSF, false);

		createSampleDbDirProfile(dbSampleDataLegacy);
		
		System.out.println("Adding our own lookup views required by testcases...");
		addLookupViews(session, dbSampleDataLegacy);
		addFakeNameDocuments(session, dbSampleDataLegacy);
		dbSampleDataLegacy.recycle();
		System.out.println("Done initializing demo database!");

		db = new NotesDatabase(session, "", DBPATH_SAMPLEDATA_NSF);
		db.setTemplateName("DominoJNA Sampledata (Dev)");
		db.setTitle("Domino JNA testcase data");

		return db;
	}

	private void createSampleDbDirProfile(Database db) throws NotesException {
		Document docProfile = db.getProfileDocument("DirectoryProfile", null);
		docProfile.replaceItemValue("Form", "DirectoryProfile");
		docProfile.replaceItemValue("Domain", db.getParent().evaluate("@Domain", docProfile));
		docProfile.replaceItemValue("GroupSortDefault", "1");
		docProfile.replaceItemValue("AutoPopulateMembersInterval", "30");
		docProfile.replaceItemValue("SecureInetPasswords", "1");
		docProfile.replaceItemValue("AltLanguageInfoAllowed", "1");
		docProfile.computeWithForm(false, false);
		docProfile.save(true, false);
		docProfile.recycle();
	}
	
	private void addLookupViews(Session session, Database dbFakeNamesLegacy) {
		//use legacy APIs to create views, since we don't have them yet in Domino JNA

	}

	private File findSampleDataFile() {
		File sampleDataDir = findSampleDataDir();
		if (sampleDataDir!=null) {
			File[] zipFile = sampleDataDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File f) {
					return f.getName().equalsIgnoreCase("fakenames.zip");
				}
			});
			if (zipFile!=null && zipFile.length>0)
				return zipFile[0];
		}
		return null;
	}
	
	private File findSampleDataDir() {
		File currDir = new File(".").getAbsoluteFile();
		while (currDir!=null) {
			File[] sampleDataDir = currDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File f) {
					return f.getName().equalsIgnoreCase("sampledata");
				}
			});
			if (sampleDataDir!=null && sampleDataDir.length>0)
				return sampleDataDir[0];
			
			currDir = currDir.getParentFile();
		}
		return null;
	}
	
	private void addFakeNameDocuments(Session session, Database dbFakeNamesLegacy) throws FileNotFoundException {
		File sampleDataZipFile = findSampleDataFile();
		if (sampleDataZipFile==null)
			throw new FileNotFoundException("Could not find a directory 'sampledata' containing 'fakenames.zip' in one of the project's parent folders");
		
		final int TOTAL = 50000;
		int cnt = 0;
		
		System.out.println("Adding person documents with fakenames...");
		
		Map<Date,String> unidByCreationDate = new TreeMap<Date,String>();
		
		InputStream in = new FileInputStream(sampleDataZipFile);
		if (in!=null) {
			List<Scanner> scanners = new ArrayList<Scanner>();
			try {
				ZipInputStream zipIn = new ZipInputStream(in);
				ZipEntry zipentry = zipIn.getNextEntry();
				while (zipentry != null) {
					if (!zipentry.isDirectory()) {
						Scanner sc = new Scanner(zipIn, "UTF-8");
						scanners.add(sc);
						if (sc.hasNextLine()) {
							String headerLine = sc.nextLine();
							String[] items = headerLine.split("\t", -1);
							Map<String,Integer> itemPositions = new HashMap<String,Integer>();
							for (int i=0; i<items.length; i++) {
								itemPositions.put(items[i], i);
							}
							
							while (sc.hasNextLine()) {
								String personLine = sc.nextLine();
								String[] personLineItems = personLine.split("\t", -1);
								try {
									addPersonDoc(session, dbFakeNamesLegacy, itemPositions, personLineItems,
											unidByCreationDate);
									cnt++;
									if ((cnt % 1000)==0) {
										System.out.println(cnt+" / "+TOTAL);
									}
									
								} catch (NotesException e) {
									throw new NotesError(0, "Could not create person document for this data: "+personLine, e);
								}
							}
						}
					}

					zipentry = zipIn.getNextEntry();
				}

				zipIn.closeEntry();
			} catch (IOException e) {
				throw new NotesError(0, "Could not populate sample db with fakenames", e);
			}
			finally {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				for (Scanner currScanner : scanners) {
					currScanner.close();
				}
			}
			
			System.out.println("Assigning employee ids...");
			cnt = 0;
			
			//assign employee id in ascending (random) creation date
			for (Entry<Date,String> currEntry : unidByCreationDate.entrySet()) {
				try {
					Document docPerson = dbFakeNamesLegacy.getDocumentByUNID(currEntry.getValue());
					int currEmployeeId = employeeId++;

					docPerson.replaceItemValue("EmployeeID", Integer.toString(currEmployeeId));
					docPerson.replaceItemValue("EmployeeIDAsNo", Integer.valueOf(currEmployeeId));
					docPerson.save(true, false);
					docPerson.recycle();
					cnt++;
					if ((cnt % 1000)==0) {
						System.out.println(cnt+" / "+TOTAL);
					}
				}
				catch (NotesException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("M/d/yyyy");

	private static int employeeId = 1;
	
	/**
	 * Adds a person document to the sample db
	 * 
	 * @param session session
	 * @param dbSampleDataLegacy sample db
	 * @param itemPositions hashmap with item name / index in items array
	 * @param items items array
	 * @param retUnidByCreationDate map to return the creation date, used to assign employee ids
	 * @throws NotesException
	 */
	private void addPersonDoc(Session session, Database dbSampleDataLegacy, Map<String,Integer> itemPositions,
			String[] items, Map<Date,String> retUnidByCreationDate) throws NotesException {
		
		Document doc = dbSampleDataLegacy.createDocument();
		doc.replaceItemValue("Form", "Person");
		doc.replaceItemValue("Type", "Person");
		
		String gender = items[itemPositions.get("Gender")]; // female/male

		String title = items[itemPositions.get("Title")];
		doc.replaceItemValue("Title", title);
		
		String firstName = items[itemPositions.get("GivenName")];
		doc.replaceItemValue("FirstName", firstName);
		
		String middleName = items[itemPositions.get("MiddleInitial")];
		doc.replaceItemValue("MiddleInitial", middleName);
		
		String lastName = items[itemPositions.get("Surname")];
		doc.replaceItemValue("LastName", lastName);
		
		String street = items[itemPositions.get("StreetAddress")];
		doc.replaceItemValue("OfficeStreetAddress", street);
		
		String city = items[itemPositions.get("City")];
		doc.replaceItemValue("OfficeCity", city);
		
		String state = items[itemPositions.get("State")];
		String stateFull = items[itemPositions.get("StateFull")];
		doc.replaceItemValue("OfficeState", stateFull);
		
		String zip = items[itemPositions.get("ZipCode")];
		doc.replaceItemValue("OfficeZIP", zip);
		
		String country = items[itemPositions.get("Country")];
		String countryFull = items[itemPositions.get("CountryFull")];
		doc.replaceItemValue("OfficeCountry", countryFull);
		
		String email = items[itemPositions.get("EmailAddress")];
		doc.replaceItemValue("InternetAddress", email);
		
		String username = items[itemPositions.get("Username")];
		doc.replaceItemValue("ShortName", username);
		
		String telephone = items[itemPositions.get("TelephoneNumber")];
		doc.replaceItemValue("OfficePhoneNumber", telephone);
		
		//birtday as date only item
		String birthdayStr = items[itemPositions.get("Birthday")]; // 12/12/1951
		if (birthdayStr!=null && birthdayStr.length()>0) {
			try {
				Date birthday = DATEFORMAT.parse(birthdayStr);
				DateTime ndtBirthday = session.createDateTime(birthday);
				ndtBirthday.setAnyTime();
				doc.replaceItemValue("Birthday", ndtBirthday);
				ndtBirthday.recycle();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		String job = items[itemPositions.get("Occupation")];
		doc.replaceItemValue("JobTitle", job);
		
		String company = items[itemPositions.get("Company")];
		doc.replaceItemValue("CompanyName", company);
		
		String fullName = "CN=" + firstName + " " + lastName + "/O="+company;
		Item itmFullName = doc.replaceItemValue("Fullname", fullName);
		itmFullName.setNames(true);
		
		//random date/time value for HTTPPasswordChangeDate, max 1 year ago (365 days * 24 hours * 60 minutes * 60 seconds * 1000 ms)
		long millisecondsAgo = (long) (Math.random() * ((double) 365*24*60*60*1000));
		Date dtPwdChange = new Date(System.currentTimeMillis() - millisecondsAgo);
		DateTime ndtPwdChange = session.createDateTime(dtPwdChange);
		doc.replaceItemValue("HTTPPasswordChangeDate", ndtPwdChange);
		
		//random creation date for the document, max 2 years earlier than HTTP password change
		long createdMillisecondsAgo = (long) (Math.random() * ((double) 2*365*24*60*60*1000));
		Date dtCreated = new Date(dtPwdChange.getTime() - createdMillisecondsAgo);
		//make sure this is unique, because we use it as hash key to assign the employee id
		while (retUnidByCreationDate.containsKey(dtCreated)) {
			dtCreated = new Date(dtCreated.getTime()+1);
		}
		DateTime ndtCreated = session.createDateTime(dtCreated);
		doc.replaceItemValue("$CREATED", ndtCreated);
		
		//add number fields
		String heightInCmStr = items[itemPositions.get("Centimeters")];
		int heightInCm = (int) Double.parseDouble(heightInCmStr);
		doc.replaceItemValue("Height", heightInCm);
		
		String weightKiloStr = items[itemPositions.get("Kilograms")];
		double weightKilo = Double.parseDouble(weightKiloStr);
		doc.replaceItemValue("Weight", weightKilo);
		
		//some geo coordinates as number list
		String latitudeStr = items[itemPositions.get("Latitude")];
		double latitude = Double.parseDouble(latitudeStr);
		
		String longitudeStr = items[itemPositions.get("Longitude")];
		double longitude = Double.parseDouble(longitudeStr);
		Vector<Double> latLong = new Vector<Double>();
		latLong.add(latitude);
		latLong.add(longitude);
		doc.replaceItemValue("LatLong", latLong);
		
		doc.save();
		
		retUnidByCreationDate.put(dtCreated, doc.getUniversalID());
		
		doc.recycle();
	}
	
	public NotesDatabase getFakeNamesDb() throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", DBPATH_FAKENAMES_NSF);
		return db;
	}

	public NotesDatabase getFakeNamesViewsDb() throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", DBPATH_FAKENAMES_VIEWS_NSF);
		return db;
	}
	
	public synchronized NotesDatabase getSampleDataViewsDb() throws NotesException {
		Session session = getSession();
		NotesDatabase db;
		try {
			db = new NotesDatabase(session, "", DBPATH_SAMPLEDATA_VIEWS_NSF);
			return db;
		}
		catch (NotesError e) {
			if (e.getId() != 259) { // file does not exist
				throw e;
			}
		}
		System.out.println("Looks like our sample database "+DBPATH_SAMPLEDATA_VIEWS_NSF+" to test private views does not exist. We will now (re)create it.");

		Database dbSampleDataLegacy = session.getDatabase("", DBPATH_SAMPLEDATA_NSF, true);
		Database dbSampleDataViewsLegacy = dbSampleDataLegacy.createFromTemplate("", DBPATH_SAMPLEDATA_VIEWS_NSF, true);
		
		createSampleDbDirProfile(dbSampleDataViewsLegacy);

		dbSampleDataLegacy.recycle();
		dbSampleDataViewsLegacy.recycle();
		
		db = new NotesDatabase(session, "", DBPATH_SAMPLEDATA_VIEWS_NSF);
		db.setTitle("Domino JNA testcase views");
		return db;
	}

	public Database getFakeNamesDbLegacy() throws NotesException {
		Session session = getSession();
		Database db = session.getDatabase("", DBPATH_FAKENAMES_NSF, false);
		return db;
	}

	public Database getFakeNamesViewsDbLegacy() throws NotesException {
		Session session = getSession();
		Database db = session.getDatabase("", DBPATH_FAKENAMES_VIEWS_NSF, false);
		return db;
	}

	public Session getSession() {
		return m_threadSession.get();
	}

	public <T> T runWithSession(final IDominoCallable<T> callable) {
		final Session[] session = new Session[1];
		try {
			session[0] = NotesFactory.createSession();
			session[0].setTrackMillisecInJavaDates(true);
			m_threadSession.set(session[0]);

			T result = NotesGC.runWithAutoGC(new Callable<T>() {

				@Override
				public T call() throws Exception {
					NotesGC.setDebugLoggingEnabled(true);

					//prevent ID password prompt
					String idFilePath = System.getenv("Notes_IDPath");
					
					String idPassword = System.getenv("Notes_IDPassword");
					if (!StringUtil.isEmpty(idPassword)) {
						IDUtils.switchToId(idFilePath, idPassword, true);
					}

					T result = callable.call(session[0]);
					return result;
				}
			});
			return result;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		finally {
			if (session[0]!=null) {
				try {
					session[0].recycle();
				} catch (NotesException e) {
					e.printStackTrace();
				}
			}
			m_threadSession.set(null);
		}
	}

	@FunctionalInterface
	public interface DatabaseConsumer {
		public void accept(NotesDatabase db) throws Exception;
	}
	
	public NotesDatabase createTempDb() throws Exception {
		File tmpFile = File.createTempFile("jnatmp_", ".nsf");
		String tmpFilePath = tmpFile.getAbsolutePath();
		tmpFile.delete();
		
		NotesDatabase.createDatabase("", tmpFilePath, DBClass.V10NOTEFILE, true,
				EnumSet.of(CreateDatabase.LARGE_UNKTABLE), Encryption.None, 0, "Temp Db", 
				AclLevel.MANAGER, IDUtils.getIdUsername(), true);
		
		NotesDatabase db = new NotesDatabase("", tmpFilePath, "");
		return db;
	}
	
	public void withTempDb(DatabaseConsumer consumer) throws Exception {
		NotesDatabase db = createTempDb();
		String tmpFilePath = db.getAbsoluteFilePathOnLocal();
		NotesError deleteDbError = null;
		try {
			consumer.accept(db);
		}
		finally {
			db.recycle();
			
			try {
				NotesDatabase.deleteDatabase("", tmpFilePath);
			}
			catch (NotesError e) {
				deleteDbError = e;
			}
		}
		
		if (deleteDbError!=null) {
			throw deleteDbError;
		}
	}

	/**
	 * Creates a R10 compatible temp db where the R11 C API can write
	 * up to 16 MB of summary data per document or a R12 compatible
	 * temp db with large item support
	 * 
	 * @param level type of database to return
	 * @param c     consumer
	 * @throws Exception
	 */
	protected void withLargeDataEnabledTempDb(final LargeDataLevel level, final DatabaseConsumer c) throws Exception {
		final Path tempDest = Files.createTempFile(this.getClass().getName(), ".nsf"); //$NON-NLS-1$
		Files.delete(tempDest);

		final DBClass dbClass = level == LargeDataLevel.R11 ? DBClass.V10NOTEFILE : DBClass.V12NOTEFILE;

		NotesDatabase.createDatabase("", tempDest.toString(), dbClass, true,
				EnumSet.of(CreateDatabase.LARGE_UNKTABLE), Encryption.None, 0, "Temp Db", 
				AclLevel.MANAGER, IDUtils.getIdUsername(), true);

		NotesDatabase db = new NotesDatabase("", tempDest.toString(), "");
		// activate large summary support; on R12 databases, this auto-enables large
		// item support as well
		db.setOption(DatabaseOption.LARGE_BUCKETS_ENABLED, true);
		db.recycle();

		// compact database to activate the large bucket / item support
		NotesDatabase.compact(tempDest.toString(), EnumSet.of(DBCompact.NO_INPLACE), EnumSet.noneOf(DBCompact2.class));
		db = new NotesDatabase("", tempDest.toString(), "");

		NotesError deleteDbError = null;
		try {
			c.accept(db);
		} finally {
			db.recycle();

			try {
				NotesDatabase.deleteDatabase("", tempDest.toString());
			}
			catch (NotesError e) {
				deleteDbError = e;
			}
		}

		if (deleteDbError!=null) {
			throw deleteDbError;
		}
	}


	public void withImportedDXL(NotesDatabase db, String resDirPath, DatabaseConsumer consumer) throws Exception {
		DXLImporter dxlImporter = new DXLImporter();
		try {
			dxlImporter.setInputValidationOption(XMLValidationOption.NEVER);
			dxlImporter.setDesignImportOption(DXLImportOption.REPLACE_ELSE_CREATE);
			dxlImporter.setReplicaRequiredForReplaceOrUpdate(false);
			getResourceFiles(resDirPath)
			.stream()
			.filter(Objects::nonNull)
			.map(name -> concat("/", name, '/'))
			.map((name) -> {
				if (StringUtil.endsWithIgnoreCase(name, ".xml")) {
					return (InputStream)getClass().getResourceAsStream(name);
				}
				else if (StringUtil.endsWithIgnoreCase(name, ".xml.gz")) {
					try {
						return new GZIPInputStream(getClass().getResourceAsStream(name));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return null;
			})
			.filter(Objects::nonNull)
			.forEach(is -> {
				try {
					dxlImporter.importDxl(is, db);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					if (is!=null) {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});

		}
		finally {
			dxlImporter.free();
		}
		
		consumer.accept(db);
	}
	
	protected static List<String> getResourceFiles(final String path) throws IOException {
		List<String> result = new ArrayList<>();
		final File jarFile = new File(BaseJNATestClass.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		if (jarFile.isFile()) { // Run with JAR file
			final JarFile jar = new JarFile(jarFile);
			final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			while (entries.hasMoreElements()) {
				final String name = entries.nextElement().getName();
				if (name.startsWith(path + "/")) { //filter according to the path
					result.add(name);
				}
			}
			jar.close();
		} else if(jarFile.isDirectory()) {
			File subDir = new File(jarFile, path);
			if(subDir != null && subDir.exists()) {
				for(String name : subDir.list()) {
					result.add(path + "/" + name);
				}
			}
		} else { // Run with IDE
			final URL url = BaseJNATestClass.class.getResource("/" + path);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					for (File app : apps.listFiles()) {
						result.add(app.getPath());
					}
				} catch (URISyntaxException ex) {
					// never happens
				}
			}
		}
		return result;
	}
	
	  /**
     * Add to path ensuring there is just one separator in between the 2 parts..
     * @ibm-api
     */
    public static String concat(String path1, String path2, char sep) {
    	if(StringUtil.isEmpty(path1)) {
    		return path2;
    	}
    	if(StringUtil.isEmpty(path2)) {
    		return path1;
    	}
    	StringBuilder b = new StringBuilder();
    	if(path1.charAt(path1.length()-1)==sep) {
    		b.append(path1,0,path1.length()-1);
    	} else {
    		b.append(path1);
    	}
    	b.append(sep);
    	if(path2.charAt(0)==sep) {
    		b.append(path2,1,path2.length());
    	} else {
    		b.append(path2);
    	}
    	return b.toString();
    }
    
	public static interface IDominoCallable<T> {

		public T call(Session session) throws Exception;

	}
	
	protected byte[] produceTestData(int size) {
		byte[] data = new byte[size];

		int offset = 0;

		while (offset < size) {
			for (char c='A'; c<='Z' && offset<size; c++) {
				data[offset++] = (byte) (c & 0xff);
			}
		}

		return data;
	}

	protected void produceTestData(int size, OutputStream out) throws IOException {
		int offset = 0;

		while (offset < size) {
			for (char c='A'; c<='Z' && offset<size; c++) {
				out.write((byte) (c & 0xff));
				offset++;
			}
		}
	}

	protected void produceTestData(int size, Writer writer) throws IOException {
		int offset = 0;

		while (offset < size) {
			for (char c='A'; c<='Z' && offset<size; c++) {
				writer.write(c);
				offset++;
			}
		}
	}

	protected static List<Pair<String, Integer>> generateNABPersons(final NotesDatabase db, final int nrOfDocs) {
		final List<Pair<String, Integer>> unidsAndNoteIds = new ArrayList<>();

		final Fairy fairy = Fairy
				.builder()
				.withRandomSeed(5) // return deterministic data
				.withLocale(Locale.ENGLISH)
				.build();

		for (int i = 0; i < nrOfDocs; i++) {
			final Person person = fairy.person();

			final NotesNote doc = db.createNote();
			doc.replaceItemValue("Form", "Person");
			doc.replaceItemValue("Type", "Person");
			doc.replaceItemValue("Firstname", person.getFirstName());
			doc.replaceItemValue("Lastname", person.getLastName());
			doc.replaceItemValue("InternetAddress", person.getCompanyEmail());
			doc.replaceItemValue("StreetAddress", person.getAddress().getStreet() + " " + person.getAddress().getStreetNumber());
			doc.replaceItemValue("City", person.getAddress().getCity());
			doc.replaceItemValue("Zip", person.getAddress().getPostalCode());
			doc.replaceItemValue("CompanyName", person.getCompany().getName());
			doc.replaceItemValue("MailAddress", person.getEmail());
			doc.replaceItemValue("OfficePhoneNumber", person.getTelephoneNumber());
			doc.replaceItemValue("Birthday", person.getDateOfBirth());
			doc.replaceItemValue("WebSite", person.getCompany().getUrl());
			doc.replaceItemValue("Country", person.getNationality().getCode());

			doc.update();
			unidsAndNoteIds.add(new Pair<>(doc.getUNID(), doc.getNoteId()));
			doc.recycle();
			
			// System.out.println("created doc with unid "+doc.getUNID()+" note id
			// "+doc.getNoteID()+
			// " ("+Integer.toHexString(doc.getNoteID())+")"+",
			// seq="+doc.getOID().getSequenceTime()+",
			// modified="+doc.getModifiedInThisFile());
		}

		return unidsAndNoteIds;
	}

}
