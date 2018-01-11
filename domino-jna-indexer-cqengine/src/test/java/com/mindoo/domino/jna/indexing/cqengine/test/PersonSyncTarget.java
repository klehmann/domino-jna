package com.mindoo.domino.jna.indexing.cqengine.test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.indexing.cqengine.AbstractCQEngineSyncTarget;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.sync.ISyncTarget;
import com.mindoo.domino.jna.sync.NotesOriginatorIdData;

/**
 * Subclass of {@link AbstractCQEngineSyncTarget} that produces an indexed collection
 * of {@link Person} objects.
 * 
 * @author Karsten Lehmann
 */
public class PersonSyncTarget extends AbstractCQEngineSyncTarget<Person> implements ISyncTarget {
	//our attributes we want to use for querying data
	public static final Attribute<Person, String> PERSON_COMPANY = new SimpleAttribute<Person, String>("company") {
		public String getValue(Person person, QueryOptions queryOptions) {
			return person.getCompanyName();
		}
	};

	public static final Attribute<Person, String> PERSON_FULLNAME = new SimpleAttribute<Person, String>("fullname") {
		public String getValue(Person person, QueryOptions queryOptions) {
			return person.getFullName();
		}
	};

	public static final Attribute<Person, String> PERSON_LASTNAME = new SimpleAttribute<Person, String>("lastname") {
		public String getValue(Person person, QueryOptions queryOptions) {
			return person.getLastName();
		}
	};

	public static final Attribute<Person, String> PERSON_FIRSTNAME = new SimpleAttribute<Person, String>("firstname") {
		public String getValue(Person person, QueryOptions queryOptions) {
			return person.getFirstName();
		}
	};

	@Override
	protected IndexedCollection<Person> createCollection() {
		IndexedCollection<Person> persons = new ConcurrentIndexedCollection<Person>();
		return persons;
	}

	@Override
	public Map<String, String> getSummaryBufferItemsAndFormulas() {
		//only read the following items:
		Map<String,String> map = new LinkedHashMap<String,String>();
		map.put("companyname", "");
		map.put("fullname", "");
		map.put("lastname", "");
		map.put("firstname", "");
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
	protected Person toObject(NotesOriginatorIdData oid, ItemTableData summaryBufferData, NotesNote note) {
		String unid = oid.getUNID();
		int seq = oid.getSequence();
		NotesTimeDate seqTime = oid.getSequenceTime();
		
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
		
		Person p = new Person(unid, seq, seqTime,
				companyName, fullName, lastName, firstName);
		return p;
	}
}
