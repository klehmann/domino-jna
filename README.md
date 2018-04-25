# Domino JNA
Cross-platform access to IBM Notes/Domino C API methods from Java

## Features
The project provides functionality that is not available in the classic Java API of IBM Notes/Domino or that is poorly implemented, for example:

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
* **direct attachment streaming** (IBM's Java API extracts files to temp disk space first to read attachment data)
* basic APIs to read note (document) item values like String/Double/Calendar single and multiple values
* several APIs to write item values and attachments (needs more work)
* **richtext item reading**: convenience method to extract text content, advanced API to read all CD records (IRichTextNavigator)
* **richtext item writing**: create richtext items with text, images, doclinks, by rendering other notes and appending other richtet items
* **richtext item conversion**: multi-level conversion of richtext item content, e.g. to **add/remove file hotspots (file icons with custom image that open file on click) independent from the actual file attachment** or do mail merge with richtext
* **richtext-html conversion** with advanced quality and access to embedded images
* **incremental data synchronization** with external databases or indexers. Generic design, sample implementation for [CQEngine](https://github.com/npgall/cqengine) and [SQLite](https://www.sqlite.org)
* **quick check if a document is editable** by a specified user (without the need to scan through author items)
* **fulltext index creation** with all available options
* supports incremental synchronization of Domino databases by **reading noteid lists of modified and deleted documents** (IBM's Java API does not return ids of deleted docs)
* searching NSF data with formula on the fly (NSFSearch in the C API) with all parameters and return values, e.g. **get summary buffer data for each document matching a formula and compute your own field values like the view indexer does**
* quick reading of files and folders in the Domino data directory or in subdirectories (IBM's DbDirectory is slow for many DBs and does not support subdirectory scanning)
* **run agents bypassing ECL checks**, pass an in-memory document for data exchange readable as `Session.DocumentContext` and redirect Agent output to a Java `Writer`
* read/write replication data (replica id and flags)
* clearing the replication history
* fast noteid / UNID bulk conversion with lookup of "modified in this file" property (part of the note OID - originator id)
* compute @Usernameslist values for any Notes user
* faster **formula execution on documents** with document modified/selected/deleted by formula check
* **SSO token computation** (with tokens also working on Websphere)
* APIs to **get/put/sync IDs with the ID Vault** and to **sign/encrypt/decrypt** documents and attachments
* APIs to **read extended busytime information** like UNID/start/end of busytime entries (not just the freetime search that IBM provides)
* APIs to **read and modify the ECL**

**Please note:**

**The project gives access to some really low level functions of IBM Notes/Domino. Using them in the wrong way or sending unexpected parameter values might crash your application server, so make sure you know what you are doing and test your code on a local machine first!**

One reason for open sourcing all this stuff was to get more hands on it and make it as robust as possible.

## Supported platforms
The code should run in 32 and 64 bit Notes Client and Domino server environments on Windows, Linux and Mac.

It is not expected to run without changes on other platforms, mainly because of little endian / big endian differences or memory alignments, but
we don't currently have access to those platforms anyway.

## Usage
Here is a code snippet to demonstrate how to use the API:

```java
NotesGC.runWithAutoGC(new Callable<Object>() {

	public Object call() throws Exception {
		NotesDatabase dbData = getFakeNamesDb();
				
		//open People view
		NotesCollection colFromDbData = dbData.openCollectionByName("People");

		//read all note ids from the collection
		boolean includeCategoryIds = false;
		LinkedHashSet<Integer> allIds = colFromDbData.getAllIds(includeCategoryIds);
				
		//pick random note ids
		Integer[] allIdsArr = allIds.toArray(new Integer[allIds.size()]);
		Set<Integer> pickedNoteIds = new HashSet<Integer>();
		while (pickedNoteIds.size() < 1000) {
			int randomIndex = (int) (Math.random() * allIdsArr.length);
			int randomNoteId = allIdsArr[randomIndex];
			pickedNoteIds.add(randomNoteId);
		}
				
		//populate the collection's selected list with picked ids
		NotesIDTable selectedList = colFromDbData.getSelectedList();
		selectedList.clear();
		selectedList.addNotes(pickedNoteIds);

		//for remote databases, re-send modified SelectedList
		colFromDbData.updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));

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
		
		List<NotesViewEntryData> selectedEntries = colFromDbData.getAllEntries(startPos, entriesToSkip,
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

This selected list feature demonstrated above is already the first big surprise, if you only know IBM's Java API for Domino.
It dynamically filters the collection to only return entries matching an id list, that you might have read via previous lookups.

Comparable to reading fulltext search results, but a lot more powerful!
And the cool thing is that Domino handles the filtering and even the paging for you (`entriesToSkip` parameter). so you don't have to waste
time to read and skip data slowly in your own code.


As you can see, all calls have to be wrapped in `NotesGC.runWithAutoGC` code blocks (which can also be nested).

We do this to automatically collect allocated C handles and automatically free them when the code block is done.

In many cases, this should avoid manual recycling of API objects, but for some edge cases, objects like `NotesCollection` (which is the term for Notes View in
the C API) or `NotesIDTable`do have a `recycle()`method.

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
To build against the IBM Notes Client on Windows, make sure you use a 32 bit JDK (e.g. 1.8) and use this command in the "domino-jna" directory:

```
mvn -DJVMPARAMS= -DDOMINOOSGIDIR="C:\Program Files (x86)\IBM\Notes\osgi" -DDOMINODIR="C:\Program Files (x86)\IBM\Notes" -DNOTESINI="C:\Program Files (x86)\IBM\Notes\Notes.ini" clean install
```

**Mac:**
On Mac, we have only had full build success including tests with the 32 bit IBM Notes Client so far, using a 32 bit JDK 1.6.
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

**[ftp://domino_jna:domino_jna@www2.mindoo.de](ftp://domino_jna:domino_jna@www2.mindoo.de)**

Next, place them in the data folder of your IBM Notes Client.

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

## Using Domino JNA in XPages applications
The projects `com.mindoo.domino.jna.xsp.build` and `domino-target` contain experimental build scripts to use Domino JNA in XPages applications, similar to IBM's XPages Extension Library.

Please use the following steps to create a build or just download a binary build from the "releases" section.
 
**1. Target platform**
 
To create the build, you first need to create the Eclipse target platform that we will compile against.
This step is only required once.

In project `domino-target`, call `mvn clean install` with the same parameters described above (`JVMPARAMS`, `DOMINOOSGIDIR`, `DOMINODIR` and `NOTESINI`).

When the build is done, the directory `domino-target/target/repository` contains a P2 Update Site containing the features and plugins of the installed IBM Notes Client that can be used by Maven/Tycho.

**2. Build Update Site**
 
Next call `mvn clean install` (also with parameters `JVMPARAMS`, `DOMINOOSGIDIR`, `DOMINODIR` and `NOTESINI`) in project `com.mindoo.domino.jna.xsp.build`.

This copies the current Domino JNA source code from project `domino-jna` into two Eclipse plugins `com.mindoo.domino.jna.xsp/jna-src` and `com.mindoo.domino.jna.xsp.source/jna-src` and starts the compilation.

`com.mindoo.domino.jna.xsp` provides the extension library for XPages and `com.mindoo.domino.jna.xsp.source` provides the source code for the Java editor of IBM Domino Designer.

You can find the created Update Site in directory `com.mindoo.domino.jna.xsp-updatesite/target/site`.

**3. Install Update Site**
 
Similar to IBM's XPages Extension Library, the Domino JNA Update Site needs to be installed both in IBM Domino Designer and IBM Domino server R9.0.1+.

[Installation instructions from IBM](https://www-10.lotus.com/ldd/ddwiki.nsf/xpAPIViewer.xsp?lookupName=XPages+Extensibility+API#action=openDocument&res_title=XPages_Extension_Library_Deployment&content=apicontent)

When using the API in XPages applications, wrapping the code in `NotesGC.runWithAutoGC` blocks is not necessary, because we already do this in the plugin and clean up at the end of the XPages/JSF lifecycle.

## Further information
* [New on Github: Domino JNA - Cross-platform access to IBM Notes/Domino C API methods from Java](http://www.mindoo.com/web/blog.nsf/dx/08.04.2016191137KLEN6U.htm?opendocument&comments)
* [Big update for Domino JNA project on Github](http://www.mindoo.com/web/blog.nsf/dx/11.07.2016233301KLETA8.htm?opendocument&comments)
* [New APIs for Domino JNA project, now available for XPages development](http://www.mindoo.com/web/blog.nsf/dx/16.01.2017082125KLEAMY.htm?opendocument&comments)
* [Explore the hidden parts of an application](https://www.eknori.de/2018-01-29/explore-the-hidden-parts-of-an-application-ibmchampion/)

## Next steps
This project is not done yet, this is just the beginning.
Here are some of the things that we plan to do:

* write [blog entries](http://blog.mindoo.com) explaining the API internals
* add more API methods, e.g. more Setters to write document items
* write more testcases
* add more syntactical sugar, hide complexity

## Licence
The code is available under Apache 2.0 license.

Copyright by [Mindoo GmbH](http://www.mindoo.com)

## Dependencies
The code uses the following open source projects:

[metadata-extractor](https://github.com/drewnoakes/metadata-extractor)
available under Apache 2.0 license

[Apache Commons Collections 4](https://commons.apache.org/proper/commons-collections/)
available under Apache 2.0 license


