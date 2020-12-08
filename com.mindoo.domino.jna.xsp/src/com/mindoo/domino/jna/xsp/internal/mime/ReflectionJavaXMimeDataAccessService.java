package com.mindoo.domino.jna.xsp.internal.mime;

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
public class ReflectionJavaXMimeDataAccessService implements IMimeDataAccessService {
	private JavaXMailClassloader m_javaxMailClassloader;
	private IMimeDataAccessService m_wrappedService;
	private Exception m_initEx;
	
	public ReflectionJavaXMimeDataAccessService() {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				m_javaxMailClassloader = new JavaXMailClassloader(getClass().getClassLoader());
				try {
					Class<? extends IMimeDataAccessService> wrappedServiceClazz = (Class<? extends IMimeDataAccessService>) m_javaxMailClassloader.loadClass("com.mindoo.domino.jna.mime.internal.JavaxMailMimeDataAccessService");
					m_wrappedService = wrappedServiceClazz.newInstance();
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					m_initEx = e;
				}
				return null;
			}
		});
	}
	
	public MIMEData getMimeData(NotesNote note, String itemName) {
		if (m_initEx!=null) {
			throw new IllegalStateException("Error initializing the wrapped MIME service", m_initEx);
		}
		return m_wrappedService.getMimeData(note, itemName);
	}

	public void setMimeData(NotesNote note, String itemName, MIMEData mimeData) {
		if (m_initEx!=null) {
			throw new IllegalStateException("Error initializing the wrapped MIME service", m_initEx);
		}
		m_wrappedService.setMimeData(note, itemName, mimeData);
	}

	@Override
	public int getPriority() {
		//lower priority than com.mindoo.domino.jna.mime.internal.JavaxMailMimeDataAccessService to
		//be picked first if both exist
		return 10;
	}
}
