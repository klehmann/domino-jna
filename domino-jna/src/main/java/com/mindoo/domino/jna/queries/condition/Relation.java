package com.mindoo.domino.jna.queries.condition;

import com.mindoo.domino.jna.queries.condition.internal.EqualsIgnoreCaseRelation;
import com.mindoo.domino.jna.queries.condition.internal.EqualsRelation;
import com.mindoo.domino.jna.queries.condition.internal.GreaterThanIgnoreCaseRelation;
import com.mindoo.domino.jna.queries.condition.internal.GreaterThanRelation;
import com.mindoo.domino.jna.queries.condition.internal.LessThanIgnoreCaseRelation;
import com.mindoo.domino.jna.queries.condition.internal.LessThanRelation;
import com.mindoo.domino.jna.queries.condition.internal.NotEqualsRelation;
import com.mindoo.domino.jna.queries.condition.internal.NotStartsWithIgnoreCaseRelation;
import com.mindoo.domino.jna.queries.condition.internal.NotStartsWithRelation;
import com.mindoo.domino.jna.queries.condition.internal.StartsWithIgnoreCaseRelation;
import com.mindoo.domino.jna.queries.condition.internal.StartsWithRelation;

public interface Relation {
	
	public static final Relation StartsWith = new StartsWithRelation();
	public static final Relation StartsWithIgnoreCase = new StartsWithIgnoreCaseRelation();
	public static final Relation NotStartsWith = new NotStartsWithRelation();
	public static final Relation NotStartsWithIgnoreCase = new NotStartsWithIgnoreCaseRelation();
	
	public static final Relation Equals = new EqualsRelation();
	public static final Relation EqualsIgnoreCase = new EqualsIgnoreCaseRelation();
	public static final Relation NotEquals = new NotEqualsRelation();
	public static final Relation NotEqualsIgnoreCase = new EqualsIgnoreCaseRelation();
	
	public static final Relation GreaterThan = new GreaterThanRelation();
	public static final Relation GreaterThanIgnoreCase = new GreaterThanIgnoreCaseRelation();
	
	public static final Relation LessThan = new LessThanRelation();
	public static final Relation LessThanIgnoreCase = new LessThanIgnoreCaseRelation();

}
