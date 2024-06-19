package com.mindoo.domino.jna.virtualviews;

import java.util.function.Function;

public class VirtualViewColumn {
	public static enum ColumnSort { ASCENDING, DESCENDING, NONE }
	public static enum Category { YES, NO }
	public static enum Hidden { YES, NO }
	
	private String title;
	private String itemName;
	private Category isCategory;
	private Hidden isHidden;
	private ColumnSort sorting;
	
	private String valueFormula;
	private VirtualViewColumnValueFunction valueFunction;
	
	public VirtualViewColumn(String title, String itemName, Category isCategory, Hidden isHidden, ColumnSort sorting, String formula) {
		this.title = title;
		this.itemName = itemName;
		this.isCategory = isCategory;
		this.isHidden = isHidden;
		this.sorting = sorting;

		this.valueFormula = formula;
	}

	public VirtualViewColumn(String title, String itemName, Category isCategory, Hidden isHidden, ColumnSort sorting, VirtualViewColumnValueFunction valueFunction) {
		this.title = title;
		this.itemName = itemName;
		this.isCategory = isCategory;
		this.isHidden = isHidden;
		if (isCategory == Category.YES && sorting == ColumnSort.NONE) {
			throw new IllegalArgumentException("Category columns must be sorted");
		}
		this.sorting = sorting;

		this.valueFunction = valueFunction;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getItemName() {
		return itemName;
	}
	
	public String getValueFormula() {
		return valueFormula;
	}
	
	public VirtualViewColumnValueFunction getValueFunction() {
		return valueFunction;
	}
	
	public boolean isCategory() {
		return isCategory == Category.YES;
	}
	
	public boolean isHidden() {
		return isHidden == Hidden.YES;
	}
	
	public ColumnSort getSorting() {
		return sorting;
	}
	
	@Override
	public String toString() {
		return "VirtualViewColumn [title=" + title + ", itemName=" + itemName + ", formula=" + valueFormula + ", isCategory="
				+ isCategory + ", sorting=" + sorting + "]";
	}
	
}
