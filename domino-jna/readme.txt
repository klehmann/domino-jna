Registration of local Notes.jar
===============================
Please use a command like:

	mvn install:install-file -Dfile="C:\Program Files (x86)\IBM\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar

to register your own local copy of Notes.jar as a local Maven plugin, followed by "Maven / Update project..." on
this Eclipse project.

