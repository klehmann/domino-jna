# domino-jna
Java project to access the IBM Domino C API using Java Native Access (JNA)

## Features
The project provides functionality that is not available in the classic Java API of IBM Notes/Domino or that is poorly implemented.

* view lookups using different key formats (e.g. strings, numbers, dates, date ranges) and equality/inequality searches (e.g. find less or greater than)
* decodes all available types of view data (string, string list, number, number list, datetime, datetime list)
* read view data as another Notes user
* support for view resorting (changing the sort column = collation)
* separation of Notes views and their data into multiple databases (like programmatically creating private views and keeping them up-to-date)
* fulltext index creation with all available options
* supports incremental synchronization of Domino databases by reading noteid lists of modified and deleted documents (IBM's Java API does not return ids of deleted docs)
* clearing the replication history
* compute @Usernameslist values for any Notes user
 
## Supported platforms
The code should run with 32 and 64 bit Notes Client and Domino server environments on Windows, Linux and Mac.

It is not expected to run without changes on other platforms, mainly because of little endian / big endian differences or memory alignments.

## Usage

### Registration of local Notes.jar
Before running the test cases, the local Notes.jar file needs to be added to Maven as a repository.

**For Windows, use this syntax (with the right path to Notes.jar on your machine):**

	`mvn install:install-file -Dfile="C:\Program Files (x86)\IBM\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar`

**For the Mac, use this syntax:**

	`mvn install:install-file -Dfile="/Applications/IBM Notes.app/Contents/MacOS/jvm/lib/ext/Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar`

### Maven build
Automatic Maven builds and test execution is not done yet.

### Running the test cases
Download the sample databases fakenames.nsf and fakenames-views.nsf from this URL:

[ftp://domino_jna:domino_jna@www2.mindoo.de](ftp://domino_jna:domino_jna@www2.mindoo.de)

and place them in the root folder of your IBM Notes Client.

fakenames.nsf contains sample documents and a few lookup views, fakenames-views.nsf uses the same database design, but does not contain any data.

We use fakenames-views.nsf to demonstrate indexing of external Domino data (take a local view and incrementally pull data from an external NSF database).

 
In Eclipse, make sure to add the following environment variables (with the right paths for your machine) to the Run Configurations for testcases:

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

## Licence
The code is available under Apache 2.0 license.

Copyright by [Mindoo GmbH](http://www.mindoo.com)

