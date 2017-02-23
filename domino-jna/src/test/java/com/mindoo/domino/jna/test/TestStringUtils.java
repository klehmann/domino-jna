package com.mindoo.domino.jna.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDbReplicaInfo;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Session;

/**
 * Tests cases for string utilities
 * 
 * @author Karsten Lehmann
 */
public class TestStringUtils extends BaseJNATestClass {
	public static final Character UML_A_SMALL = Character.valueOf((char)228);
	public static final Character UML_O_SMALL = Character.valueOf((char)246);
	public static final Character UML_U_SMALL = Character.valueOf((char)252);
	public static final Character UML_A_BIG = Character.valueOf((char)196);
	public static final Character UML_O_BIG = Character.valueOf((char)214);
	public static final Character UML_U_BIG = Character.valueOf((char)220);
	public static final Character SZ = Character.valueOf((char)223);

	/**
	 * Tests whether our LMBCS / UTF-8 conversion functions work
	 */
	@Test
	public void testStringUtils_encodingRoundtripOfLargeStrings() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting LMBCS/UTF8 roundtrip test");
				
				for (int loop=0; loop<3; loop++) {
					System.out.println("Run #"+(loop+1));
					
					final int randomStringSize = 100000;
					List<Character> chars = new ArrayList<Character>();
					for (char c='a'; c<='z'; c++) {
						chars.add(c);
					}
					for (char c='A'; c<='Z'; c++) {
						chars.add(c);
					}
					for (char c='0'; c<='9'; c++) {
						chars.add(c);
					}
					//add some German umlauts
					chars.add(UML_A_SMALL);
					chars.add(UML_O_SMALL);
					chars.add(UML_U_SMALL);
					chars.add(UML_A_BIG);
					chars.add(UML_O_BIG);
					chars.add(UML_U_BIG);
					chars.add(SZ);
					
					StringBuilder randomStrBuilder = new StringBuilder();
					for (int i=0; i<randomStringSize; i++) {
						int randomIdx = (int) (Math.random() * chars.size());
						Character randomChar = chars.get(randomIdx);
						randomStrBuilder.append(randomChar.charValue());
					}

					String randomStr = randomStrBuilder.toString();
					int randomStrLengthUTF8 = StringUtil.stringLengthInUTF8(randomStr);
					int testRandomStrLengthUTF8Converted = randomStr.getBytes("UTF-8").length;
					
					Assert.assertEquals("Method StringUtil.stringLengthInUTF8 computes a correct result", randomStrLengthUTF8, testRandomStrLengthUTF8Converted);
					
					Memory randomStrLMBCS = NotesStringUtils.toLMBCS(randomStr, false);
					String randomStrLMBCSAsStr1 = NotesStringUtils.fromLMBCS(randomStrLMBCS, randomStrLMBCS.size());
					
//					if (randomStr.length() < randomStrLMBCSAsStr1.length()) {
//						if (randomStrLMBCSAsStr1.substring(0, randomStr.length()).equals(randomStr)) {
//							System.out.println("String lengths is wrong, but prefix is ok. randomStrLengthUTF8="+randomStrLengthUTF8+
//									", randomStrLMBCS.size()="+randomStrLMBCS.size()+", randomStr.length()="+randomStr.length()+
//									", randomStrLMBCSAsStr1.length()="+randomStrLMBCSAsStr1.length());
//							System.out.println("Added character:"+randomStrLMBCSAsStr1.charAt(randomStrLMBCSAsStr1.length()-1));
//						}
//						
//					}
					
					Assert.assertEquals("UTF8-LMBCS-UTF8 conversion produces strings with same length (used Memory object for LMBCS)", randomStr.length(), randomStrLMBCSAsStr1.length());
					Assert.assertTrue("UTF8-LMBCS-UTF8 conversion does not change the text (used Memory object for LMBCS)", randomStr.equals(randomStrLMBCSAsStr1));

					String randomStrLMBCSAsStr2 = NotesStringUtils.fromLMBCS(randomStrLMBCS, randomStrLMBCS.size());
					
					Assert.assertEquals("UTF8-LMBCS-UTF8 conversion produces strings with same length (used Pointer for LMBCS)", randomStr.length(), randomStrLMBCSAsStr2.length());
					Assert.assertTrue("UTF8-LMBCS-UTF8 conversion does not change the text (used Pointer for LMBCS)", randomStr.equals(randomStrLMBCSAsStr2));
				}
				System.out.println("Done with LMBCS/UTF8 roundtrip test");
				
				return null;
			}
		});
	
	}
	
	/**
	 * Tests reading the replication info from a database and conversion functions between
	 * innards arrays and hex strings
	 */
	@Test
	public void testStringUtils_replicaIdInnards() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting replica info get and conversion test");
				
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				NotesDbReplicaInfo replInfoJNA = dbData.getReplicaInfo();
				String replicaIdJNA = replInfoJNA.getReplicaID();
				System.out.println("Replica id of fakenames db: "+replicaIdJNA);
				String replicaIdLegacy = dbLegacyAPI.getReplicaID();
				
				Assert.assertEquals("Replica id matches between JNA and legacy API", replicaIdJNA, replicaIdLegacy);
				
				int[] innards = NotesStringUtils.replicaIdToInnards(replicaIdJNA);
				String convertedReplicaId = NotesStringUtils.innardsToReplicaId(innards);
				
				Assert.assertEquals("Conversion functions between replica id and innards are ok", replicaIdJNA, convertedReplicaId);
				
				NotesTimeDate replicaIdAsDate = replInfoJNA.getReplicaIDAsDate();
				
				Assert.assertTrue("Innards are equal after conversion", Arrays.equals(replicaIdAsDate==null ? null : replicaIdAsDate.getInnards(), innards));
				
				System.out.println("Done with replica info get and conversion test");
				return null;
			}
		});
	}

	/**
	 * Tests whether the parsing of a hex encoded UNID to two {@link NotesUniversalNoteId} and
	 * the formatting back to a string roundtrip.
	 */
	@Test
	public void testStringUtils_unidFormattingAndParsing() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				String origUnid = "DF8ADEA0A485F3E34825718E00585666";
				NotesUniversalNoteIdStruct unidObj = NotesUniversalNoteIdStruct.fromString(origUnid);
				String formattedUnid = unidObj.toString();
				Assert.assertEquals("Parsing and formatting UNID returns the same result", origUnid, formattedUnid);

				return null;
			}
		});
		
	}
}
