package com.mindoo.domino.jna.mime.attachments;

import java.io.IOException;
import java.io.InputStream;

public interface IMimeAttachment {
	
	public String getFileName() throws IOException;
	
	public String getContentType() throws IOException;
	
	public InputStream getInputStream() throws IOException;
	
}