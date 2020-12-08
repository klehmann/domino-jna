package com.mindoo.domino.jna.internal;

import java.util.EnumSet;
import java.util.Formatter;

import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesSearch.SearchCallback.NoteFlags;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.sync.NotesOriginatorIdData;
import com.sun.jna.Pointer;

/**
 * Utility class to decode the SEARCH_MATCH structure and copy its values
 * into a {@link ISearchMatch}.
 * 
 * @author Karsten Lehmann
 */
public class SearchMatchDecoder {

	/**
	 * Decodes the SEARCH_MATCH structure starting at the specified memory address
	 * 
	 * @param ptr memory pointer
	 * @return object with search match data
	 */
	public static NotesSearch.ISearchMatch decodeSearchMatch(Pointer ptr) {
		int gid_file_innards0 = ptr.getInt(0);
		int gid_file_innards1 = ptr.getInt(4);
		
		int gid_note_innards0 = ptr.getInt(8);
		int gid_note_innards1 = ptr.getInt(12);
		
		int noteId = ptr.getInt(16);
		
		int oid_file_innards0 = ptr.getInt(20);
		int oid_file_innards1 = ptr.getInt(24);
		
		int oid_note_innards0 = ptr.getInt(28);
		int oid_note_innards1 = ptr.getInt(32);
		int seq = ptr.getInt(36);
		int seqTimeInnards0 = ptr.getInt(40);
		int seqTimeInnards1 = ptr.getInt(44);
		
		short noteClass = ptr.getShort(48);
		byte seRetFlags = ptr.getByte(50);
		byte privileges = ptr.getByte(51);
		short summaryLength = ptr.getShort(52);
		
		SearchMatchImpl match = new SearchMatchImpl();
		match.setGIDFileInnards(new int[] {gid_file_innards0, gid_file_innards1});
		match.setGIDNoteInnards(new int[] {gid_note_innards0, gid_note_innards1});
		match.setNoteId(noteId);
		match.setOIDFileInnards(new int[] {oid_file_innards0, oid_file_innards1});
		match.setOIDNoteInnards(new int[] {oid_note_innards0, oid_note_innards1});
		match.setSeq(seq);
		match.setSeqTimeInnards(new int[] {seqTimeInnards0, seqTimeInnards1});
		match.setNoteClass(noteClass);
		match.setSeRetFlags(seRetFlags);
		match.setPrivileges(privileges);
		match.setSummaryLength(summaryLength);
		return match;
	}
	
	private static class SearchMatchImpl implements NotesSearch.ISearchMatch {
		//global instance id
		private int[] gid_file_innards;
		private int[] gid_note_innards;
		private int noteId;
		
		//originator id
		private int[] oid_file_innards;
		private int[] oid_note_innards;
		private int seq;
		private int[] seqTimeInnards;
		
		//other data
		private short noteClass;
		private byte seRetFlags;
		private byte privileges;
		private short summaryLength;
		
		private NotesTimeDate m_dbCreated;
		private NotesTimeDate m_nodeModified;
		private NotesTimeDate m_seqTime;
		private NotesOriginatorIdData m_oidData;
		
		private EnumSet<NoteClass> noteClassAsEnum;
		private EnumSet<NoteFlags> m_flagsAsEnum;
		private String m_unid;
		
		@Override
		public int[] getGIDFileInnards() {
			return gid_file_innards;
		}
		
		void setGIDFileInnards(int[] gid_file_innards) {
			this.gid_file_innards = gid_file_innards;
		}
		
		@Override
		public int[] getGIDNoteInnards() {
			return gid_note_innards;
		}
		
		void setGIDNoteInnards(int[] gid_note_innards) {
			this.gid_note_innards = gid_note_innards;
		}
		
		@Override
		public int getNoteId() {
			return noteId;
		}
		
		void setNoteId(int noteId) {
			this.noteId = noteId;
		}
		
		@Override
		public int[] getOIDFileInnards() {
			return oid_file_innards;
		}
		
		void setOIDFileInnards(int[] oid_file_innards) {
			this.oid_file_innards = oid_file_innards;
		}
		
		@Override
		public int[] getOIDNoteInnards() {
			return oid_note_innards;
		}
		
		void setOIDNoteInnards(int[] oid_note_innards) {
			this.oid_note_innards = oid_note_innards;
		}
		
		@Override
		public int getSeq() {
			return seq;
		}
		
		void setSeq(int seq) {
			this.seq = seq;
		}
		
		@Override
		public int[] getSeqTimeInnards() {
			return seqTimeInnards;
		}
		
		void setSeqTimeInnards(int[] seqTimeInnards) {
			this.seqTimeInnards = seqTimeInnards;
		}
		
		@Override
		public EnumSet<NoteClass> getNoteClass() {
			if (noteClassAsEnum==null) {
				noteClassAsEnum = NoteClass.toNoteClasses(noteClass);
			}
			return noteClassAsEnum;
		}
		
		void setNoteClass(short noteClass) {
			this.noteClass = noteClass;
		}
		
		void setSeRetFlags(byte seRetFlags) {
			this.seRetFlags = seRetFlags;
		}
		
		void setPrivileges(byte privileges) {
			this.privileges = privileges;
		}
		
		@Override
		public int getSummaryLength() {
			return (int) (summaryLength &0xffff);
		}
		
		void setSummaryLength(short summaryLength) {
			this.summaryLength = summaryLength;
		}

		private static EnumSet<NoteFlags> toNoteFlags(byte flagsAsByte) {
			EnumSet<NoteFlags> flags = EnumSet.noneOf(NoteFlags.class);
			boolean isTruncated = (flagsAsByte & NotesConstants.SE_FTRUNCATED) == NotesConstants.SE_FTRUNCATED;
			if (isTruncated)
				flags.add(NoteFlags.Truncated);
			boolean isNoAccess = (flagsAsByte & NotesConstants.SE_FNOACCESS) == NotesConstants.SE_FNOACCESS;
			if (isNoAccess)
				flags.add(NoteFlags.NoAccess);
			boolean isTruncatedAttachment = (flagsAsByte & NotesConstants.SE_FTRUNCATT) == NotesConstants.SE_FTRUNCATT;
			if (isTruncatedAttachment)
				flags.add(NoteFlags.TruncatedAttachments);
			boolean isNoPurgeStatus = (flagsAsByte & NotesConstants.SE_FNOPURGE) == NotesConstants.SE_FNOPURGE;
			if (isNoPurgeStatus)
				flags.add(NoteFlags.NoPurgeStatus);
			boolean isPurged = (flagsAsByte & NotesConstants.SE_FPURGED) == NotesConstants.SE_FPURGED;
			if (isPurged)
				flags.add(NoteFlags.Purged);
			boolean isMatch = (flagsAsByte & NotesConstants.SE_FMATCH) == NotesConstants.SE_FMATCH;
			if (isMatch)
				flags.add(NoteFlags.Match);
			else
				flags.add(NoteFlags.NoMatch);
			boolean isSoftDeleted = (flagsAsByte & NotesConstants.SE_FSOFTDELETED) == NotesConstants.SE_FSOFTDELETED;
			if (isSoftDeleted)
				flags.add(NoteFlags.SoftDeleted);
			
			return flags;
		}
		
		@Override
		public EnumSet<NoteFlags> getFlags() {
			if (m_flagsAsEnum==null) {
				m_flagsAsEnum = toNoteFlags(this.seRetFlags);
			}
			return m_flagsAsEnum;
		}

		@Override
		public boolean matchesFormula() {
			//use flags byte directly, quicker than creating the EnumSet first
			return ((this.seRetFlags & NotesConstants.SE_FMATCH) == NotesConstants.SE_FMATCH);
		}
		
		@Override
		public boolean isLargeSummary() {
			return ((this.seRetFlags & NotesConstants.SE_FLARGESUMMARY) == NotesConstants.SE_FLARGESUMMARY);
		}
		
		@Override
		public NotesTimeDate getSeqTime() {
			if (m_seqTime==null) {
				m_seqTime = new NotesTimeDate(getSeqTimeInnards());
			}
			return m_seqTime;
		}

		@Override
		public NotesOriginatorIdData getOIDData() {
			if (m_oidData==null) {
				m_oidData = new NotesOriginatorIdData(getUNID(), getSeq(), getSeqTimeInnards());
			}
			return m_oidData;
		}

		@Override
		public String getUNID() {
			if (m_unid==null) {
				Formatter formatter = new Formatter();
				formatter.format("%08x", this.oid_file_innards[1]);
				formatter.format("%08x", this.oid_file_innards[0]);
				
				formatter.format("%08x", this.oid_note_innards[1]);
				formatter.format("%08x", this.oid_note_innards[0]);
				
				m_unid = formatter.toString().toUpperCase();
				formatter.close();
			}
			return m_unid;
		}

		@Override
		public NotesTimeDate getDbCreated() {
			if (m_dbCreated==null) {
				m_dbCreated = new NotesTimeDate(getGIDFileInnards());
			}
			return m_dbCreated;
		}

		@Override
		public NotesTimeDate getNoteModified() {
			if (m_nodeModified==null) {
				m_nodeModified = new NotesTimeDate(getGIDNoteInnards());
			}
			return m_nodeModified;
		}
		
		@Override
		public String toString() {
			return "SearchMatch [unid="+getUNID()+", seq="+getSeq()+", seqtime="+getSeqTime()+
					", noteid="+getNoteId()+", class="+getNoteClass()+",flags="+getFlags()+", modified="+getNoteModified()+"]";
			
		}
	}
}
