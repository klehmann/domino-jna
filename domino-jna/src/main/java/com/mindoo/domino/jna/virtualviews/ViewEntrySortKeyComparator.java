package com.mindoo.domino.jna.virtualviews;

import java.util.Comparator;
import java.util.List;

import com.mindoo.domino.jna.NotesTimeDate;

/**
 * Comparator to sort {@link ViewEntrySortKey} objects within one level of the {@link VirtualView} tree structure
 */
public class ViewEntrySortKeyComparator implements Comparator<ViewEntrySortKey> {
	private boolean categoryOrderDescending;
	private boolean[] docOrderPerColumnDescending;
	
	public ViewEntrySortKeyComparator(boolean categoryOrderDescending, boolean[] docOrderDescending) {
		this.categoryOrderDescending = categoryOrderDescending;
		this.docOrderPerColumnDescending = docOrderDescending;
	}
	
	@Override
	public int compare(ViewEntrySortKey o1, ViewEntrySortKey o2) {
		List<Object> values1 = o1.getValues();
		List<Object> values2 = o2.getValues();

		boolean isCategory1 = o1.isCategory();
		boolean isCategory2 = o2.isCategory();

		if (isCategory1) {
			if (isCategory2) {
				Object catVal1 = values1.get(0);
				Object catVal2 = values2.get(0);
				
				//sort null category values to the bottom ("(Not categorized)")
				if (catVal1 == null) {
					if (catVal2 == null) {
						return 0;
					} else {
						return categoryOrderDescending ? -1 : 1;
					}
				}
				else {
					if (catVal2 == null) {
						return categoryOrderDescending ? 1 : -1;
					}
					else {
						//special case, LOW_SORTVAL always on top; we use LOW_SORTVAL and HIGH_SORTVAL to select all categories or all documents
						if (VirtualViewEntryData.LOW_SORTVAL.equals(catVal1)) {
							if (VirtualViewEntryData.LOW_SORTVAL.equals(catVal2)) {
								throw new IllegalStateException("Unexpected to find this value twice");
							}
							
							return -1;
						}
						else if (VirtualViewEntryData.LOW_SORTVAL.equals(catVal2)) {
							if (VirtualViewEntryData.LOW_SORTVAL.equals(catVal1)) {
								throw new IllegalStateException("Unexpected to find this value twice");
							}
							
							return 1;
						}
						
						//special case, HIGH_SORTVAL always on bottom
						if (VirtualViewEntryData.HIGH_SORTVAL.equals(catVal1)) {
							if (VirtualViewEntryData.HIGH_SORTVAL.equals(catVal2)) {
								throw new IllegalStateException("Unexpected to find this value twice");
							}
							
							return 1;
                        }
						else if (VirtualViewEntryData.HIGH_SORTVAL.equals(catVal2)) {
							if (VirtualViewEntryData.HIGH_SORTVAL.equals(catVal1)) {
								throw new IllegalStateException("Unexpected to find this value twice");
							}
							
							return -1;
						}
						
						//for categories, just compare the value, not the origin and note id (because they are the same for all entries)
						if (catVal1 instanceof String && catVal2 instanceof String) {
							String str1 = (String) catVal1;
							String str2 = (String) catVal2;
							int result = str1.compareToIgnoreCase(str2);
							
							if (result != 0) {
								return categoryOrderDescending ? -result : result;
							}
							
						} else if (catVal1 instanceof Number && catVal2 instanceof Number) {
							Number num1 = (Number) catVal1;
							Number num2 = (Number) catVal2;
							double d1 = num1.doubleValue();
							double d2 = num2.doubleValue();
							int result = Double.compare(d1, d2);
							
							if (result != 0) {
								return categoryOrderDescending ? -result : result;
							}

						} else if (catVal1.getClass().equals(catVal2.getClass()) && catVal1 instanceof Comparable) {
							int result = ((Comparable)catVal1).compareTo((Comparable)catVal2);
							
							if (result != 0) {
								return categoryOrderDescending ? -result : result;
							}

						} else {
							String class1 = catVal1 != null ? catVal1.getClass().getName() : "null";
							String class2 = catVal2 != null ? catVal2.getClass().getName() : "null";
							
							throw new IllegalArgumentException("Incompatible/unknown value types for category comparison: " +
									"value1="+catVal1+" ("+class1+"), value2="+catVal2+" ("+class2+")");
						}
						
						//category values are equal, now sort by origin and note id
						
						return compareOriginAndNoteId(o1, o2);
					}					
				}
			}
			else {
				//sort category above document
				return -1;
			}
		}
		else {
			if (isCategory2) {
				//sort document below category
				return 1;
			}
			else {
				//both are no categories, fall through to sort by values
			}
		}

		//sort by doc values
		int nrOfValues = Math.max(values1.size(), values2.size());
		
		for (int i = 0; i < nrOfValues; i++) {
			Object currValue1 = values1.get(i);
			Object currValue2 = values2.get(i);

			//special case, LOW_SORTVAL always on top
			if (VirtualViewEntryData.LOW_SORTVAL.equals(currValue1)) {
				if (VirtualViewEntryData.LOW_SORTVAL.equals(currValue2)) {
					throw new IllegalStateException("Unexpected to find this value twice");
				}
				
				return -1;
			}
			else if (VirtualViewEntryData.LOW_SORTVAL.equals(currValue2)) {
				if (VirtualViewEntryData.LOW_SORTVAL.equals(currValue1)) {
					throw new IllegalStateException("Unexpected to find this value twice");
				}
				
				return 1;
			}
			
			//special case, HIGH_SORTVAL always on bottom
			if (VirtualViewEntryData.HIGH_SORTVAL.equals(currValue1)) {
				if (VirtualViewEntryData.HIGH_SORTVAL.equals(currValue2)) {
					throw new IllegalStateException("Unexpected to find this value twice");
				}
				
				return 1;
            }
			else if (VirtualViewEntryData.HIGH_SORTVAL.equals(currValue2)) {
				if (VirtualViewEntryData.HIGH_SORTVAL.equals(currValue1)) {
					throw new IllegalStateException("Unexpected to find this value twice");
				}
				
				return -1;
			}

			if ("".equals(currValue1)) {
				currValue1 = null;
			}
			if ("".equals(currValue2)) {
				currValue2 = null;
			}
			
			if (currValue1 == null) {
				if (currValue2 == null) {
					continue;
				} else {
					return docOrderPerColumnDescending[i] ? -1 : 1;
				}
			}
			else {
				if (currValue2 == null) {
					return docOrderPerColumnDescending[i] ? 1 : -1;
				}
				else {
					if (currValue1 instanceof String && currValue2 instanceof String) {
						String str1 = (String) currValue1;
						String str2 = (String) currValue2;
						int result = str1.compareToIgnoreCase(str2);
						if (result != 0) {
							return docOrderPerColumnDescending[i] ? -result : result;
						}
					} else if (currValue1 instanceof Number && currValue2 instanceof Number) {
						Number num1 = (Number) currValue1;
						Number num2 = (Number) currValue2;
						double d1 = num1.doubleValue();
						double d2 = num2.doubleValue();
						int result = Double.compare(d1, d2);
						if (result != 0) {
							return docOrderPerColumnDescending[i] ? -result : result;
						}
					} else if (currValue1.getClass().equals(currValue2.getClass()) && currValue1 instanceof Comparable) {
						int result = ((Comparable)currValue1).compareTo((Comparable)currValue2);
						
						if (result != 0) {
							return categoryOrderDescending ? -result : result;
						}

//					} else if (currValue1 instanceof NotesTimeDate && currValue2 instanceof NotesTimeDate) {
//						NotesTimeDate time1 = (NotesTimeDate) currValue1;
//						NotesTimeDate time2 = (NotesTimeDate) currValue2;
//						
//						int result = time1.compareTo(time2);
//						if (result != 0) {
//							return docOrderPerColumnDescending[i] ? -result : result;
//						}
					} else {
						String class1 = currValue1 != null ? currValue1.getClass().getName() : "null";
						String class2 = currValue2 != null ? currValue2.getClass().getName() : "null";
						
						throw new IllegalArgumentException("Incompatible/unknown value types for document comparison: index= " + i+
								", value1="+currValue1+" ("+class1+"), value2="+currValue2+" ("+class2+")");
					}
				}
			}
			
		}

		//all column sort values equal, now sort by origin and note id
		return compareOriginAndNoteId(o1, o2);
	}

	private int compareOriginAndNoteId(ViewEntrySortKey o1, ViewEntrySortKey o2) {
		String origin1 = o1.getOrigin();
		String origin2 = o2.getOrigin();

		if (VirtualViewEntryData.LOW_ORIGIN.equals(origin1)) {
			if (VirtualViewEntryData.LOW_ORIGIN.equals(origin2)) {
				throw new IllegalStateException("Unexpected to find this value twice");
			}

			return -1;
		} else if (VirtualViewEntryData.LOW_ORIGIN.equals(origin2)) {
			if (VirtualViewEntryData.LOW_ORIGIN.equals(origin1)) {
				throw new IllegalStateException("Unexpected to find this value twice");
			}

			return 1;
		}

		// special case, HIGH_SORTVAL always on bottom
		if (VirtualViewEntryData.HIGH_ORIGIN.equals(origin1)) {
			if (VirtualViewEntryData.HIGH_ORIGIN.equals(origin2)) {
				throw new IllegalStateException("Unexpected to find this value twice");
			}

			return 1;
		} else if (VirtualViewEntryData.HIGH_ORIGIN.equals(origin2)) {
			if (VirtualViewEntryData.HIGH_ORIGIN.equals(origin1)) {
				throw new IllegalStateException("Unexpected to find this value twice");
			}

			return -1;
		}

		int result = origin1.compareTo(origin2);
		if (result != 0) {
			return result;
		}

		int noteId1 = o1.getNoteId();
		int noteId2 = o2.getNoteId();
		return noteId1 - noteId2;
	}
}
