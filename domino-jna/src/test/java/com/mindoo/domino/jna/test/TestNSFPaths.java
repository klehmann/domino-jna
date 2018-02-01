package com.mindoo.domino.jna.test;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;

public class TestNSFPaths {

	@Test
	public void testPaths() {
		String[] nsfPathsToTest = new String[] {
				"test.nsf",
				"test.NSF",
				"test.NSf",
				"test.NS7",
				"test.Ntf",
				"path/test.box",
				"path/testäöü.box",
				"path\\test.nsh",
				"path/test.nsh"
		};
		for (String currPath : nsfPathsToTest) {
			Assert.assertTrue("Detected as NSF: "+currPath, NotesDatabase.isDatabasePath(currPath));
		}
		
		String[] otherPaths = new String[] {
				"test.txt",
				"path/test.txt",
				"path\\test.txt",
				"path\\test.nsfx",
				"path\\test.nsf\\xxxx"
		};
		
		for (String currPath : otherPaths) {
			Assert.assertFalse("Detected as non-NSF: "+currPath, NotesDatabase.isDatabasePath(currPath));
		}
	}
}
