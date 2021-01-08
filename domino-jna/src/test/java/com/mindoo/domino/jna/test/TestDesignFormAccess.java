package com.mindoo.domino.jna.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.design.NotesForm;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.richtext.FieldInfo;

import lotus.domino.Database;
import lotus.domino.Form;
import lotus.domino.Session;

public class TestDesignFormAccess extends BaseJNATestClass {

	@Test
	public void testFormAPI() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesGC.setDebugLoggingEnabled(false);
				
				AtomicInteger cnt = new AtomicInteger();
				
				NotesDatabase dbMail = NotesDatabase.openMailDatabase();
				if (dbMail==null) {
					//test only working in the Notes Client, because openMailDatabase reads the mail db location from
					//the Notes.ini
					return null;
				}
				System.out.println("All forms and subforms of database "+dbMail.getServer()+"!!"+dbMail.getRelativeFilePath());
				
				System.out.println("Read via JNA:");
				
				List<NotesForm> jnaForms = new ArrayList<>();
				
				dbMail
				.getForms().forEach((form) -> {
					int currCnt = cnt.incrementAndGet();
					System.out.println("#"+currCnt+" - name="+form);
					
					jnaForms.add(form);
				});
				
				cnt.set(0);
				
				System.out.println("Read via Notes.jar:");
				
				Database db = session.getDatabase(dbMail.getServer(), dbMail.getRelativeFilePath(), false);
				@SuppressWarnings("unchecked")
				Vector<Form> legacyForms = db.getForms();
				for (Form currForm : legacyForms) {
					int currCnt = cnt.incrementAndGet();
					System.out.println("#"+currCnt+" - name="+currForm.getName()+", aliases="+currForm.getAliases()+", issubform="+currForm.isSubForm());
				}
				
				Assert.assertEquals(jnaForms.size(), legacyForms.size());
				
				for (int i=0; i<jnaForms.size(); i++) {
					NotesForm jnaForm = jnaForms.get(i);
					Form legacyForm = legacyForms.get(i);
					
					Assert.assertEquals(jnaForm.getName(), legacyForm.getName());
					Assert.assertEquals(jnaForm.getAliases(), legacyForm.getAliases());
					Assert.assertEquals(jnaForm.isSubForm(), legacyForm.isSubForm());
					Assert.assertEquals(jnaForm.isProtectReaders(), legacyForm.isProtectReaders());
					Assert.assertEquals(jnaForm.isProtectUsers(), legacyForm.isProtectUsers());
					Assert.assertEquals(jnaForm.getFormUsers(), legacyForm.getFormUsers());
					Assert.assertEquals(jnaForm.getReaders(), legacyForm.getReaders());
					
					//please note that we do not compare Notes.jar's Form.getFields()
					//Domino JNA's and NotesForm.getFields()
					//because the field list returned by the Notes.jar is not correct.
					//
					//Notes.jar uses the $Fields field to read the field names while we
					//traverse the $Body design richtext.
					//
					//We found a lot of cases in the mail template where $Fields is not up-to-date, e.g.:
					//
					//form "CalendarInfoDoc":
					//field "$ICAL_CALENDARINFODOC" exists in the form, but is missing in $fields
					//
					//form "$$ViewTemplate for Calendar":
					//form "$$ViewTemplate for Meetings":
					//form "$$ViewTemplate for People":
					//form "$$ViewTemplate for Rules":
					//form "$$ViewTemplate for Tasks":
					//form "$$ViewTemplate for TodoByCategory":
					//form "$$ViewTemplate for TodoByStatus":
					//form "$$ViewTemplate for TodoCompleted":
					//form "$$ViewTemplate for ToDoGroup":
					//form "$$ViewTemplate for TodoPersonal":
					//form "$$ViewTemplate for ($Trash)":
					//field "$$SelectDestFolder" is listed in $fields, but does not exist in the form
				}
				
				return null;
			}
		});
	}
	
}
