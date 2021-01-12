package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.CWF_Action;
import com.mindoo.domino.jna.NotesNote.ComputeWithFormCallback;
import com.mindoo.domino.jna.NotesNote.ValidationPhase;
import com.mindoo.domino.jna.richtext.FieldInfo;

public class TestComputeWithForm extends BaseJNATestClass {

	@Test
	public void testComputeWithForm() throws Exception {
		runWithSession((session) -> {
			withTempDb((db) -> {
				//import a form with input validation formulas
				withImportedDXL(db, "/dxl/computewithform", (dbWithDesign) -> {

					NotesNote note = dbWithDesign.createNote();
					note.replaceItemValue("Form", "Person");

					{
						//list of error messages from @Failure formulas
						List<String> expectedErrorTexts = Arrays.asList(
								"Firstname is missing",
								"Lastname is missing"
								);

						Set<String> errorFields = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

						note.computeWithForm(false, new ComputeWithFormCallback() {

							@Override
							public CWF_Action errorRaised(FieldInfo fieldInfo, ValidationPhase phase, String errorTxt,
									long errCode) {

								errorFields.add(fieldInfo.getName());
								assertTrue(expectedErrorTexts.contains(errorTxt));
								//continue processing so that we can collect all errors
								return CWF_Action.NEXT_FIELD;
							}
						});

						//make sure we received all expected errors
						assertEquals(2, errorFields.size());
						assertTrue(errorFields.contains("firstname"));
						assertTrue(errorFields.contains("lastname"));
						assertTrue(!"".equals(note.getItemValueString("defaulttest")));
					}

					note.replaceItemValue("firstname", "John");

					{
						Set<String> errorFields = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

						note.computeWithForm(false, new ComputeWithFormCallback() {

							public CWF_Action errorRaised(FieldInfo fieldInfo, ValidationPhase phase, String errorTxt,
									long errCode) {
								errorFields.add(fieldInfo.getName());
								return CWF_Action.NEXT_FIELD;
							}
						});

						assertEquals(1, errorFields.size());
						assertTrue(errorFields.contains("lastname"));
					}

					note.replaceItemValue("lastname", "Doe");

					{
						Set<String> errorFields = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

						note.computeWithForm(false, new ComputeWithFormCallback() {

							@Override
							public CWF_Action errorRaised(FieldInfo fieldInfo, ValidationPhase phase, String errorTxt,
									long errCode) {
								errorFields.add(fieldInfo.getName());
								return CWF_Action.NEXT_FIELD;
							}
						});

						assertEquals(0, errorFields.size());
					}

				});
			});
			
			return null;
		});
		
	}
}
