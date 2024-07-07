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
				
				//sort null category values on top ("(no category)")
				if (catVal1 == null) {
					if (catVal2 == null) {
						return 0;
					} else {
						return categoryOrderDescending ? 1 : -1;
					}
				}
				else {
					if (catVal2 == null) {
						return categoryOrderDescending ? -1 : 1;
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

						} else if (catVal1 instanceof NotesTimeDate && catVal2 instanceof Comparable) {
							NotesTimeDate time1 = (NotesTimeDate) catVal1;
							NotesTimeDate time2 = (NotesTimeDate) catVal2;
							int result = time1.compareTo(time2);
							
							if (result != 0) {
								return categoryOrderDescending ? -result : result;
							}

						} else {
							throw new IllegalArgumentException("Unsupported value type for category: " + catVal1.getClass());
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

			if (currValue1 == null) {
				if (currValue2 == null) {
					continue;
				} else {
					return docOrderPerColumnDescending[i] ? 1 : -1;
				}
			}
			else {
				if (currValue2 == null) {
					return docOrderPerColumnDescending[i] ? -1 : 1;
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
					} else if (currValue1 instanceof NotesTimeDate && currValue2 instanceof NotesTimeDate) {
						NotesTimeDate time1 = (NotesTimeDate) currValue1;
						NotesTimeDate time2 = (NotesTimeDate) currValue2;
						
						// Ensure that we can compare the two temporals
//						if (!o1.isSupported(ChronoField.INSTANT_SECONDS) || !o2.isSupported(ChronoField.INSTANT_SECONDS)) {
//							throw new IllegalArgumentException("Both Temporals must support the INSTANT_SECONDS field for comparison");
//						}
//
//						long epochSecond1 = o1.getLong(ChronoField.INSTANT_SECONDS);
//						long epochSecond2 = o2.getLong(ChronoField.INSTANT_SECONDS);
//
//						// Compare the INSTANT_SECONDS, which represents the number of seconds from the Java epoch of 1970-01-01 (ISO).
//						int compare = Long.compare(epochSecond1, epochSecond2);
//
//						if (compare != 0) {
//							return compare;
//						}
//
//						// If the INSTANT_SECONDS are equal, compare the nanosecond part.
//						if (o1.isSupported(ChronoField.NANO_OF_SECOND) && o2.isSupported(ChronoField.NANO_OF_SECOND)) {
//							int nanoOfSecond1 = o1.get(ChronoField.NANO_OF_SECOND);
//							int nanoOfSecond2 = o2.get(ChronoField.NANO_OF_SECOND);
//							return Integer.compare(nanoOfSecond1, nanoOfSecond2);
//						}
						
						int result = time1.compareTo(time2);
						if (result != 0) {
							return docOrderPerColumnDescending[i] ? -result : result;
						}
					} else {
						throw new IllegalArgumentException("Unsupported value type " + currValue1.getClass());
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
