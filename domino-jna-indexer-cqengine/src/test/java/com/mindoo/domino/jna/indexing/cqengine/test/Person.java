package com.mindoo.domino.jna.indexing.cqengine.test;

import java.util.concurrent.atomic.AtomicLong;

import com.mindoo.domino.jna.indexing.cqengine.BaseIndexObject;

public class Person extends BaseIndexObject {
	//computing unique version according to this javadoc: http://htmlpreview.github.io/?http://raw.githubusercontent.com/npgall/cqengine/master/documentation/javadoc/apidocs/com/googlecode/cqengine/TransactionalIndexedCollection.html
	static final AtomicLong VERSION_GENERATOR = new AtomicLong();
	final long version = VERSION_GENERATOR.incrementAndGet();

	private String m_companyName;
	private String m_lastName;
	private String m_firstName;
	private String m_fullName;
	
	public Person(String unid, int sequence, int[] seqTimeInnards,
			String companyName, String fullName, String lastName, String firstName) {
		super(unid, sequence, seqTimeInnards);
		
		m_companyName = companyName;
		m_fullName = fullName;
		m_lastName = lastName;
		m_firstName = firstName;
	}
	
	public String getCompanyName() {
		return m_companyName;
	}
	
	public String getFullName() {
		return m_fullName;
	}
	
	public String getLastName() {
		return m_lastName;
	}
	
	public String getFirstName() {
		return m_firstName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_firstName == null) ? 0 : m_firstName.hashCode());
		result = prime * result + ((m_lastName == null) ? 0 : m_lastName.hashCode());
		result = prime * result + getSequence();
		result = prime * result + ((getSequenceTimeInnards() == null) ? 0 : getSequenceTimeInnards().hashCode());
		result = prime * result + ((getUNID() == null) ? 0 : getUNID().hashCode());
		result = prime * result + (int) (version ^ (version >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		Person other = (Person) obj;
		if (getSequence() != other.getSequence())
			return false;
		
		if (getSequenceTimeInnards() == null) {
			if (other.getSequenceTimeInnards() != null)
				return false;
		} else if (!getSequenceTimeInnards().equals(other.getSequenceTimeInnards()))
			return false;
		
		if (getUNID() == null) {
			if (other.getUNID() != null)
				return false;
		} else if (!getUNID().equals(other.getUNID()))
			return false;
		
		if (version != other.version)
			return false;

		if (m_firstName == null) {
			if (other.m_firstName != null)
				return false;
		} else if (!m_firstName.equals(other.m_firstName))
			return false;
		
		if (m_lastName == null) {
			if (other.m_lastName != null)
				return false;
		} else if (!m_lastName.equals(other.m_lastName))
			return false;
		
		if (m_companyName == null) {
			if (other.m_companyName != null)
				return false;
		} else if (!m_companyName.equals(other.m_companyName))
			return false;

		if (m_fullName == null) {
			if (other.m_fullName != null)
				return false;
		} else if (!m_fullName.equals(other.m_fullName))
			return false;

		return true;
	}
	
	@Override
	public String toString() {
		return "Person [unid="+getUNID()+", seq="+getSequence()+", seqtime="+getSequenceTimeInnards()+", lastname="+getLastName()+", firstname="+getFirstName()+"]";
	}
	
}
