package com.mindoo.domino.jna.errors.errortexts;

public interface IMailMiscErr extends IGlobalErr {

	@ErrorText(text="This database does not contain required calendar design elements.")
	short ERR_NO_CALENDAR_FOUND = (PKG_MAILMISC4+90);

	@ErrorText(text="Error interpreting iCalendar.")
	short ERR_ICAL2NOTE_CONVERT = (PKG_MAILMISC4+92);

	@ErrorText(text="There was an error sending out notices to meeting participants.")
	short ERR_IMPLICIT_SCHED_FAILED = (PKG_MAILMISC4+100);

	@ErrorText(text="Could not compress buffer.")
	short ERR_LZ1FAILED = PKG_MISC+146;
	
}
