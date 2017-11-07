package com.mindoo.domino.jna.html;

import java.util.List;

/**
 * HTMLAPIReference is the complete reference of converted HTML result.
 * 
 * @author Karsten Lehmann
 *
 */
public interface IHtmlApiReference {

	/**
	 * Returns how the reference is used in the HTML text (see {@link ReferenceType})
	 * 
	 * @return type
	 */
	public ReferenceType getType();
	
	/**
	 * Returns the reference Text, e.g. the URL of anchor or img tags
	 * 
	 * @return text
	 */
	public String getReferenceText();
	
	/**
	 * The fragment part of the URL
	 * 
	 * @return fragment or empty string
	 */
	public String getFragment();
	
	/**
	 * Returns the web server command associated with the target URL
	 * 
	 * @return command
	 */
	public CommandId getCommandId();
	
	/**
	 * Returns all parts of the reference text, e.g. tha name of the database in
	 * the URL, the name of the view, the UNID of the document etc.
	 * 
	 * @return url target components
	 */
	public List<IHtmlApiUrlTargetComponent<?>> getTargets();
	
	/**
	 * Returns a single target with the specified target type
	 * 
	 * @param type target type
	 * @return target or null if not found
	 */
	public IHtmlApiUrlTargetComponent<?> getTargetByType(TargetType type);
	
}
