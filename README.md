# Domino JNA
Cross-platform access to IBM/HCL Notes/Domino C API methods from Java

## Features
The project provides functionality that is not available in the classic Java API of HCL Notes/Domino or that is poorly implemented, for example:

* **run DQL (Domino Query Language) queries against databases** on Domino V10 and return the result dynamically sorted
* **view lookups using different key formats** (e.g. strings, numbers, dates, date ranges) and equality/**inequality searches (e.g. find less or greater than a search key)**
* **decodes all available types of view column data types** (string, string list, number, number list, datetime, datetime list) **and row data** (e.g. just note id, UNID, counts, unread flag etc.)
* read view data as **another Notes user**
* extract **read access information for rows in a view** that Domino stores in the view index
* **separation of Notes views and their data into multiple databases** (like programmatically creating private views and keeping them up-to-date incrementally)
* **dynamic filtering of view rows based on a Note id list with paging support (this really rocks!)**
* **reading categorized views** with expanded/collapsed entries and min/max level
* read **design collection** with design elements in a database
* **differential view reads** : on second view lookup, only read rows that have changed
* support for view resorting (changing the collation in C API terms)
* **direct attachment streaming to create and extract files** (HCL's Java API extracts files to temp disk space first to read attachment data and only supports adding files stored on disk as document attachments)
* basic APIs to read note (document) item values like String/Double/Calendar single and multiple values
* several APIs to write item values and attachments (needs more work)
* **richtext item reading**: convenience method to extract text content, advanced API to read all CD records (IRichTextNavigator)
* **richtext item writing**: create richtext items with text, images, doclinks, by rendering other notes and appending other richtet items
* **add PNG images to richtext items**: something that cannot easily be done yet in the Notes Client UI
* **richtext item conversion**: multi-level conversion of richtext item content, e.g. to **add/remove file hotspots (file icons with custom image that open file on click) independent from the actual file attachment** or do mail merge with richtext
* **design richtext processing**: e.g. to **apply a string replacement and recompile formulas** for computed text/subforms or hotspots and **find all fields in a form**
* **richtext-html conversion** with advanced quality and access to embedded images
* **incremental data synchronization** with external databases or indexers. Generic design, sample implementation for [CQEngine](https://github.com/npgall/cqengine) and [SQLite](https://www.sqlite.org)
* **quick check if a document is editable** by a specified user (without the need to scan through author items)
* **fulltext index creation** with all available options
* supports incremental synchronization of Domino databases by **reading noteid lists of modified and deleted documents** (HCL's Java API does not return ids of deleted docs)
* searching NSF data with formula on the fly (NSFSearch in the C API) with all parameters and return values, e.g. **get summary buffer data for each document matching a formula and compute your own field values like the view indexer does**
* quick reading of files and folders in the Domino data directory or in subdirectories (HCL's DbDirectory is slow for many DBs and does not support subdirectory scanning)
* **run agents bypassing ECL checks**, pass an in-memory document for data exchange readable as `Session.DocumentContext` and redirect Agent output to a Java `Writer`
* read/write replication data (change replica id and flags)
* clearing the replication history
* fast noteid / UNID bulk conversion with lookup of "modified in this file" property (part of the note OID - originator id)
* compute @Usernameslist values for any Notes user on local and remote server
* faster **formula execution on documents** with document modified/selected/deleted by formula check
* **SSO token computation** (with tokens also working on Websphere)
* APIs to **get/put/sync IDs with the ID Vault** and to **sign/encrypt/decrypt** documents and attachments
* APIs to **read extended busytime information** like UNID/start/end of busytime entries (not just the freetime search that HCL provides)
* APIs to **create/read/update Domino appointments** via iCal format and the option to only output selected fields into the generated iCal (e.g. only start/end/summary) and full meeting workflow action support (accept/decline invitation etc.)
* APIs to **read and modify the ECL**
* APIs to **read and write Out-of-Office information (OOO) of any user** 
* API to **read the item definition table** of a database (all fieldnames and fieldtypes, useful to track FT Search issues and provide fieldname typeahead)
* **DXL exporter with the option to write the DXL into a stream** (Notes.jar classes fill up the Java heap with a giant DXL string)

**Please note:**

**The project gives access to some really low level functions of HCL Notes/Domino. Using them in the wrong way or sending unexpected parameter values might crash your application server, so make sure you know what you are doing and test your code on a local machine first!**

One reason for open sourcing all this stuff was to get more hands on it and make it as robust as possible.

## Supported platforms
The code should run in 32 and 64 bit Notes Client and Domino server environments on Windows, Linux and Mac.

It is not expected to run without changes on other platforms, mainly because of little endian / big endian differences or memory alignments, but
we don't currently have access to those platforms anyway.

## XPages
Domino JNA can be used in XPages applications!

See the [release](https://github.com/klehmann/domino-jna/releases) section for ready to install builds.
Those work similar to the XPages Extension Libary. So you need to install the provided OSGi plugins both in Domino Designer and the Domino Server.

Here are installation instructions how to do this: [link](http://www.tlcc.com/admin/tlccsite.nsf/pages/extension-lib).

The API is available in the code editor after you activate the `com.mindoo.domino.jna.xsp.library` entry in the xsp.properties file.

## Maven
Domino JNA is available on Maven Central: [https://mvnrepository.com/artifact/com.mindoo.domino/domino-jna](https://mvnrepository.com/artifact/com.mindoo.domino/domino-jna).

```xml
<dependency>
    <groupId>com.mindoo.domino</groupId>
    <artifactId>domino-jna</artifactId>
    <version>0.9.24</version>
</dependency>
```

Use repository [https://oss.sonatype.org/content/repositories/snapshots](https://oss.sonatype.org/content/repositories/snapshots) to get snapshots.

```xml
<dependency>
    <groupId>com.mindoo.domino</groupId>
    <artifactId>domino-jna</artifactId>
    <version>0.9.25-SNAPSHOT</version>
</dependency>
```

## Standalone applications
There is a [sample database](https://github.com/klehmann/domino-jna/tree/master/standalone-app-sample) available that demonstrates how to use Domino JNA in standalone Java applications.

## Usage
Here is a code snippet for the API usage. It opens a database and filters view entries.

```java
NotesGC.runWithAutoGC(new Callable<Object>() {

	public Object call() throws Exception {
		//open database with the same access rights as a lotus.domino.Session
		NotesDatabase dbData = new NotesDatabase(session, "", "fakenames.nsf");

		//alternative: open database as another user (used for read and write access):
		//NotesDatabase dbData = new NotesDatabase("", "fakenames.nsf", "John Doe/Mindoo");

		//open database as the server:				
		//NotesDatabase dbData = new NotesDatabase("", "fakenames.nsf", "");

		//open People view (in C API called collection)
		NotesCollection peopleView = dbData.openCollectionByName("People");

		//read all note ids from the collection
		boolean includeCategoryIds = false;
		LinkedHashSet<Integer> allIds = peopleView.getAllIds(includeCategoryIds);
				
		//pick random note ids
		Integer[] allIdsArr = allIds.toArray(new Integer[allIds.size()]);
		Set<Integer> pickedNoteIds = new HashSet<Integer>();
		while (pickedNoteIds.size() < 1000) {
			int randomIndex = (int) (Math.random() * allIdsArr.length);
			int randomNoteId = allIdsArr[randomIndex];
			pickedNoteIds.add(randomNoteId);
		}
				
		//populate the collection's selected list with picked ids
		boolean clearPreviousSelection = true
		peopleView.select(pickedNoteIds, clearPreviousSelection);

		//next, traverse selected entries only, starting at position "0" (top of the view)
		String startPos = "0";
		//skip from "0" to the first entry that we are allowed to read
		//(its position could be different from "1" caused by reader fields)
		int entriesToSkip = 1;
		//add all read entries to the result list
		int entriesToReturn = Integer.MAX_VALUE;
		//tell the API how to navigate in the view: from one entry in the selectedList
		//to the next one (in view ordering)
		EnumSet<Navigate> returnNavigator = EnumSet.of(Navigate.NEXT_SELECTED);
		//preload the maximum number of entries, can be useful when implementing
		//filter method in EntriesAsListCallback
		int bufferSize = Integer.MAX_VALUE;
		//tell the API which data we want to read (in this case note ids and column values map)
		EnumSet<ReadMask> returnData = EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES);
		
		List<NotesViewEntryData> selectedEntries = peopleView.getAllEntries(startPos, entriesToSkip,
				returnNavigator, Integer.MAX_VALUE,
				returnData, new EntriesAsListCallback(entriesToReturn));
				
		for (NotesViewEntryData currEntry : selectedEntries) {
		
			//check that all entries that we read were from our picked id list
			Assert.assertTrue("Entry read from view is contained in selected list",
				pickedNoteIds.contains(currEntry.getNoteId()));
				
			//read column values with their programmatic name
			String firstName = (String) currEntry.get("firstname");
			String lastName = (String) currEntry.get("lastname");
			//...
		}
		
		//now remove all read ids from pickedNoteIds and make sure that we found everything
		//we were searching for
		for (NotesViewEntryData currEntry : selectedEntries) {
			pickedNoteIds.remove(currEntry.getNoteId());
		}
		
		Assert.assertTrue("All ids from the selected list can be found in the view", pickedNoteIds.isEmpty());
		return null;
	}
});
```

Reducing the view to a specific selection (of note ids) is already the first big surprise, if you only know HCL's Java API for Domino.

Comparable to reading fulltext search results, but a lot more powerful!
And the cool thing is that Domino handles the filtering and even the paging for you (`entriesToSkip` parameter). so you don't have to waste
time to read and skip data slowly in your own code.


As you can see, all calls have to be wrapped in `NotesGC.runWithAutoGC` code blocks (which can also be nested).

We do this to automatically collect allocated C handles and automatically free them when the code block is done.

In many cases, this should avoid manual recycling of API objects, but for some edge cases, objects like `NotesCollection` (which is the term for Notes View in
the C API) or `NotesIDTable` do have a `recycle()` method.

When running in an XPages environment, `NotesGC.runWithAutoGC` can be omitted when the code processes a HTTP request (e.g. an XAgent). It is only required if you run code in separate threads, e.g. using the `SessionCloner` class.

## Creating your own build
The following instructions are only relevant when you want to create your own Domino JNA release version.

### Registration of local Notes.jar
Before running the test cases or building the project, your local Notes.jar file needs to be added to Maven's local repository, because
it's platform and Domino version dependent.

**For Windows, use this syntax (with the right path to Notes.jar on your machine):**

```
mvn install:install-file -Dfile="C:\Program Files (x86)\IBM\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar
```

**For the Mac, use this syntax:**

```
mvn install:install-file -Dfile="/Applications/IBM Notes.app/Contents/MacOS/jvm/lib/ext/Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar
```

### Maven build

**Windows:**
To build against the HCL Notes Client on Windows, make sure you use a 32 bit JDK (e.g. 1.8) and use this command in the "domino-jna" directory:

```
mvn -DJVMPARAMS= -DDOMINOOSGIDIR="C:\Program Files (x86)\IBM\Notes\osgi" -DDOMINODIR="C:\Program Files (x86)\IBM\Notes" -DNOTESINI="C:\Program Files (x86)\IBM\Notes\Notes.ini" clean install
```

**Mac:**
On Mac, we have only had full build success including tests with the 32 bit HCL Notes Client so far, using a 32 bit JDK 1.6.
We had to downgrade Maven to version 3.2.5 for the build, because that was the last version compatible with JDK 1.6.

This command should work for 32 bit:
```
mvn -DJVMPARAMS=-d32 -DDOMINOOSGIDIR=/Applications/IBM\ Notes.app/Contents/MacOS -DDOMINODIR=/Applications/IBM\ Notes.app/Contents/MacOS -DNOTESINI=~/Library/Preferences/Notes\ Preferences clean install
```

For 64 bit, running the test cases currently fails with a libxml.dylib loading error and we still need to figure out how to fix this.

With skipped testcases, this command should run fine:

```
mvn -DJVMPARAMS=-d64 -DDOMINOOSGIDIR=/Applications/IBM\ Notes.app/Contents/MacOS -DDOMINODIR=/Applications/IBM\ Notes.app/Contents/MacOS -DNOTESINI=~/Library/Preferences/Notes\ Preferences clean install -Dmaven.test.skip=true
```

The directory `target/lib` contains all recursive dependencies required to use the library, e.g. JNA and Apache tool libraries.
The "domino-api-binaries.jar" generated there is just the Notes.jar that you previously have added to Maven.

### Running the test cases
The project contains a number of test cases that demonstrate how the API is used.
These test cases use sample databases that we provide for download and will update from time to time depending on the requirements of newer testcases.

You can download the two sample databases fakenames.nsf and fakenames-views.nsf from this URL:

**[Download Link](https://fs.mindoo.de/f/cd86250173344f18bce5/?dl=1)**

Next, place them in the data folder of your HCL Notes Client.

fakenames.nsf is a directory database that contains about 40,000 sample documents and some additional lookup views, fakenames-views.nsf uses the same
database design, but does not contain any data.

We use fakenames-views.nsf to demonstrate indexing of external Domino data (take a local view and incrementally pull data from an external NSF database,
like the Notes Client does with private views).
 
In Eclipse, make sure to add the following environment variables (with the right paths for your machine) to the Run Configurations to run testcases:

**Windows:**

```
PATH = C:\Program Files (x86)\IBM\Notes
```

**Mac:**

```
DYLD_LIBRARY_PATH = /Applications/IBM Notes.app/Contents/MacOS
Notes_ExecDirectory = /Applications/IBM Notes.app/Contents/MacOS
NOTESBIN = /Applications/IBM Notes.app/Contents/MacOS
PATH = /Applications/IBM Notes.app/Contents/MacOS
NotesINI = ~/Library/Preferences/Notes Preferences
```

### Creating XPages plugin build
The projects `com.mindoo.domino.jna.xsp.build` and `domino-target` contain experimental build scripts to use Domino JNA in XPages applications, similar to HCL's XPages Extension Library.

Please use the following steps to create a build or just download a binary build from the "releases" section.
 
**1. Target platform**
 
To create the build, you first need to create the Eclipse target platform that we will compile against.
This step is only required once.

In project `domino-target`, call `mvn clean install` with the same parameters described above (`JVMPARAMS`, `DOMINOOSGIDIR`, `DOMINODIR` and `NOTESINI`).

When the build is done, the directory `domino-target/target/repository` contains a P2 Update Site containing the features and plugins of the installed HCL Notes Client that can be used by Maven/Tycho.

**2. Build Update Site**
 
Next call `mvn clean install` (also with parameters `JVMPARAMS`, `DOMINOOSGIDIR`, `DOMINODIR` and `NOTESINI`) in project `com.mindoo.domino.jna.xsp.build`.

This copies the current Domino JNA source code from project `domino-jna` into two Eclipse plugins `com.mindoo.domino.jna.xsp/jna-src` and `com.mindoo.domino.jna.xsp.source/jna-src` and starts the compilation.

`com.mindoo.domino.jna.xsp` provides the extension library for XPages and `com.mindoo.domino.jna.xsp.source` provides the source code for the Java editor of HCL Domino Designer.

You can find the created Update Site in directory `com.mindoo.domino.jna.xsp-updatesite/target/site`.

## Further information
* [New on Github: Domino JNA - Cross-platform access to IBM/HCL Notes/Domino C API methods from Java](http://www.mindoo.com/web/blog.nsf/dx/08.04.2016191137KLEN6U.htm?opendocument&comments)
* [Big update for Domino JNA project on Github](http://www.mindoo.com/web/blog.nsf/dx/11.07.2016233301KLETA8.htm?opendocument&comments)
* [New APIs for Domino JNA project, now available for XPages development](http://www.mindoo.com/web/blog.nsf/dx/16.01.2017082125KLEAMY.htm?opendocument&comments)
* [Explore the hidden parts of an application](https://www.eknori.de/2018-01-29/explore-the-hidden-parts-of-an-application-ibmchampion/)
* [Query Domino data and faceted search with Domino JNA (part 1) by Mark Leusink](http://linqed.eu/2018/10/02/query-domino-data-and-faceted-search-with-domino-jna-part-1/)
* [Query Domino data and faceted search with Domino JNA (part 2) by Mark Leusink](http://linqed.eu/2018/10/08/query-domino-data-and-faceted-search-with-domino-jna-part-2/)
* [Query Domino data with Domino JNA (part 3): REST API and infinite scroll by Mark Leusink](http://linqed.eu/2018/11/02/query-domino-data-with-domino-jna-part-3-rest-api-and-infinite-scroll/)

## Next steps
This project is not done yet, this is just the beginning.
Here are some of the things that we plan to do:

* write [blog entries](http://blog.mindoo.com) explaining the API internals
* add more API methods, e.g. an API to write MIME items
* write more testcases
* add more syntactical sugar, hide complexity

## Licence
The code is available under Apache 2.0 license.

Copyright by [Mindoo GmbH](http://www.mindoo.com)

## Dependencies
The code uses the following open source projects:

[metadata-extractor](https://github.com/drewnoakes/metadata-extractor)
for image file metadata extraction, available under Apache 2.0 license

[Apache Commons Collections 4](https://commons.apache.org/proper/commons-collections/)
for case insensitive maps, available under Apache 2.0 license

[ICU4J](http://site.icu-project.org/home/why-use-icu4j)
for LMBCS - Java String conversion without C API calls, [license](https://github.com/unicode-org/icu/blob/master/icu4c/LICENSE)

