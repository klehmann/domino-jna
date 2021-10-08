package com.mindoo.domino.jna.xsp.internal.mime;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.mime.IMimeDataAccessService;
import com.mindoo.domino.jna.mime.MIMEData;

/**
 * Implementation of {@link IMimeDataAccessService} that uses its own classloader
 * to load newer versions of javax.mail and javax.activation than the Domino
 * OSGi platform provides.
 * 
 * @author Karsten Lehmann
 */
public class ReflectionJakartaMimeDataAccessService implements IMimeDataAccessService {
	private static final String LIBS_DOMINOJNA_MIMESERVICE_JAR = "/lib-customloaded/domino-jna-mime-jakartamail-0.9.44-SNAPSHOT.jar";
	private static final String LIBS_JAVAX_MAIL_JAR = "/lib-customloaded/dependencies/jakarta.mail-2.0.0-RC6.jar";
	private static final String LIBS_ACTIVATION_JAR = "/lib-customloaded/dependencies/jakarta.activation-2.0.0-RC3.jar";

	private ClassLoader m_jakartaMailClassloader;
	private IMimeDataAccessService m_wrappedService;
	private Exception m_initEx;
	
	public ReflectionJakartaMimeDataAccessService() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				ClassLoader pluginCl = ReflectionJakartaMimeDataAccessService.class.getClassLoader();
				PackageBlockingClassLoader blockingCl = new PackageBlockingClassLoader(pluginCl);
				m_jakartaMailClassloader = new CustomURLClassLoader(getResourceUrls(), blockingCl, AccessController.getContext());
				
				try {
					Class<? extends IMimeDataAccessService> wrappedServiceClazz = (Class<? extends IMimeDataAccessService>) m_jakartaMailClassloader.loadClass("com.mindoo.domino.jna.mime.internal.JakartaMailMimeDataAccessService");
					m_wrappedService = wrappedServiceClazz.newInstance();
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					m_initEx = e;
				}
				return null;
			}
		});
	}

	private static URL[] getResourceUrls() {
		URL activationJar = ReflectionJakartaMimeDataAccessService.class.getResource(LIBS_ACTIVATION_JAR);
		if (activationJar==null) {
			throw new IllegalStateException("Resource " + LIBS_ACTIVATION_JAR + " not found");
		}
		URL javaxMailJar = ReflectionJakartaMimeDataAccessService.class.getResource(LIBS_JAVAX_MAIL_JAR);
		if (javaxMailJar==null) {
			throw new IllegalStateException("Resource " + LIBS_JAVAX_MAIL_JAR + " not found");
		}
		
		URL dominoJNAMimeServiceJar = ReflectionJakartaMimeDataAccessService.class.getResource(LIBS_DOMINOJNA_MIMESERVICE_JAR);
		if (dominoJNAMimeServiceJar==null) {
			throw new IllegalStateException("Resource " + LIBS_DOMINOJNA_MIMESERVICE_JAR + " not found");
		}

		return new URL[] {activationJar, javaxMailJar, dominoJNAMimeServiceJar};
	}
	
	public MIMEData getMimeData(NotesNote note, String itemName) {
		if (m_initEx!=null) {
			throw new IllegalStateException("Error initializing the wrapped MIME service", m_initEx);
		}
		return AccessController.doPrivileged((PrivilegedAction<MIMEData>) ()->{
			ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(m_jakartaMailClassloader);
			try {
				return m_wrappedService.getMimeData(note, itemName);
			}
			finally {
				Thread.currentThread().setContextClassLoader(ctxCl);
			}
		});
	}

	public void setMimeData(NotesNote note, String itemName, MIMEData mimeData) {
		if (m_initEx!=null) {
			throw new IllegalStateException("Error initializing the wrapped MIME service", m_initEx);
		}
		
		AccessController.doPrivileged((PrivilegedAction<Object>) ()->{
			ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(m_jakartaMailClassloader);
			try {
				m_wrappedService.setMimeData(note, itemName, mimeData);
			}
			finally {
				Thread.currentThread().setContextClassLoader(ctxCl);
			}
			return null;
		});
	}

	@Override
	public int getPriority() {
		//lower priority than com.mindoo.domino.jna.mime.internal.JavaxMailMimeDataAccessService to
		//be picked first if both exist
		return 10;
	}
}
