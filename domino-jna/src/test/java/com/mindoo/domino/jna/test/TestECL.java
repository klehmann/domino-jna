package com.mindoo.domino.jna.test;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.ecl.ECL;
import com.mindoo.domino.jna.ecl.ECL.ECLCapability;
import com.mindoo.domino.jna.ecl.ECL.ECLType;

import lotus.domino.Session;

/**
 * Tests cases for ECL
 * 
 * @author Karsten Lehmann
 */
public class TestECL extends BaseJNATestClass {

	/**
	 * Reads the ECL for the current user
	 */
	@Test
	public void testStringUtils_replicaIdInnards() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting ECL test");

				ECL eclLS = ECL.getInstance(ECLType.Lotusscript, session.getEffectiveUserName());
				EnumSet<ECLCapability> lsCapabilities = eclLS.getCapabilities();
				System.out.println("Lotusscript ECL:\n"+lsCapabilities);
				Assert.assertTrue("Method returned any LS capabilities", !lsCapabilities.isEmpty());
				
				ECL eclApplets = ECL.getInstance(ECLType.JavaApplets, session.getEffectiveUserName());
				EnumSet<ECLCapability> appletCapabilities = eclApplets.getCapabilities();
				System.out.println("Java Applet ECL:\n"+appletCapabilities);
				Assert.assertTrue("Method returned any Applet capabilities", !appletCapabilities.isEmpty());

				ECL eclJavascript = ECL.getInstance(ECLType.Javascript, session.getEffectiveUserName());
				EnumSet<ECLCapability> javascriptCapabilities = eclJavascript.getCapabilities();
				System.out.println("Javascript ECL:\n"+javascriptCapabilities);
				Assert.assertTrue("Method returned any Javascript capabilities", !javascriptCapabilities.isEmpty());

				System.out.println("Done with ECL test");
				return null;
			}
		});
	}

}
