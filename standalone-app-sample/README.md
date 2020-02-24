# Standalone application example

Simple example application that initializes the connection to a local Notes Client, opens the names.nsf database and reads the People view content.

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


