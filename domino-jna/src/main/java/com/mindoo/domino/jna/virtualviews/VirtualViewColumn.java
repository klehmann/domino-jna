package com.mindoo.domino.jna.virtualviews;

import com.mindoo.domino.jna.IViewColumn;

public class VirtualViewColumn implements IViewColumn {
	public static enum Category { YES, NO }
	public static enum Hidden { YES, NO }
	public static enum Total { NONE, SUM, AVERAGE }
	
	private String title;
	private String itemName;
	private Category isCategory;
	private Hidden isHidden;
	private ColumnSort sorting;
	private Total totalMode;
	
	private String valueFormula;
	private VirtualViewColumnValueFunction<?> valueFunction;
	
	public VirtualViewColumn(String title, String itemName, Category isCategory, Hidden isHidden, ColumnSort sorting,
			Total totalMode, String formula) {
		this.title = title;
		this.itemName = itemName;
		this.isCategory = isCategory;
		this.isHidden = isHidden;
		this.sorting = sorting;
		this.totalMode = totalMode;
		
		this.valueFormula = formula;
	}

	public VirtualViewColumn(String title, String itemName, Category isCategory, Hidden isHidden, ColumnSort sorting,
			Total totalMode, VirtualViewColumnValueFunction<?> valueFunction) {
		this.title = title;
		this.itemName = itemName;
		this.isCategory = isCategory;
		this.isHidden = isHidden;
		if (isCategory == Category.YES && sorting == ColumnSort.NONE) {
			throw new IllegalArgumentException("Category columns must be sorted");
		}
		this.sorting = sorting;
		this.totalMode = totalMode;
		
		this.valueFunction = valueFunction;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getItemName() {
		return itemName;
	}
	
	public String getFormula() {
		return valueFormula;
	}
	
	public VirtualViewColumnValueFunction<?> getFunction() {
		return valueFunction;
	}
	
	public Total getTotalMode() {
		return totalMode;
	}
	
	public boolean isCategory() {
		return isCategory == Category.YES;
	}
	
	public boolean isHidden() {
		return isHidden == Hidden.YES;
	}
	
	@Override
	public ColumnSort getSorting() {
		return sorting;
	}
	
	@Override
	public String toString() {
		return "VirtualViewColumn [title=" + title + ", itemName=" + itemName + ", formula=" + valueFormula + ", isCategory="
				+ isCategory + ", sorting=" + sorting + "]";
	}
	
}
