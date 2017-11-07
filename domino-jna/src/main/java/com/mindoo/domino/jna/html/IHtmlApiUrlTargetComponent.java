package com.mindoo.domino.jna.html;

public interface IHtmlApiUrlTargetComponent<T> {

	public TargetType getType();
	
	public Class<T> getValueClass();
	
	public T getValue();
	
}
