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

## Licence
The code is available under Apache 2.0 license.

Copyright by [Mindoo GmbH](http://www.mindoo.com)

