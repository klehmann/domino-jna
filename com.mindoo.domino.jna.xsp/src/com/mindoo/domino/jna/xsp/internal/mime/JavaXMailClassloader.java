package com.mindoo.domino.jna.xsp.internal.mime;

import java.net.URL;
import java.net.URLClassLoader;

public class JavaXMailClassloader extends URLClassLoader {
	private static final String LIBS_DOMINOJNA_MIMESERVICE_JAR = "/lib-customloaded/domino-jna-mime-javaxmail-0.9.41.jar";
	private static final String LIBS_JAVAX_MAIL_JAR = "/lib-customloaded/dependencies/javax.mail-1.5.2.jar";
	private static final String LIBS_COMMONS_EMAILJAR = "/lib-customloaded/dependencies/commons-email-1.5.jar";
	private static final String LIBS_ACTIVATION_JAR = "/lib-customloaded/dependencies/activation-1.1.jar";

	public JavaXMailClassloader(ClassLoader parent) {
		super(getResourceUrls(), new PackageBlockingClassLoader(parent));
	}

	private static URL[] getResourceUrls() {
		URL activationJar = JavaXMailClassloader.class.getResource(LIBS_ACTIVATION_JAR);
		if (activationJar==null) {
			throw new IllegalStateException("Resource " + LIBS_ACTIVATION_JAR + " not found");
		}
		URL commonsEmailJar = JavaXMailClassloader.class.getResource(LIBS_COMMONS_EMAILJAR);
		if (commonsEmailJar==null) {
			throw new IllegalStateException("Resource " + LIBS_COMMONS_EMAILJAR + " not found");
		}
		URL javaxMailJar = JavaXMailClassloader.class.getResource(LIBS_JAVAX_MAIL_JAR);
		if (javaxMailJar==null) {
			throw new IllegalStateException("Resource " + LIBS_JAVAX_MAIL_JAR + " not found");
		}
		
		URL dominoJNAMimeServiceJar = JavaXMailClassloader.class.getResource(LIBS_DOMINOJNA_MIMESERVICE_JAR);
		if (dominoJNAMimeServiceJar==null) {
			throw new IllegalStateException("Resource " + LIBS_DOMINOJNA_MIMESERVICE_JAR + " not found");
		}

		return new URL[] {activationJar, commonsEmailJar, javaxMailJar, dominoJNAMimeServiceJar};
	}
	
}
