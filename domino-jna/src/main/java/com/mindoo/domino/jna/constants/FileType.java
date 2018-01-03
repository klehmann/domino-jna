package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.directory.DirectoryScanner;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * File type flags (used with NSFSearch directory searching, e.g. in {@link DirectoryScanner})
 * 
 * @author Karsten Lehmann
 */
public enum FileType {
	
	/** Any file type */
	ANY(NotesConstants.FILE_ANY),
	
	/** Starting in V3, any DB that is a candidate for replication */
	DBREPL(NotesConstants.FILE_DBREPL),
	
	/** Databases that can be templates */
	DBDESIGN(NotesConstants.FILE_DBDESIGN),
	
	/** BOX - Any .BOX (Mail.BOX, SMTP.Box...) */
	MAILBOX(NotesConstants.FILE_MAILBOX),
	
	/** NS?, any NSF version */
	DBANY(NotesConstants.FILE_DBANY),
	
	/** NT?, any NTF version */
	FTANY(NotesConstants.FILE_FTANY),
	
	/** MDM - modem command file */
	MDMTYPE(NotesConstants.FILE_MDMTYPE),
	
	/** directories only */
	DIRSONLY(NotesConstants.FILE_DIRSONLY),
	
	/** VPC - virtual port command file */
	VPCTYPE(NotesConstants.FILE_VPCTYPE),
	
	/** SCR - comm port script files */
	SCRTYPE(NotesConstants.FILE_SCRTYPE),
	
	/** ANY Notes database (.NS?, .NT?, .BOX)	*/
	ANYNOTEFILE(NotesConstants.FILE_ANYNOTEFILE),
	
	/** DTF - Any .DTF. Used for container and sort temp files to give them a more
	   unique name than .TMP so we can delete *.DTF from the temp directory and
	   hopefully not blow away other application's temp files. */
	UNIQUETEMP(NotesConstants.FILE_UNIQUETEMP),
	
	/** CLN - Any .cln file...multi user cleanup files*/
	MULTICLN(NotesConstants.FILE_MULTICLN),
	
	/** any smarticon file *.smi */
	SMARTI(NotesConstants.FILE_SMARTI),
	
	/** File type mask (for FILE_xxx codes above) */
	TYPEMASK(NotesConstants.FILE_TYPEMASK),
	
	/** List subdirectories as well as normal files */
	DIRS(NotesConstants.FILE_DIRS),
	
	/** Do NOT return ..'s */
	NOUPDIRS(NotesConstants.FILE_NOUPDIRS),
	
	/** Recurse into subdirectories */
	RECURSE(NotesConstants.FILE_RECURSE),
	
	/** All directories, linked files &amp; directories */
	LINKSONLY(NotesConstants.FILE_LINKSONLY);

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
