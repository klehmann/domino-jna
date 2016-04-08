# domino-jna
Java project to access the IBM Domino C API using Java Native Access (JNA)

## Features
The project provides functionality that is not available in the classic Java API of IBM Notes/Domino or that is poorly implemented, for example:

* view lookups using different key formats (e.g. strings, numbers, dates, date ranges) and equality/inequality searches (e.g. find less or greater than)
* decodes all available types of view data (string, string list, number, number list, datetime, datetime list)
* read view data as another Notes user
* support for view resorting (changing the collation in C API terms) 
* separation of Notes views and their data into multiple databases (like programmatically creating private views and keeping them up-to-date)
* fulltext index creation with all available options
* supports incremental synchronization of Domino databases by reading noteid lists of modified and deleted documents (IBM's Java API does not return ids of deleted docs)
* clearing the replication history
* compute @Usernameslist values for any Notes user
 
**Please note:**

**The project gives access to some really low level functions of IBM Notes/Domino. Using them in the wrong way or sending unexpected parameter values might crash your application server, so make sure you know what you are doing and test your code on a local machine first!**

One reason for open sourcing all this stuff was to get more hands on it and make it as robust as possible.

## Supported platforms
The code should run in 32 and 64 bit Notes Client and Domino server environments on Windows, Linux and Mac.

It is not expected to run without changes on other platforms, mainly because of little endian / big endian differences or memory alignments.

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
				
		//populate the collection's selected list with picked ids (only works if database with collection is accessed locally)
		NotesIDTable selectedList = colFromDbData.getSelectedList();
		selectedList.clear();
		selectedList.addNotes(pickedNoteIds);

		//next, traverse selected entries only, starting at position "0" (top of the view)
		String startPos = "0";
		//skip from "0" to the first entry that we are allowed to read
		int entriesToSkip = 1;
		//add all read entries to the result list
		int entriesToReturn = Integer.MAX_VALUE);
		//tell the API how to navigate in the view: from one entry in the selectedList to the next one (in view ordering)
		EnumSet<Navigate> returnNavigator = EnumSet.of(Navigate.NEXT_SELECTED);
		//use the maximum read buffer
		int bufferSize = Integer.MAX_VALUE;
		//tell the API which data we want to read (in this case only note ids, which is very fast)
		EnumSet<ReadMask> returnData = EnumSet.of(ReadMask.NOTEID);
		
		List<NotesViewEntryData> selectedEntries = colFromDbData.getAllEntries(startPos, entriesToSkip,
				returnNavigator, Integer.MAX_VALUE,
				returnData, new EntriesAsListCallback(entriesToReturn);
				
		//check that all entries that we read were from our picked id list
		for (NotesViewEntryData currEntry : selectedEntries) {
			Assert.assertTrue("Entry read from view is contained in selected list",
				pickedNoteIds.contains(currEntry.getNoteId()));
		}
		
		//now remove all read ids from pickedNoteIds and make sure that we found everything we were searching for
		for (NotesViewEntryData currEntry : selectedEntries) {
			pickedNoteIds.remove(currEntry.getNoteId());
		}
		
		Assert.assertTrue("All ids from the selected list can be found in the view", pickedNoteIds.isEmpty());
		return null;
	}
});
```

This selected list feature demonstrated above is already the first big surprise, if you only know IBM's Java API for Domino.

As you can see, all calls have to be wrapped in `NotesGC.runWithAutoGC` code blocks (which can also be nested).

We do this to automatically collect allocated C handles and automatically free them when the code block is done.

In many cases, this should avoid manually recycling API objects, but for some edge cases, objects like `NotesCollection` (which is the term for Notes view in
the C API) or `NotesIDTable`do have a `recycle()`method.

### Registration of local Notes.jar
Before running the test cases or building the project, the local Notes.jar file needs to be added to Maven as a repository.

**For Windows, use this syntax (with the right path to Notes.jar on your machine):**

```
mvn install:install-file -Dfile="C:\Program Files (x86)\IBM\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar`
```

**For the Mac, use this syntax:**

```
mvn install:install-file -Dfile="/Applications/IBM Notes.app/Contents/MacOS/jvm/lib/ext/Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar`
```

### Maven build

**Windows:**
To build against the IBM Notes Client on Windows, make sure you use a 32 bit JDK (e.g. 1.8) and use this command:

```
mvn -DJVMPARAMS= -DDOMINODIR="C:\Program Files (x86)\IBM\Notes" -DNOTESINI="C:\Program Files (x86)\IBM\Notes\Notes.ini" clean install
```

**Mac:**
On Mac, we have only had full build success including tests with the 32 bit IBM Notes Client so far, using a 32 bit JDK 1.6.
We had to downgrade Maven to version 3.2.5 for the build, because that was the last version compatible with JDK 1.6.

This command should work for 32 bit:
```
mvn -DJVMPARAMS=-d64 -DDOMINODIR=/Applications/IBM\ Notes.app/Contents/MacOS -DNOTESINI=~/Library/Preferences/Notes\ Preferences clean install
```

For 64 bit, running the test cases currently fails with a libxml.dylib loading error and we still need to figure out how to fix this.

With skipped testcases, this command should run fine:

```
mvn -DJVMPARAMS=-d64 -DDOMINODIR=/Applications/IBM\ Notes.app/Contents/MacOS -DNOTESINI=~/Library/Preferences/Notes\ Preferences clean install -Dmaven.test.skip=true
```

The directory `target/lib` contains all recursive dependencies required to use the library.

### Running the test cases
The project contains a number of test cases that demonstrate how the API is used.
These test cases use sample databases that we provide for download and will update from time to time depending on the requirements of newer testcases.

You can download the two sample databases fakenames.nsf and fakenames-views.nsf from this URL:

[ftp://domino_jna:domino_jna@www2.mindoo.de](ftp://domino_jna:domino_jna@www2.mindoo.de)

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

## Next steps
This project is not done yet, this is just the beginning.
Here are some of the things that we plan to do:

* write [blog entries](http://blog.mindoo.com) explaining the API internals
* add the API to an XPages Extension Library plugin for easier consumption from XPages applications
* add more API methods, e.g. NSFSearch is high on the list
* write more testcases
* add more syntactical sugar, hide complexity

## Licence
The code is available under Apache 2.0 license.

Copyright by [Mindoo GmbH](http://www.mindoo.com)

