# Notes Workspace API demo

This standalone example application reads and modifies the workspace of the local Notes Client.
It dumps the current workspace configuration to stdout, then create a new page "JNA Added Page" and a chicklet "Domino JNA Testicon" with
a read rectangle as the NSF icon.

Although the workspace API has been heavily tested, please make sure to create a backup of the desktop8.ndk before modifying it to avoid any data loss.

Make sure to have a Java 1.8 VM in the PATH and in JAVA_HOME.

# Windows
The file ```mvnw.cmd``` checks for typical Notes program directory and Notes.ini paths of a single user installation.
The implementation needs some work to support multi-user environments and to be more robust.

Use this command to run the application:

```
mvnw.cmd exec:java
```

# MacOS
The script ```mvnw``` should automatically detect the right Notes program directory path and set DYLD_LIBRARY_PATH accordingly.

Use this command to run the application:

```
./mvnw exec:java
```


