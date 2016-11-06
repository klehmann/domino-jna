package com.mindoo.domino.jna.test;

import java.io.File;

import org.junit.Test;

import com.mindoo.domino.jna.constants.OSDirectory;
import com.mindoo.domino.jna.utils.PlatformUtils;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Tests cases for platform utilities
 * 
 * @author Karsten Lehmann
 */
public class TestPlatformUtils extends BaseJNATestClass {

	/**
	 * Tests whether the methods to read platform paths return the right data.
	 */
	@Test
	public void testPlatformUtils_readPaths() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				
				String cDataDir = PlatformUtils.getOsDirectory(OSDirectory.DATA);
				System.out.println("data dir: "+cDataDir);
				String javaDataDir = session.getEnvironmentString("Directory", true);
				Assert.assertEquals("Dada directory is correct", new File(cDataDir), new File(javaDataDir));
				
				String cExeDir = PlatformUtils.getOsDirectory(OSDirectory.EXECUTABLE);
				System.out.println("exe dir: "+cExeDir);
				File notesExecutable1 = new File(cExeDir + "Notes");
				File notesExecutable2 = new File(cExeDir + "Notes.exe");
				Assert.assertEquals("Notes binary exists in executable directory", true, notesExecutable1.exists() || notesExecutable2.exists());
				
				String cTempDir = PlatformUtils.getOsDirectory(OSDirectory.TEMP);
				System.out.println("data dir: "+cTempDir);
				Assert.assertEquals("Temp dir exists", true, (new File(cTempDir)).exists());

				return null;
			}
		});
		
	}
}
