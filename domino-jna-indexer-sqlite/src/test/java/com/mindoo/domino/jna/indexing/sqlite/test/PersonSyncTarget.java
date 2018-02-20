package com.mindoo.domino.jna.indexing.sqlite.test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.indexing.sqlite.AbstractSQLiteSyncTarget;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.sync.NotesOriginatorIdData;

public class PersonSyncTarget extends AbstractSQLiteSyncTarget {

	public PersonSyncTarget(String jdbcSqliteUrl) {
		super(jdbcSqliteUrl);
	}

	@Override
	public Map<String, String> getSummaryBufferItemsAndFormulas() {
		//only read the following items:
		Map<String,String> map = new LinkedHashMap<String,String>();
		map.put("companyname", "");
		map.put("fullname", "");
		map.put("lastname", "");
		map.put("firstname", "");
		map.put("form", "");
		//add this special item combined with DataToRead.SummaryBufferSelectedItems to let
		//Domino return the readers and authors list of a note as special item "$C1$" in the summary buffer,
		//in case the note has any reader item
		map.put("$C1$", "");
		return map;
	}

	@Override
	public EnumSet<DataToRead> getWhichDataToRead() {
//		return EnumSet.of(DataToRead.NoteWithAllItems);
//		return EnumSet.of(DataToRead.NoteWithSummaryItems);
//		return EnumSet.of(DataToRead.SummaryBufferAllItems);
		return EnumSet.of(DataToRead.SummaryBufferSelectedItems);
	}

	@Override
	protected String toJson(NotesOriginatorIdData oid, ItemTableData summaryBufferData, NotesNote note) {
		String companyName;
		String fullName;
		String lastName;
		String firstName;
		
		if (note!=null) {
			companyName = note.getItemValueString("CompanyName");
			fullName = note.getItemValueString("FullName");
			lastName = note.getItemValueString("Lastname");
			firstName = note.getItemValueString("Firstname");
		}
		else {
			companyName = summaryBufferData.getAsString("CompanyName", "");
			fullName = summaryBufferData.getAsString("FullName", "");
			lastName = summaryBufferData.getAsString("Lastname", "");
			firstName = summaryBufferData.getAsString("Firstname", "");
		}
		
		JSONObject json = new JSONObject();
		json.put("company", companyName);
		json.put("fullname", fullName);
		json.put("firstname", firstName);
		json.put("lastname", lastName);
		
		return json.toString();
	}

}
