package com.mindoo.domino.jna.errors.errortexts;

public interface IMailMiscErr extends IGlobalErr {

	@ErrorText(text="Could not compress buffer.")
	short ERR_LZ1FAILED = PKG_MISC+146;
	
	/* Putting the MIME error codes here also for now */
	/* These codes are liited to 0 - 3 */

	@ErrorText(text="Unable to start ccSTR session.") // ERR_MM_INIT_FAILURE_CCSTR
	int ERR_MM_INIT_FAILURE_CCSTR = (PKG_MAILMISC+0);
	@ErrorText(text="Character set conversion translation failure.") // ERR_MM_CCSTR_TRANSLATE_FAILURE
	int ERR_MM_CCSTR_TRANSLATE_FAILURE = (PKG_MAILMISC+1);
	@ErrorText(text="Character set conversion: unable to find line break.") // ERR_MM_CCSTR_NOLINEBREAK
	int ERR_MM_CCSTR_NOLINEBREAK = (PKG_MAILMISC+2);
	@ErrorText(text="Character set conversion failure.") // ERR_MM_CCSTR_OTHER
	int ERR_MM_CCSTR_OTHER = (PKG_MAILMISC+3);
	 
	/* Don't go past PKG_MAILMISC+3 ! */

	@ErrorText(text="Internal MIME handling error.") // ERR_MIME_INTERNAL
	int ERR_MIME_INTERNAL = (PKG_MAILMISC1+0);
	@ErrorText(text="Out of memory in MIME handling.") // ERR_MIME_POOLFULL
	int ERR_MIME_POOLFULL = (PKG_MAILMISC1+1);
	@ErrorText(text="Unable to create object.") // ERR_MM_OBJECT_NOTCREATED
	int ERR_MM_OBJECT_NOTCREATED = (PKG_MAILMISC1+2);
	@ErrorText(text="[Portions of this MIME document are encrypted with a Notes certificate and cannot be read.]") // ERR_MM_DOCUMENT_ENCRYPTED
	int ERR_MM_DOCUMENT_ENCRYPTED = (PKG_MAILMISC1+3);
	@ErrorText(text="Output buffer too small in character set conversion.") // ERR_MM_CCSTR_BUFFER_TOO_SMALL
	int ERR_MM_CCSTR_BUFFER_TOO_SMALL = (PKG_MAILMISC1+4);
	@ErrorText(text="Invalid Internet address specified.") // ERR_MM_BAD_ADDR_SPEC
	int ERR_MM_BAD_ADDR_SPEC = (PKG_MAILMISC1+5);
	@ErrorText(text="Receive buffer too small for line.") // ERR_MM_RECEIVE_BUFFER_TOO_SMALL
	int ERR_MM_RECEIVE_BUFFER_TOO_SMALL = (PKG_MAILMISC1+6);
	@ErrorText(text="No MIME data.") // ERR_MIME_NO_DATA
	int ERR_MIME_NO_DATA = (PKG_MAILMISC1+7);
	@ErrorText(text="RFC822 message saved in file (%s), MIME itemize error: %e") // STR_MIME_SAVE_ITEMIZE_STREAM
	int STR_MIME_SAVE_ITEMIZE_STREAM = (PKG_MAILMISC1+8);
	@ErrorText(text="Unknown type in MIME part item.") // ERR_MIME_PART_TYPE
	int ERR_MIME_PART_TYPE = (PKG_MAILMISC1+9);
	@ErrorText(text="Unable to get NTI SSL configuration or certificate information.") // ERR_MM_NTISSL_NOT_AVAIL
	int ERR_MM_NTISSL_NOT_AVAIL = (PKG_MAILMISC1+10);
	@ErrorText(text="SSL Certificate has expired.") // ERR_MM_SSL_CERT_EXPIRED
	int ERR_MM_SSL_CERT_EXPIRED = (PKG_MAILMISC1+11);
	@ErrorText(text="SSL Certificate is Invalid.") // ERR_MM_SSL_CERT_INVALID
	int ERR_MM_SSL_CERT_INVALID = (PKG_MAILMISC1+12);
	@ErrorText(text="You must enable the Notes TCPIP port.") // ERR_MM_ENABLE_TCPIP_PORT
	int ERR_MM_ENABLE_TCPIP_PORT = (PKG_MAILMISC1+13);

	@ErrorText(text="Invalid repeat rule") // ERR_INVALID_REPEAT_RULE
	int ERR_INVALID_REPEAT_RULE = (PKG_MAILMISC2+6);

	@ErrorText(text="Error creation repeat exceptions") // ERR_CREATE_REPEAT_EXCEPTION
	int ERR_CREATE_REPEAT_EXCEPTION = (PKG_MAILMISC2+7);

	@ErrorText(text="Error creating repeat instances") // ERR_CREATE_REPEAT_INSTANCE
	int ERR_CREATE_REPEAT_INSTANCE = (PKG_MAILMISC2+8);

	@ErrorText(text="Error modifying repeat sets") // ERR_MOD_REPEAT_SETS
	int ERR_MOD_REPEAT_SETS = (PKG_MAILMISC2+9);

	@ErrorText(text="C&S BPool is full") // ERR_CSPOOL_FULL
	int ERR_CSPOOL_FULL = (PKG_MAILMISC2+15);

	@ErrorText(text="Invalid Input Parameter.") // ERR_MM_INVALID_RFC822_PARAM
	int ERR_MM_INVALID_RFC822_PARAM = (PKG_MAILMISC3+0);
	@ErrorText(text="Invalid RFC821 syntax, no Phrase required.") // ERR_MM_INVALID_RFC821
	int ERR_MM_INVALID_RFC821 = (PKG_MAILMISC3+1);
	@ErrorText(text="Double Byte or 8 Bit characters present in address.") // ERR_MM_DB_8BIT_PRESENT
	int ERR_MM_DB_8BIT_PRESENT = (PKG_MAILMISC3+2);
	@ErrorText(text="Invalid Phrase or character found.") // ERR_MM_INVALID_RFC822_PHRASE
	int ERR_MM_INVALID_RFC822_PHRASE = (PKG_MAILMISC3+3);
	@ErrorText(text="Invalid Quoted String or mismatched quotes found.") // ERR_MM_INVALID_RFC822_QUOTED_STRING
	int ERR_MM_INVALID_RFC822_QUOTED_STRING = (PKG_MAILMISC3+4);
	@ErrorText(text="Invalid comment or mismatched parens found.") // ERR_MM_INVALID_RFC822_COMMENT
	int ERR_MM_INVALID_RFC822_COMMENT = (PKG_MAILMISC3+5);
	@ErrorText(text="Invalid Route-Addr found.") // ERR_MM_INVALID_RFC822_ROUTEADDR
	int ERR_MM_INVALID_RFC822_ROUTEADDR = (PKG_MAILMISC3+6);
	@ErrorText(text="Invalid or missing Domain.") // ERR_MM_INVALID_RFC822_DOMAIN
	int ERR_MM_INVALID_RFC822_DOMAIN = (PKG_MAILMISC3+7);
	@ErrorText(text="Invalid LocalPart or character found.") // ERR_MM_INVALID_RFC822_LOCALPART
	int ERR_MM_INVALID_RFC822_LOCALPART = (PKG_MAILMISC3+8);
	@ErrorText(text="Invalid Group specification found.") // ERR_MM_INVALID_RFC822_GROUP
	int ERR_MM_INVALID_RFC822_GROUP = (PKG_MAILMISC3+9);
	@ErrorText(text="Send buffer too small for line.") // ERR_MM_SEND_BUFFER_TOO_SMALL
	int ERR_MM_SEND_BUFFER_TOO_SMALL = (PKG_MAILMISC3+10);

	@ErrorText(text="Import/Export problem: ") // ERR_IMPORT
	int ERR_IMPORT = (PKG_MAILMISC3+22);

	@ErrorText(text="Remote server certificate authentication failed.") // ERR_MM_SSL_AUTH_FAILED
	int ERR_MM_SSL_AUTH_FAILED = (PKG_MAILMISC3+24);
	@ErrorText(text="SSL No Cross Certificate available.") // ERR_MM_SSL_XCERT_NEEDED
	int ERR_MM_SSL_XCERT_NEEDED = (PKG_MAILMISC3+25);

	@ErrorText(text="[Portions of this Notes document are encrypted with an Internet certificate and cannot be read.]") // ERR_MM_DOCUMENT_ENCRYPTED2
	int ERR_MM_DOCUMENT_ENCRYPTED2 = (PKG_MAILMISC3+29);
	@ErrorText(text="[Part is multilingual]") // ERR_MM_NOTIFY_MULTILINGUAL
	int ERR_MM_NOTIFY_MULTILINGUAL = (PKG_MAILMISC3+30);
	@ErrorText(text="[MIME content for this item is stored in attachment %s.  Parsing MIME content failed: %e.]") // ERR_MM_ITEMIZE_FAILED
	int ERR_MM_ITEMIZE_FAILED = (PKG_MAILMISC3+31);
	
	@ErrorText(text="Unimplemented component")
	int ERR_UnimplementedComponent = PKG_MAILMISC4+2;
    @ErrorText(text="Unknown component")
	int ERR_UnknownComponent = PKG_MAILMISC4+3;
    @ErrorText(text="Unimplemented property" )
	int ERR_UnimplementedProperty = PKG_MAILMISC4+4;
    @ErrorText(text="Unknown property" )
	int ERR_UnknownProperty = PKG_MAILMISC4+5;
    @ErrorText(text="Extra name values" )
	int ERR_ExtraNameValues = PKG_MAILMISC4+6;
    @ErrorText(text="Missing or extra address values" )
	int ERR_MissingAddressValues = PKG_MAILMISC4+7;
    @ErrorText(text="Missing or extra label values" )
	int ERR_MissingLabelValues = PKG_MAILMISC4+8;
    @ErrorText(text="VEntity parse object not set" )
	int ERR_VEntityNotSet = PKG_MAILMISC4+9;
    @ErrorText(text="Missing Value" )
	int ERR_MissingValue = PKG_MAILMISC4+10;
    @ErrorText(text="Invalid or missing value" )
	int ERR_InvalidValue = PKG_MAILMISC4+11;
    @ErrorText(text="Invalid or missing property" )
	int ERR_InvalidProperty = PKG_MAILMISC4+12;
    @ErrorText(text="Invalid or missing property parameter" )
	int ERR_InvalidParameter = PKG_MAILMISC4+13;
	@ErrorText(text="Unexpected internal error") // ERR_UNEXPECTED_ERROR
	int ERR_UNEXPECTED_ERROR = PKG_MAILMISC4+14;
	@ErrorText(text="Unexpected end of data") // ERR_UNEXPECTED_EOS
	int ERR_UNEXPECTED_EOS = PKG_MAILMISC4+15;

    @ErrorText(text="Unknown profile" )
	int ERR_UnknownProfile = PKG_MAILMISC4+16;
    @ErrorText(text="Mandatory calendar property missing" )
	int ERR_MissingCalendarProperty = PKG_MAILMISC4+17;
    @ErrorText(text="Missing VEvent components" )
	int ERR_MissingVEventComponents = PKG_MAILMISC4+18;
    @ErrorText(text="Missing VToDo components" )
	int ERR_MissingVToDoComponents = PKG_MAILMISC4+19;
    @ErrorText(text="Missing VFreeBusy components" )
	int ERR_MissingVFreeBusyComponents = PKG_MAILMISC4+20;
    @ErrorText(text="Missing ID" )
	int ERR_MissingID = PKG_MAILMISC4+21;
    @ErrorText(text="Missing attendee or description" )
	int ERR_MissingAttendeeOrDescription = PKG_MAILMISC4+22;
    @ErrorText(text="Missing attendee or status" )
	int ERR_MissingAttendeeOrStatus = PKG_MAILMISC4+23;
    @ErrorText(text="Missing attendee, description or comment" )
	int ERR_MissingAttendee_Desc_Comment = PKG_MAILMISC4+24;
    @ErrorText(text="Missing start date or end date" )
	int ERR_MissingStartDate_EndDate = PKG_MAILMISC4+25;
    @ErrorText(text="Missing start date or due date" )
	int ERR_MissingStartDate_DueDate = PKG_MAILMISC4+26;
    @ErrorText(text="Missing completion date" )
	int ERR_MissingCompletionDate = PKG_MAILMISC4+27;
    @ErrorText(text="Invalid status" )
	int ERR_InvalidStatus = PKG_MAILMISC4+28;
    @ErrorText(text="Invalid profile" )
	int ERR_InvalidProfile = PKG_MAILMISC4+29;
    @ErrorText(text="Invalid calendar version or profile version" )
	int ERR_InvalidCalendarVersion = PKG_MAILMISC4+30;
    @ErrorText(text="Invalid component property found" )
	int ERR_InvalidComponentPropertyFound = PKG_MAILMISC4+31;
    @ErrorText(text="Invalid request status value" )
	int ERR_InvalidRequestStatusValue = PKG_MAILMISC4+32;
    @ErrorText(text="Invalid VEvent property found" )
	int ERR_InvalidVEventPropertyFound = PKG_MAILMISC4+33;
    @ErrorText(text="Invalid VToDo property found" )
	int ERR_InvalidVToDoPropertyFound = PKG_MAILMISC4+34;
    @ErrorText(text="Missing VJournal components" )
	int ERR_MissingVJournalComponents = PKG_MAILMISC4+35;
    @ErrorText(text="Invalid VJournal property found" )
	int ERR_InvalidVJournalPropertyFound = PKG_MAILMISC4+36;

    @ErrorText(text="Number format error" )
	int ERR_NumberFormatException = PKG_MAILMISC4+37;
    @ErrorText(text="Number out of bounds" )
	int ERR_NumberOutOfBounds = PKG_MAILMISC4+38;

	@ErrorText(text="Calendar Profile does not specify owner") // ERR_CS_PROFILE_NOOWNER
	int ERR_CS_PROFILE_NOOWNER = (PKG_MAILMISC4+52);

	@ErrorText(text="[No new CS dates available. Use old format]") // ERR_CS_NORESCHEDLISTAVAILABLE
	int ERR_CS_NORESCHEDLISTAVAILABLE = (PKG_MAILMISC4+53);

	@ErrorText(text="Unable to open the main calendar document. Prompt user before searching on the appointment UNID") // ERR_CS_PROMPT_BEFORE_SEARCH_ON_APPTUNID
	int ERR_CS_PROMPT_BEFORE_SEARCH_ON_APPTUNID = (PKG_MAILMISC4+54);

	@ErrorText(text="Event has not been initialized.") // ERR_CS_EVENT_NOT_INITIALIZED
	int ERR_CS_EVENT_NOT_INITIALIZED = (PKG_MAILMISC4+55);

	@ErrorText(text="Warning: unexpected MIME error: ") // ERR_MIME_UNEXPECTED_ERROR
	int ERR_MIME_UNEXPECTED_ERROR = (PKG_MAILMISC4+56);

	@ErrorText(text="Message header:") // ERR_MIME_BADHEADER
	int ERR_MIME_BADHEADER = (PKG_MAILMISC4+57);

	@ErrorText(text="This Meeting is already on your calendar for these dates.  You must delete those instances before accepting this notice.") // ERR_CS_MUST_DELETE_INSTANCES_MEETING
	int ERR_CS_MUST_DELETE_INSTANCES_MEETING = (PKG_MAILMISC4+61);

	@ErrorText(text="This To Do is already on your calendar for these dates.  You must delete those instances before accepting this notice.") // ERR_CS_MUST_DELETE_INSTANCES_TODO
	int ERR_CS_MUST_DELETE_INSTANCES_TODO = (PKG_MAILMISC4+62);

	@ErrorText(text="The repeating instance document corresponding to this notice cannot be located.") // ERR_CS_CANT_LOCATE_INSTANCE
	int ERR_CS_CANT_LOCATE_INSTANCE = (PKG_MAILMISC4+63);

	@ErrorText(text="Error disclaiming message because: %e.") // ERR_DISCLAIMING_MSG
	int ERR_DISCLAIMING_MSG = (PKG_MAILMISC4+65);

	@ErrorText(text="Message successfully disclaimed for sender %s.") // ERR_DONE_DISCLAIMING
	int ERR_DONE_DISCLAIMING = (PKG_MAILMISC4+66);

	@ErrorText(text="Meeting Resource Declined:  Your meeting date has extended beyond the date limit set up by your administrator.") // ERR_CS_OUTOFBOUNDS_BODY
	int ERR_CS_OUTOFBOUNDS_BODY = (PKG_MAILMISC4+67);

	@ErrorText(text="Declined (over date limit): ") // ERR_CS_OUTOFBOUNDS_SUBJECT
	int ERR_CS_OUTOFBOUNDS_SUBJECT = (PKG_MAILMISC4+68);

	/*	DSF Client Error Codes. */

	@ErrorText(text="DSF Protocol Error") // ERR_DSFC_ERROR
	int ERR_DSFC_ERROR = (PKG_MAILMISC4+69);
	@ErrorText(text="%i DSFClient: %s") // STR_DSFC_LOG_TEXT
	int STR_DSFC_LOG_TEXT = (PKG_MAILMISC4+70);
	@ErrorText(text="Connection to remote DSF server timed out") // ERR_DSFC_TIMEDOUT
	int ERR_DSFC_TIMEDOUT = (PKG_MAILMISC4+71);
	@ErrorText(text="DSF Client session not connected") // ERR_DSFC_NOTCONNECTED
	int ERR_DSFC_NOTCONNECTED = (PKG_MAILMISC4+72);
	@ErrorText(text="Received error response from DSF server") // ERR_DSFC_RESPONSE_ERR
	int ERR_DSFC_RESPONSE_ERR = (PKG_MAILMISC4+73);

	@ErrorText(text="The requested reservation interval was bad.") // ERR_CS_BAD_INTERVAL_REASON
	int ERR_CS_BAD_INTERVAL_REASON = (PKG_MAILMISC4+74);

	@ErrorText(text="Unable to add the reservation into busytime.  See console log for more details.") // ERR_CS_CANT_ADD_TO_BUSYTIME_REASON
	int ERR_CS_CANT_ADD_TO_BUSYTIME_REASON = (PKG_MAILMISC4+75);

	@ErrorText(text="TNEFConvert (DEBUG): %s") // ERR_TNEF_DEBUG
	int ERR_TNEF_DEBUG = (PKG_MAILMISC4+76);

	@ErrorText(text="TNEFConvert: %s") // ERR_TNEF_LOG
	int ERR_TNEF_LOG = (PKG_MAILMISC4+77);

	@ErrorText(text="No Contact records could be found. vCards can be created only from Contacts.") // ERR_VCARD_ONLY_FROM_CONTACT
	int ERR_VCARD_ONLY_FROM_CONTACT = (PKG_MAILMISC4+78);

	@ErrorText(text="You cannot generate a vCard from this source. vCards can only be created from Contact records.") // ERR_VCARD_EMPTY_SET
	int ERR_VCARD_EMPTY_SET = (PKG_MAILMISC4+79);

	@ErrorText(text="[** Private entry.  The description is not available for display. **]") // ERR_CS_PRIVATE_ENTRY
	int ERR_CS_PRIVATE_ENTRY = (PKG_MAILMISC4+80);

	@ErrorText(text="Calendar entry has save conflict.") // ERR_CS_SAVE_CONFLICT_ENTRIES
	int ERR_CS_SAVE_CONFLICT_ENTRIES = (PKG_MAILMISC4+81);

	@ErrorText(text="Calendar entry has duplicate copy.") // ERR_CS_DUPLICATED_ENTRIES
	int ERR_CS_DUPLICATED_ENTRIES = (PKG_MAILMISC4+82);

	@ErrorText(text="SequenceNum need to be fixed.") // ERR_CS_INCORRECT_SEQUENCENUM
	int ERR_CS_INCORRECT_SEQUENCENUM = (PKG_MAILMISC4+83);

	@ErrorText(text="There is a problem with this calendar entry. To correct this problem, ask the calendar owner to open the calendar entry.") // ERR_CS_INSUFFICIENT_ACCESS_FOR_REPAIR
	int ERR_CS_INSUFFICIENT_ACCESS_FOR_REPAIR = (PKG_MAILMISC4+84);

	@ErrorText(text="Unexpected VCalendar method.") // ERR_UNEXPECTED_METHOD
	int ERR_UNEXPECTED_METHOD = (PKG_MAILMISC4+89);

	@ErrorText(text="This database does not contain required calendar design elements.") // ERR_NO_CALENDAR_FOUND
	int ERR_NO_CALENDAR_FOUND = (PKG_MAILMISC4+90);

	@ErrorText(text="Error converting this entry into iCalendar.") // ERR_NOTE2ICAL_CONVERT
	int ERR_NOTE2ICAL_CONVERT = (PKG_MAILMISC4+91);

	@ErrorText(text="Error interpreting iCalendar.") // ERR_ICAL2NOTE_CONVERT
	int ERR_ICAL2NOTE_CONVERT = (PKG_MAILMISC4+92);

	@ErrorText(text="This action is not supported for this calendar entry or notice.") // ERR_CALACTION_INVALID
	int ERR_CALACTION_INVALID = (PKG_MAILMISC4+93);

	@ErrorText(text="iCalendar input is out of date in regards to sequence information.") // ERR_ICAL2NOTE_OUTOFDATE
	int ERR_ICAL2NOTE_OUTOFDATE = (PKG_MAILMISC4+94);

	@ErrorText(text="Incorrect E-Tag: This data has changed since it was last read.") // ERR_ETAG_MISMATCH
	int ERR_ETAG_MISMATCH = (PKG_MAILMISC4+95);

	@ErrorText(text="Calendar and Scheduling APIs have been disabled by your administrator.") // ERR_API_DISABLED
	int ERR_API_DISABLED = (PKG_MAILMISC4+96);

	@ErrorText(text="This action is not allowed since it would overwrite personal changes") // ERR_CALACTION_OVERWRITE_DISALLOWED
	int ERR_CALACTION_OVERWRITE_DISALLOWED = (PKG_MAILMISC4+97);

	@ErrorText(text="Partial data was returned, but there were entries that could not be read.") // ERR_PARTIAL_RESULTS
	int ERR_PARTIAL_RESULTS = (PKG_MAILMISC4+98);

	@ErrorText(text="The invitation for this meeting has not yet been accepted.") // ERR_INVITE_NOT_ACCEPTED
	int ERR_INVITE_NOT_ACCEPTED = (PKG_MAILMISC4+99);

	@ErrorText(text="There was an error sending out notices to meeting participants.") // ERR_IMPLICIT_SCHED_FAILED
	int ERR_IMPLICIT_SCHED_FAILED = (PKG_MAILMISC4+100);

	@ErrorText(text="Unable to interpret the time. No timezone information provided.") // ERR_INVALID_TIMEZONE
	int ERR_INVALID_TIMEZONE = (PKG_MAILMISC4+101);

	@ErrorText(text="The attendee lists do not contain the same number of values.") // ERR_ATTENDEE_LISTS_OUT_OF_SYNC
	int ERR_ATTENDEE_LISTS_OUT_OF_SYNC = (PKG_MAILMISC4+103);

	@ErrorText(text="The maximum number of dates has been exceeded.") // ERR_TIMELIST_TOO_MANY_ENTRIES
	int ERR_TIMELIST_TOO_MANY_ENTRIES = (PKG_MAILMISC4+104);

	@ErrorText(text="Out of memory in CVS HTML parsing.") // ERR_CVSHTML_POOLFULL
	int ERR_CVSHTML_POOLFULL = (PKG_MAILMISC4+105);

	@ErrorText(text="The new entry type does not match the existing entry type.") // ERR_ICAL2NOTE_ENTRY_TYPE_MISMATCH
	int ERR_ICAL2NOTE_ENTRY_TYPE_MISMATCH = (PKG_MAILMISC4+106);

}
