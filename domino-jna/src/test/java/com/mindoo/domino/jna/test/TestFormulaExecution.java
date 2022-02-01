package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.FormulaAttributes;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.formula.FormulaExecution;
import com.mindoo.domino.jna.formula.FormulaExecution.FormulaExecutionResult;
import com.mindoo.domino.jna.gc.NotesGC;

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
	public void testFormulaAnalyze() {
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
	
//	@Test
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
}
