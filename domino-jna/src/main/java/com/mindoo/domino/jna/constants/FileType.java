package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.directory.DirectoryScanner;
import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * File type flags (used with NSFSearch directory searching, e.g. in {@link DirectoryScanner})
 * 
 * @author Karsten Lehmann
 */
public enum FileType {
	
	/** Any file type */
	ANY(NotesCAPI.FILE_ANY),
	
	/** Starting in V3, any DB that is a candidate for replication */
	DBREPL(NotesCAPI.FILE_DBREPL),
	
	/** Databases that can be templates */
	DBDESIGN(NotesCAPI.FILE_DBDESIGN),
	
	/** BOX - Any .BOX (Mail.BOX, SMTP.Box...) */
	MAILBOX(NotesCAPI.FILE_MAILBOX),
	
	/** NS?, any NSF version */
	DBANY(NotesCAPI.FILE_DBANY),
	
	/** NT?, any NTF version */
	FTANY(NotesCAPI.FILE_FTANY),
	
	/** MDM - modem command file */
	MDMTYPE(NotesCAPI.FILE_MDMTYPE),
	
	/** directories only */
	DIRSONLY(NotesCAPI.FILE_DIRSONLY),
	
	/** VPC - virtual port command file */
	VPCTYPE(NotesCAPI.FILE_VPCTYPE),
	
	/** SCR - comm port script files */
	SCRTYPE(NotesCAPI.FILE_SCRTYPE),
	
	/** ANY Notes database (.NS?, .NT?, .BOX)	*/
	ANYNOTEFILE(NotesCAPI.FILE_ANYNOTEFILE),
	
	/** DTF - Any .DTF. Used for container and sort temp files to give them a more
	   unique name than .TMP so we can delete *.DTF from the temp directory and
	   hopefully not blow away other application's temp files. */
	UNIQUETEMP(NotesCAPI.FILE_UNIQUETEMP),
	
	/** CLN - Any .cln file...multi user cleanup files*/
	MULTICLN(NotesCAPI.FILE_MULTICLN),
	
	/** any smarticon file *.smi */
	SMARTI(NotesCAPI.FILE_SMARTI),
	
	/** File type mask (for FILE_xxx codes above) */
	TYPEMASK(NotesCAPI.FILE_TYPEMASK),
	
	/** List subdirectories as well as normal files */
	DIRS(NotesCAPI.FILE_DIRS),
	
	/** Do NOT return ..'s */
	NOUPDIRS(NotesCAPI.FILE_NOUPDIRS),
	
	/** Recurse into subdirectories */
	RECURSE(NotesCAPI.FILE_RECURSE),
	
	/** All directories, linked files &amp; directories */
	LINKSONLY(NotesCAPI.FILE_LINKSONLY);

	private int m_val;

	FileType(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static EnumSet<FileType> toFileTypes(int bitMask) {
		EnumSet<FileType> set = EnumSet.noneOf(FileType.class);
		for (FileType currClass : values()) {
			if ((bitMask & currClass.getValue()) == currClass.getValue()) {
				set.add(currClass);
			}
		}
		return set;
	}
	
	public static short toBitMask(EnumSet<FileType> noteClassSet) {
		int result = 0;
		if (noteClassSet!=null) {
			for (FileType currFind : values()) {
				if (noteClassSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<FileType> noteClassSet) {
		int result = 0;
		if (noteClassSet!=null) {
			for (FileType currFind : values()) {
				if (noteClassSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
