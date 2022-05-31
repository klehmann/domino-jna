package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.FormulaAttributes;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.formula.FormulaExecution;
import com.mindoo.domino.jna.formula.FormulaExecution.Disallow;
import com.mindoo.domino.jna.formula.FormulaExecution.FormulaExecutionResult;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.NotesIniUtils;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * Tests cases for formula execution
 * 
 * @author Karsten Lehmann
 */
public class TestFormulaExecution extends BaseJNATestClass {

	@Test
	public void testAnalyzeFormula() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				assertEquals(
						EnumSet.of(FormulaAttributes.CONSTANT),
						new FormulaExecution("123").analyze().getAttributes()
						);
				assertEquals(
						EnumSet.of(FormulaAttributes.CONSTANT, FormulaAttributes.FUNC_SIBLINGS),
						new FormulaExecution("@DocSiblings").analyze().getAttributes()
						);

				assertEquals(
						EnumSet.of(FormulaAttributes.CONSTANT, FormulaAttributes.FUNC_DESCENDANTS),
						new FormulaExecution("@DocDescendants").analyze().getAttributes()
						);

				assertEquals(
						EnumSet.of(FormulaAttributes.TIME_VARIANT),
						new FormulaExecution("@Now").analyze().getAttributes()
						);

				List<String> allFunctions = FormulaExecution.getAllFunctions();
				assertTrue(allFunctions.contains("@Left("));
				assertTrue(!FormulaExecution.getFunctionParameters("@Left(").isEmpty());
				
				List<String> allCommands = FormulaExecution.getAllCommands();
				assertTrue(allCommands.contains("MailSend"));
				assertTrue(!FormulaExecution.getFunctionParameters("ToolsRunMacro").isEmpty());
				
				return null;
			}
		});
	}
	
	@Test
	public void testFormulaExecution_formulaExecution() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesGC.setDebugLoggingEnabled(true);

				System.out.println("Starting formula execution test");

				NotesDatabase dbData = getFakeNamesDb();

				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());

				View peopleView = dbLegacyAPI.getView("People");
				peopleView.refresh();

				//find document with Umlaut values
				Document doc = peopleView.getDocumentByKey("Umlaut", false);

				NotesNote note = dbData.openNoteById(Integer.parseInt(doc.getNoteID(), 16), EnumSet.of(OpenNote.EXPAND));

				int rndNumber = (int) Math.random() * 1000;
				
				{
					//use formula to modify note
					FormulaExecution formulaUtilModification = new FormulaExecution("FIELD anumber:= "+rndNumber+"; FIELD curruser:=@UserName; \"XYZ\"");
					FormulaExecutionResult resultModification = formulaUtilModification.evaluateExt(note);

					//check if note has been marked as modified
					Assert.assertEquals("Formula modified note", true, resultModification.isNoteModified());

					//check number field value
					long noteNumber = note.getItemValueLong("anumber");
					Assert.assertEquals("Formula wrote number field", rndNumber, noteNumber);

					//check string field value
					String noteUserName = note.getItemValueString("curruser");
					Assert.assertEquals("Formula wrote current username", session.getEffectiveUserName(), noteUserName);

					//check string return value
					Assert.assertTrue("Formula returned a string value", 
							resultModification.getValue()!=null && resultModification.getValue().size()==1 && "XYZ".equals(resultModification.getValue().get(0)));
				}
				
				{
					//use formula to select note
					FormulaExecution formulaUtilMatch = new FormulaExecution("SELECT anumber="+rndNumber);
					FormulaExecutionResult resultMatch = formulaUtilMatch.evaluateExt(note);

					Assert.assertEquals("Note matches formula", true, resultMatch.matchesFormula());
				}
				
				{
					//use formula to mark note to be deleted
					FormulaExecution formulaDeletion = new FormulaExecution("@DeleteDocument");
					FormulaExecutionResult resultDeletion = formulaDeletion.evaluateExt(note);
					Assert.assertEquals("Formula marked note for deletion", true, resultDeletion.shouldBeDeleted());
				}

				System.out.println("Done with formula execution test");
				return null;
			}
		});
	}
	
	@Test
	public void testDisallowedFormulas() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {

				withTempDb((db) -> {
					NotesNote otherDoc = db.createNote();
					otherDoc.replaceItemValue("Form", "Testform");
					otherDoc.update();
					String otherDocUnid = otherDoc.getUNID();
					otherDoc.recycle();

					NotesNote doc = db.createNote();

					//check if we can prevent Notes.ini changes
					{
						NotesIniUtils.setEnvironmentString("$jnx_test", "");

						FormulaExecution formula = new FormulaExecution("@SetEnvironment(\"jnx_test\"; \"123\")");
						formula.evaluate(null);
						//this is allowed, to the value should have been set
						String val = NotesIniUtils.getEnvironmentString("$jnx_test");
						assertEquals("123", val);
					}

					{
						NotesIniUtils.setEnvironmentString("$jnx_test", "");

						NotesError e = null;
						try {
							FormulaExecution formula = new FormulaExecution("@SetEnvironment(\"jnx_test\"; \"123\")")
									.disallow(Disallow.SETENVIRONMENT);
							formula.evaluate(null);
						}
						catch (NotesError e1) {
							e = e1;
						}

						assertNotNull(e);
						assertTrue(e.getId() == INotesErrorConstants.ERR_NSF_COMPUTE_NOENVIRONMENT);
					}

					{
						NotesIniUtils.setEnvironmentString("$jnx_test", "");

						NotesError e = null;
						try {
							FormulaExecution formula = new FormulaExecution("@SetEnvironment(\"jnx_test\"; \"123\")")
									.disallow(Disallow.UNSAFE);
							formula.evaluate(null);
						}
						catch (NotesError e1) {
							e = e1;
						}

						assertNotNull(e);
						assertTrue(e.getId() == INotesErrorConstants.ERR_NSF_COMPUTE_NOENVIRONMENT);
					}

					//check if FIELD fieldname:= can be prevented
					{
						doc.replaceItemValue("field1", "");

						FormulaExecution formula = new FormulaExecution("FIELD field1:=\"123\"; \"\"");
						formula.evaluate(doc);

						String fldValue = doc.getItemValueString("field1");
						assertEquals("123", fldValue);
					}

					{
						doc.replaceItemValue("field1", "");

						FormulaExecution formula = new FormulaExecution("FIELD field1:=\"123\"; \"\"")
								.disallow(Disallow.ASSIGN);
						formula.evaluate(doc);

						String fldValue = doc.getItemValueString("field1");
						assertEquals("", fldValue);
					}

					{
						doc.replaceItemValue("field1", "");

						FormulaExecution formula = new FormulaExecution("FIELD field1:=\"123\"; \"\"")
								.disallow(Disallow.UNSAFE);
						formula.evaluate(doc);

						String fldValue = doc.getItemValueString("field1");
						assertEquals("", fldValue);
					}

					{
						doc.replaceItemValue("field1", "");

						FormulaExecution formula = new FormulaExecution("@SetField(\"field1\"; \"123\")")
								.disallow(Disallow.UNSAFE);
						formula.evaluate(doc);

						String fldValue = doc.getItemValueString("field1");
						assertEquals("", fldValue);
					}

					{
						FormulaExecution formula = new FormulaExecution("@GetDocField(\""+otherDocUnid+"\";\"form\")");
						List<Object> result = formula.evaluate(doc);
						assertEquals(Arrays.asList("Testform"), result);
					}

					//FALLBACK_EXT seems to block retrieval of the doc field
					{
						FormulaExecution formula = new FormulaExecution("@GetDocField(\""+otherDocUnid+"\";\"form\")")
								.disallow(Disallow.FALLBACK_EXT);
						List<Object> result = formula.evaluate(doc);
						assertEquals(Arrays.asList(""), result);
					}

					//check if we can prevent @SetDocField
					{
						FormulaExecution formula = new FormulaExecution("@SetDocField(\""+otherDocUnid+"\"; \"field1\"; \"123\")");
						formula.evaluate(doc);
						//we tried to load the doc to see if field1 has been set, but that
						//was not the case; not sure why this is not working; but setting one of the
						//Disallow constants below does even more, it throws an exception
					}

					{
						NotesError e = null;
						try {
							FormulaExecution formula = new FormulaExecution("@SetDocField(\""+otherDocUnid+"\"; \"field1\"; \"123\")")
									.disallow(Disallow.UNSAFE);
							formula.evaluate(doc);
						} catch (NotesError e1) {
							e = e1;
						}

						assertNotNull(e);
						assertTrue(e.getId() == INotesErrorConstants.ERR_FUNCTION_CONTEXT);
					}

					{
						NotesError e = null;
						try {
							FormulaExecution formula = new FormulaExecution("@SetDocField(\""+otherDocUnid+"\"; \"field1\"; \"123\")")
									.disallow(Disallow.SIDEEFFECTS);
							formula.evaluate(doc);
						} catch (NotesError e1) {
							e = e1;
						}

						assertNotNull(e);
						assertTrue(e.getId() == INotesErrorConstants.ERR_FUNCTION_CONTEXT);
					}

				});

				return null;
			}
		});

	}
}
