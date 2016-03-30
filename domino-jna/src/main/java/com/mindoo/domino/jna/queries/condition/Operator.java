package com.mindoo.domino.jna.queries.condition;

import java.util.ArrayList;
import java.util.List;

public class Operator extends Criteria {
	private enum OperatorType {AND, OR};
	
	private OperatorType m_type;
	private Criteria[] m_criteriaArr;
	
	private Operator(OperatorType type, Criteria[] criteriaArr) {
		m_type = type;
		m_criteriaArr = criteriaArr;
	}
	
	public static Criteria and(Criteria crit1, Criteria crit2, Criteria... criteria) {
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		criteriaList.add(crit1);
		if (criteria!=null) {
			for (Criteria currCrit : criteria) {
				criteriaList.add(currCrit);
			}
		}
		return new Operator(OperatorType.AND, criteriaList.toArray(new Criteria[criteriaList.size()]));
	}
	
	public static Criteria or(Criteria crit1, Criteria crit2, Criteria... criteria) {
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		criteriaList.add(crit1);
		if (criteria!=null) {
			for (Criteria currCrit : criteria) {
				criteriaList.add(currCrit);
			}
		}
		return new Operator(OperatorType.OR, criteriaList.toArray(new Criteria[criteriaList.size()]));
	}
}
