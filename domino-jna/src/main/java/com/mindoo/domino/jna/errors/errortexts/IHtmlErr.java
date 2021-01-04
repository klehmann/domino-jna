package com.mindoo.domino.jna.errors.errortexts;

public interface IHtmlErr extends IGlobalErr {

	@ErrorText(text="Error processing HTML option list")
	short ERR_HTMLAPI_HTMLOPTION = (PKG_HTTP+89);
	@ErrorText(text="Feature not supported in the HTML API.")
	short ERR_HTMLAPI_NOT_SUPPORTED = (PKG_HTTP+90);
	@ErrorText(text="Error reading data from client")
	short ERR_HTTP_READ_DATA = (PKG_HTTP+91);
	@ErrorText(text="HTMLAPI reference list is full")
	short ERR_HTMLAPI_REFLIST_FULL = (PKG_HTTP+92);
	@ErrorText(text="HTMLAPI Problem converting to HTML")
	short ERR_HTMLAPI_GENERATING_HTML = (PKG_HTTP+93);
	@ErrorText(text="HTMLAPI Cannot set a readonly property")
	short ERR_HTMLAPI_READONLY_PROP = (PKG_HTTP+94);
	@ErrorText(text="HTMLAPI Invalid property type")
	short ERR_HTMLAPI_INVALID_PROP_TYPE = (PKG_HTTP+95);
	@ErrorText(text="HTMLAPI Invalid function argument")
	short ERR_HTMLAPI_INVALID_ARG = (PKG_HTTP+96);
	@ErrorText(text="Unable to initialize HTMLAPI conversion object")
	short ERR_HTMLAPI_INIT = (PKG_HTTP+97);
	@ErrorText(text="Unable to initialize inotes")
	short ERR_INOTES_INIT = (PKG_HTTP+98);
	@ErrorText(text="Unable to terminate inotes")
	short ERR_INOTES_TERM = (PKG_HTTP+99);
	@ErrorText(text="Insufficient memory - thread local heap is full")
	short ERR_HTTP_THMEMORY = (PKG_HTTP+100);
	@ErrorText(text="iNotes XSS Security:  Request check found unexpected '<', in query string '%s', possible attempt to insert unauthorized commands.  Request not processed.")
	short ERR_XSS_GET_BAD_URL = (PKG_HTTP+101);
	@ErrorText(text="iNotes XSS Security:  Referer Check Error: Referer host '%s' not found in whitelist.  Request not processed.")
	short ERR_XSS_POST_REFERER_NOT_IN_LIST = (PKG_HTTP+102);
	@ErrorText(text="iNotes XSS Security:  Referer Check Error: Unauthorized domain '%s' attempted to issue an iNotes command.  Request not processed.")
	short ERR_XSS_REFERER_BAD_DOMAIN = (PKG_HTTP+103);
	@ErrorText(text="iNotes XSS Security:  Referer Check Error: No referer value exists when it is required to ensure that iNotes commands are not issued without the user's knowledge.  Request not processed.")
	short ERR_XSS_POST_REFERER_NOT_FOUND = (PKG_HTTP+104);
	@ErrorText(text="iNotes XSS Security:  Invalid Request, missing expected nonce value; with Referer: '%s'. Request not processed, throwing exception.")
	short ERR_XSS_POST_NONCE_NOT_FOUND = (PKG_HTTP+105);
	@ErrorText(text="iNotes XSS Security:  Invalid Request, unexpected nonce value; with Referer: '%s'. Request not processed, throwing exception.")
	short ERR_XSS_POST_NONCE_WRONG = (PKG_HTTP+106);

}
