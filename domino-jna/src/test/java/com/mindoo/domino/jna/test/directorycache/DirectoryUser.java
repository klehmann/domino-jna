package com.mindoo.domino.jna.test.directorycache;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class DirectoryUser {
	private String m_id;
	private String m_salutation;
	private String m_firstName;
	private String m_lastName;
	private String m_email;
	private String m_street;
	private String m_zip;
	private String m_city;
	private String m_phone;
	private String m_mobile;
	private String m_fullName;
	
	private List<String> m_lookupNames;
	
	public DirectoryUser() {
	}
	
	public String getId() {
		return m_id;
	}
	
	public DirectoryUser setId(String newId) {
		m_id = newId;
		return this;
	}
	
	public String getFirstName() {
		return m_firstName;
	}
	
	public DirectoryUser setFirstName(String newFirstName) {
		m_firstName = newFirstName;
		return this;
	}
	
	public String getLastName() {
		return m_lastName;
	}
	
	public DirectoryUser setLastName(String newLastName) {
		m_lastName = newLastName;
		return this;
	}
	
	public String getEmail() {
		return m_email;
	}
	
	public DirectoryUser setEmail(String newEmail) {
		m_email = newEmail;
		return this;
	}

	public String getZip() {
		return m_zip;
	}

	public DirectoryUser setZip(String newZip) {
		m_zip = newZip;
		return this;
	}

	public String getCity() {
		return m_city;
	}

	public DirectoryUser setCity(String newCity) {
		m_city = newCity;
		return this;
	}

	public String getPhone() {
		return m_phone;
	}

	public DirectoryUser setPhone(String newPhone) {
		m_phone = newPhone;
		return this;
	}

	public String getMobile() {
		return m_mobile;
	}

	public DirectoryUser setMobile(String newMobile) {
		m_mobile = newMobile;
		return this;
	}

	public String getStreet() {
		return m_street;
	}

	public DirectoryUser setStreet(String newStreet) {
		m_street = newStreet;
		return this;
	}

	public String getSalutation() {
		return m_salutation;
	}

	public String getFullName() {
		return m_fullName;
	}
	
	public DirectoryUser setSalutation(String newSalutation) {
		m_salutation = newSalutation;
		return this;
	}

	public DirectoryUser setFullName(String newFullName) {
		m_fullName = newFullName;
		return this;
	}

	public List<String> getLookupNames() {
		return m_lookupNames;
	}
	
	public DirectoryUser setLookupNames(List<String> lookupNames) {
		m_lookupNames = lookupNames;
		return this;
	}
	
	public static JSONObject toJson(DirectoryUser user) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", user.getId());
		json.put("salutation", user.getSalutation());
		json.put("firstname", user.getFirstName());
		json.put("lastname", user.getLastName());
		json.put("street", user.getStreet());
		json.put("zip", user.getZip());
		json.put("city", user.getCity());
		json.put("email", user.getEmail());
		json.put("phone", user.getPhone());
		json.put("mobile", user.getMobile());
		
		return json;
	}
}
