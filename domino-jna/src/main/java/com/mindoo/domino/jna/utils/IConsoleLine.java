package com.mindoo.domino.jna.utils;

/**
 * Server console text data with available meta data
 */
public interface IConsoleLine {

	/**
	 * Returns a sequential number of the message
	 * 
	 * @return sequential number
	 */
	int getMsgSeqNum();

	String getTimeStamp();

	/**
	 * Returns the name of the executable that was responsible for
	 * writing the console line
	 * 
	 * @return executable name
	 */
	String getExecName();

	/**
	 * 
	 * @return
	 */
	int getPid();

	long getTid();

	long getVTid();

	int getStatus();

	int getType();

	int getSeverity();

	int getColor();

	String getAddName();

	/**
	 * Returns the text data of the console entry
	 * 
	 * @return console text data
	 */
	String getData();

	boolean isPasswordString();

	boolean isPromptString();
	
}
