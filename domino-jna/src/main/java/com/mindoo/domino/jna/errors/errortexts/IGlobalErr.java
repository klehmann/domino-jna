package com.mindoo.domino.jna.errors.errortexts;

public interface IGlobalErr {

	short PKG_MAIN = 0x0000;	/* Codes are limited to 0-63 */
	short PKG_CCONSOLE = 0x0040;	/* Codes are limited to 0-31 */
	short PKG_MAILMISC3 = 0x0060;	/* Mailmisc3 codes starting from 0-31 */
	short PKG_MINDER = 0x0080;	/* Minder limit to 0 - 95 */
	short PKG_SERVER3 = 0x00E0;	/* Codes are limited to 1 - 31 */
	short PKG_OS = 0x0100;
	short PKG_NSF = 0x0200;
	short PKG_NIF = 0x0300;	/* NIF codes are limited to 0 - 127 */
	short PKG_NSF2 = 0x0380;	/* More NSF codes - limited to 0-127*/
	short PKG_MISC = 0x0400;	/* Codes are limited to 0 - 191 */
	short PKG_SERVER2 = 0x04C0;	/* Codes are limited to 0 - 47 */
	short PKG_THUNK = 0x04F0;	/* Codes are limited to 0 - 15 */
	short PKG_FORMULA = 0x0500;	/* FORMULA codes are limited to 0-127 */
	short PKG_NSF8 = 0x0580;	/* AVAILABLE limit to 0 - 127 */
	short PKG_ODS = 0x0600;	/* ODS codes, limited to 0-47 */
	short PKG_LSXUI4 = 0x0630;	/* LotusScript Front-end classes, 0 - 47 */
	short PKG_AGENTS2 = 0x0660;	/* for agents, limited to 0-31 */
	short PKG_SCHUI = 0x0680;	/* Schedule UI codes are limited to 0 - 31 */
	short PKG_BERT = 0x06A0;	/* BERT codes are limited to 0 - 31 */
	short PKG_PLUGINS = 0x06C0;	/* plugin codes are limited to 0 - 15 */

	short PKG_IMAIL = 0x06D0;	/* IMAIL Client 0 - 47 */

	short PKG_PKIX = 0x0700;	/* codes 0 - 95 New CA/PKIX error codes - in pkcs12 dir */
	short PKG_GT = 0x0760;	/* GraphTrends error codes 0 - 31 */
	short PKG_NIF2 = 0x0780;	/* Second nif error package 0-127 */
	short PKG_DIR = 0x07d8;	/* Directory Independent Package 0-39 */
	short PKG_CLIENT = 0x0800;  /* Client codes limited to 0 - 47 */
	short PKG_LSXUI3 = 0x0830;  /* more LSXUI ERROR codes limited to 0 - 15 */
	short PKG_APC = 0x0840;  /* APC codes limited to 0 - 31 */
	short PKG_DBMISC2 = 0x0860;	/* DBMISC2 codes limited to 0 - 15 */
	short PKG_TASK_LOADER = 0x0870;	/* Task Loader package */
	short PKG_MAILMISC4 = 0x0880;	/* Mail Misc  limit to 0 - 127 */
	short PKG_SERVER = 0x0900;
	short PKG_NETWORK = 0x0A00;		/* If you split this range, fix IS_PKG_NETWORK below */
	short PKG_WMISC_ERR = 0x0B00;		/* Codes are limited to 0 - 63 */
	short PKG_XPM_ERR = 0x0B40;		/* Codes are limited to 0 - 63 */
	short PKG_ACTION = 0x0B80;		/* Codes are limited to 0 - 63 */
	short PKG_ACTIONPANE = 0x0BC0;		/* Codes are limited to 0 - 63 */
	short PKG_EDIT_ERR = 0x0C00;
	/* SDK END */
	short PKG_ADDIN3 = 0x0C00;		/* Overlaps PKG_EDIT_ERR intentionally */
	/* Intended to be used ONLY for SMTP MTA */
	/* error messages that are logged "publicly" */
	/* in the EVENTS db, and therefore, must */
	/* be unique.  See PKG_ADDIN4-5 also. */
	/* SDK BEGIN */
	short PKG_VIEW_ERR = 0x0D00;
	short PKG_MAIL = 0x0E00;		/* MAIL errors are limited to 0-63 */
	short PKG_BSAFE3 = 0x0E40;	    /* Limited to 0-31 */
	short PKG_BSAFE4 = 0x0E60;	    /* Limited to 0-31 */
	short PKG_CONV = 0x0E80;		/* CONV errors are limited to 0 - 31 */
	short PKG_NSF4 = 0x0EA0;		/* NSF4 errors are limited to 0 - 31 */
	short PKG_FRAMDES = 0x0EC0;		/* Frame design errors limited to 0 - 31 */
	short PKG_NETWORK3 = 0x0EE0;		/* AVAILABLE codes are limited to 0 - 31 */
	short PKG_FT = 0x0F00;		/* FT errors are limited to 0 - 63 */
	short PKG_BSAFE5 = 0x0F40;	    /* Limited to 0-15 */
	short PKG_DBMISC = 0x0F50;		/* 0 - 47 */
	short PKG_NETWORK2 = 0x0F80;		/* 0 - 127, if this changes, fix IS_PKG_NETWORK below */
	short PKG_DEBUG = 0x1000;		/* Debug strings, like for DDE, limited to 0 - 79 */
	short PKG_SSL = 0x1040;		/* SSL errors 0 - 15 */
	short PKG_SERVER4 = 0x1050;		/* SERVER4, limited to 0 - 15 */
	short PKG_BOOKMARK = 0x1060;		/* BOOKMARK, limited to 0 - 15 */
	short PKG_NSF5 = 0x1070;		/* NSF5, limited to 0 - 15 */
	short PKG_NEM_ERR = 0x1080;		/* NEM, limited to 0 - 127 */
	short PKG_ROUTER = 0x1100;		/* Errors returned by ROUTERL, 0 - 79 */
	short PKG_MAILMAN = 0x1150;		/* Errors returned by MAILMAN, 0 - 31 */
	short PKG_ROUTER2 = 0x1170;		/* Errors returned by ROUTERL, 0 - 47 */
	short PKG_LSBE = 0x11b0;		/* LSXBE errors, 0 - 47 */
	short PKG_LSDO = 0x11e0;		/* LSDOE errors, 0 - 15 */
	short PKG_LSXDB2 = 0x11f0;		/* LSXDB2 errors, 0 - 15 */
	short PKG_REG2 = 0x1200;		/* REG2 errors, 0 - 127 */
	short PKG_LSIDE = 0x1280;		/* ide specific errors 0 - 95 */
	short PKG_HTML = 0x12D0;		/* html parser errors, 0 - 31 */ 
	short PKG_SERVER5 = 0x12F0;		/* PKG_SERVER5 limit to 0 - 15 */
	short PKG_LOG = 0x1300;
	short PKG_NSF3 = 0x1380;		/* More NSF codes - limited to 0-127*/
	short PKG_MOBILE_GEOGPS = 0x1400;	/* NotesGPS Frontend error codes - limit to 0-63 */
	short PKG_UNUSED_1 = 0x1440;		/* Limit this to 0 - 63 */
	short PKG_UNUSED_2 = 0x1480;		/* Limit this to 0 - 63 */
	short PKG_UNUSED_3 = 0x14C0;		/* Limit this to 0 - 63 */
	short PKG_EVENT = 0x1500;		/* Event codes starting from 0-47 */
	short PKG_FIDE = 0x1530;		/* Event codes starting from 0-31 */
	short PKG_NETWORK4 = 0x1550;		/* Limited to 0 - 15, if this changes, fix IS_PKG_NETWORK below */
	short PKG_MAILMISC2 = 0x1560;		/* Mailmisc2 codes starting from 0-31 */
	short PKG_BCASE = 0x1580;		/* Briefcase codes are limited to 0 - 95 */
	short PKG_SECURE2 = 0x15E0;		/* Secure2 limit to 0 - 15 */
	short PKG_BSAFE6 = 0x15F0;		/* Bsafe codes are limited to 0 - 15 */
	short PKG_REPL = 0x1600;		/* Errors returned by REPLSUB library, limited to 0 - 199 */
	short PKG_ADMIN_ERR2 = 0x16C8;		/* PKG_ADMIN_ERR2 limited to 0 - 55 */
	short PKG_BSAFE = 0x1700;		/* BSAFE codes are limited to 0 - 151 */
	short PKG_SERVER7 = 0x1798;		/* PKG_SERVER7 limit to 0 - 103 */
	short PKG_DESK_ERR = 0x1800;
	short PKG_SECURE = 0x1900;
	short PKG_AGENT = 0x1A00;		/* AGENT codes are limited to 0-63 */
	short PKG_CCONSOLE2 = 0x1A50;		/* CCONSOLE2 codes are limited to 0-15 */
	short PKG_PLAT_STAT_ERR = 0x1A60;	/* Platform Statistics error package */
	short PKG_AGENT1 = 0x1A70;		/* AGENT1 codes are limited to 0-15 */
	short PKG_AGENTS3 = 0x1A80;		/* AGENTS3 codes are limited to 0-47 */
	short PKG_AGENT2 = 0x1AB0;		/* AGENT2 codes are limited to 0-79 */
	short PKG_XML = 0x1B00;		/* XML codes: limit to 0 - 235 */
	short PKG_DAOS2 = 0x1BEB;		/* DAOS2 codes: limit to 0 - 4 */
	short PKG_DAOS = 0x1BF0;		/* DAOS codes: limit to 0 - 15 */
	short PKG_NETDRV = 0x1C00;		/* If this PKG space is split, fix IS_PKG_NETWORK below */
	short PKG_IMPORT = 0x1D00;      /* Used for all imports - see IMPKG_xxx below */
	short PKG_EXPORT = 0x1E00;      /* Used for all exports - see EXPKG_xxx below */
	short PKG_LSXUI2 = 0x1F00;		/* LSXUI2 Codes are limited to 0 - 63 */
	/* SDK END */
	short PKG_TEST = 0x1F40;		/* For use by test programs ONLY! */
	/* SDK BEGIN */
	short PKG_REG = 0x2000;

	/* Following 8 groups used for native OS error codes, mapped by OSMapError
		and translated by OSLoadString.  These give better feedback in certain
		cases, but should NEVER be interpreted if STS_REMOTE bit is set,
		indicating that another OS on server might have generated the error! */

	short PKG_NATIVE_FIRST = 0x2100;
	short PKG_NATIVE_LAST = 0x28FF;

	short PKG_XPC = 0x2100;	/* Feature is no longer used and deprecated, leave in until code is removed */

	short PKG_NSE = 0x2900;		/* Network script engine */
	short PKG_NSF6 = 0x29B0;		/* NSF6, limited to 0 - 15 */
	short PKG_PERFSTAT = 0x29C0;		/* PERFSTAT error codes 0-31 */
	short PKG_MISC2 = 0x29E0;		/* MISC2 limit to 0 - 31 */
	short PKG_NETDRVLCL = 0x2A00;		/* Used for all Network Drivers, if PKG space is split, fix IS_PKG_NETWORK below */
	short PKG_NTI = 0x2B00;		/* Used for NTI and its new Net drivers */

	short PKG_VIEWMAP = 0x2C00;		/* for ViewMap */
	short PKG_BSAFE2 = 0x2CF0;		/* for BSAFE x509 routines */

	short PKG_REPL2 = 0x2D00;		/* for remote debug 0-127 */
	short PKG_RDBGERR = 0x2D80;		/* for remote debug err messages 0-63 */
	short PKG_NSF10 = 0x2DC0;		/* PGK_NSF10 err messages 0-63 */

	short PKG_AGENTS = 0x2E00;		/* for agents, limit to 0 - 127 */
	short PKG_DESK_ERR2 = 0x2E80;		/* more Desk limit to 0 - 127 */
	short PKG_LSCRIPT = 0x2F00; 		/* LotusScript Interface, limit 0-127 */
	short PKG_LSXUI = 0x2F80;		/* LotusScript Front-end classes, 0-127 */
	short PKG_DSGN = 0x3000;		/* Database design package, 0-63 */
	short PKG_SERVER6 = 0x3040;		/* PKG_SERVER6 error codes 0 - 63 */
	short PKG_ADMIN_ERR = 0x3080;		/* Admin facility, 0 - 127 */
	short PKG_DBD = 0x3100;		/* database driver error codes, 0 - 199 */
	short PKG_COMPILER7 = 0x31B8;		/* More COMPILER codes, limited to 0-15 */
	short PKG_NEWS = 0x31C8;		/* News classes, 0 - 15 */
	short PKG_IMAIL_EXT = 0x31D8;		/* IMail database extensions 0 - 23 */
	short PKG_DAEMON = 0x31F0;		/* CDaemon codes, 0 - 15 */
	short PKG_COMPILER8 = 0x3200;		/* More COMPILER codes, limit to 0 - 255 */
	short PKG_ADDIN = 0x3300;		/* For use by mail gateways, etc. */
	short PKG_EDIT_ERR2 = 0x3400;		/* need additional block for edit */
	/* SDK END */
	short PKG_ADDIN4 = 0x3400;		/* Overlaps PKG_EDIT_ERR2 intentionally */
	/* (see comments in PKG_ADDIN3) */
	/* SDK BEGIN */
	short PKG_LSCRIPT2 = 0x3500;		/* Lotusscript interface, limit 0 - 127 */
	short PKG_ADMIN_ERR3 = 0x3580;		/* PKG_ADMIN_ERR3 limited to 0 - 55 */																
	short PKG_DSGN2 = 0x35C0;		/* PKG_DSGN2 limit to 0 - 63 */	
	short PKG_GRMISC = 0x3600;      /* Graphics Library 0-31 */
	short PKG_VIMSMI = 0x3620;		/* VIM and SMI block codes 0-10 */
	short PKG_WEB = 0x3640;      /* InterNotes client extensions 0-191 */
	short PKG_ADDIN2 = 0x3700;		/* For extensions to PKG_ADDIN */
	short PKG_NSF9 = 0x3800;		/* NSF9, limited to 0 - 255 */
	short PKG_DESK_ERR4 = 0x3900;		/* DESK error, limit to 0 - 127 */
	short PKG_ORB = 0x3980;		/* ORB, limit to 0 - 63 */
	short PKG_LSXUI5 = 0x39C0;		/* LSXUI5 Codes are limited to 0 - 63 */
	short PKG_HTTP = 0x3A00;		/* Web Server. limit to 0 - 199 */
	short PKG_POP3 = 0x3AC8;		/* POP3.  0 - 19 */
	short PKG_MAILMISC = 0x3ADC;		/* Mailmisc. 0 - 3 */
	short PKG_SMTP = 0x3AE0;		/* SMTP.  0 - 7 */
	short PKG_POP3C = 0x3AE8;		/* POP3 CLIENT.  0-2 */
	short PKG_SMTPC = 0x3AEB;		/* SMTP.  0 - 6 */
	short PKG_MAILMISC1 = 0x3AF2;		/* MAILMISC.  0 - 13 */

	short PKG_DB2NSF = 0x3B00;		/* DB2NSF stuff 				0 - 191	[0x3B00 - 0x3BBF] */

	short PKG_ADPWS = 0x3BC0;		/* Active Directory Password sync 0 - 31 [0x3BC0 - 0x3BDF] */

	short PKG_DIREX = 0x3BE0;		/* Directory Extension Manager 	0 - 31 	[0x3BE0 - 0x3BFF] */

	short PKG_SMARTI = 0x3C00;		/* To make smart icon res unique */
	short PKG_TOOLBAR = PKG_SMARTI;  /* R6 Reuse smarticon pkg for new toolbars */
	short PKG_SMTPC2 = 0x3C80;		/* SMTP server and client. 0-7 */
	short PKG_DESK_ERR5 = 0x3C90;		/* more Desk limit to 0- 111 */
	/* SDK END */

	/* SmartIcon package can theoretically be moved from 0x3C00 to > 0x8000 
	 * because it is frontend only, but it is tricky because there are some
	 * hardcoded references to the package range floating around.  
	 * Move later if we get desperate.
	 */

	/* SDK BEGIN */
	short PKG_OLE_ERR = 0x3D00;		/* OLE error codes 0-63*/
	/* Note, PKG_OLE_ERR+0-53 were used in V3 */
	short PKG_MISC3 = 0x3D40;		/* PKG_MISC3 codes are limited to 0-31 */
	short PKG_NSF7 = 0x3D60;		/* PKG_NSF7 codes are limited to 0-31 */
	short PKG_NETWORK5 = 0x3D80;		/* Limited to 0-31, if this area changes, fix IS_PKG_NETWORK below */
	short PKG_DESK_ERR3 = 0x3DA0;		/* PKG_DESK_ERR3 are limited to 0-63 */
	short PKG_EVENT2 = 0x3DE0;		/* Event codes starting from 0-31 */
	/* #define PKG_OLE_CMD		0x3E00	 OLE Api Command Descriptions - OBSOLETE in V5 */
	/* Note, PKG_OLE_CMD+0-56 were used in V3 */
	short PKG_JAVAWRAP = 0x3E00;		/* javawrap error codes 0-63 */
	short PKG_ASSISTANT_ERR = 0x3E40;	/* Assistant codes limit to 0 - 63 */
	short PKG_JSWRAP = 0x3E80;		/* javascript wrap error codes 0-63 */
	short PKG_PRINT_ERR = 0x3EC0;		/* client prshort error codes limit to 0 - 63 */
	short PKG_EDIT_ERR3 = 0x3F00;		/* More editor codes limit to 0 - 191*/
	/* SDK END */
	short PKG_ADDIN5 = 0x3F00;		/* Overlaps PKG_EDIT_ERR3 intentionally */
	/* (see comments in PKG_ADDIN3) */
	short PKG_BCASE2 = 0x3FC0;		/* PKG_BCASE2 Briefcase codes are limited to 0 - 47 */
	short PKG_SECURE3 = 0x3FF0;		/* PKG_SECURE3 Security3 codes are limited to 0 - 15 */
	/* SDK BEGIN */
	/*	3F00 IS THE LAST PACKAGE THAT CAN BE DEFINED FOR ERROR CODES!
	 *  (LOOK FOR HOLES ABOVE).  THE RANGE 0x4000-0x7FFF CANNOT BE USED.
	 */

	/* SDK END */
	/*  THE RANGE 0x8000-0xBFFF CANNOT BE USED FOR ERROR CODES OR BLOCK CODES, 
	 *  BUT CAN BE USED FOR STRING CODES (FOR UI, ETC) BECAUSE AS LONG AS 
	 *  THE CODE DOES NOT NEED TO PARTICIPATE IN STS_DISPLAYED OR
	 *  STS_REMOTE MANAGEMENT, THAT IS, IT IS NEVER TREATED AS AN ERROR CODE.
	 *  NOTE THAT IF WE NEED 0xC000-0xFFFF, WE WILL HAVE TO CHANGE HOW
	 *  HELP LINES ARE MANAGED.
	 */

	/* Package defines below this line cannot be used for error codes,
	 * only for non-error string resources.  These packages are outside
	 * of the SDK because the SDK should not need them.
	 */

	short PKG_DESK_STR = (short) 0x8000;
	short PKG_DESK_STR2 = (short) 0x8100;
	short PKG_DESK_STR3 = (short) 0x8200;
	short PKG_DESK_STR4 = (short) 0x8300;
	short PKG_EDIT_STR = (short) 0x8400;
	short PKG_EDIT_STR2 = (short) 0x8500;
	short PKG_VIEW_STR = (short) 0x8600;
	short PKG_VIEW_STR2 = (short) 0x8700;					   
	short PKG_NEM_STR = (short) 0x8800;
	short PKG_WMISC_STR = (short) 0x8900;		/* limited 0-63 */
	short PKG_CTRY_STR = (short) 0x8940;		/* country name strings limited 0-63 */
	short PKG_ADMIN_STR2 = (short) 0x8980;		/* more Admin limited to 0-127 */
	short PKG_COMPILER_STR = (short) 0x8A00;
	short PKG_COMPILER_STR2 = (short) 0x8B00;
	short PKG_COMPILER_STR3 = (short) 0x8C00;
	short PKG_COMPILER_STR4 = (short) 0x8D00;
	short PKG_IXMETHOD = (short) 0x8E00;		/* import/export methods for notes.ini */
	short PKG_ASSISTANT_STR = (short) 0x8F00;
	short PKG_ADMIN_STR = (short) 0x9000;		/* Admin package */
	short PKG_DESK_STR5 = (short) 0x9100;		/* desk string codes, limit to 0 - 255 */
	short PKG_SUBZONE_STR = (short) 0x9200;		/* Time zone - subzone strings limited to 0 - 127 */
	short PKG_PRINT_STR = (short) 0x9280;		/* Client Prshort (calendar, doc, view, ...) 0-127  */
	short PKG_TOOLBAR_STR = (short) 0x9300;		/* Rnext Toolbar non error strings 0 - 255 */
	short PKG_ACTIONPANE_STR = (short) 0x9400;		/* Action pane non error strings 0 - 127 */
	short PKG_ADMIN_STR3 = (short) 0x9480;		/* more Admin limited to 0-127 */
	short PKG_DESK_STR6 = (short) 0x9500;		/* desk6; 0 - 255 */
	short PKG_MISC_STR = (short) 0x9600;		/* misc strings 2 - 255*/
	short PKG_BSAFE_STR = (short) 0x9700;		/* bsafe strings 0 - 255 */
	short PKG_XMLPARSER_STR1 = (short) 0x9800;		/* first set of apache xml parser error msgs 0 - 511 */
	short PKG_LOG_STR = (short) 0x9A00;		/* logging string codes 0-127 */
	short PKG_LDAP_STR = (short) 0x9A80;		/* LDAP strings */
	short PKG_DESK_STR7 = (short) 0x9B00;		/* desk7 strings 0 - 255 */
	short PKG_XMLPARSER_STR2 = (short) 0x9C00;		/* second set of apache xml parser error msgs 0 - 511 */
	short PKG_SCHUI_STR = (short) 0x9E00;		/* Scheduler control non error strings 0 - 127 */
	short PKG_SUPPORT_STR = (short) 0x9E80;		/* Support related (NSD, ADC, etc ...) strings 0 - 31 */
	short PKG_EVENT3 = (short) 0x9EA0;		/* More Events Codes: 0 - 60 */
	short PKG_ADMIN_STR4 = (short) 0x9F00;		/* more Admin limited to 0-255 */
	short PKG_DESK_STR8 = (short) 0xA000;		/* more Desk limited to 0-255 */
	short PKG_SERVER_STR = (short) 0xA100;		/* server string code, limit to 0 - 127 */
	short PKG_NSF_STR = (short) 0xA180;		/* NSF string codes, limit to 0 - 127 */
	short PKG_DB2_STR = (short) 0xA200;		/* More DB2 stringd limited 0 - 255 */
	short PKG_OS_STR = (short) 0xA300;		/* second set of 128 error msgs for OS */
	short PKG_ROUTER_STR = (short) 0xA380;		/* Router strings */
	short PKG_WEBSVC_STR = (short) 0xA400;		/* Web Services 0 - 127 */
	short PKG_AVAIL_A47F = (short) 0xA47F;		/* keep an available one at the end for a marker */
	short PKG_ADMIN_STR5 = (short) 0xA500;		/* more Admin limited to 0-255 */
	short PKG_ADMIN_VAULT_STR = (short) 0xA600;		/* more Admin limited to 0-255 */
	short PKG_VIEW_STR3 = (short) 0xA700;		/* more View limited to 0-255*/
	short PKG_DESK_STR9 = (short) 0xA800;		/* more Desk limited to 0-255 */
	short PKG_SERVER_STR1 = (short) 0xA900;		/* more Server limited to 0-255*/
	short PKG_NET_STR = (short) 0xAA00;		/* more Net limited to 0-255*/
	short PKG_NSF_STR2 = (short) 0xAB00;		/* more Nsf limited to 0-255*/
	short PKG_QOS = (short) 0xAC00;		/* more QOS limited to 0-31*/
	short PKG_GQF_STR = (short) 0xAC20;		/* General Query Facility limited to 32-63 */

}
